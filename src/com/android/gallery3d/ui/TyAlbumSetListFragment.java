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
import com.android.gallery3d.app.TyAlbumSetListPage;

import android.util.Log;

public class TyAlbumSetListFragment extends TyBaseFragment {
    public static final String STATEMGRTAG = "TyAlbumSetListFragment";

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
        return inflater.inflate(R.layout.ty_albumset_list_fragment, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Bundle data = new Bundle();
        data.putString(TyAlbumSetListPage.KEY_MEDIA_PATH,
                mActivity.getDataManager().getTopSetPath(DataManager.INCLUDE_ALL));
        data.putBoolean(TyAlbumSetListPage.KEY_CLOSEPAGE_IF_NONEDATA, false);
        getStateManager().startState(TyAlbumSetListPage.class, data);
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
}
