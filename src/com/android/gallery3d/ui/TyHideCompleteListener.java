package com.android.gallery3d.ui;

import android.widget.Toast;

import com.android.gallery3d.R;
import com.android.gallery3d.app.GalleryContext;

/**
 * @author wangqin
 * @details New Design Gallery
 *
 */

public class TyHideCompleteListener extends WakeLockHoldingProgressListener {
    private static final String WAKE_LOCK_LABEL = "Gallery TyHide";
    private boolean mIsHide;

    public TyHideCompleteListener(GalleryContext galleryActivity, boolean isHide) {
        super(galleryActivity, WAKE_LOCK_LABEL+(isHide ? "hide" : "show"));
        mIsHide = isHide;
    }

    @Override
    public void onProgressComplete(int result) {
        super.onProgressComplete(result);
        int message;
        if (result == MenuExecutor.EXECUTION_RESULT_SUCCESS) {
            if (mIsHide){
                message = R.string.ty_hide_sucess;
            }else{
                message = R.string.ty_showhidden_sucess;
            }
        } else {
            if (mIsHide){
                message = R.string.ty_hide_fail;
            }else{
                message = R.string.ty_showhidden_fail;
            }
        }
        Toast.makeText(getActivity().getAndroidContext(), message, Toast.LENGTH_LONG).show();
    }
}
