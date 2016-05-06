/*
 * TIANYU hanpengzhe 20130723 add for typhoto_widget base on PhotoAppWidgetProvider.java 
 */

package com.android.gallery3d.gadget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Log;
import android.widget.RemoteViews;

import com.android.gallery3d.R;
import com.android.gallery3d.gadget.WidgetDatabaseHelper.Entry;
import com.android.gallery3d.onetimeinitializer.GalleryWidgetMigrator;

public class TYPhotoAppWidgetProvider extends AppWidgetProvider {

    private static final String TAG = "WidgetProvider";

    static RemoteViews buildWidget(Context context, int id, Entry entry) {

        switch (entry.type) {
            case WidgetDatabaseHelper.TYPE_ALBUM:
            case WidgetDatabaseHelper.TYPE_SHUFFLE:
                return buildStackWidget(context, id, entry);
            case WidgetDatabaseHelper.TYPE_SINGLE_PHOTO:
                return buildFrameWidget(context, id, entry);
        }
        throw new RuntimeException("invalid type - " + entry.type);
    }

    @Override
    public void onUpdate(Context context,
            AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // migrate gallery widgets from pre-JB releases to JB due to bucket ID change
        GalleryWidgetMigrator.migrateGalleryWidgets(context);

        WidgetDatabaseHelper helper = new WidgetDatabaseHelper(context);
        try {
            for (int id : appWidgetIds) {
                Entry entry = helper.getEntry(id);
                if (entry != null) {
                    RemoteViews views = buildWidget(context, id, entry);
                    appWidgetManager.updateAppWidget(id, views);
                } else {
                    Log.e(TAG, "cannot load widget: " + id);
                }
            }
        } finally {
            helper.close();
        }
        super.onUpdate(context, appWidgetManager, appWidgetIds);
    }

    private static RemoteViews buildStackWidget(Context context, int widgetId, Entry entry) {
        RemoteViews views = new RemoteViews(
                context.getPackageName(), R.layout.ty_appwidget_main);//hanpengzhe

        Intent intent = new Intent(context, TYWidgetService.class);   //HANPENGZHE
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
        intent.putExtra(TYWidgetService.EXTRA_WIDGET_TYPE, entry.type);
        intent.putExtra(TYWidgetService.EXTRA_ALBUM_PATH, entry.albumPath);
        intent.setData(Uri.parse("widget://gallery/" + widgetId));

        views.setRemoteAdapter(R.id.appwidget_stack_view, intent);
        views.setEmptyView(R.id.appwidget_stack_view, R.id.appwidget_empty_view);

        Intent clickIntent = new Intent(context, WidgetClickHandler.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 0, clickIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        views.setPendingIntentTemplate(R.id.appwidget_stack_view, pendingIntent);

        return views;
    }

    static RemoteViews buildFrameWidget(Context context, int appWidgetId, Entry entry) {
        RemoteViews views = new RemoteViews(
                context.getPackageName(), R.layout.photo_frame);
        try {
            byte[] data = entry.imageData;
            Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
            views.setImageViewBitmap(R.id.photo, bitmap);
        } catch (Throwable t) {
            Log.w(TAG, "cannot load widget image: " + appWidgetId, t);
        }

        if (entry.imageUri != null) {
            try {
                Uri uri = Uri.parse(entry.imageUri);
                Intent clickIntent = new Intent(context, WidgetClickHandler.class)
                        .setData(uri);
                PendingIntent pendingClickIntent = PendingIntent.getActivity(context, 0,
                        clickIntent, PendingIntent.FLAG_CANCEL_CURRENT);
                views.setOnClickPendingIntent(R.id.photo, pendingClickIntent);
            } catch (Throwable t) {
                Log.w(TAG, "cannot load widget uri: " + appWidgetId, t);
            }
        }
        return views;
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        // Clean deleted photos out of our database
        WidgetDatabaseHelper helper = new WidgetDatabaseHelper(context);
        for (int appWidgetId : appWidgetIds) {
            helper.deleteEntry(appWidgetId);
        }
        helper.close();
    }
}
