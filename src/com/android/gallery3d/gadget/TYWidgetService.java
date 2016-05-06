/*
 * TIANYU hanpengzhe 20130723 add for typhoto_widget base on WidgetService.java
 */

package com.android.gallery3d.gadget;

import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.android.gallery3d.R;
import com.android.gallery3d.app.GalleryApp;
import com.android.gallery3d.data.ContentListener;
import com.android.gallery3d.data.DataManager;
import com.android.gallery3d.data.MediaSet;
import com.android.gallery3d.data.Path;

/*TIANYURD wanggj 20130328 modify for PROD101630606 begin*/
import android.util.Log;
/*TIANYURD wanggj 20130328 modify for PROD101630606 end*/

public class TYWidgetService extends RemoteViewsService {

    @SuppressWarnings("unused")
    private static final String TAG = "TYGalleryAppWidgetService";

    public static final String EXTRA_WIDGET_TYPE = "widget-type";
    public static final String EXTRA_ALBUM_PATH = "album-path";

    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        int id = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID);
        int type = intent.getIntExtra(EXTRA_WIDGET_TYPE, 0);
        String albumPath = intent.getStringExtra(EXTRA_ALBUM_PATH);

        return new PhotoRVFactory((GalleryApp) getApplicationContext(),
                id, type, albumPath);
    }

    private static class EmptySource implements WidgetSource {

        @Override
        public int size() {
            return 0;
        }

        @Override
        public Bitmap getImage(int index) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Uri getContentUri(int index) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setContentListener(ContentListener listener) {}

        @Override
        public void reload() {}

        @Override
        public void close() {}
    }

    private static class PhotoRVFactory implements
            RemoteViewsService.RemoteViewsFactory, ContentListener {

        private final int mAppWidgetId;
        private final int mType;
        private final String mAlbumPath;
        private final GalleryApp mApp;

        private WidgetSource mSource;

        public PhotoRVFactory(GalleryApp app, int id, int type, String albumPath) {
            mApp = app;
            mAppWidgetId = id;
            mType = type;
            mAlbumPath = albumPath;
        }

        @Override
        public void onCreate() {
            if (mType == WidgetDatabaseHelper.TYPE_ALBUM) {
                Path path = Path.fromString(mAlbumPath);
                DataManager manager = mApp.getDataManager();
                MediaSet mediaSet = (MediaSet) manager.getMediaObject(path);
                mSource = mediaSet == null
                        ? new EmptySource()
                        : new TYMediaSetSource(mediaSet);
            } else {
                mSource = new TYLocalPhotoSource(mApp.getAndroidContext());
            }
            mSource.setContentListener(this);
            AppWidgetManager.getInstance(mApp.getAndroidContext())
                    .notifyAppWidgetViewDataChanged(
                    mAppWidgetId, R.id.appwidget_stack_view);
        }

        @Override
        public void onDestroy() {
            mSource.close();
            mSource = null;
        }

        public int getCount() {
            return mSource.size();
        }

        public long getItemId(int position) {
            return position;
        }

        public int getViewTypeCount() {
            return 1;
        }

        public boolean hasStableIds() {
            return true;
        }

        public RemoteViews getLoadingView() {
            RemoteViews rv = new RemoteViews(
                    mApp.getAndroidContext().getPackageName(),
                    R.layout.ty_appwidget_loading_item);
            rv.setProgressBar(R.id.appwidget_loading_item, 0, 0, true);
            return rv;
        }

        public RemoteViews getViewAt(int position) {
            Bitmap bitmap = mSource.getImage(position);
            if (bitmap == null) return getLoadingView();
            RemoteViews views = new RemoteViews(
                    mApp.getAndroidContext().getPackageName(),
                    R.layout.ty_appwidget_photo_item);
            views.setImageViewBitmap(R.id.appwidget_photo_item, bitmap);
            //views.setImageViewResource(R.id.appwidget_photo_item_background, R.drawable.ty_widget_photo_back);//hanpengzhe
            /*TIANYURD wanggj 20130328 modify for PROD101630606 begin*/
            /*
            views.setOnClickFillInIntent(R.id.appwidget_photo_item, new Intent()
                    .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    .setData(mSource.getContentUri(position)));
            */
            Intent clickIntent = new Intent();
            try { 
                clickIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                clickIntent.setData(mSource.getContentUri(position));
                views.setOnClickFillInIntent(R.id.appwidget_photo_item, clickIntent);
            } catch (NullPointerException ex) {
                Log.d(TAG, "getViewAt NullPointerException");
            }
            /*TIANYURD wanggj 20130328 modify for PROD101630606 end*/
            return views;
        }

        @Override
        public void onDataSetChanged() {
            mSource.reload();
        }

        @Override
        public void onContentDirty() {
            AppWidgetManager.getInstance(mApp.getAndroidContext())
                    .notifyAppWidgetViewDataChanged(
                    mAppWidgetId, R.id.appwidget_stack_view);
        }
    }
}
