/*
 * TIANYU yuxin add for for New Design Gallery
 */

package com.android.gallery3d.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.graphics.Rect;
import android.graphics.Paint;
import android.text.TextPaint;
import android.text.TextUtils;
import com.android.photos.data.GalleryBitmapPool;
import com.android.gallery3d.util.ThreadPool;
import com.android.gallery3d.util.ThreadPool.JobContext;
import com.android.gallery3d.R;

public class TyAlbumTitleMaker {
    private static final int BORDER_SIZE = 1; //TY zhencc 20150810 modify "0" to "1" for PROD103973107

    private final TyAlbumTimeSlotRenderer.TitleSpec mSpec;
    private final TextPaint mTitlePaint;
    private final TextPaint mCountPaint;
    //private final LazyLoadedBitmap mCountBgIcon;
    private final Context mContext;

    private int mAlbumLTitleWidth;
    private int mBitmapWidth;
    private int mBitmapHeight;

    public TyAlbumTitleMaker(Context context, TyAlbumTimeSlotRenderer.TitleSpec spec) {
        mContext = context;
        mSpec = spec;
        mTitlePaint = getTextPaint(spec.titleFontSize, spec.titleColor, false);
        mCountPaint = getTextPaint(spec.countFontSize, spec.countColor, false);
        //mCountBgIcon = new LazyLoadedBitmap(R.drawable.ty_bg_gallrey_quantity);
        
    }
    
    private static TextPaint getTextPaint(int textSize, int color, boolean isBold) {
        TextPaint paint = new TextPaint();
        paint.setTextSize(textSize);
        paint.setAntiAlias(true);
        paint.setColor(color);
        //paint.setShadowLayer(2f, 0f, 0f, Color.LTGRAY);
        if (isBold) {
            paint.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
        }
        return paint;
    }
    
    private class LazyLoadedBitmap {
        private Bitmap mBitmap;
        private int mResId;

        public LazyLoadedBitmap(int resId) {
            mResId = resId;
        }
        
        public synchronized Bitmap get() {
            if (mBitmap == null) {
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inPreferredConfig = Bitmap.Config.ARGB_8888;
                mBitmap = BitmapFactory.decodeResource(
                        mContext.getResources(), mResId, options);
            }
            return mBitmap.copy(Bitmap.Config.ARGB_8888, true);
        }
    }

    public synchronized void setTitleWidth(int width) {
        if (mAlbumLTitleWidth == width) return;
        mAlbumLTitleWidth = width;
        int borders = 2 * BORDER_SIZE;
        mBitmapWidth = width + borders;
        mBitmapHeight = mSpec.titleBackgroundHeight + borders;
    }

    //taoxj modify
    public ThreadPool.Job<Bitmap> requestTitle(
            String title, String count) {
        return new AlbumLTitleJob(/*mContext.getResources().getString(title)*/title, count);
    }

    static void drawText(Canvas canvas,
            int x, int y, String text, int lengthLimit, TextPaint p) {
        // The TextPaint cannot be used concurrently
        synchronized (p) {
            text = TextUtils.ellipsize(
                    text, p, lengthLimit, TextUtils.TruncateAt.END).toString();
            canvas.drawText(text, x, y - p.getFontMetricsInt().ascent, p);
        }
    }
    
    private class AlbumLTitleJob implements ThreadPool.Job<Bitmap> {
        private final String mTitle;
        private final String mCount;

        public AlbumLTitleJob(String title, String count) {
            mTitle = title;
            mCount = count;
        }

        @Override
        public Bitmap run(JobContext jc) {
            TyAlbumTimeSlotRenderer.TitleSpec s = mSpec;

            String title = mTitle;
            String count = mCount;

            Bitmap bitmap;
            int albumLTitleWidth;
            synchronized (this) {
                albumLTitleWidth = mAlbumLTitleWidth;
                bitmap = GalleryBitmapPool.getInstance().get(mBitmapWidth, mBitmapHeight);
            }

            if (bitmap == null) {
                bitmap = Bitmap.createBitmap(albumLTitleWidth,
                        s.titleBackgroundHeight + 2 * BORDER_SIZE, Config.ARGB_8888); //TY zhencc 20150810 add "2 * BORDER_SIZE" for PROD103973107
            }
            Canvas canvas = new Canvas(bitmap);
            canvas.clipRect(0, 0, bitmap.getWidth(), bitmap.getHeight());
            canvas.drawColor(mSpec.backgroundColor, PorterDuff.Mode.SRC);

            canvas.translate(0, 0);

            // draw title
            if (jc.isCancelled()) {
                return null;
            }
            int x = s.titleLeftMargin;
            int y = (s.titleBackgroundHeight + 2 * BORDER_SIZE - s.titleFontSize - s.fontBottomMargin); //TY zhencc 20150810 add "2 * BORDER_SIZE" for PROD103973107
            drawText(canvas, x, y, title, albumLTitleWidth - s.titleLeftMargin - 
                    s.titleRightMargin, mTitlePaint);

            // draw the icon
            //Bitmap icon = mCountBgIcon.get();
            //if (icon != null) {
            //    if (jc.isCancelled()) return null;
            //
            //    Canvas iconCanvas = new Canvas(icon);
            //    int iconWidth = icon.getWidth();
            //    int iconHeight = icon.getHeight();
            //    iconCanvas.clipRect(0, 0, iconWidth, iconHeight);
            //    iconCanvas.translate(0, 0);
            //    // draw count
            //    Paint paint = new Paint();
            //    paint.setTextSize(mSpec.countFontSize);
            //    Rect rect = new Rect();
            //    paint.getTextBounds(count,0,count.length(),rect);
            //
            //    x = (iconWidth - rect.width()) / 2 - 1;
            //    y = (iconHeight - s.countFontSize) / 2 - 1;
            //    drawText(iconCanvas, x, y, count, iconWidth , mCountPaint);
            //
            //    paint.setTextSize(mSpec.titleFontSize);
            //    paint.getTextBounds(title,0,title.length(),rect);
            //    x = s.titleLeftMargin + s.titleRightMargin + rect.width();
            //    y = s.titleBackgroundHeight - s.countFontSize - s.fontCountBottomMargin;
            //    canvas.drawBitmap(icon, x, y, null);
            //    if (icon != null && !icon.isRecycled()) {
            //        icon.recycle();
            //        icon = null;
            //    }
            //}else{
                // draw count
                if (jc.isCancelled()) {
                    return null;
                }
                //taoxj modify for remove count begin
                /*Paint paint = new Paint();
                paint.setTextSize(mSpec.titleFontSize);
                Rect rect = new Rect();
                paint.getTextBounds(title,0,title.length(),rect);
                x = s.titleLeftMargin + s.titleRightMargin + rect.width();
                y = s.titleBackgroundHeight + 2 * BORDER_SIZE - s.countFontSize - s.fontBottomMargin; //TY zhencc 20150810 add "2 * BORDER_SIZE" for PROD103973107
                drawText(canvas, x, y, count,
                        albumLTitleWidth - x , mCountPaint);*/
            //}
            return bitmap;
        }
    }
}
