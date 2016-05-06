package com.android.gallery3d.ui;

import android.widget.Toast;

import com.android.gallery3d.R;
import com.android.gallery3d.app.GalleryContext;

/**
 * @author zhencc
 * @details New Design Gallery
 *
 */

public class TyAddToAlbumCompleteListener extends WakeLockHoldingProgressListener {
    private static final String WAKE_LOCK_LABEL = "Gallery Add To Album";

    public TyAddToAlbumCompleteListener(GalleryContext galleryActivity) {
        super(galleryActivity, WAKE_LOCK_LABEL);
    }

    @Override
    public void onProgressComplete(int result) {
        super.onProgressComplete(result);
        int message;
        if (result == MenuExecutor.EXECUTION_RESULT_SUCCESS) {
            message = R.string.ty_add_to_album_sucess;
        } else if (result == MenuExecutor.EXECUTION_RESULT_FAIL_EXISTS) {
        	message = R.string.ty_add_to_album_fail_exists;
        } else if(result == MenuExecutor.EXECUTION_RESULT_FAIL_PART_EXISTS){
        	message = R.string.ty_add_to_album_fail_part_exists;
        } else if(result==MenuExecutor.EXECUTION_ADD_ALBUM_SUCCESS){//TY wb034 20150126 add for tygally
            message = R.string.ty_create_album_success; //TY wb034 20150126 add for tygally      	
        }else {
            message = R.string.ty_add_to_album_fail;
        }
        Toast.makeText(getActivity().getAndroidContext(), message, Toast.LENGTH_LONG).show();
    }
}
