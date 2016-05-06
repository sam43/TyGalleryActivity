/*
 * TIANYU hanpengzhe 20130723 add fortyphot_widget base on WidgetUtils.java
 */

package com.android.gallery3d.gadget;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.Log;

import com.android.gallery3d.R;
import com.android.gallery3d.data.MediaItem;
import com.android.gallery3d.util.ThreadPool;

public class TYWidgetUtils {

    private static final String TAG = "TYWidgetUtils";

    private static int sStackPhotoWidth = 220;
    private static int sStackPhotoHeight = 170;

    private TYWidgetUtils() {
    }

    public static void initialize(Context context) {
        Resources r = context.getResources();
        sStackPhotoWidth = r.getDimensionPixelSize(R.dimen.ty_stack_photo_width);
        sStackPhotoHeight = r.getDimensionPixelSize(R.dimen.ty_stack_photo_height);
    }

    public static Bitmap createWidgetBitmap(MediaItem image) {
        Bitmap bitmap = image.requestImage(MediaItem.TYPE_THUMBNAIL)
               .run(ThreadPool.JOB_CONTEXT_STUB);
        if (bitmap == null) {
            Log.w(TAG, "fail to get image of " + image.toString());
            return null;
        }
        return createWidgetBitmap(bitmap, image.getRotation());
    }

    public static Bitmap createWidgetBitmap(Bitmap bitmap, int rotation) {
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();

        float scale;
        if (((rotation / 90) & 1) == 0) {
            scale = Math.max((float) sStackPhotoWidth / w,
                    (float) sStackPhotoHeight / h);
        } else {
            scale = Math.max((float) sStackPhotoWidth / h,
                    (float) sStackPhotoHeight / w);
        }

        Bitmap target = Bitmap.createBitmap(
                sStackPhotoWidth, sStackPhotoHeight, Config.ARGB_8888);
        Canvas canvas = new Canvas(target);
        canvas.translate(sStackPhotoWidth / 2, sStackPhotoHeight / 2);
        canvas.rotate(rotation);
        canvas.scale(scale, scale);
        Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.DITHER_FLAG);
        canvas.drawBitmap(bitmap, -w / 2, -h / 2, paint);
        return target;
    }
}
