package com.android.gallery3d.ui;

import android.widget.Toast;

import com.android.gallery3d.R;
import com.android.gallery3d.app.GalleryContext;

/**
 * @author zhencc
 * @details New Design Gallery
 *
 */

public class TyDeleteCompleteListener extends WakeLockHoldingProgressListener {
    private static final String WAKE_LOCK_LABEL = "Gallery Delete";

    public TyDeleteCompleteListener(GalleryContext galleryActivity) {
        super(galleryActivity, WAKE_LOCK_LABEL);
    }

    @Override
    public void onProgressComplete(int result) {
        super.onProgressComplete(result);
        int message;
        if (result == MenuExecutor.EXECUTION_RESULT_SUCCESS) {
            message = R.string.ty_delete_sucess; 
        } else {
            message = R.string.ty_delete_fail;
            Toast.makeText(getActivity().getAndroidContext(), message, Toast.LENGTH_LONG).show();
        }
        //Toast.makeText(getActivity().getAndroidContext(), message, Toast.LENGTH_LONG).show();
    }
}
