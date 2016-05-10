/*
 * TIANYU: yuxin add for New Design Gallery
 */
package com.android.gallery3d.app;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.RelativeLayout;
import android.widget.Toast;
import com.android.gallery3d.ui.TyActionModeView;
import com.android.gallery3d.R;
import com.android.gallery3d.common.Utils;
import com.android.gallery3d.data.DataManager;
import com.android.gallery3d.data.MediaDetails;
import com.android.gallery3d.data.MediaItem;
import com.android.gallery3d.data.MediaObject;
import com.android.gallery3d.data.MediaSet;
import com.android.gallery3d.data.Path;
import com.android.gallery3d.filtershow.crop.CropActivity;
import com.android.gallery3d.filtershow.crop.CropExtras;
import com.android.gallery3d.glrenderer.FadeTexture;
import com.android.gallery3d.glrenderer.GLCanvas;
import com.android.gallery3d.ui.ActionModeHandler;
import com.android.gallery3d.ui.ActionModeHandler.ActionModeListener;
import com.android.gallery3d.ui.TyAlbumTimeSlotRenderer;
import com.android.gallery3d.ui.DetailsHelper;
import com.android.gallery3d.ui.DetailsHelper.CloseListener;
import com.android.gallery3d.ui.GLRoot;
import com.android.gallery3d.ui.GLView;
import com.android.gallery3d.ui.PhotoFallbackEffect;
import com.android.gallery3d.ui.RelativePosition;
import com.android.gallery3d.ui.SelectionManager;
import com.android.gallery3d.ui.TySlotView;
import com.android.gallery3d.ui.SynchronizedHandler;
import com.android.gallery3d.util.Future;
import com.android.gallery3d.util.GalleryUtils;
import com.android.gallery3d.util.MediaSetUtils;
import java.util.ArrayList;
import java.util.Random;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Handler;
import com.android.gallery3d.ui.SynchronizedHandler;
import com.android.gallery3d.util.ThreadPool.Job;
import com.android.gallery3d.util.ThreadPool.JobContext;
import android.util.Log;
import com.android.gallery3d.ui.MenuExecutor;
//TY wb034 20150209 add begin for tygallery
import com.android.gallery3d.ui.TySlotView.Spec;
import android.content.res.Configuration;
//TY wb034 20150209 add end for tygallery
import com.android.gallery3d.ui.GLRoot.OnGLChanegeListener;
//TY liuyuchuan add begin for  New Design Gallery
import android.graphics.BitmapFactory;
//TY liuyuchuan add end for  New Design Gallery

public class TyAlbumTimePage extends ActivityState implements SelectionManager.SelectionListener
        ,MediaSet.SyncListener, GalleryActionBar.OnActionBarListener, TyGalleryBottomBar.OnBottomBarListener{
    @SuppressWarnings("unused")
    private static final String TAG = "TyAlbumTimePage";

    public static final String KEY_MEDIA_PATH = "media-path";
    public static final String KEY_PARENT_MEDIA_PATH = "parent-media-path";
    public static final String KEY_SET_CENTER = "set-center";
    public static final String KEY_AUTO_SELECT_ALL = "auto-select-all";
    public static final String KEY_SHOW_CLUSTER_MENU = "cluster-menu";
    public static final String KEY_EMPTY_ALBUM = "empty-album";
    public static final String KEY_RESUME_ANIMATION = "resume_animation";
    public static final String KEY_DIRECT_START_SELECTED_MODE = "direct_start_selected_mode";
	public static final String KEY_ADD_PHOTO_DESTINATION_PATH = "add_photo_to_destination_path";
	private String mAddPhotoToDirString;
    private  MenuExecutor mMenuExecutor;
    private static final int REQUEST_SLIDESHOW = 1;
    public static final int REQUEST_PHOTO = 2;
    private static final int REQUEST_DO_ANIMATION = 3;

    private static final int BIT_LOADING_RELOAD = 1;
    private static final int BIT_LOADING_SYNC = 2;

    private static final float USER_DISTANCE_METER = 0.3f;

    private ProgressDialog mProgressDialog;
    private Future<?> mConvertUriTask;
    private boolean mIsActive = false;
    private TyAlbumTimeSlotRenderer mTyAlbumTimeSlotRenderer;
    private Path mMediaSetPath;
    private String mParentMediaSetString;
    private TySlotView mTySlotView;

    private TyAlbumTimeDataLoader mTyAlbumTimeDataLoader;

    protected SelectionManager mSelectionManager;

    private boolean mGetContent;
    private TyActionModeView mTyActionModeView;
    private MenuItem mItemSelected;
    private MenuItem mItemDelete;
    private boolean mDirectStartSelectedMode;

    private int mFocusIndex = 0;
    private DetailsHelper mDetailsHelper;
    private MyDetailsSource mDetailsSource;
    private MediaSet mBaseMediaSet;
    private MediaSet mMediaSet;
    private boolean mShowDetails;
    private float mUserDistance; // in pixel
    private Future<Integer> mSyncTask = null;
    private boolean mLaunchedFromPhotoPage;
    private boolean mInCameraApp;
    private boolean mInCameraAndWantQuitOnPause;

    private int mLoadingBits = 0;
    private boolean mInitialSynced = false;
    private int mSyncResult;
    private boolean mLoadingFailed;
    private RelativePosition mOpenCenter = new RelativePosition();

    private Handler mHandler;
    private static final int MSG_PICK_PHOTO = 0;

    private PhotoFallbackEffect mResumeEffect;

    private boolean mShowedEmpty = false;
    private View mTyEmptyLayout;
    private Menu mTyAlbumMenu;
    
    private GalleryActionBar mActionBar;
    private TyGalleryBottomBar mTyGalleryBottomBar;
    //TY wb034 20150210 add begin for tygallery
    private Config.TimeGroupPage mConfig ;
    //TY wb034 20150210 add end for tygallery
	    
    private OnGLChanegeListener mGLChanegeListener = new OnGLChanegeListener(){
        @Override
        public void onGLChanege() {
            mTyAlbumTimeSlotRenderer.setReserveData(false);
        }
    };
    
    private PhotoPage.OnPhotoChanegeListener mPhotoChanegeListener = new PhotoPage.OnPhotoChanegeListener(){
        @Override
        public void onPhotoChanege(int index) {
            if (mFocusIndex != index){
                mFocusIndex = index;
                mTySlotView.makeSlotVisible(mFocusIndex);
            }
        }
    };

    private PhotoFallbackEffect.PositionProvider mPositionProvider =
            new PhotoFallbackEffect.PositionProvider() {
        @Override
        public Rect getPosition(int index) {
            Rect rect = mTySlotView.getSlotRect(index);
            Rect bounds = mTySlotView.bounds();
            rect.offset(bounds.left - mTySlotView.getScrollX(),
                    bounds.top - mTySlotView.getScrollY());
            return rect;
        }

        @Override
        public int getItemIndex(Path path) {
            int start = mTySlotView.getVisibleStart();
            int end = mTySlotView.getVisibleEnd();
            for (int i = start; i < end; ++i) {
                MediaItem item = mTyAlbumTimeDataLoader.get(i);
                if (item != null && item.getPath() == path) return i;
            }
            return -1;
        }
    };

    @Override
    protected int getBackgroundColorId() {
        return R.color.album_background;
    }

    private final GLView mRootPane = new GLView() {
        private final float mMatrix[] = new float[16];

        @Override
        protected void onLayout(
                boolean changed, int left, int top, int right, int bottom) {

            int slotViewTop = 0;
            int slotViewBottom = bottom - top;
            int slotViewRight = right - left;
            mConfig.setMarin(mActionBar.getHeight(),
                    mTyGalleryBottomBar.getAreaHeight(TyGalleryBottomBar.TyHeightKind.TySelect));

            if (mShowDetails) {
                mDetailsHelper.layout(left, slotViewTop, right, bottom);
            } else {
                mTyAlbumTimeSlotRenderer.setHighlightItemPath(null);
            }

            // Set the mTySlotView as a reference point to the open animation
            mOpenCenter.setReferencePosition(0, slotViewTop);
            mTySlotView.layout(0, slotViewTop, slotViewRight, slotViewBottom);
            GalleryUtils.setViewPointMatrix(mMatrix,
                    (right - left) / 2, (bottom - top) / 2, -mUserDistance);
        }

        @Override
        protected void render(GLCanvas canvas) {
            canvas.save(GLCanvas.SAVE_FLAG_MATRIX);
            canvas.multiplyMatrix(mMatrix, 0);
            super.render(canvas);

            if (mResumeEffect != null) {
                boolean more = mResumeEffect.draw(canvas);
                if (!more) {
                    mResumeEffect = null;
                    mTyAlbumTimeSlotRenderer.setSlotFilter(null);
                }
                // We want to render one more time even when no more effect
                // required. So that the animated thumbnails could be draw
                // with declarations in super.render().
                invalidate();
            }
            canvas.restore();
        }
    };

    // This are the transitions we want:
    //
    // +--------+           +------------+    +-------+    +----------+
    // | Camera |---------->| Fullscreen |--->| Album |--->| AlbumSet |
    // |  View  | thumbnail |   Photo    | up | Page  | up |   Page   |
    // +--------+           +------------+    +-------+    +----------+
    //     ^                      |               |            ^  |
    //     |                      |               |            |  |         close
    //     +----------back--------+               +----back----+  +--back->  app
    //
    @Override
    protected void onBackPressed() {
        if (mShowDetails) {
            hideDetails();
        } else if (mSelectionManager.inSelectionMode()) {
            //TY WB034 20150126 add begin for tygallery
            if(mDirectStartSelectedMode&&!MenuExecutor.mAddFromAlbumPage){
                new AlertDialog.Builder(mActivity.getAndroidContext())
                   .setTitle(mActivity.getResources().getString(R.string.prompt)) 
                   .setMessage(mActivity.getResources().getString(R.string.confirm_to_quit_creating_new_ablum))
                   .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                      @Override
                      public void onClick(DialogInterface arg0, int arg1) {
                          mSelectionManager.leaveSelectionMode();
                      }
                   })
                   .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                      @Override
                      public void onClick(DialogInterface arg0, int arg1) {
                          arg0.dismiss();
                      }
                   })
                  .create().show();
              }else{
                  mSelectionManager.leaveSelectionMode();
              }
            //TY WB034 20150126 add end for tygallery
        } else {
            if(mLaunchedFromPhotoPage) {
                mActivity.getTransitionStore().putIfNotPresent(
                        PhotoPage.KEY_ALBUMPAGE_TRANSITION,
                        PhotoPage.MSG_ALBUMPAGE_RESUMED);
            }
            // TODO: fix this regression
            if (mInCameraApp) {
                super.onBackPressed();
            } else {
                onUpPressed();
            }
        }
    }

    private void onUpPressed() {
        if (mInCameraApp) {
            GalleryUtils.startGalleryActivity(mActivity.getAndroidContext());
        } else if (mSelectionManager.inSelectionMode()) {
            mSelectionManager.leaveSelectionMode();
        } else if (mActivity.getStateManager().getStateCount() > 0) {
            super.onBackPressed();
        } else if (mParentMediaSetString != null) {
            Bundle data = new Bundle(getData());
            data.putString(AlbumSetPage.KEY_MEDIA_PATH, mParentMediaSetString);
            mActivity.getStateManager().switchState(
                    this, AlbumSetPage.class, data);
        }
    }

    private void onDown(int index) {
        mTyAlbumTimeSlotRenderer.setPressedIndex(index);
    }

    private void onUp(boolean followedByLongPress) {
        if (followedByLongPress) {
            // Avoid showing press-up animations for long-press.
            mTyAlbumTimeSlotRenderer.setPressedIndex(-1);
        } else {
            mTyAlbumTimeSlotRenderer.setPressedUp();
        }
    }

    private void onSingleTapUp(int slotIndex) {
        if (!mIsActive) return;

        if (mSelectionManager.inSelectionMode()) {
            MediaItem item = mTyAlbumTimeDataLoader.get(slotIndex);
            if (item == null) return; // Item not ready yet, ignore the click
            mSelectionManager.toggle(item.getPath());
            mTySlotView.invalidate();
        } else {
            // Render transition in pressed state
            mTyAlbumTimeSlotRenderer.setPressedIndex(slotIndex);
            mTyAlbumTimeSlotRenderer.setPressedUp();
            mHandler.sendMessageDelayed(mHandler.obtainMessage(MSG_PICK_PHOTO, slotIndex, 0),
                    FadeTexture.DURATION);
        }
    }

    private void pickPhoto(int slotIndex) {
        pickPhoto(slotIndex, false);
    }

    private void pickPhoto(int slotIndex, boolean startInFilmstrip) {
        if (!mIsActive) return;

        if (!startInFilmstrip) {
            // Launch photos in lights out mode
            mActivity.getGLRoot().setLightsOutMode(true);
        }
        MediaItem item = null;
        try {
            item = mTyAlbumTimeDataLoader.get(slotIndex);
        } catch (java.lang.NullPointerException ex) {
            item = null;
            ex.printStackTrace();
        }
        if (item == null) return; // Item not ready yet, ignore the click
        
        hideEmptyLayout();
        
        if (mGetContent) {
            onGetContent(item);
        } else if (mLaunchedFromPhotoPage) {
            TransitionStore transitions = mActivity.getTransitionStore();
            transitions.put(
                    PhotoPage.KEY_ALBUMPAGE_TRANSITION,
                    PhotoPage.MSG_ALBUMPAGE_PICKED);
            transitions.put(PhotoPage.KEY_INDEX_HINT, slotIndex);
            onBackPressed();
        } else {
            // Get into the PhotoPage.
            Bundle data = new Bundle();
            data.putInt(PhotoPage.KEY_INDEX_HINT, slotIndex);
            data.putParcelable(PhotoPage.KEY_OPEN_ANIMATION_RECT,
                    mTySlotView.getSlotRect(slotIndex, mRootPane));
            data.putString(PhotoPage.KEY_MEDIA_SET_PATH,
                    mMediaSetPath.toString());
            data.putString(PhotoPage.KEY_MEDIA_ITEM_PATH,
                    item.getPath().toString());
            data.putInt(PhotoPage.KEY_ALBUMPAGE_TRANSITION,
                    PhotoPage.MSG_ALBUMPAGE_STARTED);
            data.putBoolean(PhotoPage.KEY_START_IN_FILMSTRIP,
                    startInFilmstrip);
            data.putBoolean(PhotoPage.KEY_IN_CAMERA_ROLL, mMediaSet.isCameraRoll());
            //TY liuyuchuan add begin for  New Design Gallery
            if(item.getWidth() != 0 && item.getWidth() != 0){
                data.putInt(PhotoPage.mPictureWidth, item.getWidth());
                data.putInt(PhotoPage.mPictureHeight, item.getHeight());
            }else{
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                BitmapFactory.decodeFile(item.getFilePath(),options);
                data.putInt(PhotoPage.mPictureWidth, options.outWidth);
                data.putInt(PhotoPage.mPictureHeight, options.outHeight);
            }
            data.putString(PhotoPage.mPictureMimeType, item.getMimeType());
            //TY liuyuchuan add end for  New Design Gallery
            
            mTyAlbumTimeSlotRenderer.setReserveData(true);
            if (startInFilmstrip) {
                mActivity.getStateManager().switchState(this, FilmstripPage.class, data);
            } else {
                mActivity.getStateManager().startStateForResult(
                            SinglePhotoPage.class, REQUEST_PHOTO, data);
            }
            ActivityState activityState = mActivity.getStateManager().getTopState();
            if (activityState instanceof PhotoPage){
                mFocusIndex = slotIndex;
                ((PhotoPage)activityState).setOnPhotoChanegeListener(mPhotoChanegeListener);
            }
        }
    }

    private void onGetContent(final MediaItem item) {
        DataManager dm = mActivity.getDataManager();
        Activity activity = (Activity)mActivity.getAndroidContext();
        if (mData.getString(GalleryActivity.EXTRA_CROP) != null) {
            Uri uri = dm.getContentUri(item.getPath());
            Intent intent = new Intent(CropActivity.CROP_ACTION, uri)
                    .addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT)
                    .putExtras(getData());
            if (mData.getParcelable(MediaStore.EXTRA_OUTPUT) == null) {
                intent.putExtra(CropExtras.KEY_RETURN_DATA, true);
            }
            activity.startActivity(intent);
            activity.finish();
        } else {
            Intent intent = new Intent(null, item.getContentUri())
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            activity.setResult(Activity.RESULT_OK, intent);
            activity.finish();
        }
    }

    public void onLongTap(int slotIndex) {
        //taoxj add begin
        return;
        /*if (mGetContent) return;
        MediaItem item = mTyAlbumTimeDataLoader.get(slotIndex);
        if (item == null) return;
        if (mSelectionManager.inSelectionMode()){
            mSelectionManager.toggle(item.getPath());
            mTySlotView.invalidate();
        }else{
            mSelectionManager.setAutoLeaveSelectionMode(false);
            mItemSelected = mItemDelete;
            mSelectionManager.toggle(item.getPath());
        }*/
       //taoxj add end
    }

    @Override
    protected void onCreate(Bundle data, Bundle restoreState) {
        super.onCreate(data, restoreState);        
        mUserDistance = GalleryUtils.meterToPixel(USER_DISTANCE_METER);
        mActionBar = mActivity.getGalleryActionBar();
        mTyGalleryBottomBar = mActivity.getGalleryBottomBar();
		//wangqin add for gallery feature begin       
		mDirectStartSelectedMode = mData.getBoolean(KEY_DIRECT_START_SELECTED_MODE, false);
		mAddPhotoToDirString = mData.getString(KEY_ADD_PHOTO_DESTINATION_PATH);
		//wangqin add for gallery feature begin        
        initializeViews();
        initializeData(data);
        mGetContent = data.getBoolean(GalleryActivity.KEY_GET_CONTENT, false);
        mDirectStartSelectedMode = data.getBoolean(KEY_DIRECT_START_SELECTED_MODE, false);       
        mDetailsSource = new MyDetailsSource();
        Context context = mActivity.getAndroidContext();
        mActivity.getGLRoot().addOnGLChanegeListener(mGLChanegeListener);

        // Enable auto-select-all for mtp album
        if (data.getBoolean(KEY_AUTO_SELECT_ALL)) {
            mSelectionManager.selectAll();
        }

        mLaunchedFromPhotoPage =
                mActivity.getStateManager().hasStateClass(FilmstripPage.class);
        mInCameraApp = data.getBoolean(PhotoPage.KEY_APP_BRIDGE, false);

        mHandler = new SynchronizedHandler(mActivity.getGLRoot()) {
            @Override
            public void handleMessage(Message message) {
                switch (message.what) {
                    case MSG_PICK_PHOTO: {
                        pickPhoto(message.arg1);
                        break;
                    }
                    default:
                        throw new AssertionError(message.what);
                }
            }
        };
    }

    @Override
    protected void onResume() {
        super.onResume();
        //taoxj add begin
        mActionBar.enableBackMode(this,false);
        //taoxj add end
        mIsActive = true;
        mResumeEffect = mActivity.getTransitionStore().get(KEY_RESUME_ANIMATION);
        if (mResumeEffect != null) {
            mTyAlbumTimeSlotRenderer.setSlotFilter(mResumeEffect);
            mResumeEffect.setPositionProvider(mPositionProvider);
            mResumeEffect.start();
        }
        
        mTyGalleryBottomBar.setOnClickListener(this);
        mTyGalleryBottomBar.enableCamMode(false);
        setContentPane(mRootPane);

        // Set the reload bit here to prevent it exit this page in clearLoadingBit().
        setLoadingBit(BIT_LOADING_RELOAD);
        mLoadingFailed = false;
        mTyAlbumTimeDataLoader.resume();
        if (!mInitialSynced) {
            setLoadingBit(BIT_LOADING_SYNC);
            mSyncTask = mMediaSet.requestSync(this);
        }
        mTyActionModeView.resume();
        
        mInCameraAndWantQuitOnPause = mInCameraApp;

        mTyAlbumTimeSlotRenderer.resume();
        mTyAlbumTimeSlotRenderer.setPressedIndex(-1);
        mTyAlbumTimeSlotRenderer.setReserveData(false);

    }

    @Override
    protected void onPause() {
        super.onPause();
        mIsActive = false;
        mTyAlbumTimeDataLoader.pause();
    	mTyActionModeView.pause();
    	if (mSelectionManager.inSelectionMode()) {
            mSelectionManager.leaveSelectionMode();
        }
        DetailsHelper.pause();
        mTyAlbumTimeSlotRenderer.setSlotFilter(null);

        mTyAlbumTimeSlotRenderer.pause();
        
        mTyActionModeView.pause();
        if (mSyncTask != null) {
            mSyncTask.cancel();
            mSyncTask = null;
            clearLoadingBit(BIT_LOADING_SYNC);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //cleanupEmptyLayout();
        if (mTyAlbumTimeDataLoader != null) {
            mTyAlbumTimeDataLoader.setLoadingListener(null);
        }

        mTyActionModeView.destroy();
        mActivity.getGLRoot().removeOnGLChanegeListener(mGLChanegeListener);
    }

    private void initializeViews() {
        mSelectionManager = new SelectionManager(mActivity, false);        
        mMenuExecutor = new MenuExecutor(mActivity, mSelectionManager);
        mSelectionManager.setSelectionListener(this);
        mConfig = Config.TimeGroupPage.get(mActivity.getAndroidContext());
        mTySlotView = new TySlotView(mActivity, mConfig.slotViewSpec);
        mTyAlbumTimeSlotRenderer = new TyAlbumTimeSlotRenderer(mActivity, mTySlotView,
                mSelectionManager, mConfig.titleSpec, mConfig.placeholderColor, mConfig.placeholderTitleColor);
        mTySlotView.setSlotRenderer(mTyAlbumTimeSlotRenderer);
        mRootPane.addComponent(mTySlotView);
        mTySlotView.setListener(new TySlotView.SimpleListener() {
            @Override
            public void onDown(int index) {
                TyAlbumTimePage.this.onDown(index);
            }

            @Override
            public void onUp(boolean followedByLongPress) {
                TyAlbumTimePage.this.onUp(followedByLongPress);
            }

            @Override
            public void onSingleTapUp(int slotIndex) {
                TyAlbumTimePage.this.onSingleTapUp(slotIndex);
            }

            @Override
            public void onLongTap(int slotIndex) {
                TyAlbumTimePage.this.onLongTap(slotIndex);
            }
        });
        
    	mTyActionModeView = new TyActionModeView(mActivity, mSelectionManager, false);
		//wangqin add for gallery feature begin
		mTyActionModeView.setDataSelectMode(mDirectStartSelectedMode);
		mTyActionModeView.setAddAlbumDirPath(mAddPhotoToDirString);
		//wangqin add for gallery feature end
    }

    private void initializeData(Bundle data) {
        String mediaPath = data.getString(KEY_MEDIA_PATH);
         mBaseMediaSet = mActivity.getDataManager().getMediaSet(mediaPath);
        
        String basePath = mBaseMediaSet.getPath().toString();
        String setLoadPath = FilterUtils.switchClusterPath(basePath, FilterUtils.CLUSTER_BY_TY_TIME);

        Path setPath = null;
        setPath = Path.fromString(setLoadPath);
        MediaSet mMediasetLoadPath = mActivity.getDataManager().getMediaSet(setPath);

        String newPath = setLoadPath + "/0";
        mMediaSetPath = Path.fromString(newPath);

        mParentMediaSetString = data.getString(KEY_PARENT_MEDIA_PATH);
        mMediaSet = mActivity.getDataManager().getMediaSet(mMediaSetPath);
        if (mMediaSet == null) {
            Utils.fail("MediaSet is null. Path = %s", mMediaSetPath);
        }
        mSelectionManager.setSourceMediaSet(mMediaSet);
        mTyAlbumTimeDataLoader = new TyAlbumTimeDataLoader(mActivity, mMediasetLoadPath);
        mTyAlbumTimeDataLoader.setLoadingListener(new MyLoadingListener());
        mTyAlbumTimeSlotRenderer.setModel(mTyAlbumTimeDataLoader);
    }

    private void showDetails() {
        mShowDetails = true;
        if (mDetailsHelper == null) {
            mDetailsHelper = new DetailsHelper(mActivity, mRootPane, mDetailsSource);
            mDetailsHelper.setCloseListener(new CloseListener() {
                @Override
                public void onClose() {
                    hideDetails();
                }
            });
        }
        mDetailsHelper.show();
    }

    private void hideDetails() {
        mShowDetails = false;
        mDetailsHelper.hide();
        mTyAlbumTimeSlotRenderer.setHighlightItemPath(null);
        mTySlotView.invalidate();
    }

    @Override
    protected boolean onCreateActionBar(Menu menu) {
        MenuInflater inflator = getSupportMenuInflater();
        if (mGetContent) {
            inflator.inflate(R.menu.pickup, menu);
            int typeBits = mData.getInt(GalleryActivity.KEY_TYPE_BITS,
                    DataManager.INCLUDE_IMAGE);
            mActionBar.setTitle(GalleryUtils.getSelectionModePrompt(typeBits));
        } else {
           // inflator.inflate(R.menu.ty_album, menu);
           // mTyAlbumMenu = menu;
            mTyAlbumMenu = null;
          //  mTyAlbumMenu.setGroupVisible(R.id.ty_album_operation_menu, !mShowedEmpty);
            mItemSelected = menu.findItem(R.id.ty_action_delete);
            mItemDelete = mItemSelected;
            FilterUtils.setupMenuItems(mActionBar, mMediaSetPath, true);
            if (!mSelectionManager.inSelectionMode()) {
                //taoxj modify begin
                //mActionBar.enableTabMode(false);
                 mActionBar.enableBackMode(this, false);
                 mActionBar.showCamera(false);
                //taoxj modify end
            }else{
           //     mTyAlbumMenu.setGroupVisible(R.id.ty_album_operation_menu, false);
            }
        }
        
        if(mDirectStartSelectedMode){
        	 mItemSelected = menu.findItem(R.id.ty_action_add_to_album);
             if (mItemSelected != null){
        	    onItemSelected(mItemSelected);
             }
        }
        return true;
    }

    @Override
    protected boolean onItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_cancel:
                mActivity.getStateManager().finishState(this);
                return true;
            case R.id.action_select:
                mSelectionManager.setAutoLeaveSelectionMode(false);
                mSelectionManager.enterSelectionMode();
                return true;
            case R.id.action_slideshow: {
                mInCameraAndWantQuitOnPause = false;
                Bundle data = new Bundle();
                data.putString(SlideshowPage.KEY_SET_PATH,
                        mMediaSetPath.toString());
                data.putBoolean(SlideshowPage.KEY_REPEAT, true);
                mActivity.getStateManager().startStateForResult(
                        SlideshowPage.class, REQUEST_SLIDESHOW, data);
                return true;
            }
            case R.id.action_details: {
                if (mShowDetails) {
                    hideDetails();
                } else {
                    showDetails();
                }
                return true;
            }
            case R.id.action_camera: {
                GalleryUtils.startCameraActivity(mActivity.getAndroidContext());
                return true;
            }
            case R.id.ty_action_delete:
            case R.id.ty_action_share:
            case R.id.ty_action_add_to_album:
            	mItemSelected = item;
            	mSelectionManager.setAutoLeaveSelectionMode(false);
                mSelectionManager.enterSelectionMode();
                mTySlotView.invalidate();
                return true;
            default:
                return false;
        }
    }

    @Override
    public void onActionBarClick(View v){
        switch (v.getId()) {
            case R.id.ty_action_sel_close:{
                if(mDirectStartSelectedMode&&!MenuExecutor.mAddFromAlbumPage){
                    new AlertDialog.Builder(mActivity.getAndroidContext())
                        .setTitle(mActivity.getResources().getString(R.string.prompt)) 
                        .setMessage(mActivity.getResources().getString(R.string.confirm_to_quit_creating_new_ablum))
                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface arg0, int arg1) {
                                mSelectionManager.leaveSelectionMode();
                            }
                         })
                        .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface arg0, int arg1) {
                                arg0.dismiss();
                            }
                        })
                        .create().show();
                }else{
                    mSelectionManager.leaveSelectionMode();
                }
                break;
            }
             //taoxj add begin
             case R.id.ty_action_title_close:{
                onBackPressed();
                break;
            }
            //taoxj add end
        }
    }
    
    @Override
    public void onBottomBarClick(View v){
        switch (v.getId()) {
            case R.id.ty_cam_act:
                GalleryUtils.startCameraActivity(mActivity.getAndroidContext());
                break;
            default:
                break;
        }
    }

    @Override
    protected void onStateResult(int request, int result, Intent data) {
        switch (request) {
            case REQUEST_SLIDESHOW: {
                // data could be null, if there is no images in the album
                if (data == null) return;
                mFocusIndex = data.getIntExtra(SlideshowPage.KEY_PHOTO_INDEX, 0);
                mTySlotView.setCenterIndex(mFocusIndex);
                break;
            }
            case REQUEST_PHOTO: {
                if (data == null) return;
                int index = data.getIntExtra(PhotoPage.KEY_RETURN_INDEX_HINT, 0);
                if (mFocusIndex != index){
                    mTySlotView.makeSlotVisible(mFocusIndex);
                }
                break;
            }
            case REQUEST_DO_ANIMATION: {
                mTySlotView.startRisingAnimation();
                break;
            }
        }
    }

    @Override
    public void onSelectionModeChange(int mode) {
        switch (mode) {
            case SelectionManager.ENTER_SELECTION_MODE: {
                mActionBar.enableSelectMode(this);
                if(mItemSelected != null){
                    if (mTyAlbumMenu != null){
                        mTyAlbumMenu.setGroupVisible(R.id.ty_album_operation_menu, false);
                    }
                    mTyActionModeView.startActionMode(mItemSelected);
                }
                mRootPane.requestLayout();
                break;
            }
            case SelectionManager.LEAVE_SELECTION_MODE: {
        	    if(mItemSelected != null){
                    if (mTyAlbumMenu != null){
                        mTyAlbumMenu.setGroupVisible(R.id.ty_album_operation_menu, true);
                    }
                    mTyActionModeView.finishActionMode();
        	    }
                if(mDirectStartSelectedMode == true){
                    mDirectStartSelectedMode = false;
                    tyGoOutNewFolder();
                    return;
                }
                mActionBar.enableTabMode(true);
                mTyGalleryBottomBar.setOnClickListener(this);
                mTyGalleryBottomBar.enableCamMode(true);
                mRootPane.requestLayout();
                break;
            }
            case SelectionManager.DESELECT_ALL_MODE:
            case SelectionManager.SELECT_ALL_MODE: {
        	    if(mItemSelected != null){
        		    mTyActionModeView.updateSupportedOperation();
        	    }
                mRootPane.invalidate();
                break;
            }
        }
    }

    @Override
    public void onSelectionChange(Path path, boolean selected) {
        int count = mSelectionManager.getSelectedCount();       
        String format = mActivity.getResources().getQuantityString(
               R.plurals.ty_number_of_items_selected, count, count);
        mTyActionModeView.setTitle(format);
        mTyActionModeView.updateSupportedOperation(path, selected);
    }

    @Override
    public void onSyncDone(final MediaSet mediaSet, final int resultCode) {
        Log.d(TAG, "onSyncDone: " + Utils.maskDebugInfo(mediaSet.getName()) + " result="
                + resultCode);
        ((Activity) mActivity.getAndroidContext()).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                GLRoot root = mActivity.getGLRoot();
                root.lockRenderThread();
                mSyncResult = resultCode;
                try {
                    if (resultCode == MediaSet.SYNC_RESULT_SUCCESS) {
                        mInitialSynced = true;
                    }
                    clearLoadingBit(BIT_LOADING_SYNC);
                    showSyncErrorIfNecessary(mLoadingFailed);
                } finally {
                    root.unlockRenderThread();
                }
            }
        });
    }

    // Show sync error toast when all the following conditions are met:
    // (1) both loading and sync are done,
    // (2) sync result is error,
    // (3) the page is still active, and
    // (4) no photo is shown or loading fails.
    private void showSyncErrorIfNecessary(boolean loadingFailed) {
        if ((mLoadingBits == 0) && (mSyncResult == MediaSet.SYNC_RESULT_ERROR) && mIsActive
                && (loadingFailed || (mTyAlbumTimeDataLoader.size() == 0))) {
            Toast.makeText(mActivity.getAndroidContext(), R.string.sync_album_error,
                    Toast.LENGTH_LONG).show();
        }
    }

    private void setLoadingBit(int loadTaskBit) {
        mLoadingBits |= loadTaskBit;
    }

    private void clearLoadingBit(int loadTaskBit) {
        mLoadingBits &= ~loadTaskBit;
        if (mLoadingBits == 0 && mIsActive) {
            if (mTyAlbumTimeDataLoader.size() == 0) {
                if (mActivity.getStateManager().getStateCount() > 1) {
                    Intent result = new Intent();
                    result.putExtra(TyAlbumTimePage.KEY_EMPTY_ALBUM, true);
                    setStateResult(Activity.RESULT_OK, result);
                    mActivity.getStateManager().finishState(this);
                } else {
                    mShowedEmpty = true;
                    showEmptyLayout();
                    if (mTyAlbumMenu != null){
                        mTyAlbumMenu.setGroupVisible(R.id.ty_album_operation_menu, false);
                    }
                    mTySlotView.invalidate();
                }
                return;
            }
        }

        if (mShowedEmpty) {
            mShowedEmpty = false;
            hideEmptyLayout();
            if (mTyAlbumMenu != null){
                mTyAlbumMenu.setGroupVisible(R.id.ty_album_operation_menu, true);
            }
        }
    }

    private class MyLoadingListener implements LoadingListener {
        @Override
        public void onLoadingStarted() {
            setLoadingBit(BIT_LOADING_RELOAD);
            mLoadingFailed = false;
        }

        @Override
        public void onLoadingFinished(boolean loadingFailed) {
            clearLoadingBit(BIT_LOADING_RELOAD);
            mLoadingFailed = loadingFailed;
            showSyncErrorIfNecessary(loadingFailed);
        }
    }

    private class MyDetailsSource implements DetailsHelper.DetailsSource {
        private int mIndex;

        @Override
        public int size() {
            return mTyAlbumTimeDataLoader.size();
        }

        @Override
        public int setIndex() {
            Path id = mSelectionManager.getSelected(false).get(0);
            mIndex = mTyAlbumTimeDataLoader.findItem(id);
            return mIndex;
        }

        @Override
        public MediaDetails getDetails() {
            // this relies on setIndex() being called beforehand
            MediaObject item = mTyAlbumTimeDataLoader.get(mIndex);
            if (item != null) {
                mTyAlbumTimeSlotRenderer.setHighlightItemPath(item.getPath());
                return item.getDetails();
            } else {
                return null;
            }
        }
    }

    private void showEmptyLayout() {
        if (mTyEmptyLayout == null){
            RelativeLayout galleryRoot = (RelativeLayout) mActivity.getGalleryAssignView(R.id.gallery_root);
            if (galleryRoot == null) return;
            mTyEmptyLayout = galleryRoot.findViewById(R.id.ty_empty_layout);
        }
        mTyEmptyLayout.setVisibility(View.VISIBLE);
    }

    private void hideEmptyLayout() {
        if (mTyEmptyLayout == null) return;
        mTyEmptyLayout.setVisibility(View.GONE);
    }

    private void tyGoOutNewFolder() {
        mHandler.post(new Runnable(){
            public void run() {
                setStateResult(Activity.RESULT_OK, null);
                mActivity.getStateManager().finishState(TyAlbumTimePage.this);
            }
        });
    }
}
