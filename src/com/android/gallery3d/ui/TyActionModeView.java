/*
 * @author zhencc
 * @details New Design Gallery
 *
 */

package com.android.gallery3d.ui;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.nfc.NfcAdapter;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.gallery3d.R;
import com.android.gallery3d.app.GalleryContext;
import com.android.gallery3d.common.ApiHelper;
import com.android.gallery3d.common.Utils;
import com.android.gallery3d.data.DataManager;
import com.android.gallery3d.data.MediaObject;
import com.android.gallery3d.data.MediaObject.PanoramaSupportCallback;
import com.android.gallery3d.data.Path;
import com.android.gallery3d.ui.MenuExecutor.ProgressListener;
import com.android.gallery3d.util.Future;
import com.android.gallery3d.util.GalleryUtils;
import com.android.gallery3d.util.ThreadPool.Job;
import com.android.gallery3d.util.ThreadPool.JobContext;

import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.graphics.drawable.Drawable;

import com.android.gallery3d.data.MediaItem;
import com.android.gallery3d.app.GalleryActionBar;
import com.android.gallery3d.app.TyGalleryBottomBar;

public class TyActionModeView {

    @SuppressWarnings("unused")
    private static final String TAG = "TyActionModeView";

    private static final int MAX_SELECTED_ITEMS_FOR_SHARE_INTENT = 300;
    private static final int MAX_SELECTED_ITEMS_FOR_PANORAMA_SHARE_INTENT = 10;

    private static final int SUPPORT_MULTIPLE_MASK = MediaObject.SUPPORT_DELETE
            | MediaObject.SUPPORT_ROTATE | MediaObject.SUPPORT_SHARE
            | MediaObject.SUPPORT_CACHE;

    private final GalleryContext mGalleryContext;
    private final MenuExecutor mMenuExecutor;
    private final SelectionManager mSelectionManager;
    private final NfcAdapter mNfcAdapter;
    private Menu mMenu;
    private Future<?> mMenuTask;
    private final Handler mMainHandler;
    /*Tywangqin add for gallery2 begin*/
    public String mAddPicPath;
    public boolean mdataFromSelectedMode;
    /*Tywangqin add for gallery2 end*/
    //TY zhencc add for New Design Gallery
    private Intent mTyShareIntent;
    private AlertDialog mShareListAlertDialog;
    private List<AppInfo> mListAppInfo;
    //TY zhencc end for New Design Gallery
    private GalleryActionBar mActionBar;
    private TyGalleryBottomBar mTyGalleryBottomBar;
    private boolean mIsAlbum;

    private static class GetAllPanoramaSupports implements PanoramaSupportCallback {
        private int mNumInfoRequired;
        private JobContext mJobContext;
        public boolean mAllPanoramas = true;
        public boolean mAllPanorama360 = true;
        public boolean mHasPanorama360 = false;
        private Object mLock = new Object();

        public GetAllPanoramaSupports(ArrayList<MediaObject> mediaObjects, JobContext jc) {
            mJobContext = jc;
            mNumInfoRequired = mediaObjects.size();
            for (MediaObject mediaObject : mediaObjects) {
                mediaObject.getPanoramaSupport(this);
            }
        }

        @Override
        public void panoramaInfoAvailable(MediaObject mediaObject, boolean isPanorama,
                boolean isPanorama360) {
            synchronized (mLock) {
                mNumInfoRequired--;
                mAllPanoramas = isPanorama && mAllPanoramas;
                mAllPanorama360 = isPanorama360 && mAllPanorama360;
                mHasPanorama360 = mHasPanorama360 || isPanorama360;
                if (mNumInfoRequired == 0 || mJobContext.isCancelled()) {
                    mLock.notifyAll();
                }
            }
        }

        public void waitForPanoramaSupport() {
            synchronized (mLock) {
                while (mNumInfoRequired != 0 && !mJobContext.isCancelled()) {
                    try {
                        mLock.wait();
                    } catch (InterruptedException e) {
                        // May be a cancelled job context
                    }
                }
            }
        }
    }

    public TyActionModeView(
            GalleryContext galleryContext, SelectionManager selectionManager, boolean isAlbum) {
        mGalleryContext = Utils.checkNotNull(galleryContext);
        mSelectionManager = Utils.checkNotNull(selectionManager);
        mMenuExecutor = new MenuExecutor(mGalleryContext, selectionManager);
        mMainHandler = new Handler(mGalleryContext.getMainLooper());
        mNfcAdapter = NfcAdapter.getDefaultAdapter(mGalleryContext.getAndroidContext());
        mActionBar = mGalleryContext.getGalleryActionBar();
        mTyGalleryBottomBar = mGalleryContext.getGalleryBottomBar();
        mIsAlbum = isAlbum;
    }

    //TY zhencc add for New Design Gallery
    public void startActionMode(MenuItem item) {
    	setupMenuView(item);
        updateSelectionMenu();
    }
    //TY zhencc end for New Design Gallery
	//wangqin add for new feature gallery begin
	public void setAddAlbumDirPath(String path){
		mAddPicPath = path;
	} 
	public void setDataSelectMode(boolean data){
		mdataFromSelectedMode = data;
	}
	//wangqin add for new feature gallery end
    public void finishActionMode() {
        //Ty zhencc add for New Design Gallery
        dismissShareListAlertDialog();
        cleanMenuView();
        //TY zhencc end for New Design Gallery
    }
    
    private void setupMenuView(MenuItem item){
        mTyGalleryBottomBar.setOnClickListener(new TyGalleryBottomBar.OnBottomBarListener(){
            @Override
            public void onBottomBarClick(View v) {
                if (v.getId() == R.id.ty_selection_all_btn){
                    mMenuExecutor.onMenuClicked(R.id.ty_selection_all_btn, null, false, true);
                    updateSelectAllBtnText();
                }else{
                	MenuItem tag = (MenuItem)v.getTag();
                	if(tag != null){
                		tyMenuItemClicked(tag);
                	}
                }
            }
        });
        switch (item.getItemId()) {
            case R.id.ty_action_delete: 
                mTyGalleryBottomBar.enableSelectMode(TyGalleryBottomBar.TySelectKind.TyDeleteKind, item);
                break;
            case R.id.ty_action_share: 
                mTyGalleryBottomBar.enableSelectMode(TyGalleryBottomBar.TySelectKind.TyShareKind, item);
                break;
            case R.id.ty_action_add_to_album: 
                mTyGalleryBottomBar.enableSelectMode(TyGalleryBottomBar.TySelectKind.TyAddKind, item);
                break;
            case R.id.ty_action_hide:
                mTyGalleryBottomBar.enableSelectMode(TyGalleryBottomBar.TySelectKind.TyHideKind, item);
                break;
            case R.id.ty_action_show:
                mTyGalleryBottomBar.enableSelectMode(TyGalleryBottomBar.TySelectKind.TyShowKind, item);
                break;
            default:
                mTyGalleryBottomBar.enableSelectMode(TyGalleryBottomBar.TySelectKind.TyNullKind, item);
                break;
        }
        updateSelectAllBtnText();
    }
    
    private void updateSelectAllBtnText(){
    	if (mSelectionManager.inSelectAllMode()) {
            mTyGalleryBottomBar.setSelectAllInSelectMode(false);
        } else {
            mTyGalleryBottomBar.setSelectAllInSelectMode(true);
        }
    }
    
    private void setMenuBtnEnabled(boolean enabled){
        mTyGalleryBottomBar.setKindEnableInSelectMode(enabled);
    }
    
    private void cleanMenuView() {
        mTyGalleryBottomBar.setOnClickListener(null);
    }
    
    private void dismissShareListAlertDialog(){
    	if(mShareListAlertDialog != null){
    		mShareListAlertDialog.dismiss();
    		mShareListAlertDialog = null;
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
    	PackageManager pManager = mGalleryContext.getAndroidContext().getPackageManager(); 
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
    	ListView listView = new ListView(mGalleryContext.getAndroidContext());
    	mListAppInfo = getShareApps();
    	if(mListAppInfo == null){
    		return null;
    	}
    	listView.setAdapter(new ShareAdapter());
    	listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, 
					long id) {
				Intent shareIntent = mTyShareIntent;  
                AppInfo appInfo = (AppInfo) mListAppInfo.get(position);
                shareIntent.setComponent(new ComponentName(appInfo.mPackageName ,appInfo.mLauncherClassName));  
                ((Activity)mGalleryContext.getAndroidContext()).startActivity(shareIntent); 
                //TY zhencc 20150724 add for PROD103923109 begin
                ((Activity)mGalleryContext.getAndroidContext()).overridePendingTransition(0,0);
                //TY zhencc 20150724 add for PROD103923109 end
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
				convertView = View.inflate(mGalleryContext.getAndroidContext(), R.layout.ty_share_list_item,
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
    //TY zhencc end for New Design Gallery
    
    public void setTitle(String title) {
        mActionBar.setTitle(title);
    }

    //TY zhencc add for New Design Gallery
    private boolean tyMenuItemClicked(MenuItem item) {
        GLRoot root = mGalleryContext.getGLRoot();
        root.lockRenderThread();
        try {
            ProgressListener listener = null;
            String confirmMsg = null;
            int action = item.getItemId();
            if (action == R.id.ty_action_delete) {
                confirmMsg = mGalleryContext.getResources().getQuantityString(
                    R.plurals.delete_selection, mSelectionManager.getSelectedCount());
                listener = new TyDeleteCompleteListener(mGalleryContext);
            } else if(action == R.id.ty_action_share){
                //TY liuyuchuan modify  for PROD103709484  mSelectionManager.getSelectedCount()-->mSelectionManager.getSelected(true).size()
                if(mSelectionManager.getSelected(true).size() > MAX_SELECTED_ITEMS_FOR_SHARE_INTENT){
                    Toast.makeText(mGalleryContext.getAndroidContext(), String.format(
                            mGalleryContext.getAndroidContext().getString(R.string.ty_max_share_items), 
                            MAX_SELECTED_ITEMS_FOR_SHARE_INTENT), 
                            Toast.LENGTH_SHORT)
                        .show();
                     return true;
                }
            	
                if(mShareListAlertDialog == null){
                    mShareListAlertDialog = new AlertDialog.Builder(mGalleryContext.getAndroidContext())
                        .setTitle(mGalleryContext.getResources().getString(R.string.ty_share_title))
                        .create();
                }
                mShareListAlertDialog.setView(getShareListView());
                mShareListAlertDialog.show();
            }else if(action == R.id.ty_action_add_to_album){
	          /*Tywangqin add for gallery2 begin*/
				mMenuExecutor.setAddPicDirPath(mAddPicPath);
				mMenuExecutor.setValuefromSelectMode(mdataFromSelectedMode);
            	listener = new TyAddToAlbumCompleteListener(mGalleryContext);
            }else if(action == R.id.ty_action_hide){
                confirmMsg = mGalleryContext.getResources().getString(
                        R.string.confirm_hide);//TY wb034 20150120 add for tygallery
                listener = new TyHideCompleteListener(mGalleryContext, true);
            }else if(action == R.id.ty_action_show){        
                listener = new TyHideCompleteListener(mGalleryContext, false);
            }
            mMenuExecutor.onMenuClicked(item, confirmMsg, listener);
        } finally {
            root.unlockRenderThread();
        }
        return true;
    }
    //TY zhencc end for New Design Gallery

    public void updateSelectionMenu() {
        // update title
        int count = mSelectionManager.getSelectedCount();
        int id = mIsAlbum ? R.plurals.ty_number_of_albums_selected : R.plurals.ty_number_of_items_selected;
        String format = mGalleryContext.getResources().getQuantityString(
                id, count, count);
        setTitle(format);
    }

    private ArrayList<MediaObject> getSelectedMediaObjects(JobContext jc) {
        ArrayList<Path> unexpandedPaths = mSelectionManager.getSelected(false);
        if (unexpandedPaths.isEmpty()) {
            // This happens when starting selection mode from overflow menu
            // (instead of long press a media object)
            return null;
        }
        ArrayList<MediaObject> selected = new ArrayList<MediaObject>();
        DataManager manager = mGalleryContext.getDataManager();
        for (Path path : unexpandedPaths) {
            if (jc.isCancelled()) {
                return null;
            }
            selected.add(manager.getMediaObject(path));
        }

        return selected;
    }
    // Menu options are determined by selection set itself.
    // We cannot expand it because MenuExecuter executes it based on
    // the selection set instead of the expanded result.
    // e.g. LocalImage can be rotated but collections of them (LocalAlbum) can't.
    private int computeMenuOptions(ArrayList<MediaObject> selected) {
        //TY zhencc add for New Design Gallery
        if (selected == null)
            return 0;
        //TY zhencc end for New Design Gallery
        int operation = MediaObject.SUPPORT_ALL;
        int type = 0;
        for (MediaObject mediaObject: selected) {
            int support = mediaObject.getSupportedOperations();
            type |= mediaObject.getMediaType();
            operation &= support;
        }

        switch (selected.size()) {
            case 1:
                final String mimeType = MenuExecutor.getMimeType(type);
                if (!GalleryUtils.isEditorAvailable(mGalleryContext.getAndroidContext(), mimeType)) {
                    operation &= ~MediaObject.SUPPORT_EDIT;
                }
                break;
            default:
                operation &= SUPPORT_MULTIPLE_MASK;
        }

        return operation;
    }

    @TargetApi(ApiHelper.VERSION_CODES.JELLY_BEAN)
    private void setNfcBeamPushUris(Uri[] uris) {
        if (mNfcAdapter != null && ApiHelper.HAS_SET_BEAM_PUSH_URIS) {
            mNfcAdapter.setBeamPushUrisCallback(null, (Activity)mGalleryContext.getAndroidContext());
            mNfcAdapter.setBeamPushUris(uris, (Activity)mGalleryContext.getAndroidContext());
        }
    }

    // Share intent needs to expand the selection set so we can get URI of
    // each media item
    private Intent computePanoramaSharingIntent(JobContext jc, int maxItems) {
        ArrayList<Path> expandedPaths = mSelectionManager.getSelected(true, maxItems);
        if (expandedPaths == null || expandedPaths.size() == 0) {
            return new Intent();
        }
        final ArrayList<Uri> uris = new ArrayList<Uri>();
        DataManager manager = mGalleryContext.getDataManager();
        final Intent intent = new Intent();
        for (Path path : expandedPaths) {
            if (jc.isCancelled()) return null;
            uris.add(manager.getContentUri(path));
        }

        final int size = uris.size();
        if (size > 0) {
            if (size > 1) {
                intent.setAction(Intent.ACTION_SEND_MULTIPLE);
                intent.setType(GalleryUtils.MIME_TYPE_PANORAMA360);
                intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
            } else {
                intent.setAction(Intent.ACTION_SEND);
                intent.setType(GalleryUtils.MIME_TYPE_PANORAMA360);
                intent.putExtra(Intent.EXTRA_STREAM, uris.get(0));
            }
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }

        return intent;
    }

    private Intent computeSharingIntent(JobContext jc, int maxItems) {
        ArrayList<Path> expandedPaths = mSelectionManager.getSelected(true, maxItems);
        if (expandedPaths == null || expandedPaths.size() == 0) {
            setNfcBeamPushUris(null);
            return new Intent();
        }
        final ArrayList<Uri> uris = new ArrayList<Uri>();
        DataManager manager = mGalleryContext.getDataManager();
        int type = 0;
        final Intent intent = new Intent();
        for (Path path : expandedPaths) {
            if (jc.isCancelled()) return null;
            int support = manager.getSupportedOperations(path);
            type |= manager.getMediaType(path);

            if ((support & MediaObject.SUPPORT_SHARE) != 0) {
                uris.add(manager.getContentUri(path));
            }
        }

        final int size = uris.size();
        if (size > 0) {
            final String mimeType = MenuExecutor.getMimeType(type);
            if (size > 1) {
                intent.setAction(Intent.ACTION_SEND_MULTIPLE).setType(mimeType);
                intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
            } else {
                intent.setAction(Intent.ACTION_SEND).setType(mimeType);
                intent.putExtra(Intent.EXTRA_STREAM, uris.get(0));
            }
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            setNfcBeamPushUris(uris.toArray(new Uri[uris.size()]));
        } else {
            setNfcBeamPushUris(null);
        }

        return intent;
    }

    public void updateSupportedOperation(Path path, boolean selected) {
        // TODO: We need to improve the performance
        updateSupportedOperation();
    }

    public void updateSupportedOperation() {
        // Interrupt previous unfinished task, mMenuTask is only accessed in main thread
        if (mMenuTask != null) mMenuTask.cancel();

        updateSelectionMenu();

        // Generate sharing intent and update supported operations in the background
        // The task can take a long time and be canceled in the mean time.
        mMenuTask = mGalleryContext.getThreadPool().submit(new Job<Void>() {
            @Override
            public Void run(final JobContext jc) {
                // Pass1: Deal with unexpanded media object list for menu operation.
                ArrayList<MediaObject> selected = getSelectedMediaObjects(jc);
                final int operation = computeMenuOptions(selected);
                if (jc.isCancelled()) {
                    return null;
                }
                //TY zhencc modify for New Design Gallery
                //int numSelected = selected.size();
                int numSelected = 0;
                if(selected != null){
                    numSelected = selected.size();
                }
                final int tyNumSelected = numSelected;
                //TY zhencc end for New Design Gallery

                // Pass2: Deal with expanded media object list for sharing operation. 
                final Intent share_intent =
                        computeSharingIntent(jc, MAX_SELECTED_ITEMS_FOR_SHARE_INTENT);
                if (jc.isCancelled()) {
                    return null;
                }
                mMainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mMenuTask = null;
                        if (jc.isCancelled()) return;
                        //TY zhencc add for New Design Gallery
                        if(tyNumSelected > 0){
                            setMenuBtnEnabled(true);
                        }else{
                            setMenuBtnEnabled(false);
                        }
                        updateSelectAllBtnText();
                        mTyShareIntent = share_intent;
                        //TY zhencc end for New Design Gallery
                    }
                });
                return null;
            }
        });
    }

    public void pause() {
        if (mMenuTask != null) {
            mMenuTask.cancel();
            mMenuTask = null;
        }
        mMenuExecutor.pause();
    }

    public void destroy() {
        mMenuExecutor.destroy();
    }

    public void resume() {
        if (mSelectionManager.inSelectionMode()) updateSupportedOperation();
        mMenuExecutor.resume();
    }
}
