/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.gallery3d.ui;

import android.graphics.Rect;
import android.os.Handler;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.animation.DecelerateInterpolator;

import com.android.gallery3d.anim.Animation;
import com.android.gallery3d.app.GalleryContext;
import com.android.gallery3d.common.Utils;
import com.android.gallery3d.glrenderer.GLCanvas;
//TY zhencc add for New Design Gallery
import com.android.gallery3d.util.GalleryUtils;
//TY zhencc end for New Design Gallery
import com.android.gallery3d.R;
import android.view.VelocityTracker;
public class SlotView extends GLView {
    @SuppressWarnings("unused")
    private static final String TAG = "SlotView";

    //TY zhencc modify for New Design Gallery
    //private static final boolean WIDE = true;
    private static final boolean WIDE = false;
    //TY zhencc end for New Design Gallery
    private static final int INDEX_NONE = -1;

    public static final int RENDER_MORE_PASS = 1;
    public static final int RENDER_MORE_FRAME = 2;

    public interface Listener {
        public void onDown(int index);
        public void onUp(boolean followedByLongPress);
        public void onSingleTapUp(int index);
        public void onLongTap(int index);
        public void onScrollPositionChanged(int position, int total);
    }

    public static class SimpleListener implements Listener {
        @Override public void onDown(int index) {}
        @Override public void onUp(boolean followedByLongPress) {}
        @Override public void onSingleTapUp(int index) {}
        @Override public void onLongTap(int index) {}
        @Override public void onScrollPositionChanged(int position, int total) {}
    }

    public static interface SlotRenderer {
        public void prepareDrawing();
        public void onVisibleRangeChanged(int visibleStart, int visibleEnd);
        public void onSlotSizeChanged(int width, int height);
        public int renderSlot(GLCanvas canvas, int index, int pass, int width, int height);
    }


    //TYRD:changjj add for get slot pos begin
    public interface SlotPos{
        public int getSlotOldPos(int index);
        public int getSlotNewPos(int index);
    }

    private SlotPos   mSlotPosData;
    //TYRD:changjj add for get slot pos end
    

    private final GestureDetector mGestureDetector;
    private final ScrollerHelper mScroller;
    private final Paper mPaper = new Paper();

    private Listener mListener;
    private UserInteractionListener mUIListener;

    private boolean mMoreAnimation = false;
    private SlotAnimation mAnimation = null;
    private final Layout mLayout = new Layout();
    private int mStartIndex = INDEX_NONE;

    // whether the down action happened while the view is scrolling.
    private boolean mDownInScrolling;
    private int mOverscrollEffect = OVERSCROLL_TY;//TY wb034 20150108 add for tygallery
    private final Handler mHandler;

    private SlotRenderer mRenderer;

    private int[] mRequestRenderSlots = new int[16];

    public static final int OVERSCROLL_3D = 0;
    public static final int OVERSCROLL_SYSTEM = 1;
    public static final int OVERSCROLL_NONE = 2;
    public static final int OVERSCROLL_TY = 4;
    
    private final int drag_height;

    // to prevent allocating memory
    private final Rect mTempRect = new Rect();
	//TY wb034 20150108 add begin for tygallery
    private EdgeView mEdgeView;
    private VelocityTracker mVelocityTracker;
    private boolean isOverscrollEffect = false;
	//TY wb034 20150108 add end for tygallery
    
    public SlotView(GalleryContext activity, Spec spec) {
    	mEdgeView = new EdgeView(activity);
        mGestureDetector = new GestureDetector(activity.getAndroidContext(), new MyGestureListener());
        mScroller = new ScrollerHelper(activity.getAndroidContext());
        mHandler = new SynchronizedHandler(activity.getGLRoot());
        drag_height=activity.getResources().getDimensionPixelSize(R.dimen.ty_drag_height);//TY wb034 20150108 add for tygallery
        
        setSlotSpec(spec);
    }

    public void setSlotRenderer(SlotRenderer slotDrawer) {
        mRenderer = slotDrawer;
        if (mRenderer != null) {
            mRenderer.onSlotSizeChanged(mLayout.mSlotWidth, mLayout.mSlotHeight);
            mRenderer.onVisibleRangeChanged(getVisibleStart(), getVisibleEnd());
        }
    }

    //TYRD:changjj add for anamition begin
    public void setSlotPosInterface(SlotPos slotPosData){
        mSlotPosData = slotPosData;
    }
    //TYRD:changjj add for anamition end

    public void setCenterIndex(int index) {
        int slotCount = mLayout.mSlotCount;
        if (index < 0 || index >= slotCount) {
            return;
        }
        Rect rect = mLayout.getSlotRect(index, mTempRect);
        int position = WIDE
                ? (rect.left + rect.right - getWidth()) / 2
                : (rect.top + rect.bottom - getHeight()) / 2;
        setScrollPosition(position);
    }

    public void makeSlotVisible(int index) {
        Rect rect = mLayout.getSlotRect(index, mTempRect);
        int visibleBegin = WIDE ? mScrollX : mScrollY;
        int visibleLength = WIDE ? getWidth() : getHeight();
        int visibleEnd = visibleBegin + visibleLength;
        int slotBegin = WIDE ? rect.left : rect.top;
        int slotEnd = WIDE ? rect.right : rect.bottom;

        int position = visibleBegin;
        if (visibleLength < slotEnd - slotBegin) {
            position = visibleBegin;
        } else if (slotBegin < visibleBegin) {
            position = slotBegin;
        } else if (slotEnd > visibleEnd) {
            position = slotEnd - visibleLength;
        }

        setScrollPosition(position);
    }

    public void setScrollPosition(int position) {
    	//TY wb034 20150108 modify begin  for tygallery
       // position = Utils.clamp(position, 0, mLayout.getScrollLimit());
    	if(mOverscrollEffect==OVERSCROLL_TY){
    		 position = Utils.clamp(position,-drag_height, mLayout.getScrollLimit()+drag_height);
    	}else{
    		 position = Utils.clamp(position,0, mLayout.getScrollLimit());
    	}
    	//TY wb034 20150108 modify end for tygallery
    	
        mScroller.setPosition(position);
        updateScrollPosition(position, false);
    }

    public void setSlotSpec(Spec spec) {
        mLayout.setSlotSpec(spec);
    }

    @Override
    public void addComponent(GLView view) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void onLayout(boolean changeSize, int l, int t, int r, int b) {
        if (mLayout.mSpec.layoutChange){
            mLayout.mSpec.layoutChange = false;
        }else{
            if (!changeSize) return;
        }

        // Make sure we are still at a resonable scroll position after the size
        // is changed (like orientation change). We choose to keep the center
        // visible slot still visible. This is arbitrary but reasonable.
        int visibleIndex = (mLayout.getVisibleStart() + mLayout.getVisibleEnd()) / 2;
        mLayout.setSize(r - l, b - t);
        if(isOverscrollEffect && mOverscrollEffect == OVERSCROLL_TY){
            mEdgeView.layout(0, 
                -mLayout.mSpec.marinTop, 
                r - l, 
                b - t - mLayout.mSpec.marinBottom - 2*mLayout.mSpec.marinTop);
        }
        makeSlotVisible(visibleIndex);
        if (mOverscrollEffect == OVERSCROLL_3D) {
            mPaper.setSize(r - l, b - t);
        }else if (mOverscrollEffect == OVERSCROLL_TY){
            if(mScrollY < 0 || mScrollY > mLayout.getScrollLimit()){
                mScroller.forceFinished();                       
                mScroller.startScrollBack(Math.round(mScrollY), 0, mLayout.getScrollLimit());                      
            }
        }
    }

    public void startScatteringAnimation(RelativePosition position) {
        mAnimation = new ScatteringAnimation(position);
        mAnimation.start();
        if (mLayout.mSlotCount != 0) invalidate();
    }

    public void startRisingAnimation() {
        mAnimation = new RisingAnimation();
        mAnimation.start();
        if (mLayout.mSlotCount != 0) invalidate();
    }

    //TYRD:changjj add for translate animation begin
    public void startTranslatingAnimation(){
        mAnimation = new TranslatingAnimation();
        mAnimation.start();
        if (mLayout.mSlotCount != 0) invalidate();
    }
    //TYRD:changjj add for translate animation end

    private void updateScrollPosition(int position, boolean force) {
        if (!force && (WIDE ? position == mScrollX : position == mScrollY)) return;
        if (WIDE) {
            mScrollX = position;
        } else {
            mScrollY = position;
        }
        mLayout.setScrollPosition(position);
        onScrollPositionChanged(position);
    }

    protected void onScrollPositionChanged(int newPosition) {
        int limit = mLayout.getScrollLimit();
        mListener.onScrollPositionChanged(newPosition, limit);
    }

    public Rect getSlotRect(int slotIndex) {
        return mLayout.getSlotRect(slotIndex, new Rect());
    }

    @Override
    protected boolean onTouch(MotionEvent event) {
    	if(mVelocityTracker==null){
    		mVelocityTracker =VelocityTracker.obtain();
    	}
    	mVelocityTracker.addMovement(event);
        if (mUIListener != null) mUIListener.onUserInteraction();
        mGestureDetector.onTouchEvent(event);
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mDownInScrolling = !mScroller.isFinished();
                mScroller.forceFinished();
                break;
            case MotionEvent.ACTION_UP:
            	//TY wb034 20150108 add begin for tygallery
            	if(mOverscrollEffect==OVERSCROLL_TY){
                    if(isOverscrollEffect){
            		    mEdgeView.onRelease();
                    }
                	if(mScrollY<0||mScrollY>mLayout.getScrollLimit()){
                        mScroller.forceFinished();                       
                		mScroller.startScrollBack( Math.round(mScrollY), 0, mLayout.getScrollLimit());
                	}
            		invalidate();
            		return true;
            	}
            	//TY wb034 20150108 add end for tygallery
                mPaper.onRelease();
                invalidate();                 
               break;
             //TY wb034 20140108 add begin for tygallery
            case MotionEvent.ACTION_MOVE:            	
		if (mOverscrollEffect == OVERSCROLL_TY) {
			final VelocityTracker velocityTracker = mVelocityTracker;
			velocityTracker.computeCurrentVelocity(1000);
			float vel = velocityTracker.getYVelocity();
			if (isOverscrollEffect && mScrollY < 0) {	
				mEdgeView.Absorb((int) (vel), EdgeView.TOP);	
			}
			if (isOverscrollEffect && mScrollY > mLayout.getScrollLimit()) {	
				mEdgeView.Absorb((int) (vel), EdgeView.BOTTOM);	
			}	
		}				
            	break;
            case MotionEvent.ACTION_CANCEL:
            	if(mVelocityTracker!=null){
            		mVelocityTracker.recycle();
            		mVelocityTracker=null;
            	}
            	break;
            //TY wb034 20140108 add end for tygallery
        }
        return true;
    }

    public void setListener(Listener listener) {
        mListener = listener;
    }

    public void setUserInteractionListener(UserInteractionListener listener) {
        mUIListener = listener;
    }

    public void setOverscrollEffect(int kind) {
        mOverscrollEffect = kind;
        mScroller.setOverfling(kind == OVERSCROLL_SYSTEM);
    }

    private static int[] expandIntArray(int array[], int capacity) {
        while (array.length < capacity) {
            array = new int[array.length * 2];
        }
        return array;
    }

    @Override
    protected void render(GLCanvas canvas) {
        super.render(canvas);

        if (mRenderer == null) return;
        mRenderer.prepareDrawing();

        long animTime = AnimationTime.get();
        boolean more = mScroller.advanceAnimation(animTime);
        more |= mLayout.advanceAnimation(animTime);
        int oldX = mScrollX;
        updateScrollPosition(mScroller.getPosition(), false);

        boolean paperActive = false;
        if (mOverscrollEffect == OVERSCROLL_3D) {
            // Check if an edge is reached and notify mPaper if so.
            int newX = mScrollX;
            int limit = mLayout.getScrollLimit();
            if (oldX > 0 && newX == 0 || oldX < limit && newX == limit) {
                float v = mScroller.getCurrVelocity();
                if (newX == limit) v = -v;

                // I don't know why, but getCurrVelocity() can return NaN.
                if (!Float.isNaN(v)) {
                    mPaper.edgeReached(v);
                }
            }
            paperActive = mPaper.advanceAnimation();
        }

        more |= paperActive;

        if (mAnimation != null) {
            more |= mAnimation.calculate(animTime);
        }

        canvas.translate(-mScrollX, -mScrollY);

        int requestCount = 0;
        int requestedSlot[] = expandIntArray(mRequestRenderSlots,
                mLayout.mVisibleEnd - mLayout.mVisibleStart);

        for (int i = mLayout.mVisibleEnd - 1; i >= mLayout.mVisibleStart; --i) {
            int r = renderItem(canvas, i, 0, paperActive);
            if ((r & RENDER_MORE_FRAME) != 0) more = true;
            if ((r & RENDER_MORE_PASS) != 0) requestedSlot[requestCount++] = i;
        }

        for (int pass = 1; requestCount != 0; ++pass) {
            int newCount = 0;
            for (int i = 0; i < requestCount; ++i) {
                int r = renderItem(canvas,
                        requestedSlot[i], pass, paperActive);
                if ((r & RENDER_MORE_FRAME) != 0) more = true;
                if ((r & RENDER_MORE_PASS) != 0) requestedSlot[newCount++] = i;
            }
            requestCount = newCount;
        }
      //TY wb034 20140108 add begin for tygallery
        if(mOverscrollEffect==OVERSCROLL_TY){
            if(mScrollY<0&&!mEdgeView.isfinished(EdgeView.TOP)){
            	renderChild(canvas,mEdgeView,EdgeView.TOP);
            }else if((mScrollY>mLayout.getScrollLimit())&&!mEdgeView.isfinished(EdgeView.BOTTOM)){           
            	renderChild(canvas,mEdgeView,EdgeView.BOTTOM);
            }
     
        }
      //TY wb034 20140108 add end for tygallery
        canvas.translate(mScrollX, mScrollY);

        if (more) invalidate();

        final UserInteractionListener listener = mUIListener;
        if (mMoreAnimation && !more && listener != null) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    listener.onUserInteractionEnd();
                }
            });
        }
        mMoreAnimation = more;
    }
	 //TY wb034 20140108 add this method for tygallery
    protected void renderChild(GLCanvas canvas, GLView component,int direction) {
        if (component.getVisibility() != GLView.VISIBLE) return;
        canvas.save(GLCanvas.SAVE_FLAG_ALPHA | GLCanvas.SAVE_FLAG_MATRIX);
        int xoffset = component.mBounds.left - mScrollX;
        int yoffset = component.mBounds.top - mScrollY;
        canvas.translate(-xoffset,-yoffset);
        if(isOverscrollEffect){
            mEdgeView.render(canvas,direction);
        }
        canvas.restore();
    }
    private int renderItem(
            GLCanvas canvas, int index, int pass, boolean paperActive) {
        canvas.save(GLCanvas.SAVE_FLAG_ALPHA | GLCanvas.SAVE_FLAG_MATRIX);
        Rect rect = mLayout.getSlotRect(index, mTempRect);
        if (paperActive) {
            canvas.multiplyMatrix(mPaper.getTransform(rect, mScrollX), 0);
        } else {
            canvas.translate(rect.left, rect.top, 0);
        }
        if (mAnimation != null && mAnimation.isActive()) {
            mAnimation.apply(canvas, index, rect);
        }
        int result = mRenderer.renderSlot(
                canvas, index, pass, rect.right - rect.left, rect.bottom - rect.top);
        canvas.restore();
        return result;
    }

    public static abstract class SlotAnimation extends Animation {
        protected float mProgress = 0;

        public SlotAnimation() {
          //taoxj modify  setInterpolator(new DecelerateInterpolator(4));
            setDuration(1500);
        }

        @Override
        protected void onCalculate(float progress) {
            mProgress = progress;
        }

        abstract public void apply(GLCanvas canvas, int slotIndex, Rect target);
    }

    public static class RisingAnimation extends SlotAnimation {
        private static final int RISING_DISTANCE = 128;

        @Override
        public void apply(GLCanvas canvas, int slotIndex, Rect target) {
            canvas.translate(0, 0, RISING_DISTANCE * (1 - mProgress));
        }
    }

    public static class ScatteringAnimation extends SlotAnimation {
        private int PHOTO_DISTANCE = 1000;
        private RelativePosition mCenter;

        public ScatteringAnimation(RelativePosition center) {
            mCenter = center;
        }

        @Override
        public void apply(GLCanvas canvas, int slotIndex, Rect target) {
            canvas.translate(
                    (mCenter.getX() - target.centerX()) * (1 - mProgress),
                    (mCenter.getY() - target.centerY()) * (1 - mProgress),
                    slotIndex * PHOTO_DISTANCE * (1 - mProgress));
            canvas.setAlpha(mProgress);
        }
    }

    //TYRD:changjj add for new Gallary desgine begin
    public class TranslatingAnimation extends SlotAnimation {
        
        public TranslatingAnimation(){
        }
        @Override
        public void apply(GLCanvas canvas, int slotIndex, Rect target) {
            int oldPos = mSlotPosData.getSlotOldPos(slotIndex);
            if(oldPos ==  -1 || oldPos == slotIndex)
                return;
            Rect mTempRect1 = new Rect();
            Rect mTempRect2 = new Rect();
            Rect oldRect = mLayout.getSlotRect(oldPos,mTempRect1);
            Rect newRect = mLayout.getSlotRect(slotIndex,mTempRect2);
            int xDistance = oldRect.left - newRect.left;
            int yDistance = oldRect.top - newRect.top;
           
            float tempx = xDistance * (1 - mProgress);
            float tempy = yDistance * (1 - mProgress);
            canvas.translate(tempx,
                     tempy);
        } 
        public void caculateSlotPostion(GLCanvas canvas,int slotIndex){
            
        }
    }
    //TYRD:changjj add for new Gallary desgine end

    // This Spec class is used to specify the size of each slot in the SlotView.
    // There are two ways to do it:
    //
    // (1) Specify slotWidth and slotHeight: they specify the width and height
    //     of each slot. The number of rows and the gap between slots will be
    //     determined automatically.
    // (2) Specify rowsLand, rowsPort, and slotGap: they specify the number
    //     of rows in landscape/portrait mode and the gap between slots. The
    //     width and height of each slot is determined automatically.
    //
    // The initial value of -1 means they are not specified.
    public static class Spec {
        public int slotWidth = -1;
        public int slotHeight = -1;
        public int slotHeightAdditional = 0;

        public boolean layoutChange = false;
        public int marinTop = 0;
        public int marinBottom = 0;
        public int rowsLand = -1;
        public int rowsPort = -1;
        public int slotGap = -1;
        public int slotGapEdge = -1;
        public int placeholderPageGap = -1;
    }

    public class Layout {

        private int mVisibleStart;
        private int mVisibleEnd;

        private int mSlotCount;
        private int mSlotWidth;
        private int mSlotHeight;
        private int mSlotGap;
        //TY zhencc add for New Design Gallery
        private int mSlotGapEdge;
        public int mPlaceholderPageGap;
        //TY zhencc end for New Design Gallery
        private int mMarinTop;
        private int mMarinBottom;

        private Spec mSpec;

        private int mWidth;
        private int mHeight;

        private int mUnitCount;
        private int mContentLength;
        private int mScrollPosition;

        private IntegerAnimation mVerticalPadding = new IntegerAnimation();
        private IntegerAnimation mHorizontalPadding = new IntegerAnimation();

        public void setSlotSpec(Spec spec) {
            mSpec = spec;
        }

        public boolean setSlotCount(int slotCount) {
            if (slotCount == mSlotCount) return false;
            if (mSlotCount != 0) {
                mHorizontalPadding.setEnabled(true);
                mVerticalPadding.setEnabled(true);
            }
            mSlotCount = slotCount;
            int hPadding = mHorizontalPadding.getTarget();
            int vPadding = mVerticalPadding.getTarget();
            initLayoutParameters();
            return vPadding != mVerticalPadding.getTarget()
                    || hPadding != mHorizontalPadding.getTarget();
        }

        public Rect getSlotRect(int index, Rect rect) {
            int col, row;
            if (WIDE) {
                col = index / mUnitCount;
                row = index - col * mUnitCount;
            } else {
                row = index / mUnitCount;
                col = index - row * mUnitCount;
            }

	        //TIANYU: yuxin modify begin for New Design Gallery
            //int x = mHorizontalPadding.get() + col * (mSlotWidth + mSlotGap);
            //int y = mVerticalPadding.get() + row * (mSlotHeight + mSlotGap);
            //rect.set(x, y, x + mSlotWidth, y + mSlotHeight);
            //return rect;
            
            int x = 0;
            int y = 0;
            //if (mSlotCount >= mUnitCount){
            //    x = col * (mSlotWidth + mSlotGap) + mSlotGapEdge;
            //    y = row * (mSlotHeight + mSlotGap) + mPlaceholderPageGap;
            //}else{
            //    x = mHorizontalPadding.get() + col * (mSlotWidth + mSlotGap);
            //    y = mVerticalPadding.get() + row * (mSlotHeight + mSlotGap);
            //}
            x = col * (mSlotWidth + mSlotGap) + mSlotGapEdge;
            y = row * (mSlotHeight + mSlotGap) + mPlaceholderPageGap;
            rect.set(x, y + mMarinTop, x + mSlotWidth, y + mSlotHeight + mMarinTop);
            return rect;
            //TIANYU: yuxin modify end for New Design Gallery
        }

        public int getSlotWidth() {
            return mSlotWidth;
        }

        public int getSlotHeight() {
            return mSlotHeight;
        }

        // Calculate
        // (1) mUnitCount: the number of slots we can fit into one column (or row).
        // (2) mContentLength: the width (or height) we need to display all the
        //     columns (rows).
        // (3) padding[]: the vertical and horizontal padding we need in order
        //     to put the slots towards to the center of the display.
        //
        // The "major" direction is the direction the user can scroll. The other
        // direction is the "minor" direction.
        //
        // The comments inside this method are the description when the major
        // directon is horizontal (X), and the minor directon is vertical (Y).
        private void initLayoutParameters(
                int majorLength, int minorLength,  /* The view width and height */
                int majorUnitSize, int minorUnitSize,  /* The slot width and height */
                int[] padding) {
            int unitCount = (minorLength + mSlotGap) / (minorUnitSize + mSlotGap);
            if (unitCount == 0) unitCount = 1;
            mUnitCount = unitCount;

            // We put extra padding above and below the column.
            int availableUnits = Math.min(mUnitCount, mSlotCount);
            int usedMinorLength = availableUnits * minorUnitSize +
                    (availableUnits - 1) * mSlotGap;
            padding[0] = (minorLength - usedMinorLength) / 2;

            // Then calculate how many columns we need for all slots.
            int count = ((mSlotCount + mUnitCount - 1) / mUnitCount);
            
            //TIANYU: yuxin modify begin for New Design Gallery
            //mContentLength = count * majorUnitSize + (count - 1) * mSlotGap;
            mContentLength = count * majorUnitSize + (count - 1) * mSlotGap + mPlaceholderPageGap*2 + mMarinTop + mMarinBottom;
            //TIANYU: yuxin modify end for New Design Gallery

            // If the content length is less then the screen width, put
            // extra padding in left and right.
            padding[1] = Math.max(0, (majorLength - mContentLength) / 2);
        }

        private void initLayoutParameters() {
            // Initialize mSlotWidth and mSlotHeight from mSpec
            if (mSpec.slotWidth != -1) {
                mSlotGap = 0;
                mSlotWidth = mSpec.slotWidth;
                mSlotHeight = mSpec.slotHeight;
            } else {
                mMarinTop = mSpec.marinTop;
                mMarinBottom = mSpec.marinBottom;
                int rows = (mWidth > mHeight) ? mSpec.rowsLand : mSpec.rowsPort;
                mSlotGap = mSpec.slotGap;
                //TY zhencc modify for New Design Gallery
                //mSlotHeight = Math.max(1, (mHeight - (rows - 1) * mSlotGap) / rows);
                //mSlotWidth = mSlotHeight - mSpec.slotHeightAdditional;
                mSlotGapEdge = mSpec.slotGapEdge;
                mPlaceholderPageGap = mSpec.placeholderPageGap;
                mSlotWidth = Math.max(1, (mWidth - (rows - 1) * mSlotGap - 2 * mSlotGapEdge) / rows);
                mSlotHeight = mSlotWidth - mSpec.slotHeightAdditional;
		        //TY zhencc end for New Design Gallery
            }

            if (mRenderer != null) {
                mRenderer.onSlotSizeChanged(mSlotWidth, mSlotHeight);
            }

            int[] padding = new int[2];
            if (WIDE) {
                initLayoutParameters(mWidth, mHeight, mSlotWidth, mSlotHeight, padding);
                mVerticalPadding.startAnimateTo(padding[0]);
                mHorizontalPadding.startAnimateTo(padding[1]);
            } else {
                initLayoutParameters(mHeight, mWidth, mSlotHeight, mSlotWidth, padding);
                mVerticalPadding.startAnimateTo(padding[1]);
                mHorizontalPadding.startAnimateTo(padding[0]);
            }
            updateVisibleSlotRange();
        }

        public void setSize(int width, int height) {
            mWidth = width;
            mHeight = height;
            initLayoutParameters();
        }

        private void updateVisibleSlotRange() {
            int position = mScrollPosition;

            if (WIDE) {
                int startCol = position / (mSlotWidth + mSlotGap);
                int start = Math.max(0, mUnitCount * startCol);
                int endCol = (position + mWidth + mSlotWidth + mSlotGap - 1) /
                        (mSlotWidth + mSlotGap);
                int end = Math.min(mSlotCount, mUnitCount * endCol);
                setVisibleRange(start, end);
            } else {
                int startRow = position / (mSlotHeight + mSlotGap);
                int start = Math.max(0, mUnitCount * startRow);
                int endRow = (position + mHeight + mSlotHeight + mSlotGap - 1) /
                        (mSlotHeight + mSlotGap);
                int end = Math.min(mSlotCount, mUnitCount * endRow);
                setVisibleRange(start, end);
            }
        }

        public void setScrollPosition(int position) {
            if (mScrollPosition == position) return;
            mScrollPosition = position;
            updateVisibleSlotRange();
        }

        private void setVisibleRange(int start, int end) {
            if (start == mVisibleStart && end == mVisibleEnd) return;
            if (start < end) {
                mVisibleStart = start;
                mVisibleEnd = end;
            } else {
                mVisibleStart = mVisibleEnd = 0;
            }
            if (mRenderer != null) {
                mRenderer.onVisibleRangeChanged(mVisibleStart, mVisibleEnd);
            }
        }

        public int getVisibleStart() {
            return mVisibleStart;
        }

        public int getVisibleEnd() {
            return mVisibleEnd;
        }

        public int getSlotIndexByPosition(float x, float y) {
            int absoluteX = Math.round(x) + (WIDE ? mScrollPosition : 0);
            int absoluteY = Math.round(y) + (WIDE ? 0 : mScrollPosition);

	        //TIANYU: yuxin modify begin for New Design Gallery
            //absoluteX -= mHorizontalPadding.get();
            //absoluteY -= mVerticalPadding.get();
            absoluteY -= mMarinTop;
	        //TIANYU: yuxin modify end for New Design Gallery

            if (absoluteX < 0 || absoluteY < 0) {
                return INDEX_NONE;
            }

            int columnIdx = absoluteX / (mSlotWidth + mSlotGap);
            int rowIdx = absoluteY / (mSlotHeight + mSlotGap);

            if (!WIDE && columnIdx >= mUnitCount) {
                return INDEX_NONE;
            }

            if (WIDE && rowIdx >= mUnitCount) {
                return INDEX_NONE;
            }

            if (absoluteX % (mSlotWidth + mSlotGap) >= mSlotWidth) {
                return INDEX_NONE;
            }

            if (absoluteY % (mSlotHeight + mSlotGap) >= mSlotHeight) {
                return INDEX_NONE;
            }

            int index = WIDE
                    ? (columnIdx * mUnitCount + rowIdx)
                    : (rowIdx * mUnitCount + columnIdx);

            return index >= mSlotCount ? INDEX_NONE : index;
        }

        public int getScrollLimit() {
            int limit = WIDE ? mContentLength - mWidth : mContentLength - mHeight;
            return limit <= 0 ? 0 : limit;
        }

        public boolean advanceAnimation(long animTime) {
            // use '|' to make sure both sides will be executed
            return mVerticalPadding.calculate(animTime) | mHorizontalPadding.calculate(animTime);
        }
    }

    private class MyGestureListener implements GestureDetector.OnGestureListener {
        private boolean isDown;

        // We call the listener's onDown() when our onShowPress() is called and
        // call the listener's onUp() when we receive any further event.
        @Override
        public void onShowPress(MotionEvent e) {
            GLRoot root = getGLRoot();
            root.lockRenderThread();
            try {
                if (isDown) return;
                int index = mLayout.getSlotIndexByPosition(e.getX(), e.getY());
                if (index != INDEX_NONE) {
                    isDown = true;
                    mListener.onDown(index);
                }
            } finally {
                root.unlockRenderThread();
            }
        }

        private void cancelDown(boolean byLongPress) {
            if (!isDown) return;
            isDown = false;
            mListener.onUp(byLongPress);
        }

        @Override
        public boolean onDown(MotionEvent e) {
            return false;
        }

        @Override
        public boolean onFling(MotionEvent e1,
                MotionEvent e2, float velocityX, float velocityY) {
            cancelDown(false);
            int scrollLimit = mLayout.getScrollLimit();
            if (scrollLimit == 0) return false;
            float velocity = WIDE ? velocityX : velocityY;
            mScroller.fling((int) -velocity, 0, scrollLimit);
          //TY wb034 20140108 add begin for tygallery
            if(isOverscrollEffect && mOverscrollEffect==OVERSCROLL_TY){
            	mEdgeView.onRelease();
            }           
          //TY wb034 20140108 add end for tygallery
            if (mUIListener != null) mUIListener.onUserInteractionBegin();
            invalidate();
            return true;
        }

        @Override
        public boolean onScroll(MotionEvent e1,
                MotionEvent e2, float distanceX, float distanceY) {
            cancelDown(false);
            float distance = WIDE ? distanceX : distanceY;
          //TY wb034 20140108 modify begin  for tygallery
          /*  int overDistance = mScroller.startScroll(
                    Math.round(distance), 0, mLayout.getScrollLimit());*/ //wb034            
            int overDistance=0;
            
            if(mOverscrollEffect==OVERSCROLL_TY){
            	   overDistance = mScroller.startScroll(
                          Math.round(distance),-drag_height, mLayout.getScrollLimit()+drag_height);
            }else{
            	  overDistance = mScroller.startScroll(
                         Math.round(distance),0, mLayout.getScrollLimit());
            }
            if(isOverscrollEffect && mOverscrollEffect==OVERSCROLL_TY){
            	if (mScrollY<=0) {
                    mEdgeView.onPull((int)(mScrollY), EdgeView.TOP);                   
                } else if (mScrollY >= mLayout.getScrollLimit()) {
                	mEdgeView.onPull((int)(mScrollY-mLayout.getScrollLimit()), EdgeView.BOTTOM);
                }
            }
          //TY wb034 20140108 modify end for tygallery
            
            if (mOverscrollEffect == OVERSCROLL_3D && overDistance != 0) {
                mPaper.overScroll(overDistance);
            }
            invalidate();
            return true;
        }

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            cancelDown(false);
            if (mDownInScrolling) return true;
            int index = mLayout.getSlotIndexByPosition(e.getX(), e.getY());
            if (index != INDEX_NONE) mListener.onSingleTapUp(index);
            return true;
        }

        @Override
        public void onLongPress(MotionEvent e) {
            cancelDown(true);
            if (mDownInScrolling) return;
            lockRendering();
            try {
                int index = mLayout.getSlotIndexByPosition(e.getX(), e.getY());
                if (index != INDEX_NONE) mListener.onLongTap(index);
            } finally {
                unlockRendering();
            }
        }
    }

    public void setStartIndex(int index) {
        mStartIndex = index;
    }

    // Return true if the layout parameters have been changed
    public boolean setSlotCount(int slotCount) {
        boolean changed = mLayout.setSlotCount(slotCount);

        // mStartIndex is applied the first time setSlotCount is called.
        if (mStartIndex != INDEX_NONE) {
            setCenterIndex(mStartIndex);
            mStartIndex = INDEX_NONE;
        }
        // Reset the scroll position to avoid scrolling over the updated limit.
        setScrollPosition(WIDE ? mScrollX : mScrollY);
        return changed;
    }

    public int getVisibleStart() {
        return mLayout.getVisibleStart();
    }

    public int getVisibleEnd() {
        return mLayout.getVisibleEnd();
    }

    public int getScrollX() {
        return mScrollX;
    }

    public int getScrollY() {
        return mScrollY;
    }

    public Rect getSlotRect(int slotIndex, GLView rootPane) {
        // Get slot rectangle relative to this root pane.
        Rect offset = new Rect();
        rootPane.getBoundsOf(this, offset);
        Rect r = getSlotRect(slotIndex);
        r.offset(offset.left - getScrollX(),
                offset.top - getScrollY());
        return r;
    }

    private static class IntegerAnimation extends Animation {
        private int mTarget;
        private int mCurrent = 0;
        private int mFrom = 0;
        private boolean mEnabled = false;

        public void setEnabled(boolean enabled) {
            mEnabled = enabled;
        }

        public void startAnimateTo(int target) {
            if (!mEnabled) {
                mTarget = mCurrent = target;
                return;
            }
            if (target == mTarget) return;

            mFrom = mCurrent;
            mTarget = target;
            setDuration(180);
            start();
        }

        public int get() {
            return mCurrent;
        }

        public int getTarget() {
            return mTarget;
        }

        @Override
        protected void onCalculate(float progress) {
            mCurrent = Math.round(mFrom + progress * (mTarget - mFrom));
            if (progress == 1f) mEnabled = false;
        }
    }
}
