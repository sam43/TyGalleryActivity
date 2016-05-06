/*
 * TIANYU hanpengzhe 20130723 add for typhoto_widget base on LocalPhotoSourcr.java
 */

package com.android.gallery3d.gadget;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore.Images.Media;

import com.android.gallery3d.app.GalleryApp;
import com.android.gallery3d.common.Utils;
import com.android.gallery3d.data.ContentListener;
import com.android.gallery3d.data.DataManager;
import com.android.gallery3d.data.MediaItem;
import com.android.gallery3d.data.Path;
import com.android.gallery3d.util.GalleryUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;

public class TYLocalPhotoSource implements WidgetSource {

    private static final String TAG = "TYLocalPhotoSource";

    private static final int MAX_PHOTO_COUNT = 128;

    /* Static fields used to query for the correct set of images */
    private static final Uri CONTENT_URI = Media.EXTERNAL_CONTENT_URI;
    private static final String DATE_TAKEN = Media.DATE_TAKEN;
    private static final String[] PROJECTION = {Media._ID};
    private static final String[] COUNT_PROJECTION = {"count(*)"};
    /* We don't want to include the download directory */
    private static final String SELECTION =
            String.format("%s != %s", Media.BUCKET_ID, getDownloadBucketId());
    private static final String ORDER = String.format("%s DESC", DATE_TAKEN);

    private Context mContext;
    private ArrayList<Long> mPhotos = new ArrayList<Long>();
    private ContentListener mContentListener;
    private ContentObserver mContentObserver;
    private boolean mContentDirty = true;
    private DataManager mDataManager;
    private static final Path LOCAL_IMAGE_ROOT = Path.fromString("/local/image/item");

    public TYLocalPhotoSource(Context context) {
        mContext = context;
        mDataManager = ((GalleryApp) context.getApplicationContext()).getDataManager();
        mContentObserver = new ContentObserver(new Handler()) {
            @Override
            public void onChange(boolean selfChange) {
                mContentDirty = true;
                if (mContentListener != null) mContentListener.onContentDirty();
            }
        };
        mContext.getContentResolver()
                .registerContentObserver(CONTENT_URI, true, mContentObserver);
    }

    public void close() {
        mContext.getContentResolver().unregisterContentObserver(mContentObserver);
    }

    @Override
    public Uri getContentUri(int index) {
        if (index < mPhotos.size()) {
            return CONTENT_URI.buildUpon()
                    .appendPath(String.valueOf(mPhotos.get(index)))
                    .build();
        }
        return null;
    }

    @Override
    public Bitmap getImage(int index) {
        if (index >= mPhotos.size()) return null;
        long id = mPhotos.get(index);
        MediaItem image = (MediaItem)
                mDataManager.getMediaObject(LOCAL_IMAGE_ROOT.getChild(id));
        if (image == null) return null;

        return TYWidgetUtils.createWidgetBitmap(image);
    }

    private int[] getExponentialIndice(int total, int count) {
        Random random = new Random();
        if (count > total) count = total;
        HashSet<Integer> selected = new HashSet<Integer>(count);
        while (selected.size() < count) {
            int row = (int)(-Math.log(random.nextDouble()) * total / 2);
            if (row < total) selected.add(row);
        }
        int values[] = new int[count];
        int index = 0;
        for (int value : selected) {
            values[index++] = value;
        }
        return values;
    }

    private int getPhotoCount(ContentResolver resolver) {
        Cursor cursor = resolver.query(
                CONTENT_URI, COUNT_PROJECTION, SELECTION, null, null);
        if (cursor == null) return 0;
        try {
            Utils.assertTrue(cursor.moveToNext());
            return cursor.getInt(0);
        } finally {
            cursor.close();
        }
    }

    private boolean isContentSound(int totalCount) {
        if (mPhotos.size() < Math.min(totalCount, MAX_PHOTO_COUNT)) return false;
        if (mPhotos.size() == 0) return true; // totalCount is also 0

        StringBuilder builder = new StringBuilder();
        for (Long imageId : mPhotos) {
            if (builder.length() > 0) builder.append(",");
            builder.append(imageId);
        }
        Cursor cursor = mContext.getContentResolver().query(
                CONTENT_URI, COUNT_PROJECTION,
                String.format("%s in (%s)", Media._ID, builder.toString()),
                null, null);
        if (cursor == null) return false;
        try {
            Utils.assertTrue(cursor.moveToNext());
            return cursor.getInt(0) == mPhotos.size();
        } finally {
            cursor.close();
        }
    }

    public void reload() {
        if (!mContentDirty) return;
        mContentDirty = false;

        ContentResolver resolver = mContext.getContentResolver();
        int photoCount = getPhotoCount(resolver);
        if (isContentSound(photoCount)) return;

        int choosedIds[] = getExponentialIndice(photoCount, MAX_PHOTO_COUNT);
        Arrays.sort(choosedIds);

        mPhotos.clear();
        Cursor cursor = mContext.getContentResolver().query(
                CONTENT_URI, PROJECTION, SELECTION, null, ORDER);
        if (cursor == null) return;
        try {
            for (int index : choosedIds) {
                if (cursor.moveToPosition(index)) {
                    mPhotos.add(cursor.getLong(0));
                }
            }
        } finally {
            cursor.close();
        }
    }

    @Override
    public int size() {
        reload();
        return mPhotos.size();
    }

    /**
     * Builds the bucket ID for the public external storage Downloads directory
     * @return the bucket ID
     */
    private static int getDownloadBucketId() {
        String downloadsPath = Environment
                .getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                .getAbsolutePath();
        return GalleryUtils.getBucketId(downloadsPath);
    }

    @Override
    public void setContentListener(ContentListener listener) {
        mContentListener = listener;
    }
}
