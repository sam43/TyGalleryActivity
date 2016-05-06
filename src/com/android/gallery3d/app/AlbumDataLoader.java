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

package com.android.gallery3d.app;

import android.os.Handler;
import android.os.Message;
import android.os.Process;

import com.android.gallery3d.common.Utils;
import com.android.gallery3d.data.ContentListener;
//TY WB034 20150130 add begin for tygallery
import com.android.gallery3d.data.DataManager;
import com.android.gallery3d.data.DataManager.CollectListener;
//TY WB034 20150130 add end for tygallery
import com.android.gallery3d.data.MediaItem;
import com.android.gallery3d.data.MediaObject;
import com.android.gallery3d.data.MediaSet;
import com.android.gallery3d.data.Path;
import com.android.gallery3d.data.DataManager.FilterDatachangeListener;
import com.android.gallery3d.ui.SynchronizedHandler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

public class AlbumDataLoader {
    @SuppressWarnings("unused")
    private static final String TAG = "AlbumDataAdapter";
    private static final int DATA_CACHE_SIZE = 1000;

    private static final int MSG_LOAD_START = 1;
    private static final int MSG_LOAD_FINISH = 2;
    private static final int MSG_RUN_OBJECT = 3;

    private static final int MIN_LOAD_COUNT = 32;
    private static final int MAX_LOAD_COUNT = 64;

    private final MediaItem[] mData;
    private final long[] mItemVersion;
    private final long[] mSetVersion;

    public static interface DataListener {
        public void onContentChanged(int index);
        public void onSizeChanged(int size);
        //TYRD:changjj add for load finish begin
        public void onLoadFinished();
        //TYRD:changjj add for load finish end
    }

    private int mActiveStart = 0;
    private int mActiveEnd = 0;

    private int mContentStart = 0;
    private int mContentEnd = 0;

    private final MediaSet mSource;
    private long mSourceVersion = MediaObject.INVALID_DATA_VERSION;

    private final Handler mMainHandler;
    private int mSize = 0;

    private DataListener mDataListener;
    private MySourceListener mSourceListener = new MySourceListener();
    private LoadingListener mLoadingListener;

    private ReloadTask mReloadTask;
    // the data version on which last loading failed
    private long mFailedVersion = MediaObject.INVALID_DATA_VERSION;
    //TY wb034 20150129 add begin foy thgallery
    private DataManager mDataManager;
    private MyDataManagerListener mDataManagerListener = new MyDataManagerListener();
    //TY wb034 20150129 add end for tygallery
    private MyFilterDatachangeListener mFilterDatachangeListener = new MyFilterDatachangeListener();
    private boolean mNeedChange = true;
    
    public AlbumDataLoader(GalleryContext context, MediaSet mediaSet) {
        mSource = mediaSet;
        mDataManager = context.getDataManager();
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
                        if (mLoadingListener != null) mLoadingListener.onLoadingStarted();
                        return;
                    case MSG_LOAD_FINISH:
                        if (mLoadingListener != null) {
                            boolean loadingFailed =
                                    (mFailedVersion != MediaObject.INVALID_DATA_VERSION);
                            mLoadingListener.onLoadingFinished(loadingFailed);
                        }
                        return;
                }
            }
        };
    }

    public void resume() {
        mDataManager.addFilterDatachangeListener(mFilterDatachangeListener);
        mDataManager.addCollectListener(mDataManagerListener);
        mSource.addContentListener(mSourceListener);
        mReloadTask = new ReloadTask();
        mReloadTask.start();
    }

    public void pause() {
        mReloadTask.terminate();
        mReloadTask = null;
        mDataManager.removeCollectListener(mDataManagerListener);
        mSource.removeContentListener(mSourceListener);
        mDataManager.removeFilterDatachangeListener(mFilterDatachangeListener);
    }

    public MediaItem get(int index) {
        if (!isActive(index)) {
            return mSource.getMediaItem(index, 1).get(0);
        }
        //TY wb034 20150129 add begin foy thgallery
        if(isCollectLoader()){
             return mDataManager.getMediaItem(index);
        }
        //TY wb034 20150129 add end for tygallery          
        return mData[index % mData.length];
    }

    public int getActiveStart() {
        return mActiveStart;
    }

    public boolean isActive(int index) {
        return index >= mActiveStart && index < mActiveEnd;
    }

    public int size() {
        //TY wb034 20150206 add begin for tygallery
        if(isCollectLoader()) return mDataManager.getCollectCount();
        //TY wb034 20150206 add end for tygallery
        return mSize;
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
        //TY wb034 20150130 add begin for tygallery
        if(isCollectLoader()){
            mSize = mDataManager.getCollectCount();
        }
        //TY wb034 20150130 add end for tygallery
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
            long setVersion[] = mSetVersion;
            //TY wb034 20150130 add begin for tygallery
            if(isCollectLoader()){
                 return info.size == mDataManager.getCollectCount()?null:info;
           //TY wb034 20150130 add add for tygallery
            }
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
            if (mSize != info.size) {
                //TYRD:changjj modify for delete function begin
                boolean directReturn = false;
                if(mSize < info.size){
                    directReturn = true;
                }
                mSize = info.size;
                if (mDataListener != null) mDataListener.onSizeChanged(mSize);
                if (mContentEnd > mSize) mContentEnd = mSize;
                if (mActiveEnd > mSize) mActiveEnd = mSize;
                if(directReturn) return null;
                //TYRD:changjj modify for delete function begin
                //return null;
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
            
            //TYRD:changjj modify for delete function begin
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
                //TY wb034 20150306 add begin for tygallery
                if(isCollectLoader()){
                    info.size = mDataManager.getCollectCount(); 
                    info.items = mDataManager.getCollectItem(0,info.size);
                    mDirty = true;
                //TY wb034 20150306 add end for tygallery
                }else{
                    if (info.version != version) {              
                       info.size = mSource.getMediaItemCount();
                        info.version = version;
                    }
                    if (info.reloadCount > 0) {
                         info.items = mSource.getMediaItem(info.reloadStart, info.reloadCount);
                         //TY wb034 20150306 add end for tygallery
                    }
                 
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
    //TY wb034 20150129 add begin foy thgallery
    private class MyDataManagerListener implements CollectListener{
        @Override
       public  synchronized void onCollectChanged(int size,boolean isAdd) {
               if(isCollectLoader()){
                    mDataListener.onSizeChanged(size);
                    for (int i = 0; i < size; i++) {
                        mDataListener.onContentChanged(i);
                    }
                    if(size==0){
                        mMainHandler.sendEmptyMessage(MSG_LOAD_FINISH);
                    }
                    //TYRD:changjj add for function begin
                    mDataListener.onLoadFinished();
                    //TYRD:changjj add for function end
               }
       }  
    }
    public boolean isCollectLoader(){
         return mSource.getBucketId()==mDataManager.mCollectBucketId;
    }
    //TY wb034 20150129 add end for tygallery  
    
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
