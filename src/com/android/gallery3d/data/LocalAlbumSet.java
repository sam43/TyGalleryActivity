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

package com.android.gallery3d.data;

import android.net.Uri;
import android.os.Handler;
import android.provider.MediaStore.Images;
import android.provider.MediaStore.Video;
import android.provider.MediaStore.Files;

import com.android.gallery3d.R;
import com.android.gallery3d.app.GalleryApp;
import com.android.gallery3d.data.BucketHelper.BucketEntry;
import com.android.gallery3d.util.Future;
import com.android.gallery3d.util.FutureListener;
//TY zhencc add for New Design Gallery
import com.android.gallery3d.util.GalleryUtils;
import android.content.Context;
import android.content.SharedPreferences;
//TY zhencc end for New Design Gallery
import com.android.gallery3d.util.MediaSetUtils;
import com.android.gallery3d.util.ThreadPool;
import com.android.gallery3d.util.ThreadPool.JobContext;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
/* TIANYU: yuxin add begin for New Design Gallery*/
import java.util.Map;
import java.util.HashMap;
/* TIANYU: yuxin add end for New Design Gallery*/
import java.util.Iterator;


// LocalAlbumSet lists all image or video albums in the local storage.
// The path should be "/local/image", "local/video" or "/local/all"
public class LocalAlbumSet extends MediaSet
        implements FutureListener<ArrayList<MediaSet>> {
    @SuppressWarnings("unused")
    private static final String TAG = "LocalAlbumSet";

    public static final Path PATH_ALL = Path.fromString("/local/all");
    public static final Path PATH_IMAGE = Path.fromString("/local/image");
    public static final Path PATH_VIDEO = Path.fromString("/local/video");

    private static final Uri[] mWatchUris =
        {Images.Media.EXTERNAL_CONTENT_URI, Video.Media.EXTERNAL_CONTENT_URI,
          //  Files.getMtpObjectsUri("external")};
          getExternalUri()};
    private final GalleryApp mApplication;
    private final int mType;
    private ArrayList<MediaSet> mAlbums = new ArrayList<MediaSet>();
    private final ChangeNotifier mNotifier;
    private final String mName;
    private final Handler mHandler;
    private boolean mIsLoading;

    private Future<ArrayList<MediaSet>> mLoadTask;
    private ArrayList<MediaSet> mLoadBuffer;
    /* TIANYU: yuxin add begin for New Design Gallery*/
    private int mTyMode;
    /* TIANYU: yuxin add end for New Design Gallery*/

    public LocalAlbumSet(Path path, GalleryApp application) {
        super(path, nextVersionNumber());
        mApplication = application;
        mHandler = new Handler(application.getMainLooper());
        mType = getTypeFromPath(path);
        mNotifier = new ChangeNotifier(this, mWatchUris, application);
        mName = application.getResources().getString(
                R.string.set_label_local_albums);
    }

    private static int getTypeFromPath(Path path) {
        String name[] = path.split();
        if (name.length < 2) {
            throw new IllegalArgumentException(path.toString());
        }
        return getTypeFromString(name[1]);
    }

    @Override
    public MediaSet getSubMediaSet(int index) {
        return mAlbums.get(index);
    }

    @Override
    public int getSubMediaSetCount() {
        return mAlbums.size();
    }

    @Override
    public String getName() {
        return mName;
    }

    //TY zhencc add for New Design Gallery
    public boolean drop(int from, int to) {
    	if(from == to || mAlbums == null){
			return false;
		}
    	
    	int formBucketId = mAlbums.get(from).getBucketId();
    	int toBucketId = mAlbums.get(to).getBucketId();
    	
    	MediaSet data = mAlbums.remove(from);
    	mAlbums.add(to, data);
    	
    	return dropSharedPre(formBucketId, toBucketId);
    }
    
    private boolean dropSharedPre(int fromBucketId, int toBucketId) {
    	ArrayList<Integer> albumsBucketIdSharedPre = GalleryUtils.getAlbumsBucketIdFromSharedPre(
    			mApplication.getAndroidContext());
    	
    	if(albumsBucketIdSharedPre == null){
    		ArrayList<Integer> albumsBucketId = new ArrayList<Integer>();
    		for (MediaSet album : mAlbums) {
    			albumsBucketId.add(album.getBucketId());
    		}
    		GalleryUtils.clearAlbumsSharedPre(mApplication.getAndroidContext());
    		return GalleryUtils.saveAlbumsBucketIdToSharedPre(mApplication.getAndroidContext(), albumsBucketId);
    	}
    	
    	int indexFrom = albumsBucketIdSharedPre.indexOf(fromBucketId);
    	int indexTo = albumsBucketIdSharedPre.indexOf(toBucketId);
    	int bucketId = albumsBucketIdSharedPre.remove(indexFrom);
    	albumsBucketIdSharedPre.add(indexTo, bucketId);
    	
    	GalleryUtils.clearAlbumsSharedPre(mApplication.getAndroidContext());
    	boolean suc =  GalleryUtils.saveAlbumsBucketIdToSharedPre(mApplication.getAndroidContext(), albumsBucketIdSharedPre);
        fakeChange();
        return suc;
    }
    //TY zhencc end for New Design Gallery
    
    private static int findBucket(BucketEntry entries[], int bucketId) {
        for (int i = 0, n = entries.length; i < n; ++i) {
            if (entries[i].bucketId == bucketId) return i;
        }
        return -1;
    }
    
    //TY zhencc add for new Design Gallery
    private static int findBucket(ArrayList<MediaSet> albums, int bucketId) {
        for (int i = 0, n = albums.size(); i < n; ++i) {
            if (albums.get(i).getBucketId() == bucketId) return i;
        }
        return -1;
    }
    //TY zhencc end for New Design Gallery


    private class AlbumsLoader implements ThreadPool.Job<ArrayList<MediaSet>> {

        @Override
        @SuppressWarnings("unchecked")
        public ArrayList<MediaSet> run(JobContext jc) {
            // Note: it will be faster if we only select media_type and bucket_id.
            //       need to test the performance if that is worth
            BucketEntry[] entries = BucketHelper.loadBucketEntries(
                    jc, mApplication.getContentResolver(), mType);

            if (jc.isCancelled()) return null;

            int offsetDrag = 0;
            ArrayList<Integer> albumsBucketId = GalleryUtils.getAlbumsBucketIdFromSharedPre(
            		mApplication.getAndroidContext());
            if(albumsBucketId != null){
            	for(int i=0; i<albumsBucketId.size(); i++){
            		int indexDrag = findBucket(entries, albumsBucketId.get(i));
                    if (indexDrag != -1) {
                        circularShiftRight(entries, offsetDrag++, indexDrag);
                    }
                }
            }
            
            GalleryUtils.clearAlbumsSharedPre(mApplication.getAndroidContext());
            GalleryUtils.saveAlbumsToSharedPre(mApplication.getAndroidContext(), entries);

            ArrayList<MediaSet> albums = new ArrayList<MediaSet>();
            DataManager dataManager = mApplication.getDataManager();           
            GalleryUtils.mEntriesBucketIdList.clear();
            GalleryUtils.mEntriesBucketMap.clear();
            GalleryUtils.mEntriesAlbumFilePathList.clear();
            HashMap<String, String> mHides = dataManager.getHides();
            
            for (BucketEntry entry : entries) {
                if (mTyMode == MediaObject.TY_HIDE_MODE){
                    if (mHides.get(String.valueOf(entry.bucketId)) != null) {
                         mHides.remove(String.valueOf(entry.bucketId));
                         continue;
                    }
                    if (entry.bucketId == MediaSetUtils.CAMERA_BUCKET_ID){
                        continue;
                    }
                }else if (mTyMode == MediaObject.TY_SHOW_MODE){
                    if (mHides.get(String.valueOf(entry.bucketId)) == null) {
                       continue;
                    } else {
                       mHides.remove(String.valueOf(entry.bucketId));
                       if (entry.bucketId == MediaSetUtils.CAMERA_BUCKET_ID){
                           continue;
                       }
                    }
                }else {
                    if (mHides.get(String.valueOf(entry.bucketId)) != null) {
                         mHides.remove(String.valueOf(entry.bucketId));
                         continue;
                    }
                }
            
                MediaSet album = getLocalAlbum(dataManager,
                        mType, mPath, entry.bucketId, entry.bucketName);
                albums.add(album);
                GalleryUtils.mEntriesBucketIdList.add(entry.bucketId);
                GalleryUtils.mEntriesBucketMap.put(entry.bucketId, entry.bucketName);
                GalleryUtils.mEntriesAlbumFilePathList.add(entry.albumFilePath);
            }

            //TY liuyuchuan add begin for PROD103710950
            Iterator iterator = mHides.keySet().iterator();
            while(iterator.hasNext()){
                String key = (String)iterator.next();
                dataManager.removeToHidesArray(key);
            }
            //TY liuyuchuan add end for PROD103710950
            
            boolean showCollect = false;
            if (mTyMode == MediaObject.TY_HIDE_MODE || mTyMode == MediaObject.TY_SHOW_MODE){
                showCollect = false;
            }else {
                if(dataManager.getCollectCount() > 0){
                    showCollect = true;
                }
            }

            if (showCollect){
                BucketHelper.BucketEntry collectBucketEntry = dataManager.getCollectBucketEntry();
                MediaSet album = getLocalAlbum(dataManager,
                     MEDIA_TYPE_ALL, 
                     Path.fromString(dataManager.getTopSetPath(DataManager.INCLUDE_LOCAL_ALL_ONLY)),
                     collectBucketEntry.bucketId, 
                     collectBucketEntry.bucketName);
                albums.add(album);
                GalleryUtils.mEntriesBucketIdList.add(dataManager.mCollectBucketId);
                GalleryUtils.mEntriesBucketMap.put(collectBucketEntry.bucketId, collectBucketEntry.bucketName);
                GalleryUtils.mEntriesAlbumFilePathList.add(null); 
            }
                        
            int offset = 0;
            GalleryUtils.mFrontAlbumCount = 0;
            int index = findBucket(albums, MediaSetUtils.CAMERA_BUCKET_ID);
            if (index != -1) {
                GalleryUtils.mFrontAlbumCount++;
                circularShiftRight(albums, offset++, index);
            }
            index = findBucket(albums, dataManager.mCollectBucketId);
            if (index != -1) {
                GalleryUtils.mFrontAlbumCount++;
                circularShiftRight(albums, offset++, index);
            }

            return albums;
        }
    }

    private MediaSet getLocalAlbum(
            DataManager manager, int type, Path parent, int id, String name) {
        synchronized (DataManager.LOCK) {
            Path path = parent.getChild(id);
            MediaObject object = manager.peekMediaObject(path);
            if (object != null) return (MediaSet) object;
            switch (type) {
                case MEDIA_TYPE_IMAGE:
                    return new LocalAlbum(path, mApplication, id, true, name);
                case MEDIA_TYPE_VIDEO:
                    //taoxj modify begin
                    //return new LocalAlbum(path, mApplication, id, false, name);
                    return null;
                   //taoxj modify end
                case MEDIA_TYPE_ALL:
                    Comparator<MediaItem> comp = DataManager.sDateTakenComparator;
                    return new LocalMergeAlbum(mApplication, //TY zhencc add paramater "Context" for New Design Gallery
                    		path, comp, new MediaSet[] {
                            getLocalAlbum(manager, MEDIA_TYPE_IMAGE, PATH_IMAGE, id, name)/*,
                            getLocalAlbum(manager, MEDIA_TYPE_VIDEO, PATH_VIDEO, id, name)*/}, id);
            }
            throw new IllegalArgumentException(String.valueOf(type));
        }
    }

    @Override
    public synchronized boolean isLoading() {
        return mIsLoading;
    }

    @Override
    // synchronized on this function for
    //   1. Prevent calling reload() concurrently.
    //   2. Prevent calling onFutureDone() and reload() concurrently
    public synchronized long reload() {
            /*TY wb034 20150130 add begin for tygallery:DataManager.isCollectFileUpdate*/ 
        if (mNotifier.isDirty()||DataManager.isCollectFileUpdate) {
            /*TY wb034 20150130 add end for tygallery:DataManager.isCollectFileUpdate*/ 
            if (mLoadTask != null) mLoadTask.cancel();
            mIsLoading = true;
            mLoadTask = mApplication.getThreadPool().submit(new AlbumsLoader(), this);
            /*TY wb034 20150130 add begin for tygallery*/            
            DataManager.isCollectFileUpdate =false;
            /*TY wb034 20150130 add end for tygallery*/           
        }
        if (mLoadBuffer != null) {
            mAlbums = mLoadBuffer;
            mLoadBuffer = null;
            for (MediaSet album : mAlbums) {
                album.reload();
            }
            mDataVersion = nextVersionNumber();
        }
        return mDataVersion;
    }

    @Override
    public synchronized void onFutureDone(Future<ArrayList<MediaSet>> future) {
        if (mLoadTask != future) return; // ignore, wait for the latest task
        mLoadBuffer = future.get();
        mIsLoading = false;
        if (mLoadBuffer == null) mLoadBuffer = new ArrayList<MediaSet>();
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                notifyContentChanged();
            }
        });
    }

    // For debug only. Fake there is a ContentObserver.onChange() event.
    void fakeChange() {
        mNotifier.fakeChange();
    }

    // Circular shift the array range from a[i] to a[j] (inclusive). That is,
    // a[i] -> a[i+1] -> a[i+2] -> ... -> a[j], and a[j] -> a[i]
    private static <T> void circularShiftRight(T[] array, int i, int j) {
        T temp = array[j];
        for (int k = j; k > i; k--) {
            array[k] = array[k - 1];
        }
        array[i] = temp;
    }
    
    //TY zhencc add begin for New Design Gallery
    private static void circularShiftRight(ArrayList<MediaSet> albums, int i, int j) {
        albums.add(i, albums.remove(j));
    }
    //TY zhencc add end for New Design Gallery

    /* TIANYU: yuxin add begin for New Design Gallery*/
    @Override
    public void changeTyMode(int tymode) {
        mTyMode = tymode;
        fakeChange();
    }
    
    @Override
    public void hide(boolean ishide) {
        fakeChange();
    }
    /* TIANYU: yuxin add end for New Design Gallery*/
    /* TIANYU: wb034 add begin for New Design Gallery*/
    public void collect(boolean isCollect) {
        DataManager.isCollectFileUpdate =true;
        fakeChange();
    }
    /* TIANYU: wb034 add end for New Design Gallery*/
    //taoxj add begin
    public static Uri getExternalUri(){
        Uri uri = null;
        try {
             Method m = Files.class.getDeclaredMethod("getMtpObjectsUri", String.class);
             uri = (Uri)m.invoke(null,new Object[]{"external"});
        } catch (Exception e) {
            e.printStackTrace();
        }
        return uri;
    }
    //taoxj add end
}
