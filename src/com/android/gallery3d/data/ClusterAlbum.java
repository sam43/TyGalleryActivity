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

import java.util.ArrayList;
//TIANYU: yuxin add begin for New Design Gallery
import java.util.LinkedHashMap;
//TIANYU: yuxin add begin for New Design Gallery

public class ClusterAlbum extends MediaSet implements ContentListener {
    @SuppressWarnings("unused")
    private static final String TAG = "ClusterAlbum";
    private ArrayList<Path> mPaths = new ArrayList<Path>();
    private String mName = "";
    private DataManager mDataManager;
    private MediaSet mClusterAlbumSet;
    private MediaItem mCover;
    //TIANYU: yuxin add begin for New Design Gallery
    private LinkedHashMap<String, Integer> mTimeslotInfo; //taoxj modify
    //TIANYU: yuxin add end for New Design Gallery

    public ClusterAlbum(Path path, DataManager dataManager,
            MediaSet clusterAlbumSet) {
        super(path, nextVersionNumber());
        mDataManager = dataManager;
        mClusterAlbumSet = clusterAlbumSet;
        mClusterAlbumSet.addContentListener(this);
    }

    public void setCoverMediaItem(MediaItem cover) {
        mCover = cover;
    }

    @Override
    public MediaItem getCoverMediaItem() {
        return mCover != null ? mCover : super.getCoverMediaItem();
    }

    void setMediaItems(ArrayList<Path> paths) {
        mPaths = paths;
    }

    ArrayList<Path> getMediaItems() {
        return mPaths;
    }

    public void setName(String name) {
        mName = name;
    }

    @Override
    public String getName() {
        return mName;
    }

    @Override
    public int getMediaItemCount() {
        return mPaths.size();
    }

    @Override
    public ArrayList<MediaItem> getMediaItem(int start, int count) {
        return getMediaItemFromPath(mPaths, start, count, mDataManager);
    }

    public static ArrayList<MediaItem> getMediaItemFromPath(
            ArrayList<Path> paths, int start, int count,
            DataManager dataManager) {
        if (start >= paths.size()) {
            return new ArrayList<MediaItem>();
        }
        int end = Math.min(start + count, paths.size());
        ArrayList<Path> subset = new ArrayList<Path>(paths.subList(start, end));
        final MediaItem[] buf = new MediaItem[end - start];
        ItemConsumer consumer = new ItemConsumer() {
            @Override
            public void consume(int index, MediaItem item) {
                buf[index] = item;
            }
        };
        dataManager.mapMediaItems(subset, consumer, 0);
        //ArrayList<MediaItem> result = new ArrayList<MediaItem>(end - start);
        ArrayList<MediaItem> result = new ArrayList<MediaItem>();
        for (int i = 0; i < buf.length; i++) {
            if (buf[i] != null) result.add(buf[i]);
        }
        return result;
    }

    @Override
    protected int enumerateMediaItems(ItemConsumer consumer, int startIndex) {
        mDataManager.mapMediaItems(mPaths, consumer, startIndex);
        return mPaths.size();
    }

    @Override
    public int getTotalMediaItemCount() {
        return mPaths.size();
    }

    @Override
    public long reload() {
        if (mClusterAlbumSet.reload() > mDataVersion) {
            mDataVersion = nextVersionNumber();
        }
        return mDataVersion;
    }

    @Override
    public void onContentDirty() {
        notifyContentChanged();
    }

    @Override
    public int getSupportedOperations() {
        return SUPPORT_SHARE | SUPPORT_DELETE | SUPPORT_INFO;
    }

    @Override
    public void delete() {
        ItemConsumer consumer = new ItemConsumer() {
            @Override
            public void consume(int index, MediaItem item) {
                if (item != null && (item.getSupportedOperations() & SUPPORT_DELETE) != 0) {
                    item.delete();
                }
            }
        };
        mDataManager.mapMediaItems(mPaths, consumer, 0);
    }

    @Override
    public boolean isLeafAlbum() {
        return true;
    }

    // Clear mPaths to make it empty
    public void clear() {
        mPaths.clear();
    }

    //TIANYU: yuxin add begin for New Design Gallery
    public void setTimeslotInfo(LinkedHashMap<String, Integer> timeslotInfo){ //taoxj modify
        mTimeslotInfo = timeslotInfo;
    }

    @Override
    public LinkedHashMap<String, Integer> getTimeslotInfo(){ //taoxj modify
        if (mTimeslotInfo == null) {
            return null;
        }
        return new LinkedHashMap<String, Integer>(mTimeslotInfo);
    }
    //TIANYU: yuxin add end for New Design Gallery
}
