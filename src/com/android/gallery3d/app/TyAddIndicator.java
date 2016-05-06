/*
 * TIANYU: yuxin add for New Design Gallery
 */
package com.android.gallery3d.app;

import android.app.Activity;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.RelativeLayout;
import com.android.gallery3d.R;

public class TyAddIndicator extends RelativeLayout{
    private Context mContext;

    public TyAddIndicator(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
    }

    public void setVisibility(int visibility, boolean bAnimation){
        int oldvisibility = getVisibility();
        if (oldvisibility != visibility){
            if (bAnimation){
                if (visibility == View.VISIBLE){
                    startAnimation(AnimationUtils.loadAnimation(mContext, R.anim.ty_bottommenu_up));
                }else if (oldvisibility == View.VISIBLE){
                    startAnimation(AnimationUtils.loadAnimation(mContext, R.anim.ty_bottommenu_down));
                }
            }
            setVisibility(visibility);
        }
        
    }
}
