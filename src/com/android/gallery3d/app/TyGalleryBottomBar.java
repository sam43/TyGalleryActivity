
package com.android.gallery3d.app;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import com.android.gallery3d.R;
import com.android.gallery3d.app.TyCamIndicator;
import com.android.gallery3d.app.TyAddIndicator;
import android.widget.Button;
import android.widget.ImageButton;
import android.view.View.OnClickListener;
import android.view.animation.AnimationUtils;
import android.content.res.Configuration;
/*
 * TIANYU: yuxin add for New Design Gallery
 */

public class TyGalleryBottomBar implements OnClickListener{
    private static final String TAG = "TyGalleryBottomBar";

    private GalleryContext mGalleryContext;
    private Activity mActivity;
    private Context mContext;
    
    public static enum TyBottomMode {TyNullMode, TyNoneMode, TyCamMode, TyAddMode, TySelectMode}
    private TyBottomMode mCurmode = TyBottomMode.TyNullMode;
    private TyBottomMode mNeedhidebyNoneMode  = TyBottomMode.TyNullMode;
    
    public static enum TySelectKind {TyNullKind, TyDeleteKind, TyShareKind, TyAddKind, TyHideKind, TyShowKind}
    private TySelectKind mCurKind = TySelectKind.TyNullKind;

    public static enum TyHeightKind {TyAll, TySelect, TyFloat}

    private View mTyBottomBarContainer;
    
    private TyBottomSelectIndicator mTyBottomSelectInd;
    private Button mTySelectAllBtn;
    private Button mTyDeleteBtn;
    private Button mTyShareBtn;
    private Button mTyAddBtn;
    private Button mTyHideBtn;
    private Button mTyShowBtn;

    private TyCamIndicator mTyCamIndicator;
    private ImageButton mTyCamAct;
    
    private TyAddIndicator mTyAddIndicator;
    private ImageButton mTyAddAct;
    
    private OnBottomBarListener mOnClickListener;

    private int mTyBottombarHeight;
    private int mTyBottomSelectHeight;

    public TyGalleryBottomBar(GalleryContext galleryContext) {
        mGalleryContext = galleryContext;
        mContext = mGalleryContext.getAndroidContext();
        mActivity = (Activity)mContext;
        
        mTyBottomBarContainer = mGalleryContext.getGalleryAssignView(R.id.ty_bottombar_container);
        if (mTyBottomBarContainer != null){
            mTyBottomSelectInd = (TyBottomSelectIndicator)mTyBottomBarContainer.findViewById(R.id.ty_bottom_select);
            if (mTyBottomSelectInd != null){
                mTySelectAllBtn = (Button)mTyBottomSelectInd.findViewById(R.id.ty_selection_all_btn);
                mTySelectAllBtn.setOnClickListener(this);
                mTyDeleteBtn = (Button)mTyBottomSelectInd.findViewById(R.id.ty_delete_btn);
                mTyDeleteBtn.setOnClickListener(this);
                mTyShareBtn = (Button)mTyBottomSelectInd.findViewById(R.id.ty_share_btn);
                mTyShareBtn.setOnClickListener(this);
                mTyAddBtn = (Button)mTyBottomSelectInd.findViewById(R.id.ty_add_btn);
                mTyAddBtn.setOnClickListener(this);
                mTyHideBtn = (Button)mTyBottomSelectInd.findViewById(R.id.ty_hide_btn);
                mTyHideBtn.setOnClickListener(this);
                mTyShowBtn = (Button)mTyBottomSelectInd.findViewById(R.id.ty_show_btn);
                mTyShowBtn.setOnClickListener(this);
            }
            mTyCamIndicator = (TyCamIndicator)mTyBottomBarContainer.findViewById(R.id.ty_cam_ind);
            if (mTyCamIndicator != null){
                mTyCamAct = (ImageButton)mTyCamIndicator.findViewById(R.id.ty_cam_act);
                mTyCamAct.setOnClickListener(this);
            }
            mTyAddIndicator = (TyAddIndicator)mTyBottomBarContainer.findViewById(R.id.ty_add_ind);
            if (mTyAddIndicator != null){
                mTyAddAct = (ImageButton)mTyAddIndicator.findViewById(R.id.ty_add_act);
                mTyAddAct.setOnClickListener(this);
            }
        }
        mTyBottombarHeight = mActivity.getResources().getDimensionPixelSize(R.dimen.ty_bottom_height);
        mTyBottomSelectHeight = mActivity.getResources().getDimensionPixelSize(R.dimen.ty_bottom_select);
    }

    public void enableNoneMode(boolean bAnim) {
        if (mCurmode == TyBottomMode.TyNoneMode) return;
        
        if (mTyBottomBarContainer != null){
            if (bAnim){
                mTyBottomBarContainer.startAnimation(AnimationUtils.loadAnimation(mActivity, R.anim.ty_bottommenu_down));
            }
            mTyBottomBarContainer.setVisibility(View.GONE);
        }
        if (mTyBottomSelectInd != null){
            mTyBottomSelectInd.setVisibility(View.GONE);
        }
        mNeedhidebyNoneMode = mCurmode;
        mCurmode = TyBottomMode.TyNoneMode;
    }
        
    public void enableCamMode(boolean bAnim) {
        dealNeedhidebyNoneMode();
        if (mCurmode == TyBottomMode.TyCamMode) return;
        if (mTyCamIndicator != null){
            mTyCamIndicator.setVisibility(View.GONE);
        }
        
        if (mTyBottomBarContainer != null){
            mTyBottomBarContainer.setVisibility(View.VISIBLE);
        }
        if (mTyCamIndicator != null){
            mTyCamIndicator.setVisibility(View.VISIBLE, bAnim);
        }
        if (mTyAddIndicator != null){
            mTyAddIndicator.setVisibility(View.GONE, bAnim);
        }
        if (mTyBottomSelectInd != null){
            mTyBottomSelectInd.setVisibility(View.GONE, bAnim);
        }
        mCurmode = TyBottomMode.TyCamMode;
    }
    
    public void enableAddMode(boolean bAnim) {
        dealNeedhidebyNoneMode();
        if (mCurmode == TyBottomMode.TyAddMode) return;
        if (mTyAddIndicator != null){
            mTyAddIndicator.setVisibility(View.GONE);
        }
        
        if (mTyBottomBarContainer != null){
            mTyBottomBarContainer.setVisibility(View.VISIBLE);
        }
        if (mTyAddIndicator != null){
            mTyAddIndicator.setVisibility(View.VISIBLE, bAnim);
        }
        if (mTyCamIndicator != null){
            mTyCamIndicator.setVisibility(View.GONE);
        }
        if (mTyBottomSelectInd != null){
            mTyBottomSelectInd.setVisibility(View.GONE );
        }
        mCurmode = TyBottomMode.TyAddMode;
    }
    
    public void enableSelectMode(TySelectKind selectKind, Object tag) {
        dealNeedhidebyNoneMode();
        if (mCurmode == TyBottomMode.TySelectMode) return;
        if (mTyBottomSelectInd != null){
            mTyBottomSelectInd.setVisibility(View.GONE);
        }
        
        if (mTyBottomBarContainer != null){
            mTyBottomBarContainer.setVisibility(View.VISIBLE);
        }
        if (mTyBottomSelectInd != null){
            mTyBottomSelectInd.setVisibility(View.VISIBLE, true);
            mCurKind = switchSelectKind(mCurKind, selectKind, tag);
        }
        if (mTyCamIndicator != null){
            mTyCamIndicator.setVisibility(View.GONE, true);
        }
        if (mTyAddIndicator != null){
            mTyAddIndicator.setVisibility(View.GONE, true);
        }
        mCurmode = TyBottomMode.TySelectMode;
    }

    private TySelectKind switchSelectKind(TySelectKind oldKind, TySelectKind newKind, Object tag){
        switch(oldKind){
            case TyDeleteKind:
                mTyDeleteBtn.setVisibility(View.GONE);
                break;
            case TyShareKind:
                mTyShareBtn.setVisibility(View.GONE);
                break;
            case TyAddKind:
                mTyAddBtn.setVisibility(View.GONE);
                break;
            case TyHideKind:
                mTyHideBtn.setVisibility(View.GONE);
                break;
            case TyShowKind:
                mTyShowBtn.setVisibility(View.GONE);
                break;
            default:
                break;
        }

        switch(newKind){
            case TyDeleteKind:
                mTyDeleteBtn.setVisibility(View.VISIBLE);
                mTyDeleteBtn.setTag(tag);
                mTyDeleteBtn.setEnabled(false);
                break;
            case TyShareKind:
                mTyShareBtn.setVisibility(View.VISIBLE);
                mTyShareBtn.setTag(tag);
                mTyShareBtn.setEnabled(false);
                break;
            case TyAddKind:
                mTyAddBtn.setVisibility(View.VISIBLE);
                mTyAddBtn.setTag(tag);
                mTyAddBtn.setEnabled(false);
                break;
            case TyHideKind:
                mTyHideBtn.setVisibility(View.VISIBLE);
                mTyHideBtn.setTag(tag);
                mTyHideBtn.setEnabled(false);
                break;
            case TyShowKind:
                mTyShowBtn.setVisibility(View.VISIBLE);
                mTyShowBtn.setTag(tag);
                mTyShowBtn.setEnabled(false);
                break;
            default:
                break;
        }
        return newKind;
    }
    
    private void dealNeedhidebyNoneMode() {
        if (mNeedhidebyNoneMode == TyBottomMode.TyNullMode){
            return;
        }
        /*taoxj remove begin
        switch(mNeedhidebyNoneMode){
            case TyCamMode:
                mTyCamIndicator.setVisibility(View.GONE);
                break;
            case TyAddMode:
                mTyAddIndicator.setVisibility(View.GONE);
                break;
            case TySelectMode:
                mTyBottomSelectInd.setVisibility(View.GONE);
                break;
        } 
        taoxj remove end
        */
        mNeedhidebyNoneMode = TyBottomMode.TyNullMode;
    }
    public boolean isAssignMode(TyBottomMode mode) {
        return mode == mCurmode;
    }

    public int getHeight() {
        int height = 0;
        if (mTyBottomBarContainer != null && (mTyBottomBarContainer.getVisibility() != View.GONE)){
            height = mTyBottombarHeight;
        }
        return height;
    }
    
    public int getAreaHeight(TyHeightKind kind) {
        int height = 0;
        if (kind == TyHeightKind.TyAll){
            height = getHeight();
        } else if (kind == TyHeightKind.TySelect){
            if (mTyBottomSelectInd != null && (mTyBottomSelectInd.getVisibility() != View.GONE)){
                height = mTyBottomSelectHeight;
            }
        }
        return height;
    }

    public void onConfigurationChanged(Configuration config) {
    }

    public void setSelectAllInSelectMode(boolean bSelectAll) {
        if (mCurmode != TyBottomMode.TySelectMode){
            return;
        }
        mTySelectAllBtn.setText(bSelectAll ? R.string.select_all : R.string.deselect_all);
    }
    
    public void setKindEnableInSelectMode(boolean enabled){
        if (mCurmode != TyBottomMode.TySelectMode){
            return;
        }
        switch(mCurKind){
            case TyDeleteKind:
                mTyDeleteBtn.setEnabled(enabled);
                break;
            case TyShareKind:
                mTyShareBtn.setEnabled(enabled);
                break;
            case TyAddKind:
                mTyAddBtn.setEnabled(enabled);
                break;
            case TyHideKind:
                mTyHideBtn.setEnabled(enabled);
                break;
            case TyShowKind:
                mTyShowBtn.setEnabled(enabled);
                break;
            default:
                break;
        }
    }

    public static interface OnBottomBarListener {
        public void onBottomBarClick(View v);
    }

    public void setOnClickListener(OnBottomBarListener listener){
        mOnClickListener = listener;
    }

    @Override
    public void onClick(View v) {
        if (mOnClickListener != null){
            mOnClickListener.onBottomBarClick(v);
        }
    }
}
