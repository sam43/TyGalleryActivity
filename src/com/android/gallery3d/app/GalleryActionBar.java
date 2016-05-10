/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.gallery3d.app;

import android.app.ActionBar;
import android.app.ActionBar.OnMenuVisibilityListener;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ShareActionProvider;
import android.support.v4.view.PagerAdapter;
import com.android.gallery3d.R;
import com.android.gallery3d.app.TyViewPager;
import com.android.gallery3d.app.TyTabFragmentIndicator;
import com.android.gallery3d.app.TybackTitleIndicator;
import com.android.gallery3d.app.TyTransbackTitleIndicator;
import com.android.gallery3d.cache.ImageCacheManager;
import com.android.gallery3d.ui.TyBaseFragment;
import com.android.gallery3d.common.Utils;
import java.util.ArrayList;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.view.View.OnClickListener;
import android.view.animation.AnimationUtils;
//TY wb034 20150209 add begin for tygallery
import android.widget.FrameLayout.LayoutParams;
import com.android.gallery3d.util.GalleryUtils;
import com.android.volley.toolbox.ImageLoader;

import android.content.res.Configuration;
//TY wb034 20150209 add end for tygallery
public class GalleryActionBar implements OnClickListener{
    @SuppressWarnings("unused")
    private static final String TAG = "GalleryActionBar";

    private GalleryContext mGalleryContext;
    private Activity mActivity;
    private Context mContext;
    private ActionBar mActionBar;
    
    public static enum TyActionMode {TyNullMode, TyNoneMode, TyTabMode, TyBackMode, TyTransBackMode, TySelectMode}
    private TyActionMode mCurmode = TyActionMode.TyNullMode;
    private TyActionMode mNeedhidebyNoneMode = TyActionMode.TyNullMode;
    
    private View mTyActionbarContainer;
    private View mTyStatusBarBg;
    private TyTabFragmentIndicator mTabFragmentIndicator;
    private TybackTitleIndicator mTyBackIndicator;
    private Button mTyBackClose;
    private ImageView mTyBackCamera;    
    private TyTransbackTitleIndicator mTyTransBackIndicator;
    private ImageButton mTyTransBackClose;
    private ImageView mTyTransUserPhoto;
    private TyActionSelectIndicator mTySelectIndicator;
    private Button mTySelClose;
    private TyViewPager mViewPager;
    private int mTyStatusBarHeight;
    private int mTyActionbarHight;
    private boolean mIsLandScape;
    private OnActionBarListener mOnClickListener;

    public GalleryActionBar(GalleryContext galleryContext) {
        mGalleryContext = galleryContext;
        mContext = mGalleryContext.getAndroidContext();
        mActivity = (Activity)mContext;
        mActionBar = mActivity.getActionBar();
        mActionBar.hide();
        
        mTyActionbarContainer = mActivity.findViewById(R.id.ty_actionbar_container);
        if (mTyActionbarContainer != null){            
            mTyStatusBarBg = mTyActionbarContainer.findViewById(R.id.ty_statusbar_bg);
            mTabFragmentIndicator = (TyTabFragmentIndicator)mTyActionbarContainer.findViewById(R.id.ty_tab_ind);
            mTyBackIndicator = (TybackTitleIndicator)mTyActionbarContainer.findViewById(R.id.ty_back_ind);
            if (mTyBackIndicator != null){
                mTyBackClose = (Button)mTyBackIndicator.findViewById(R.id.ty_action_title_close);
                mTyBackClose.setOnClickListener(this);
                mTyBackCamera =  (ImageView)mTyBackIndicator.findViewById(R.id.ty_action_camera);
                mTyBackCamera.setOnClickListener(this);
            }
            mTyTransBackIndicator = (TyTransbackTitleIndicator)mTyActionbarContainer.findViewById(R.id.ty_transback_ind);
            if (mTyTransBackIndicator != null){
                mTyTransBackClose = (ImageButton)mTyTransBackIndicator.findViewById(R.id.ty_action_title_trans_close);
                mTyTransUserPhoto = (ImageView)mTyTransBackIndicator.findViewById(R.id.ty_action_user_photo);
                mTyTransBackClose.setOnClickListener(this);
            }
            mTySelectIndicator = (TyActionSelectIndicator)mTyActionbarContainer.findViewById(R.id.ty_select_ind);
            if (mTySelectIndicator != null){
                mTySelClose = (Button)mTySelectIndicator.findViewById(R.id.ty_action_sel_close);
                mTySelClose.setOnClickListener(this);
            }
        }
        mViewPager = (TyViewPager)mActivity.findViewById(R.id.pager);
        mTyStatusBarHeight = mActivity.getResources().getDimensionPixelSize(com.android.internal.R.dimen.status_bar_height);
        mTyActionbarHight = mActivity.getResources().getDimensionPixelSize(R.dimen.ty_actionbar_height);
        requestParams(mActivity.getResources().getConfiguration());
    }

    public void enableNoneMode(boolean bAnim) {
        mOnClickListener = null;
        if (mCurmode == TyActionMode.TyNoneMode) return;
        
        if (mTyActionbarContainer != null){
            if (bAnim){
                mTyActionbarContainer.startAnimation(AnimationUtils.loadAnimation(mActivity, R.anim.ty_top_up));
            }
            mTyActionbarContainer.setVisibility(View.GONE);
        }
        if (mViewPager != null){
            mViewPager.setIntercept(false);
        }
        
        mNeedhidebyNoneMode = mCurmode;
        mCurmode = TyActionMode.TyNoneMode;
    }
    
    public void enableTabMode(boolean bAnim) {
        mOnClickListener = null;
        dealNeedhidebyNoneMode();
        if (mCurmode == TyActionMode.TyTabMode) return;
        if (mTyStatusBarBg != null){
            mTyStatusBarBg.setVisibility(mIsLandScape ? View.GONE : View.VISIBLE);
        }

        if (mTabFragmentIndicator != null){
            mTabFragmentIndicator.setVisibility(View.GONE);
        }
        
        if (mTyActionbarContainer != null){
            mTyActionbarContainer.setVisibility(View.VISIBLE);
        }
        if (mTabFragmentIndicator != null){
            mTabFragmentIndicator.setVisibility(View.VISIBLE, bAnim);
            mTabFragmentIndicator.reCalculateSlider();
        }
        if (mTyBackIndicator != null){
            mTyBackIndicator.setVisibility(View.GONE, bAnim);
        }
        
        if (mTyTransBackIndicator != null){
            mTyTransBackIndicator.setVisibility(View.GONE, bAnim);
        }
        
        if (mTySelectIndicator != null){
            mTySelectIndicator.setVisibility(View.GONE, bAnim);
        }
        if (mViewPager != null){
            mViewPager.setIntercept(true);
        }
        mCurmode = TyActionMode.TyTabMode;
    }
    
    public void enableBackMode(OnActionBarListener listener, boolean bAnim) { 
        mOnClickListener = listener;
        dealNeedhidebyNoneMode();
        if (mCurmode == TyActionMode.TyBackMode) return;
        if (mTyStatusBarBg != null){
            mTyStatusBarBg.setVisibility(mIsLandScape ? View.GONE : View.VISIBLE);
        }

        if (mTyBackIndicator != null){
            mTyBackIndicator.setVisibility(View.GONE);
        }
        if (mTyActionbarContainer != null){
            mTyActionbarContainer.setVisibility(View.VISIBLE);
        }
        if (mTabFragmentIndicator != null){
            mTabFragmentIndicator.setVisibility(View.GONE);
        }
        if (mTyBackIndicator != null){
            mTyBackIndicator.setVisibility(View.VISIBLE, bAnim);
        }
        if (mTyTransBackIndicator != null){
            mTyTransBackIndicator.setVisibility(View.GONE);
        }
        if (mTySelectIndicator != null){
            mTySelectIndicator.setVisibility(View.GONE, bAnim);
        }
        if (mViewPager != null){
            mViewPager.setIntercept(false);
        }
        mCurmode = TyActionMode.TyBackMode;
    }
    
    public void enableTransBackMode(OnActionBarListener listener) {  
        mOnClickListener = listener;
        dealNeedhidebyNoneMode();
        if (mCurmode == TyActionMode.TyTransBackMode) return;
        if (mTyStatusBarBg != null){
            mTyStatusBarBg.setVisibility(View.GONE);
        }

        if (mTyTransBackIndicator != null){
            mTyTransBackIndicator.setVisibility(View.GONE);
        }
        if (mTyActionbarContainer != null){
            mTyActionbarContainer.setVisibility(View.VISIBLE);
        }

        if (mTabFragmentIndicator != null){
            mTabFragmentIndicator.setVisibility(View.GONE);
        }
        if (mTyBackIndicator != null){
            mTyBackIndicator.setVisibility(View.GONE );
        }
        if (mTyTransBackIndicator != null){
            mTyTransBackIndicator.setVisibility(View.VISIBLE, true);
        }
        if (mTySelectIndicator != null){
            mTySelectIndicator.setVisibility(View.GONE);
        }
        
        if (mViewPager != null){
            mViewPager.setIntercept(false);
        }
        mCurmode = TyActionMode.TyTransBackMode;
    }
    
    public void enableSelectMode(OnActionBarListener listener) {
        mOnClickListener = listener;
        dealNeedhidebyNoneMode();
        if (mCurmode == TyActionMode.TySelectMode) return;
        if (mTyStatusBarBg != null){
           mTyStatusBarBg.setVisibility(mIsLandScape ? View.GONE : View.VISIBLE);
        }
        if (mTySelectIndicator != null){
            mTySelectIndicator.setVisibility(View.GONE);
        }

        if (mTyActionbarContainer != null){
            mTyActionbarContainer.setVisibility(View.VISIBLE);
        }
        if (mTabFragmentIndicator != null){
            mTabFragmentIndicator.setVisibility(View.GONE, true);
        }
        if (mTyBackIndicator != null){
            mTyBackIndicator.setVisibility(View.GONE, true);
        }
        if (mTyTransBackIndicator != null){
            mTyTransBackIndicator.setVisibility(View.GONE, true);
        }
        if (mTySelectIndicator != null){
            mTySelectIndicator.setVisibility(View.VISIBLE, true);
        }
        if (mViewPager != null){
            mViewPager.setIntercept(false);
        }
        mCurmode = TyActionMode.TySelectMode;
    }

    public void showCamera(boolean bShowCamera){
        if (mCurmode == TyActionMode.TyBackMode){
            if (mTyBackCamera != null){
                mTyBackCamera.setVisibility(bShowCamera ? View.VISIBLE : View.GONE);
            }
        }
    }
    
    public boolean isAssignMode(TyActionMode mode) {
        return mode == mCurmode;
    }
    
    private void dealNeedhidebyNoneMode() {
        if (mNeedhidebyNoneMode == TyActionMode.TyNullMode){
            return;
        }
        
        switch(mNeedhidebyNoneMode){
            case TyTabMode:
                mTabFragmentIndicator.setVisibility(View.GONE);
                break;
            case TyBackMode:
                mTyBackIndicator.setVisibility(View.GONE);
                break;
            case TyTransBackMode:
                mTyTransBackIndicator.setVisibility(View.GONE);
                break;
            case TySelectMode:
                mTySelectIndicator.setVisibility(View.GONE);
                break;
        }
        mNeedhidebyNoneMode = TyActionMode.TyNullMode;
    }

    public int getHeight() {
        int height = 0;
        if (mTyActionbarContainer != null && (mTyActionbarContainer.getVisibility() != View.GONE)){
            height = mIsLandScape ? mTyActionbarHight : (mTyStatusBarHeight + mTyActionbarHight);
        }
        return height;
    }
    
    public int getExpectHeight() {
        int height = 0;
        if (mTyActionbarContainer != null){
            height = mIsLandScape ? mTyActionbarHight : (mTyStatusBarHeight + mTyActionbarHight);
        }
        return height;
    }

    public void setClusterItemEnabled(int id, boolean enabled) {
    }
    
    public void setClusterItemVisibility(int id, boolean visible) {
    }

    public void onConfigurationChanged(Configuration config) {
        requestParams(config);
        if (mTabFragmentIndicator != null){
            mTabFragmentIndicator.reCalculateSlider();
        }
    }
    public void setTitle(String title) {
        //taoxj add begin
        title= "";
        //taoxj add end
        switch (mCurmode){
            case TyBackMode:{
                if (mTyBackClose != null){
                    mTyBackClose.setText(title);
                }
                break;
            }
            case TyTransBackMode:{
                if (mTyTransBackClose != null){
                  //  mTyTransBackClose.setText(title);
                }
                break;
            }
            case TySelectMode:{
                if (mTySelClose != null){
                    mTySelClose.setText(title);
                }
                break;
            }
            default:
                break;
        }
    }

    public void setTitle(int titleId) {
        switch (mCurmode){
            case TyBackMode:{
                if (mTyBackClose != null){
                    mTyBackClose.setText(/*titleId*/"");
                }
                break;
            }
            case TyTransBackMode:{
                if (mTyTransBackClose != null){
                 //   mTyTransBackClose.setText(/*titleId*/"");
                }
                break;
            }
            case TySelectMode:{
                if (mTySelClose != null){
                    mTySelClose.setText(/*titleId*/"");
                }
                break;
            }
            default:
                break;
        }
    }

    public void addOnMenuVisibilityListener(OnMenuVisibilityListener listener) {
        if (mActionBar != null) mActionBar.addOnMenuVisibilityListener(listener);
    }

    public void removeOnMenuVisibilityListener(OnMenuVisibilityListener listener) {
        if (mActionBar != null) mActionBar.removeOnMenuVisibilityListener(listener);
    }

    public boolean setSelectedAction(int type) {
        return false;
    }

    private Menu mActionBarMenu;
    private ShareActionProvider mSharePanoramaActionProvider;
    private ShareActionProvider mShareActionProvider;
    private Intent mSharePanoramaIntent;
    private Intent mShareIntent;

    public void createActionBarMenu(int menuRes, Menu menu) {
        mActivity.getMenuInflater().inflate(menuRes, menu);
        mActionBarMenu = menu;

        MenuItem item = menu.findItem(R.id.action_share_panorama);
        if (item != null) {
            mSharePanoramaActionProvider = (ShareActionProvider)
                item.getActionProvider();
            mSharePanoramaActionProvider
                .setShareHistoryFileName("panorama_share_history.xml");
            mSharePanoramaActionProvider.setShareIntent(mSharePanoramaIntent);
        }

        item = menu.findItem(R.id.action_share);
        if (item != null) {
            mShareActionProvider = (ShareActionProvider)
                item.getActionProvider();
            mShareActionProvider
                .setShareHistoryFileName("share_history.xml");
            mShareActionProvider.setShareIntent(mShareIntent);
        }
    }

    public Menu getMenu() {
        return mActionBarMenu;
    }

    public void setShareIntents(Intent sharePanoramaIntent, Intent shareIntent,
        ShareActionProvider.OnShareTargetSelectedListener onShareListener) {
        mSharePanoramaIntent = sharePanoramaIntent;
        if (mSharePanoramaActionProvider != null) {
            mSharePanoramaActionProvider.setShareIntent(sharePanoramaIntent);
        }
        mShareIntent = shareIntent;
        if (mShareActionProvider != null) {
            mShareActionProvider.setShareIntent(shareIntent);
            mShareActionProvider.setOnShareTargetSelectedListener(
                onShareListener);
        }
    }

    public boolean isFirstTabPage() {
        ArrayList<TyBaseFragment> bfs = mGalleryContext.getAllFragment();
        if (bfs == null || bfs.size() == 0){
            return false;
        }
        boolean isFirstPage = true;
        for (TyBaseFragment bf : bfs){
            if (bf.getStateManager().getStateCount() > 1){
                isFirstPage = false;
            }
        }
        return isFirstPage;
    }
    
   private void requestParams(Configuration config){
       mIsLandScape = config.orientation == Configuration.ORIENTATION_LANDSCAPE;
       if (mTyStatusBarBg == null) return;
       
       if (mCurmode == TyActionMode.TyTransBackMode){
           mTyStatusBarBg.setVisibility(View.GONE);
       }else{
          mTyStatusBarBg.setVisibility(mIsLandScape ? View.GONE : View.VISIBLE);
       }
   }

   public static interface OnActionBarListener {
       public void onActionBarClick(View v);
   }
   
   @Override
   public void onClick(View v) {
       if (mOnClickListener != null){
           mOnClickListener.onActionBarClick(v);
       }
   }
    //taoxj add for user photo begin
    public void setUserPhoto(String userPhoto){
        ImageCacheManager.getInstance().displayImage(userPhoto,mTyTransUserPhoto,R.drawable.default_user_photo,30,30);
       // ImageCacheManager.getInstance().displayImage("http://img0w.pconline.com.cn/pconline/1401/15/4172339_touxiang/23.jpg",mTyTransUserPhoto,R.drawable.default_user_photo,0,0);
    }
    //taoxj add for user photo end
}
