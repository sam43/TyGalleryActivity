/*
 * TIANYU: yuxin add for New Design Gallery
 */

package com.android.gallery3d.ui;

import android.graphics.Rect;
import android.os.Handler;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.animation.DecelerateInterpolator;
import com.android.gallery3d.glrenderer.GLCanvas;
import com.android.gallery3d.anim.Animation;
import com.android.gallery3d.app.GalleryContext;
import com.android.gallery3d.common.Utils;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import com.android.gallery3d.R;
import android.view.VelocityTracker;

public class TySlotView extends GLView {
    @SuppressWarnings("unused")
    private static final String TAG = "Gallery2/TySlotView";

    private static final boolean WIDE = false;
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
        public void onVisibleRangeChanged(int visibleStart, int visibleEnd, int visibleStartTimeslot, int visibleEndTimeslot);
        public void onTitleSizeChanged(int width, int height);
        public int renderTitle(GLCanvas canvas, int titleIndex, int pass, int width, int height);
        public void onSlotSizeChanged(int width, int height);
        public int renderSlot(GLCanvas canvas, int index, int pass, int width, int height);
        //taoxj add for separator line begin
        public void onSeparatorLineSizeChanged(int width, int height);
        public int renderSeparatorLine(GLCanvas canvas, int titleIndex, int pass, int width, int height);
        //taoxj add for separator line end
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
    //TYRD:changjj add for delete anamition begin
    private Layout mOldLayout = null;
    //TYRD:changjj add for delete anamition end

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
    //TY wb034 20150108 add for tygallery
    private EdgeView mEdgeView;
    private VelocityTracker mVelocityTracker;
    private boolean isOverscrollEffect = false;
    //TY wb034 20150108 add for tygallery
    
    public TySlotView(GalleryContext activity, Spec spec) {
    	mEdgeView = new EdgeView(activity);
        mGestureDetector = new GestureDetector(activity.getAndroidContext(), new MyGestureListener());
        mScroller = new ScrollerHelper(activity.getAndroidContext());
        mHandler = new SynchronizedHandler(activity.getGLRoot());
        drag_height = activity.getResources().getDimensionPixelSize(R.dimen.ty_drag_height);//TY wb034 20150108 add for tygallery
        
        setSlotSpec(spec);
    }

    public void setSlotRenderer(SlotRenderer slotDrawer) {
        mRenderer = slotDrawer;
        if (mRenderer != null) {
            if(mLayout.mSpec.titleHeight > -1){
                int titleWidth = Math.max(0, mLayout.mWidth - 2 * mLayout.mSlotGapEdge);
                int titleHeight = Math.max(0, mLayout.mSpec.titleHeight);
                mRenderer.onTitleSizeChanged(titleWidth, titleHeight);
            }
            mRenderer.onSlotSizeChanged(mLayout.mSlotWidth, mLayout.mSlotHeight);
            mRenderer.onVisibleRangeChanged(getVisibleStart(), getVisibleEnd(),
                getVisibleStartTimeslot(), getVisibleEndTimeslot());
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
    	if(mOverscrollEffect == OVERSCROLL_TY){
    		 position = Utils.clamp(position, -drag_height, mLayout.getScrollLimit()+drag_height);
    	}else{
    		 position = Utils.clamp(position, 0, mLayout.getScrollLimit());
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
        int visibleIndex = 0;
        if (mLayout.mSpec.layoutChange){
            mLayout.mSpec.layoutChange = false;
            visibleIndex = (mLayout.getVisibleStart() + mLayout.getVisibleEnd()) / 2;
        }else{
            if (!changeSize) return;
            visibleIndex = mLayout.getVisibleStart();
        }

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
        if(mVelocityTracker == null){
            mVelocityTracker = VelocityTracker.obtain();
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
                if(mOverscrollEffect == OVERSCROLL_TY){
                    if(isOverscrollEffect){
                        mEdgeView.onRelease();
                    }
                    if(mScrollY < 0 || mScrollY > mLayout.getScrollLimit()){
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
                if(isOverscrollEffect){
                    mEdgeView.onRelease();
                }
                if(mScrollY < 0 || mScrollY > mLayout.getScrollLimit()){
                    mScroller.forceFinished();                       
                    mScroller.startScrollBack( Math.round(mScrollY), 0, mLayout.getScrollLimit());                      
                }
                invalidate();
                if(mVelocityTracker != null){
                    mVelocityTracker.recycle();
                    mVelocityTracker = null;
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

        if(mLayout.mSpec.titleHeight > -1){
            for (int ts = mLayout.mVisibleEndTimeslot - 1; ts >= mLayout.mVisibleStartTimeslot; --ts) {
                renderTimeslot(canvas, ts, 0, paperActive);
            }
        }
        //taoxj add for separator line begin
        if(mLayout.mSpec.separatorLineHeight > -1){
            for (int ts = mLayout.mVisibleEndTimeslot - 1; ts >= mLayout.mVisibleStartTimeslot - 1; --ts) {
                if(ts < 0) continue;
                renderSeparatorLine(canvas, ts, 0, paperActive);
            }
        }
        //taoxj add for separator line end

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
    ////TY wb034 20140108 add this method for tygallery
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

    private int renderTimeslot(
            GLCanvas canvas, int titleIndex, int pass, boolean paperActive) {
        canvas.save(GLCanvas.SAVE_FLAG_ALPHA | GLCanvas.SAVE_FLAG_MATRIX);
        Rect rect = mLayout.getTimeslotRect(titleIndex, mTempRect);
        if (paperActive) {
            canvas.multiplyMatrix(mPaper.getTransform(rect, mScrollX), 0);
        } else {
            canvas.translate(rect.left, rect.top, 0);
        }
        if (mAnimation != null && mAnimation.isActive()) {
            mAnimation.apply(canvas, titleIndex, rect);
        }
        int result = mRenderer.renderTitle(
                canvas, titleIndex, pass, rect.right - rect.left, rect.bottom - rect.top);
        canvas.restore();
        return result;
    }

     //taoxj add for separator line begin
       private int renderSeparatorLine(
            GLCanvas canvas, int titleIndex, int pass, boolean paperActive) {
        canvas.save(GLCanvas.SAVE_FLAG_ALPHA | GLCanvas.SAVE_FLAG_MATRIX);
        Rect rect = mLayout.getSeparatorLineRect(titleIndex, mTempRect);
        if (paperActive) {
            canvas.multiplyMatrix(mPaper.getTransform(rect, mScrollX), 0);
        } else {
            canvas.translate(rect.left, rect.top, 0);
        }
        if (mAnimation != null && mAnimation.isActive()) {
            mAnimation.apply(canvas, titleIndex, rect);
        }
        int result = mRenderer.renderSeparatorLine(
                canvas, titleIndex, pass, rect.right - rect.left, rect.bottom - rect.top);
        canvas.restore();
        return result;
    }
        //taoxj add for separator line end

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
          //  setInterpolator(new DecelerateInterpolator(4));
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
            int newPos = mSlotPosData.getSlotNewPos(slotIndex);
            if(oldPos ==  -1 || oldPos == newPos || mOldLayout == null)
                return;
            //Log.i("changjj","slotIndex " + slotIndex + " oldPos " + oldPos + " newPos " + newPos);
            Rect mTempRect1 = new Rect();
            Rect mTempRect2 = new Rect();
            Rect oldRect = mOldLayout.getSlotRect(oldPos,mTempRect1);
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

    // This Spec class is used to specify the size of each slot in the TySlotView.
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
        public int areaVSize = -1;
        public int areaHSize = -1;
        public int slotGap = -1;
        public int slotGapEdge = -1;
        public int titleHeight = -1;
        //taoxj add 
        public int slotRowMargin = 0; 
        public int separatorLineHeight = -1;
    }

    public class Layout implements Cloneable{

        private int mVisibleStart;
        private int mVisibleEnd;

        private int mSlotCount;
        private ArrayList<Integer> mTimeslotSize = new ArrayList<Integer>();
        private int mVisibleStartTimeslot;
        private int mVisibleEndTimeslot;
        private int mSlotWidth;
        private int mSlotHeight;
        private int mSlotGap;
        private int mSlotGapEdge;
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

        private int mSpecialCount;

        public Object clone(){
            Layout cloneObject = null;
            try{
                cloneObject = (Layout)super.clone();
                ArrayList<Integer> timeslotSize = new ArrayList<Integer>();
                //timeslotSize = (ArrayList<Integer>)mTimeslotSize.clone();
                timeslotSize.addAll(mTimeslotSize);
                cloneObject.mTimeslotSize = timeslotSize;
            }catch(CloneNotSupportedException e){
                e.printStackTrace();
            }
            return cloneObject;
        }
        public void release(){
            if(mTimeslotSize != null){
                mTimeslotSize.clear();
            }
        }

        public void setSlotSpec(Spec spec) {
            mSpec = spec;
        }
        //taoxj modify
        public boolean setSlotCount(int slotCount, LinkedHashMap<String, Integer> timeslotInfo) {
            if (slotCount == mSlotCount) return false;
            //TYRD:changjj add for anamition begin
            try{
                if(mOldLayout != null){
                    mOldLayout.release();
                }
                mOldLayout = (Layout)mLayout.clone();
                
            }catch(Exception e){
                Log.e(TAG,"exception " + e);
            }
            //TYRD:changjj add for anamition end
            if (mSlotCount != 0) {
                mHorizontalPadding.setEnabled(true);
                mVerticalPadding.setEnabled(true);
            }
            mSlotCount = slotCount;
            if(mLayout.mSpec.titleHeight > -1){
                mTimeslotSize.clear();
                if (timeslotInfo != null && timeslotInfo.size() > 0){
                    for (Map.Entry<String, Integer> item : timeslotInfo.entrySet()){
                        mTimeslotSize.add(item.getValue());
                    }                    
                }
            }
            int hPadding = mHorizontalPadding.getTarget();
            int vPadding = mVerticalPadding.getTarget();
            initLayoutParameters();
            return vPadding != mVerticalPadding.getTarget()
                    || hPadding != mHorizontalPadding.getTarget();
        }
        
        //taoxj add for separator line begin
        public Rect getSeparatorLineRect(int index, Rect rect) {
              int size = mTimeslotSize.size();
            if (size > 0){
                int x = mSlotGapEdge;
                int y = 0;
                int num = 0;
                int row = 0;
                if (index < size){
                    for (int i = 0; i <= index; i++){
                        num = mTimeslotSize.get(i);
                        if (i == 0){
                            if (num < mSpecialCount){
                                row = 1;
                            }else{
                                row = 1 + (num-mSpecialCount) / mUnitCount + ((num-mSpecialCount) % mUnitCount > 0 ? 1 : 0);
                            }
                            //row = 1 + (num-1) / mUnitCount + ((num-1) % mUnitCount > 0 ? 1 : 0);
                        }else{
                            row = num / mUnitCount + (num % mUnitCount > 0 ? 1 : 0);
                        }
                        if(i == index){
                         y += row * (mSlotHeight + mSlotGap) + mSpec.titleHeight;
                        }else{
                         y += row * (mSlotHeight + mSlotGap) + mSpec.titleHeight + mSpec.separatorLineHeight;
                        }
                }
                rect.set(x, y + mMarinTop, mWidth - mSlotGapEdge, y + mSpec.separatorLineHeight + mMarinTop);
            }
          }
            return rect;
          
        }
        //taoxj add for separator line end

        public Rect getTimeslotRect(int index, Rect rect) {
            int size = mTimeslotSize.size();
            if (size > 0){
                int x = mSlotGapEdge;
                int y = 0;
                int num = 0;
                int row = 0;
                if (index > 0 && index < size){
                    for (int i = 0; i < index; i++){
                        num = mTimeslotSize.get(i);
                        if (i == 0){
                            if (num < mSpecialCount){
                                row = 1;
                            }else{
                                row = 1 + (num-mSpecialCount) / mUnitCount + ((num-mSpecialCount) % mUnitCount > 0 ? 1 : 0);
                            }
                            //row = 1 + (num-1) / mUnitCount + ((num-1) % mUnitCount > 0 ? 1 : 0);
                            //taoxj modify
                            y += (row/*+1*/) * (mSlotHeight + mSlotGap) + mSpec.titleHeight + mSpec.separatorLineHeight;
                        }else{
                            row = num / mUnitCount + (num % mUnitCount > 0 ? 1 : 0);
                            y += row * (mSlotHeight + mSlotGap) + mSpec.titleHeight + mSpec.separatorLineHeight;
                        }
                    }
                }else{
                    y = 0;
                }
                rect.set(x, y + mMarinTop, x + (mWidth-x-mSlotGapEdge), y + mSpec.titleHeight + mMarinTop);
            }
            return rect;
        }
		
        public Rect getSlotRect(int index, Rect rect) {
            int x = 0;
            int y = 0;
			int timeslotNum = 0;
			int size = mTimeslotSize.size();
            if(mSpec.titleHeight > -1 && size > 0){
                int rows = 0;
                int iSize = 0;
                int num = 0;
                int preNum = 0;
                for (int timeNum = 0; timeNum < size ;timeNum++){
                    iSize = mTimeslotSize.get(timeNum);
                    num += iSize;
                    if (index < num){
                        timeslotNum = timeNum + 1;
                        index = index - preNum;
                        break;
                    }
                    preNum = num;
                    
                    if (timeNum == 0){
                        if (iSize < mSpecialCount){
                            rows += 1;
                        }else{
                            rows += 1 + ((iSize-mSpecialCount)/mUnitCount) + (((iSize-mSpecialCount)%mUnitCount) > 0 ? 1 : 0);
                        }
                    }else{
                        rows += ((iSize)/mUnitCount) + (((iSize)%mUnitCount) > 0 ? 1 : 0);
                    }
                }

            	if(timeslotNum <= 1){
                    int firstSize = mTimeslotSize.get(0);
                    int left = 0;
                    int top = 0;
                    int right = 0;
                    int bottom = 0;
                    if (index >= mSpecialCount){
                        index += mUnitCount*mSpec.areaVSize - mSpecialCount;
                    }else{
                        if (index == 0){
                            left = mSlotGapEdge;
                            top = mSpec.titleHeight;
                            right = left + mSpec.areaHSize*mSlotWidth + (mSpec.areaHSize-1)*mSlotGap;
                            bottom = top + mSpec.areaVSize*mSlotHeight + (mSpec.areaVSize-1)*mSlotGap;
                        }else if (index == 1 && firstSize == 2){
                            left = mSlotGapEdge + mSpec.areaHSize*(mSlotWidth + mSlotGap);
                            top = mSpec.titleHeight;
                            right = left + mSpec.areaHSize*mSlotWidth + (mSpec.areaHSize-1)*mSlotGap;
                            bottom = top + mSpec.areaVSize*mSlotHeight + (mSpec.areaVSize-1)*mSlotGap;
                        }else{
                            int remaincount = mUnitCount - mSpec.areaHSize;
                            index -= 1;
                            int hRow = index/remaincount;
                            int vRow = index%remaincount;
                            left = mSlotGapEdge + mSpec.areaHSize*(mSlotWidth + mSlotGap) + vRow*(mSlotWidth+mSlotGap);
                            top = mSpec.titleHeight + hRow*(mSlotHeight+mSlotGap);
                    	    right = left + mSlotWidth;
                            bottom = top + mSlotHeight;
                        }
                        //taoxj modify
                        rect.set(left, top + mMarinTop, right, bottom + mMarinTop);
                        return rect;
                    }
            	}else{
                    index = index + mSpec.areaVSize*mUnitCount + (rows-1)* mUnitCount;
            	}
            }

            int col, row;
            if (WIDE) {
                col = index / mUnitCount;
                row = index - col * mUnitCount;
            } else {
                row = index / mUnitCount;
                col = index - row * mUnitCount;
            }

		    if (size > 0 && mSpec.titleHeight > -1){
            	x = col * (mSlotWidth + mSlotGap) + mSlotGapEdge;
            	y = row * (mSlotHeight + mSlotGap) + timeslotNum*mSpec.titleHeight + timeslotNum*mSpec.separatorLineHeight;
            }else{
                x = col * (mSlotWidth + mSlotGap) + mSlotGapEdge;
                y = row * (mSlotHeight + mSlotGap);
            }
            rect.set(x, y + mMarinTop, x + mSlotWidth, y + mSlotHeight + mMarinTop);
            return rect;
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
            Log.i("koala","unitCount =" + unitCount);
            if (unitCount == 0) unitCount = 1;
            mUnitCount = unitCount;
           if (mSpec.areaHSize > mUnitCount){
                mSpec.areaHSize = mUnitCount;
            }
            mSpecialCount = mUnitCount*mSpec.areaVSize - mSpec.areaVSize*mSpec.areaHSize + 1;
            if (mSpecialCount < 0){
                mSpecialCount = 0;
            }
            // We put extra padding above and below the column.
            int availableUnits = Math.min(mUnitCount, mSlotCount);
            int usedMinorLength = availableUnits * minorUnitSize +
                    (availableUnits - 1) * mSlotGap;
            padding[0] = (minorLength - usedMinorLength) / 2;

            // Then calculate how many columns we need for all slots.
            int size = mTimeslotSize.size();
            if(mLayout.mSpec.titleHeight > -1 && size > 0){
                mContentLength = 0;
                int row = 0;
                int num = 0;
                for (int timeNum = 0; timeNum < size; timeNum++){
                    num = mTimeslotSize.get(timeNum);
                    mContentLength += mSpec.titleHeight;
                    if (timeNum == 0){
                        if (num < mSpecialCount){
                            row = 1;
                        }else{
                            row = 1 + ((num-mSpecialCount) / mUnitCount + (((num-mSpecialCount) % mUnitCount) > 0 ? 1 : 0));
                        }
                        //row = 1 + ((num-1) / mUnitCount + (((num-1) % mUnitCount) > 0 ? 1 : 0));
                        mContentLength += (majorUnitSize + mSlotGap) * 2 + (row -1) * (majorUnitSize + mSlotGap);
                    }else{
                        row = num / mUnitCount + ((num % mUnitCount) > 0 ? 1 : 0);
                        mContentLength += row * (majorUnitSize + mSlotGap);
                    }
                    mContentLength += mSpec.separatorLineHeight; //taoxj add for separator line
                }
                mContentLength -= mSlotGap;
                mContentLength -= mSpec.separatorLineHeight; //taoxj add for separator line
                mContentLength += mMarinTop + mMarinBottom;
            }else{
                int count = ((mSlotCount + mUnitCount - 1) / mUnitCount);
                mContentLength = count * majorUnitSize + (count - 1) * mSlotGap;
            }

            // If the content length is less then the screen width, put
            // extra padding in left and right.
            padding[1] = Math.max(0, (majorLength - mContentLength) / 2);
        }

        private void initLayoutParameters() {
            // Initialize mSlotWidth and mSlotHeight from mSpec
            if (mSpec.slotWidth != -1) {
                Log.i("koala","use special slot width");
                mMarinTop = mSpec.marinTop;
                mMarinBottom = mSpec.marinBottom;
                int rows = (mWidth > mHeight) ? mSpec.rowsLand : mSpec.rowsPort;
                mSlotWidth = mSpec.slotWidth;
                mSlotHeight = mSpec.slotHeight;
                mSlotGapEdge = mSpec.slotGapEdge;
                mSlotGap = (mWidth - 2 * mSlotGapEdge - rows * mSlotWidth) / (rows - 1);
            } else {
                mMarinTop = mSpec.marinTop;
                mMarinBottom = mSpec.marinBottom;
                int rows = (mWidth > mHeight) ? mSpec.rowsLand : mSpec.rowsPort;
                mSlotGap = mSpec.slotGap;
            	mSlotGapEdge = mSpec.slotGapEdge;
            	mSlotWidth = Math.max(1, (mWidth - (rows - 1) * mSlotGap - 2 * mSlotGapEdge) / rows);
            	mSlotHeight = mSlotWidth - mSpec.slotHeightAdditional;
            }

            if (mRenderer != null) {
                if(mSpec.titleHeight > -1){
                    int titleWidth = Math.max(0, mWidth - 2 * mSlotGapEdge);
                    int titleHeight = Math.max(0, mSpec.titleHeight);
                    mRenderer.onTitleSizeChanged(titleWidth, titleHeight);
                }
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
                setVisibleRange(start, end, 0, 0);
            } else {
				int size = mTimeslotSize.size();
            	if(mSpec.titleHeight > -1 && size > 0){
                    int tHeight = mHeight - mMarinTop - mMarinBottom;
                    int num = 0;
                    int start = 0;
                    int end = 0;
                    int startRow = 0;
                    int endRow = 0;
                    int startTimeslot = 0;
                    int endTimeslot = 0;
                    int endTimeslotRow = 0;
                    int timeTitleHeights = 0;
                    int timeNumHeights = 0;
                    int rowIdx = 0;
                    for (int timeNum = 0; timeNum < size; timeNum++){
                        num = mTimeslotSize.get(timeNum);
                        int offsetNum = 0;
                        int residues = num % mUnitCount;
                        int row = num / mUnitCount + (residues > 0 ? 1 : 0);
                        if (timeNum == 0){
                            if (num < mSpecialCount){
                                offsetNum = num;
                                residues = 0;
                                row = 1;
                            }else{
                                offsetNum = mSpecialCount;
                                residues = (num-mSpecialCount) % mUnitCount;
                                row = 1 + ((num-mSpecialCount) / mUnitCount + (residues > 0 ? 1 : 0));
                            }
                        }

                        timeTitleHeights = mSpec.titleHeight + timeNumHeights;
                        if (position <= timeTitleHeights){
                            startTimeslot = timeNum;
                            endTimeslotRow = startTimeslot;
                            
                            int willHeight = timeTitleHeights - position;
                            if (willHeight >= tHeight){
                                endTimeslot = endTimeslotRow + 1;
                                start = end = 0;
                            }else{
                                start += startRow;
                                endRow = start;
                                
                                int titleAndSlotheights = timeTitleHeights;
                                int slotAndTitleheights = timeNumHeights;
                                for (; timeNum < size; timeNum++){
                                    int num2 = mTimeslotSize.get(timeNum);
                                    int offsetNum2 = 0;
                                    int residues2 = num2 % mUnitCount;
                                    int row2 = num2 / mUnitCount + (residues2 > 0 ? 1 : 0);
                                    if (timeNum == 0){
                                        if (num2 < mSpecialCount){
                                            offsetNum2 = num2;
                                            residues2 = 0;
                                            row2 = 1;
                                        }else{
                                            offsetNum2 = mSpecialCount;
                                            residues2 = (num2-mSpecialCount) % mUnitCount;
                                            row2 = 1 + ((num2-mSpecialCount) / mUnitCount + (residues2 > 0 ? 1 : 0));
                                        }
                                    }
                                    
                                    slotAndTitleheights = titleAndSlotheights 
                                        + ((timeNum == 0) ? (mSpec.areaVSize-1)*(mSlotHeight + mSlotGap) : 0) 
                                        + row2 * (mSlotHeight + mSlotGap);
                                    if (slotAndTitleheights - position >= tHeight){
                                        rowIdx = (position + tHeight - titleAndSlotheights)/(mSlotHeight + mSlotGap);
                                        if (timeNum == 0){
                                            if (rowIdx < mSpec.areaVSize){
                                                end = endRow + offsetNum2;
                                            }else{
                                                endRow += offsetNum2;
                                                if (rowIdx >= row2 && residues2 > 0){
                                                    rowIdx = rowIdx - mSpec.areaVSize;
                                                    end = endRow + rowIdx * mUnitCount + residues2;
                                                }else{
                                                    rowIdx = rowIdx - mSpec.areaVSize;
                                                    end = endRow + (rowIdx + 1) * mUnitCount;
                                                }
                                            }
                                        }else{
                                            if ((rowIdx + 1) >= row2 && residues2 > 0){
                                                end = endRow + rowIdx * mUnitCount + residues2;
                                            }else{
                                                end = endRow + (rowIdx + 1) * mUnitCount;
                                            }
                                        }
                                        endTimeslot = endTimeslotRow + 1;                                        
                                        break;
                                    }
                                    
                                    endRow += num2;
                                    
                                    if (timeNum + 1 == size){
                                        endTimeslot = endTimeslotRow + 1;
                                        end = endRow;
                                        break;
                                    }
                                    
                                    endTimeslotRow += 1;
                                    
                                    titleAndSlotheights = mSpec.titleHeight + slotAndTitleheights;
                                    if (titleAndSlotheights - position >= tHeight){
                                        endTimeslot = endTimeslotRow + 1;  
                                        end = endRow;
                                        break;
                                    }
                                }
                            }
                            break;
                        }
                        
                        timeNumHeights = timeTitleHeights 
                            + ((timeNum == 0) ? (mSpec.areaVSize-1)*(mSlotHeight + mSlotGap) : 0) 
                            + row * (mSlotHeight + mSlotGap);
                        if (position <= timeNumHeights){
                            rowIdx = (position - timeTitleHeights)/(mSlotHeight + mSlotGap);
                            if (timeNum == 0){
                                if (rowIdx < mSpec.areaVSize){
                                    start = startRow;
                                }else{
                                    start = startRow + (rowIdx - mSpec.areaVSize) * mUnitCount + offsetNum;
                                }
                            }else{
                                start = startRow + rowIdx * mUnitCount;
                            }
                            endRow = startRow;
                            
                            int willHeight = timeNumHeights - position;
                            if (willHeight >= tHeight){
                                rowIdx = (position - timeTitleHeights + tHeight )/(mSlotHeight + mSlotGap);
                                if (timeNum == 0){
                                    if (rowIdx < mSpec.areaVSize){
                                        end = endRow + offsetNum;
                                    }else{
                                        endRow += offsetNum;
                                        if (rowIdx >= row && residues > 0){
                                            rowIdx = rowIdx - mSpec.areaVSize;
                                            end = endRow + rowIdx * mUnitCount + residues;
                                        }else{
                                            rowIdx = rowIdx - mSpec.areaVSize;
                                            end = endRow + (rowIdx + 1) * mUnitCount;
                                        }
                                    }
                                }else{
                                    if ((rowIdx + 1) >= row && residues > 0){
                                        end = endRow + rowIdx * mUnitCount + residues;
                                    }else{
                                        end = endRow + (rowIdx + 1) * mUnitCount;
                                    }
                                }
                                startTimeslot = endTimeslot = 0;
                            }else{
                                 if (timeNum + 1 == size){
                                     startTimeslot = endTimeslot = 0;
                                     end = endRow + num;
                                     break;
                                 }
                                 timeNum++;
                                 endRow += num;
                                 startTimeslot = timeNum;
                                 endTimeslotRow = startTimeslot;
                                 
                                 int titleAndSlotheights = timeTitleHeights;
                                 int slotAndTitleheights = timeNumHeights;
                                 for (; timeNum < size; timeNum++){
                                     int num2 = mTimeslotSize.get(timeNum);
                                     int offsetNum2 = 0;
                                     int residues2 = num2 % mUnitCount;
                                     int row2 = num2 / mUnitCount + (residues2 > 0 ? 1 : 0);
                                     if (timeNum == 0){
                                         if (num2 < mSpecialCount){
                                             offsetNum2 = num2;
                                             residues2 = 0;
                                             row2 = 1;
                                         }else{
                                             offsetNum2 = mSpecialCount;
                                             residues2 = (num2-mSpecialCount) % mUnitCount;
                                             row2 = 1 + ((num2-mSpecialCount) / mUnitCount + (residues2 > 0 ? 1 : 0));
                                         }
                                     }
                                                                              
                                     titleAndSlotheights = mSpec.titleHeight + slotAndTitleheights;
                                     if (titleAndSlotheights - position >= tHeight){
                                         endTimeslot = endTimeslotRow + 1;  
                                         end = endRow;
                                         break;
                                     }
                                     
                                     slotAndTitleheights = titleAndSlotheights 
                                        + ((timeNum == 0) ? (mSpec.areaVSize-1)*(mSlotHeight + mSlotGap) : 0) 
                                        + row2 * (mSlotHeight + mSlotGap);
                                     if (slotAndTitleheights - position >= tHeight){
                                         rowIdx = (position + tHeight - titleAndSlotheights)/(mSlotHeight + mSlotGap);
                                         if (timeNum == 0){
                                             if (rowIdx < mSpec.areaVSize){
                                                 end = endRow + offsetNum2;
                                             }else{
                                                 endRow += offsetNum2;
                                                 if (rowIdx >= row2 && residues2 > 0){
                                                     rowIdx = rowIdx - mSpec.areaVSize;
                                                     end = endRow + rowIdx * mUnitCount + residues2;
                                                 }else{
                                                     rowIdx = rowIdx - mSpec.areaVSize;
                                                     end = endRow + (rowIdx + 1) * mUnitCount;
                                                 }
                                             }
                                         }else{
                                             if ((rowIdx + 1) >= row2 && residues2 > 0){
                                                 end = endRow + rowIdx * mUnitCount + residues2;
                                             }else{
                                                 end = endRow + (rowIdx + 1) * mUnitCount;
                                             }
                                         }
                                         endTimeslot = endTimeslotRow + 1;                                        
                                         break;
                                     }
                                     endRow += num2;
                                     endTimeslotRow += 1;
                                     
                                     if (timeNum + 1 == size){
                                         endTimeslot = endTimeslotRow;  
                                         end = endRow;
                                         break;
                                     }
                                 }
                            }
                            break;
                        }
                        startRow += num;
                    }
                    end = Math.min(mSlotCount, end);
                    setVisibleRange(start, end, startTimeslot, endTimeslot);
        	    }else{
                    int startRow = position / (mSlotHeight + mSlotGap);
                    int start = Math.max(0, mUnitCount * startRow);
                    int endRow = (position + mHeight + mSlotHeight + mSlotGap - 1) /
                            (mSlotHeight + mSlotGap);
                    int end = Math.min(mSlotCount, mUnitCount * endRow);
                    setVisibleRange(start, end, 0, 0);
            	}
            }
        }

        public void setScrollPosition(int position) {
            if (mScrollPosition == position) return;
            mScrollPosition = position;
            updateVisibleSlotRange();
        }

		private void setVisibleRange(int start, int end, int startTimeslot, int endTimeslot) {
		    if(mLayout.mSpec.titleHeight > -1){
                if (start == mVisibleStart && end == mVisibleEnd
                    && startTimeslot == mVisibleStartTimeslot && endTimeslot == mVisibleEndTimeslot){
                    return;
                }
            
                if (start < end) {
                    mVisibleStart = start;
                    mVisibleEnd = end;
                } else {
                    mVisibleStart = mVisibleEnd = 0;
                }
            
                if (startTimeslot < endTimeslot) {
                    mVisibleStartTimeslot = startTimeslot;
                    mVisibleEndTimeslot = endTimeslot;
                } else {
                    mVisibleStartTimeslot = mVisibleEndTimeslot = 0;
                }
            
                if (mRenderer != null) {
                    mRenderer.onVisibleRangeChanged(mVisibleStart, mVisibleEnd, mVisibleStartTimeslot, mVisibleEndTimeslot);
                }
            }else{
                if (start == mVisibleStart && end == mVisibleEnd) return;
                if (start < end) {
                    mVisibleStart = start;
                    mVisibleEnd = end;
                } else {
                    mVisibleStart = mVisibleEnd = 0;
                }
                if (mRenderer != null) {
    				mRenderer.onVisibleRangeChanged(mVisibleStart, mVisibleEnd, mVisibleStartTimeslot, mVisibleEndTimeslot);
                }
            }
        }

        public int getVisibleStart() {
            return mVisibleStart;
        }

        public int getVisibleEnd() {
            return mVisibleEnd;
        }
        
        public int getVisibleStartTimeslot() {
            return mVisibleStartTimeslot;
        }

        public int getVisibleEndTimeslot() {
            return mVisibleEndTimeslot;
        }

        public int getSlotIndexByPosition(float x, float y) {
            int index = INDEX_NONE;
            int absoluteX = Math.round(x) + (WIDE ? mScrollPosition : 0);
            int absoluteY = Math.round(y) + (WIDE ? 0 : mScrollPosition) - mMarinTop;

			int size = mTimeslotSize.size();
            if(mLayout.mSpec.titleHeight > -1 && size > 0){
                if (absoluteX < 0 || absoluteY < 0) {
                    return INDEX_NONE;
                }
                
                int timeTitleHeights = 0;
                int timeNumHeights = 0;
                int num = 0;
                index = 0;
                for (int timeNum = 0; timeNum < size ;timeNum++){
                    num = mTimeslotSize.get(timeNum);
                    int offsetNum = 0;
                    int residues = num % mUnitCount;
                    int row = num / mUnitCount + (residues > 0 ? 1 : 0);
                    if (timeNum == 0){
                        if (num < mSpecialCount){
                            offsetNum = num;
                            residues = 0;
                            row = 1;
                        }else{
                            offsetNum = mSpecialCount;
                            residues = (num-mSpecialCount) % mUnitCount;
                            row = 1 + ((num-mSpecialCount) / mUnitCount + (residues > 0 ? 1 : 0));
                        }
                    }

                    timeTitleHeights = mSpec.titleHeight + timeNumHeights;
                    if (absoluteY <= timeTitleHeights){
                        return INDEX_NONE;
                    }
                    
                    timeNumHeights = timeTitleHeights 
                        + ((timeNum == 0) ? (mSpec.areaVSize-1)*(mSlotHeight + mSlotGap) : 0) 
                        + row * (mSlotHeight + mSlotGap);
                    if (absoluteY <= timeNumHeights){
                        int curAbsoluteY = absoluteY - timeTitleHeights;
                        
                        int rowIdx = curAbsoluteY / (mSlotHeight + mSlotGap);
                        int columnIdx = absoluteX / (mSlotWidth + mSlotGap);

                        if (columnIdx >= mUnitCount) {
                            return INDEX_NONE;
                        }

                        if (timeNum == 0){
                            if (rowIdx < mSpec.areaVSize){
                                if (columnIdx < mSpec.areaHSize){
                                    index = 0;
                                }else if (num == 2){
                                    if (columnIdx >= mSpec.areaHSize*2){
                                        return INDEX_NONE;
                                    }
                                    index = 1;
                                }else{
                                    int offsetNum2 = offsetNum-1;
                                    if (offsetNum2 == 0){
                                        return INDEX_NONE;
                                    }
                                    int unitCount = mUnitCount - mSpec.areaHSize;
                                    int hRow = offsetNum2/unitCount;
                                    int vResidues = offsetNum2%unitCount;
                                    int columnIdx2 = columnIdx-mSpec.areaVSize;
                                    
                                    if (columnIdx2 >= unitCount) {
                                        return INDEX_NONE;
                                    }

                                    if (rowIdx >= hRow && vResidues > 0){
                                        if ((columnIdx2 + 1) > vResidues){
                                            return INDEX_NONE;
                                        }
                                    }
                                    index = rowIdx * (mUnitCount - mSpec.areaHSize) + columnIdx2;
                                    if (index >= offsetNum2){
                                        index = INDEX_NONE;
                                    }else{
                                        index += 1;
                                    }
                                }
                                break;
                            }else{
                                if (rowIdx >= row && residues > 0){
                                    if ((columnIdx + 1) > residues){
                                        return INDEX_NONE;
                                    }
                                }
                                rowIdx = rowIdx - mSpec.areaVSize;
                                index += offsetNum;
                            }
                        }

                        if (absoluteX % (mSlotWidth + mSlotGap) >= mSlotWidth) {
                            return INDEX_NONE;
                        }
                        
                        if (curAbsoluteY % (mSlotHeight + mSlotGap) >= mSlotHeight) {
                            return INDEX_NONE;
                        }

                        if ((rowIdx + 1) >= row && residues > 0){
                            if ((columnIdx + 1) > residues){
                                return INDEX_NONE;
                            }
                        }
                        index += rowIdx * mUnitCount + columnIdx;
                        break;
                    }
                    index += num;
                }
            }else{

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

            index = WIDE
                    ? (columnIdx * mUnitCount + rowIdx)
                    : (rowIdx * mUnitCount + columnIdx);
            }
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

        public float getVelocity(float velocity) {
            int limit = (WIDE ? mWidth : mHeight)*3;
            if (velocity > 0 && velocity > limit){
                velocity = limit;
            }else if (velocity < 0 && velocity < -limit){
                velocity = -limit;
            }
            return velocity;
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
            //float velocity = mLayout.getVelocity(WIDE ? velocityX : velocityY);
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
            
            if(mOverscrollEffect == OVERSCROLL_TY){
            	   overDistance = mScroller.startScroll(
                          Math.round(distance),-drag_height, mLayout.getScrollLimit()+drag_height);
            }else{
            	  overDistance = mScroller.startScroll(
                         Math.round(distance),0, mLayout.getScrollLimit());
            }
            if(isOverscrollEffect && mOverscrollEffect == OVERSCROLL_TY){
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

    //taoxj modify
    public boolean setSlotCount(int slotCount, LinkedHashMap<String, Integer> timeslotInfo) {
        boolean changed = mLayout.setSlotCount(slotCount, timeslotInfo);

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
    
    public int getVisibleStartTimeslot() {
        return mLayout.getVisibleStartTimeslot();
    }
    
    public int getVisibleEndTimeslot() {
        return mLayout.getVisibleEndTimeslot();
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
