/*
 * Copyright (C) 2010 The Android Open Source Project
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

import android.app.Activity;
import android.app.WallpaperManager;
import android.content.Context;
import android.content.Intent;
import android.content.ActivityNotFoundException;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.view.HapticFeedbackConstants;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
//TY zhencc add for New Design Gallery
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import com.android.gallery3d.ui.TyActionModeView;
//TY zhencc end for New Design Gallery
import android.widget.Toast;

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
import com.android.gallery3d.ui.AlbumSlotRenderer;
import com.android.gallery3d.ui.DetailsHelper;
import com.android.gallery3d.ui.DetailsHelper.CloseListener;
import com.android.gallery3d.ui.GLRoot;
import com.android.gallery3d.ui.GLView;
import com.android.gallery3d.ui.PhotoFallbackEffect;
import com.android.gallery3d.ui.RelativePosition;
import com.android.gallery3d.ui.SelectionManager;
import com.android.gallery3d.ui.SlotView;
import com.android.gallery3d.ui.SynchronizedHandler;
import com.android.gallery3d.util.Future;
import com.android.gallery3d.util.GalleryUtils;
import com.android.gallery3d.util.MediaSetUtils;
import java.util.ArrayList;
import java.lang.IllegalArgumentException;

//TY wb034 20150209 add begin for tygallery
import android.content.res.Configuration;
import com.android.gallery3d.ui.MenuExecutor;
import com.android.gallery3d.ui.SlotView.Spec;
import android.view.WindowManager;
//TY wb034 20150209 add end for tygallery
import com.android.gallery3d.ui.GLRoot.OnGLChanegeListener;
//TY liuyuchuan add begin for  New Design Gallery
import android.graphics.BitmapFactory;
//TY liuyuchuan add end for  New Design Gallery

public class AlbumPage extends ActivityState implements SelectionManager.SelectionListener
    , MediaSet.SyncListener, GalleryActionBar.OnActionBarListener, TyGalleryBottomBar.OnBottomBarListener{
    @SuppressWarnings("unused")
    private static final String TAG = "AlbumPage";

    public static final String KEY_MEDIA_PATH = "media-path";
    public static final String KEY_PARENT_MEDIA_PATH = "parent-media-path";
    public static final String KEY_SET_CENTER = "set-center";
    public static final String KEY_AUTO_SELECT_ALL = "auto-select-all";
    public static final String KEY_SHOW_CLUSTER_MENU = "cluster-menu";
    public static final String KEY_EMPTY_ALBUM = "empty-album";
    public static final String KEY_RESUME_ANIMATION = "resume_animation";

    private static final int REQUEST_SLIDESHOW = 1;
    public static final int REQUEST_PHOTO = 2;
    private static final int REQUEST_DO_ANIMATION = 3;
    public static final int REQUEST_DO_ADD_PHOTO = 4;//TY wb034 20150126 add for tygallery

    private static final int BIT_LOADING_RELOAD = 1;
    private static final int BIT_LOADING_SYNC = 2;

    private static final float USER_DISTANCE_METER = 0.3f;

    private boolean mIsActive = false;
    private AlbumSlotRenderer mAlbumView;
    private Path mMediaSetPath;
    private String mParentMediaSetString;
    private SlotView mSlotView;

    private AlbumDataLoader mAlbumDataAdapter;

    protected SelectionManager mSelectionManager;

    private boolean mGetContent;
    private boolean mTyNoCloseScene;
    private boolean mShowClusterMenu;
    //TY zhencc add for New Design Gallery
    private int mBottomGap;
    private TyActionModeView mTyActionModeView;
    private MenuItem mItemSelected;
    //TY zhencc end for New Design Gallery

    private ActionModeHandler mActionModeHandler;
    private int mFocusIndex = 0;
    private DetailsHelper mDetailsHelper;
    private MyDetailsSource mDetailsSource;
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

    private Menu mActionMenu;
    private static final int REQUEST_WALLPAPER_STATUS = 10;

    //yuxin add begin for  New Design Gallery
    private long mOptNum;
    private Config.AlbumPage mConfig;
    private GalleryActionBar mActionBar;
    private TyGalleryBottomBar mTyGalleryBottomBar;
    private OnGLChanegeListener mGLChanegeListener = new OnGLChanegeListener(){
        @Override
        public void onGLChanege() {
            mAlbumView.setReserveData(false);
        }
    };

    private PhotoPage.OnPhotoChanegeListener mPhotoChanegeListener = new PhotoPage.OnPhotoChanegeListener(){
        @Override
        public void onPhotoChanege(int index) {
            if (mFocusIndex != index){
                mFocusIndex = index;
                mSlotView.makeSlotVisible(mFocusIndex);
            }
        }
    };
    //yuxin add end for  New Design Gallery

    private PhotoFallbackEffect mResumeEffect;
    private PhotoFallbackEffect.PositionProvider mPositionProvider =
            new PhotoFallbackEffect.PositionProvider() {
        @Override
        public Rect getPosition(int index) {
            Rect rect = mSlotView.getSlotRect(index);
            Rect bounds = mSlotView.bounds();
            rect.offset(bounds.left - mSlotView.getScrollX(),
                    bounds.top - mSlotView.getScrollY());
            return rect;
        }

        @Override
        public int getItemIndex(Path path) {
            int start = mSlotView.getVisibleStart();
            int end = mSlotView.getVisibleEnd();
            for (int i = start; i < end; ++i) {
                MediaItem item = mAlbumDataAdapter.get(i);
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

            //yuxin add begin for  New Design Gallery
            mConfig.setMarin(mActionBar.getHeight(), 
                mTyGalleryBottomBar.getAreaHeight(TyGalleryBottomBar.TyHeightKind.TySelect));
            //yuxin add end for  New Design Gallery
            
            if (mShowDetails) {
                mDetailsHelper.layout(left, slotViewTop, right, bottom);
            } else {
                mAlbumView.setHighlightItemPath(null);
            }

            // Set the mSlotView as a reference point to the open animation
            mOpenCenter.setReferencePosition(0, slotViewTop);
            mSlotView.layout(0, slotViewTop, slotViewRight, slotViewBottom);
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
                    mAlbumView.setSlotFilter(null);
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
            mSelectionManager.leaveSelectionMode();
        } else {
            if(mLaunchedFromPhotoPage) {
                mActivity.getTransitionStore().putIfNotPresent(
                        PhotoPage.KEY_ALBUMPAGE_TRANSITION,
                        PhotoPage.MSG_ALBUMPAGE_RESUMED);
            }
            // TODO: fix this regression
            // mAlbumView.savePositions(PositionRepository.getInstance(mActivity));
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
        } else if (mActivity.getStateManager().getStateCount() > 1) {
            super.onBackPressed();
        } else if (mParentMediaSetString != null) {
            Bundle data = new Bundle(getData());
            data.putString(AlbumSetPage.KEY_MEDIA_PATH, mParentMediaSetString);
            mActivity.getStateManager().switchState(
                    this, AlbumSetPage.class, data);
        }
    }

    private void onDown(int index) {
        mAlbumView.setPressedIndex(index);
    }

    private void onUp(boolean followedByLongPress) {
        if (followedByLongPress) {
            // Avoid showing press-up animations for long-press.
            mAlbumView.setPressedIndex(-1);
        } else {
            mAlbumView.setPressedUp();
        }
    }

    private void onSingleTapUp(int slotIndex) {
        if (!mIsActive) return;

        if (mSelectionManager.inSelectionMode()) {
            MediaItem item = mAlbumDataAdapter.get(slotIndex);
            if (item == null) return; // Item not ready yet, ignore the click
            mSelectionManager.toggle(item.getPath());
            mSlotView.invalidate();
        } else {
            // Render transition in pressed state
            mAlbumView.setPressedIndex(slotIndex);
            mAlbumView.setPressedUp();
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

        MediaItem item = mAlbumDataAdapter.get(slotIndex);
        if (item == null) return; // Item not ready yet, ignore the click
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
            // mAlbumView.savePositions(PositionRepository.getInstance(mActivity));
            Bundle data = new Bundle();
            data.putInt(PhotoPage.KEY_INDEX_HINT, slotIndex);
            data.putParcelable(PhotoPage.KEY_OPEN_ANIMATION_RECT,
                    mSlotView.getSlotRect(slotIndex, mRootPane));
            data.putString(PhotoPage.KEY_MEDIA_SET_PATH,
                    mMediaSetPath.toString());
            data.putString(PhotoPage.KEY_MEDIA_ITEM_PATH,
                    item.getPath().toString());
            data.putInt(PhotoPage.KEY_ALBUMPAGE_TRANSITION,
                    PhotoPage.MSG_ALBUMPAGE_STARTED);
            data.putBoolean(PhotoPage.KEY_START_IN_FILMSTRIP,
                    startInFilmstrip);
            //TY wb034 20150130 add begin for tygallery
            data.putString(PhotoPage.KEY_ID, mMediaSetPath.getSuffix());
            //TY wb034 20150130 add  end for tygallery
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
            mAlbumView.setReserveData(true);
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
        if (mTyNoCloseScene){
            WallpaperManager wpm = WallpaperManager.getInstance(activity);
            try {
                Intent wallpaperIntent = wpm.getCropAndSetWallpaperIntent(item.getContentUri());
                activity.startActivityForResult(wallpaperIntent, REQUEST_WALLPAPER_STATUS);
                //TY wb034 20150601 add begin for PROD103787230
                activity.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN,WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
                //TY wb034 20150601 add end for PROD103787230
            } catch (ActivityNotFoundException anfe) {
                // ignored; fallthru to existing crop activity
            } catch (IllegalArgumentException iae) {
                // ignored; fallthru to existing crop activity
            }
        } else if (mData.getString(GalleryActivity.EXTRA_CROP) != null) {
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
        if (mGetContent) return;
        MediaItem item = mAlbumDataAdapter.get(slotIndex);
        if (item != null && mActionMenu != null){
            mItemSelected = mActionMenu.findItem(R.id.ty_action_delete);
            if (mItemSelected != null){
                mSelectionManager.setAutoLeaveSelectionMode(false);
                mSelectionManager.toggle(item.getPath());
                mSlotView.invalidate();
            }
        }
    }

    @Override
    protected void onCreate(Bundle data, Bundle restoreState) {
        super.onCreate(data, restoreState);
        mActionBar = mActivity.getGalleryActionBar();
        mTyGalleryBottomBar = mActivity.getGalleryBottomBar();
        mUserDistance = GalleryUtils.meterToPixel(USER_DISTANCE_METER);
        mActivity.getGLRoot().addOnGLChanegeListener(mGLChanegeListener);
        initializeViews();
        initializeData(data);
        mGetContent = data.getBoolean(GalleryActivity.KEY_GET_CONTENT, false);
        mTyNoCloseScene = data.getBoolean(GalleryUtils.KEY_EX_TY_NO_CLOSESENCE, false);
        
        mShowClusterMenu = data.getBoolean(KEY_SHOW_CLUSTER_MENU, false);
        mDetailsSource = new MyDetailsSource();
        Context context = mActivity.getAndroidContext();

        if (data.getBoolean(KEY_AUTO_SELECT_ALL)) {
            mSelectionManager.selectAll();
        }

        //TY zhencc add for New Design Gallery
        mBottomGap = mActivity.getResources().getDimensionPixelSize(R.dimen.ty_bottom_height);
        //TY zhencc end for New Design Gallery
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
        mIsActive = true;

        mResumeEffect = mActivity.getTransitionStore().get(KEY_RESUME_ANIMATION);
        if (mResumeEffect != null) {
            mAlbumView.setSlotFilter(mResumeEffect);
            mResumeEffect.setPositionProvider(mPositionProvider);
            mResumeEffect.start();
        }

        setContentPane(mRootPane);
        mTyGalleryBottomBar.setOnClickListener(this);
        boolean enableHomeButton = (mActivity.getStateManager().getStateCount() > 1) |
                mParentMediaSetString != null;
        if (!mGetContent) {
            mActionBar.enableBackMode(this, false);
            mActionBar.showCamera(MediaSetUtils.isCameraSource(mMediaSetPath)
                    && GalleryUtils.isCameraAvailable(mActivity.getAndroidContext()));
            mActionBar.setTitle(mMediaSet.getName());
        }
        mTyGalleryBottomBar.enableNoneMode(true);

        // Set the reload bit here to prevent it exit this page in clearLoadingBit().
        setLoadingBit(BIT_LOADING_RELOAD);
        mLoadingFailed = false;
        mAlbumDataAdapter.resume();

        mAlbumView.resume();
        mAlbumView.setPressedIndex(-1);
        mTyActionModeView.resume();
        if (!mInitialSynced) {
            setLoadingBit(BIT_LOADING_SYNC);
            mSyncTask = mMediaSet.requestSync(this);
        }
        mInCameraAndWantQuitOnPause = mInCameraApp;
        mAlbumView.setReserveData(false);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mIsActive = false;

        if (mSelectionManager.inSelectionMode()) {
            mSelectionManager.leaveSelectionMode();
        }
        mAlbumView.setSlotFilter(null);
        //TY zhencc modify for New Design Gallery
        mTyActionModeView.pause();
        //TY zhencc end for New Design Gallery

        mAlbumDataAdapter.pause();
        mAlbumView.pause();
        
        DetailsHelper.pause();
        if (!mGetContent && mAlbumDataAdapter.size() > 0) {
            mActionBar.enableBackMode(this, false);
            mActionBar.showCamera(MediaSetUtils.isCameraSource(mMediaSetPath)
                    && GalleryUtils.isCameraAvailable(mActivity.getAndroidContext()));
            mActionBar.setTitle(mMediaSet.getName());
        }

        if (mSyncTask != null) {
            mSyncTask.cancel();
            mSyncTask = null;
            clearLoadingBit(BIT_LOADING_SYNC);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mAlbumDataAdapter != null) {
            mAlbumDataAdapter.setLoadingListener(null);
        }
        mTyActionModeView.destroy();
        mActivity.getGLRoot().removeOnGLChanegeListener(mGLChanegeListener);
    }

    private void initializeViews() {
        mSelectionManager = new SelectionManager(mActivity, false);
        mSelectionManager.setSelectionListener(this);
        mConfig = Config.AlbumPage.get(mActivity.getAndroidContext());
        mSlotView = new SlotView(mActivity, mConfig.slotViewSpec);
        mAlbumView = new AlbumSlotRenderer(mActivity, mSlotView,
                mSelectionManager, mConfig.placeholderColor);
        mSlotView.setSlotRenderer(mAlbumView);
        mRootPane.addComponent(mSlotView);
        mSlotView.setListener(new SlotView.SimpleListener() {
            @Override
            public void onDown(int index) {
                AlbumPage.this.onDown(index);
            }

            @Override
            public void onUp(boolean followedByLongPress) {
                AlbumPage.this.onUp(followedByLongPress);
            }

            @Override
            public void onSingleTapUp(int slotIndex) {
                AlbumPage.this.onSingleTapUp(slotIndex);
            }

            @Override
            public void onLongTap(int slotIndex) {
                AlbumPage.this.onLongTap(slotIndex);
            }
        });
        
        mTyActionModeView = new TyActionModeView(mActivity, mSelectionManager, false);
    }

    private void initializeData(Bundle data) {
        mMediaSetPath = Path.fromString(data.getString(KEY_MEDIA_PATH));
        mParentMediaSetString = data.getString(KEY_PARENT_MEDIA_PATH);
        mMediaSet = mActivity.getDataManager().getMediaSet(mMediaSetPath);
        if (mMediaSet == null) {
            Utils.fail("MediaSet is null. Path = %s", mMediaSetPath);
        }
        mSelectionManager.setSourceMediaSet(mMediaSet);
        mAlbumDataAdapter = new AlbumDataLoader(mActivity, mMediaSet);
        mAlbumDataAdapter.setLoadingListener(new MyLoadingListener());
        mAlbumView.setModel(mAlbumDataAdapter);
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
        mAlbumView.setHighlightItemPath(null);
        mSlotView.invalidate();
    }

    @Override
    protected boolean onCreateActionBar(Menu menu) {
        MenuInflater inflator = getSupportMenuInflater();
        if (mGetContent) {
            int typeBits = mData.getInt(GalleryActivity.KEY_TYPE_BITS,
                    DataManager.INCLUDE_IMAGE);
            mActionBar.enableBackMode(this, false);
            mActionBar.showCamera(false);
            mActionBar.setTitle(GalleryUtils.getSelectionModePrompt(typeBits));
        } else {
            mActionMenu = menu;
            inflator.inflate(R.menu.ty_album, menu);
            MenuItem addToAlbumMenu = mActionMenu.findItem(R.id.ty_action_add_to_album);
            if(addToAlbumMenu != null){
                addToAlbumMenu.setTitle(R.string.ty_add_new_photo);
            }
            refreshMenu();
            
            FilterUtils.setupMenuItems(mActionBar, mMediaSetPath, true);
            mActionBar.enableBackMode(this, false);
            mActionBar.showCamera(MediaSetUtils.isCameraSource(mMediaSetPath)
                    && GalleryUtils.isCameraAvailable(mActivity.getAndroidContext()));
            mActionBar.setTitle(mMediaSet.getName());
        }
        return true;
    }

    private boolean allVideoFiles() {
        if (mMediaSet == null)
            return false;
        int count = mMediaSet.getMediaItemCount();
        ArrayList<MediaItem> mediaItems;
        MediaItem item;
        for (int i = 0; i < count; i++) {
            mediaItems = mMediaSet.getMediaItem(i, 1);
            if (mediaItems.size() <= 0) {
                continue;
            }
            item = mediaItems.get(0);
            if (item == null) {
                continue;
            }
            if (item.getMimeType().trim().startsWith("image/"))
                return false;
        }
        return true;
    }

    private void prepareAnimationBackToFilmstrip(int slotIndex) {
        if (mAlbumDataAdapter == null || !mAlbumDataAdapter.isActive(slotIndex)) return;
        MediaItem item = mAlbumDataAdapter.get(slotIndex);
        if (item == null) return;
        TransitionStore transitions = mActivity.getTransitionStore();
        transitions.put(PhotoPage.KEY_INDEX_HINT, slotIndex);
        transitions.put(PhotoPage.KEY_OPEN_ANIMATION_RECT,
                mSlotView.getSlotRect(slotIndex, mRootPane));
    }

    @Override
    protected boolean onItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home: {
                onUpPressed();
                return true;
            }
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
            case R.id.ty_action_delete:
                if(mMediaSet.getBucketId() == mActivity.getDataManager().mCollectBucketId){
                    MenuExecutor.mIsFromCollectAlbum = true;
                } 
            case R.id.ty_action_share:
            	mItemSelected = item;
            	mSelectionManager.setAutoLeaveSelectionMode(false);
                mSelectionManager.enterSelectionMode();
                mSlotView.invalidate();
                return true;
             case R.id.ty_action_add_to_album:
                 Bundle data = new Bundle();
                 data.putString(AlbumSetPage.KEY_MEDIA_PATH,
                     mActivity.getDataManager().getTopSetPath(DataManager.INCLUDE_ALL));
                 data.putString(TyAlbumTimePage.KEY_ADD_PHOTO_DESTINATION_PATH, mMediaSetPath.toString());
                 data.putBoolean(TyAlbumTimePage.KEY_DIRECT_START_SELECTED_MODE, true);
                 MenuExecutor.mAddFromAlbumPage=true;
                 if(mMediaSet.getBucketId()== mActivity.getDataManager().mCollectBucketId){
                    MenuExecutor.mIsFromCollectAlbum = true;
                 }
                 mActivity.getStateManager().startStateForResult(
                     TyAlbumTimePage.class, REQUEST_DO_ADD_PHOTO, data);
                return true;
                //TY wb034 20150127 add end for tygallery
            default:
                return false;
        }
    }

    @Override
    public void onActionBarClick(View v){
        switch (v.getId()) {
            case R.id.ty_action_title_close:{
                onUpPressed();
                break;
            }
            case R.id.ty_action_camera: {
                GalleryUtils.startCameraActivity(mActivity.getAndroidContext());
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
    }

    @Override
    protected void onStateResult(int request, int result, Intent data) {
        switch (request) {
            case REQUEST_SLIDESHOW: {
                // data could be null, if there is no images in the album
                if (data == null) return;
                mFocusIndex = data.getIntExtra(SlideshowPage.KEY_PHOTO_INDEX, 0);
                mSlotView.setCenterIndex(mFocusIndex);
                break;
            }
            case REQUEST_PHOTO: {
                if (data == null) return;
                int index = data.getIntExtra(PhotoPage.KEY_RETURN_INDEX_HINT, 0);
                if (mFocusIndex != index){
                    mSlotView.makeSlotVisible(mFocusIndex);
                }
                break;
            }
            case REQUEST_DO_ANIMATION: {
                mSlotView.startRisingAnimation();
                break;
            }
            //TY wb034 20150127 add begin for tygallery
            case REQUEST_DO_ADD_PHOTO:{
                MenuExecutor.mAddFromAlbumPage=false;
                break;
            }
            //TY wb034 20150127 add begin for tygallery       
            case REQUEST_WALLPAPER_STATUS:{
                //TY wb034 20150601 add begin for PROD103787230
                ((Activity)mActivity.getAndroidContext()).getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
              //TY wb034 20150601 add end for PROD103787230
                if (result == Activity.RESULT_OK){
                    Activity activity = (Activity)mActivity.getAndroidContext();
                    activity.finish();
                }
                break;
            }
        }
    }

    private void updateMenuItem() {
        //TY zhencc delete begin for New Design Gallery
        //if (!mGetContent && allVideoFiles()) {
        //    mActionMenu.findItem(R.id.action_slideshow).setVisible(false);
        //}   
        //TY zhencc delete end for New Design Gallery
    }

    @Override
    public void onSelectionModeChange(int mode) {
        switch (mode) {
            case SelectionManager.ENTER_SELECTION_MODE: {
                mActionBar.enableSelectMode(this);
                mOptNum = MenuExecutor.mOptNum;
                refreshMenu();
        	    if(mItemSelected != null){
        	        mTyActionModeView.startActionMode(mItemSelected);
        	    }
                mRootPane.requestLayout();
                performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                break;
            }
            case SelectionManager.LEAVE_SELECTION_MODE: {
                //TY wb034 20150306 add begin for tygallery
                MenuExecutor.mIsFromCollectAlbum = false;
                //TY wb034 20150306 add end for tygallery
                if (mOptNum == MenuExecutor.mOptNum){
                    mActionBar.enableBackMode(this, true);
                    mActionBar.showCamera(MediaSetUtils.isCameraSource(mMediaSetPath)
                            && GalleryUtils.isCameraAvailable(mActivity.getAndroidContext()));
                    mActionBar.setTitle(mMediaSet.getName());
                    mTyGalleryBottomBar.setOnClickListener(this);
                    mTyGalleryBottomBar.enableNoneMode(true);
                }
                    
                if(mItemSelected != null){
                    mTyActionModeView.finishActionMode();
        	    }
                refreshMenu();
                mRootPane.requestLayout();
                mRootPane.invalidate();
                updateMenuItem();
                break;
            }
            case SelectionManager.SELECT_ALL_MODE: 
            case SelectionManager.DESELECT_ALL_MODE: {
                if(mItemSelected != null){
                    mTyActionModeView.updateSupportedOperation();
                }
                mRootPane.requestLayout();
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
                && (loadingFailed || (mAlbumDataAdapter.size() == 0))) {
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
            if (mAlbumDataAdapter.size() == 0) {
                Intent result = new Intent();
                result.putExtra(KEY_EMPTY_ALBUM, true);
                setStateResult(Activity.RESULT_OK, result);
                mActivity.getStateManager().finishState(this);
            }else{
                if (!mGetContent){
                    mActionBar.enableBackMode(this, true);
                    mActionBar.showCamera(MediaSetUtils.isCameraSource(mMediaSetPath)
                            && GalleryUtils.isCameraAvailable(mActivity.getAndroidContext()));
                    mActionBar.setTitle(mMediaSet.getName());
                    mTyGalleryBottomBar.setOnClickListener(this);
                    mTyGalleryBottomBar.enableNoneMode(true);
                }
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
            return mAlbumDataAdapter.size();
        }

        @Override
        public int setIndex() {
            Path id = mSelectionManager.getSelected(false).get(0);
            mIndex = mAlbumDataAdapter.findItem(id);
            return mIndex;
        }

        @Override
        public MediaDetails getDetails() {
            // this relies on setIndex() being called beforehand
            MediaObject item = mAlbumDataAdapter.get(mIndex);
            if (item != null) {
                mAlbumView.setHighlightItemPath(item.getPath());
                return item.getDetails();
            } else {
                return null;
            }
        }
    }

    private void refreshMenu(){
        if (mActionMenu == null) return;
        
        if (mSelectionManager.inSelectionMode()){
            mActionMenu.setGroupVisible(R.id.ty_album_operation_menu, false);
        } else{
            mActionMenu.setGroupVisible(R.id.ty_album_operation_menu, true);
            /*
            MenuItem deleteMenu = mActionMenu.findItem(R.id.ty_action_delete);
            if(deleteMenu != null){
                deleteMenu.setVisible(true);
            }
            
            MenuItem shareMenu = mActionMenu.findItem(R.id.ty_action_share);
            if(shareMenu != null){
                shareMenu.setVisible(true);
            }
            
            MenuItem addToAlbumMenu = mActionMenu.findItem(R.id.ty_action_add_to_album);
            if(addToAlbumMenu != null){
                addToAlbumMenu.setTitle(R.string.ty_add_new_photo);
                addToAlbumMenu.setVisible(true);
            }
            */
        }
    }
}
