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
import android.net.Uri;

import com.android.gallery3d.app.GalleryApp;
//TY zhencc add for New Design Gallery
import com.android.gallery3d.util.GalleryUtils;
//TY zhencc end for New Design Gallery

import java.util.ArrayList;
import java.util.HashSet;
//TIANYU: yuxin add begin for New Design Gallery
import java.util.Calendar;
//TIANYU: yuxin add end for New Design Gallery

public class ClusterAlbumSet extends MediaSet implements ContentListener {
    @SuppressWarnings("unused")
    private static final String TAG = "ClusterAlbumSet";
    private GalleryApp mApplication;
    private MediaSet mBaseSet;
    private int mKind;
    private ArrayList<ClusterAlbum> mAlbums = new ArrayList<ClusterAlbum>();
    private boolean mFirstReloadDone;
    //TIANYU: yuxin add begin for New Design Gallery
    private int mTodayTime;
    //TIANYU: yuxin add end for New Design Gallery

    public ClusterAlbumSet(Path path, GalleryApp application,
            MediaSet baseSet, int kind) {
        super(path, INVALID_DATA_VERSION);
        mApplication = application;
        mBaseSet = baseSet;
        mKind = kind;
        baseSet.addContentListener(this);
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
        return mBaseSet.getName();
    }

    @Override
    public long reload() {
        if (mBaseSet.reload() > mDataVersion) {
            if (mFirstReloadDone) {
                //updateClustersContents();
                updateClusters();
            } else {
                updateClusters();
                //mFirstReloadDone = true;
            }
            mDataVersion = nextVersionNumber();
        }else if (mKind == ClusterSource.CLUSTER_TY_ALBUMSET_TIME
            && mAlbums.size() > 0 && mAlbums.get(0).getMediaItemCount()> 0){
            Calendar todayCal = Calendar.getInstance();
            int y = todayCal.get(Calendar.YEAR);
            int m = todayCal.get(Calendar.MONTH);
            int d = todayCal.get(Calendar.DAY_OF_MONTH);
            int todayTime = (y << 14) + (m << 7) + d;
            if (mTodayTime != todayTime){
                updateClusters();
            }
        }
        return mDataVersion;
    }

    @Override
    public void onContentDirty() {
        notifyContentChanged();
    }

    private synchronized void updateClusters() {
        /*
        for (ClusterAlbum entry : mAlbums) {
            entry.clear();
        }
        */
        updateEmptyClusters();

        mAlbums.clear();
        Clustering clustering;
        Context context = mApplication.getAndroidContext();
        switch (mKind) {
            case ClusterSource.CLUSTER_ALBUMSET_TIME:
                clustering = new TimeClustering(context);
                break;
            case ClusterSource.CLUSTER_ALBUMSET_LOCATION:
                clustering = new LocationClustering(context);
                break;
            case ClusterSource.CLUSTER_ALBUMSET_TAG:
                clustering = new TagClustering(context);
                break;
            case ClusterSource.CLUSTER_ALBUMSET_FACE:
                clustering = new FaceClustering(context);
                break;
            /*TIANYU: yuxin add begin for New Design Gallery*/
            case ClusterSource.CLUSTER_TY_ALBUMSET_TIME:
                //TY wb034 20150306 add params:mAppliction for tygallery
                clustering = new TyTimeClustering(mApplication,context);
                //TY wb034 20150306 add params:mAppliction for tygallery
                break;
            /*TIANYU: yuxin add end for New Design Gallery*/
            default: /* CLUSTER_ALBUMSET_SIZE */
                clustering = new SizeClustering(context);
                break;
        }

        clustering.run(mBaseSet);
        int n = clustering.getNumberOfClusters();
        DataManager dataManager = mApplication.getDataManager();
        for (int i = 0; i < n; i++) {
            Path childPath;
            String childName = clustering.getClusterName(i);
            if (mKind == ClusterSource.CLUSTER_ALBUMSET_TAG) {
                childPath = mPath.getChild(Uri.encode(childName));
            } else if (mKind == ClusterSource.CLUSTER_ALBUMSET_SIZE) {
                long minSize = ((SizeClustering) clustering).getMinSize(i);
                childPath = mPath.getChild(minSize);
            } else if (mKind == ClusterSource.CLUSTER_ALBUMSET_TIME) {
                childPath = mPath.getChild(childName.toLowerCase().hashCode());
            /*TIANYU: yuxin add begin for New Design Gallery*/
            } else if (mKind == ClusterSource.CLUSTER_TY_ALBUMSET_TIME) {
                childPath = mPath.getChild(i);
                mTodayTime = ((TyTimeClustering)clustering).getTodayTime();
            /*TIANYU: yuxin add end for New Design Gallery*/
            } else {
                childPath = mPath.getChild(i);
            }

            ClusterAlbum album;
            synchronized (DataManager.LOCK) {
                album = (ClusterAlbum) dataManager.peekMediaObject(childPath);
                if (album == null) {
                    album = new ClusterAlbum(childPath, dataManager, this);
                }
            }
            album.setMediaItems(clustering.getCluster(i));
            album.setName(childName);
            album.setCoverMediaItem(clustering.getClusterCover(i));
            album.setTimeslotInfo(clustering.getTimeslotInfo());
            mAlbums.add(album);
        }
    }

    private void updateClustersContents() {
        final HashSet<Path> existing = new HashSet<Path>();
        mBaseSet.enumerateTotalMediaItems(new MediaSet.ItemConsumer() {
            @Override
            public void consume(int index, MediaItem item) {
                existing.add(item.getPath());
            }
        });

        int n = mAlbums.size();

        // The loop goes backwards because we may remove empty albums from
        // mAlbums.
        for (int i = n - 1; i >= 0; i--) {
            ArrayList<Path> oldPaths = mAlbums.get(i).getMediaItems();
            ArrayList<Path> newPaths = new ArrayList<Path>();
            int m = oldPaths.size();
            for (int j = 0; j < m; j++) {
                Path p = oldPaths.get(j);
                if (existing.contains(p)) {
                    newPaths.add(p);
                }
            }
            mAlbums.get(i).setMediaItems(newPaths);
            if (newPaths.isEmpty()) {
                mAlbums.remove(i);
            }
        }
    }

    private void updateEmptyClusters() {
        final HashSet<Path> existing = new HashSet<Path>();
        mBaseSet.enumerateTotalMediaItems(new MediaSet.ItemConsumer() {
            public void consume(int index, MediaItem item) {
                if (item == null) return;
                existing.add(item.getPath());
            }
        });

        int n = mAlbums.size();

        // The loop goes backwards because we may set empty to inexistent cluster albums.
        for (int i = n - 1; i >= 0; i--) {
            ArrayList<Path> oldPaths = mAlbums.get(i).getMediaItems();
            ArrayList<Path> newPaths = new ArrayList<Path>();
            int m = oldPaths.size();
            for (int j = 0; j < m; j++) {
                Path p = oldPaths.get(j);
                if (existing.contains(p)) {
                    newPaths.add(p);
                }
            }
            if (newPaths.isEmpty()) {
                mAlbums.get(i).setMediaItems(new ArrayList<Path>());
            }
        }
    }
}
