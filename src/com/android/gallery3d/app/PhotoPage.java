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

import android.annotation.TargetApi;
import android.app.ActionBar.OnMenuVisibilityListener;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.net.Uri;
import android.nfc.NfcAdapter;
import android.nfc.NfcAdapter.CreateBeamUrisCallback;
import android.nfc.NfcEvent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
//TY zhencc add for New Design Gallery
import java.util.ArrayList;
import java.util.List;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.content.pm.ResolveInfo;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import com.android.gallery3d.database.PictureDAO;
import com.android.gallery3d.database.PictureDAOImpl;
import com.android.gallery3d.ui.MenuExecutor.ProgressListener;
import com.android.gallery3d.ui.TyAddToAlbumCompleteListener;
import com.android.gallery3d.ui.TyCollectToAlbumCompleteListener;
import com.android.gallery3d.ui.GLRoot;
import com.android.gallery3d.util.MediaSetUtils;
//TY zhencc end for New Design Gallery
import android.widget.RelativeLayout;
import android.widget.ShareActionProvider;
import android.widget.Toast;
import android.media.MediaFile;

import com.android.gallery3d.R;
import com.android.gallery3d.common.ApiHelper;
import com.android.gallery3d.data.ComboAlbum;
import com.android.gallery3d.data.DataManager;
import com.android.gallery3d.data.FilterDeleteSet;
import com.android.gallery3d.data.FilterSource;
import com.android.gallery3d.data.LocalImage;
import com.android.gallery3d.data.MediaDetails;
import com.android.gallery3d.data.MediaItem;
import com.android.gallery3d.data.MediaObject;
import com.android.gallery3d.data.MediaObject.PanoramaSupportCallback;
import com.android.gallery3d.data.MediaSet;
import com.android.gallery3d.data.Path;
import com.android.gallery3d.data.SecureAlbum;
import com.android.gallery3d.data.SecureSource;
import com.android.gallery3d.data.SnailAlbum;
import com.android.gallery3d.data.SnailItem;
import com.android.gallery3d.data.SnailSource;
//TY zhencc 20150715 add for PROD103891409 begin
import com.android.gallery3d.data.UriImage;
//TY zhencc 20150715 add for PROD103891409 end
import com.android.gallery3d.filtershow.FilterShowActivity;
import com.android.gallery3d.filtershow.crop.CropActivity;
import com.android.gallery3d.picasasource.PicasaSource;
import com.android.gallery3d.ui.DetailsHelper;
import com.android.gallery3d.ui.DetailsHelper.CloseListener;
import com.android.gallery3d.ui.DetailsHelper.DetailsSource;
import com.android.gallery3d.ui.GLRootView;
import com.android.gallery3d.ui.GLView;
import com.android.gallery3d.ui.MenuExecutor;
import com.android.gallery3d.ui.PhotoView;
import com.android.gallery3d.ui.SelectionManager;
import com.android.gallery3d.ui.SynchronizedHandler;
import com.android.gallery3d.util.GalleryUtils;
import com.android.gallery3d.util.UsageStatistics;
import com.android.gallery3d.util.ViewGifImage;
import java.io.File;
//yuxin add begin for  New Design Gallery
import com.android.gallery3d.ui.PhotoFallbackEffect;
import com.android.gallery3d.glrenderer.GLCanvas;
import com.android.gallery3d.ui.GLRoot.OnGLIdleListener;
import android.os.ConditionVariable;
//yuxin add end for  New Design Gallery
import android.view.WindowManager;//TY liuyuchuan add for New Design Gallery
//TY wb034 20150204 add begin for tygallery
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
//TY wb034 20150204 add end for tygallery
//TIANYURD:Lizhy 20150429 add 
import android.app.Fragment;;
//TY zhencc add for makeup begin
import com.android.gallery3d.common.BitmapUtils;
//import android.os.SystemProperties;
//TY zhencc add for makeup end

public abstract class PhotoPage extends ActivityState implements
        PhotoView.Listener, AppBridge.Server, ShareActionProvider.OnShareTargetSelectedListener,
        PhotoPageBottomControls.Delegate, GalleryActionBar.OnActionBarListener, OnClickListener{
    private static final String TAG = "PhotoPage";

    private static final int MSG_HIDE_BARS = 1;
    private static final int MSG_ON_FULL_SCREEN_CHANGED = 4;
    private static final int MSG_UPDATE_ACTION_BAR = 5;
    private static final int MSG_UNFREEZE_GLROOT = 6;
    private static final int MSG_WANT_BARS = 7;
    private static final int MSG_REFRESH_BOTTOM_CONTROLS = 8;
    private static final int MSG_ON_CAMERA_CENTER = 9;
    private static final int MSG_ON_PICTURE_CENTER = 10;
    private static final int MSG_REFRESH_IMAGE = 11;
    private static final int MSG_UPDATE_PHOTO_UI = 12;
    private static final int MSG_UPDATE_DEFERRED = 14;
    private static final int MSG_UPDATE_SHARE_URI = 15;
    private static final int MSG_UPDATE_PANORAMA_UI = 16;

    private static final int HIDE_BARS_TIMEOUT = 3500;
    private static final int UNFREEZE_GLROOT_TIMEOUT = 250;

    private static final int REQUEST_SLIDESHOW = 1;
    private static final int REQUEST_CROP = 2;
    private static final int REQUEST_CROP_PICASA = 3;
    private static final int REQUEST_EDIT = 4;
    private static final int REQUEST_PLAY_VIDEO = 5;
    private static final int REQUEST_TRIM = 6;

    public static final String KEY_MEDIA_SET_PATH = "media-set-path";
    public static final String KEY_MEDIA_ITEM_PATH = "media-item-path";
    public static final String KEY_INDEX_HINT = "index-hint";
    public static final String KEY_OPEN_ANIMATION_RECT = "open-animation-rect";
    public static final String KEY_APP_BRIDGE = "app-bridge";
    public static final String KEY_TREAT_BACK_AS_UP = "treat-back-as-up";
    public static final String KEY_START_IN_FILMSTRIP = "start-in-filmstrip";
    public static final String KEY_RETURN_INDEX_HINT = "return-index-hint";
    public static final String KEY_SHOW_WHEN_LOCKED = "show_when_locked";
    public static final String KEY_IN_CAMERA_ROLL = "in_camera_roll";
    public static final String KEY_READONLY = "read-only";
    //TY wb034 20150130 add begin for tygallery
    public static final String KEY_ID = "media-set-id";
    private boolean mInCollectAlbum = false;
    private TyCollectToAlbumCompleteListener  mTyCollectToAlbumCompleteListener;
    private MyListener myListener =new MyListener();
    private MediaItem mPreCurrentPhoto = null;
    private ImageButton mTyPhotoCollectMenu;
    private boolean mIsCollectFolder = false;
    //TY wb034 20150130 add end for tygallery
    public static final String KEY_ALBUMPAGE_TRANSITION = "albumpage-transition";
    public static final int MSG_ALBUMPAGE_NONE = 0;
    public static final int MSG_ALBUMPAGE_STARTED = 1;
    public static final int MSG_ALBUMPAGE_RESUMED = 2;
    public static final int MSG_ALBUMPAGE_PICKED = 4;
    //TY liuyuchuan add begin for  New Design Gallery
    public static String mPictureWidth = "pictureWidth";
    public static String mPictureHeight = "pictureHeight";
    private int pictureWidth = 50;
    private int pictureHeight = 50;
    public static String mPictureMimeType = "pictureMimeType";
    //TY liuyuchuan add end for  New Design Gallery

    public static final String ACTION_NEXTGEN_EDIT = "action_nextgen_edit";
    public static final String ACTION_SIMPLE_EDIT = "action_simple_edit";

    private GalleryApp mApplication;
    private SelectionManager mSelectionManager;

    private PhotoView mPhotoView;
    private PhotoPage.Model mModel;
    private DetailsHelper mDetailsHelper;
    private boolean mShowDetails;

    // mMediaSet could be null if there is no KEY_MEDIA_SET_PATH supplied.
    // E.g., viewing a photo in gmail attachment
    private FilterDeleteSet mMediaSet;

    // The mediaset used by camera launched from secure lock screen.
    private SecureAlbum mSecureAlbum;

    private int mCurrentIndex = 0;
    private Handler mHandler;
    //TY zhencc modify for New Design Gallery
    private boolean mShowBars;
    private Menu mActionMenu;
    private View mSelectCountView;
    private Button mTitleBtn;
    private RelativeLayout mTyPhotoBottomMenuLayout;
    private Intent mTyShareIntent;
    private AlertDialog mShareListAlertDialog;
    private List<AppInfo> mListAppInfo;
    //TY zhencc end for New Design Gallery
    private volatile boolean mActionBarAllowed = true;
    private GalleryActionBar mActionBar;
    private TyGalleryBottomBar mTyGalleryBottomBar;
    private boolean mIsMenuVisible;
    private boolean mHaveImageEditor;
    private PhotoPageBottomControls mBottomControls;
    private MediaItem mCurrentPhoto = null;
    private MenuExecutor mMenuExecutor;
    private boolean mIsActive;
    private String mSetPathString;
    // This is the original mSetPathString before adding the camera preview item.
    private boolean mReadOnlyView = false;
    private String mOriginalSetPathString;
    private AppBridge mAppBridge;
    private SnailItem mScreenNailItem;
    private SnailAlbum mScreenNailSet;
    private OrientationManager mOrientationManager;
    private boolean mTreatBackAsUp;
    private boolean mStartInFilmstrip;
    private boolean mHasCameraScreennailOrPlaceholder = false;
    private boolean mRecenterCameraOnResume = true;

    // These are only valid after the panorama callback
    private boolean mIsPanorama;
    private boolean mIsPanorama360;

    private long mCameraSwitchCutoff = 0;
    private boolean mSkipUpdateCurrentPhoto = false;
    private static final long CAMERA_SWITCH_CUTOFF_THRESHOLD_MS = 300;

    private static final long DEFERRED_UPDATE_MS = 250;
    private boolean mDeferredUpdateWaiting = false;
    private long mDeferUpdateUntil = Long.MAX_VALUE;

    // The item that is deleted (but it can still be undeleted before commiting)
    private Path mDeletePath;
    private boolean mDeleteIsFocus;  // whether the deleted item was in focus

    private Uri[] mNfcPushUris = new Uri[1];

    private final MyMenuVisibilityListener mMenuVisibilityListener =
            new MyMenuVisibilityListener();

    private int mLastSystemUiVis = 0;

    private final PanoramaSupportCallback mUpdatePanoramaMenuItemsCallback = new PanoramaSupportCallback() {
        @Override
        public void panoramaInfoAvailable(MediaObject mediaObject, boolean isPanorama,
                boolean isPanorama360) {
            if (mediaObject == mCurrentPhoto) {
                mHandler.obtainMessage(MSG_UPDATE_PANORAMA_UI, isPanorama360 ? 1 : 0, 0,
                        mediaObject).sendToTarget();
            }
        }
    };

    private final PanoramaSupportCallback mRefreshBottomControlsCallback = new PanoramaSupportCallback() {
        @Override
        public void panoramaInfoAvailable(MediaObject mediaObject, boolean isPanorama,
                boolean isPanorama360) {
            if (mediaObject == mCurrentPhoto) {
                mHandler.obtainMessage(MSG_REFRESH_BOTTOM_CONTROLS, isPanorama ? 1 : 0, isPanorama360 ? 1 : 0,
                        mediaObject).sendToTarget();
            }
        }
    };

    private final PanoramaSupportCallback mUpdateShareURICallback = new PanoramaSupportCallback() {
        @Override
        public void panoramaInfoAvailable(MediaObject mediaObject, boolean isPanorama,
                boolean isPanorama360) {
            if (mediaObject == mCurrentPhoto) {
                mHandler.obtainMessage(MSG_UPDATE_SHARE_URI, isPanorama360 ? 1 : 0, 0, mediaObject)
                        .sendToTarget();
            }
        }
    };

    public static interface Model extends PhotoView.Model {
        public void resume();
        public void pause();
        public boolean isEmpty();
        public void setCurrentPhoto(Path path, int indexHint);
    }

    private class MyMenuVisibilityListener implements OnMenuVisibilityListener {
        @Override
        public void onMenuVisibilityChanged(boolean isVisible) {
            mIsMenuVisible = isVisible;
            refreshHidingMessage();
        }
    }

    @Override
    protected int getBackgroundColorId() {
        //taoxj modify
        /*int backgroundColorId = R.color.photo_background_dark;
        if (mShowBars){
            backgroundColorId = R.color.photo_background;
        }
        return backgroundColorId;*/
        return R.color.photo_background;
    }

    private final GLView mRootPane = new GLView() {
        @Override
        protected void onLayout(
                boolean changed, int left, int top, int right, int bottom) {
            mPhotoView.layout(0, 0, right - left, bottom - top);
            if (mShowDetails) {
                mDetailsHelper.layout(left, 0, right, bottom);
            }
        }
    };

    @Override
    public void onCreate(Bundle data, Bundle restoreState) {
        super.onCreate(data, restoreState);
        mActionBar = mActivity.getGalleryActionBar();
        mActionBar.enableNoneMode(false);
        mTyGalleryBottomBar = mActivity.getGalleryBottomBar();
        mTyGalleryBottomBar.setOnClickListener(null);
        mTyGalleryBottomBar.enableNoneMode(false);
        mSelectionManager = new SelectionManager(mActivity, false);
        mMenuExecutor = new MenuExecutor(mActivity, mSelectionManager);
        //TY wb034 20150204 add begin for tygallery
        mTyCollectToAlbumCompleteListener = new TyCollectToAlbumCompleteListener(mActivity);
        mTyCollectToAlbumCompleteListener.setListener(myListener);
        //TY wb034 20150204 add end for tygallery
        mPhotoView = new PhotoView(mActivity);
        mPhotoView.setListener(this);
        mRootPane.addComponent(mPhotoView);
        mApplication = (GalleryApp) ((Activity) mActivity.getAndroidContext()).getApplication();
        mOrientationManager = mActivity.getOrientationManager();
        mActivity.getGLRoot().setOrientationSource(mOrientationManager);        
        mHandler = new SynchronizedHandler(mActivity.getGLRoot()) {
            @Override
            public void handleMessage(Message message) {
                switch (message.what) {
                    case MSG_HIDE_BARS: {
                        hideBars();
                        hidePhotoBottomMenu(true);
                        break;
                    }
                    case MSG_REFRESH_BOTTOM_CONTROLS: {
                        if (mCurrentPhoto == message.obj && mBottomControls != null) {
                            mIsPanorama = message.arg1 == 1;
                            mIsPanorama360 = message.arg2 == 1;
                        }
                        break;
                    }
                    case MSG_ON_FULL_SCREEN_CHANGED: {
                        if (mAppBridge != null) {
                            mAppBridge.onFullScreenChanged(message.arg1 == 1);
                        }
                        break;
                    }
                    case MSG_UPDATE_ACTION_BAR: {
                        updateBars();
                        break;
                    }
                    case MSG_WANT_BARS: {
                        wantBars();
                        break;
                    }
                    case MSG_UNFREEZE_GLROOT: {
                        mActivity.getGLRoot().unfreeze();
                        break;
                    }
                    case MSG_UPDATE_DEFERRED: {
                        long nextUpdate = mDeferUpdateUntil - SystemClock.uptimeMillis();
                        if (nextUpdate <= 0) {
                            mDeferredUpdateWaiting = false;
                            updateUIForCurrentPhoto();
                        } else {
                            mHandler.sendEmptyMessageDelayed(MSG_UPDATE_DEFERRED, nextUpdate);
                        }
                        break;
                    }
                    case MSG_ON_CAMERA_CENTER: {
                        mSkipUpdateCurrentPhoto = false;
                        boolean stayedOnCamera = false;
                        if (!mPhotoView.getFilmMode()) {
                            stayedOnCamera = true;
                        } else if (SystemClock.uptimeMillis() < mCameraSwitchCutoff &&
                                mMediaSet.getMediaItemCount() > 1) {
                            mPhotoView.switchToImage(1);
                        } else {
                            if (mAppBridge != null) mPhotoView.setFilmMode(false);
                            stayedOnCamera = true;
                        }

                        if (stayedOnCamera) {
                            if (mAppBridge == null && mMediaSet.getTotalMediaItemCount() > 1) {
                                launchCamera();
                                /* We got here by swiping from photo 1 to the
                                   placeholder, so make it be the thing that
                                   is in focus when the user presses back from
                                   the camera app */
                                mPhotoView.switchToImage(1);
                            } else {
                                updateBars();
                                Log.i("koala","MSG_ON_CAMERA_CENTER");
                                updateCurrentPhoto(mModel.getMediaItem(0));
                            }
                        }
                        break;
                    }
                    case MSG_ON_PICTURE_CENTER: {
                        if (!mPhotoView.getFilmMode() && mCurrentPhoto != null
                                && (mCurrentPhoto.getSupportedOperations() & MediaObject.SUPPORT_ACTION) != 0) {
                            mPhotoView.setFilmMode(true);
                        }
                        break;
                    }
                    case MSG_REFRESH_IMAGE: {
                        final MediaItem photo = mCurrentPhoto;
                        mCurrentPhoto = null;
                        Log.i("koala","MSG_REFRESH_IMAGE");
                        updateCurrentPhoto(photo);
                        break;
                    }
                    case MSG_UPDATE_PHOTO_UI: {
                        updateUIForCurrentPhoto();
                        break;
                    }
                    case MSG_UPDATE_SHARE_URI: {
                        if (mCurrentPhoto == message.obj) {
                            boolean isPanorama360 = message.arg1 != 0;
                            Uri contentUri = mCurrentPhoto.getContentUri();
                            Intent panoramaIntent = null;
                            if (isPanorama360) {
                                panoramaIntent = createSharePanoramaIntent(contentUri);
                            }
                            Intent shareIntent = createShareIntent(mCurrentPhoto);
                            //TY zhencc add for New Design Gallery
                            mTyShareIntent = shareIntent;
                            //TY zhencc end for New Design Gallery

                            mActionBar.setShareIntents(panoramaIntent, shareIntent, PhotoPage.this);
                            setNfcBeamPushUri(contentUri);
                        }
                        break;
                    }
                    case MSG_UPDATE_PANORAMA_UI: {
                        if (mCurrentPhoto == message.obj) {
                            boolean isPanorama360 = message.arg1 != 0;
                            updatePanoramaUI(isPanorama360);
                        }
                        break;
                    }
                    default: throw new AssertionError(message.what);
                }
            }
        };

        mSetPathString = data.getString(KEY_MEDIA_SET_PATH);
        mReadOnlyView = data.getBoolean(KEY_READONLY);
        //TY liuyuchuan add begin for  New Design Gallery
        pictureWidth = data.getInt(mPictureWidth);
        pictureHeight = data.getInt(mPictureHeight);
        mPhotoView.setSize(pictureWidth, pictureHeight);
        mPhotoView.setMimeType(data.getString(mPictureMimeType));
        //TY liuyuchuan add end for  New Design Gallery
        mOriginalSetPathString = mSetPathString;
        setupNfcBeamPush();
        String itemPathString = data.getString(KEY_MEDIA_ITEM_PATH);
        Path itemPath = itemPathString != null ?
                Path.fromString(data.getString(KEY_MEDIA_ITEM_PATH)) :
                    null;
        mTreatBackAsUp = data.getBoolean(KEY_TREAT_BACK_AS_UP, false);
        mStartInFilmstrip = data.getBoolean(KEY_START_IN_FILMSTRIP, false);
        boolean inCameraRoll = data.getBoolean(KEY_IN_CAMERA_ROLL, false);
        mCurrentIndex = data.getInt(KEY_INDEX_HINT, 0);
        
        if (mSetPathString != null) {
            mAppBridge = (AppBridge) data.getParcelable(KEY_APP_BRIDGE);
            if (mAppBridge != null) {
                mHasCameraScreennailOrPlaceholder = true;
                mAppBridge.setServer(this);

                // Get the ScreenNail from AppBridge and register it.
                int id = SnailSource.newId();
                Path screenNailSetPath = SnailSource.getSetPath(id);
                Path screenNailItemPath = SnailSource.getItemPath(id);
                mScreenNailSet = (SnailAlbum) mActivity.getDataManager()
                        .getMediaObject(screenNailSetPath);
                mScreenNailItem = (SnailItem) mActivity.getDataManager()
                        .getMediaObject(screenNailItemPath);
                mScreenNailItem.setScreenNail(mAppBridge.attachScreenNail());

                if (data.getBoolean(KEY_SHOW_WHEN_LOCKED, false)) {
                    // Set the flag to be on top of the lock screen.
                    mFlags |= FLAG_SHOW_WHEN_LOCKED;
                }

                // Don't display "empty album" action item for capture intents.
                if (!mSetPathString.equals("/local/all/0")) {
                    // Check if the path is a secure album.
                    if (SecureSource.isSecurePath(mSetPathString)) {
                        mSecureAlbum = (SecureAlbum) mActivity.getDataManager()
                                .getMediaSet(mSetPathString);
                    }
                    mSetPathString = "/filter/empty/{"+mSetPathString+"}";
                }

                // Combine the original MediaSet with the one for ScreenNail
                // from AppBridge.
                mSetPathString = "/combo/item/{" + screenNailSetPath +
                        "," + mSetPathString + "}";

                // Start from the screen nail.
                itemPath = screenNailItemPath;
            }
            /* wangqin delete it for new gallery 
            else if (inCameraRoll && GalleryUtils.isCameraAvailable(mActivity.getAndroidContext())) {
                mSetPathString = "/combo/item/{" + FilterSource.FILTER_CAMERA_SHORTCUT +
                        "," + mSetPathString + "}";
                mCurrentIndex++;
                mHasCameraScreennailOrPlaceholder = true;
            }
            wangqin delete it for new gallery end*/

            MediaSet originalSet = mActivity.getDataManager()
                    .getMediaSet(mSetPathString);
            if (mHasCameraScreennailOrPlaceholder && originalSet instanceof ComboAlbum) {
                // Use the name of the camera album rather than the default
                // ComboAlbum behavior
                ((ComboAlbum) originalSet).useNameOfChild(1);
            }
            mSelectionManager.setSourceMediaSet(originalSet);
            mSetPathString = "/filter/delete/{" + mSetPathString + "}";
            mMediaSet = (FilterDeleteSet) mActivity.getDataManager()
                    .getMediaSet(mSetPathString);
            if (mMediaSet == null) {
                Log.w(TAG, "failed to restore " + mSetPathString);
            }
            if (itemPath == null) {
                int mediaItemCount = mMediaSet.getMediaItemCount();
                if (mediaItemCount > 0) {
                    if (mCurrentIndex >= mediaItemCount) mCurrentIndex = 0;
                    itemPath = mMediaSet.getMediaItem(mCurrentIndex, 1)
                        .get(0).getPath();
                } else {
                    // Bail out, PhotoPage can't load on an empty album
                    return;
                }
            }
            //TY wb034 20150128 add begin for tygallery
            String id = data.getString(PhotoPage.KEY_ID,"-1");
            if(id.equals(String.valueOf(mActivity.getDataManager().mCollectBucketId))){
                mIsCollectFolder = true;
             }
            //TY wb034 20150128 add end for tygallery
            PhotoDataAdapter pda = new PhotoDataAdapter(
                    mActivity, mPhotoView, mMediaSet, itemPath, mCurrentIndex,
                    mAppBridge == null ? -1 : 0,
                    mAppBridge == null ? false : mAppBridge.isPanorama(),
					//TY wb034 add "id" for tygallery
                    mAppBridge == null ? false : mAppBridge.isStaticCamera(),id);
            mModel = pda;
            mPhotoView.setModel(mModel);

            pda.setDataListener(new PhotoDataAdapter.DataListener() {

                @Override
                public void onPhotoChanged(int index, Path item) {
                    int oldIndex = mCurrentIndex;
                    mCurrentIndex = index;
                    if (mOnPhotoChanegeListener != null){
                        mOnPhotoChanegeListener.onPhotoChanege(mCurrentIndex);
                    }
                    //TY zhencc add for New Design Gallery
                    setActionbarTitle();
                    //TY zhencc ebd for New Design Gallery

                    if (mHasCameraScreennailOrPlaceholder) {
                        if (mCurrentIndex > 0) {
                            mSkipUpdateCurrentPhoto = false;
                        }

                        if (oldIndex == 0 && mCurrentIndex > 0
                                && !mPhotoView.getFilmMode()) {
                            mPhotoView.setFilmMode(true);
                            if (mAppBridge != null) {
                                UsageStatistics.onEvent("CameraToFilmstrip",
                                        UsageStatistics.TRANSITION_SWIPE, null);
                            }
                        } else if (oldIndex == 2 && mCurrentIndex == 1) {
                            mCameraSwitchCutoff = SystemClock.uptimeMillis() +
                                    CAMERA_SWITCH_CUTOFF_THRESHOLD_MS;
                            mPhotoView.stopScrolling();
                        } else if (oldIndex >= 1 && mCurrentIndex == 0) {
                            mPhotoView.setWantPictureCenterCallbacks(true);
                            mSkipUpdateCurrentPhoto = true;
                        }
                    }
                    if (!mSkipUpdateCurrentPhoto) {
                        if (item != null) {
                            MediaItem photo = mModel.getMediaItem(0);
                            if (photo != null) updateCurrentPhoto(photo);
                            Log.i("koala","mSkipUpdateCurrentPhoto");
                        }
                        updateBars();
                    }
                    // Reset the timeout for the bars after a swipe
                    refreshHidingMessage();
                }

                @Override
                public void onLoadingFinished(boolean loadingFailed) {
                    if (!mModel.isEmpty()) {
                        MediaItem photo = mModel.getMediaItem(0);
                        if (photo != null) updateCurrentPhoto(photo);
                        Log.i("koala","onLoadingFinished");
                    } else if (mIsActive) {
                        // We only want to finish the PhotoPage if there is no
                        // deletion that the user can undo.
                        if (mMediaSet.getNumberOfDeletions() == 0) {
                            mActivity.getStateManager().finishState(
                                    PhotoPage.this);
                        }
                    }
                }

                @Override
                public void onLoadingStarted() {
                }
            });
        } else {
            // Get default media set by the URI
            MediaItem mediaItem = (MediaItem)
                    mActivity.getDataManager().getMediaObject(itemPath);
            mModel = new SinglePhotoDataAdapter(mActivity, mPhotoView, mediaItem);
            mPhotoView.setModel(mModel);
            Log.i("koala","Get default media set by the URI");
            updateCurrentPhoto(mediaItem);
        }

        mPhotoView.setFilmMode(mStartInFilmstrip && mMediaSet.getMediaItemCount() > 1);
        //RelativeLayout galleryRoot = (RelativeLayout) ((Activity) mActivity.getAndroidContext())
        //        .findViewById(mAppBridge != null ? R.id.content : R.id.gallery_root);
        RelativeLayout galleryRoot = (RelativeLayout) mActivity.getGalleryAssignView(mAppBridge != null ? R.id.content : R.id.gallery_root);
        if (galleryRoot != null) {
            if (mSecureAlbum == null) {
                mBottomControls = new PhotoPageBottomControls(this, mActivity.getAndroidContext(), galleryRoot);
            }
        }

        //((GLRootView) mActivity.getGLRoot()).setOnSystemUiVisibilityChangeListener(
        //        new View.OnSystemUiVisibilityChangeListener() {
        //        @Override
        //            public void onSystemUiVisibilityChange(int visibility) {
        //                int diff = mLastSystemUiVis ^ visibility;
        //                mLastSystemUiVis = visibility;
        //                if ((diff & View.SYSTEM_UI_FLAG_FULLSCREEN) != 0
        //                        && (visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
        //                    if (mIsActive == true){
        //                        showBars();
        //                        if (!mPhotoView.getFilmMode()){
        //                            showPhotoBottomMenu();
        //                        }
        //                    }
        //                }
        //            }
        //        });
        
        //yuxin add begin for  New Design Gallery        
        if (restoreState == null) {
            mPhotoView.setOpenAnimationRect((Rect) data.getParcelable(KEY_OPEN_ANIMATION_RECT));
        }
        //yuxin add end for  New Design Gallery
        //taoxj add 
        wantBars(); //show bars default
    }

    @Override
    public void onPictureCenter(boolean isCamera) {
        isCamera = isCamera || (mHasCameraScreennailOrPlaceholder && mAppBridge == null);
        mPhotoView.setWantPictureCenterCallbacks(false);
        mHandler.removeMessages(MSG_ON_CAMERA_CENTER);
        mHandler.removeMessages(MSG_ON_PICTURE_CENTER);
        mHandler.sendEmptyMessage(isCamera ? MSG_ON_CAMERA_CENTER : MSG_ON_PICTURE_CENTER);
    }

    @Override
    public boolean canDisplayBottomControls() {
        return mIsActive && !mPhotoView.canUndo();
    }

    @Override
    public boolean canDisplayBottomControl(int control) {
        if (mCurrentPhoto == null) {
            return false;
        }
        switch(control) {
            case R.id.photopage_bottom_control_edit:
                return mHaveImageEditor && mShowBars && !mReadOnlyView
                        && !mPhotoView.getFilmMode()
                        && (mCurrentPhoto.getSupportedOperations() & MediaItem.SUPPORT_EDIT) != 0
                        && mCurrentPhoto.getMediaType() == MediaObject.MEDIA_TYPE_IMAGE;
            case R.id.photopage_bottom_control_panorama:
                return mIsPanorama;
            case R.id.photopage_bottom_control_tiny_planet:
                return mHaveImageEditor && mShowBars
                        && mIsPanorama360 && !mPhotoView.getFilmMode();
            default:
                return false;
        }
    }

    @Override
    public void onBottomControlClicked(int control) {
        switch(control) {
            case R.id.photopage_bottom_control_edit:
                launchPhotoEditor();
                return;
            case R.id.photopage_bottom_control_panorama:
                mActivity.getPanoramaViewHelper()
                        .showPanorama(mCurrentPhoto.getContentUri());
                return;
            case R.id.photopage_bottom_control_tiny_planet:
                launchTinyPlanet();
                return;
            default:
                return;
        }
    }

    @TargetApi(ApiHelper.VERSION_CODES.JELLY_BEAN)
    private void setupNfcBeamPush() {
        if (!ApiHelper.HAS_SET_BEAM_PUSH_URIS) return;

        NfcAdapter adapter = NfcAdapter.getDefaultAdapter(mActivity.getAndroidContext());
        if (adapter != null) {
            adapter.setBeamPushUris(null, (Activity)mActivity.getAndroidContext());
            adapter.setBeamPushUrisCallback(new CreateBeamUrisCallback() {
                @Override
                public Uri[] createBeamUris(NfcEvent event) {
                    return mNfcPushUris;
                }
            }, (Activity)mActivity.getAndroidContext());
        }
    }

    private void setNfcBeamPushUri(Uri uri) {
        mNfcPushUris[0] = uri;
    }

    private static Intent createShareIntent(MediaObject mediaObject) {
        int type = mediaObject.getMediaType();
        return new Intent(Intent.ACTION_SEND)
                .setType(MenuExecutor.getMimeType(type))
                .putExtra(Intent.EXTRA_STREAM, mediaObject.getContentUri())
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
    }

    private static Intent createSharePanoramaIntent(Uri contentUri) {
        return new Intent(Intent.ACTION_SEND)
                .setType(GalleryUtils.MIME_TYPE_PANORAMA360)
                .putExtra(Intent.EXTRA_STREAM, contentUri)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
    }

    private void overrideTransitionToEditor() {
        ((Activity)mActivity.getAndroidContext()).overridePendingTransition(android.R.anim.fade_in,
                android.R.anim.fade_out);
    }

    private void launchTinyPlanet() {
        // Deep link into tiny planet
        MediaItem current = mModel.getMediaItem(0);
        Intent intent = new Intent(FilterShowActivity.TINY_PLANET_ACTION);
        intent.setClass(mActivity.getAndroidContext(), FilterShowActivity.class);
        intent.setDataAndType(current.getContentUri(), current.getMimeType())
            .setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.putExtra(FilterShowActivity.LAUNCH_FULLSCREEN,
                mActivity.isFullscreen());
        ((Activity)mActivity.getAndroidContext()).startActivityForResult(intent, REQUEST_EDIT);
        overrideTransitionToEditor();
    }

    private void launchCamera() {
        mRecenterCameraOnResume = false;
        GalleryUtils.startCameraActivity(mActivity.getAndroidContext());
    }

    private void launchPhotoEditor() {
        MediaItem current = mModel.getMediaItem(0);
        //TY wb034 20150203 add begin for tygallery        
        mInCollectAlbum = false;
        if(mActivity.getDataManager().isAleadyCollect(current.getPath())){
             mInCollectAlbum = true;
        }
        //TY wb034 20150203 add end for tygallery  
        if (current == null || (current.getSupportedOperations()
                & MediaObject.SUPPORT_EDIT) == 0) {
            return;
        }

        Intent intent = new Intent(ACTION_NEXTGEN_EDIT);

        intent.setDataAndType(current.getContentUri(), current.getMimeType())
                .setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        if (mActivity.getAndroidContext().getPackageManager()
                .queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY).size() == 0) {
            intent.setAction(Intent.ACTION_EDIT);
        }
        intent.putExtra(FilterShowActivity.LAUNCH_FULLSCREEN,
                mActivity.isFullscreen());
        ((Activity)mActivity.getAndroidContext()).startActivityForResult(Intent.createChooser(intent, null),
                REQUEST_EDIT);
        overrideTransitionToEditor();
    }

    private void launchSimpleEditor() {
        MediaItem current = mModel.getMediaItem(0);
        if (current == null || (current.getSupportedOperations()
                & MediaObject.SUPPORT_EDIT) == 0) {
            return;
        }

        Intent intent = new Intent(ACTION_SIMPLE_EDIT);

        intent.setDataAndType(current.getContentUri(), current.getMimeType())
                .setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        if (mActivity.getAndroidContext().getPackageManager()
                .queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY).size() == 0) {
            intent.setAction(Intent.ACTION_EDIT);
        }
        intent.putExtra(FilterShowActivity.LAUNCH_FULLSCREEN,
                mActivity.isFullscreen());
        ((Activity)mActivity.getAndroidContext()).startActivityForResult(Intent.createChooser(intent, null),
                REQUEST_EDIT);
        overrideTransitionToEditor();
    }

    private void requestDeferredUpdate() {
        mDeferUpdateUntil = SystemClock.uptimeMillis() + DEFERRED_UPDATE_MS;
        if (!mDeferredUpdateWaiting) {
            mDeferredUpdateWaiting = true;
            mHandler.sendEmptyMessageDelayed(MSG_UPDATE_DEFERRED, DEFERRED_UPDATE_MS);
        }
    }

    private void updateUIForCurrentPhoto() {
        if (mCurrentPhoto == null) return;

        // If by swiping or deletion the user ends up on an action item
        // and zoomed in, zoom out so that the context of the action is
        // more clear
        if ((mCurrentPhoto.getSupportedOperations() & MediaObject.SUPPORT_ACTION) != 0
                && !mPhotoView.getFilmMode()) {
            mPhotoView.setWantPictureCenterCallbacks(true);
        }
        updateMenuOperations();
        refreshBottomControlsWhenReady();
        if (mShowDetails) {
            mDetailsHelper.reloadDetails();
        }
        if ((mSecureAlbum == null)
                && (mCurrentPhoto.getSupportedOperations() & MediaItem.SUPPORT_SHARE) != 0) {
            mCurrentPhoto.getPanoramaSupport(mUpdateShareURICallback);
        }
    }

    private void updateCurrentPhoto(MediaItem photo) {
        //taoxj add for user photo begin
        PictureDAO dao = new PictureDAOImpl(mActivity.getAndroidContext());
        String userPhotoUri = dao.queryByPath(((LocalImage)photo).getFilePath()).getUserPhotoUrl();
        if(userPhotoUri != null && !"".equals(userPhotoUri)){
            mActionBar.setUserPhoto(userPhotoUri);
        }
        //taoxj add for user photo end
        if (mCurrentPhoto == photo) return;
        //TY wb034 20150204 add begin for tygallery
        mPreCurrentPhoto = mCurrentPhoto;
        mCurrentPhoto = photo;
        if (mPhotoView.getFilmMode()) {
            requestDeferredUpdate();
        } else {
            updateUIForCurrentPhoto();
        }
    }

    private void updateMenuOperations() {
        Menu menu = mActionBar.getMenu();

        // it could be null if onCreateActionBar has not been called yet
        if (menu == null) return;
        if (mCurrentPhoto == null) return;

        int supportedOperations = mCurrentPhoto.getSupportedOperations();
        if (mReadOnlyView) {
            supportedOperations ^= MediaObject.SUPPORT_EDIT;
        }
        if (mSecureAlbum != null) {
            supportedOperations &= MediaObject.SUPPORT_DELETE;
        } else {
            mCurrentPhoto.getPanoramaSupport(mUpdatePanoramaMenuItemsCallback);
            if (!mHaveImageEditor) {
                supportedOperations &= ~MediaObject.SUPPORT_EDIT;
            }
            // If current photo page is single item only, to cut some menu items
            boolean singleItemOnly = mData.getBoolean("SingleItemOnly", false);
            if (singleItemOnly) {
                supportedOperations &= ~MediaObject.SUPPORT_DELETE;
                supportedOperations &= ~MediaObject.SUPPORT_ROTATE;
                supportedOperations &= ~MediaObject.SUPPORT_SHARE;
                supportedOperations &= ~MediaObject.SUPPORT_CROP;
                supportedOperations &= ~MediaObject.SUPPORT_INFO;
            }
        }
        setActionbarTitle();
        tySetBottomMenuTag(menu);
        tyUpdateBottomMenuOperation(menu, supportedOperations);
        //TY zhencc add for makeup begin
        tyUpdateMakeupButton();
        //TY zhencc add for makeup end
        if(mPhotoView.getFilmMode()){
            setMenuVisible(false);
        }else{
            MenuExecutor.tyUpdateMenuOperation(menu, supportedOperations);
            //TY liuyuchuan add begin for  New Design Gallery
            ((Activity)mActivity.getAndroidContext()).getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
            //TY liuyuchuan add end for  New Design Gallery
            //TIANYU hanpengzhe 20151214 add for statusbar icon color  begin
            //taoxj remove
          //  ((Activity)mActivity.getAndroidContext()).getWindow().setStatusBarIconDark(false);
            //TIANYU hanpengzhe 20151214 add for statusbar icon color  end
        }
    }

    //TY zhencc add for New Design Gallery
    private void setActionbarTitle(){
        if(mActionBar != null){
            int index = mCurrentIndex + 1;
            int size = mMediaSet != null ? mMediaSet.getMediaItemCount() : 1;
            //TY wb034 20150202 add begin for tygallery
            DataManager dataManeger = mActivity.getDataManager();
            if (mOriginalSetPathString != null){
                MediaSet original = dataManeger.getMediaSet(mOriginalSetPathString);
                if(original.getBucketId() == dataManeger.mCollectBucketId){
                    int count =  dataManeger.getCollectCount();           	
                    size = count > 0 ? count : 1;           	
                }
            }
            //TY wb034 20150202 add end for tygallery 
            //TY liuyuchuan delete for PROD103693482
            if(size == 0){
                index = 0;
            }
            //TY liuyuchuan delete for PROD103693482
            mActionBar.setTitle(index + File.separator + size);
        }
    }
    
    private void tySetBottomMenuTag(Menu menu){
        tySetBottomMenuItemTag(menu, R.id.action_delete, R.id.ty_bottom_menu_delete);
        if (mCurrentPhoto.getMediaType() == MediaObject.MEDIA_TYPE_IMAGE){
            tySetBottomMenuItemTag(menu, R.id.action_edit, R.id.ty_bottom_menu_edit);
        } else if (mCurrentPhoto.getMediaType() == MediaObject.MEDIA_TYPE_VIDEO){
            tySetBottomMenuItemTag(menu, R.id.action_trim, R.id.ty_bottom_menu_edit);
        }
        tySetBottomMenuItemTag(menu, R.id.ty_action_share, R.id.ty_bottom_menu_share);
        tySetBottomMenuItemTag(menu, R.id.ty_action_menu_collect, R.id.ty_bottom_menu_collect);
    }
    
    private void tySetBottomMenuItemTag(Menu menu, int itemId, int bottomMenuId){
        if(mTyPhotoBottomMenuLayout != null){
            //taoxj modify to Button
            if(bottomMenuId == R.id.ty_bottom_menu_delete){
                ImageView bottomMenuItem = (ImageView) mTyPhotoBottomMenuLayout.findViewById(bottomMenuId);
                if (bottomMenuItem != null) {
                    MenuItem item = menu.findItem(itemId);
                    bottomMenuItem.setTag(item);
                }
            }else{
                ImageButton bottomMenuItem = (ImageButton) mTyPhotoBottomMenuLayout.findViewById(bottomMenuId);
                if (bottomMenuItem != null) {
                    MenuItem item = menu.findItem(itemId);
                    bottomMenuItem.setTag(item);
                }
            }
        }
    }
    
    private void tyUpdateBottomMenuOperation(Menu menu, int supported){
        boolean supportDelete = (supported & MediaObject.SUPPORT_DELETE) != 0;
        boolean supportShare = (supported & MediaObject.SUPPORT_SHARE) != 0;
        boolean supportEdit = (supported & MediaObject.SUPPORT_EDIT) != 0;
        boolean supportTrim = (supported & MediaObject.SUPPORT_TRIM) != 0;
        boolean suppotrCollect = (supported & MediaObject.SUPPORT_COLLECT) != 0;
        
        tySetBottomMenuImage(R.id.ty_bottom_menu_delete, supportDelete);

        if (mCurrentPhoto.getMediaType() == MediaObject.MEDIA_TYPE_IMAGE){
            tySetBottomMenuImage(R.id.ty_bottom_menu_edit, supportEdit);
        } else if (mCurrentPhoto.getMediaType() == MediaObject.MEDIA_TYPE_VIDEO){
            tySetBottomMenuImage(R.id.ty_bottom_menu_edit, supportTrim);
        } else {
            tySetBottomMenuImage(R.id.ty_bottom_menu_edit, false);
        }
        
        tySetBottomMenuImage(R.id.ty_bottom_menu_share, supportShare);

        tySetBottomMenuImage(R.id.ty_bottom_menu_collect, suppotrCollect);
        if (suppotrCollect){
            boolean isCurrentCollect = mActivity.getDataManager().isAleadyCollect(mCurrentPhoto.getPath());
            boolean isPreCollect = mPreCurrentPhoto == null ? isCurrentCollect 
                : mActivity.getDataManager().isAleadyCollect(mPreCurrentPhoto.getPath());
            if(!mIsCollectFolder){
               if(isCurrentCollect&&!isPreCollect){
                   startCollect();
               }else if(!isCurrentCollect&&isPreCollect){
                   startUnCollect();
               }else if(isCurrentCollect){
                   if(mTyPhotoCollectMenu != null) mTyPhotoCollectMenu.setAlpha(255);
               }else{
                   if(mTyPhotoCollectMenu != null) mTyPhotoCollectMenu.setAlpha(0);
               }
            }
        }
    }
    
    //TY zhencc add for makeup begin
    private void tyUpdateMakeupButton(){
       /*taoxj remove
    	boolean support = BitmapUtils.isMakeupSupported(mCurrentPhoto.getMimeType());
    	if (mTyPhotoBottomMenuLayout != null){
    		if(!(SystemProperties.getBoolean("ro.ty.makeup.support", false))){
    			View tyPhotoMakeupBg = (View) mTyPhotoBottomMenuLayout.findViewById(R.id.ty_bottom_menu_makeup_bg);
    			tyPhotoMakeupBg.setVisibility(View.GONE);
    			return;
    		}
    		ImageButton tyPhotoMakeup = (ImageButton) mTyPhotoBottomMenuLayout.findViewById(R.id.ty_bottom_menu_makeup);
    		//TY zhencc 20150715 add for PROD103891409 begin
    		if (mCurrentPhoto instanceof UriImage){
    			tyPhotoMakeup.setEnabled(false);
    			return;
    		}
    		//TY zhencc 20150715 add for PROD103891409 end
    		MediaDetails mediaDetails = mCurrentPhoto.getDetails();
    		int width = toLocalInteger(mediaDetails.getDetail(MediaDetails.INDEX_WIDTH));
    		int height = toLocalInteger(mediaDetails.getDetail(MediaDetails.INDEX_HEIGHT));
    		Log.d(TAG, "tyUpdateMakeupButton() mediaType:"+mCurrentPhoto.getMediaType() +", support:"+support
    				+ ", width:"+width + ", height:"+height);
    		if((mCurrentPhoto.getMediaType() == MediaObject.MEDIA_TYPE_IMAGE) 
    				&& support
    				&& width > 50
    				&& height > 50){	
                tyPhotoMakeup.setEnabled(true);
    		} else{
    			tyPhotoMakeup.setEnabled(false);
    		}
    	} 	*/		
    }
    
    private int toLocalInteger(Object valueObj) {
        if (valueObj instanceof Integer) {
            return (Integer)valueObj;
        } else {
            String value = valueObj.toString();
            return Integer.parseInt(value);
        }
    }
    //TY zhencc add for makeup end
    
    private void tySetBottomMenuImage(int bottomMenuId, boolean support/*int enableImage, int disableImage*/){
        if(mTyPhotoBottomMenuLayout != null){
            //taoxj modify to Button
            if(bottomMenuId == R.id.ty_bottom_menu_delete){
                ImageView bottomMenuItem = (ImageView) mTyPhotoBottomMenuLayout.findViewById(bottomMenuId);
                if (bottomMenuItem != null) {
                    bottomMenuItem.setEnabled(support);
                }
            }else{
                ImageButton bottomMenuItem = (ImageButton) mTyPhotoBottomMenuLayout.findViewById(bottomMenuId);
                if (bottomMenuItem != null) {
                    if (bottomMenuId == R.id.ty_bottom_menu_collect && support == false){
                        mTyPhotoCollectMenu.setAlpha(255);
                    }
                    bottomMenuItem.setEnabled(support);
                }
            }

        }
    }
    
    private class AppInfo{
    	Drawable mAppIcon;
    	String mAppName;
    	String mPackageName;
    	String mLauncherClassName;
    }
    
    private List<AppInfo> getShareApps(){
    	if(mTyShareIntent == null){
    		return null;
    	}
    	List<ResolveInfo> resolveInfoList = new ArrayList<ResolveInfo>();
    	PackageManager pManager = mActivity.getAndroidContext().getPackageManager(); 
    	resolveInfoList = pManager.queryIntentActivities(mTyShareIntent, PackageManager.COMPONENT_ENABLED_STATE_DEFAULT);
    	
    	List<AppInfo> shareAppInfos = new ArrayList<AppInfo>(); 
    	if (null == resolveInfoList) {  
            return null;  
        } else {  
            for (ResolveInfo resolveInfo : resolveInfoList) {  
                AppInfo appInfo = new AppInfo();  
                appInfo.mAppIcon = resolveInfo.loadIcon(pManager);  
                appInfo.mAppName = resolveInfo.loadLabel(pManager).toString();  
                appInfo.mPackageName = resolveInfo.activityInfo.packageName;  
                appInfo.mLauncherClassName = resolveInfo.activityInfo.name;  
                shareAppInfos.add(appInfo);  
            }  
        }
    	return shareAppInfos; 
    }
    
    private View getShareListView(){
        ListView listView = new ListView(mActivity.getAndroidContext());
        mListAppInfo = getShareApps();
        if(mListAppInfo == null){
            return null;
        }
        listView.setAdapter(new ShareAdapter());
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent shareIntent = new Intent(mTyShareIntent);  
                AppInfo appInfo = (AppInfo) mListAppInfo.get(position);
                shareIntent.setComponent(new ComponentName(appInfo.mPackageName ,appInfo.mLauncherClassName));  
                mActivity.getAndroidContext().startActivity(shareIntent); 
                //TY zhencc 20150724 add for PROD103923109 begin
                ((Activity)mActivity.getAndroidContext()).overridePendingTransition(0,0);
                //TY zhencc 20150724 add for PROD103923109 end
                dismissAlertDialog();
            }
        });
        return listView;
    }
    
    private class ShareAdapter extends BaseAdapter{
    	
		@Override
		public int getCount() {
			return mListAppInfo.size();
		}

		@Override
		public Object getItem(int position) {
			return mListAppInfo.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			final ViewHolder holder;

			if (null == convertView) {
				convertView = View.inflate(mActivity.getAndroidContext(), R.layout.ty_share_list_item,
						null);
				holder = new ViewHolder();
				holder.appIcon = (ImageView) convertView
						.findViewById(R.id.ty_item_icon);
				holder.appName = (TextView) convertView
						.findViewById(R.id.ty_item_name);

				convertView.setTag(holder);
			} else {
				holder = (ViewHolder) convertView.getTag();
			}
			holder.appIcon.setImageDrawable(mListAppInfo.get(position).mAppIcon);
			holder.appName.setText(mListAppInfo.get(position).mAppName);
			return convertView;
		}
		
		class ViewHolder {
			ImageView appIcon;
			TextView appName;
		}
    }
    
    private void dismissAlertDialog(){
    	if(mShareListAlertDialog != null){
    		mShareListAlertDialog.dismiss();
    		mShareListAlertDialog = null;
    	}
    }
    //TY zhencc end for New Design Gallery
    
    private boolean canDoSlideShow() {
        if (mMediaSet == null || mCurrentPhoto == null) {
            return false;
        }
        if (mCurrentPhoto.getMediaType() != MediaObject.MEDIA_TYPE_IMAGE) {
            return false;
        }
        return true;
    }

    //////////////////////////////////////////////////////////////////////////
    //  Action Bar show/hide management
    //////////////////////////////////////////////////////////////////////////

    private void showBars() {
        if (mShowBars) return;
        mShowBars = true;
        
        mOrientationManager.unlockOrientation();
        mActionBar.enableTransBackMode(this);
        setActionbarTitle();
        
        mActivity.getGLRoot().setLightsOutMode(false);
        refreshHidingMessage();
        refreshBottomControlsWhenReady();
        float[] bg = GalleryUtils.intColorToFloatARGBArray(
                        mActivity.getResources().getColor(getBackgroundColorId()));
        mRootPane.setBackgroundColor(bg);
        mRootPane.invalidate();
    }

    private void hideBars() {
        if (!mShowBars) return;
        mShowBars = false;
        Log.i("koala","photo page hideBars enableNoneMode true");
        mActionBar.enableNoneMode(true);
        mActivity.getGLRoot().setLightsOutMode(true);
        mHandler.removeMessages(MSG_HIDE_BARS);
        refreshBottomControlsWhenReady();
        float[] bg = GalleryUtils.intColorToFloatARGBArray(
                        mActivity.getResources().getColor(getBackgroundColorId()));
        mRootPane.setBackgroundColor(bg);
        mRootPane.invalidate();
    }

    private void refreshHidingMessage() {
        mHandler.removeMessages(MSG_HIDE_BARS);
        if (!mIsMenuVisible && !mPhotoView.getFilmMode()) {
           // mHandler.sendEmptyMessageDelayed(MSG_HIDE_BARS, HIDE_BARS_TIMEOUT);//taoxj remove
        }
    }

    private boolean canShowBars() {
        // No bars if we are showing camera preview.
        if (mAppBridge != null && mCurrentIndex == 0
                && !mPhotoView.getFilmMode()) return false;

        // No bars if it's not allowed.
        if (!mActionBarAllowed) return false;
		
        /*TIANYURD:Lizhy 20150429 modifyfor PROD103739417 start*/
        //if(((Fragment)mActivity).isAdded()){
        Configuration config = mActivity.getResources().getConfiguration();
        if (config.touchscreen == Configuration.TOUCHSCREEN_NOTOUCH) {
            return false;
        }
        //}
        /*TIANYURD:Lizhy 20150429 modifyfor PROD103739417 end*/
        return true;
    }

    private void wantBars() {
        if (canShowBars()) {
            showBars();
            if (!mPhotoView.getFilmMode()){
                showPhotoBottomMenu();
            }
        }
    }

    private void toggleBars() {
        if (mShowBars) {
            if (!mPhotoView.getFilmMode()){
                hideBars();
                hidePhotoBottomMenu(true);
            }
        } else {
            if (canShowBars()) {
                showBars();
                if (!mPhotoView.getFilmMode()){
                    showPhotoBottomMenu();
                }
            }
        }
    }

    private void updateBars() {
        if (!canShowBars()) {
            hideBars();
            hidePhotoBottomMenu(true);
        }
    }

    @Override
    protected void onBackPressed() {
        //showBars();
        if (mShowDetails) {
            hideDetails();
        } else if (mAppBridge == null || !switchWithCaptureAnimation(-1)) {
            // We are leaving this page. Set the result now.
            setResult();
            if (mStartInFilmstrip && !mPhotoView.getFilmMode()) {
                mPhotoView.setFilmMode(true);
            } else if (mTreatBackAsUp) {
                onUpPressed();
            } else {
                super.onBackPressed();
            }
        }
    }

    private void onUpPressed() {
        if ((mStartInFilmstrip || mAppBridge != null)
                && !mPhotoView.getFilmMode()) {
            mPhotoView.setFilmMode(true);
            return;
        }

        if (mActivity.getStateManager().getStateCount() > 1) {
            setResult();
            super.onBackPressed();
            return;
        }
        //TY zhencc add for New Design Gallery
        else if(mActivity.getStateManager().getStateCount() == 1){
            mActivity.getStateManager().finishState(this);
            return;
        }
        //TY zhencc end for New Design Gallery

        if (mOriginalSetPathString == null) return;

        if (mAppBridge == null) {
            // We're in view mode so set up the stacks on our own.
            Bundle data = new Bundle(getData());
            data.putString(AlbumPage.KEY_MEDIA_PATH, mOriginalSetPathString);
            data.putString(AlbumPage.KEY_PARENT_MEDIA_PATH,
                    mActivity.getDataManager().getTopSetPath(
                            DataManager.INCLUDE_ALL));
            mActivity.getStateManager().switchState(this, AlbumPage.class, data);
        } else {
            GalleryUtils.startGalleryActivity(mActivity.getAndroidContext());
        }
    }

    private void setResult() {
        Intent result = null;
        result = new Intent();
        result.putExtra(KEY_RETURN_INDEX_HINT, mCurrentIndex);
        setStateResult(Activity.RESULT_OK, result);
    }

    //////////////////////////////////////////////////////////////////////////
    //  AppBridge.Server interface
    //////////////////////////////////////////////////////////////////////////

    @Override
    public void setCameraRelativeFrame(Rect frame) {
        mPhotoView.setCameraRelativeFrame(frame);
    }

    @Override
    public boolean switchWithCaptureAnimation(int offset) {
        return mPhotoView.switchWithCaptureAnimation(offset);
    }

    @Override
    public void setSwipingEnabled(boolean enabled) {
        mPhotoView.setSwipingEnabled(enabled);
    }

    @Override
    public void notifyScreenNailChanged() {
        mScreenNailItem.setScreenNail(mAppBridge.attachScreenNail());
        mScreenNailSet.notifyChange();
    }

    @Override
    public void addSecureAlbumItem(boolean isVideo, int id) {
        mSecureAlbum.addMediaItem(isVideo, id);
    }

    @Override
    protected boolean onCreateActionBar(Menu menu) {
        mActionMenu = menu;
        //taoxj remove menu
        mActionBar.createActionBarMenu(R.menu.ty_photo, menu);
        mHaveImageEditor = GalleryUtils.isEditorAvailable(mActivity.getAndroidContext(), "image/*");
        updateMenuOperations();
        setActionbarTitle();
        return true;
    }

    private MenuExecutor.ProgressListener mConfirmDialogListener =
            new MenuExecutor.ProgressListener() {
        @Override
        public void onProgressUpdate(int index) {}

        @Override
        public void onProgressComplete(int result) {}

        @Override
        public void onConfirmDialogShown() {
            mHandler.removeMessages(MSG_HIDE_BARS);
        }

        @Override
        public void onConfirmDialogDismissed(boolean confirmed) {
            refreshHidingMessage();
        }

        @Override
        public void onProgressStart() {}
    };

    @Override
    protected boolean onItemSelected(MenuItem item) {
        if (mModel == null) return true;
        refreshHidingMessage();
        MediaItem current = mModel.getMediaItem(0);

        // This is a shield for monkey when it clicks the action bar
        // menu when transitioning from filmstrip to camera
        if (current instanceof SnailItem) return true;
        // TODO: We should check the current photo against the MediaItem
        // that the menu was initially created for. We need to fix this
        // after PhotoPage being refactored.
        if (current == null) {
            // item is not ready, ignore
            return true;
        }
        int currentIndex = mModel.getCurrentIndex();
        Path path = current.getPath();

        DataManager manager = mActivity.getDataManager();
        int action = item.getItemId();
        String confirmMsg = null;
        switch (action) {
            case android.R.id.home: {
                onUpPressed();
                return true;
            }
            case R.id.action_slideshow: {
                Log.i("koala","photopage action_slideshow");
                Bundle data = new Bundle();
                data.putString(SlideshowPage.KEY_SET_PATH, mMediaSet.getPath().toString());
                data.putString(SlideshowPage.KEY_ITEM_PATH, path.toString());
                data.putInt(SlideshowPage.KEY_PHOTO_INDEX, currentIndex);
                data.putBoolean(SlideshowPage.KEY_REPEAT, true);
                mActivity.getStateManager().startStateForResult(
                        SlideshowPage.class, REQUEST_SLIDESHOW, data);
                return true;
            }
            case R.id.action_crop: {
                Activity activity = (Activity)mActivity.getAndroidContext();
                Intent intent = new Intent(CropActivity.CROP_ACTION);
                intent.setClass(activity, CropActivity.class);
                intent.setDataAndType(manager.getContentUri(path), current.getMimeType())
                    .setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                activity.startActivityForResult(intent, PicasaSource.isPicasaImage(current)
                        ? REQUEST_CROP_PICASA
                        : REQUEST_CROP);
                return true;
            }
            case R.id.action_trim: {
                Intent intent = new Intent(mActivity.getAndroidContext(), TrimVideo.class);
                intent.setData(manager.getContentUri(path));
                // We need the file path to wrap this into a RandomAccessFile.
                String str = MediaFile.getMimeTypeForFile(current.getFilePath());
                if ("video/mp4".equals(str) || "video/mpeg4".equals(str)
                            || "video/3gpp".equals(str) || "video/3gpp2".equals(str)) {
                    intent.putExtra(KEY_MEDIA_ITEM_PATH, current.getFilePath());
                    ((Activity)mActivity.getAndroidContext()).startActivityForResult(intent, REQUEST_TRIM);
                } else {
                    Toast.makeText(mActivity.getAndroidContext(), mActivity.getAndroidContext().getString(R.string.can_not_trim),
                        Toast.LENGTH_SHORT).show();
                }
                return true;
            }
            case R.id.action_mute: {
                final String mime = MediaFile.getMimeTypeForFile(current.getFilePath());
                // Can only mute mp4, mpeg4 and 3gp
                if ("video/mp4".equals(mime) || "video/mpeg4".equals(mime)
                            || "video/3gpp".equals(mime) || "video/3gpp2".equals(mime)) {
                    MuteVideo muteVideo = new MuteVideo(current.getFilePath(),
                            manager.getContentUri(path), (Activity)mActivity.getAndroidContext());
                    muteVideo.muteInBackground();
                } else {
                    Toast.makeText(mActivity.getAndroidContext(), mActivity.getAndroidContext().getString(R.string.video_mute_err),
                            Toast.LENGTH_SHORT).show();
                }
                return true;
            }
            case R.id.action_edit: {
                launchPhotoEditor();
                return true;
            }
            case R.id.action_simple_edit: {
                launchSimpleEditor();
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
            case R.id.print: {
                mActivity.printSelectedImage(manager.getContentUri(path));
                return true;
            }
            case R.id.action_delete:
                confirmMsg = mActivity.getResources().getQuantityString(
                        R.plurals.delete_selection, 1);
            case R.id.action_setas:
            case R.id.action_rotate_ccw:
            case R.id.action_rotate_cw:
            case R.id.action_show_on_map:
                mSelectionManager.deSelectAll();
                mSelectionManager.toggle(path);
                mMenuExecutor.onMenuClicked(item, confirmMsg, mConfirmDialogListener);
                return true;
            case R.id.ty_action_add_to_album:
                mSelectionManager.deSelectAll();
                mSelectionManager.toggle(path);
                mMenuExecutor.onMenuClicked(item, confirmMsg, 
                    new TyAddToAlbumCompleteListener(mActivity));
                return true;
            case R.id.ty_action_share:
                if(mShareListAlertDialog == null){
                    mShareListAlertDialog = new AlertDialog.Builder(mActivity.getAndroidContext())
                        .setTitle(mActivity.getResources().getString(R.string.ty_share_title))
                        .create();
                }
                mShareListAlertDialog.setView(getShareListView());
                mShareListAlertDialog.show();
                return true;
            case R.id.ty_action_menu_collect:
                mSelectionManager.deSelectAll();
                mSelectionManager.toggle(path);
                mMenuExecutor.onMenuClicked(action, mTyCollectToAlbumCompleteListener,
                    false, false);
                return true;
            default :
                return false;
        }
    }
  //TY wb034 20150204 add begin for tygallery
    public  class MyListener implements TyCollectToAlbumCompleteListener.Listener{
		@Override
		public void collectComplete(int result) {
            switch (result) {
			case MenuExecutor.EXECUTION_RESULT_SUCCESS:
			    if(!mIsCollectFolder)startCollect();
				break;
			case MenuExecutor.EXECUTION_DELETE_ALBUM_SUCCESS:
			    if(!mIsCollectFolder)startUnCollect();	
				break;

			default:
				break;
			}			
		}      
    } 
    public void startCollect(){
        if(mTyPhotoCollectMenu != null){
            mTyPhotoCollectMenu.setAlpha(255);
            AlphaAnimation alpha = new AlphaAnimation(0f, 1.0f);
            alpha.setDuration(300);
            alpha.setFillAfter(true);
            mTyPhotoCollectMenu.startAnimation(alpha);
        }
    }
    public void startUnCollect(){
        if(mTyPhotoCollectMenu != null){
            AlphaAnimation unalpha = new AlphaAnimation(1.0f, 0f);
            unalpha.setDuration(300);
            unalpha.setFillAfter(true);
            unalpha.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation arg0) {}
                
                @Override
                public void onAnimationRepeat(Animation arg0) {}
                
                @Override
                public void onAnimationEnd(Animation arg0) {
                    mTyPhotoCollectMenu.setAlpha(0);
                }
            });
            mTyPhotoCollectMenu.startAnimation(unalpha);
        }
      }    
//TY wb034 20150204 add end for tygallery
    
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
        	case R.id.ty_bottom_menu_delete:
            case R.id.ty_bottom_menu_edit:
            case R.id.ty_bottom_menu_share:
            case R.id.ty_bottom_menu_collect:{
                MenuItem item = (MenuItem)v.getTag();
                if(item != null){
                  onItemSelected(item);
                }
                break;
            }
            //TY zhencc add for makeup begin
            case R.id.ty_bottom_menu_makeup:
            	try {
            		Intent intent = new Intent();
                	intent.setComponent(new ComponentName("com.gangyun.makeup",
                			"com.gangyun.makeup.gallery3d.makeup.MakeUpActivity"));
                	intent.setData(mCurrentPhoto.getContentUri());
                	mActivity.getAndroidContext().startActivity(intent);
                } catch (ActivityNotFoundException e) {
                    Toast.makeText(mActivity.getAndroidContext(), mActivity.getAndroidContext().getString(R.string.makeup_error),
                            Toast.LENGTH_SHORT).show();
                }
            	break;
            //TY zhencc add for makeup end
        }
    }
    @Override
    public void onActionBarClick(View v){
        switch (v.getId()) {
            case R.id.ty_action_title_close:
            case R.id.ty_action_title_trans_close:{
                GLRoot root = mActivity.getGLRoot();
                root.lockRenderThread();
                try {
                    mActivity.getStateManager().onBackPressed();
                } finally {
                    root.unlockRenderThread();
                }
                break;
            }
        }
    }

    private void hideDetails() {
        mShowDetails = false;
        mDetailsHelper.hide();
    }

    private void showDetails() {
        mShowDetails = true;
        if (mDetailsHelper == null) {
            mDetailsHelper = new DetailsHelper(mActivity, mRootPane, new MyDetailsSource());
            mDetailsHelper.setCloseListener(new CloseListener() {
                @Override
                public void onClose() {
                    hideDetails();
                }
            });
        }
        mDetailsHelper.show();
    }

    ////////////////////////////////////////////////////////////////////////////
    //  Callbacks from PhotoView
    ////////////////////////////////////////////////////////////////////////////
    @Override
    public void onSingleTapUp(int x, int y) {
        if (mAppBridge != null) {
            if (mAppBridge.onSingleTapUp(x, y)) return;
        }

        MediaItem item = mModel.getMediaItem(0);
        if (item == null || item == mScreenNailItem) {
            // item is not ready or it is camera preview, ignore
            return;
        }

        try{
            if (item.getMimeType().equals(MediaItem.MIME_TYPE_GIF)) {
                //TY liuyuchuan  modify begin for PROD103694686
                int w = mPhotoView.getWidth();
                int h = mPhotoView.getHeight();
                boolean playGif = (Math.abs(x - w / 2) * 12 <= w)
                        && (Math.abs(y - h / 2) * 12 <= h);
                if (playGif) {
                    viewAnimateGif((Activity) mActivity.getAndroidContext(), item.getContentUri());
                    return;  
                }
                //TY liuyuchuan  modify end for PROD103694686
            }
        }catch(NullPointerException e){
            Log.e(TAG, "NullPointerException:" + e);
            return;
        }
        
        int supported = item.getSupportedOperations();
        boolean playVideo = ((supported & MediaItem.SUPPORT_PLAY) != 0);
        boolean unlock = ((supported & MediaItem.SUPPORT_UNLOCK) != 0);
        boolean goBack = ((supported & MediaItem.SUPPORT_BACK) != 0);
        boolean launchCamera = ((supported & MediaItem.SUPPORT_CAMERA_SHORTCUT) != 0);

        if (playVideo) {
            // determine if the point is at center (1/6) of the photo view.
            // (The position of the "play" icon is at center (1/6) of the photo)
            int w = mPhotoView.getWidth();
            int h = mPhotoView.getHeight();
            playVideo = (Math.abs(x - w / 2) * 12 <= w)
                && (Math.abs(y - h / 2) * 12 <= h);
        }

        if (playVideo) {
            if (mSecureAlbum == null) {
                playVideo((Activity)mActivity.getAndroidContext(), item.getPlayUri(), item.getName());
            } else {
                mActivity.getStateManager().finishState(this);
            }
        } else if (goBack) {
            onBackPressed();
        } else if (unlock) {
            Intent intent = new Intent(mActivity.getAndroidContext(), GalleryActivity.class);
            intent.putExtra(GalleryActivity.KEY_DISMISS_KEYGUARD, true);
            mActivity.getAndroidContext().startActivity(intent);
        } else if (launchCamera) {
            if(mAppBridge == null && mMediaSet.getTotalMediaItemCount() <= 1){
                launchCamera();
            }
        } else {
            toggleBars();
        }
    }

    @Override
    public void onActionBarAllowed(boolean allowed) {
        mActionBarAllowed = allowed;
        mHandler.sendEmptyMessage(MSG_UPDATE_ACTION_BAR);
    }

    @Override
    public void onActionBarWanted() {
        mHandler.sendEmptyMessage(MSG_WANT_BARS);
    }

    @Override
    public void onFullScreenChanged(boolean full) {
        Message m = mHandler.obtainMessage(
                MSG_ON_FULL_SCREEN_CHANGED, full ? 1 : 0, 0);
        m.sendToTarget();
    }

    // How we do delete/undo:
    //
    // When the user choose to delete a media item, we just tell the
    // FilterDeleteSet to hide that item. If the user choose to undo it, we
    // again tell FilterDeleteSet not to hide it. If the user choose to commit
    // the deletion, we then actually delete the media item.
    @Override
    public void onDeleteImage(Path path, int offset) {
        onCommitDeleteImage();  // commit the previous deletion
        mDeletePath = path;
        mDeleteIsFocus = (offset == 0);
        mMediaSet.addDeletion(path, mCurrentIndex + offset);
    }

    @Override
    public void onUndoDeleteImage() {
        if (mDeletePath == null) return;
        // If the deletion was done on the focused item, we want the model to
        // focus on it when it is undeleted.
        if (mDeleteIsFocus) mModel.setFocusHintPath(mDeletePath);
        mMediaSet.removeDeletion(mDeletePath);
        mDeletePath = null;
    }

    @Override
    public void onCommitDeleteImage() {
        if (mDeletePath == null) return;
        mMenuExecutor.startSingleItemAction(R.id.action_delete, mDeletePath);
        mDeletePath = null;
    }

    public void playVideo(Activity activity, Uri uri, String title) {
        try {
            Intent intent = new Intent(activity, MovieActivity.class)
            //Intent intent = new Intent(Intent.ACTION_VIEW)
                    .setDataAndType(uri, "video/*")
                    .putExtra(Intent.EXTRA_TITLE, title)
                    .putExtra(MovieActivity.KEY_TREAT_UP_AS_BACK, true);
            activity.startActivityForResult(intent, REQUEST_PLAY_VIDEO);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(activity, activity.getString(R.string.video_err),
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void setCurrentPhotoByIntent(Intent intent) {
        if (intent == null) return;
        Path path = mApplication.getDataManager()
                .findPathByUri(intent.getData(), intent.getType());
        if (path != null) {
            Path albumPath = mApplication.getDataManager().getDefaultSetOf(path);
            if (albumPath == null) {
                return;
            }
            //TY wb034 20150203 add begin for tygaller
            if(mInCollectAlbum){
                 mActivity.getDataManager().Collect(true, path);
            }
            //TY wb034 20150203 add begin for tygaller
            if (!albumPath.equalsIgnoreCase(mOriginalSetPathString)) {
                // If the edited image is stored in a different album, we need
                // to start a new activity state to show the new image
                Bundle data = new Bundle(getData());
                data.putString(KEY_MEDIA_SET_PATH, albumPath.toString());
                data.putString(PhotoPage.KEY_MEDIA_ITEM_PATH, path.toString());
                mActivity.getStateManager().startState(SinglePhotoPage.class, data);
                return;
            }
            mModel.setCurrentPhoto(path, mCurrentIndex);
        }
    }

    @Override
    protected void onStateResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_CANCELED) {
            // This is a reset, not a canceled
            return;
        }
        mRecenterCameraOnResume = false;
        switch (requestCode) {
            case REQUEST_EDIT:
                setCurrentPhotoByIntent(data);
                break;
            case REQUEST_CROP:
                if (resultCode == Activity.RESULT_OK) {
                    setCurrentPhotoByIntent(data);
                }
                break;
            case REQUEST_CROP_PICASA: {
                if (resultCode == Activity.RESULT_OK) {
                    Context context = mActivity.getAndroidContext();
                    String message = context.getString(R.string.crop_saved,
                            context.getString(R.string.folder_edited_online_photos));
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
                }
                break;
            }
            case REQUEST_SLIDESHOW: {
                if (data == null) break;
                String path = data.getStringExtra(SlideshowPage.KEY_ITEM_PATH);
                int index = data.getIntExtra(SlideshowPage.KEY_PHOTO_INDEX, 0);
                if (path != null) {
                    mModel.setCurrentPhoto(Path.fromString(path), index);
                }
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        mIsActive = false;
        mActivity.getGLRoot().unfreeze();
        mHandler.removeMessages(MSG_UNFREEZE_GLROOT);
        if (isFinishing()) {
            preparePhotoFallbackView();
        }
        /*TIANYURD:Lizhy 20150515 add for PROD103739407 start*/
        hideBars();
        /*TIANYURD:Lizhy 20150515 add for PROD103739407 end*/
        hidePhotoBottomMenu(false);
        DetailsHelper.pause();
        // Hide the detail dialog on exit
        if (mShowDetails) hideDetails();
        if (mModel != null) {
            mModel.pause();
        }
        mPhotoView.pause();
        mHandler.removeMessages(MSG_HIDE_BARS);
        mHandler.removeMessages(MSG_REFRESH_BOTTOM_CONTROLS);
        refreshBottomControlsWhenReady();
        mActionBar.removeOnMenuVisibilityListener(mMenuVisibilityListener);
        onCommitDeleteImage();
        mMenuExecutor.pause();
        if (mMediaSet != null) mMediaSet.clearDeletion();        
    }

    @Override
    public void onCurrentImageUpdated() {
        mActivity.getGLRoot().unfreeze();
    }

    @Override
    public void onFilmModeChanged(boolean enabled) {
        //TY wb034 20150204 add begin for tygallery
        updateMenuOperations();
        //TY wb034 20150204 add end for tygallery
        refreshBottomControlsWhenReady();
        if (enabled) {
            mHandler.removeMessages(MSG_HIDE_BARS);
            showBars();
            hidePhotoBottomMenu(true);
            UsageStatistics.onContentViewChanged(
                    UsageStatistics.COMPONENT_GALLERY, "FilmstripPage");
        } else {
            refreshHidingMessage();
            hideBars();
            hidePhotoBottomMenu(true);
            if (mAppBridge == null || mCurrentIndex > 0) {
                UsageStatistics.onContentViewChanged(
                        UsageStatistics.COMPONENT_GALLERY, "SinglePhotoPage");
            } else {
                UsageStatistics.onContentViewChanged(
                        UsageStatistics.COMPONENT_CAMERA, "Unknown"); // TODO
            }
        }
    }
    
    private void setMenuVisible(boolean visible){
        mActionMenu.setGroupVisible(R.id.ty_photo_operation_menu, visible);
    }

    private void transitionFromAlbumPageIfNeeded() {
        TransitionStore transitions = mActivity.getTransitionStore();

        int albumPageTransition = transitions.get(
                KEY_ALBUMPAGE_TRANSITION, MSG_ALBUMPAGE_NONE);
        if (albumPageTransition == MSG_ALBUMPAGE_NONE && mAppBridge != null
                && mRecenterCameraOnResume) {
            // Generally, resuming the PhotoPage when in Camera should
            // reset to the capture mode to allow quick photo taking
            mCurrentIndex = 0;
            mPhotoView.resetToFirstPicture();
        } else {
            int resumeIndex = transitions.get(KEY_INDEX_HINT, -1);
            if (resumeIndex >= 0) {
                if (mHasCameraScreennailOrPlaceholder) {
                    // Account for preview/placeholder being the first item
                    resumeIndex++;
                }
                if (resumeIndex < mMediaSet.getMediaItemCount()) {
                    mCurrentIndex = resumeIndex;
                    mModel.moveTo(mCurrentIndex);
                }
            }
        }

        if (albumPageTransition == MSG_ALBUMPAGE_RESUMED) {
            mPhotoView.setFilmMode(mStartInFilmstrip || mAppBridge != null);
        } else if (albumPageTransition == MSG_ALBUMPAGE_PICKED) {
            mPhotoView.setFilmMode(false);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mModel == null) {
            mActivity.getStateManager().finishState(this);
            return;
        }
        transitionFromAlbumPageIfNeeded();

        mActivity.getGLRoot().freeze();
        mIsActive = true;
        setContentPane(mRootPane);

        mModel.resume();
        mPhotoView.resume();
        mActionBar.addOnMenuVisibilityListener(mMenuVisibilityListener);
        refreshBottomControlsWhenReady();
        
        if (mPhotoView.getFilmMode()) {
            if (mShowBars){
                float[] bg = GalleryUtils.intColorToFloatARGBArray(
                                mActivity.getResources().getColor(getBackgroundColorId()));
                mRootPane.setBackgroundColor(bg);
                mRootPane.invalidate();
            }
            showBars();
            hidePhotoBottomMenu(true);
        }else{
            if (mShowBars){
                mShowBars = false;
                showBars();
                showPhotoBottomMenu();
            }
        }
        
        boolean haveImageEditor = GalleryUtils.isEditorAvailable(mActivity.getAndroidContext(), "image/*");
        if (haveImageEditor != mHaveImageEditor) {
            mHaveImageEditor = haveImageEditor;
            updateMenuOperations();
        }

        mRecenterCameraOnResume = true;
        mHandler.sendEmptyMessageDelayed(MSG_UNFREEZE_GLROOT, UNFREEZE_GLROOT_TIMEOUT);
        //TIANYU hanpengzhe 20151214 add for statusbar icon color  begin
       //((Activity)mActivity.getAndroidContext()).getWindow().setStatusBarIconDark(false);
        //TIANYU hanpengzhe 20151214 add for statusbar icon color  end
    }
    //TY zhencc add for New Design Gallery
    private boolean setupPhotoBottomMenu() {
        RelativeLayout galleryRoot = (RelativeLayout) mActivity.getGalleryAssignView(mAppBridge != null ? R.id.content : R.id.gallery_root);
        if (galleryRoot == null) return false;

        if(mTyPhotoBottomMenuLayout == null){
        	mTyPhotoBottomMenuLayout = (RelativeLayout)((Activity) mActivity.getAndroidContext()).getLayoutInflater().from(mActivity.getAndroidContext()).inflate(R.layout.ty_photo_bottom_menu_layout_health, null);//taoxj modify

            ImageView tyPhotoDeleteMenu = (ImageView) mTyPhotoBottomMenuLayout.findViewById(R.id.ty_bottom_menu_delete);
        	ImageButton tyPhotoEditMenu = (ImageButton) mTyPhotoBottomMenuLayout.findViewById(R.id.ty_bottom_menu_edit);
        	//TY zhencc add for makeup begin
        	ImageButton tyPhotoMakeup = (ImageButton) mTyPhotoBottomMenuLayout.findViewById(R.id.ty_bottom_menu_makeup);
        	//TY zhencc add for makeup end
        	ImageButton tyPhotoShareMenu = (ImageButton) mTyPhotoBottomMenuLayout.findViewById(R.id.ty_bottom_menu_share);
        	mTyPhotoCollectMenu = (ImageButton) mTyPhotoBottomMenuLayout.findViewById(R.id.ty_bottom_menu_collect);
                /*taoxj remove
        	//TY WB034 20150311 add begin for tygallery
        	if(mIsCollectFolder) {
        	    mTyPhotoCollectMenu.setAlpha(255);
        	}else {
        	    mTyPhotoCollectMenu.setAlpha(0);
        	    mTyPhotoCollectMenu.getBackground().setAlpha(0);
        	}
        	//TY wb034 20150311 add end for tygallery */
        	tyPhotoDeleteMenu.setOnClickListener(this);
                /*taoxj remove
        	tyPhotoEditMenu.setOnClickListener(this);
        	//TY zhencc add for makeup begin
        	tyPhotoMakeup.setOnClickListener(this);
        	//TY zhencc add for makeup end
        	tyPhotoShareMenu.setOnClickListener(this);
        	mTyPhotoCollectMenu.setOnClickListener(this);*/
        }
        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT);
        lp.addRule(RelativeLayout.CENTER_HORIZONTAL);
        lp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        galleryRoot.addView(mTyPhotoBottomMenuLayout, lp);
        return true;
    }
    
    private void cleanupPhotoBottomMenu() {
        if (mTyPhotoBottomMenuLayout == null) return;
        
        RelativeLayout galleryRoot = (RelativeLayout) mActivity.getGalleryAssignView(mAppBridge != null ? R.id.content : R.id.gallery_root);
        if (galleryRoot == null) return;
        
        galleryRoot.removeView(mTyPhotoBottomMenuLayout);
        mTyPhotoBottomMenuLayout = null;
    }
    
    private void showPhotoBottomMenu() {
        if (mTyPhotoBottomMenuLayout == null && !setupPhotoBottomMenu()) return;
        if (mTyPhotoBottomMenuLayout.getVisibility() == View.VISIBLE) return;
        mTyPhotoBottomMenuLayout.startAnimation(AnimationUtils.loadAnimation(mActivity.getAndroidContext(), R.anim.ty_bottommenu_up));
        mTyPhotoBottomMenuLayout.setVisibility(View.VISIBLE);
        updateMenuOperations();
    }
    
    private void hidePhotoBottomMenu(boolean bAnim) {
        if (mTyPhotoBottomMenuLayout == null) return;
        if (mTyPhotoBottomMenuLayout.getVisibility() == View.GONE) return;
        if (bAnim){
            mTyPhotoBottomMenuLayout.startAnimation(AnimationUtils.loadAnimation(mActivity.getAndroidContext(), R.anim.ty_bottommenu_down));
        }
        mTyPhotoBottomMenuLayout.setVisibility(View.GONE);
    }
    //TY zhencc end for New Design Gallery

    @Override
    protected void onDestroy() {
    //TIANYU hanpengzhe 20151214 add for statusbar icon color  begin        
        /*if(mActivity.getResources().getConfiguration().orientation==Configuration.ORIENTATION_PORTRAIT)
        ((Activity)mActivity.getAndroidContext()).getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);*/
        if(mActivity.getResources().getConfiguration().orientation==Configuration.ORIENTATION_PORTRAIT){
            ((Activity)mActivity.getAndroidContext()).getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
           // ((Activity)mActivity.getAndroidContext()).getWindow().setStatusBarIconDark(true);
        }
    //TIANYU hanpengzhe 20151214 add for statusbar icon color  end
        if (mAppBridge != null) {
            mAppBridge.setServer(null);
            mScreenNailItem.setScreenNail(null);
            mAppBridge.detachScreenNail();
            mAppBridge = null;
            mScreenNailSet = null;
            mScreenNailItem = null;
        }
        mActivity.getGLRoot().setOrientationSource(null);
        if (mBottomControls != null) mBottomControls.cleanup();

        cleanupPhotoBottomMenu();
        // Remove all pending messages.
        mHandler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    private class MyDetailsSource implements DetailsSource {

        @Override
        public MediaDetails getDetails() {
            return mModel.getMediaItem(0).getDetails();
        }

        @Override
        public int size() {
            return mMediaSet != null ? mMediaSet.getMediaItemCount() : 1;
        }

        @Override
        public int setIndex() {
            return mModel.getCurrentIndex();
        }
    }

    @Override
    public void refreshBottomControlsWhenReady() {
        if (mBottomControls == null) {
            return;
        }
        MediaObject currentPhoto = mCurrentPhoto;
        if (currentPhoto == null) {
            mHandler.obtainMessage(MSG_REFRESH_BOTTOM_CONTROLS, 0, 0, currentPhoto).sendToTarget();
        } else {
            currentPhoto.getPanoramaSupport(mRefreshBottomControlsCallback);
        }
    }

    private void updatePanoramaUI(boolean isPanorama360) {
        Menu menu = mActionBar.getMenu();

        // it could be null if onCreateActionBar has not been called yet
        if (menu == null) {
            return;
        }

        MenuExecutor.updateMenuForPanorama(menu, isPanorama360, isPanorama360);

        if (isPanorama360) {
            MenuItem item = menu.findItem(R.id.action_share);
            if (item != null) {
                item.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
                item.setTitle(mActivity.getResources().getString(R.string.share_as_photo));
            }
        } else if ((mCurrentPhoto.getSupportedOperations() & MediaObject.SUPPORT_SHARE) != 0) {
            MenuItem item = menu.findItem(R.id.action_share);
            if (item != null) {
                item.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
                item.setTitle(mActivity.getResources().getString(R.string.share));
            }
        }
    }

    @Override
    public void onUndoBarVisibilityChanged(boolean visible) {
        refreshBottomControlsWhenReady();
    }

    @Override
    public boolean onShareTargetSelected(ShareActionProvider source, Intent intent) {
        final long timestampMillis = mCurrentPhoto.getDateInMs();
        final String mediaType = getMediaTypeString(mCurrentPhoto);
        UsageStatistics.onEvent(UsageStatistics.COMPONENT_GALLERY,
                UsageStatistics.ACTION_SHARE,
                mediaType,
                        timestampMillis > 0
                        ? System.currentTimeMillis() - timestampMillis
                        : -1);
        return false;
    }

    private static String getMediaTypeString(MediaItem item) {
        if (item.getMediaType() == MediaObject.MEDIA_TYPE_VIDEO) {
            return "Video";
        } else if (item.getMediaType() == MediaObject.MEDIA_TYPE_IMAGE) {
            return "Photo";
        } else {
            return "Unknown:" + item.getMediaType();
        }
    }

    private static void viewAnimateGif(Activity activity, Uri uri) {
        Intent intent = new Intent(ViewGifImage.VIEW_GIF_ACTION);
        intent.setDataAndType(uri, MediaItem.MIME_TYPE_GIF);
        activity.startActivity(intent);
    }

    //yuxin add begin for  New Design Gallery
    private class PreparePhotoFallback implements OnGLIdleListener {
        private static final long TIMEOUT = 200;
        private PhotoFallbackEffect mPhotoFallback = new PhotoFallbackEffect();
        private ConditionVariable mResultReady = new ConditionVariable(false);
        private int mRetryCount = 10;

        public synchronized PhotoFallbackEffect get() {
            if (mResultReady.block(TIMEOUT)){
                return mPhotoFallback;
            }
            --mRetryCount;
            return null;
        }
        
        public int getRetryCount(){
            return mRetryCount;
        }

        @Override
        public boolean onGLIdle(GLCanvas canvas, boolean renderRequested) {
            try {
                mPhotoFallback = mPhotoView.buildFallbackEffect(mRootPane, canvas);
            } catch (RuntimeException e){
                mRetryCount = 0;
                mPhotoFallback = null;
                Log.e(TAG, "PreparePhotoFallback onGLIdle RuntimeException:" + e);
            }
            mResultReady.open();
            return false;
        }
    }

    private void preparePhotoFallbackView() {
        if (isFinishing()) {
            GLRoot root = mActivity.getGLRoot();
            PreparePhotoFallback task = new PreparePhotoFallback();
            PhotoFallbackEffect anim = null;
            
            root.unlockRenderThread();
            try {
                root.addOnGLIdleListener(task);
                anim = task.get();
            } finally {
                root.lockRenderThread();
            }
            
            while (anim == null && task.getRetryCount() > 0) {
                root.unlockRenderThread();
                try {
                    root.addOnGLIdleListener(null);
                    anim = task.get();
                } finally {
                    root.lockRenderThread();
                }
            }
            if (anim != null){
                mActivity.getTransitionStore().put(
                    AlbumPage.KEY_RESUME_ANIMATION, anim);
            }
        }
    }

    private OnPhotoChanegeListener mOnPhotoChanegeListener;
    public static interface OnPhotoChanegeListener {
        public void onPhotoChanege(int index);
    }
    public void setOnPhotoChanegeListener(OnPhotoChanegeListener listener){
        mOnPhotoChanegeListener = listener;
    }
    //yuxin add end for  New Design Gallery
}
