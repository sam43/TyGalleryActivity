/*
 * TIANYU: yuxin add for New Design Gallery
 */
package com.android.gallery3d.app;

import android.app.Dialog;
import android.app.ActionBar;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.os.Bundle;
import android.view.InputDevice;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.Menu;
import android.view.MenuItem;
import android.support.v13.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import com.android.gallery3d.R;
import com.android.gallery3d.common.Utils;
import com.android.gallery3d.data.DataManager;
import com.android.gallery3d.data.MediaItem;
import com.android.gallery3d.data.MediaSet;
import com.android.gallery3d.data.Path;
import com.android.gallery3d.datatask.DownloadAsyncTask;
import com.android.gallery3d.datatask.LoginAsyncTask;
import com.android.gallery3d.picasasource.PicasaSource;
import com.android.gallery3d.util.GalleryUtils;
import com.android.gallery3d.ui.TyAlbumTimeGroupFragment;
import com.android.gallery3d.ui.TyAlbumSetListFragment;
import com.android.gallery3d.ui.TyBaseFragment;
import com.android.gallery3d.ui.GLRoot;
import com.android.gallery3d.app.TyViewPager;
import com.android.gallery3d.app.TyTabFragmentIndicator;
import com.android.gallery3d.volley.InstagramRestClient;

import java.util.ArrayList;
import java.util.HashMap;
//TY liuyuchuan add begin for PROD103694656
import android.content.pm.ActivityInfo;
import android.view.Surface;
//TY liuyuchuan add end for PROD103694656

public final class TyGalleryActivity extends TyAbstractGalleryActivity implements OnCancelListener{
    public static final String EXTRA_SLIDESHOW = GalleryActivity.EXTRA_SLIDESHOW;
    public static final String EXTRA_DREAM = GalleryActivity.EXTRA_DREAM;
    public static final String EXTRA_CROP = GalleryActivity.EXTRA_CROP;;

    public static final String ACTION_REVIEW = GalleryActivity.ACTION_REVIEW;
    public static final String KEY_GET_CONTENT = GalleryActivity.KEY_GET_CONTENT;
    public static final String KEY_GET_ALBUM = GalleryActivity.KEY_GET_ALBUM;
    public static final String KEY_TYPE_BITS = GalleryActivity.KEY_TYPE_BITS;
    public static final String KEY_MEDIA_TYPES = GalleryActivity.KEY_MEDIA_TYPES;
    public static final String KEY_DISMISS_KEYGUARD = GalleryActivity.KEY_DISMISS_KEYGUARD;

    private static final String TAG = "TyGalleryActivity";
    private Dialog mVersionCheckDialog;

    private TyTabFragmentIndicator mTabFragmentIndicator;
    private TyViewPager mViewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_ACTION_BAR);
        requestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
        if (getIntent().getBooleanExtra(KEY_DISMISS_KEYGUARD, false)) {
            getWindow().addFlags(
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        }
        setContentView(R.layout.ty_main);
        mTabFragmentIndicator = (TyTabFragmentIndicator) findViewById(R.id.ty_tab_ind);
        mViewPager = (TyViewPager) findViewById(R.id.pager);

        mTabFragmentIndicator.addTabFragment(R.string.tab_photos,
            TyAlbumTimeGroupFragment.STATEMGRTAG,
            TyAlbumTimeGroupFragment.class,
            null);
        /*taoxj remove
        mTabFragmentIndicator.addTabFragment(R.string.tab_albums,
            TyAlbumSetListFragment.STATEMGRTAG,
            TyAlbumSetListFragment.class,
            null); */
        mTabFragmentIndicator.setViewPager(mViewPager);
        mTabFragmentIndicator.commit();

        int curTab = 0;
        if (savedInstanceState != null) {
            curTab = savedInstanceState.getInt("tab", 0);
        }
        mTabFragmentIndicator.setCurrentTab(curTab);
    }
    
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        int curTab = 0; 
        if (mTabFragmentIndicator != null){
            curTab = mTabFragmentIndicator.getCurrentTab();
        }
        outState.putInt("tab", curTab);
    }

    @Override
    protected void onResume() {
        super.onResume();
        //TY liuyuchuan add begin for PROD103694656
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER);
        //TY liuyuchuan add end for PROD103694656
        if (mVersionCheckDialog != null) {
            mVersionCheckDialog.show();
        }
        mTabFragmentIndicator.reCalculateSlider();
        //taoxj add begin
        if(!"".equals(InstagramRestClient.getSeesionId())){
            new DownloadAsyncTask().execute();
        }else {
            new LoginAsyncTask().execute();
        }
        //taoxj add end
     }

    @Override
    protected void onPause() {
        super.onPause();
        //TY liuyuchuan add begin for PROD103694656
        if(getWindowManager().getDefaultDisplay().getRotation() == Surface.ROTATION_0){
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }else if(getWindowManager().getDefaultDisplay().getRotation() == Surface.ROTATION_90){
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }else if(getWindowManager().getDefaultDisplay().getRotation() == Surface.ROTATION_180){
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT);
        }else if(getWindowManager().getDefaultDisplay().getRotation() == Surface.ROTATION_270){
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
        }
        //TY liuyuchuan add end for PROD103694656
        if (mVersionCheckDialog != null) {
            mVersionCheckDialog.dismiss();
        }
    }
    
    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
    
    @Override
    public void onBackPressed() {
        if (mTabFragmentIndicator != null){
            TyBaseFragment fragment = mTabFragmentIndicator.getCurrentTabFragment();
            if (fragment != null){
                Log.i("koala","tygallery activity back pressed");
                fragment.onBackPressed();
            }
        }
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        /*TIANYURD:Lizhy 20150505 modify for PROD103691678 start*/
        if(mTabFragmentIndicator != null &&  mTabFragmentIndicator.getCurrentTabFragment() != null){
           mTabFragmentIndicator.getCurrentTabFragment().onActivityResult(requestCode, resultCode, data);
        }
        //mTabFragmentIndicator.getCurrentTabFragment().onActivityResult(requestCode, resultCode, data);
        /*TIANYURD:Lizhy 20150505 modify for PROD103691678 end*/
    }

    @Override
    public ArrayList<StateManager> getOtherStateManagers(StateManager sm){
        return mTabFragmentIndicator.getOtherStateManagers(sm);
    }

    @Override
    public ArrayList<TyBaseFragment> getAllFragment(){
        return mTabFragmentIndicator.getAllFragment();
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        if (dialog == mVersionCheckDialog) {
            mVersionCheckDialog = null;
        }
    }

    @Override
    public boolean onGenericMotionEvent(MotionEvent event) {
        final boolean isTouchPad = (event.getSource()
                & InputDevice.SOURCE_CLASS_POSITION) != 0;
        if (isTouchPad) {
            float maxX = event.getDevice().getMotionRange(MotionEvent.AXIS_X).getMax();
            float maxY = event.getDevice().getMotionRange(MotionEvent.AXIS_Y).getMax();
            View decor = getWindow().getDecorView();
            float scaleX = decor.getWidth() / maxX;
            float scaleY = decor.getHeight() / maxY;
            float x = event.getX() * scaleX;
            //x = decor.getWidth() - x; // invert x
            float y = event.getY() * scaleY;
            //y = decor.getHeight() - y; // invert y
            MotionEvent touchEvent = MotionEvent.obtain(event.getDownTime(),
                    event.getEventTime(), event.getAction(), x, y, event.getMetaState());
            return dispatchTouchEvent(touchEvent);
        }
        return super.onGenericMotionEvent(event);
    }
}
