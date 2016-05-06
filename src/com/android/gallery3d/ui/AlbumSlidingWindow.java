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

import android.graphics.Bitmap;
import android.os.Message;

import com.android.gallery3d.app.GalleryContext;
import com.android.gallery3d.app.AlbumDataLoader;
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
import com.android.gallery3d.R;

public class AlbumSlidingWindow implements AlbumDataLoader.DataListener,SlotView.SlotPos {
    @SuppressWarnings("unused")
    private static final String TAG = "AlbumSlidingWindow";

    private static final int MSG_UPDATE_ENTRY = 0;
    private static final int JOB_LIMIT = 2;

    //TYRD:changjj add for bind texture to slot render begin
    private static final int SLOT_UNINIT = 0;
    private static final int SLOT_INITED = 1;
    private static final int SLOT_USED = 2;
    private static final int SLOT_UNUSED = 3;

    private boolean mIsInitLoaded = false;  
    private boolean mAnamitionPlaying = false;
    //TYRD:changjj add for bind texture to slot render end
    private boolean mReserveData;

    public static interface Listener {
        public void onSizeChanged(int size);
        public void onContentChanged();
        //TYRD:changjj add for delete function begin
        public void onContentLoadFinished();
        //TYRD:changjj add for delete function end
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
        public int  useStatus;
        public int  operatorCount;
        public int  oldPos;
        public int  newPos;
        //
    }

    private final AlbumDataLoader mSource;
    private final AlbumEntry mData[];
    private final SynchronizedHandler mHandler;
    private final JobLimiter mThreadPool;
    private final TiledTexture.Uploader mTileUploader;
    /*TIANYU: yuxin add begin for New Design Gallery*/
    private final TiledTexture.Resources mTiledResources;
    /*TIANYU: yuxin add end for New Design Gallery*/
    private int mSize;

    private int mContentStart = 0;
    private int mContentEnd = 0;

    private int mActiveStart = 0;
    private int mActiveEnd = 0;

    //TYRD:changjj add delete function begin 
    private final AlbumEntry mBackData[];

    private int mBackDataStart = 0;
    private int mBackDataEnd = 0;
    private boolean mEditMode = false;
    //TYRD:changjj add delete function  end

    private Listener mListener;

    private int mActiveRequestCount = 0;
    private boolean mIsActive = false;
    private GalleryContext mGalleryContext;
    //TY wb034 20150206 add begin for tygallery
    public boolean mIsCollectAlbum = false;
    //TY wb034 20150206 add end for tygallery
	
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

    public AlbumSlidingWindow(GalleryContext activity,
            AlbumDataLoader source, int cacheSize) {
        mGalleryContext = activity;
        source.setDataListener(this);
        mSource = source;
        //TY wb034 20150206 add begin for tygallery
        mIsCollectAlbum = mSource.isCollectLoader();
        //TY wb034 20150206 add end for tygallery
        mData = new AlbumEntry[cacheSize];
        mSize = source.size();

        mHandler = new SynchronizedHandler(activity.getGLRoot()) {
            @Override
            public void handleMessage(Message message) {
                Utils.assertTrue(message.what == MSG_UPDATE_ENTRY);
                ((ThumbnailLoader) message.obj).updateEntry();
            }
        };

        mThreadPool = new JobLimiter(activity.getThreadPool(), JOB_LIMIT);
        mTileUploader = new TiledTexture.Uploader(activity.getGLRoot());
        /*TIANYU: yuxin add beginmData.lengte for New Design Gallery*/
        mTiledResources = new TiledTexture.Resources();
        /*TIANYU: yuxin add end for New Design Gallery*/
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
        //TY wb034 20150206 add begin for tygallery
        if(mIsCollectAlbum&&(mSource.get(slotIndex)==null)) return null;
        //TY wb034 20150206 add end for tygallery
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
        
        //TYRD:changjj modiry for delete anamition function begin
        if(mEditMode){
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
        //TYRD:changjj modiry for delete anamition function end

        mContentStart = contentStart;
        mContentEnd = contentEnd;

    }

    public void setActiveWindow(int start, int end) {
        if (!(start <= end && end - start <= mData.length && end <= mSize)) {
            Utils.fail("%s, %s, %s, %s", start, end, mData.length, mSize);
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
            if (entry.bitmapTexture != null) {
                mTileUploader.addBgTexture(entry.bitmapTexture);
            }
        }
    }

    private void updateTextureUploadQueue() {
        if (!mIsActive && !mReserveData) return;
        mTileUploader.clear();

        // add foreground textures
        for (int i = mActiveStart, n = mActiveEnd; i < n; ++i) {
            AlbumEntry entry = mData[i % mData.length];
            if (entry.bitmapTexture != null) {
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
        if (entry.content != null || entry.item == null) return false;

        // Set up the panorama callback
        entry.mPanoSupportListener = new PanoSupportListener(entry);
        entry.item.getPanoramaSupport(entry.mPanoSupportListener);

        entry.contentLoader.startLoad();
        return entry.contentLoader.isRequestInProgress();
    }

    private void cancelNonactiveImages() {
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
        if (item.contentLoader != null) item.contentLoader.cancelLoad();
    }

    private void freeSlotContent(int slotIndex) {
        AlbumEntry data[] = mData;
        int index = slotIndex % data.length;
        AlbumEntry entry = data[index];
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
        AlbumEntry entry = mData[index % mData.length];
        if(entry == null) return -1;
        return entry.oldPos;
    }
    public int getSlotNewPos(int index){
        if(index < 0 ) return -1;
        AlbumEntry entry = mData[index % mData.length];
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
        }else{
            mBackData[searchIndex % mBackData.length].oldPos = slotIndex;
            mBackData[searchIndex % mBackData.length].operatorCount++;
            if(mBackData[searchIndex % mBackData.length].operatorCount == 1){
                mBackData[searchIndex % mBackData.length].useStatus = SLOT_UNUSED;
            }
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
        //TYRD: changjj add
        int searchIndex = findSlotContent(entry);
        //Log.i("changjj","prepareSlotContentCache searchIndex " + searchIndex + " entry.path " + entry.path + " slotIndex " + slotIndex);
        if(searchIndex == -1){
            //Log.i("changjj"," prepareSlotContent1 new Slot ready");

            entry.useStatus = SLOT_UNINIT;
            entry.newPos = slotIndex;
            entry.operatorCount = 0;

            entry.contentLoader = new ThumbnailLoader(slotIndex, entry.item);
            mData[slotIndex % mData.length] = entry;
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
                reAdjustBackData(searchIndex);
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
            //Log.i("changjj","freeBackSlotContent entry.status " + entry.useStatus);
            //Log.i("changjj","freeBackSlotContent entry.path " + entry.path);
            if (entry.contentLoader != null) entry.contentLoader.recycle();
            if (entry.bitmapTexture != null) entry.bitmapTexture.recycle();
            reAdjustBackData(slotIndex);
        }
    }
    private void printBackSlotData(int slotIndex){
        AlbumEntry data[] = mBackData;
        int index = slotIndex % data.length;
        AlbumEntry entry = data[index];
        if(entry == null) return;
        //Log.i("changjj","printBackSlotContent entry.status " + entry.useStatus);
        //Log.i("changjj","printBackSlotContent entry.path " + entry.path);
        //Log.i("changjj","printBackSlotContent entry.oldPos " + entry.oldPos);
        //Log.i("changjj","printBackSlotContent entry.newPos " + entry.newPos);

    }
    private void printSlotData(int slotIndex){
        AlbumEntry data[] = mData;
        int index = slotIndex % data.length;
        AlbumEntry entry = data[index];
        if(entry == null) return;
        //Log.i("changjj","printBackSlotContent entry.status " + entry.useStatus);
        //Log.i("changjj","printBackSlotContent entry.path " + entry.path);
        //Log.i("changjj","printBackSlotContent entry.oldPos " + entry.oldPos);
        //Log.i("changjj","printBackSlotContent entry.newPos " + entry.newPos);

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
        //Log.d("changjj","reAdjustBackData index " + index + " entry.useStatus " + entry.useStatus);
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
                        //backData[i % mBackData.length] = null;
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
        for ( ;index > mBackDataStart; index -- ){
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
            /*TIANYU: yuxin modify begin for New Design Gallery*/
            //entry.bitmapTexture = new TiledTexture(bitmap)
            entry.bitmapTexture = new TiledTexture(bitmap, mTiledResources, mGalleryContext.getGLTag(R.id.ty_gl_tag), TAG);
            /*TIANYU: yuxin modify end for New Design Gallery*/
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
    public void onSizeChanged(int size) {
        //TYRD:changjj add for delete function begin
        if(mSize != 0){
            mIsInitLoaded = true;
        }
        
        //TYRD:changjj add for delete function end
        if (mSize != size) {
            //TYRD:changjj add for delete function begin
            BackCopyData(mContentStart,mContentEnd);
            mEditMode = true;
            //TYRD:changjj add for delete function end
            mSize = size;
            if (mListener != null) mListener.onSizeChanged(mSize);
            if (mContentEnd > mSize) mContentEnd = mSize;
            if (mActiveEnd > mSize) mActiveEnd = mSize;
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
        if (mListener != null && mIsInitLoaded  && !mAnamitionPlaying) {
            mListener.onContentLoadFinished();
            mAnamitionPlaying = true;
        }
        mEditMode = false;
        /*for(int j = mActiveStart ; j < mActiveEnd; j++){
            printSlotData(j);
        }*/
    }
    //TYRD:changjj add for delete function end


    public void resume() {
        mIsActive = true;
        if (mReserveData) return;
        /*TIANYU: yuxin modify begin for New Design Gallery*/
        //TiledTexture.prepareResources();
        mTiledResources.prepareResources();
        /*TIANYU: yuxin modify end for New Design Gallery*/
        for (int i = mContentStart, n = mContentEnd; i < n; ++i) {
            prepareSlotContent(i);
        }
        updateAllImageRequests();
    }

    public void pause() {
        mIsActive = false;
        if (mReserveData) return;
        mTileUploader.clear();
        /*TIANYU: yuxin modify begin for New Design Gallery*/
        //TiledTexture.freeResources();
        mTiledResources.freeResources();
        /*TIANYU: yuxin modify end for New Design Gallery*/
        for (int i = mContentStart, n = mContentEnd; i < n; ++i) {
            freeSlotContent(i);
        }
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
