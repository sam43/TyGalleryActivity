/*
 * TIANYU: yuxin add for New Design Gallery
 */
package com.android.gallery3d.ui;

import com.android.gallery3d.app.GalleryContext;
import com.android.gallery3d.app.TyAlbumTimeDataLoader;
import com.android.gallery3d.data.MediaObject;
import com.android.gallery3d.data.Path;
import com.android.gallery3d.glrenderer.ColorTexture;
import com.android.gallery3d.glrenderer.FadeInTexture;
import com.android.gallery3d.glrenderer.GLCanvas;
import com.android.gallery3d.glrenderer.Texture;
import com.android.gallery3d.glrenderer.TiledTexture;
import java.util.LinkedHashMap;
//taoxj add 
import android.graphics.Color;

public class TyAlbumTimeSlotRenderer extends TyAbstractSlotRenderer {
    @SuppressWarnings("unused")
    private static final String TAG = "Gallery2/TyAlbumTimeSlotRenderer";

    /*use to interface SlotFilter in AlbumSlotRenderer*/
    //public interface SlotFilter {
    //    public boolean acceptSlot(int index);
    //}
    
    private final int mPlaceholderTitleColor;
    private final int mPlaceholderColor;
    private static final int CACHE_SIZE = 96;

    private TyAlbumTimeSlidingWindow mDataWindow;
    private final GalleryContext mActivity;
    private final ColorTexture mWaitLoadingTitleTexture;
    private final ColorTexture mWaitLoadingTexture;
    private final TySlotView mSlotView;
    private final SelectionManager mSelectionManager;

    private int mPressedIndex = -1;
    private boolean mAnimatePressedUp;
    private Path mHighlightItemPath = null;
    private boolean mInSelectionMode;

    private AlbumSlotRenderer.SlotFilter mSlotFilter;

    public static class TitleSpec {
        public int titleBackgroundHeight;
        public int titleFontSize;
        public int countFontSize;
        public int fontBottomMargin;
        public int fontTopMargin;
        public int fontCountBottomMargin;
        public int titleLeftMargin;
        public int titleRightMargin;
        public int backgroundColor;
        public int titleColor;
        public int countColor;
        public TySlotView slotView;
    }
    protected final TitleSpec mTitleSpec;

    public TyAlbumTimeSlotRenderer(GalleryContext activity, TySlotView slotView,
            SelectionManager selectionManager, TitleSpec titleSpec, int placeholderColor,int placeholderTitleColor) {
        super(activity);
        mActivity = activity;
        mSlotView = slotView;
        mSelectionManager = selectionManager;
        mTitleSpec = titleSpec;
        if (mTitleSpec != null){
            mTitleSpec.slotView = slotView;
        }
        mPlaceholderTitleColor = placeholderTitleColor;
        mPlaceholderColor = placeholderColor;

        mWaitLoadingTitleTexture = new ColorTexture(mPlaceholderTitleColor);
        mWaitLoadingTitleTexture.setSize(1, 1);

        mWaitLoadingTexture = new ColorTexture(mPlaceholderColor);
        mWaitLoadingTexture.setSize(1, 1);
    }

    public void setPressedIndex(int index) {
        if (mPressedIndex == index) return;
        mPressedIndex = index;
        mSlotView.invalidate();
    }

    public void setPressedUp() {
        if (mPressedIndex == -1) return;
        mAnimatePressedUp = true;
        mSlotView.invalidate();
    }

    public void setHighlightItemPath(Path path) {
        if (mHighlightItemPath == path) return;
        mHighlightItemPath = path;
        mSlotView.invalidate();
    }

    public void setModel(TyAlbumTimeDataLoader model) {
        if (mDataWindow != null) {
            mDataWindow.setListener(null);
            mSlotView.setSlotCount(0, null);
            mDataWindow = null;
        }
        if (model != null) {
            mDataWindow = new TyAlbumTimeSlidingWindow(mActivity, model, mTitleSpec, CACHE_SIZE);
            mDataWindow.setListener(new MyDataModelListener());
            mSlotView.setSlotCount(model.size(), model.timeslotInfo());
            //TYRD:changjj add for get Slot Pos begin
            mSlotView.setSlotPosInterface(mDataWindow);
            //TYRD:changjj add for get Slot Pos end

        }
    }

    private static Texture checkTexture(Texture texture) {
        return (texture instanceof TiledTexture)
                && !((TiledTexture) texture).isReady()
                ? null
                : texture;
    }


    @Override
    public int renderTitle(GLCanvas canvas, int titleIndex, int pass, int width, int height){
        TyAlbumTimeSlidingWindow.AlbumTitleEntry titlEntry = mDataWindow.getTitle(titleIndex);
        if (titlEntry == null){
            return 0;
        }
        Texture content = checkTexture(titlEntry.titleContent);
        if (content == null) {
            content = mWaitLoadingTitleTexture;
        }
        content.draw(canvas, 0, 0, width, height);
        return 0;
    }

    @Override
    public int renderSlot(GLCanvas canvas, int index, int pass, int width, int height) {
        if (mSlotFilter != null && !mSlotFilter.acceptSlot(index)) return 0;

        TyAlbumTimeSlidingWindow.AlbumEntry entry = mDataWindow.get(index);
        if (entry == null){
            return 0;
        }

        int renderRequestFlags = 0;

        Texture content = checkTexture(entry.content);
        if (content == null) {
            content = mWaitLoadingTexture;
            entry.isWaitDisplayed = true;
        } else if (entry.isWaitDisplayed) {
            entry.isWaitDisplayed = false;
            content = new FadeInTexture(mPlaceholderColor, entry.bitmapTexture);
            entry.content = content;
        }
        drawContent(canvas, content, width, height, entry.rotation);
        if ((content instanceof FadeInTexture) &&
                ((FadeInTexture) content).isAnimating()) {
            renderRequestFlags |= TySlotView.RENDER_MORE_FRAME;
        }
        
        if (entry.mediaType == MediaObject.MEDIA_TYPE_VIDEO) {
            //if(index == 0){
            //    tyDrawFirstVideoOverlay(canvas, width, height);
            //}else{
                tyDrawVideoOverlay(canvas, width, height);

        }

        if (entry.isPanorama) {
            drawPanoramaIcon(canvas, width, height);
        }

        renderRequestFlags |= renderOverlay(canvas, index, entry, width, height);

        return renderRequestFlags;
    }

    private int renderOverlay(GLCanvas canvas, int index,
            TyAlbumTimeSlidingWindow.AlbumEntry entry, int width, int height) {
        int renderRequestFlags = 0;
        if (mPressedIndex == index) {
            if (mAnimatePressedUp) {
                drawPressedUpFrame(canvas, width, height);
                renderRequestFlags |= TySlotView.RENDER_MORE_FRAME;
                if (isPressedUpFrameFinished()) {
                    mAnimatePressedUp = false;
                    mPressedIndex = -1;
                }
            } else {
                drawPressedFrame(canvas, width, height);
            }
            if (mInSelectionMode){
                 if (mSelectionManager.isItemSelected(entry.path)){
                     drawSelectedFrame(canvas, width, height);
                 }
                 /*else{
                     drawSelectionMode(canvas, width, height);
                 }*/
            }
        } else if ((entry.path != null) && (mHighlightItemPath == entry.path)) {
            drawSelectedFrame(canvas, width, height);
        } else if (mInSelectionMode && mSelectionManager.isItemSelected(entry.path)) {
            drawSelectedFrame(canvas, width, height);
        }
        /*else if (mInSelectionMode ) {
        	drawSelectionMode(canvas, width, height);
        }*/
        return renderRequestFlags;
    }

    private class MyDataModelListener implements TyAlbumTimeSlidingWindow.Listener {
        @Override
        public void onContentChanged() {
            mSlotView.invalidate();
        }

        @Override
        public void onSizeChanged(int size, LinkedHashMap<String, Integer> timeslotInfo) { //taoxj modify
            mSlotView.setSlotCount(size, timeslotInfo);
            mSlotView.invalidate();
        }

        //TYRD:changjj add for delete anamition begin
        @Override
        public void onContentLoadFinished(){
            //taoxj remove
            //mSlotView.startTranslatingAnimation();
        }
        //TYRD:changjj add for delete anamition end
    }

    public void resume() {
        mDataWindow.resume();
    }

    public void pause() {
        mDataWindow.pause();
    }

    @Override
    public void prepareDrawing() {
        mInSelectionMode = mSelectionManager.inSelectionMode();
    }

    @Override
    public void onVisibleRangeChanged(int visibleStart, int visibleEnd, int visibleStartTimeslot, int visibleEndTimeslot) {
        if (mDataWindow != null) {
            mDataWindow.setActiveWindow(visibleStart, visibleEnd, visibleStartTimeslot, visibleEndTimeslot);
        }
    }

    @Override
    public void onTitleSizeChanged(int width, int height){
        if (mDataWindow != null) {
            mDataWindow.onTitleSizeChanged(width, height);
        }
    }

     //taoxj add for separator line begin
     @Override
    public void onSeparatorLineSizeChanged(int width, int height){
       
    }


     @Override
    public int renderSeparatorLine(GLCanvas canvas, int titleIndex, int pass, int width, int height){
         
        Texture content = new ColorTexture(Color.GRAY);
        content.draw(canvas, 0, 0, width, height);
        return 0;
    }
    
    //taoxj add for separator line end

    @Override
    public void onSlotSizeChanged(int width, int height) {
        // Do nothing
    }

    public void setSlotFilter(AlbumSlotRenderer.SlotFilter slotFilter) {
        mSlotFilter = slotFilter;
    }

    public void setReserveData(boolean bReserveData){
        if (mDataWindow != null) {
            mDataWindow.setReserveData(bReserveData);
        }
    }
    
    public boolean getReserveData(){
        boolean reserveData = false;
        if (mDataWindow != null) {
            reserveData = mDataWindow.getReserveData();
        }
        return reserveData;
    }
}
