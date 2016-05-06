/*
 * TIANYU: yuxin add for New Design Gallery
 */
package com.android.gallery3d.ui;

import android.app.Activity;
import android.app.Fragment;
import android.os.Looper;
import android.os.Bundle;
import android.net.Uri;
import android.view.MenuInflater;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Menu;
import android.view.MenuItem;
import android.content.Intent;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.Configuration;
import com.android.photos.data.GalleryBitmapPool;
import com.android.gallery3d.util.ThreadPool;
import com.android.gallery3d.util.PanoramaViewHelper;
import com.android.gallery3d.data.DataManager;
import com.android.gallery3d.data.MediaItem;
import com.android.gallery3d.ui.GLRoot;
import com.android.gallery3d.ui.GLRootView;
import com.android.gallery3d.app.StateManager;
import com.android.gallery3d.app.GalleryApp;
import com.android.gallery3d.app.TransitionStore;
import com.android.gallery3d.app.GalleryActionBar;
import com.android.gallery3d.app.TyGalleryBottomBar;
import com.android.gallery3d.app.OrientationManager;
import com.android.gallery3d.app.TyAbstractGalleryActivity;
import com.android.gallery3d.R;
import com.android.gallery3d.common.Utils;
import com.android.gallery3d.app.GalleryContext;
import java.util.ArrayList;

import android.util.Log;


public abstract class TyBaseFragment extends Fragment implements GalleryContext{
    
    protected View mGalleryRoot;
    protected GLRootView mGLRootView;
    protected StateManager mStateManager;
    protected TyAbstractGalleryActivity mActivity;
    protected TyGalleryBottomBar mBottomBar;

    protected abstract String getStateTag();

    protected abstract View inflateView(LayoutInflater inflater, ViewGroup container);

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mActivity = (TyAbstractGalleryActivity)activity;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        mGalleryRoot = inflateView(inflater, container);
        mGLRootView = (GLRootView) mGalleryRoot.findViewById(R.id.gl_root_view);
        mGLRootView.setTag(R.id.ty_gl_tag, getMarkTag());
        return mGalleryRoot;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onStart() {
        super.onStart();
    }
    
    @Override
    public void onResume() {
        super.onResume();
        Utils.assertTrue(getStateManager().getStateCount() > 0);
        
        mGLRootView.lockRenderThread();
        try {
            getStateManager().resume();
            getDataManager().resume();
        } finally {
            mGLRootView.unlockRenderThread();
        }
        mGLRootView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mGLRootView.onPause();
        mGLRootView.lockRenderThread();
        try {
            getStateManager().pause();
            getDataManager().pause();
        } finally {
            mGLRootView.unlockRenderThread();
        }
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
        mGLRootView.lockRenderThread();
        try {
            getStateManager().destroy();
        } finally {
            mGLRootView.unlockRenderThread();
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        mGLRootView.lockRenderThread();
        try {
            super.onSaveInstanceState(outState);
            getStateManager().saveState(outState);
        } finally {
            mGLRootView.unlockRenderThread();
        }
    }
    @Override
    public void onConfigurationChanged(Configuration config) {
        super.onConfigurationChanged(config);
        mStateManager.onConfigurationChange(config);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        getStateManager().createOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        GLRoot root = getGLRoot();
        root.lockRenderThread();
        try {
            return getStateManager().itemSelected(item);
        } finally {
            root.unlockRenderThread();
        }
    }
    
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        mGLRootView.lockRenderThread();
        try {
            getStateManager().notifyActivityResult(
                    requestCode, resultCode, data);
        } finally {
            mGLRootView.unlockRenderThread();
        }
    }
    
    public void onBackPressed() {
        GLRoot root = getGLRoot();
        root.lockRenderThread();
        try {
            StateManager stateManager = getStateManager();
            if (stateManager.getStateCount() <= 1){
                ArrayList<StateManager> sms = getOtherStateManagers(stateManager);
                if (sms != null && sms.size() > 0){
                    for (StateManager sm : sms){
                        sm.finishTasks();
                    }
                }
            }
            stateManager.onBackPressed();
        } finally {
            root.unlockRenderThread();
        }
    }

    public String getMarkTag(){
        return getStateTag();
    }

    public DataManager getDataManager(){
        return mActivity.getDataManager();
    }
    
    public Context getAndroidContext(){
        return mActivity;
    }
    
    public Looper getMainLooper(){
        return mActivity.getMainLooper();
    }
    
    public ThreadPool getThreadPool(){
        return mActivity.getThreadPool();
    }

    public GLRoot getGLRoot(){
        return mGLRootView;
    }
    
    public synchronized StateManager getStateManager() {
        if (mStateManager == null) {
            mStateManager = new StateManager(this);
        }
        return mStateManager;
    }
    
    public TransitionStore getTransitionStore(){
        return mActivity.getTransitionStore();
    }
    
    public GalleryActionBar getGalleryActionBar() {
        return mActivity.getGalleryActionBar();
    }
    
    public TyGalleryBottomBar getGalleryBottomBar() {
        if (mBottomBar == null) {
            mBottomBar = new TyGalleryBottomBar(this);
        }
        return mBottomBar;
    }
    
    public ThreadPool getBatchServiceThreadPoolIfAvailable(){
        return mActivity.getBatchServiceThreadPoolIfAvailable();
    }

    public OrientationManager getOrientationManager(){
        return mActivity.getOrientationManager();
    }
    
    public PanoramaViewHelper getPanoramaViewHelper(){
        return mActivity.getPanoramaViewHelper();
    }
    
    public boolean isFullscreen(){
        return mActivity.isFullscreen();
    }

    public void printSelectedImage(Uri uri){
        mActivity.printSelectedImage(uri);
    }

    public View getGalleryAssignView(int resId){
        return mGalleryRoot.findViewById(resId);
    }

    public ArrayList<StateManager> getOtherStateManagers(StateManager sm){
        return mActivity.getOtherStateManagers(sm);
    }

    public ArrayList<TyBaseFragment> getAllFragment(){
        return mActivity.getAllFragment();
    }
    
    public String getGLTag(int key){
        return (String)mGLRootView.getTag(key);
    }
}
