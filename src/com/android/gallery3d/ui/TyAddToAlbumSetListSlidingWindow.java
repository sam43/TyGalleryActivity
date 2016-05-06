/*
 * TIANYU: yuxin add for New Design Gallery
 */

package com.android.gallery3d.ui;

import android.graphics.Bitmap;
import android.os.Message;
import android.os.Handler;

import com.android.gallery3d.R;
import com.android.gallery3d.app.GalleryContext;
import com.android.gallery3d.app.TyAddToAlbumSetListDataLoader;
import com.android.gallery3d.app.TyAddToAlbumSetListDataLoader.DataListener;
import com.android.gallery3d.common.Utils;
//TY wb034 02150130 add begin for tygallery
import com.android.gallery3d.data.DataManager;
//TY wb034 02150130 add end for tygallery
import com.android.gallery3d.data.MediaItem;
import com.android.gallery3d.data.MediaObject;
import com.android.gallery3d.data.MediaSet;
import com.android.gallery3d.data.Path;
import com.android.gallery3d.util.Future;
import com.android.gallery3d.util.FutureListener;
import com.android.gallery3d.util.ThreadPool;
import com.android.gallery3d.glrenderer.TiledTexture;
import com.android.gallery3d.common.BitmapUtils;

public class TyAddToAlbumSetListSlidingWindow implements TyAddToAlbumSetListDataLoader.DataListener{
    private static final String TAG = "Gallery2/TyAddToAlbumSetListSlidingWindow";
    private static final int MSG_UPDATE_ALBUM_ENTRY = 1;
    public static interface Listener {
        public void onSizeChanged(int size);
        public void onContentChanged();
    }

    private final TyAddToAlbumSetListDataLoader mSource;
    private int mSize;

    private int mContentStart = 0;
    private int mContentEnd = 0;

    private int mActiveStart = 0;
    private int mActiveEnd = 0;

	private Listener mListener;

    private final AlbumSetEntry mData[];
    private final Handler mHandler;
    private final ThreadPool mThreadPool;

    private int mActiveRequestCount = 0;
    private boolean mIsActive = false;
   //TY wb034 20150129 add begin for tygallery
    private DataManager mDataManager;
   //TY wb034 20150129 add end for tygallery
    public static class AlbumSetEntry {
        public MediaSet album;
        public MediaItem coverItem;
        public Bitmap bitmap;
        public Path setPath;
        public String title;
        public int totalCount;
        public int sourceType;
        public int cacheFlag;
        public int cacheStatus;
        public int rotation;
        public long setDataVersion;
        public long coverDataVersion;
        private BitmapLoader coverLoader;
        public int MEDIA_TYPE_VIDEO;//TY liuyuchuan  add begin for PROD103692994

    }
    public TyAddToAlbumSetListSlidingWindow(GalleryContext activity,
    		TyAddToAlbumSetListDataLoader source, int cacheSize) {
        source.setModelListener(this);
        //TY wb034 20150129 add begin for tygallery
        mDataManager = activity.getDataManager();
        //TY wb034 20150129 add end for tygallery       
        mSource = source;
        mData = new AlbumSetEntry[cacheSize];
        mSize = source.size();
        mThreadPool = activity.getThreadPool();
        mHandler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                Utils.assertTrue(message.what == MSG_UPDATE_ALBUM_ENTRY);
                ((EntryUpdater) message.obj).updateEntry();
            }
        };
    }

    public void setListener(Listener listener) {
        mListener = listener;
    }

    public AlbumSetEntry get(int itemIndex) {
        if (!isActiveItem(itemIndex)) {
            Utils.fail("invalid item: %s outsides (%s, %s)",
                    itemIndex, mActiveStart, mActiveEnd);
        }
        return mData[itemIndex % mData.length];
    }
    
    public AlbumSetEntry getNoCheck(int itemIndex) {
        return mData[itemIndex % mData.length];
    }
    

    public int size() {
        return mSize;
    }

    public boolean isActiveItem(int itemIndex) {
        return itemIndex >= mActiveStart && itemIndex < mActiveEnd;
    }

    private void setContentWindow(int contentStart, int contentEnd) {
        if (contentStart == mContentStart && contentEnd == mContentEnd) return;

        if (contentStart >= mContentEnd || mContentStart >= contentEnd) {
            for (int i = mContentStart, n = mContentEnd; i < n; ++i) {
                freeItemContent(i);
            }
            mSource.setActiveWindow(contentStart, contentEnd);
            for (int i = contentStart; i < contentEnd; ++i) {
                prepareItemContent(i);
            }
        } else {
            for (int i = mContentStart; i < contentStart; ++i) {
                freeItemContent(i);
            }
            for (int i = contentEnd, n = mContentEnd; i < n; ++i) {
                freeItemContent(i);
            }
            mSource.setActiveWindow(contentStart, contentEnd);
            for (int i = contentStart, n = mContentStart; i < n; ++i) {
                prepareItemContent(i);
            }
            for (int i = mContentEnd; i < contentEnd; ++i) {
                prepareItemContent(i);
            }
        }

        mContentStart = contentStart;
        mContentEnd = contentEnd;
    }

    public void setActiveWindow(int start, int end) {
        if (!(start <= end && end - start <= mData.length && end <= mSize)) {
            Utils.fail("start = %s, end = %s, length = %s, size = %s",
                    start, end, mData.length, mSize);
        }

        AlbumSetEntry data[] = mData;
        mActiveStart = start;
        mActiveEnd = end;
        int contentStart = Utils.clamp((start + end) / 2 - data.length / 2,
                0, Math.max(0, mSize - data.length));
        int contentEnd = Math.min(contentStart + data.length, mSize);
        setContentWindow(contentStart, contentEnd);

        if (mIsActive) {
            updateAllImageRequests();
        }
    }

    // We would like to request non active item in the following order:
    // Order:    8 6 4 2                   1 3 5 7
    //         |---------|---------------|---------|
    //                   |<-  active  ->|
    //         |<-------- cached range ----------->|
    private void requestNonactiveImages() {
        int range = Math.max(
                mContentEnd - mActiveEnd, mActiveStart - mContentStart);
        for (int i = 0 ;i < range; ++i) {
            requestImagesInItem(mActiveEnd + i);
            requestImagesInItem(mActiveStart - 1 - i);
        }
    }

    private void cancelNonactiveImages() {
        int range = Math.max(
                mContentEnd - mActiveEnd, mActiveStart - mContentStart);
        for (int i = 0 ;i < range; ++i) {
            cancelImagesInItem(mActiveEnd + i);
            cancelImagesInItem(mActiveStart - 1 - i);
        }
    }

    private void requestImagesInItem(int itemIndex) {
        if (itemIndex < mContentStart || itemIndex >= mContentEnd) return;
        AlbumSetEntry entry = mData[itemIndex % mData.length];
        if (entry.coverLoader != null) entry.coverLoader.startLoad();
    }

    private void cancelImagesInItem(int itemIndex) {
        if (itemIndex < mContentStart || itemIndex >= mContentEnd) return;
        AlbumSetEntry entry = mData[itemIndex % mData.length];
        if (entry.coverLoader != null) entry.coverLoader.cancelLoad();
    }

    private static long getDataVersion(MediaObject object) {
        return object == null
                ? MediaSet.INVALID_DATA_VERSION
                : object.getDataVersion();
    }

    private void freeItemContent(int itemIndex) {
        AlbumSetEntry entry = mData[itemIndex % mData.length];
        if(entry==null)return;
    	if (entry.coverLoader != null) {
            if (entry.rotation != 0){
                entry.bitmap.recycle();
            }
            entry.coverLoader.recycle();
        }
        if (entry.bitmap != null) entry.bitmap = null;
        
        mData[itemIndex % mData.length] = null;
    }
    private  void updateAlbumSetEntry(AlbumSetEntry entry, int itemIndex) {
        MediaSet album = mSource.getMediaSet(itemIndex);
        int totalCount = mSource.getTotalCount(itemIndex);
        MediaItem cover =mSource.getCoverItem(itemIndex);
        entry.album = album;
        entry.setDataVersion = getDataVersion(album);
        entry.cacheFlag = identifyCacheFlag(album);
        entry.cacheStatus = identifyCacheStatus(album);
        entry.setPath = (album == null) ? null : album.getPath();
        //entry.totalCount = totalCount;
        String title = (album == null) ? "" : Utils.ensureNotNull(album.getName());
        entry.title = title;
        if(album!=null){
            if(album.getBucketId()== mDataManager.mCollectBucketId){
                 totalCount = mDataManager.getCollectCount();
                 if(totalCount<=0) return;
                 cover = mDataManager.getMediaItem(0);
            }
        }
        entry.totalCount = totalCount;        

        //TY liuyuchuan  add begin for PROD103692994
        if(cover != null){
            entry.MEDIA_TYPE_VIDEO = cover.getMediaType();

        }
        //TY liuyuchuan  add end for PROD103692994
        if (getDataVersion(cover) != entry.coverDataVersion) {
            entry.coverDataVersion = getDataVersion(cover);
            entry.rotation = (cover == null) ? 0 : cover.getRotation();
            if (entry.coverLoader != null) {
                if (entry.rotation != 0){
                    entry.bitmap.recycle();
                }
                entry.coverLoader.recycle();
                entry.coverLoader = null;   
                entry.bitmap = null;
            }
            if (cover != null) {
                entry.coverLoader = new AlbumCoverLoader(itemIndex, cover);
            }
        }
    }

    private void prepareItemContent(int itemIndex) {
        AlbumSetEntry entry = new AlbumSetEntry();
        updateAlbumSetEntry(entry, itemIndex);
        mData[itemIndex % mData.length] = entry;
    }

    private static boolean startLoadBitmap(BitmapLoader loader) {
        if (loader == null) return false;
        loader.startLoad();
        return loader.isRequestInProgress();
    }

    private synchronized void updateAllImageRequests() {
        mActiveRequestCount = 0;
        for (int i = mActiveStart, n = mActiveEnd; i < n; ++i) {
            AlbumSetEntry entry = mData[i % mData.length];
          if (startLoadBitmap(entry.coverLoader)) ++mActiveRequestCount;                   
        }
        if (mActiveRequestCount == 0) {
            requestNonactiveImages();
        } else {
            cancelNonactiveImages();
        }
    }
    
    @Override
    public void onSizeChanged(int size) {
        if (mSize != size) {
            mSize = size;

            if (mListener != null) mListener.onSizeChanged(mSize);
            if (mContentEnd > mSize) mContentEnd = mSize;
            if (mActiveEnd > mSize) mActiveEnd = mSize;
        }
    }
    @Override
    public synchronized void onContentChanged(int index) {	    
        if (!mIsActive) {
            // paused, ignore Item changed event
            return;
        }

        // If the updated content is not cached, ignore it
        if (index < mContentStart || index >= mContentEnd) {
            Log.w(TAG, String.format(
                    "invalid update: %s is outside (%s, %s)",
                    index, mContentStart, mContentEnd) );
            return;
        }
        AlbumSetEntry entry = mData[index % mData.length];
        updateAlbumSetEntry(entry, index);
        updateAllImageRequests();
         if (mListener != null) {
               mListener.onContentChanged();
        }
    }

    public void pause() {
        mIsActive = false;
        //TiledTexture.freeResources();
        for (int i = mContentStart, n = mContentEnd; i < n; ++i) {
            freeItemContent(i);
        }
    }

    public void resume() {
        mIsActive = true;
        for (int i = mContentStart, n = mContentEnd; i < n; ++i) {
            prepareItemContent(i);
        }
        updateAllImageRequests();
    }

    private static interface EntryUpdater {
        public void updateEntry();
    }

    private class AlbumCoverLoader extends BitmapLoader implements EntryUpdater {
        private MediaItem mMediaItem;
        private final int mItemIndex;
        
        public AlbumCoverLoader(int itemIndex, MediaItem item) {
            mItemIndex = itemIndex;
            mMediaItem = item;
        }

        @Override
        protected Future<Bitmap> submitBitmapTask(FutureListener<Bitmap> l) {
            return mThreadPool.submit(mMediaItem.requestImage(
                    MediaItem.TYPE_MICROTHUMBNAIL), l);
        }

        @Override
        protected void onLoadComplete(Bitmap bitmap) {
            mHandler.obtainMessage(MSG_UPDATE_ALBUM_ENTRY, this).sendToTarget();
        }

        @Override
        public synchronized void updateEntry() {
            Bitmap bitmap = getBitmap();
            if (bitmap == null) return; // error or recycled

            AlbumSetEntry entry = mData[mItemIndex % mData.length];
            if (entry.rotation != 0){
                bitmap = BitmapUtils.rotateBitmap(bitmap,
                    entry.rotation, false);
            }
        	entry.bitmap = bitmap;               
            if (isActiveItem(mItemIndex)) {
                --mActiveRequestCount;
                if (mActiveRequestCount == 0) requestNonactiveImages();
                if (mListener != null) mListener.onContentChanged();
            }
        }
    }

    private static int identifyCacheFlag(MediaSet set) {
        if (set == null || (set.getSupportedOperations()
                & MediaSet.SUPPORT_CACHE) == 0) {
            return MediaSet.CACHE_FLAG_NO;
        }

        return set.getCacheFlag();
    }

    private static int identifyCacheStatus(MediaSet set) {
        if (set == null || (set.getSupportedOperations()
                & MediaSet.SUPPORT_CACHE) == 0) {
            return MediaSet.CACHE_STATUS_NOT_CACHED;
        }

        return set.getCacheStatus();
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

    public int getActiveRequestCount() {
        return mActiveRequestCount;
    }

    public AlbumSetEntry getThumbnailEntryAt(int slotIndex) {
        if (isActiveItem(slotIndex)) {
            return mData[slotIndex % mData.length];
        }
        return null;
    }
    
    public boolean isAllActiveSlotsStaticThumbnailReady() {
        int start = mActiveStart;
        int end = mActiveEnd;

        if (start < 0 || start >= end) {
            return false;
        }

        return true;
    }
    /// @}
}
