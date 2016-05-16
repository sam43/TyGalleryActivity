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

import android.app.Application;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;

import com.android.gallery3d.cache.ImageCacheManager;
import com.android.gallery3d.data.DataManager;
import com.android.gallery3d.data.DownloadCache;
import com.android.gallery3d.data.ImageCacheService;
import com.android.gallery3d.datatask.LoginAsyncTask;
import com.android.gallery3d.gadget.WidgetUtils;
import com.android.gallery3d.gadget.TYWidgetUtils;  //TIANYU hanpengzhe 20130723 add for typhoto_widget
import com.android.gallery3d.picasasource.PicasaSource;
import com.android.gallery3d.util.GalleryUtils;
import com.android.gallery3d.util.LightCycleHelper;
import com.android.gallery3d.util.ThreadPool;
import com.android.gallery3d.util.UsageStatistics;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;

import java.io.File;

public class GalleryAppImpl extends Application implements GalleryApp {

    private static final String DOWNLOAD_FOLDER = "download";
    private static final long DOWNLOAD_CAPACITY = 64 * 1024 * 1024; // 64M
    private static GalleryAppImpl sGalleryAppImpl;

    private ImageCacheService mImageCacheService;
    private Object mLock = new Object();
    private DataManager mDataManager;
    private ThreadPool mThreadPool;
    private DownloadCache mDownloadCache;

    //taoxj add begin
    private static int DISK_IMAGECACHE_SIZE = 1024*1024*10;
    private static Bitmap.CompressFormat DISK_IMAGECACHE_COMPRESS_FORMAT = Bitmap.CompressFormat.PNG;
    private static int DISK_IMAGECACHE_QUALITY = 100;
    public RequestQueue requestQueue;
    //taoxj add end
    @Override
    public void onCreate() {
        super.onCreate();
        initializeAsyncTask();
        GalleryUtils.initialize(this);
        WidgetUtils.initialize(this);
        TYWidgetUtils.initialize(this);  //TIANYU hanpengzhe 20130723 add for typhoto_widget
        PicasaSource.initialize(this);
        UsageStatistics.initialize(this);
        sGalleryAppImpl = this;
        createImageCache();
        //taoxj remove
        new LoginAsyncTask().execute();
    }

    @Override
    public Context getAndroidContext() {
        return this;
    }

    public static Context getContext() {
        return sGalleryAppImpl;
    }

    @Override
    public synchronized DataManager getDataManager() {
        if (mDataManager == null) {
            mDataManager = new DataManager(this);
            mDataManager.initializeSourceMap();
        }
        return mDataManager;
    }


    @Override
    public ImageCacheService getImageCacheService() {
        // This method may block on file I/O so a dedicated lock is needed here.
        synchronized (mLock) {
            if (mImageCacheService == null) {
                mImageCacheService = new ImageCacheService(getAndroidContext());
            }
            return mImageCacheService;
        }
    }

    @Override
    public synchronized ThreadPool getThreadPool() {
        if (mThreadPool == null) {
            mThreadPool = new ThreadPool();
        }
        return mThreadPool;
    }

    @Override
    public synchronized DownloadCache getDownloadCache() {
        if (mDownloadCache == null) {
            File cacheDir = new File(getExternalCacheDir(), DOWNLOAD_FOLDER);

            if (!cacheDir.isDirectory()) cacheDir.mkdirs();

            if (!cacheDir.isDirectory()) {
                throw new RuntimeException(
                        "fail to create: " + cacheDir.getAbsolutePath());
            }
            mDownloadCache = new DownloadCache(this, cacheDir, DOWNLOAD_CAPACITY);
        }
        return mDownloadCache;
    }

    private void initializeAsyncTask() {
        // AsyncTask class needs to be loaded in UI thread.
        // So we load it here to comply the rule.
        try {
            Class.forName(AsyncTask.class.getName());
        } catch (ClassNotFoundException e) {
        }
    }
    //TY wb034 20150130 add begin for tygallery
    @Override
    public void onTerminate() {
        if (mDataManager != null) {
            mDataManager.cleanArrays();
        }
        super.onTerminate();
    }
    //TY wb034 20150130 add end for tygallery

    //taoxj add begin
    public RequestQueue getRequestQueue() {
        if (requestQueue == null) {
            requestQueue = Volley.newRequestQueue(getApplicationContext());
        }
        return requestQueue;
    }
    public <T> void addToRequestQueue(Request<T> req, String tag) {
        // set the default tag if tag is empty
        getRequestQueue().add(req);
    }

    public <T> void addToRequestQueue(Request<T> req) {
        getRequestQueue().add(req);
    }

    public void cancelPendingRequests(Object tag) {
        if (requestQueue != null) {
            requestQueue.cancelAll(tag);
        }
    }

    /**
     * Create the image cache. Uses Memory Cache by default. Change to Disk for a Disk based LRU implementation.
     */
    private void createImageCache(){
        final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
        DISK_IMAGECACHE_SIZE = maxMemory / 8;
        ImageCacheManager.getInstance().init(this,
                "gallery3d"
                , DISK_IMAGECACHE_SIZE
                , DISK_IMAGECACHE_COMPRESS_FORMAT
                , DISK_IMAGECACHE_QUALITY
                , ImageCacheManager.CacheType.MEMORY);
    }
    //taoxj add end
}
