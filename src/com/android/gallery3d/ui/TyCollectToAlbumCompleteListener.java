package com.android.gallery3d.ui;

import android.widget.Toast;

import com.android.gallery3d.R;
import com.android.gallery3d.app.GalleryContext;

/**
 * @author zhencc
 * @details New Design Gallery
 *
 */

public class TyCollectToAlbumCompleteListener extends WakeLockHoldingProgressListener {
    private static final String WAKE_LOCK_LABEL = "Gallery Add To Album";
    //TY wb034 20150204 add begin for tygallery
    private Listener mListener;
    //TY wb034 20150204 add end for tygallery
    public TyCollectToAlbumCompleteListener(GalleryContext galleryActivity) {
        super(galleryActivity, WAKE_LOCK_LABEL);
    }

    @Override
    public void onProgressComplete(int result) {
        super.onProgressComplete(result);
        int message;
        if (result == MenuExecutor.EXECUTION_RESULT_SUCCESS) {
            message = R.string.ty_collect_to_album_sucess;
        } else if (result == MenuExecutor.EXECUTION_RESULT_FAIL_EXISTS) {
        	message = R.string.ty_collect_to_album_fail_exists;
        } else if(result == MenuExecutor.EXECUTION_RESULT_FAIL_PART_EXISTS){
        	message = R.string.ty_add_to_album_fail_part_exists;
         //TY wb034 20150128 add begin for tygallery
        } else if(result ==MenuExecutor.EXECUTION_DELETE_ALBUM_SUCCESS){
            message = R.string.ty_remove_from_collect;
        } else if(result ==MenuExecutor.EXECUTION_DELETE_ALBUM_FAILED){
            message = R.string.ty_remove_from_collect_fail;
            //TY wb034 20150128 add begin for tygallery        	
        }else{
            message = R.string.ty_collect_to_album_fail;
        }
        if(mListener!=null){
            mListener.collectComplete(result);
        }
        Toast.makeText(getActivity().getAndroidContext(), message, Toast.LENGTH_LONG).show();
    }
  //TY wb034 20150204 add begin for tygallery
     public interface Listener {
        public void collectComplete(int result);
    }
     public void setListener(Listener listener){
        this.mListener = listener;
     }
//TY wb034 20150204 add end for tygallery
}
