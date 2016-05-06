/*
 * TIANYU: yuxin add for New Design Gallery
 */
package com.android.gallery3d.ui;

import android.graphics.Bitmap;
import android.os.Message;

import com.android.gallery3d.app.GalleryContext;
import com.android.gallery3d.app.TyAlbumTimeDataLoader;
import com.android.gallery3d.common.Utils;
import com.android.gallery3d.data.MediaItem;
import com.android.gallery3d.data.MediaObject;
import com.android.gallery3d.data.MediaObject.PanoramaSupportCallback;
import com.android.gallery3d.data.Path;
import com.android.gallery3d.glrenderer.Texture;
import com.android.gallery3d.glrenderer.TiledTexture;
import com.android.gallery3d.glrenderer.BasicTexture;
import com.android.gallery3d.util.Future;
import com.android.gallery3d.util.FutureListener;
import com.android.gallery3d.util.JobLimiter;

import android.util.Log;

import java.util.LinkedHashMap;
import java.util.Map;
import android.graphics.Rect;
import com.android.gallery3d.R;

public class TyAlbumTimeSlidingWindow implements TyAlbumTimeDataLoader.DataListener ,TySlotView.SlotPos {
    @SuppressWarnings("unused")
    private static final String TAG = "Gallery2/TyAlbumTimeSlidingWindow";

    //TYRD:changjj add for bind texture to slot render
    private static final int SLOT_UNINIT = 0;
    private static final int SLOT_INITED = 1;
    private static final int SLOT_USED = 2;
    private static final int SLOT_UNUSED = 3;

    private boolean mIsInitLoaded = false;
    private boolean mAnamitionPlaying = false;
    //TYRD:changjj add for bind texture to slot render end    

    private static final int MSG_UPDATE_ENTRY = 0;
    private static final int MSG_UPDATE_ALBUM_TITLE = 1;
    private static final int JOB_LIMIT = 2;

    public static interface Listener {
        //taoxj modify
        public void onSizeChanged(int size, LinkedHashMap<String, Integer> timeslotInfo);
        public void onContentChanged();
        //TYRD:changjj add for delete function begin
        public void onContentLoadFinished();
        //TYRD:changjj add for delete function end
    }

    public static class AlbumTitleEntry {
        public TiledTexture titleTexture;
        public Texture titleContent;
        private BitmapLoader titleLoader;
    }

    public static class AlbumEntry {
        public MediaItem item;
        public Path path;
        public boolean isPanorama;
        public int rotation;
        public int mediaType;
        public boolean isWaitDisplayed;
        public TiledTexture bitmapTexture;
        public Texture content;
        private BitmapLoader contentLoader;
        private PanoSupportListener mPanoSupportListener;
        //TYRD changjj add for load data;
        public  int  useStatus;
        public  int  operatorCount;
        public  int  oldPos;
        public  int  newPos;
        //TYRD:changjj add for anamition end
    }

    private final TyAlbumTimeDataLoader mSource;
    private final AlbumEntry mData[];
    private final SynchronizedHandler mHandler;
    private final JobLimiter mThreadPool;
    private final TiledTexture.Uploader mTileUploader;
    private final TiledTexture.Resources mTiledResources;

    private int mSize;

    private int mContentStart = 0;
    private int mContentEnd = 0;

    private int mActiveStart = 0;
    private int mActiveEnd = 0;

    private Listener mListener;
    //TYRD:changjj add delete function begin
    private final AlbumEntry mBackData[];

    private int mBackDataStart = 0;
    private int mBackDataEnd = 0;
    private boolean mEdieMode = false;
    //TYRD:changjj add delete function  end

    private int mActiveRequestCount = 0;
    private boolean mIsActive = false;
    private TyAlbumTitleMaker mTitleMaker;
    //taoxj modify
    private LinkedHashMap<String, Integer> mTimeslotInfo;
    private AlbumTitleEntry mTitleData[];
    private int mTitleContentStart = 0;
    private int mTitleContentEnd = 0;
    private int mTitleActiveStart = 0;
    private int mTitleActiveEnd = 0;

    private int mTitleWidth;
    protected TyAlbumTimeSlotRenderer.TitleSpec mTitleSpec;
    private GalleryContext mGalleryContext;
    private boolean mReserveData;

    private class PanoSupportListener implements PanoramaSupportCallback {
        public final AlbumEntry mEntry;
        public PanoSupportListener (AlbumEntry entry) {
            mEntry = entry;
        }
        @Override
        public void panoramaInfoAvailable(MediaObject mediaObject, boolean isPanorama,
                boolean isPanorama360) {
            if (mEntry != null) mEntry.isPanorama = isPanorama;
        }
    }

    public TyAlbumTimeSlidingWindow(GalleryContext activity,
            TyAlbumTimeDataLoader source, TyAlbumTimeSlotRenderer.TitleSpec titleSpec, int cacheSize) {
        mGalleryContext = activity;
        source.setDataListener(this);
        mSource = source;
        mData = new AlbumEntry[cacheSize];
        mSize = source.size();

        mTitleSpec = titleSpec;
        if (mTitleSpec != null){
            mTitleMaker = new TyAlbumTitleMaker(activity.getAndroidContext(), mTitleSpec);
            mTitleData = new AlbumTitleEntry[8];
            mTimeslotInfo = source.timeslotInfo();
        }

        mHandler = new SynchronizedHandler(activity.getGLRoot()) {
            @Override
            public void handleMessage(Message message) {
                if(mTitleSpec != null){
                    Utils.assertTrue(message.what == MSG_UPDATE_ENTRY || message.what == MSG_UPDATE_ALBUM_TITLE);
                    if (message.what == MSG_UPDATE_ENTRY){
                        ((ThumbnailLoader) message.obj).updateEntry();
                    }else{
                        ((TyAlbumTitleLoader) message.obj).updateEntry();
                    }
                }else{
                    Utils.assertTrue(message.what == MSG_UPDATE_ENTRY);
                    ((ThumbnailLoader) message.obj).updateEntry();
                }
            }
        };

        Log.i(TAG, "TyAlbumTimeSlidingWindow new joblimiter num " + JOB_LIMIT);
        mThreadPool = new JobLimiter(activity.getThreadPool(), JOB_LIMIT);
        mTileUploader = new TiledTexture.Uploader(activity.getGLRoot());
        mTiledResources = new TiledTexture.Resources();
        
        //TYRD:changjj add begin
        mBackData =  new AlbumEntry[cacheSize * 2];
        //TYRD:changjj add end        
    }

    public void setListener(Listener listener) {
        mListener = listener;
    }

    public AlbumEntry get(int slotIndex) {
        if (!isActiveSlot(slotIndex)) {
            Utils.fail("invalid slot: %s outsides (%s, %s)",
                    slotIndex, mActiveStart, mActiveEnd);
        }
        return mData[slotIndex % mData.length];
    }

    public boolean isActiveSlot(int slotIndex) {
        return slotIndex >= mActiveStart && slotIndex < mActiveEnd;
    }

    private void setContentWindow(int contentStart, int contentEnd) {
        if (contentStart == mContentStart && contentEnd == mContentEnd) return;

        if (!mIsActive && !mReserveData) {
            mContentStart = contentStart;
            mContentEnd = contentEnd;
            mSource.setActiveWindow(contentStart, contentEnd);
            return;
        }
  
        //TYRD:changjj modify for delete animation function begin
        if(mEdieMode){
            BackCopyDataWindow(mContentStart,mContentEnd);
            if (contentStart >= mContentEnd || mContentStart >= contentEnd) {
                for (int i = mContentStart, n = mContentEnd; i < n; ++i) {
                    freeSlotContentCache(i);
                }
                mSource.setActiveWindow(contentStart, contentEnd);
                for (int i = contentStart; i < contentEnd; ++i) {
                    prepareSlotContentCache(i);
                }
            } else {
                for (int i = mContentStart; i < contentStart; ++i) {
                freeSlotContentCache(i);
                }
                for (int i = contentEnd, n = mContentEnd; i < n; ++i) {
                    freeSlotContentCache(i);
                }
                mSource.setActiveWindow(contentStart, contentEnd);
                for (int i = contentStart, n = mContentStart; i < n; ++i) {
                    prepareSlotContentCache(i);
                }
                for (int i = mContentEnd; i < contentEnd; ++i) {
                    prepareSlotContentCache(i);
                }
            }
        }else{
            if (contentStart >= mContentEnd || mContentStart >= contentEnd) {
                for (int i = mContentStart, n = mContentEnd; i < n; ++i) {
                    freeSlotContent(i);
                }
                mSource.setActiveWindow(contentStart, contentEnd);
                for (int i = contentStart; i < contentEnd; ++i) {
                    prepareSlotContent(i);
                }
            } else {
                for (int i = mContentStart; i < contentStart; ++i) {
                    freeSlotContent(i);
                }
                for (int i = contentEnd, n = mContentEnd; i < n; ++i) {
                    freeSlotContent(i);
                }
                mSource.setActiveWindow(contentStart, contentEnd);
                for (int i = contentStart, n = mContentStart; i < n; ++i) {
                    prepareSlotContent(i);
                }
                for (int i = mContentEnd; i < contentEnd; ++i) {
                    prepareSlotContent(i);
                }
            }
        }
        //TYRD:changjj modify for delete animation function end

        mContentStart = contentStart;
        mContentEnd = contentEnd;
    }

    public void setActiveWindow(int start, int end, int startTimeslot, int endTimeslot) {
        if (!(start <= end && end - start <= mData.length && end <= mSize)) {
            Utils.fail("%s, %s, %s, %s", start, end, mData.length, mSize);
        }

        if(mTitleSpec != null){
            if (!(startTimeslot <= endTimeslot && endTimeslot - startTimeslot <= mTitleData.length && endTimeslot <= timeSlotSize())) {
                Utils.fail("%s, %s, %s, %s", startTimeslot, endTimeslot, mTitleData.length, timeSlotSize());
            }
            AlbumTitleEntry titleData[] = mTitleData;
            mTitleActiveStart = startTimeslot;
            mTitleActiveEnd = endTimeslot;
            int titleContentStart = Utils.clamp((startTimeslot + endTimeslot) / 2 - titleData.length / 2,
                    0, Math.max(0, timeSlotSize() - titleData.length));
            int titleContentEnd = Math.min(titleContentStart + titleData.length, timeSlotSize());
            setTitleContentWindow(titleContentStart, titleContentEnd);
        }
        
        AlbumEntry data[] = mData;

        mActiveStart = start;
        mActiveEnd = end;

        int contentStart = Utils.clamp((start + end) / 2 - data.length / 2,
                0, Math.max(0, mSize - data.length));
        int contentEnd = Math.min(contentStart + data.length, mSize);
        setContentWindow(contentStart, contentEnd);
        updateTextureUploadQueue();
        if (mIsActive || mReserveData) updateAllImageRequests();
    }

    private void uploadBgTextureInSlot(int index) {
        if (index < mContentEnd && index >= mContentStart) {
            AlbumEntry entry = mData[index % mData.length];
            if (entry != null && entry.bitmapTexture != null) {
                mTileUploader.addBgTexture(entry.bitmapTexture);
            }
        }
    }

    private void updateTextureUploadQueue() {
        if (!mIsActive && !mReserveData) return;
        mTileUploader.clear();

        if(mTitleSpec != null){
            for (int i = mTitleActiveStart, n = mTitleActiveEnd; i < n; ++i) {
                AlbumTitleEntry titleEntry = mTitleData[i % mTitleData.length];
                if (titleEntry != null && titleEntry.titleTexture != null) {
                    mTileUploader.addTexture(titleEntry.titleTexture);
                }
            }
            int titleRange = Math.max(
                    (mTitleContentEnd - mTitleActiveEnd), (mTitleActiveStart - mTitleContentStart));
            for (int i = 0; i < titleRange; ++i) {
                uploadBgTextureInTitleSlot(mTitleActiveEnd + i);
                uploadBgTextureInTitleSlot(mTitleActiveStart - i - 1);
            }
        }
        
        // add foreground textures
        for (int i = mActiveStart, n = mActiveEnd; i < n; ++i) {
            AlbumEntry entry = mData[i % mData.length];
            if (entry != null && entry.bitmapTexture != null) {
                mTileUploader.addTexture(entry.bitmapTexture);
            }
        }

        // add background textures
        int range = Math.max(
                (mContentEnd - mActiveEnd), (mActiveStart - mContentStart));
        for (int i = 0; i < range; ++i) {
            uploadBgTextureInSlot(mActiveEnd + i);
            uploadBgTextureInSlot(mActiveStart - i - 1);
        }
    }

    // We would like to request non active slots in the following order:
    // Order:    8 6 4 2                   1 3 5 7
    //         |---------|---------------|---------|
    //                   |<-  active  ->|
    //         |<-------- cached range ----------->|
    private void requestNonactiveImages() {
        if(mTitleSpec != null){
            int titleRange = Math.max(
                    (mTitleContentEnd - mTitleActiveEnd), (mTitleActiveStart - mTitleContentStart));
            for (int i = 0 ;i < titleRange; ++i) {
                requestTitleImage(mTitleActiveEnd + i);
                requestTitleImage(mTitleContentStart - 1 - i);
            }
        }
        
        int range = Math.max(
                (mContentEnd - mActiveEnd), (mActiveStart - mContentStart));
        for (int i = 0 ;i < range; ++i) {
            requestSlotImage(mActiveEnd + i);
            requestSlotImage(mActiveStart - 1 - i);
        }
    }

    // return whether the request is in progress or not
    private boolean requestSlotImage(int slotIndex) {
        if (slotIndex < mContentStart || slotIndex >= mContentEnd) return false;
        AlbumEntry entry = mData[slotIndex % mData.length];
        if (entry == null || entry.content != null || entry.item == null) return false;

        // Set up the panorama callback
        entry.mPanoSupportListener = new PanoSupportListener(entry);
        entry.item.getPanoramaSupport(entry.mPanoSupportListener);

        entry.contentLoader.startLoad();
        return entry.contentLoader.isRequestInProgress();
    }

    private void cancelNonactiveImages() {
        if(mTitleSpec != null){
            int titleRange = Math.max(
                    (mTitleContentEnd - mTitleActiveEnd), (mTitleActiveStart - mTitleContentStart));
            for (int i = 0 ;i < titleRange; ++i) {
                cancelTitleImage(mTitleActiveEnd + i);
                cancelTitleImage(mTitleContentStart - 1 - i);
            }
        }
        
        int range = Math.max(
                (mContentEnd - mActiveEnd), (mActiveStart - mContentStart));
        for (int i = 0 ;i < range; ++i) {
            cancelSlotImage(mActiveEnd + i);
            cancelSlotImage(mActiveStart - 1 - i);
        }
    }

    private void cancelSlotImage(int slotIndex) {
        if (slotIndex < mContentStart || slotIndex >= mContentEnd) return;
        AlbumEntry item = mData[slotIndex % mData.length];
        if (item != null && item.contentLoader != null) item.contentLoader.cancelLoad();
    }

    private void freeSlotContent(int slotIndex) {
        AlbumEntry data[] = mData;
        int index = slotIndex % data.length;
        AlbumEntry entry = data[index];
        if (entry == null){
            return;
        }
        if (entry.contentLoader != null) entry.contentLoader.recycle();
        if (entry.bitmapTexture != null) entry.bitmapTexture.recycle();
        data[index] = null;
    }

    private void prepareSlotContent(int slotIndex) {
        AlbumEntry entry = new AlbumEntry();
        MediaItem item = mSource.get(slotIndex); // item could be null;
        entry.item = item;
        entry.mediaType = (item == null)
                ? MediaItem.MEDIA_TYPE_UNKNOWN
                : entry.item.getMediaType();
        entry.path = (item == null) ? null : item.getPath();
        entry.rotation = (item == null) ? 0 : item.getRotation();
        entry.contentLoader = new ThumbnailLoader(slotIndex, entry.item);  
        mData[slotIndex % mData.length] = entry;
    }

    private void updateAllImageRequests() {
        mActiveRequestCount = 0;

        if(mTitleSpec != null){
            for (int i = mTitleActiveStart, n = mTitleActiveEnd; i < n; ++i) {
                if (requestTitleImage(i)) ++mActiveRequestCount;
            }
        }
        
        for (int i = mActiveStart, n = mActiveEnd; i < n; ++i) {
            if (requestSlotImage(i)) ++mActiveRequestCount;
        }
        if (mActiveRequestCount == 0) {
            requestNonactiveImages();
        } else {
            cancelNonactiveImages();
        }
    }
    //TYRD:changjj add for delete function begin
    public int getSlotOldPos(int index){
        if(index < 0 ) return -1;
        AlbumEntry entry = mData[index%mData.length];
        if(entry == null) return -1;
        return entry.oldPos;
    }
    public int getSlotNewPos(int index){
        if(index < 0) return -1;
        AlbumEntry entry = mData[index%mData.length];
        if(entry == null) return -1;
        return entry.newPos;
    }

    private void freeSlotContentCache(int slotIndex) {
        AlbumEntry data[] = mData;
        int index = slotIndex % data.length;
        AlbumEntry entry = data[index];

        data[index] = null;
        if(entry == null || entry.item == null) return;
        int searchIndex = findSlotContent(entry);
        //Log.i("changjj","freeSlotContentCache searchIndex " + searchIndex + " slotIndex " + slotIndex);
        if(searchIndex == -1){
            entry.oldPos = slotIndex;
            entry.operatorCount++;
            if(entry.operatorCount == 1){
                entry.useStatus = SLOT_UNUSED;
            }
            if(mBackDataEnd >= mBackData.length){
                AlbumEntry releaseEntry = mBackData[mBackDataEnd % mBackData.length];
                if (releaseEntry != null){
                    if (releaseEntry.contentLoader != null) releaseEntry.contentLoader.recycle();
                    if (releaseEntry.bitmapTexture != null) releaseEntry.bitmapTexture.recycle();
                }
                mBackData[mBackDataEnd % mBackData.length] = null;
            }
            mBackData[mBackDataEnd++ % mBackData.length] = entry;
            //Log.i("changjj","freeSlotContentCache searchIndex null entry.useStatus " + entry.useStatus  + " entry.path " + entry.path);
        }else{
            mBackData[searchIndex % mBackData.length].oldPos = slotIndex;
            mBackData[searchIndex % mBackData.length].operatorCount++;
            if(mBackData[searchIndex % mBackData.length].operatorCount == 1){
                mBackData[searchIndex % mBackData.length].useStatus = SLOT_UNUSED;
            }
            //Log.i("changjj","freeSlotContentCache searchIndex sucessful entry.useStatus " + entry.useStatus + " entry.path " + entry.path );
        }
        //Log.i("changjj","freeSlotContentCache " + mBackDataEnd);
    }
    private void prepareSlotContentCache(int slotIndex) {
        //Log.i("changjj","prepareSlotContentCache " + slotIndex);
        AlbumEntry entry = new AlbumEntry();
        MediaItem item = mSource.get(slotIndex); // item could be null;
        entry.item = item;
        entry.mediaType = (item == null)
                ? MediaItem.MEDIA_TYPE_UNKNOWN
                : entry.item.getMediaType();
        entry.path = (item == null) ? null : item.getPath();
        entry.rotation = (item == null) ? 0 : item.getRotation();
        int searchIndex = findSlotContent(entry);
        //Log.i("changjj","prepareSlotContentCache searchIndex " + searchIndex + " entry.path " + entry.path + " slotIndex " + slotIndex);
        if(searchIndex == -1){
            //Log.i("changjj"," prepareSlotContent1 new Slot ready");

            entry.useStatus = SLOT_UNINIT;
            entry.newPos = slotIndex;
            entry.operatorCount = 0;

            entry.contentLoader = new ThumbnailLoader(slotIndex, entry.item);
            mData[slotIndex % mData.length] = entry;
            //Log.i("changjj","prepareSlotContentCache searchIndex null entry.useStatus " + entry.useStatus  + " entry.path " + entry.path);
        }else {
            AlbumEntry searchEntry = mBackData[searchIndex %  mBackData.length]; 
            if(searchEntry.bitmapTexture == null ){
                entry.useStatus = SLOT_UNINIT;
                entry.newPos = slotIndex;
                entry.operatorCount = 0;

                entry.contentLoader = new ThumbnailLoader(slotIndex, entry.item);
                mData[slotIndex % mData.length] = entry;
            }else{
                searchEntry.newPos = slotIndex;
                searchEntry.useStatus = SLOT_USED;
                searchEntry.operatorCount++;
                mData[slotIndex % mData.length] = searchEntry;
                //reAdjustBackData(searchIndex);
                //Log.i("changjj","prepareSlotContentCache searchIndex suessful entry.useStatus " + searchEntry.useStatus  + " entry.path " + searchEntry.path);
            }
        }
    }
    private void BackCopyData(int startPos,int endPos){
        if(endPos - startPos <= 0)return;
        AlbumEntry data[] = mData;
        AlbumEntry backData[] = mBackData;
        mBackDataStart = startPos;
        mBackDataEnd = startPos;
        for(int i = startPos; i < endPos; i++){
        //for(int i = 0; i < (endPos - startPos); i++){
            backData[mBackDataEnd % backData.length] = data[i % mData.length];
            if (backData[mBackDataEnd % backData.length] != null){
                backData[mBackDataEnd % backData.length].oldPos = i;
                backData[mBackDataEnd % backData.length].useStatus = SLOT_INITED;
                backData[mBackDataEnd % backData.length].operatorCount = 0;
            }
            mBackDataEnd++;
        }
        //Log.i("changjj","BackCopyData mBackDataStart " + mBackDataStart + " mBackDataEnd " + mBackDataEnd);
    }
    private void BackCopyDataWindow(int startPos,int endPos){
        if(endPos - startPos <= 0)return;
        AlbumEntry data[] = mData;
        AlbumEntry backData[] = mBackData;
        mBackDataStart = startPos;
        mBackDataEnd = startPos;
        for(int i = startPos; i < endPos; i++){
        //for(int i = 0; i < (endPos - startPos); i++){
            backData[mBackDataEnd % backData.length] = data[i % mData.length];
            if(backData[mBackDataEnd % backData.length].oldPos == 0){
                backData[mBackDataEnd % backData.length].oldPos = i;
            }
            mBackDataEnd++;
        }
    }
    private void freeBackSlotContent(int slotIndex){
        AlbumEntry data[] = mBackData;
        int index = slotIndex % data.length;
        AlbumEntry entry = data[index];
        //Log.i("changjj","freeBackSlotContent entry " + entry);
        if(entry == null) return;
        if(entry.useStatus == SLOT_UNUSED && entry.operatorCount == 1){
            Log.i("changjj","freeBackSlotContent entry.status " + entry.useStatus);
            Log.i("changjj","freeBackSlotContent entry.path " + entry.path);
            if (entry.contentLoader != null) entry.contentLoader.recycle();
            if (entry.bitmapTexture != null) entry.bitmapTexture.recycle();
            //data[index] = null;
            reAdjustBackData(slotIndex);
        }
    }
    private void reAdjustBackData(int index){
        if(mBackDataEnd <= 0) return;

        AlbumEntry data[] = mBackData;
        AlbumEntry entry = data[index % mBackData.length];
        if(mBackDataEnd == 1){
            mBackData[index %  mBackData.length] = null;
        }else{
            int i = index % mBackData.length;
            for(;i < (mBackDataEnd - 1) % mBackData.length; i++){
                mBackData[i] = mBackData[i + 1];
            }
            mBackData[(mBackDataEnd - 1) %  mBackData.length] = null;
        }
        mBackDataEnd --;
        //Log.i("changjj","reAdjustBackData index " + index + " entry.useStatus " + entry.useStatus);
    }

    private void compareDataArray(){
        AlbumEntry data[] = mData;
        AlbumEntry backData[] = mBackData;
        AlbumEntry srcEntry = null;
        AlbumEntry backEntry = null;
        for(int i = mBackDataStart; i < mBackDataEnd; i++){
            backEntry = mBackData[i % mBackData.length];
            if(backEntry != null && backEntry.item != null){
                for(int j = mContentStart ; j < mContentEnd; j++){
                    srcEntry = mData[j % mData.length];
                    if(backEntry.item.equals(srcEntry.item)){
                        //backData[i%mBackData.length] = null;
                        backData[i%mBackData.length].useStatus = SLOT_USED;
                        break;
                    }
                }
            }
        }
    }
    private int findSlotContent(AlbumEntry srcEntry){
        AlbumEntry data[] = mBackData;
        if(srcEntry.item == null) return -1;
        //Log.d("changjj","findSlotContent mBackDataStart " + mBackDataStart + " mBackDataEnd " + mBackDataEnd + "srcEntry " + srcEntry.path);
        int index = mBackDataEnd - 1;
        for ( ;index >= mBackDataStart; index--){
            AlbumEntry entry = data[index % data.length];

            //Log.d("changjj","findSlotContent entry.path " + entry.path + " srcEntry.path " + srcEntry.path);
            if(entry != null && entry.item != null){
                if(srcEntry.item.equals(entry.item)){
                    return index;
                }
            }
        }
        return -1;
    }

    //TYRD:changjj add for delete function end


    private class ThumbnailLoader extends BitmapLoader  {
        private final int mSlotIndex;
        private final MediaItem mItem;

        public ThumbnailLoader(int slotIndex, MediaItem item) {
            mSlotIndex = slotIndex;
            mItem = item;
        }

        @Override
        protected Future<Bitmap> submitBitmapTask(FutureListener<Bitmap> l) {
            return mThreadPool.submit(
                    mItem.requestImage(MediaItem.TYPE_MICROTHUMBNAIL), this);
        }

        @Override
        protected void onLoadComplete(Bitmap bitmap) {
            mHandler.obtainMessage(MSG_UPDATE_ENTRY, this).sendToTarget();
        }

        public void updateEntry() {
            Bitmap bitmap = getBitmap();
            if (bitmap == null) return; // error or recycled
            AlbumEntry entry = mData[mSlotIndex % mData.length];
            if (entry == null) return;
            entry.bitmapTexture = new TiledTexture(bitmap, mTiledResources, mGalleryContext.getGLTag(R.id.ty_gl_tag), TAG);
            entry.content = entry.bitmapTexture;

            if (isActiveSlot(mSlotIndex)) {
                mTileUploader.addTexture(entry.bitmapTexture);
                --mActiveRequestCount;
                if (mActiveRequestCount == 0) requestNonactiveImages();
                if (mListener != null) mListener.onContentChanged();
            } else {
                mTileUploader.addBgTexture(entry.bitmapTexture);
            }
        }
    }

    @Override
    public void onSizeChanged(int size, LinkedHashMap<String, Integer> timeslotInfo) { //taoxj modify
        //TYRD:changjj add for delete function begin
        if(mSize != 0){
            mIsInitLoaded = true;
        }
        mEdieMode = true;
        BackCopyData(mContentStart,mContentEnd);
        //TYRD:changjj add for delete function end
        if(mTitleSpec != null){
            boolean titleEquals = timeslotEquals(mTimeslotInfo, timeslotInfo);
            if (mSize != size || !titleEquals) {
                mSize = size;
                mTimeslotInfo = timeslotInfo;
                if (!titleEquals){
                    for (int i = mTitleContentStart, n = mTitleContentEnd; i < n; ++i) {
                        freeTitleContent(i);
                        prepareTitleContent(i);
                    }
                    updateAllImageRequests();
                }
                
                if (mListener != null) mListener.onSizeChanged(mSize, timeslotInfo);
                if (mContentEnd > mSize) mContentEnd = mSize;
                if (mActiveEnd > mSize) mActiveEnd = mSize;
            }
        }else{
            if (mSize != size) {

                mSize = size;
                if (mListener != null) mListener.onSizeChanged(mSize, null);
                if (mContentEnd > mSize) mContentEnd = mSize;
                if (mActiveEnd > mSize) mActiveEnd = mSize;
            }
        }
        //TYRD:changjj add for delete function begin
        mAnamitionPlaying = false;
        //TYRD:changjj add for delete function end
    }

    @Override
    public void onContentChanged(int index) {
        if (index >= mContentStart && index < mContentEnd && mIsActive) {
            //TYRD;changjj modify for delete function begin
            //freeSlotContent(index);
            //prepareSlotContent(index);
            freeSlotContentCache(index);
            prepareSlotContentCache(index);
            //TYRD;changjj modify for delete function end
            updateAllImageRequests();
            if (mListener != null && isActiveSlot(index)) {
                mListener.onContentChanged();
            }
        }
    }

    //TYRD:changjj add for delete function begin
    @Override
    public void onLoadFinished(){
        //compareDataArray();

        for(int i = mBackDataStart; i < mBackDataEnd; i++){
            freeBackSlotContent(i);
        }
        //mBackDataStart = 0;
        //mBackDataEnd = 0;
        if(mListener != null && mIsInitLoaded && !mAnamitionPlaying){
            mListener.onContentLoadFinished();
            mAnamitionPlaying = true;
        }
        mEdieMode = false;
    }
    //TYRD:changjj add for delete function end

    public void resume() {
        mIsActive = true;
        if (mReserveData) return;
        mTiledResources.prepareResources();
        if(mTitleSpec != null){
            for (int i = mTitleContentStart, n = mTitleContentEnd; i < n; ++i) {
                prepareTitleContent(i);
            }
        }
        for (int i = mContentStart, n = mContentEnd; i < n; ++i) {
            prepareSlotContent(i);
        }
        updateAllImageRequests();
    }

    public void pause() {
        mIsActive = false;
        if (mReserveData) return;
        
        mTileUploader.clear();
        mTiledResources.freeResources();
        for (int i = mContentStart, n = mContentEnd; i < n; ++i) {
            freeSlotContent(i);
        }
        if(mTitleSpec != null){
            for (int i = mTitleContentStart, n = mTitleContentEnd; i < n; ++i) {
                freeTitleContent(i);
            }
        }
    }

    /// M: Video thumbnail play @{
    public int getActiveStart() {
        return mActiveStart;
    }
    public int getActiveEnd() {
        return mActiveEnd;
    }
    public int getContentStart() {
        return mContentStart;
    }
    public int getContentEnd() {
        return mContentEnd;
    }

    public AlbumEntry getThumbnailEntryAt(int slotIndex) {
        if (isActiveSlot(slotIndex)) {
            return mData[slotIndex % mData.length];
        }
        return null;
    }

    public boolean isAllActiveSlotsStaticThumbnailReady() {
//        int start = mActiveStart;
//        int end = mActiveEnd;
//
//        if (start < 0 || start >= end) {
//            MtkLog.w(TAG, "isAllActiveSlotsStaticThumbnailReady: active range not ready yet");
//            return false;
//        }
//
//        for (int i = start; i < end; ++i) {
//            AlbumEntry entry = mData[i % mData.length];
//            boolean isReady = (entry != null && entry.contentLoader != null && entry.contentLoader
//                    .isLoadingCompleted());
//            if (!isReady) {
//                return false;
//            }
//        }

        return true;
    }
    /// @}

    private class TyAlbumTitleLoader extends BitmapLoader {
        private final int mTitleIndex;
        private final String mTitle;
        private final int mTotalCount;

        public TyAlbumTitleLoader(
                int titleIndex, String title, int totalCount) { //taoxj modify
            mTitleIndex = titleIndex;
            mTitle = title;
            mTotalCount = totalCount;
        }

        @Override
        protected Future<Bitmap> submitBitmapTask(FutureListener<Bitmap> l) {
            String totalCount = String.valueOf(mTotalCount);
            if (mTotalCount > 999){
                totalCount = "999+";
            }
            return mThreadPool.submit(mTitleMaker.requestTitle(
                    mTitle, totalCount), l);
        }

        @Override
        protected void onLoadComplete(Bitmap bitmap) {
            mHandler.obtainMessage(MSG_UPDATE_ALBUM_TITLE, this).sendToTarget();
        }

        public void updateEntry() {
            Bitmap bitmap = getBitmap();
            if (bitmap == null) return; // Error or recycled

            AlbumTitleEntry titleEntry = mTitleData[mTitleIndex % mTitleData.length];
            if (titleEntry == null) return;
            
            titleEntry.titleTexture = new TiledTexture(bitmap, mTiledResources, mGalleryContext.getGLTag(R.id.ty_gl_tag), TAG);
            //texture.setOpaque(false);
            titleEntry.titleContent = titleEntry.titleTexture;;

            if (isActiveTitle(mTitleIndex)) {
                mTileUploader.addTexture(titleEntry.titleTexture);
                --mActiveRequestCount;
                if (mActiveRequestCount == 0) requestNonactiveImages();
                if (mListener != null) mListener.onContentChanged();
            } else {
                mTileUploader.addBgTexture(titleEntry.titleTexture);
            }
        }
    }
    //taoxj modify
    private boolean timeslotEquals (LinkedHashMap<String, Integer> ste, LinkedHashMap<String, Integer> dte) {
        if ((ste == null && dte != null)
                || (ste != null && dte == null)){
            return false;
        }
        
        if (ste != null && dte != null){
            int len = ste.size();
            if (len != dte.size()){
                return false;
            }

            for (Map.Entry<String, Integer> item : ste.entrySet()){
                Integer detValue = dte.get(item.getKey());
                if (detValue == null){
                    return false;
                }
                
                if (detValue.intValue() != item.getValue().intValue()){
                    return false;
                }
            }
        }
        return true;
    }
    
    private int timeSlotSize() {
        return mTimeslotInfo == null ? 0 : mTimeslotInfo.size();
    }

    private void setTitleContentWindow(int titleContentStart, int titleContentEnd) {
        if (titleContentStart == mTitleContentStart && titleContentEnd == mTitleContentEnd) return;
        
        if (!mIsActive && !mReserveData) {
            mTitleContentStart = titleContentStart;
            mTitleContentEnd = titleContentEnd;
            return;
        }

        if (titleContentStart >= mTitleContentEnd || mTitleContentStart >= titleContentEnd) {
            for (int i = mTitleContentStart, n = mTitleContentEnd; i < n; ++i) {
                freeTitleContent(i);
            }
            for (int i = titleContentStart; i < titleContentEnd; ++i) {
                prepareTitleContent(i);
            }
        } else {
            for (int i = mTitleContentStart; i < titleContentStart; ++i) {
                freeTitleContent(i);
            }
            for (int i = titleContentEnd, n = mTitleContentEnd; i < n; ++i) {
                freeTitleContent(i);
            }
            for (int i = titleContentStart, n = mTitleContentStart; i < n; ++i) {
                prepareTitleContent(i);
            }
            for (int i = mTitleContentEnd; i < titleContentEnd; ++i) {
                prepareTitleContent(i);
            }
        }

        mTitleContentStart = titleContentStart;
        mTitleContentEnd = titleContentEnd;
    }
    
    private void cancelTitleImage(int titleIndex) {
        if (titleIndex < mTitleContentStart || titleIndex >= mTitleContentEnd) return;
        AlbumTitleEntry titleEntry = mTitleData[titleIndex % mTitleData.length];
        if (titleEntry != null && titleEntry.titleLoader != null) titleEntry.titleLoader.cancelLoad();
    }

    
    private void freeTitleContent(int titleIndex) {
        AlbumTitleEntry titleData[] = mTitleData;
        int index = titleIndex % titleData.length;
        AlbumTitleEntry entry = titleData[index];
        if (entry == null) return;
        if (entry.titleLoader != null) entry.titleLoader.recycle();
        if (entry.titleTexture != null) entry.titleTexture.recycle();
        titleData[index] = null;
    }

    private void prepareTitleContent(int titleIndex) {
        AlbumTitleEntry entry = new AlbumTitleEntry();
        if (titleIndex < 0 || titleIndex >= timeSlotSize()){
            return;
        }
        //taoxj modify
        String titleRes = "";
        int totalCount = 0;
        int i = 0;
        for (Map.Entry<String, Integer> item : mTimeslotInfo.entrySet()){ //taoxj modify
            if (i == titleIndex){
                titleRes = item.getKey();
                totalCount = item.getValue();
                break;
            }
            i++;
        }

        entry.titleLoader = new TyAlbumTitleLoader(titleIndex, titleRes, totalCount);
        mTitleData[titleIndex % mTitleData.length] = entry;
    }

    private void uploadBgTextureInTitleSlot(int titleIndex) {
        if (titleIndex < mTitleContentEnd && titleIndex >= mTitleContentStart) {
            AlbumTitleEntry titleEntry = mTitleData[titleIndex % mTitleData.length];
            if (titleEntry != null && titleEntry.titleTexture != null) {
                mTileUploader.addBgTexture(titleEntry.titleTexture);
            }
        }
    }
    
    private boolean requestTitleImage(int titleIndex) {
        if (titleIndex < mTitleContentStart || titleIndex >= mTitleContentEnd) return false;
        AlbumTitleEntry titleEntry = mTitleData[titleIndex % mTitleData.length];
        if (titleEntry == null || titleEntry.titleContent != null) return false;

        titleEntry.titleLoader.startLoad();
        return titleEntry.titleLoader.isRequestInProgress();
    }

    public boolean isActiveTitle(int titleIndex) {
        return titleIndex >= mTitleActiveStart && titleIndex < mTitleActiveEnd;
    }

    public AlbumTitleEntry getTitle(int titleIndex) {
        if (!isActiveTitle(titleIndex)) {
            Utils.fail("invalid title: %s outsides (%s, %s)",
                    titleIndex, mTitleActiveStart, mTitleActiveEnd);
        }
        return mTitleData[titleIndex % mTitleData.length];
    }

    public void onTitleSizeChanged(int width, int height){
        if (mTitleSpec == null || mTitleWidth == width) return;
        mTitleWidth = width;
        mTitleMaker.setTitleWidth(mTitleWidth);
        for (int i = mTitleContentStart, n = mTitleContentEnd; i < n; ++i) {
            freeTitleContent(i);
            prepareTitleContent(i);
        }
        updateAllImageRequests();
    }

    public void setReserveData(boolean bReserveData){
        if (mReserveData == bReserveData) return;
        if (bReserveData){
            BasicTexture.setReserveString(mGalleryContext.getGLTag(R.id.ty_gl_tag), TAG);
        }else{
            BasicTexture.removeReserveString(mGalleryContext.getGLTag(R.id.ty_gl_tag));
        }
        mReserveData = bReserveData;
    }
    
    public boolean getReserveData(){
        return mReserveData;
    }
}
