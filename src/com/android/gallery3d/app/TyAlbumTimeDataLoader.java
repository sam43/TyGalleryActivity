/*
 * TIANYU: yuxin add for New Design Gallery
 */
package com.android.gallery3d.app;

import android.os.Handler;
import android.os.Message;
import android.os.Process;
import com.android.gallery3d.common.Utils;
import com.android.gallery3d.data.ContentListener;
import com.android.gallery3d.data.MediaItem;
import com.android.gallery3d.data.MediaObject;
import com.android.gallery3d.data.MediaSet;
import com.android.gallery3d.data.Path;
import com.android.gallery3d.ui.SynchronizedHandler;
import com.android.gallery3d.data.DataManager;
import com.android.gallery3d.data.DataManager.FilterDatachangeListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.LinkedHashMap;
import java.util.Map;

public class TyAlbumTimeDataLoader {
    private static final String TAG = "Gallery2/TyAlbumTimeDataLoader";
    private static final int DATA_CACHE_SIZE = 1000;

    private static final int MSG_LOAD_START = 1;
    private static final int MSG_LOAD_FINISH = 2;
    private static final int MSG_RUN_OBJECT = 3;
    private static final int MSG_SETLOAD_START = 4;
    private static final int MSG_SETLOAD_FINISH = 5;

    private static final int MIN_LOAD_COUNT = 32;
    private static final int MAX_LOAD_COUNT = 64;

    private final MediaItem[] mData;
    private final long[] mItemVersion;
    private final long[] mSetVersion;

    public static interface DataListener {
        public void onContentChanged(int index);
        //taoxj modify
        public void onSizeChanged(int size, LinkedHashMap<String, Integer> timeslotInfo);
        //TYRD:changjj add for load finish begin
        public void onLoadFinished();
        //TYRD:changjj add for load finish end
    }

    private int mActiveStart = 0;
    private int mActiveEnd = 0;

    private int mContentStart = 0;
    private int mContentEnd = 0;

    private final MediaSet mSourceSet;
    private MediaSet mSource;
    private long mSourceVersion = MediaObject.INVALID_DATA_VERSION;

    private final Handler mMainHandler;
    private int mSize = 0;
    //taoxj modify
    private LinkedHashMap<String, Integer> mTimeslotInfo;
    private DataListener mDataListener;
    private MySourceListener mSourceListener = new MySourceListener();
    private LoadingListener mLoadingListener;

    private ReloadTask mReloadTask;
    // the data version on which last loading failed
    private long mFailedVersion = MediaObject.INVALID_DATA_VERSION;

    private TyAlbumSetTimeDataLoader mTyAlbumSetTimeDataLoader;
    private boolean mSetTimeDataLoaderFinished;
    private boolean mStateLoading;
    
    private DataManager mDataManager;
    private MyFilterDatachangeListener mFilterDatachangeListener = new MyFilterDatachangeListener();
    private boolean mNeedChange = true;

    public TyAlbumTimeDataLoader(GalleryContext context, MediaSet mediaSet) {
        mDataManager = context.getDataManager();
        mSourceSet = mediaSet;
        mData = new MediaItem[DATA_CACHE_SIZE];
        mItemVersion = new long[DATA_CACHE_SIZE];
        mSetVersion = new long[DATA_CACHE_SIZE];
        Arrays.fill(mItemVersion, MediaObject.INVALID_DATA_VERSION);
        Arrays.fill(mSetVersion, MediaObject.INVALID_DATA_VERSION);

        mMainHandler = new SynchronizedHandler(context.getGLRoot()) {
            @Override
            public void handleMessage(Message message) {
                switch (message.what) {
                    case MSG_RUN_OBJECT:
                        ((Runnable) message.obj).run();
                        return;
                    case MSG_LOAD_START:
                        if (mStateLoading == false && mLoadingListener != null) {
                            mLoadingListener.onLoadingStarted();
                        }
                        mStateLoading = true;
                        return;
                    case MSG_LOAD_FINISH:
                        mStateLoading = false;
                        if (mLoadingListener != null) {
                            boolean loadingFailed =
                                    (mFailedVersion != MediaObject.INVALID_DATA_VERSION);
                            mLoadingListener.onLoadingFinished(loadingFailed);
                        }
                        return;
                    case MSG_SETLOAD_START:
                        mStateLoading = true;
                        if (mLoadingListener != null){
                            mLoadingListener.onLoadingStarted();
                        }
                        return;
                    case MSG_SETLOAD_FINISH:
                        if (mSetTimeDataLoaderFinished == true
                            && mTyAlbumSetTimeDataLoader != null){
                            mTyAlbumSetTimeDataLoader.pause();
                            mTyAlbumSetTimeDataLoader = null;
                            if (mSourceSet.getSubMediaSetCount() > 0){
                                mSource = mSourceSet.getSubMediaSet(0);
                                mFailedVersion = MediaObject.INVALID_DATA_VERSION;
                                resumeOwner();
                            }else{
                                mMainHandler.sendEmptyMessage(MSG_LOAD_FINISH);
                            }
                        }else{
                            mMainHandler.sendEmptyMessage(MSG_LOAD_FINISH);
                        }
                        return;
                }
            }
        };

        mTyAlbumSetTimeDataLoader = new TyAlbumSetTimeDataLoader(mSourceSet, mMainHandler);
    }

    public void resume() {
        mDataManager.addFilterDatachangeListener(mFilterDatachangeListener);
        if (mSetTimeDataLoaderFinished == false
            && mTyAlbumSetTimeDataLoader != null){
            mTyAlbumSetTimeDataLoader.resume();
        }else{
            resumeOwner();
        }
    }
    
    public void resumeOwner() {
        if (mSource != null){
            mSource.addContentListener(mSourceListener);
            mReloadTask = new ReloadTask();
            mReloadTask.start();
        }
    }

    public void pause() {
        mDataManager.removeFilterDatachangeListener(mFilterDatachangeListener);
        if (mTyAlbumSetTimeDataLoader != null){
            mTyAlbumSetTimeDataLoader.pause();
            mSetTimeDataLoaderFinished = false;
        }else{
            pauseOwner();
        }
    }
    
    public void pauseOwner() {
        if (mSource != null){
            if (mReloadTask != null){
                mReloadTask.terminate();
                mReloadTask = null;
            }
            mSource.removeContentListener(mSourceListener);
        }
    }

    public MediaItem get(int index) {
        if (!isActive(index)) {
            return null;
        }
        return mData[index % mData.length];
    }

    public int getActiveStart() {
        return mActiveStart;
    }

    public boolean isActive(int index) {
        return index >= mActiveStart && index < mActiveEnd;
    }

    public int size() {
        return mSize;
    }

    public int timeSlotSize() {
        return mTimeslotInfo == null ? 0 : mTimeslotInfo.size();
    }
    
    //taoxj modify
    public LinkedHashMap<String, Integer> timeslotInfo() {
        return mTimeslotInfo;
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

    // Returns the index of the MediaItem with the given path or
    // -1 if the path is not cached
    public int findItem(Path id) {
        for (int i = mContentStart; i < mContentEnd; i++) {
            MediaItem item = mData[i % DATA_CACHE_SIZE];
            if (item != null && id == item.getPath()) {
                return i;
            }
        }
        return -1;
    }

    private void clearSlot(int slotIndex) {
        mData[slotIndex] = null;
        mItemVersion[slotIndex] = MediaObject.INVALID_DATA_VERSION;
        mSetVersion[slotIndex] = MediaObject.INVALID_DATA_VERSION;
    }

    private void setContentWindow(int contentStart, int contentEnd) {
        if (contentStart == mContentStart && contentEnd == mContentEnd) return;
        int end = mContentEnd;
        int start = mContentStart;

        // We need change the content window before calling reloadData(...)
        synchronized (this) {
            mContentStart = contentStart;
            mContentEnd = contentEnd;
        }
        long[] itemVersion = mItemVersion;
        long[] setVersion = mSetVersion;
        if (contentStart >= end || start >= contentEnd) {
            for (int i = start, n = end; i < n; ++i) {
                clearSlot(i % DATA_CACHE_SIZE);
            }
        } else {
            for (int i = start; i < contentStart; ++i) {
                clearSlot(i % DATA_CACHE_SIZE);
            }
            for (int i = contentEnd, n = end; i < n; ++i) {
                clearSlot(i % DATA_CACHE_SIZE);
            }
        }
        if (mReloadTask != null) mReloadTask.notifyDirty();
    }

    public void setActiveWindow(int start, int end) {
        if (start == mActiveStart && end == mActiveEnd) return;

        Utils.assertTrue(start <= end
                && end - start <= mData.length && end <= mSize);

        int length = mData.length;
        mActiveStart = start;
        mActiveEnd = end;

        // If no data is visible, keep the cache content
        if (start == end) return;

        int contentStart = Utils.clamp((start + end) / 2 - length / 2,
                0, Math.max(0, mSize - length));
        int contentEnd = Math.min(contentStart + length, mSize);
        if (mContentStart > start || mContentEnd < end
                || Math.abs(contentStart - mContentStart) > MIN_LOAD_COUNT) {
            setContentWindow(contentStart, contentEnd);
        }
    }

    private class MySourceListener implements ContentListener {
        @Override
        public void onContentDirty() {
            if (mReloadTask != null && mNeedChange) mReloadTask.notifyDirty();
        }
    }

    public void setDataListener(DataListener listener) {
        mDataListener = listener;
    }

    public void setLoadingListener(LoadingListener listener) {
        mLoadingListener = listener;
    }

    private <T> T executeAndWait(Callable<T> callable) {
        FutureTask<T> task = new FutureTask<T>(callable);
        mMainHandler.sendMessage(
                mMainHandler.obtainMessage(MSG_RUN_OBJECT, task));
        try {
            return task.get();
        } catch (InterruptedException e) {
            return null;
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    private static class UpdateInfo {
        public long version;
        public int reloadStart;
        public int reloadCount;

        public int size;
        public ArrayList<MediaItem> items;
        //use to test data
        //taoxj modify
        public LinkedHashMap<String, Integer> timeslotInfo;
        
        @Override
        public String toString() {
            String strItems = "";
            if (items != null && items.size() > 0){
                strItems = "[";
                for (MediaItem item : items){
                    strItems += "("+item+")";
                }
                strItems += "]";
            }else{
                strItems = "null";
            }
            
            String strtimeslotInfo = "";
            if (timeslotInfo != null && timeslotInfo.size() > 0){
                strtimeslotInfo = "[";
                for (Map.Entry<String, Integer> item : timeslotInfo.entrySet()){
                    strtimeslotInfo += "("+item.getKey()+","+item.getValue()+")";
                }
                strtimeslotInfo += "]";
            }else{
                strtimeslotInfo = "null";
            }
            return "UpdateInfo{version=" + version 
                + " reloadStart=" + reloadStart
                + " reloadCount=" + reloadCount
                + " size=" + size
                + " items=" + strItems
                + " timeslotInfo=" + strtimeslotInfo
                + "}";
        }
    }

    private class GetUpdateInfo implements Callable<UpdateInfo> {
        private final long mVersion;

        public GetUpdateInfo(long version) {
            mVersion = version;
        }

        @Override
        public UpdateInfo call() throws Exception {
            if (mFailedVersion == mVersion) {
                // previous loading failed, return null to pause loading
                return null;
            }
            UpdateInfo info = new UpdateInfo();
            long version = mVersion;
            info.version = mSourceVersion;
            info.size = mSize;
            info.timeslotInfo = mTimeslotInfo;
            long setVersion[] = mSetVersion;
            for (int i = mContentStart, n = mContentEnd; i < n; ++i) {
                int index = i % DATA_CACHE_SIZE;
                if (setVersion[index] != version) {
                    info.reloadStart = i;
                    info.reloadCount = Math.min(MAX_LOAD_COUNT, n - i);
                    return info;
                }
            }
            return mSourceVersion == mVersion ? null : info;
        }
    }

    private class UpdateContent implements Callable<Void> {

        private UpdateInfo mUpdateInfo;

        public UpdateContent(UpdateInfo info) {
            mUpdateInfo = info;
        }

        @Override
        public Void call() throws Exception {
            UpdateInfo info = mUpdateInfo;
            mSourceVersion = info.version;
            if (mSize != info.size || !timeslotEquals(mTimeslotInfo, info.timeslotInfo)) {
                //TYRD:changjj add for delete funtin begin
                boolean directReturn = false;
                if(mSize < info.size){
                    directReturn = true;
                }
                mSize = info.size;
                mTimeslotInfo = info.timeslotInfo;
                if (mDataListener != null) mDataListener.onSizeChanged(mSize, mTimeslotInfo);
                if (mContentEnd > mSize) mContentEnd = mSize;
                if (mActiveEnd > mSize) mActiveEnd = mSize;
                if(directReturn) return null;
                //TYRD:changjj add for delete funtin end
            }

            ArrayList<MediaItem> items = info.items;

            mFailedVersion = MediaObject.INVALID_DATA_VERSION;
            if ((items == null) || items.isEmpty()) {
                if (info.reloadCount > 0) {
                    mFailedVersion = info.version;
                    Log.d(TAG, "loading failed: " + mFailedVersion);
                }
                return null;
            }
            int start = Math.max(info.reloadStart, mContentStart);
            int end = Math.min(info.reloadStart + items.size(), mContentEnd);

            //TYRD:changjj add for delete function begin
            boolean bFlag = false;
            for (int i = start; i < end; ++i) {
                int index = i % DATA_CACHE_SIZE;
                mSetVersion[index] = info.version;
                MediaItem updateItem = items.get(i - info.reloadStart);
                long itemVersion = updateItem.getDataVersion();
                if (mItemVersion[index] != itemVersion) {
                    mItemVersion[index] = itemVersion;
                    mData[index] = updateItem;
                    if (mDataListener != null && i >= mActiveStart && i < mActiveEnd) {
                        mDataListener.onContentChanged(i);
                        bFlag = true;
                    }
                }
            
            }
            if(bFlag){
                mDataListener.onLoadFinished();
            }
            //TYRD:changjj add for delete function end
            return null;
        }
    }

    /*
     * The thread model of ReloadTask
     *      *
     * [Reload Task]       [Main Thread]
     *       |                   |
     * getUpdateInfo() -->       |           (synchronous call)
     *     (wait) <----    getUpdateInfo()
     *       |                   |
     *   Load Data               |
     *       |                   |
     * updateContent() -->       |           (synchronous call)
     *     (wait)          updateContent()
     *       |                   |
     *       |                   |
     */
    private class ReloadTask extends Thread {

        private volatile boolean mActive = true;
        private volatile boolean mDirty = true;
        private boolean mIsLoading = false;

        private void updateLoading(boolean loading) {
            if (mIsLoading == loading) return;
            mIsLoading = loading;
            mMainHandler.sendEmptyMessage(loading ? MSG_LOAD_START : MSG_LOAD_FINISH);
        }

        @Override
        public void run() {
            setName("TyAlbumTimeDL ReloadTask");
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);

            boolean updateComplete = false;
            while (mActive) {
                synchronized (this) {
                    if (mActive && !mDirty && updateComplete) {
                        updateLoading(false);
                        if (mFailedVersion != MediaObject.INVALID_DATA_VERSION) {
                            Log.d(TAG, "reload pause");
                        }
                        Utils.waitWithoutInterrupt(this);
                        if (mActive && (mFailedVersion != MediaObject.INVALID_DATA_VERSION)) {
                            Log.d(TAG, "reload resume");
                        }
                        continue;
                    }
                    mDirty = false;
                }
                updateLoading(true);
                long version = mSource.reload();
                UpdateInfo info = executeAndWait(new GetUpdateInfo(version));
                updateComplete = info == null;
                if (updateComplete) continue;
                if (info.version != version) {
                    info.size = mSource.getMediaItemCount();
                    info.timeslotInfo = mSource.getTimeslotInfo();
                    info.version = version;
                }
                if (info.reloadCount > 0) {
                    info.items = mSource.getMediaItem(info.reloadStart, info.reloadCount);
                }
                executeAndWait(new UpdateContent(info));
            }
            updateLoading(false);
        }

        public synchronized void notifyDirty() {
            mDirty = true;
            notifyAll();
        }

        public synchronized void terminate() {
            mActive = false;
            notifyAll();
        }
    }    

    public int getActiveEnd() {
        return mActiveEnd;
    }

    private class TyAlbumSetTimeDataLoader {
        private static final int INDEX_NONE = -1;

        private final MediaSet mSource;
        private long mSourceVersion = MediaObject.INVALID_DATA_VERSION;

        private ReloadTask mReloadTask;

        private final Handler mMainHandler;

        public TyAlbumSetTimeDataLoader(MediaSet albumSet, Handler handler) {
            mSource = Utils.checkNotNull(albumSet);
            mMainHandler = handler;
        }

        public void pause() {
            if (mReloadTask != null){
                mReloadTask.terminate();
                mReloadTask = null;
            }
        }

        public void resume() {
            mReloadTask = new ReloadTask();
            mReloadTask.start();
        }

        private class UpdateSetInfo {
            public long version;
            public int index;
            public int size;
        }

        private class ReloadTask extends Thread {
            private volatile boolean mActive = true;
            private volatile boolean mIsLoading = false;

            private void updateLoading(boolean loading) {
                if (mIsLoading == loading) return;
                mIsLoading = loading;
                mMainHandler.sendEmptyMessage(loading ? MSG_SETLOAD_START : MSG_SETLOAD_FINISH);
            }

            @Override
            public void run() {
                setName("TyAlbumSetTimeDL ReloadTask");
                Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);

                boolean updateComplete = false;
                while (mActive) {
                    synchronized (this) {
                        if (mActive && updateComplete) {
                            if (!mSource.isLoading()) {
                                mSetTimeDataLoaderFinished = true;
                                updateLoading(false);
                            }
                            Utils.waitWithoutInterrupt(this);
                            continue;
                        }
                    }
                    updateLoading(true);
                    long version = mSource.reload();
                    UpdateSetInfo info = executeAndWait(new GetUpdateSetInfo(version));
                    updateComplete = info == null;
                    if (updateComplete) continue;
                    if (info.version != version) {
                        info.version = version;
                        info.size = mSource.getSubMediaSetCount();
                        if (info.index >= info.size) {
                            info.index = INDEX_NONE;
                        }
                    }
                    mFailedVersion = MediaObject.INVALID_DATA_VERSION;
                    if (info.index != INDEX_NONE) {
                        if (mSource.getSubMediaSet(info.index) == null) continue;
                    }
                    executeAndWait(new UpdateContent(info));
                }
                updateLoading(false);
            }

            public synchronized void terminate() {
                mActive = false;
                notifyAll();
            }
        }
        
        private class GetUpdateSetInfo implements Callable<UpdateSetInfo> {
        
            private final long mVersion;
        
            public GetUpdateSetInfo(long version) {
                mVersion = version;
            }
        
            @Override
            public UpdateSetInfo call() throws Exception {
                if (mSourceVersion == mVersion) return null;
                
                UpdateSetInfo info = new UpdateSetInfo();
                info.version = mSourceVersion;
                info.index = 0;
                info.size = mSize;
                
                return info;
            }
        }
        
        private class UpdateContent implements Callable<Void> {
            private final UpdateSetInfo mUpdateSetInfo;
        
            public UpdateContent(UpdateSetInfo info) {
                mUpdateSetInfo = info;
            }
        
            @Override
            public Void call() {
                // Avoid notifying listeners of status change after pause
                // Otherwise gallery will be in inconsistent state after resume.
                if (mReloadTask == null) return null;
                UpdateSetInfo info = mUpdateSetInfo;
                mSourceVersion = info.version;
                mFailedVersion = mSourceVersion;
                return null;
            }
        }
    }

    private class MyFilterDatachangeListener implements FilterDatachangeListener{
        @Override
       public synchronized void onFilterDataChange(boolean bChange) {
            mNeedChange = bChange;
            if((mReloadTask != null) && mNeedChange){
               mReloadTask.notifyDirty();
            }
       }
    }
}
