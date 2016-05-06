/*
 * TIANYU: yuxin add for New Design Gallery
 */
package com.android.gallery3d.app;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.support.v4.view.ViewPager;



public final class TyViewPager extends ViewPager{
    private static final String TAG = "TyViewPager";

    private boolean mIntercept = true;
    
    public TyViewPager(Context context) {
        super(context);
    }

    public TyViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setIntercept(boolean bIntercept){
        mIntercept = bIntercept;
    }
    
    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        if (mIntercept) {
            return super.onInterceptTouchEvent(event);
        }
        return false;
    }
}
