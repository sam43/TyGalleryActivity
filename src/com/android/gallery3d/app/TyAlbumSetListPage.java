/*
 * TIANYU: yuxin add for New Design Gallery
 */


package com.android.gallery3d.app;

import android.app.AlertDialog;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.graphics.Point;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.os.Environment;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.MotionEvent;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.Toast;
import android.widget.ListView;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.ImageView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.AdapterView;
import android.text.TextWatcher;
import android.text.Editable;
import android.text.TextUtils;

import com.android.gallery3d.R;
import com.android.gallery3d.common.Utils;
import com.android.gallery3d.data.DataManager;
import com.android.gallery3d.data.MediaDetails;
import com.android.gallery3d.data.MediaItem;
import com.android.gallery3d.data.MediaObject;
import com.android.gallery3d.data.MediaSet;
import com.android.gallery3d.data.Path;
import com.android.gallery3d.data.BucketHelper;
import com.android.gallery3d.glrenderer.FadeTexture;
import com.android.gallery3d.glrenderer.GLCanvas;
import com.android.gallery3d.picasasource.PicasaSource;
import com.android.gallery3d.settings.GallerySettings;
import com.android.gallery3d.ui.ActionModeHandler;
import com.android.gallery3d.ui.ActionModeHandler.ActionModeListener;
import com.android.gallery3d.ui.TyAlbumSetListRenderer;
import com.android.gallery3d.ui.DetailsHelper;
import com.android.gallery3d.ui.DetailsHelper.CloseListener;
import com.android.gallery3d.ui.SelectionManager;
import com.android.gallery3d.ui.SlotView;
import com.android.gallery3d.ui.SynchronizedHandler;
import com.android.gallery3d.ui.TyActionModeView;
import com.android.gallery3d.ui.MenuExecutor;
import com.android.gallery3d.ui.GLView;
import com.android.gallery3d.util.Future;
import com.android.gallery3d.util.GalleryUtils;
import com.android.gallery3d.util.HelpUtils;
import com.android.gallery3d.util.MediaSetUtils;
import com.android.gallery3d.app.TyAlbumSetListAdapter;

import java.io.File;
import java.util.ArrayList;

public class TyAlbumSetListPage extends ActivityState implements
        SelectionManager.SelectionListener, MediaSet.SyncListener, GalleryActionBar.OnActionBarListener
        ,TyGalleryBottomBar.OnBottomBarListener{
    @SuppressWarnings("unused")
    private static final String TAG = "Gallery2/TyAlbumSetListPage";

    private static final int MSG_PICK_ALBUM = 1;

	private RelativeLayout mListlayout;
	private TyDragSortListView mListView;
    private RelativeLayout.LayoutParams mListViewParams;
	private TyAlbumSetListAdapter mTyAlbumSetListAdapter;
    
    private TyActionModeView mTyActionModeView;
    private MenuItem mItemSelected;
    private  MenuExecutor mMenuExecutor;
    private boolean mInDeleteMode;

    public static final String KEY_MEDIA_PATH = "media-path";
    public static final String KEY_SET_TITLE = "set-title";
    public static final String KEY_SET_SUBTITLE = "set-subtitle";
    public static final String KEY_SELECTED_CLUSTER_TYPE = "selected-cluster";
    public static final String KEY_EMPTY_ALBUM = "empty-album";
    public static final String KEY_CLOSEPAGE_IF_NONEDATA = "closepage-none-data";

    private static final int DATA_CACHE_SIZE = 256;
    private static final int REQUEST_DO_ANIMATION = 1;
    private static final int REQUEST_DO_NEW_FOLDER = 2;

    private static final int BIT_LOADING_RELOAD = 1;
    private static final int BIT_LOADING_SYNC = 2;

    private boolean mIsActive = false;
    private TyAlbumSetListRenderer mTyAlbumSetListRenderer;

    private MediaSet mMediaSet;
    private String mTitle;
    private String mSubtitle;
    private GalleryActionBar mActionBar;
    private TyGalleryBottomBar mTyGalleryBottomBar;

    protected SelectionManager mSelectionManager;
    private TyAlbumSetListDataLoader mTyAlbumSetListDataLoader;

    private boolean mGetContent;
    private boolean mGetAlbum;
    private Handler mHandler;
    
    // The eyes' position of the user, the origin is at the center of the
    // device and the unit is in pixels.
    private float mX;
    private float mY;
    private float mZ;

    private Future<Integer> mSyncTask = null;

    private int mLoadingBits = 0;
    private boolean mInitialSynced = false;

    private boolean mShowedEmpty = false;
    private View mTyEmptyAlbumLayout;
    private boolean mClosePageIfNoneData = false;

    // save selection for onPause/onResume
    private boolean mNeedUpdateSelection = false;

    private Menu mTyAlbumsMenu;
    private String mNewPath = null;
    private boolean isCreateNewAlbum = false;
    
    @Override
    protected int getBackgroundColorId() {
        return R.color.albumset_background;
    }
    
    private final GLView mRootPane = new GLView() {
        @Override
        protected void onLayout(
                boolean changed, int left, int top, int right, int bottom) {
        }
    };
    

    @Override
    public void onBackPressed() {
        if (mSelectionManager.inSelectionMode()) {
            mSelectionManager.leaveSelectionMode();
        } else {
            while(mActivity.getStateManager().getStateCount() > 0){
                mActivity.getStateManager().finishState(mActivity.getStateManager().getTopState());
            }
        }
    }

    private static boolean albumShouldOpenInFilmstrip(MediaSet album) {
        int itemCount = album.getMediaItemCount();
        ArrayList<MediaItem> list = (itemCount == 1) ? album.getMediaItem(0, 1) : null;
        // open in film strip only if there's one item in the album and the item exists
        return (list != null && !list.isEmpty());
    }

    private void pickAlbum(int slotIndex) {
        if (!mIsActive) return;
        MediaSet targetSet = mTyAlbumSetListDataLoader.getMediaSet(slotIndex);
		if (targetSet == null) return; // Content is dirty, we shall reload soon
		
        hideTyEmptyAlbumLayout();

        String mediaPath = targetSet.getPath().toString();
        Bundle data = new Bundle(getData());
        if (mGetAlbum && targetSet.isLeafAlbum()) {
            Activity activity = (Activity)mActivity.getAndroidContext();
            Intent result = new Intent()
                    .putExtra(AlbumPicker.KEY_ALBUM_PATH, targetSet.getPath().toString());
            activity.setResult(Activity.RESULT_OK, result);
            activity.finish();
        } else if (targetSet.getSubMediaSetCount() > 0) {
            data.putString(AlbumSetPage.KEY_MEDIA_PATH, mediaPath);
            mActivity.getStateManager().startStateForResult(
                    AlbumSetPage.class, REQUEST_DO_ANIMATION, data);
        } else {
            //TY zhencc delete for New Design Gallery
            /*if (!mGetContent && albumShouldOpenInFilmstrip(targetSet)) {
                data.putInt(PhotoPage.KEY_INDEX_HINT, 0);
                data.putString(PhotoPage.KEY_MEDIA_SET_PATH,
                        mediaPath);
                data.putBoolean(PhotoPage.KEY_START_IN_FILMSTRIP, true);
                data.putBoolean(PhotoPage.KEY_IN_CAMERA_ROLL, targetSet.isCameraRoll());
                mActivity.getStateManager().startStateForResult(
                        FilmstripPage.class, AlbumPage.REQUEST_PHOTO, data);
                return;
            }*/
            //TY zhencc end for New Design Gallery
            data.putString(AlbumPage.KEY_MEDIA_PATH, mediaPath);

            // We only show cluster menu in the first AlbumPage in stack
            boolean inAlbum = mActivity.getStateManager().hasStateClass(AlbumPage.class);
            data.putBoolean(AlbumPage.KEY_SHOW_CLUSTER_MENU, !inAlbum);
            mActivity.getStateManager().startStateForResult(
                    AlbumPage.class, REQUEST_DO_ANIMATION, data);
                mTyAlbumSetListRenderer.setPressedIndex(-1);
        }
    }

    @Override
    public void onCreate(Bundle data, Bundle restoreState) {  
        super.onCreate(data, restoreState);
        mActionBar = mActivity.getGalleryActionBar();
        mTyGalleryBottomBar = mActivity.getGalleryBottomBar();
        mGetContent = data.getBoolean(GalleryActivity.KEY_GET_CONTENT, false);
        mGetAlbum = data.getBoolean(GalleryActivity.KEY_GET_ALBUM, false);
        mTitle = data.getString(AlbumSetPage.KEY_SET_TITLE);
        mClosePageIfNoneData = data.getBoolean(TyAlbumSetListPage.KEY_CLOSEPAGE_IF_NONEDATA, false);
        mHandler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                switch (message.what) {
                    case MSG_PICK_ALBUM: {
                        pickAlbum(message.arg1);
                        break;
                    }
                    default: throw new AssertionError(message.what);
                }
            }
        };
        initializeViews();
        initializeData(data);
    }

    @Override
    public void onDestroy() {
        mTyActionModeView.destroy();
        super.onDestroy();
    }

    private void clearLoadingBit(int loadingBit) {
        mLoadingBits &= ~loadingBit;
        if (mLoadingBits == 0 && mIsActive) {
            if (mTyAlbumSetListDataLoader.size() == 0) {
                if (mClosePageIfNoneData && mActivity.getStateManager().getStateCount() > 1) {
                    Intent result = new Intent();
                    result.putExtra(TyAlbumSetListPage.KEY_EMPTY_ALBUM, true);
                    setStateResult(Activity.RESULT_OK, result);
                    mActivity.getStateManager().finishState(this);
                } else {
                    mShowedEmpty = true;
                    showTyEmptyAlbumLayout();
                    if (!mSelectionManager.inSelectionMode()) {
                        mTyGalleryBottomBar.enableCamMode(false);
                    }
                    refreshMenu();
                }
                return;
            }else{
                if (!mSelectionManager.inSelectionMode()){
                    if (!mGetContent && !mGetAlbum){
                        mTyGalleryBottomBar.enableAddMode(false);
                    }
                }
                refreshMenu();
                mTyAlbumSetListAdapter.notifyDataSetChanged();
            }
        }
        if (mShowedEmpty) {
            mShowedEmpty = false;
            hideTyEmptyAlbumLayout();
            if (!mSelectionManager.inSelectionMode()) {
                if (!mGetContent && !mGetAlbum){
                    if (mIsActive) mTyGalleryBottomBar.enableAddMode(false);
                }
            }
            refreshMenu();
        }
    }

    private void setLoadingBit(int loadingBit) {
        mLoadingBits |= loadingBit;
    }

    @Override
    public void onPause() {
        super.onPause();
        mIsActive = false;
        mTyActionModeView.pause(); 
    	if (mSelectionManager.inSelectionMode()) {
            mSelectionManager.leaveSelectionMode();
        }

        mTyAlbumSetListDataLoader.pause();
        mTyAlbumSetListRenderer.pause();
        
        tyHideListLayout();
        if (mSyncTask != null) {
            mSyncTask.cancel();
            mSyncTask = null;
            clearLoadingBit(BIT_LOADING_SYNC);
        }
    }

    @Override
    public void onResume() {
        if(isCreateNewAlbum)return;//TY wb034 02150126 add begin for tygallery
        super.onResume();
        mIsActive = true;
        // Set the reload bit here to prevent it exit this page in clearLoadingBit().
        setLoadingBit(BIT_LOADING_RELOAD);
        setContentPane(mRootPane);
        
        mTyGalleryBottomBar.setOnClickListener(this);
        if (mGetContent || mGetAlbum){
            mTyGalleryBottomBar.enableNoneMode(false);
        }else{
            if (!mSelectionManager.inSelectionMode()) {
                StateManager tempSm = mActivity.getStateManager();
                if (tempSm.getStateCount() == 1){
                    boolean bEnableClusterMenu = true;
                    ArrayList<StateManager> sms = mActivity.getOtherStateManagers(tempSm);
                    if (sms != null && sms.size() > 0){
                        for (StateManager sm : sms){
                            if (sm.getStateCount() > 1 ){
                                bEnableClusterMenu = false;
                            }
                        }
                    }
                    if (bEnableClusterMenu){
                        mActionBar.enableTabMode(false);
                    }
                }
            }
        }
        tyShowListLayout();

        mTyAlbumSetListDataLoader.resume();
        mTyAlbumSetListRenderer.resume();
        
        /*Tywangqin add for gallery2 begin*/
		mTyActionModeView.resume();
        /*Tywangqin add for gallery2 end*/
        if (!mInitialSynced) {
            setLoadingBit(BIT_LOADING_SYNC);
            mSyncTask = mMediaSet.requestSync(TyAlbumSetListPage.this);
        }
    }
    private SectionController c;//TY wb034 add for New Design Gallery
    private void initializeData(Bundle data) {
        String mediaPath = data.getString(AlbumSetPage.KEY_MEDIA_PATH);

        mMediaSet = mActivity.getDataManager().getMediaSet(mediaPath);
        mSelectionManager.setSourceMediaSet(mMediaSet);

        mTyAlbumSetListDataLoader = new TyAlbumSetListDataLoader(
                mActivity, mMediaSet, DATA_CACHE_SIZE);
        mTyAlbumSetListDataLoader.setLoadingListener(new MyLoadingListener());

        mTyAlbumSetListAdapter = new TyAlbumSetListAdapter(mActivity.getAndroidContext(), mSelectionManager);
        mListView.setAdapter(mTyAlbumSetListAdapter);
        mListView.setDropListener(mTyAlbumSetListAdapter);
        mTyAlbumSetListRenderer.setModel(mTyAlbumSetListDataLoader);
		
        c = new SectionController(mListView, mTyAlbumSetListAdapter);
        mListView.setFloatViewManager(c);
        mListView.setOnTouchListener(c);
    }

    private void initializeViews() {
        mSelectionManager = new SelectionManager(mActivity, true);
        mSelectionManager.setSelectionListener(this);

        mMenuExecutor = new MenuExecutor(mActivity, mSelectionManager);
        mTyActionModeView = new TyActionModeView(mActivity, mSelectionManager, true);
        tyShowListLayout();
        mTyAlbumSetListRenderer = new TyAlbumSetListRenderer(
            mActivity, mSelectionManager, mListView);
    }

    @Override
    protected boolean onCreateActionBar(Menu menu) {
        Activity activity = (Activity)mActivity.getAndroidContext();
        MenuInflater inflater = getSupportMenuInflater();
        if (mGetContent) {
            int typeBits = mData.getInt(
                    GalleryActivity.KEY_TYPE_BITS, DataManager.INCLUDE_IMAGE);
            mActionBar.enableBackMode(this, false);
            mActionBar.showCamera(false);
            mActionBar.setTitle(GalleryUtils.getSelectionModePrompt(typeBits));
        } else  if (mGetAlbum) {
            inflater.inflate(R.menu.pickup, menu);
            mActionBar.enableBackMode(this, false);
            mActionBar.showCamera(false);
            mActionBar.setTitle(R.string.select_album);
        } else {
            inflater.inflate(R.menu.ty_albums, menu);
            mTyAlbumsMenu = menu;
            if (!mSelectionManager.inSelectionMode()) {
                mActionBar.enableTabMode(false);
            }
            refreshMenu();
		}
        return true;
    }

    @Override
    protected boolean onItemSelected(MenuItem item) {
        Activity activity = (Activity)mActivity.getAndroidContext();
        switch (item.getItemId()) {
            case R.id.action_cancel:
                activity.setResult(Activity.RESULT_CANCELED);
                activity.finish();
                return true;
            case R.id.action_select:
                mSelectionManager.setAutoLeaveSelectionMode(false);
                mSelectionManager.enterSelectionMode();
                return true;
            case R.id.action_camera: {
                GalleryUtils.startCameraActivity(activity);
                return true;
            }
            case R.id.ty_action_delete:
            case R.id.ty_action_share:
            case R.id.ty_action_hide:	
            case R.id.ty_action_show:
                mItemSelected = item;
                mSelectionManager.setAutoLeaveSelectionMode(false);
                mSelectionManager.enterSelectionMode();
                return true;
            default:
                return false;
        }
    }
    
    @Override
    public void onActionBarClick(View v){
        switch (v.getId()) {
            case R.id.ty_action_title_close:{
                onBackPressed();
                break;
            }
            case R.id.ty_action_sel_close:{
                mSelectionManager.leaveSelectionMode();
                break;
            }
        }
    }
    
    @Override
    public void onBottomBarClick(View v){
        switch (v.getId()) {
            case R.id.ty_cam_act:
                GalleryUtils.startCameraActivity(mActivity.getAndroidContext());
                break;
            case R.id.ty_add_act:
                newDir();
                break;
            default:
                break;
        }
    }

    @Override
    protected void onStateResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
        case REQUEST_DO_ANIMATION:
            isCreateNewAlbum = false; //TY wb034 02150126 add  for tygallery
            break;
        case REQUEST_DO_NEW_FOLDER:
            if (resultCode == Activity.RESULT_OK){
                if (BucketHelper.isNoneMediaData(mActivity.getAndroidContext().getContentResolver(),
                    String.valueOf(GalleryUtils.getBucketId(mNewPath)),
                    MediaObject.MEDIA_TYPE_ALL)){
                    return;
                }
                //TY wb034 02150126 add begin for tygallery
                isCreateNewAlbum = true;
                String mediaPath = mActivity.getDataManager().getTopSetPath(DataManager.INCLUDE_LOCAL_ALL_ONLY)
                        +"/"+String.valueOf(GalleryUtils.getBucketId(mNewPath));
                Bundle set = new Bundle(getData());    
                set.putString(AlbumPage.KEY_MEDIA_PATH, mediaPath);
                boolean inAlbum = mActivity.getStateManager().hasStateClass(AlbumPage.class);
                set.putBoolean(AlbumPage.KEY_SHOW_CLUSTER_MENU, !inAlbum);
                mActivity.getStateManager().startStateForResult(
                            AlbumPage.class, REQUEST_DO_ANIMATION, set);
                mTyAlbumSetListRenderer.setPressedIndex(-1);
                //TY wb034 02150126 add end for tygallery                 
            }
            break;
        }
    }

    public String getSelectedString() {
        int count = mSelectionManager.getSelectedCount();
        int string = R.plurals.number_of_groups_selected;
        String format = mActivity.getResources().getQuantityString(string, count);
        return String.format(format, count);
    }

    @Override
    public void onSelectionModeChange(int mode) {
        switch (mode) {
            case SelectionManager.ENTER_SELECTION_MODE: {
                mActionBar.enableSelectMode(this);
                refreshMenu();
                if (mItemSelected != null){
                    switch(mItemSelected.getItemId()){
                        case R.id.ty_action_hide:
                            mTyAlbumSetListAdapter.setTyMode(MediaObject.TY_HIDE_MODE);
                            mMediaSet.changeTyMode(MediaObject.TY_HIDE_MODE);
                            break;
                        case R.id.ty_action_show:
                            mTyAlbumSetListAdapter.setTyMode(MediaObject.TY_SHOW_MODE);
                            mMediaSet.changeTyMode(MediaObject.TY_SHOW_MODE);
                            break;
                        case R.id.ty_action_delete:
                            mTyAlbumSetListAdapter.setTyMode(MediaObject.TY_DELETE_MODE);
                            mListView.setDragEnabled(true);
                            break;
                        case R.id.ty_action_share:
                            mTyAlbumSetListAdapter.setTyMode(MediaObject.TY_SHARE_MODE);
                            break;
                        default:
                            mTyAlbumSetListAdapter.setTyMode(MediaObject.TY_NONE_MODE);
                            break;
                    }
                    mTyActionModeView.startActionMode(mItemSelected);
                }
                requestLayout();
                mTyAlbumSetListAdapter.notifyDataSetChanged();
                break;
            }
            case SelectionManager.LEAVE_SELECTION_MODE: {
                mListView.setDragEnabled(false);
                mActionBar.enableTabMode(true);
                if (mItemSelected != null){
                    mTyActionModeView.finishActionMode();
                    mTyAlbumSetListAdapter.setTyMode(MediaObject.TY_NONE_MODE);
                    mMediaSet.changeTyMode(MediaObject.TY_NONE_MODE);
                    mTyGalleryBottomBar.enableAddMode(true);
                }
                
                mTyGalleryBottomBar.setOnClickListener(this);
                requestLayout();
                mTyAlbumSetListAdapter.notifyDataSetChanged();
				break;
            }
            // M: when click deselect all in menu, not leave selection mode
            case SelectionManager.DESELECT_ALL_MODE:
            case SelectionManager.SELECT_ALL_MODE: {
                if(mItemSelected != null){
                    mTyActionModeView.updateSupportedOperation();
                }
                mTyAlbumSetListAdapter.notifyDataSetChanged();
                break;
            }
        }
    }

    @Override
    public void onSelectionChange(Path path, boolean selected) {
        /*Tywangqin add for gallery2 begin*/
        int count = mSelectionManager.getSelectedCount();
        String format = mActivity.getResources().getQuantityString(
            R.plurals.ty_number_of_albums_selected, count, count);
        mTyActionModeView.setTitle(format);
        mTyActionModeView.updateSupportedOperation(path, selected);
        /*Tywangqin add for gallery2 end*/
	}

    @Override
    public void onSyncDone(final MediaSet mediaSet, final int resultCode) {
        if (resultCode == MediaSet.SYNC_RESULT_ERROR) {
            Log.d(TAG, "onSyncDone: " + Utils.maskDebugInfo(mediaSet.getName()) + " result="
                    + resultCode);
        }
        ((Activity) mActivity.getAndroidContext()).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (resultCode == MediaSet.SYNC_RESULT_SUCCESS) {
                        mInitialSynced = true;
                    }
                    clearLoadingBit(BIT_LOADING_SYNC);
                    if (resultCode == MediaSet.SYNC_RESULT_ERROR && mIsActive) {
                        Log.w(TAG, "failed to load album set");
                    }
                } finally {
                }
            }
        });
    }

    private class MyLoadingListener implements LoadingListener {
        @Override
        public void onLoadingStarted() {
            setLoadingBit(BIT_LOADING_RELOAD);
        }

        @Override
        public void onLoadingFinished(boolean loadingFailed) {
            clearLoadingBit(BIT_LOADING_RELOAD);
            /*Tywangqin add for gallery2 begin*/
            if (mSelectionManager.inSelectionMode()){
                mTyActionModeView.updateSupportedOperation();
                mTyActionModeView.updateSelectionMenu();
            }
            /*Tywangqin add for gallery2 end*/ 
        }
    }
    
    private void newDir() {
        try {
            View newAlertView = ((Activity)mActivity.getAndroidContext()).getLayoutInflater().from(mActivity.getAndroidContext())
                .inflate(R.layout.ty_new_alert, null);
            final EditText nameEdit = (EditText) newAlertView.findViewById(R.id.new_edit);
            final AlertDialog nameDlg = new AlertDialog.Builder(mActivity.getAndroidContext()).create();
            nameDlg.setView(newAlertView);
            nameDlg.setTitle(R.string.ty_newalbum);
            //nameDlg.setIcon(android.R.drawable.ic_input_add);
            nameDlg.setButton(DialogInterface.BUTTON_POSITIVE,
                mActivity.getAndroidContext().getString(R.string.confirm), 
                new DialogInterface.OnClickListener(){
                    public void onClick(DialogInterface dialog, int which) {
                        final String newName = nameEdit.getText().toString().trim();
                        String newPath = Environment.getExternalStorageDirectory().toString() 
                            + File.separator + newName;
                        if (!BucketHelper.isNoneMediaData(mActivity.getAndroidContext().getContentResolver(),
                            String.valueOf(GalleryUtils.getBucketId(newPath)),
                            MediaObject.MEDIA_TYPE_ALL)){
                            Toast.makeText(mActivity.getAndroidContext(), mActivity.getAndroidContext().getText(R.string.ty_folder_exist),
                                Toast.LENGTH_SHORT).show();
                            return;
                        }
                        tyTransferAlbumTimePage(newPath);
                    }
                }
            );
            nameDlg.setButton(DialogInterface.BUTTON_NEGATIVE,
                mActivity.getAndroidContext().getString(R.string.cancel), 
                new DialogInterface.OnClickListener(){
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                }
            );	

            nameEdit.addTextChangedListener(new TextWatcher(){
                @Override
                public void afterTextChanged(Editable s) {
                    if(s.toString().length()<=0){
                        nameDlg.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(false);
                    }else{
                        nameDlg.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(true);
                    }
                }
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count,int after) {
                }
        
                @Override
                public void onTextChanged(CharSequence s, int start, int before,int count) {
                }
            });
            
            mHandler.postDelayed(new Runnable(){
                public void run() {
                    InputMethodManager imm = (InputMethodManager)mActivity.getAndroidContext()
                        .getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (imm != null) {
                        if (!imm.showSoftInput(nameEdit, 0)) {
                            Log.w(TAG, "Failed to show soft input method.");
                        }
                    }
                }
            }, 150);
            
            nameDlg.show();
	        String name = nameEdit.getText().toString().trim();
            if (TextUtils.isEmpty(name)) {
                nameDlg.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(false);
            }
        } catch (Exception e) {
            Log.d(TAG, "new dir error is " + e.getMessage());
        }
        return;
    }
   
    private void tyTransferAlbumTimePage(String newPath) {
        mNewPath = newPath;//TY wb034 02150126 add begin for tygallery
        Bundle data = new Bundle();
        data.putString(AlbumSetPage.KEY_MEDIA_PATH,
            mActivity.getDataManager().getTopSetPath(DataManager.INCLUDE_ALL));
        data.putString(TyAlbumTimePage.KEY_ADD_PHOTO_DESTINATION_PATH, newPath);
        data.putBoolean(TyAlbumTimePage.KEY_DIRECT_START_SELECTED_MODE, true);        
        mActivity.getStateManager().startStateForResult(
            TyAlbumTimePage.class, REQUEST_DO_NEW_FOLDER, data);
    }
    
    private void showTyEmptyAlbumLayout() {
        if (mTyEmptyAlbumLayout == null){
            RelativeLayout galleryRoot = (RelativeLayout) mActivity.getGalleryAssignView(R.id.gallery_root);
            if (galleryRoot == null) return;
            mTyEmptyAlbumLayout = galleryRoot.findViewById(R.id.ty_empty_layout);
            ImageView eImage = (ImageView)mTyEmptyAlbumLayout.findViewById(R.id.ty_empty_image);
            TextView eText = (TextView)mTyEmptyAlbumLayout.findViewById(R.id.ty_empty_text);
            eImage.setImageResource(R.drawable.ty_ic_gallery_album_blank);
            eText.setText(R.string.ty_empty_album);
        }
        mTyEmptyAlbumLayout.setVisibility(View.VISIBLE);
    }

    private void hideTyEmptyAlbumLayout() {
        if (mTyEmptyAlbumLayout == null) return;
            mTyEmptyAlbumLayout.setVisibility(View.GONE);
    }
    
    private void tyShowListLayout() {
        if (mListlayout == null){
            RelativeLayout galleryRoot = (RelativeLayout) mActivity.getGalleryAssignView(R.id.gallery_root);
            if (galleryRoot == null) return;
            mListlayout = (RelativeLayout) galleryRoot.findViewById(R.id.listpanel);

            mListView = (TyDragSortListView) mListlayout.findViewById(R.id.account_list); 
            mListViewParams = (RelativeLayout.LayoutParams) mListlayout.getLayoutParams();
            mListView.setOnItemClickListener(new OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    if (mSelectionManager.inSelectionMode()) {
                        MediaSet targetSet = mTyAlbumSetListDataLoader.getMediaSet(position);
                        mSelectionManager.toggle(targetSet.getPath());
                        mTyAlbumSetListAdapter.notifyDataSetChanged();
                    }else{
                        mTyAlbumSetListRenderer.setPressedIndex(position);
                        mTyAlbumSetListRenderer.setPressedUp();
                        mHandler.sendMessage(mHandler.obtainMessage(MSG_PICK_ALBUM, position, 0));
                    }
                }
            });
            mListView.setOnItemLongClickListener(new OnItemLongClickListener() {
                @Override
                public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                    if (mGetContent || mGetAlbum) return true;
                    
                    MediaSet targetSet = mTyAlbumSetListDataLoader.getMediaSet(position);
                    if (targetSet == null) return false;
                    if (mSelectionManager.inSelectionMode()){
                        mSelectionManager.toggle(targetSet.getPath());
                        mTyAlbumSetListAdapter.notifyDataSetChanged();
                    }else{
                        if (mTyAlbumsMenu != null){
                            mItemSelected = mTyAlbumsMenu.findItem(R.id.ty_action_delete);
                            mSelectionManager.setAutoLeaveSelectionMode(false);
                            mInDeleteMode = true;
                            mSelectionManager.toggle(targetSet.getPath());
                        }
                    }
                    return true;
                }
            });
            requestLayout();
        }
        mListlayout.setVisibility(View.VISIBLE);
    }

    private void tyHideListLayout() {
        if (mListlayout == null) return;
        mListlayout.setVisibility(View.GONE);
    }
    
    //TY zhencc add for New Design Gallery
    private class SectionController extends TyDragSortController {

		private int mPos;
		private int mDivPos;
		private TyAlbumSetListAdapter mAdapter;
		TyDragSortListView mDslv;
        private Bitmap mFloatBitmap;
        private ImageView mImageView;

		public SectionController(TyDragSortListView dslv, TyAlbumSetListAdapter adapter) {
			super(dslv, R.id.drag_handle, TyDragSortController.ON_DOWN, 0);
			setRemoveEnabled(false);
			mDslv = dslv;
			mAdapter = adapter;
			mDivPos = adapter.getDivPosition();
		}

		@Override
		public int startDragPosition(MotionEvent ev) {
			int res = super.dragHandleHitPosition(ev);
			mDivPos = mAdapter.getDivPosition();
			if (res <= mDivPos) {
				return TyDragSortController.MISS;
			}
			return res;
		}

        @Override
        public View onCreateFloatView(int position) {
            mPos = position;
            //View v = mAdapter.getView(position, null, mDslv);
            View v = mDslv.getChildAt(position + mDslv.getHeaderViewsCount() - mDslv.getFirstVisiblePosition());
        
            if (v == null) {
                return null;
            }
        
            v.setPressed(false);
            v.setDrawingCacheEnabled(true);
            mFloatBitmap = Bitmap.createBitmap(v.getDrawingCache());
            v.setDrawingCacheEnabled(false);
        
            if (mImageView == null) {
                mImageView = new ImageView(mListView.getContext());
            }
            mImageView.setBackgroundResource(R.drawable.ty_list_item_drag_bg);
            mImageView.setPadding(0, 0, 0, 0);
            mImageView.setImageBitmap(mFloatBitmap);
            mImageView.setLayoutParams(new ViewGroup.LayoutParams(v.getWidth(), v.getHeight()));
        
            return mImageView;
        }

		@Override
		public void onDragFloatView(View floatView, Point floatPoint,
				 Point floatDrawPoint, Point touchPoint) {
			
			final int first = mDslv.getFirstVisiblePosition();
			final int lvDivHeight = mDslv.getDividerHeight();

			mDivPos = mAdapter.getDivPosition();
			View div = mDslv.getChildAt(mDivPos - first);

			if (div != null) {
				if (mPos > mDivPos) {
					// don't allow floating View to go above
					// section divider
					final int limit = div.getBottom() + lvDivHeight;
					if (floatPoint.y < limit) {
						floatPoint.y = limit;
					}
				} else {
					// don't allow floating View to go below
					// section divider
					final int limit = div.getBottom() - lvDivHeight
							- floatView.getHeight();
					if (floatPoint.y > limit) {
						floatPoint.y = limit;
						floatDrawPoint.y = limit;
					}
				}
			}
		}

		@Override
		public void onDestroyFloatView(View floatView) {
            if (mFloatBitmap != null){
                mFloatBitmap.recycle();
                mFloatBitmap = null;
            }
		}

		@Override
		public int getDivPos() {
			// TODO Auto-generated method stub
			return mDivPos;
		}
		//TY wb034 20150119 add begin for tygallery
		public void refresh(){
			mAdapter.notifyDataSetChanged();
		}
		//TY wb034 20150119 add end for tygallery
	}
    //TY zhencc end for New Design Gallery
    
    @Override
	protected void onConfigurationChanged(Configuration config) {
		requestLayout();
		super.onConfigurationChanged(config);
	}
    private void requestLayout(){
        if (mListlayout == null) return;
        int top = mActionBar.getExpectHeight();
        int bottom = mTyGalleryBottomBar.getAreaHeight(TyGalleryBottomBar.TyHeightKind.TySelect);
        if (mListViewParams.topMargin != top
            || mListViewParams.bottomMargin != bottom){
            mListViewParams.topMargin = top;
            mListViewParams.bottomMargin = bottom;
            mListlayout.setLayoutParams(mListViewParams);
            mListlayout.invalidate();
        }
     }

    private void refreshMenu(){
        if (mTyAlbumsMenu == null) return;
        
        if (mSelectionManager.inSelectionMode()){
            mTyAlbumsMenu.setGroupVisible(R.id.ty_albums_operation_menu, false);
        } else{
            int iSize = mTyAlbumSetListDataLoader.size();
            
            MenuItem deleteMenu = mTyAlbumsMenu.findItem(R.id.ty_action_delete);
            if(deleteMenu != null){
                deleteMenu.setVisible(iSize > 0);
            }
            
            MenuItem shareMenu = mTyAlbumsMenu.findItem(R.id.ty_action_share);
            if(shareMenu != null){
                shareMenu.setVisible(iSize > 0);
            }
            
            MenuItem showMenu = mTyAlbumsMenu.findItem(R.id.ty_action_show);
            if(showMenu != null){
                int hideCount = mActivity.getDataManager().getHideCount();
                if (hideCount > 0){
                    showMenu.setVisible(true);
                    showMenu.setTitle(String.format(
                            mActivity.getAndroidContext().getString(R.string.ty_showhidden_with_count), 
                            hideCount));
                }else{
                    showMenu.setVisible(false);
                }

            }

            MenuItem hideMenu = mTyAlbumsMenu.findItem(R.id.ty_action_hide);
            if (hideMenu != null){
                boolean isVisible = false;
                if (iSize > 2){
                    isVisible = true;
                }else{
                    for (int i = 0; i < iSize ; i++){
                        int bucketId = mTyAlbumSetListDataLoader.getMediaSet(i).getBucketId();
                        if (bucketId != MediaSetUtils.CAMERA_BUCKET_ID
                            && bucketId != mActivity.getDataManager().mCollectBucketId){
                            isVisible = true;
                            break;
                        }
                    }
                }
                hideMenu.setVisible(isVisible);
            }
        }
    }
}
