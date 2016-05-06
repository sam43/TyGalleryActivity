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

import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;

import com.android.gallery3d.api.ApiRequestListener;
import com.android.gallery3d.app.GalleryApp;
import com.android.gallery3d.app.StitchingChangeListener;
import com.android.gallery3d.common.Utils;
import com.android.gallery3d.data.MediaObject.PanoramaSupportCallback;
import com.android.gallery3d.data.MediaSet.ItemConsumer;
import com.android.gallery3d.data.MediaSource.PathId;
import com.android.gallery3d.database.PictureDAO;
import com.android.gallery3d.database.PictureDAOImpl;
import com.android.gallery3d.parser.DeleteParser;
import com.android.gallery3d.parser.DeleteResp;
import com.android.gallery3d.picasasource.PicasaSource;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.WeakHashMap;
//TY wb034 add begin for  New Design Gallery
import com.android.gallery3d.R;
import com.android.gallery3d.ui.TyAlbumSetListSlidingWindow.Listener;
import com.android.gallery3d.data.BucketHelper.BucketEntry;
import com.android.gallery3d.util.GalleryUtils;
import android.content.SharedPreferences.Editor;
import java.util.Collections;
//TY wb034 add end for  New Design Gallery
/* TIANYU: yuxin add begin for New Design Gallery*/
import android.content.SharedPreferences;
import com.android.gallery3d.util.MediaSetUtils;
import com.android.gallery3d.util.NetworkUtils;
import com.android.gallery3d.volley.GalleryApi;
import com.android.gallery3d.volley.InstagramPlusException;
import com.android.gallery3d.volley.InstagramRestClient;
import com.android.gallery3d.volley.RequestParams;
import com.android.volley.Request;
/* TIANYU: yuxin add end for New Design Gallery*/

// DataManager manages all media sets and media items in the system.
//
// Each MediaSet and MediaItem has a unique 64 bits id. The most significant
// 32 bits represents its parent, and the least significant 32 bits represents
// the self id. For MediaSet the self id is is globally unique, but for
// MediaItem it's unique only relative to its parent.
//
// To make sure the id is the same when the MediaSet is re-created, a child key
// is provided to obtainSetId() to make sure the same self id will be used as
// when the parent and key are the same. A sequence of child keys is called a
// path. And it's used to identify a specific media set even if the process is
// killed and re-created, so child keys should be stable identifiers.

public class DataManager implements StitchingChangeListener {
    public static final int INCLUDE_IMAGE = 1;
    public static final int INCLUDE_VIDEO = 2;
    public static final int INCLUDE_ALL = INCLUDE_IMAGE | INCLUDE_VIDEO;
    public static final int INCLUDE_LOCAL_ONLY = 4;
    public static final int INCLUDE_LOCAL_IMAGE_ONLY =
            INCLUDE_LOCAL_ONLY | INCLUDE_IMAGE;
    public static final int INCLUDE_LOCAL_VIDEO_ONLY =
            INCLUDE_LOCAL_ONLY | INCLUDE_VIDEO;
    public static final int INCLUDE_LOCAL_ALL_ONLY =
            INCLUDE_LOCAL_ONLY | INCLUDE_IMAGE | INCLUDE_VIDEO;

    public static final int INCLUDE_LOCAL_CAMERA = 100;

    // Any one who would like to access data should require this lock
    // to prevent concurrency issue.
    public static final Object LOCK = new Object();

    //taoxj add begin
    public static PictureDAO dao;
    //taoxj add end

    public static DataManager from(Context context) {
        GalleryApp app = (GalleryApp) context.getApplicationContext();
        return app.getDataManager();
    }

    private static final String TAG = "DataManager";

    // This is the path for the media set seen by the user at top level.
    private static final String TOP_SET_PATH = "/combo/{/local/all,/picasa/all}";

    private static final String TOP_IMAGE_SET_PATH = "/combo/{/local/image,/picasa/image}";

    private static final String TOP_VIDEO_SET_PATH =
            "/combo/{/local/video,/picasa/video}";

    private static final String TOP_LOCAL_SET_PATH = "/local/all";

    private static final String TOP_LOCAL_IMAGE_SET_PATH = "/local/image";

    private static final String TOP_LOCAL_VIDEO_SET_PATH = "/local/video";

    private static final String TOP_LOCAL_CAMERA_SET_PATH = "/local/all/" + MediaSetUtils.CAMERA_BUCKET_ID;

    public static final Comparator<MediaItem> sDateTakenComparator =
            new DateTakenComparator();

    private static class DateTakenComparator implements Comparator<MediaItem> {
        @Override
        public int compare(MediaItem item1, MediaItem item2) {
            return -Utils.compare(item1.getDateInMs(), item2.getDateInMs());
        }
    }

    private final Handler mDefaultMainHandler;

    private GalleryApp mApplication;
    private int mActiveCount = 0;

    private HashMap<Uri, NotifyBroker> mNotifierMap =
            new HashMap<Uri, NotifyBroker>();


    private HashMap<String, MediaSource> mSourceMap =
            new LinkedHashMap<String, MediaSource>();

    /* TIANYU: yuxin add begin for New Design Gallery*/
    private HashMap<String, String> mItemsHide = new HashMap<String, String>();
    private SharedPreferences mHidePreferences;
    /* TIANYU: yuxin add end for New Design Gallery*/
    //TY wb034 add begin for  New Design Gallery
    public static boolean isCollectFileUpdate = false;
    public int mCollectBucketId;

    private SharedPreferences mCollectPreferences;
    private ArrayList<String> mMediaItemsCollect = new ArrayList<String>();
    private ArrayList<CollectListener> mCollectListeners = new ArrayList<CollectListener>();
    private ArrayList<FilterDatachangeListener> mFilterDatachangeListeners = new ArrayList<FilterDatachangeListener>();
    //TY wb034 add end for  New Design Gallery

    public DataManager(GalleryApp application) {
        mApplication = application;
        mDefaultMainHandler = new Handler(application.getMainLooper());
        dao = new PictureDAOImpl(mApplication.getAndroidContext());
    }

    public synchronized void initializeSourceMap() {
        if (!mSourceMap.isEmpty()) return;

        // the order matters, the UriSource must come last
        //taoxj modify for remove video begin
        addSource(new LocalSource(mApplication));
        addSource(new PicasaSource(mApplication));
        addSource(new ComboSource(mApplication));
        addSource(new ClusterSource(mApplication));
        addSource(new FilterSource(mApplication));
        addSource(new SecureSource(mApplication));
        addSource(new UriSource(mApplication));
        addSource(new SnailSource(mApplication));
        //taoxj modify for remove video end
        //TY wb034 add begin for  New Design Gallery
        tyLoadHidesItem();
        initCollectSp();
        //TY wb034 add end for  New Design Gallery
        if (mActiveCount > 0) {
            for (MediaSource source : mSourceMap.values()) {
                source.resume();
            }
        }
    }

    public String getTopSetPath(int typeBits) {

        switch (typeBits) {
            case INCLUDE_IMAGE: return TOP_IMAGE_SET_PATH;
            case INCLUDE_VIDEO: return TOP_VIDEO_SET_PATH;
            case INCLUDE_ALL: return TOP_SET_PATH;
            case INCLUDE_LOCAL_IMAGE_ONLY: return TOP_LOCAL_IMAGE_SET_PATH;
            case INCLUDE_LOCAL_VIDEO_ONLY: return TOP_LOCAL_VIDEO_SET_PATH;
            case INCLUDE_LOCAL_ALL_ONLY: return TOP_LOCAL_SET_PATH;
            case INCLUDE_LOCAL_CAMERA: return TOP_LOCAL_CAMERA_SET_PATH;
            default: throw new IllegalArgumentException();
        }
    }

    // open for debug
    void addSource(MediaSource source) {
        if (source == null) return;
        mSourceMap.put(source.getPrefix(), source);
    }

    // A common usage of this method is:
    // synchronized (DataManager.LOCK) {
    //     MediaObject object = peekMediaObject(path);
    //     if (object == null) {
    //         object = createMediaObject(...);
    //     }
    // }
    public MediaObject peekMediaObject(Path path) {
        return path.getObject();
    }

    public MediaObject getMediaObject(Path path) {
        synchronized (LOCK) {
            MediaObject obj = path.getObject();
            if (obj != null) return obj;
             android.util.Log.i("data","path = " + path + " path.getPrefix() = " + path.getPrefix());
            MediaSource source = mSourceMap.get(path.getPrefix());
            if (source == null) {
                Log.w(TAG, "cannot find media source for path: " + path);
                return null;
            }

            try {
                MediaObject object = source.createMediaObject(path);
                if (object == null) {
                    Log.w(TAG, "cannot create media object: " + path);
                }
                return object;
            } catch (Throwable t) {
                Log.w(TAG, "exception in creating media object: " + path, t);
                return null;
            }
        }
    }

    public MediaObject getMediaObject(String s) {
        return getMediaObject(Path.fromString(s));
    }

    public MediaSet getMediaSet(Path path) {
        return (MediaSet) getMediaObject(path);
    }

    public MediaSet getMediaSet(String s) {
        return (MediaSet) getMediaObject(s);
    }

    public MediaSet[] getMediaSetsFromString(String segment) {
        String[] seq = Path.splitSequence(segment);
        int n = seq.length;
        MediaSet[] sets = new MediaSet[n];
        for (int i = 0; i < n; i++) {
            sets[i] = getMediaSet(seq[i]);
        }
        return sets;
    }

    // Maps a list of Paths to MediaItems, and invoke consumer.consume()
    // for each MediaItem (may not be in the same order as the input list).
    // An index number is also passed to consumer.consume() to identify
    // the original position in the input list of the corresponding Path (plus
    // startIndex).
    public void mapMediaItems(ArrayList<Path> list, ItemConsumer consumer,
            int startIndex) {
        HashMap<String, ArrayList<PathId>> map =
                new HashMap<String, ArrayList<PathId>>();

        // Group the path by the prefix.
        int n = list.size();
        for (int i = 0; i < n; i++) {
            Path path = list.get(i);
            String prefix = path.getPrefix();
            ArrayList<PathId> group = map.get(prefix);
            if (group == null) {
                group = new ArrayList<PathId>();
                map.put(prefix, group);
            }
            group.add(new PathId(path, i + startIndex));
        }

        // For each group, ask the corresponding media source to map it.
        for (Entry<String, ArrayList<PathId>> entry : map.entrySet()) {
            String prefix = entry.getKey();
            MediaSource source = mSourceMap.get(prefix);
            source.mapMediaItems(entry.getValue(), consumer);
        }
    }

    // The following methods forward the request to the proper object.
    public int getSupportedOperations(Path path) {
        return getMediaObject(path).getSupportedOperations();
    }

    public void getPanoramaSupport(Path path, PanoramaSupportCallback callback) {
        getMediaObject(path).getPanoramaSupport(callback);
    }

    public void delete(Path path) {
        //TY wb034 20150130 add begin for tygallery
        String key = path.getSuffix();
        String value = path.toString();
        if(mMediaItemsCollect.contains(path.toString())){
            removeToCollectsArray(key , value);
        }
        if(key.equals(String.valueOf(mCollectBucketId))){
            ArrayList<String> paths = new ArrayList<String>();
            paths.addAll(mMediaItemsCollect);
            for(String str:paths){
                removeToCollectsArray(Path.fromString(str).getSuffix(), str);
            }
           mMediaItemsCollect.clear();
        }
        //TY wb034 20150130 add end for tygallery
        getMediaObject(path).delete();
        //taoxj add forcloud delete and modify database begin
        delete(getContentUri(path).toString());
        //taoxj add for cloud delete and modify database end

    }
    //taoxj add for cloud delete begin
    /**
     * cloud delete
     */
    public void delete(String uri){
         //get image id
        final String pictureId = dao.queryByUri(uri).getPictureId();
        //only delete local picture
        if(pictureId == null || "".equals(pictureId)){
            dao.deletePictureByUri(uri);
            return;
        }
         if(!NetworkUtils.isConnected()){
            dao.modifyDeleteStatus(pictureId);
            return ;
        }
        RequestParams rp = new RequestParams("{'telephone':'18610277013','pictureId':'" +  pictureId + "'}");
        InstagramRestClient.getInstance().postData(Request.Method.POST, GalleryApi.delete, rp, new DeleteParser(), new ApiRequestListener<DeleteResp>() {

            @Override
            public void onPreExecute() {
                // TODO Auto-generated method stub
            }

            @Override
            public void onComplete(int statusCode, DeleteResp value) {
                boolean isDeleted = value.isSuccess();
                if (isDeleted) {
                    Log.i("koala", "delete pictureId success");
                    //delete database item
                    dao.deletePicture(pictureId);
                } else {
                    //modify database
                    Log.i("koala", "modify picture");
                    dao.modifyDeleteStatus(pictureId);
                }
            }

            @Override
            public void onException(InstagramPlusException e) {
                // TODO Auto-gener

            }});
    }
    //taoxj add for cloud delete end

    //TY zhencc add for New Design Gallery
    public long getFileSize(Path path) {
        return getMediaObject(path).getFileSize();
    }
    //TY zhencc end for New Design Gallery
    
    /* TIANYU: yuxin add begin for New Design Gallery*/
    public boolean hide(boolean ishide, Path path) {
        MediaObject parentMediaObject = getMediaObject(path.getParent());
        if (parentMediaObject == null){
            return false;
        }
        if (ishide){
            addToHidesArray(path.getSuffix(), path.toString());
        }else{
            removeToHidesArray(path.getSuffix());
        }
        parentMediaObject.hide(ishide);
        return true;
    }

    public HashMap<String, String> getHides() {
        HashMap<String, String> hides = null;
        synchronized (mItemsHide) {
            int size = mItemsHide.size();
            if (size > 0){
                hides = new HashMap<String, String>(mItemsHide);
            }else{
                hides = new HashMap<String, String>();
            }
        }
        return hides;
    }

    private void addToHidesArray(String key, String value){
        synchronized (mItemsHide) {
            SharedPreferences.Editor editor = mHidePreferences.edit();
            editor.putString(key, value);
            editor.commit();
            mItemsHide.put(key, value);
        }
    }
    
    public void removeToHidesArray(String key){
        synchronized (mItemsHide) {
            SharedPreferences.Editor editor = mHidePreferences.edit();
            editor.remove(key);
            editor.commit();
            mItemsHide.remove(key);
        }
    }
    
    public int getHideCount() {
        return mItemsHide.size();
    }

    private void tyLoadHidesItem(){
        mHidePreferences = mApplication.getAndroidContext().getSharedPreferences("TYHidesSp", 0);
        synchronized (mItemsHide) {
            HashMap<String, String> map = (HashMap<String, String>)mHidePreferences.getAll();          
            if (map != null){
                mItemsHide = map;
            }
        }
    }

    private void initCollectSp(){
        mCollectPreferences = mApplication.getAndroidContext().getSharedPreferences("TYCollectsSp", 0);
        synchronized (mMediaItemsCollect) {
            HashMap<String, String> map = (HashMap<String, String>)mCollectPreferences.getAll();
            if (map != null){
                for(Entry<String,String> entry : map.entrySet()){
                    String path = entry.getValue();
                    mMediaItemsCollect.add(path);
                }
            }
        }
        mCollectBucketId = GalleryUtils.getPhoneSourceBucketId(mApplication.getAndroidContext(), "/"+"MyFavourteite".hashCode());
        updateCollectsFile(true);
    }
    
    private void addToCollectsArray(String key, String value){
        synchronized (mMediaItemsCollect) {
            SharedPreferences.Editor editor = mCollectPreferences.edit();
            editor.putString(key, value);
            editor.commit();
            mMediaItemsCollect.add(value);
            updateCollectsFile(true);
        }
    }
    
    private void removeToCollectsArray(String key,String value){
        synchronized (mMediaItemsCollect) {
            SharedPreferences.Editor editor = mCollectPreferences.edit();
            editor.remove(key);
            editor.commit();
            mMediaItemsCollect.remove(value);
            updateCollectsFile(false);
        }
    }
    public  boolean Collect(boolean isCollect, Path path) {
        MediaObject parentMediaObject = getMediaObject(path.getParent().getParent());
        if (parentMediaObject == null){
            return false;
        }
        if (!isCollect){
           removeToCollectsArray(path.getSuffix(),path.toString());
        }else{
           addToCollectsArray(path.getSuffix(), path.toString());
        }
        parentMediaObject.collect(isCollect);
        return true;
    }
    public int getCollectCount() {
        return mMediaItemsCollect.size();
      }
    public boolean isAleadyCollect(Path path) {
        return mMediaItemsCollect.contains(path.toString());
      }
    public MediaItem[] getCollectItem(int max){
        int count =  getCollectCount();
        int temp = count>max?max:count;
        MediaItem[] covers = new MediaItem[temp];
        for (int i = 0; i < covers.length; i++) {
            MediaItem item =(MediaItem) getMediaObject(mMediaItemsCollect.get(i));
             covers[i] = item;
        }
        return covers;
    }
    public ArrayList<MediaItem> getCollects(){
         ArrayList<MediaItem> list = new ArrayList<MediaItem>();
         for (String path:mMediaItemsCollect) {
            MediaItem item =(MediaItem) getMediaObject(path);
            list.add(item);
        }
         Collections.sort(list, sDateComparator);
        return list;
    }
    private static final Comparator<MediaItem> sDateComparator =
            new DateComparator();

    private static class DateComparator implements Comparator<MediaItem> {
        @Override
        public int compare(MediaItem item1, MediaItem item2) {
            return -Utils.compare(item1.getDateInMs(), item2.getDateInMs());
        }
    }

    public synchronized MediaItem getMediaItem(int index){
        int count =  getCollectCount();
        if(index >= count) {
            return null;
        }
        return (MediaItem) getMediaObject(mMediaItemsCollect.get(index));
    }
    public ArrayList<MediaItem> getCollectItem(int start ,int loadcount){
        int count =  getCollectCount();
        int temp = count<(start+loadcount)?count:(start+loadcount);
        ArrayList<MediaItem> infos= new ArrayList<MediaItem>();
        for (int i = start; i < temp; i++) {
            MediaItem item =(MediaItem) getMediaObject(mMediaItemsCollect.get(i));
            infos.add(item);
        }
       return infos;
    }
    
    public BucketHelper.BucketEntry getCollectBucketEntry(){
        return new BucketHelper.BucketEntry(mCollectBucketId, mApplication.getResources().getString(R.string.ty_collect));
    }
    
    public void cleanArrays(){
        mMediaItemsCollect.clear();
        mCollectPreferences = null;
    }
    
    public void updateCollectsFile(boolean isAdd){
        synchronized (mCollectListeners){
            int listenerSize = mCollectListeners.size();
            if (listenerSize == 0) return;

            int size = getCollectCount();
            for (CollectListener listener : mCollectListeners){
                listener.onCollectChanged(size, isAdd);
            }
        }
    }
    //TY wb034 add end for  New Design Gallery
    /* TIANYU: yuxin add begin for New Design Gallery*/
    public void rotate(Path path, int degrees) {
        getMediaObject(path).rotate(degrees);
    }

    public Uri getContentUri(Path path) {
        return getMediaObject(path).getContentUri();
    }

    public int getMediaType(Path path) {
        return getMediaObject(path).getMediaType();
    }

    public Path findPathByUri(Uri uri, String type) {
        if (uri == null) return null;
        for (MediaSource source : mSourceMap.values()) {
            Path path = source.findPathByUri(uri, type);
            if (path != null) return path;
        }
        return null;
    }

    public Path getDefaultSetOf(Path item) {
        MediaSource source = mSourceMap.get(item.getPrefix());
        return source == null ? null : source.getDefaultSetOf(item);
    }

    // Returns number of bytes used by cached pictures currently downloaded.
    public long getTotalUsedCacheSize() {
        long sum = 0;
        for (MediaSource source : mSourceMap.values()) {
            sum += source.getTotalUsedCacheSize();
        }
        return sum;
    }

    // Returns number of bytes used by cached pictures if all pending
    // downloads and removals are completed.
    public long getTotalTargetCacheSize() {
        long sum = 0;
        for (MediaSource source : mSourceMap.values()) {
            sum += source.getTotalTargetCacheSize();
        }
        return sum;
    }

    public void registerChangeNotifier(Uri uri, ChangeNotifier notifier) {
        //taoxj add
        Log.i("koala","registerChangeNotifier" + uri.toString());
        NotifyBroker broker = null;
        synchronized (mNotifierMap) {
            broker = mNotifierMap.get(uri);
            if (broker == null) {
                broker = new NotifyBroker(mDefaultMainHandler);
                mApplication.getContentResolver()
                        .registerContentObserver(uri, true, broker);
                mNotifierMap.put(uri, broker);
            }
        }
        broker.registerNotifier(notifier);
    }

    public void resume() {
        if (++mActiveCount == 1) {
            for (MediaSource source : mSourceMap.values()) {
                source.resume();
            }
        }
    }

    public void pause() {
        if (--mActiveCount == 0) {
            for (MediaSource source : mSourceMap.values()) {
                source.pause();
            }
        }
    }

    private static class NotifyBroker extends ContentObserver {
        private WeakHashMap<ChangeNotifier, Object> mNotifiers =
                new WeakHashMap<ChangeNotifier, Object>();

        public NotifyBroker(Handler handler) {
            super(handler);
        }

        public synchronized void registerNotifier(ChangeNotifier notifier) {
            mNotifiers.put(notifier, null);
        }

        @Override
        public synchronized void onChange(boolean selfChange) {
            for(ChangeNotifier notifier : mNotifiers.keySet()) {
                notifier.onChange(selfChange);
                //taoxj add
                /*
                04-27 17:24:47.089 19758-19790/? I/koala: registerChangeNotifiercontent://media/external/images/media
04-27 17:24:47.089 19758-19790/? I/koala: registerChangeNotifiercontent://media/external/video/media
04-27 17:24:47.089 19758-19790/? I/koala: registerChangeNotifiercontent://media/external/images/media
04-27 17:24:47.089 19758-19790/? I/koala: registerChangeNotifiercontent://media/external/video/media
                 */
            }
        }
    }

    @Override
    public void onStitchingQueued(Uri uri) {
        // Do nothing.
    }

    @Override
    public void onStitchingResult(Uri uri) {
        Path path = findPathByUri(uri, null);
        if (path != null) {
            MediaObject mediaObject = getMediaObject(path);
            if (mediaObject != null) {
                mediaObject.clearCachedPanoramaSupport();
            }
        }
    }

    @Override
    public void onStitchingProgress(Uri uri, int progress) {
        // Do nothing.
    }
   //TY wb034 20150121 add begin for tygallery
    public static interface CollectListener {
        public void onCollectChanged(int size, boolean isAdd);
    }
    public void addCollectListener(CollectListener listener) {
        synchronized (mCollectListeners){
            if (listener != null){
                mCollectListeners.add(listener);
            }
        }
    }
    
    public void removeCollectListener(CollectListener listener) {
        synchronized (mCollectListeners){
            mCollectListeners.remove(listener);
        }
    }

    public static interface FilterDatachangeListener {
        public void onFilterDataChange(boolean bChange);
    }
    public void addFilterDatachangeListener(FilterDatachangeListener listener) {
        synchronized (mFilterDatachangeListeners){
            if (listener != null){
                mFilterDatachangeListeners.add(listener);
            }
        }
    }
    
    public void removeFilterDatachangeListener(FilterDatachangeListener listener) {
        synchronized (mFilterDatachangeListeners){
            mFilterDatachangeListeners.remove(listener);
        }
    }

    public void setNeedChange(boolean bChange){
        synchronized (mFilterDatachangeListeners){
            int listenerSize = mFilterDatachangeListeners.size();
            if (listenerSize == 0) return;

            for (FilterDatachangeListener listener : mFilterDatachangeListeners){
                listener.onFilterDataChange(bChange);
            }
        }
    }
}
