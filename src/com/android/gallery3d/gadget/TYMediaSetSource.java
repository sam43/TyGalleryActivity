/*
 *TIANYU hanpengzhe 20130723 add for typhoto_widget base on MediaSetSource.java 
 */

package com.android.gallery3d.gadget;

import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Binder;

import com.android.gallery3d.common.Utils;
import com.android.gallery3d.data.ContentListener;
import com.android.gallery3d.data.MediaItem;
import com.android.gallery3d.data.MediaObject;
import com.android.gallery3d.data.MediaSet;

import java.util.ArrayList;
import java.util.Arrays;

public class TYMediaSetSource implements WidgetSource, ContentListener {
    private static final int CACHE_SIZE = 32;

    private static final String TAG = "MediaSetSource";

    private MediaSet mSource;
    private MediaItem mCache[] = new MediaItem[CACHE_SIZE];
    private int mCacheStart;
    private int mCacheEnd;
    private long mSourceVersion = MediaObject.INVALID_DATA_VERSION;

    private ContentListener mContentListener;

    public TYMediaSetSource(MediaSet source) {
        mSource = Utils.checkNotNull(source);
        mSource.addContentListener(this);
    }

    @Override
    public void close() {
        mSource.removeContentListener(this);
    }

    private void ensureCacheRange(int index) {
        if (index >= mCacheStart && index < mCacheEnd) return;

        long token = Binder.clearCallingIdentity();
        try {
            mCacheStart = index;
            ArrayList<MediaItem> items = mSource.getMediaItem(mCacheStart, CACHE_SIZE);
            mCacheEnd = mCacheStart + items.size();
            items.toArray(mCache);
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    @Override
    public synchronized Uri getContentUri(int index) {
        ensureCacheRange(index);
        if (index < mCacheStart || index >= mCacheEnd) return null;
        return mCache[index - mCacheStart].getContentUri();
    }

    @Override
    public synchronized Bitmap getImage(int index) {
        ensureCacheRange(index);
        if (index < mCacheStart || index >= mCacheEnd) return null;
        return TYWidgetUtils.createWidgetBitmap(mCache[index - mCacheStart]);
    }

    @Override
    public void reload() {
        long version = mSource.reload();
        if (mSourceVersion != version) {
            mSourceVersion = version;
            mCacheStart = 0;
            mCacheEnd = 0;
            Arrays.fill(mCache, null);
        }
    }

    @Override
    public void setContentListener(ContentListener listener) {
        mContentListener = listener;
    }

    @Override
    public int size() {
        long token = Binder.clearCallingIdentity();
        try {
            return mSource.getMediaItemCount();
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    @Override
    public void onContentDirty() {
        if (mContentListener != null) mContentListener.onContentDirty();
    }
}
