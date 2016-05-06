package com.android.gallery3d.ui;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore.Images;
import android.provider.MediaStore.Images.ImageColumns;
import android.provider.MediaStore.Video;
import android.provider.MediaStore.Video.VideoColumns;
import com.android.gallery3d.R;

import com.android.gallery3d.app.GalleryContext;
import com.android.gallery3d.data.DataManager;
import com.android.gallery3d.data.MediaObject;
import com.android.gallery3d.data.Path;
import com.android.gallery3d.util.GalleryUtils;
import com.android.gallery3d.util.MediaSetUtils;
//TY WB034 20150306 add begin for tygallery
import android.content.ContentUris;
//TY wb034 20150306 add end for tygallery
/**
 * @author zhencc
 * @details New Design Gallery
 *
 */
public class TyMoveToAlbumManager {

	private static final String TAG = "Gallery2/TyMoveToAlbumManager";
	
	public static final int COLLECT = -1;
	private final GalleryContext mActivity;
	private Path mPath;

	public TyMoveToAlbumManager(GalleryContext activity, Path path) {
		mActivity = activity;
        mPath = path;
    }

    public int tyMoveToAlbum(int position) {
        DataManager manager = mActivity.getDataManager();
        File sourceFile = getSourceDirectory(mActivity.getAndroidContext(),
                manager.getContentUri(mPath));
        String name = sourceFile.getName();
        int type = manager.getMediaType(mPath);
        File destinationFileDir = null;
        //TY wb034 20150128 add begin for tygallery			
        int id = manager.mCollectBucketId;
        //TY wb034 20150128 add end  for tygallery	
        if (position == COLLECT){
            //TY wb034 20150128 delete begin for tygallery			
             /*destinationFileDir = new File(MediaSetUtils.COLLECT_ALBUM_PATH);
                   if (!destinationFileDir.exists()) {
                   boolean mkdir = destinationFileDir.mkdir();
               }*/
            //TY wb034 20150128 delete end for tygallery
        //TY wb034 20150128 add begin for tygallery					
            if(manager.isAleadyCollect(mPath)){
                return manager.Collect(false, mPath)?
                MenuExecutor.EXECUTION_DELETE_ALBUM_SUCCESS:MenuExecutor.EXECUTION_DELETE_ALBUM_FAILED;				
            }else{
                return manager.Collect(true, mPath)?
                MenuExecutor.EXECUTION_RESULT_SUCCESS:MenuExecutor.EXECUTION_RESULT_FAIL;						
            }
           /*destinationFileDir = new File(MediaSetUtils.COLLECT_ALBUM_PATH);
            if (!destinationFileDir.exists()) {
               boolean mkdir = destinationFileDir.mkdir();
            }*/
       }else if(MenuExecutor.mIsFromCollectAlbum||((!MenuExecutor.mAddFromAlbumPage)&&(id==GalleryUtils.mEntriesBucketIdList.get(position)))){
            if(!manager.isAleadyCollect(mPath)){
                 if(manager.Collect(true, mPath)){
                    return MenuExecutor.EXECUTION_RESULT_SUCCESS;
                 }else{
                    return MenuExecutor.EXECUTION_RESULT_FAIL;
                 }					
            }else{
                return MenuExecutor.EXECUTION_RESULT_SUCCESS;				
            }				
       } else {
			destinationFileDir = new File(
                 //GalleryUtils.mEntriesAlbumFilePathList.get(position)
                 GalleryUtils.mEntriesAlbumFilePathListCopy.get(position).toString());
         //TY wb034 20150128 add end for tygallery	
		}
		File destinationFile = new File(destinationFileDir, name);
		int copyResult = copyFile(sourceFile.toString(),
             destinationFile.toString());
		int insertResult = MenuExecutor.EXECUTION_RESULT_SUCCESS;
		if (copyResult == MenuExecutor.EXECUTION_RESULT_SUCCESS) {
			Log.d(TAG,"copy file successful");
			Uri sourceUri = manager.getContentUri(mPath);
			Uri insertUri = insertContent(sourceUri, destinationFile, type);
			//TY wb034 20150306 add begin for tygallery
			if(manager.isAleadyCollect(mPath)){
			    manager.Collect(false, mPath);
			    Path newPath = manager.findPathByUri(insertUri, MediaObject.getTypeString(type));
			    manager.Collect(true, newPath);
			}
			//TY wb034 20150306 add end for tygallery
			if (insertUri != null) {
				int deleteReturn = deleteSourceImageContent(sourceUri);
			} else {
				insertResult = MenuExecutor.EXECUTION_RESULT_FAIL;
			}
		} else {
			insertResult = copyResult;
		}
		Log.d(TAG, "tyMoveToAlbum()-insertResult:" + insertResult);

		int result = insertResult;
		return result;
	}

	public int tyMoveToAlbumDestination(String destination) {
		DataManager manager = mActivity.getDataManager();
		File sourceFile = getSourceDirectory(mActivity.getAndroidContext(),
				manager.getContentUri(mPath));
		String name = sourceFile.getName();
		int type = manager.getMediaType(mPath);
		File destinationFileDir = null;
	   destinationFileDir = new File(destination);
		File destinationFile = new File(destinationFileDir, name);
		int copyResult = copyFile(sourceFile.toString(),
				destinationFile.toString());
		int insertResult = MenuExecutor.EXECUTION_RESULT_SUCCESS;
		if (copyResult == MenuExecutor.EXECUTION_RESULT_SUCCESS) {
			Log.d(TAG,"copy file successful");
			Uri sourceUri = manager.getContentUri(mPath);
			Uri insertUri = insertContent(sourceUri, destinationFile, type);
	         //TY wb034 20150306 add begin for tygallery
            if(manager.isAleadyCollect(mPath)){
                manager.Collect(false, mPath);
                Path newPath = manager.findPathByUri(insertUri, MediaObject.getTypeString(type));
                manager.Collect(true, newPath);
            }
            //TY wb034 20150306 add end for tygallery
			if (insertUri != null) {
				int deleteReturn = deleteSourceImageContent(sourceUri);
			} else {
				insertResult = MenuExecutor.EXECUTION_RESULT_FAIL;
			}
		} else {
			insertResult = copyResult;
		}
		int result = insertResult;
		return result;
	}

	private int copyFile(String oldPath, String newPath) {
		File newFile = new File(newPath);
		if (newFile.exists()) {
			return MenuExecutor.EXECUTION_RESULT_FAIL_EXISTS;
		}
		int result = MenuExecutor.EXECUTION_RESULT_FAIL;
		InputStream inStream = null;
		FileOutputStream fs = null;
		try {
			int byteSum = 0;
			int byteRead = 0;
			File oldFile = new File(oldPath);
			if (oldFile.exists()) {
				inStream = new FileInputStream(oldPath);
				fs = new FileOutputStream(newPath);
				byte[] buffer = new byte[1024];
				int length;
				while ((byteRead = inStream.read(buffer)) != -1) {
					byteSum += byteRead;
					fs.write(buffer, 0, byteRead);
				}
				result = MenuExecutor.EXECUTION_RESULT_SUCCESS;
			}
		} catch (Exception e1) {
			Log.d(TAG, "Copy file e1:",e1);
			result = MenuExecutor.EXECUTION_RESULT_FAIL;
		} finally {
			try {
				if (fs != null) {
					fs.flush();
					fs.close();
				}
				if (inStream != null) {
					inStream.close();
				}
			} catch (Exception e2) {
				Log.d(TAG, "Copy file e2:",e2);
				result = MenuExecutor.EXECUTION_RESULT_FAIL;
			}
			return result;
		}
	}

	private Uri insertContent(Uri sourceUri, File file, int type) {
		if (type == MediaObject.MEDIA_TYPE_VIDEO) {
			return insertVideo(sourceUri, file);
		} else if (type == MediaObject.MEDIA_TYPE_IMAGE) {
			return insertImage(sourceUri, file);
		} else {
			return null;
		}
	}

	private Uri insertVideo(Uri sourceUri, File file) {
		final ContentValues values = new ContentValues();
		values.put(Video.Media.DATE_MODIFIED, file.lastModified());
		values.put(Video.Media.DATA, file.getAbsolutePath());
		values.put(Video.Media.SIZE, file.length());

		final String[] projectionVideo = new String[] {
				VideoColumns.DISPLAY_NAME, VideoColumns.MIME_TYPE,
				VideoColumns.TITLE, VideoColumns.DATE_ADDED,
				VideoColumns.DATE_TAKEN, VideoColumns.DURATION,
				VideoColumns.RESOLUTION, VideoColumns.WIDTH,
				VideoColumns.HEIGHT, VideoColumns.LATITUDE,
				VideoColumns.LONGITUDE};
		querySource(mActivity.getAndroidContext(), sourceUri, projectionVideo,
				new ContentResolverQueryCallback() {

					@Override
					public void onCursorResult(Cursor cursor) {
						values.put(Video.Media.DISPLAY_NAME,
								cursor.getString(0));
						values.put(Video.Media.MIME_TYPE, cursor.getString(1));
						values.put(Video.Media.TITLE, cursor.getString(2));
						values.put(Video.Media.DATE_ADDED, cursor.getLong(3));
						values.put(Video.Media.DATE_TAKEN, cursor.getLong(4));
						values.put(Video.Media.DURATION, cursor.getInt(5));
						values.put(Video.Media.RESOLUTION, cursor.getString(6));
						values.put(Video.Media.WIDTH, cursor.getInt(7));
						values.put(Video.Media.HEIGHT, cursor.getInt(8));

						double latitude = cursor.getDouble(9);
						double longitude = cursor.getDouble(10);
						if ((latitude != 0f) || (longitude != 0f)) {
							values.put(Video.Media.LATITUDE, latitude);
							values.put(Video.Media.LONGITUDE, longitude);
						}

					}
				});
		return mActivity.getAndroidContext().getContentResolver().insert(
				Video.Media.EXTERNAL_CONTENT_URI, values);
	}

	private int deleteSourceImageContent(Uri sourceUri) {
		return mActivity.getAndroidContext().getContentResolver().delete(sourceUri, null, null);
	}

	private Uri insertImage(Uri sourceUri, File file) {
		final ContentValues values = new ContentValues();
		values.put(Images.Media.DATE_MODIFIED, file.lastModified());
		values.put(Images.Media.DATA, file.getAbsolutePath());
		values.put(Images.Media.SIZE, file.length());

		final String[] projectionImage = new String[] {
				ImageColumns.DISPLAY_NAME, ImageColumns.MIME_TYPE,
				ImageColumns.TITLE, ImageColumns.DATE_ADDED,
				ImageColumns.DATE_TAKEN, ImageColumns.ORIENTATION,
				ImageColumns.WIDTH, ImageColumns.HEIGHT, ImageColumns.LATITUDE,
				ImageColumns.LONGITUDE, };
		querySource(mActivity.getAndroidContext(), sourceUri, projectionImage,
				new ContentResolverQueryCallback() {

					@Override
					public void onCursorResult(Cursor cursor) {
						values.put(Images.Media.DISPLAY_NAME,
								cursor.getString(0));
						values.put(Images.Media.MIME_TYPE, cursor.getString(1));
						values.put(Images.Media.TITLE, cursor.getString(2));
						values.put(Images.Media.DATE_ADDED, cursor.getLong(3));
						values.put(Images.Media.DATE_TAKEN, cursor.getLong(4));
						values.put(Images.Media.ORIENTATION, cursor.getInt(5));
						values.put(Images.Media.WIDTH, cursor.getInt(6));
						values.put(Images.Media.HEIGHT, cursor.getInt(7));

						double latitude = cursor.getDouble(8);
						double longitude = cursor.getDouble(9);
						if ((latitude != 0f) || (longitude != 0f)) {
							values.put(Images.Media.LATITUDE, latitude);
							values.put(Images.Media.LONGITUDE, longitude);
						}
					}
				});
		return mActivity.getAndroidContext().getContentResolver().insert(
				Images.Media.EXTERNAL_CONTENT_URI, values);
	}

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

}
