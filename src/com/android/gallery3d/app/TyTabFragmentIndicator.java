/*
 * TIANYU: yuxin add for New Design Gallery
 */
package com.android.gallery3d.app;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.content.Context;
import android.content.res.Resources;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Button;
import android.support.v13.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.TypedValue;
import com.android.gallery3d.R;
import com.android.gallery3d.ui.TyBaseFragment;
import com.android.gallery3d.app.StateManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
/*TIANYU: liuyuchuan add begin for PROD103682575*/
import com.android.gallery3d.ui.TyAlbumTimeGroupFragment;
import com.android.gallery3d.ui.TyAlbumSetListFragment;
/*TIANYU: liuyuchuan add begin for PROD103682575*/

public class TyTabFragmentIndicator extends FrameLayout implements ViewPager.OnPageChangeListener, OnClickListener {
    private static final int MSG_REFLASH_SLIDER = 1;
    private final int mFocTextSize;
    private final int mDefTextSize;
    private Activity mActivity;
    private ViewPager mViewPager;
    private LinearLayout mContainer;
    private ImageView mSlider;
    private SectionsPagerAdapter mSectionsPagerAdapter;
    private final ArrayList<TabInfo> mTabs = new ArrayList<TabInfo>();
    private final HashMap<String, TyBaseFragment> mViewMap = new HashMap<String, TyBaseFragment>();
    private Handler mHandler;
    private final boolean mEnableSlider = false;

    private class TabInfo {
        public final int titleResId;
        public final Class<?> clss;
        public final String stateMgrTag;
        public final Bundle args;

        TabInfo(int _titleResId, Class<?> _class, String _stateMgrTag, Bundle _args) {
            titleResId = _titleResId;
            clss = _class;
            stateMgrTag = _stateMgrTag;
            args = _args;
        }
    }

    public TyTabFragmentIndicator(Context context, AttributeSet attrs) {
        super(context, attrs);
        mActivity = (Activity)context;
        Resources res = mActivity.getResources();
        mFocTextSize = res.getDimensionPixelSize(R.dimen.ty_tab_foc_text_size);
        mDefTextSize = res.getDimensionPixelSize(R.dimen.ty_tab_def_text_size);
    }

    public void addTabFragment(int TitleResId, String stateMgrTag, Class<?> clss, Bundle args){
        TabInfo info = new TabInfo(TitleResId, clss, stateMgrTag, args);
        mTabs.add(info);
    }

    public void setViewPager(ViewPager viewPager){
        mViewPager = viewPager;
        mViewPager.setOnPageChangeListener(this);
        mSectionsPagerAdapter = new SectionsPagerAdapter(mActivity);
        mViewPager.setAdapter(mSectionsPagerAdapter);
    }

    public void commit(){	
        mHandler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                switch (message.what) {
                    case MSG_REFLASH_SLIDER: {
                        int position = mViewPager.getCurrentItem();
                        int x = (int)(position * mSlider.getWidth());
                        ((View)mSlider.getParent()).scrollTo(-x, mSlider.getScrollY());
                        break;
                    }
                    default: throw new AssertionError(message.what);
                }
            }
        };
        int size = mTabs.size();
        if(size > 0 ){
            mContainer = (LinearLayout)mActivity.findViewById(R.id.tab_content);
            if (mContainer.getChildCount() == size){
                for(int i = 0; i < size; i++ ){
                    Button view = (Button)mContainer.getChildAt(i);
                    view.setTag(i);
                    view.setOnClickListener(this);
                    view.setText(mActivity.getString(mTabs.get(i).titleResId));
                }

                mSlider = (ImageView)mActivity.findViewById(R.id.tabslider);
                ((View)mSlider.getParent()).setVisibility(mEnableSlider ? View.VISIBLE : View.GONE);
                setSliderWidth();
            }
        }
    }

    public TyBaseFragment getCurrentTabFragment(){
        int pos = mViewPager.getCurrentItem();
        TabInfo info = mTabs.get(pos);
        return mViewMap.get(info.stateMgrTag);
    }

    public ArrayList<TyBaseFragment> getAllFragment() {
        ArrayList<TyBaseFragment> fgs = new ArrayList<TyBaseFragment>();
        Iterator iter = mViewMap.entrySet().iterator();
        Map.Entry entry = null;
        while (iter.hasNext()){
            entry = (Map.Entry)iter.next();
            fgs.add((TyBaseFragment)entry.getValue());
        }
        return fgs;
    }

    public ArrayList<StateManager> getOtherStateManagers(StateManager sm){
        ArrayList<StateManager> sms = new ArrayList<StateManager>();
        int size = mTabs.size();
        for (int i = 0; i < size; i++){
            TabInfo tabInfo = mTabs.get(i);
            if (tabInfo != null){
                TyBaseFragment baseFragment = mViewMap.get(tabInfo.stateMgrTag);
                if (baseFragment != null){
                    StateManager bf = baseFragment.getStateManager();
                    if (sm != bf){
                        sms.add(bf);
                    }
                }
            }
        }
        return sms;
    }

    public void setCurrentTab(int position) {
        Button view = (Button)mContainer.getChildAt(position);
        if (view != null){
            view.setSelected(true);
            view.setTextSize(TypedValue.COMPLEX_UNIT_PX, mFocTextSize);
        }
        mViewPager.setCurrentItem(position);
    }
    
    public int getCurrentTab() {
        return mViewPager.getCurrentItem();
    }

    public void reCalculateSlider(){
        setSliderWidth();
        if (mEnableSlider){
            mHandler.sendMessage(mHandler.obtainMessage(MSG_REFLASH_SLIDER));
        }
    }

    public void setVisibility(int visibility, boolean bAnimation){
        int oldvisibility = getVisibility();
        if (oldvisibility != visibility){
            if (bAnimation){
                if (visibility == View.VISIBLE){
                    startAnimation(AnimationUtils.loadAnimation(mActivity, R.anim.ty_top_down));
                }else if (oldvisibility == View.VISIBLE){
                    startAnimation(AnimationUtils.loadAnimation(mActivity, R.anim.ty_top_up));
                }
            }
            setVisibility(visibility);
        }
        
    }

    public class SectionsPagerAdapter extends FragmentPagerAdapter {
        public SectionsPagerAdapter(Activity activity) {
            super(activity.getFragmentManager());
        }

        @Override
        public Object instantiateItem(final ViewGroup container, final int position) {
            final TyBaseFragment fg = (TyBaseFragment)super.instantiateItem(container, position);
            TabInfo info = mTabs.get(position);
            mViewMap.put(fg.getMarkTag(), fg);
            return fg;
        }

        @Override
        public Fragment getItem(int index) {
            TabInfo info = mTabs.get(index);
            Fragment fg = Fragment.instantiate(mActivity, info.clss.getName(), info.args);
            return fg;
        }
        
        @Override
        public int getCount() {
            return mTabs.size();
        }
    }


    @Override
    public void onPageSelected(int position) {    
        int size = mTabs.size();
        for(int i = 0; i < size; i++) {
            Button view = (Button)mContainer.getChildAt(i);
            if (view != null){
                if(i == position) {
                    view.setSelected(true);
                    view.setTextSize(TypedValue.COMPLEX_UNIT_PX, mFocTextSize);
                }
                else {
                    view.setSelected(false);
                    view.setTextSize(TypedValue.COMPLEX_UNIT_PX, mDefTextSize);
                }
            }
        }
    }
    private int mPreviousPosition = 0;

    @Override
    public void onPageScrolled(int position,float positionOffset, int positionOffsetPixels) {
    	Log.d("zhencc","onPageScrolled()--position:"+position+",--mPreviousPosition:"+mPreviousPosition);
        if (mEnableSlider){
            int x = (int)((position + positionOffset) * mSlider.getWidth());
            ((View)mSlider.getParent()).scrollTo(-x, mSlider.getScrollY());
        }
        
        /*TIANYU: liuyuchuan add begin for PROD103682575*/
        if(mPreviousPosition != position){
            TabInfo info = mTabs.get(position);
            TyBaseFragment mTyBaseFragment = mViewMap.get(info.stateMgrTag);
            TabInfo info2 = mTabs.get(0);
            TyBaseFragment mTyBaseFragment2 = mViewMap.get(info2.stateMgrTag);
            TyAlbumTimeGroupFragment mTyAlbumTimeGroupFragment = (TyAlbumTimeGroupFragment)mTyBaseFragment2;
            Log.d("zhencc","onPageScrolled()--mTyBaseFragment:"+mTyBaseFragment);
            if (mTyBaseFragment instanceof TyAlbumTimeGroupFragment){
            	Log.d("zhencc","onPageScrolled()-@@-if--call changeViewPagerWidth1");
                mTyAlbumTimeGroupFragment.changeViewPagerWidth1();
            }else if (mTyBaseFragment instanceof TyAlbumSetListFragment){
            	Log.d("zhencc","onPageScrolled()-@@-else if--call changeViewPagerWidth2");
                mTyAlbumTimeGroupFragment.changeViewPagerWidth2();
            }
            mPreviousPosition = position;
        }
        /*TIANYU: liuyuchuan add end for PROD103682575*/
    }

    @Override
    public void onPageScrollStateChanged(int state) {
    }

    @Override
    public void onClick(View v) {
        int position = (Integer)v.getTag();
        int current = mViewPager.getCurrentItem();
        if (current == position){
            return;
        }
        mViewPager.setCurrentItem(position);
    }

    private void setSliderWidth(){
        if (mEnableSlider){
            int size = mTabs.size();
            int cursorWidth = getWindowWidth()/size;
            ViewGroup.LayoutParams params = mSlider.getLayoutParams();
            params.width = cursorWidth;
            mSlider.setLayoutParams(params);
        }
    }

    private int getWindowWidth(){
        DisplayMetrics dm = new DisplayMetrics();
        mActivity.getWindowManager().getDefaultDisplay().getMetrics(dm);
        return dm.widthPixels;
    }
}
