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

package com.android.gallery3d.ui;

import android.app.Activity;
import android.app.Dialog;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore.Images.ImageColumns;
import android.os.Handler;
import android.os.Message;
import android.os.Environment;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Adapter;
import android.widget.EditText;
import android.widget.Button;
import android.widget.Toast;
import android.text.TextUtils;
import android.text.Editable;
import android.text.TextWatcher;
import android.support.v4.print.PrintHelper;

import com.android.gallery3d.app.GalleryContext;
import com.android.gallery3d.app.TyAddToAlbumListAdapter;
import com.android.gallery3d.app.TyAddToAlbumSetListDataLoader;
import com.android.gallery3d.ui.TyAddToAlbumSetListSlidingWindow.AlbumSetEntry;
import com.android.gallery3d.data.MediaSet;
import com.android.gallery3d.data.LocalAlbum;
import com.android.gallery3d.data.BucketHelper;
import com.android.gallery3d.data.DataManager;
import com.android.gallery3d.data.MediaItem;
import com.android.gallery3d.data.MediaObject;
import com.android.gallery3d.data.Path;
import com.android.gallery3d.filtershow.crop.CropActivity;
import com.android.gallery3d.util.Future;
import com.android.gallery3d.util.GalleryUtils;
import com.android.gallery3d.util.ThreadPool.Job;
import com.android.gallery3d.util.ThreadPool.JobContext;
import com.android.gallery3d.common.Utils;
import com.android.gallery3d.R;

import java.util.List;
import java.io.File;
import java.util.ArrayList;

public class MenuExecutor {
    private static final String TAG = "MenuExecutor";

    private static final int MSG_TASK_COMPLETE = 1;
    private static final int MSG_TASK_UPDATE = 2;
    private static final int MSG_TASK_START = 3;
    private static final int MSG_DO_SHARE = 4;

    public static final int EXECUTION_RESULT_SUCCESS = 1;
    public static final int EXECUTION_RESULT_FAIL = 2;
    public static final int EXECUTION_RESULT_CANCEL = 3;
    //TY zhencc add for New Design Gallery
    public static final int EXECUTION_RESULT_FAIL_EXISTS = 4;
    public static final int EXECUTION_RESULT_FAIL_PART_EXISTS = 5;
    //TY zhencc end for New Design Gallery
    //TY wb034 add for New Design Gallery
    public static final int EXECUTION_ADD_ALBUM_SUCCESS = 6;
    public static final int EXECUTION_DELETE_ALBUM_SUCCESS = 7;
    public static final int EXECUTION_DELETE_ALBUM_FAILED = 8; 
    public static boolean mIsFromCollectAlbum = false;
    public static long mOptNum = 0;
    //TY wb034 end for New Design Gallery
    private ProgressDialog mDialog;
    private Future<?> mTask;
    // wait the operation to finish when we want to stop it.
    private boolean mWaitOnStop;
    private boolean mPaused;

    private final GalleryContext mActivity;
    private final SelectionManager mSelectionManager;
    private final Handler mHandler;
    //TY zhencc add for New Design Gallery
    //private AlertDialog mAlbumListAlertDialog;//TY wb034 20150123 delete for thgallery
    private Dialog mAlbumListAlertDialog;//TY wb034 20150123 modify for thgallery
    public static boolean mAddFromAlbumPage = false;//TY wb034 20150127 add for tygallery
    private int mPosition;
    private int mAddToAlbumResult;
    private int mCollectToAlbumResult;
    //TY zhencc end for New Design Gallery
    //wangqin add for New Design Gallery
    private String mDestiPath;
    private boolean mValueFromSelectMode;
    //wangqin add for New Design Gallery 20141212 end
    private static final int DATA_CACHE_SIZE = 256;
    int mStart = 0;
    int mEnd = 0;
  //TY wb034 20150203 add begin for tygallery
    private TyAddToAlbumListAdapter mTyAddToAlbumListAdapter;
    private TyAddToAlbumSetListDataLoader mTyAlbumSetListDataLoader;
    private TyAddToAlbumSetListRenderer mTyAlbumSetListRenderer;
   //TY wb034 20150203 add end for tygallery
    private static ProgressDialog createProgressDialog(
            Context context, int titleId, int progressMax) {
        ProgressDialog dialog = new ProgressDialog(context);
        dialog.setTitle(titleId);
        dialog.setMax(progressMax);
        dialog.setCancelable(false);
        dialog.setIndeterminate(false);
        if (progressMax > 1) {
            dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        }
        return dialog;
    }

    public interface ProgressListener {
        public void onConfirmDialogShown();
        public void onConfirmDialogDismissed(boolean confirmed);
        public void onProgressStart();
        public void onProgressUpdate(int index);
        public void onProgressComplete(int result);
    }

    public MenuExecutor(
            GalleryContext activity, SelectionManager selectionManager) {
        mActivity = Utils.checkNotNull(activity);
        mSelectionManager = Utils.checkNotNull(selectionManager);
        mHandler = new SynchronizedHandler(mActivity.getGLRoot()) {
            @Override
            public void handleMessage(Message message) {
                switch (message.what) {
                    case MSG_TASK_START: {
                        if (message.obj != null) {
                            ProgressListener listener = (ProgressListener) message.obj;
                            listener.onProgressStart();
                        }
                        break;
                    }
                    case MSG_TASK_COMPLETE: {
                        stopTaskAndDismissDialog();
                        if (message.obj != null) {
                            ProgressListener listener = (ProgressListener) message.obj;
                            listener.onProgressComplete(message.arg1);
                        }
                        mSelectionManager.leaveSelectionMode();
                        break;
                    }
                    case MSG_TASK_UPDATE: {
                        if (mDialog != null && !mPaused) mDialog.setProgress(message.arg1);
                        if (message.obj != null) {
                            ProgressListener listener = (ProgressListener) message.obj;
                            listener.onProgressUpdate(message.arg1);
                        }
                        break;
                    }
                    case MSG_DO_SHARE: {
                        ((Activity)mActivity.getAndroidContext()).startActivity((Intent) message.obj);
                        break;
                    }
                }
            }
        };
    }
	//wangqin add for new feature gallery begin
	public void setAddPicDirPath(String path){
		mDestiPath = path;
	} 
	public void setValuefromSelectMode(boolean value){
		mValueFromSelectMode = value;
	}
	//wangqin add for new feature gallery end

    private void stopTaskAndDismissDialog() {
        if (mTask != null) {
            if (!mWaitOnStop) mTask.cancel();
            if (mDialog != null && mDialog.isShowing()) mDialog.dismiss();
            mDialog = null;
            mTask = null;
        }
    }

    public void resume() {
        mPaused = false;
        if (mDialog != null) mDialog.show();
    }

    public void pause() {
        mPaused = true;
        if (mDialog != null && mDialog.isShowing()) mDialog.hide();
    }

    public void destroy() {
        stopTaskAndDismissDialog();
    }

    private void onProgressUpdate(int index, ProgressListener listener) {
        mHandler.sendMessage(
                mHandler.obtainMessage(MSG_TASK_UPDATE, index, 0, listener));
    }

    private void onProgressStart(ProgressListener listener) {
        mHandler.sendMessage(mHandler.obtainMessage(MSG_TASK_START, listener));
    }

    private void onProgressComplete(int result, ProgressListener listener) {
        mHandler.sendMessage(mHandler.obtainMessage(MSG_TASK_COMPLETE, result, 0, listener));
    }

    public static void updateMenuOperation(Menu menu, int supported) {
        boolean supportDelete = (supported & MediaObject.SUPPORT_DELETE) != 0;
        boolean supportRotate = (supported & MediaObject.SUPPORT_ROTATE) != 0;
        boolean supportCrop = (supported & MediaObject.SUPPORT_CROP) != 0;
        boolean supportTrim = (supported & MediaObject.SUPPORT_TRIM) != 0;
        boolean supportMute = (supported & MediaObject.SUPPORT_MUTE) != 0;
        boolean supportShare = (supported & MediaObject.SUPPORT_SHARE) != 0;
        boolean supportSetAs = (supported & MediaObject.SUPPORT_SETAS) != 0;
        boolean supportShowOnMap = (supported & MediaObject.SUPPORT_SHOW_ON_MAP) != 0;
        boolean supportCache = (supported & MediaObject.SUPPORT_CACHE) != 0;
        boolean supportEdit = (supported & MediaObject.SUPPORT_EDIT) != 0;
        boolean supportInfo = (supported & MediaObject.SUPPORT_INFO) != 0;
        boolean supportPrint = (supported & MediaObject.SUPPORT_PRINT) != 0;
        supportPrint &= PrintHelper.systemSupportsPrint();

        setMenuItemVisible(menu, R.id.action_delete, supportDelete);
        setMenuItemVisible(menu, R.id.action_rotate_ccw, supportRotate);
        setMenuItemVisible(menu, R.id.action_rotate_cw, supportRotate);
        setMenuItemVisible(menu, R.id.action_crop, supportCrop);
        setMenuItemVisible(menu, R.id.action_trim, supportTrim);
        setMenuItemVisible(menu, R.id.action_mute, supportMute);
        // Hide panorama until call to updateMenuForPanorama corrects it
        setMenuItemVisible(menu, R.id.action_share_panorama, false);
        setMenuItemVisible(menu, R.id.action_share, supportShare);
        setMenuItemVisible(menu, R.id.action_setas, supportSetAs);
        setMenuItemVisible(menu, R.id.action_show_on_map, supportShowOnMap);
        setMenuItemVisible(menu, R.id.action_edit, supportEdit);
        // setMenuItemVisible(menu, R.id.action_simple_edit, supportEdit);
        setMenuItemVisible(menu, R.id.action_details, supportInfo);
/*TIANYURD:yanghao modify for CQWB00014617 start*/        
        //setMenuItemVisible(menu, R.id.print, supportPrint);
        setMenuItemVisible(menu, R.id.print, false);
/*TIANYURD:yanghao modify for CQWB00014617 end*/
    }
    
    //TY zhencc add for New Design Gallery
    public static void tyUpdateMenuOperation(Menu menu, int supported) {
        boolean supportSetAs = (supported & MediaObject.SUPPORT_SETAS) != 0;
        boolean supportInfo = (supported & MediaObject.SUPPORT_INFO) != 0;
        boolean supportAddToAlbum = (supported & MediaObject.SUPPORT_ADD_TO_ALBUM) != 0;

        setMenuItemVisible(menu, R.id.action_delete, false);
        setMenuItemVisible(menu, R.id.action_rotate_ccw, false);
        setMenuItemVisible(menu, R.id.action_rotate_cw, false);
        setMenuItemVisible(menu, R.id.action_crop, false);
        setMenuItemVisible(menu, R.id.action_trim, false);
        setMenuItemVisible(menu, R.id.action_mute, false);
        // Hide panorama until call to updateMenuForPanorama corrects it
        setMenuItemVisible(menu, R.id.action_share_panorama, false);
        setMenuItemVisible(menu, R.id.action_share, false);
        setMenuItemVisible(menu, R.id.action_setas, supportSetAs);
        setMenuItemVisible(menu, R.id.action_show_on_map, false);
        setMenuItemVisible(menu, R.id.action_edit, false);
        setMenuItemVisible(menu, R.id.action_details, supportInfo);
        setMenuItemVisible(menu, R.id.print, false);

        setMenuItemVisible(menu, R.id.ty_action_menu_collect, false);
        setMenuItemVisible(menu, R.id.ty_action_add_to_album, supportAddToAlbum);
    }
    
    private void dismissAlbumListAlertDialog(){
    	if(mAlbumListAlertDialog != null){
    		mAlbumListAlertDialog.dismiss();
    		mAlbumListAlertDialog = null;
         //TY wb034 20150203 add end for tygallery
            mTyAlbumSetListDataLoader.pause();
            mTyAlbumSetListRenderer.pause();
	     //TY wb034 20150203 add end for tygallery
    	}
    }
    
    private View getAlbumListView(int actionId, ProgressListener listener){
    	ListView listView = new ListView(((Activity)mActivity.getAndroidContext()));
    	listView.setAdapter(new AlbumAdapter(GalleryUtils.mEntriesBucketIdList));
    	listView.setOnItemClickListener(new MyAdapterListener(actionId, listener));
    	return listView;
    }
    
    private class MyAdapterListener implements AdapterView.OnItemClickListener{
    	
    	private final int mActionId;
        private final ProgressListener mListener;

        public MyAdapterListener(int actionId, ProgressListener listener) {
            mActionId = actionId;
            mListener = listener;
        }

		public void onItemClick(AdapterView<?> parent, View view, int position, 
				long id) {            
            //TY wb034 20150130 add begin for tygallery			
            //mPosition = position;
            Adapter adapter = parent.getAdapter();
            if (adapter instanceof TyAddToAlbumListAdapter){
                AlbumSetEntry entry =(AlbumSetEntry)adapter.getItem(position);
                String path = entry.album.getPath().toString();
                int bucketId =Integer.parseInt(path.substring(path.lastIndexOf("/")).substring(1));
                mPosition = GalleryUtils.mEntriesBucketIdList.indexOf(bucketId);
            }
            dismissAlbumListAlertDialog();
            //TY wb034 20150130 add end for tygallery
            onMenuClicked(mActionId, mListener);
         }
    }
    
    private class AlbumAdapter extends BaseAdapter{
    	
    	private final List<Integer> mItems;
    	
    	public AlbumAdapter(List<Integer> list) {
            mItems = list;
        }
    	
		@Override
		public int getCount() {
			return mItems.size();
		}

		@Override
		public Object getItem(int position) {
			return mItems.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			final ViewHolder holder;

			if (null == convertView) {
				convertView = View.inflate(((Activity)mActivity.getAndroidContext()), R.layout.ty_share_list_item,
						null);
				holder = new ViewHolder();
				holder.name = (TextView) convertView
						.findViewById(R.id.ty_item_name);

				convertView.setTag(holder);
			} else {
				holder = (ViewHolder) convertView.getTag();
			}

			int bucketId = mItems.get(position);
			String name = LocalAlbum.getLocalizedName(
					mActivity.getResources(), 
					bucketId, 
					GalleryUtils.mEntriesBucketMap.get(bucketId));
			holder.name.setText(name);
			
			return convertView;
		}
		
		class ViewHolder {
			TextView name;
		}
    	
    }
    //TY zhencc end for New Design Gallery

    public static void updateMenuForPanorama(Menu menu, boolean shareAsPanorama360,
            boolean disablePanorama360Options) {
        setMenuItemVisible(menu, R.id.action_share_panorama, shareAsPanorama360);
        if (disablePanorama360Options) {
            setMenuItemVisible(menu, R.id.action_rotate_ccw, false);
            setMenuItemVisible(menu, R.id.action_rotate_cw, false);
        }
    }

    private static void setMenuItemVisible(Menu menu, int itemId, boolean visible) {
        MenuItem item = menu.findItem(itemId);
        if (item != null) item.setVisible(visible);
    }

    private Path getSingleSelectedPath() {
        ArrayList<Path> ids = mSelectionManager.getSelected(true);
        Utils.assertTrue(ids.size() == 1);
        return ids.get(0);
    }

    private Intent getIntentBySingleSelectedPath(String action) {
        DataManager manager = mActivity.getDataManager();
        Path path = getSingleSelectedPath();
        String mimeType = getMimeType(manager.getMediaType(path));
        return new Intent(action).setDataAndType(manager.getContentUri(path), mimeType);
    }

    private void onMenuClicked(int action, ProgressListener listener) {
        onMenuClicked(action, listener, false, true);
    }

    public void onMenuClicked(int action, ProgressListener listener,
            boolean waitOnStop, boolean showDialog) {
        int title;
        switch (action) {
            case R.id.action_select_all:
                if (mSelectionManager.inSelectAllMode()) {
                    mSelectionManager.deSelectAll();
                } else {
                    mSelectionManager.selectAll();
                }
                return;
            case R.id.action_crop: {
                Intent intent = getIntentBySingleSelectedPath(CropActivity.CROP_ACTION);
                ((Activity)mActivity.getAndroidContext()).startActivity(intent);
                return;
            }
            case R.id.action_edit: {
                Intent intent = getIntentBySingleSelectedPath(Intent.ACTION_EDIT)
                        .setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                ((Activity)mActivity.getAndroidContext()).startActivity(Intent.createChooser(intent, null));
                return;
            }
            case R.id.action_setas: {
                Intent intent = getIntentBySingleSelectedPath(Intent.ACTION_ATTACH_DATA)
                        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                intent.putExtra("mimeType", intent.getType());
                Activity activity = ((Activity)mActivity.getAndroidContext());
                activity.startActivity(Intent.createChooser(
                        intent, activity.getString(R.string.set_as)));
                return;
            }
            case R.id.action_delete:
                title = R.string.delete;
                break;
            case R.id.action_rotate_cw:
                title = R.string.rotate_right;
                break;
            case R.id.action_rotate_ccw:
                title = R.string.rotate_left;
                break;
            case R.id.action_show_on_map:
                title = R.string.show_on_map;
                break;
            //TY zhencc add for New Design Gallery
            case R.id.ty_selection_all_btn:
                if (mSelectionManager.inSelectAllMode()) {
                    mSelectionManager.deSelectAll();
                } else {
                    mSelectionManager.selectAll();
                }
                return;
            case R.id.ty_action_delete:
            	title = R.string.delete;
                 break;
            case R.id.ty_action_share:
            	return;
            case R.id.ty_action_add_to_album:
                GalleryUtils.mEntriesAlbumFilePathListCopy.clear();
                GalleryUtils.mEntriesAlbumFilePathListCopy.addAll(GalleryUtils.mEntriesAlbumFilePathList);
                //wangqin add by 20141212
                if((mValueFromSelectMode == true)&&(!mAddFromAlbumPage)){
                    CreateNewDir(mDestiPath);
                }
                //TY wb034 20150127 add begin for tygallery
                if(mAddFromAlbumPage){
                    int id =Integer.parseInt(mDestiPath.substring(mDestiPath.lastIndexOf("/")).substring(1));
                    mPosition = GalleryUtils.mEntriesBucketIdList.indexOf(id);
                }
               //TY wb034 20150127 add end for tygallery
                //wangqin add by 20141212
            	title = R.string.ty_add_to_album;
            	break;
            case R.id.ty_action_menu_collect:
            	title = R.string.ty_collect;
            	break;
            //TY zhencc end for New Design Gallery
            /*Tywangqin add for gallery2 begin*/
            case R.id.ty_action_hide:
            	title = R.string.ty_hide;
            	break;
			case R.id.ty_action_show:
            	title = R.string.ty_showhidden;
            	break;
            /*Tywangqin add for gallery2 end*/
            default:
                return;
        }
        startAction(action, title, listener, waitOnStop, showDialog);
    }

    private class ConfirmDialogListener implements OnClickListener, OnCancelListener {
        private final int mActionId;
        private final ProgressListener mListener;

        public ConfirmDialogListener(int actionId, ProgressListener listener) {
            mActionId = actionId;
            mListener = listener;
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            if (which == DialogInterface.BUTTON_POSITIVE) {
                if (mListener != null) {
                    mListener.onConfirmDialogDismissed(true);
                }
                onMenuClicked(mActionId, mListener);
            } else {
                if (mListener != null) {
                    mListener.onConfirmDialogDismissed(false);
                }
            }
        }

        @Override
        public void onCancel(DialogInterface dialog) {
            if (mListener != null) {
                mListener.onConfirmDialogDismissed(false);
            }
        }
    }

    public void onMenuClicked(MenuItem menuItem, String confirmMsg,
            final ProgressListener listener) {
        final int action = menuItem.getItemId();

        if (confirmMsg != null) {
            if (listener != null) listener.onConfirmDialogShown();
            ConfirmDialogListener cdl = new ConfirmDialogListener(action, listener);
            if(action == R.id.ty_action_hide){
               new AlertDialog.Builder(mActivity.getAndroidContext())
                  .setTitle(mActivity.getResources().getString(R.string.hide_hint)) 
                  .setMessage(confirmMsg)
                  .setOnCancelListener(cdl)
                  .setPositiveButton(R.string.ok, cdl)
                  .setNegativeButton(R.string.cancel, cdl)
                  .create().show();               	
            }else{            	
               new AlertDialog.Builder(mActivity.getAndroidContext())
                 //taoxj remove title
                // .setTitle(mActivity.getResources().getString(R.string.delete)) 
                 .setMessage(confirmMsg)
                 .setOnCancelListener(cdl)
                 .setPositiveButton(R.string.ok, cdl)
                 .setNegativeButton(R.string.cancel, cdl)
                 .create().show();   
            }
        }else if (mValueFromSelectMode == false
				&& action == R.id.ty_action_add_to_album) {
			mAlbumListAlertDialog = new Dialog(mActivity.getAndroidContext(),
					R.style.TyAddToAlbum);
			mAlbumListAlertDialog.setContentView(R.layout.ty_add_to_album);
		    ListView listView = (ListView) mAlbumListAlertDialog
					.findViewById(R.id.add);
		    Button ic_back = (Button)mAlbumListAlertDialog.findViewById(R.id.ic_back);
		    TextView create_new_album = (TextView)mAlbumListAlertDialog.findViewById(R.id.create_new_album);
		    ic_back.setOnClickListener(new View.OnClickListener() {		
				@Override
				public void onClick(View arg0) {
					dismissAlbumListAlertDialog();		
				}
			});
		    create_new_album.setOnClickListener(new View.OnClickListener() {			
				@Override
				public void onClick(View arg0) {
					newDir(action,listener);	
				}
			});
		        mTyAlbumSetListRenderer = new TyAddToAlbumSetListRenderer(
	                mActivity, mSelectionManager, listView);
			    String mediaPath = mActivity.getDataManager().getTopSetPath(DataManager.INCLUDE_ALL);
	                MediaSet mMediaSet = mActivity.getDataManager().getMediaSet(mediaPath);
	                mTyAlbumSetListDataLoader = new TyAddToAlbumSetListDataLoader(
	                mActivity, mMediaSet, DATA_CACHE_SIZE);
			        mTyAddToAlbumListAdapter = new TyAddToAlbumListAdapter(
					mActivity.getAndroidContext(), mSelectionManager);
			listView.setAdapter(mTyAddToAlbumListAdapter);		
	                mTyAlbumSetListRenderer.setModel(mTyAlbumSetListDataLoader);	       
			listView.setOnItemClickListener(new MyAdapterListener(action,
					listener));
	                mTyAlbumSetListDataLoader.resume();
	                mTyAlbumSetListRenderer.resume();
			mAlbumListAlertDialog.show();
		}else {
            onMenuClicked(action, listener);
        }   
    }
    private void newDir(final int action,final ProgressListener listener) {
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
                        setValuefromSelectMode(true);
                        setAddPicDirPath(newPath);
                        dismissAlbumListAlertDialog();
                        onMenuClicked(action, listener);
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
            e.printStackTrace();
        }
        return;
    }
    
    public void startAction(int action, int title, ProgressListener listener) {
        startAction(action, title, listener, false, true);
    }

    public void startAction(int action, int title, ProgressListener listener,
            boolean waitOnStop, boolean showDialog) {
        ArrayList<Path> ids = mSelectionManager.getSelected(false);
        stopTaskAndDismissDialog();
        Activity activity = ((Activity)mActivity.getAndroidContext());
        if (showDialog ) {
            mDialog = createProgressDialog(activity, title, ids.size());
            if(ids.size() > 1){
                mDialog.show();
            }else if(ids.size() == 1){
                int inItemSize = mSelectionManager.getSelected(true).size();
                if(inItemSize >= 200){
                    mDialog.show();
                }
            }
        } else {
            mDialog = null;
        }
        MediaOperation operation = new MediaOperation(action, ids, listener);
        mTask = mActivity.getBatchServiceThreadPoolIfAvailable().submit(operation, null);
        mWaitOnStop = waitOnStop;
    }

    public void startSingleItemAction(int action, Path targetPath) {
        ArrayList<Path> ids = new ArrayList<Path>(1);
        ids.add(targetPath);
        mDialog = null;
        MediaOperation operation = new MediaOperation(action, ids, null);
        mTask = mActivity.getBatchServiceThreadPoolIfAvailable().submit(operation, null);
        mWaitOnStop = false;
    }

    public static String getMimeType(int type) {
        switch (type) {
            case MediaObject.MEDIA_TYPE_IMAGE :
                return GalleryUtils.MIME_TYPE_IMAGE;
            case MediaObject.MEDIA_TYPE_VIDEO :
                return GalleryUtils.MIME_TYPE_VIDEO;
            default: return GalleryUtils.MIME_TYPE_ALL;
        }
    }

    private boolean execute(
            DataManager manager, JobContext jc, int cmd, Path path) {
        boolean result = true;
        Log.v(TAG, "Execute cmd: " + cmd + " for " + path);
        long startTime = System.currentTimeMillis();

        switch (cmd) {
            case R.id.action_delete:
                manager.delete(path);
                break;
            case R.id.action_rotate_cw:
                manager.rotate(path, 90);
                break;
            case R.id.action_rotate_ccw:
                manager.rotate(path, -90);
                break;
            case R.id.action_toggle_full_caching: {
                MediaObject obj = manager.getMediaObject(path);
                int cacheFlag = obj.getCacheFlag();
                if (cacheFlag == MediaObject.CACHE_FLAG_FULL) {
                    cacheFlag = MediaObject.CACHE_FLAG_SCREENNAIL;
                } else {
                    cacheFlag = MediaObject.CACHE_FLAG_FULL;
                }
                obj.cache(cacheFlag);
                break;
            }
            case R.id.action_show_on_map: {
                MediaItem item = (MediaItem) manager.getMediaObject(path);
                double latlng[] = new double[2];
                item.getLatLong(latlng);
                if (GalleryUtils.isValidLocation(latlng[0], latlng[1])) {
                    GalleryUtils.showOnMap(mActivity.getAndroidContext(), latlng[0], latlng[1]);
                }
                break;
            }
            //TY zhencc add for New Design Gallery
            case R.id.ty_action_delete:
                //Ty wb034 20150123 add begin for tygallery
                if((!path.getSuffix().equals(String.valueOf(manager.mCollectBucketId)))&& MenuExecutor.mIsFromCollectAlbum){
                     manager.Collect(false, path);
                }else{
                     manager.delete(path);            		
                }
                //Ty wb034 20150123 add end for tygallery            	
                //manager.delete(path);
                break;
            case R.id.ty_action_add_to_album:
				//wangqin add for newfeature gallery2 begin
                //if(mValueFromSelectMode == true){TY wb034 20150127 modify this to next for tygallery
				if((mValueFromSelectMode == true)&&!mAddFromAlbumPage){
					mAddToAlbumResult = new TyMoveToAlbumManager(mActivity, path).tyMoveToAlbumDestination(mDestiPath);
				}else{
					mAddToAlbumResult = new TyMoveToAlbumManager(mActivity, path).tyMoveToAlbum(mPosition);
				}
				//wangqin add for newfeature gallery2 end
            	result = mAddToAlbumResult == EXECUTION_RESULT_SUCCESS;
                break;
            case R.id.ty_action_menu_collect:
            	mCollectToAlbumResult = new TyMoveToAlbumManager(mActivity, path).tyMoveToAlbum(TyMoveToAlbumManager.COLLECT);
            	result = mCollectToAlbumResult == EXECUTION_RESULT_SUCCESS;
            	break;
            //TY zhencc end for New Design Gallery
            /*Tywangqin add for gallery2 begin*/
            case R.id.ty_action_hide:
            	manager.hide(true, path);
                break;
            case R.id.ty_action_show:
                manager.hide(false, path);
                break;
            /*Tywangqin add for gallery2 end*/
            default:
                throw new AssertionError();
        }
        Log.v(TAG, "It takes " + (System.currentTimeMillis() - startTime) +
                " ms to execute cmd for " + path);
        return result;
    }
    
    //Ty wb034 20150123 add begin for tygallery
    private boolean CreateNewDir(String Path){
        if(Path == null) return false;
        final File f_new = new File(Path);
        if (!f_new.exists()) {
            try {
                f_new.mkdir();
            } catch (Exception e) {
                Log.e(TAG, "failed to CreateNewDir operation e=" + e);
                return false;
            }
        }
        return true;
    }
    //Ty wb034 20150123 add end for tygallery
    
    private class MediaOperation implements Job<Void> {
        private final ArrayList<Path> mItems;
        private final int mOperation;
        private final ProgressListener mListener;

        public MediaOperation(int operation, ArrayList<Path> items,
                ProgressListener listener) {
            mOperation = operation;
            mItems = items;
            mListener = listener;
        }

        @Override
        public Void run(JobContext jc) {
            int index = 0;
            DataManager manager = mActivity.getDataManager();
            int result = EXECUTION_RESULT_SUCCESS;
            //TY zhencc add for New Design Gallery
            int  addToAlbumStatusCount = 0;
            //TY zhencc end for New Design Gallery
            try {
                manager.setNeedChange(false);//Ty wb034 20150123 add for tygaller
                mOptNum++;
                onProgressStart(mListener);
                for (Path id : mItems) {
                    if (jc.isCancelled()) {
                        result = EXECUTION_RESULT_CANCEL;
                        break;
                    }
                    if (!execute(manager, jc, mOperation, id)) {
                    	result = EXECUTION_RESULT_FAIL;
                    	//TY zhencc modify for New Design Gallery
                    	if(mOperation == R.id.ty_action_add_to_album){
                    		if(mAddToAlbumResult == EXECUTION_RESULT_FAIL_EXISTS){
                    			addToAlbumStatusCount++;
                    		}
                    		result = mAddToAlbumResult;
                    	}
                    	if(mOperation == R.id.ty_action_menu_collect){
                    		result = mCollectToAlbumResult;
                    	}
                    	//TY zhencc end for New Design Gallery
                    }
                    onProgressUpdate(index++, mListener);
                }
            } catch (Throwable th) {
                Log.e(TAG, "failed to execute operation " + mOperation
                        + " : " + th);
            }finally {
                //TY zhencc add for New Design Gallery
                if(addToAlbumStatusCount != 0){
                	if(mItems.size() == addToAlbumStatusCount){
                		Log.d(TAG,"result set EXISTS");
                		result = EXECUTION_RESULT_FAIL_EXISTS;
                	}else{
                		Log.d(TAG,"result set PART EXISTS");
                		result = EXECUTION_RESULT_FAIL_PART_EXISTS;
                	}
                }
                //TY zhencc end for New Design Gallery
               if((result == EXECUTION_RESULT_SUCCESS)&&mValueFromSelectMode&&!mAddFromAlbumPage)//TY wb034 20150126 add this line for tygallery
               result = EXECUTION_ADD_ALBUM_SUCCESS;//TY wb034 20150126 add this line for tygallery
               onProgressComplete(result, mListener);
               
               manager.setNeedChange(true);//Ty wb034 20150123 add for tygallery
             //Ty wb034 20150123 add begin for tygallery
               MenuExecutor.mIsFromCollectAlbum =false;
               //Ty wb034 20150123 add end for tygallery
            }
            return null;
        }
    } 
    
    //TY zhencc add for New Design Gallery
    private interface ContentResolverQueryCallback {
		void onCursorResult(Cursor cursor);
	 }
    
    private File getSourceDirectory(Context context, Uri sourceUri) {
		final File[] dir = new File[1];
		querySource(context, sourceUri, new String[] { ImageColumns.DATA },
				new ContentResolverQueryCallback() {

					@Override
					public void onCursorResult(Cursor cursor) {
						dir[0] = new File(cursor.getString(0));
					}
				});
		return dir[0];
	 }

	private void querySource(Context context, Uri sourceUri,
			String[] projection, ContentResolverQueryCallback callback) {
		ContentResolver contentResolver = context.getContentResolver();
		Cursor cursor = null;
		try {
			cursor = contentResolver.query(sourceUri, projection, null, null,
					null);
			if ((cursor != null) && cursor.moveToNext()) {
				callback.onCursorResult(cursor);
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
	 }
    //TY zhencc end for New Design Gallery
}
