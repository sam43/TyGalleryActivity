/*
 * TIANYU: yuxin add for New Design Gallery
 */
package com.android.gallery3d.ui;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.android.gallery3d.R;
import com.android.gallery3d.data.DataManager;
import com.android.gallery3d.app.TyAlbumTimePage;
import android.util.Log;
/*TIANYU: liuyuchuan add begin for PROD103682575*/
import android.widget.RelativeLayout;
/*TIANYU: liuyuchuan add end for PROD103682575*/

public class TyAlbumTimeGroupFragment extends TyBaseFragment {
    public static final String STATEMGRTAG = "TyAlbumTimeGroupFragment";

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected View inflateView(LayoutInflater inflater, ViewGroup container) {
        return inflater.inflate(R.layout.ty_album_timegroup_fragment, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Bundle data = new Bundle();
        data.putString(TyAlbumTimePage.KEY_MEDIA_PATH,
                mActivity.getDataManager().getTopSetPath(DataManager.INCLUDE_LOCAL_CAMERA));
        getStateManager().startState(TyAlbumTimePage.class, data);
    }


    @Override
    public void onStart() {
        super.onStart();
    }
    
    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Override
    public String getStateTag(){
        return STATEMGRTAG;
    }

    /*TIANYU: liuyuchuan add begin for PROD103682575*/
    public void changeViewPagerWidth1() {
        RelativeLayout.LayoutParams dd = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        mGLRootView.setLayoutParams(dd);
    }
    public void changeViewPagerWidth2() {
        int screenWidth = mActivity.getWindowManager().getDefaultDisplay().getWidth();
        RelativeLayout.LayoutParams dd = new RelativeLayout.LayoutParams(screenWidth - 1, ViewGroup.LayoutParams.MATCH_PARENT);
        mGLRootView.setLayoutParams(dd);
    }
    /*TIANYU: liuyuchuan add end for PROD103682575*/
}

