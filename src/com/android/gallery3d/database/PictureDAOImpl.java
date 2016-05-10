package com.android.gallery3d.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.provider.MediaStore;
import android.provider.MediaStore.Images.ImageColumns;
import android.util.Log;

import com.android.gallery3d.datatask.UploadAsyncTask;
import com.android.gallery3d.util.MediaSetUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by taoxj on 16-4-27.
 */
public class PictureDAOImpl implements PictureDAO {
    private static final String TAG = PictureDAOImpl.class.getSimpleName();

     protected static final String DB_NAME = "picture.db";
    private static PictureDatabaseHelper  databaseHelper = null;
    private Context mContext;
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private final String mWhereClause ;
    private final String mOrderClause;
    public PictureDAOImpl(Context mContext) {
        this.mContext = mContext;
        instanceDatabaseHelper(mContext);
        mWhereClause = ImageColumns.BUCKET_ID + " = ?" + " and " +  ImageColumns.DATE_TAKEN + " > ?" ;
        mOrderClause = ImageColumns.DATE_TAKEN + ", "
                + ImageColumns._ID;
      /*  if (isImage) {
            mWhereClause = MediaStore.Images.ImageColumns.BUCKET_ID + " = ?";
            mOrderClause = ImageColumns.DATE_TAKEN + " DESC, "
                    + ImageColumns._ID + " DESC";
         } else {
            mWhereClause = MediaStore.Video.VideoColumns.BUCKET_ID + " = ?";
            mOrderClause = VideoColumns.DATE_TAKEN + " DESC, "
                    + VideoColumns._ID + " DESC";
        }*/
    }


    @Override
    public void addPicture(PictureDetail picture) {
        //local picture insert
        if(picture.getPictureId() == null){
            String path =  picture.getPath();
            Cursor cursor = databaseHelper.getReadableDatabase().query(Pictures.TABLE,null,"path = ?",new String[]{path},null,null,null);
            if(cursor.moveToNext()){
                //already exists,update
                ContentValues values = new ContentValues();
                values.put(Pictures.URI, picture.getUri().toString());
                 databaseHelper.getWritableDatabase().update(Pictures.TABLE, values, "path = ?", new String[]{path});
                Log.i("koala", "upload cloud data");
                return;
            }
            //new picture from camera
            ContentValues values = new ContentValues();
            values.put(Pictures.TAKEPICTURETIME,picture.getTakePictureTime() + "");
            values.put(Pictures.URI,picture.getUri().toString());
            values.put(Pictures.PATH,path);
            values.put(Pictures.TYPE, picture.getType());
             databaseHelper.getWritableDatabase().insert(Pictures.TABLE, null, values);
            Log.i("koala", "insert local data");

        }else{//cloud picture insert
            ContentValues values = new ContentValues();
            values.put(Pictures.TAKEPICTURETIME,picture.getTakePictureTime() + "");
            values.put(Pictures.PICTUREID,picture.getPictureId());
            values.put(Pictures.USERPHOTOURL,picture.getUserPhotoUrl());
            values.put(Pictures.URL,picture.getUrl());
            values.put(Pictures.PATH,picture.getPath());
            values.put(Pictures.TYPE, picture.getType());
             databaseHelper.getWritableDatabase().insert(Pictures.TABLE, null, values);
            Log.i("koala","insert cloud data");
        }
    }

    @Override
    public void deletePicture(String pictureId) {
           databaseHelper.getWritableDatabase().delete(Pictures.TABLE,"pictureId = ?",new String[]{pictureId});
    }

    @Override
    public List<PictureDetail> query() {
        return null;
    }

    public long  queryLastTime(Uri pictureUri ){
        String path = null;
        Cursor cursor =  databaseHelper.getReadableDatabase().query(Pictures.TABLE,null,null,null,null,null,"id desc");
        while(cursor.moveToNext()) {
            if(cursor.getString(cursor.getColumnIndex(Pictures.URI)) != null){
                path = cursor.getString(cursor.getColumnIndex(Pictures.PATH));
                break;
            }
        }
        if(path != null){
             cursor = mContext.getContentResolver().query(
                    pictureUri, null, ImageColumns.BUCKET_ID + " = ?" + " and " + MediaStore.MediaColumns.DATA + " = ?",
                    new String[]{String.valueOf(MediaSetUtils.CAMERA_BUCKET_ID),path},
                    null);
            if(cursor.moveToNext()){
                long strLastTime = cursor.getLong(cursor.getColumnIndex(ImageColumns.DATE_TAKEN));
                cursor.close();
                return strLastTime;
            }
        }
        return 0;
    }

    public SQLiteOpenHelper instanceDatabaseHelper(Context context) {
        databaseHelper =  new PictureDatabaseHelper(context, DB_NAME);
        return databaseHelper;
    }

    public synchronized void updatePictures(Uri pictureUri){
        long  lastTime = queryLastTime(pictureUri);
        Log.i("koala","lastTime = "  + lastTime);
        Cursor cursor = mContext.getContentResolver().query(
                pictureUri, null, mWhereClause,
                new String[]{String.valueOf(MediaSetUtils.CAMERA_BUCKET_ID),lastTime + ""},
                mOrderClause);
        if (cursor == null) {
            Log.w(TAG, "query fail: " + pictureUri);
        }else{
            Log.i("koala","cursor count = " + cursor.getCount());
        }
        try {
            PictureDetail picture = null;
            while (cursor.moveToNext()) {
                int id = cursor.getInt(0);  // _id must be in the first column
               Uri uri = pictureUri.buildUpon().appendPath(Long.toString(id)).build();
                long time = cursor.getLong(cursor.getColumnIndex(ImageColumns.DATE_TAKEN));
                String path  = cursor.getString(cursor.getColumnIndex(MediaStore.MediaColumns.DATA));
                String type = cursor.getString(cursor.getColumnIndex(ImageColumns.MIME_TYPE));
            //    if(sdf.format(date).equals(strLastTime)){continue;}
                picture = new PictureDetail();
                picture.setTakePictureTime(time);
                picture.setUri(uri);
                picture.setPath(path);
                picture.setType(type.replace("image/", ""));
                addPicture(picture);
             }
        } finally {
            cursor.close();
        }
       }

    public PictureDetail queryByPictureId(String  pid){
        PictureDetail pictureDetail = new PictureDetail();
        Cursor cursor =  databaseHelper.getReadableDatabase().query(Pictures.TABLE,null,Pictures.PICTUREID + " = ?",new String[]{pid},null,null,null);
        if(cursor.moveToNext()){
            if(cursor.getString(cursor.getColumnIndex(Pictures.URI)) != null){
                pictureDetail.setUri(Uri.parse(cursor.getString(cursor.getColumnIndex(Pictures.URI))));
            }
            if(cursor.getString(cursor.getColumnIndex(Pictures.URL)) != null){
                pictureDetail.setUrl(cursor.getString(cursor.getColumnIndex(Pictures.URL)));
            }
            pictureDetail.setType(cursor.getString(cursor.getColumnIndex(Pictures.TYPE)));
            pictureDetail.setId(cursor.getInt(cursor.getColumnIndex(Pictures._ID)));
            pictureDetail.setPictureId(pid);
            return pictureDetail;
        }
        return null;
    }

    @Override
    public List<PictureDetail> queryLocalPic() {
        List<PictureDetail> picList = new ArrayList<PictureDetail>();
        Cursor cursor =  databaseHelper.getReadableDatabase().query(Pictures.TABLE,null,Pictures.PICTUREID + " = ?",new String[]{""},null,null,null);
        while(cursor.moveToNext()){
            PictureDetail pic = new PictureDetail();
            String uri = cursor.getString(cursor.getColumnIndex(Pictures.URI));
            long time =  Long.parseLong(cursor.getString(cursor.getColumnIndex(Pictures.TAKEPICTURETIME)));
           String type= cursor.getString(cursor.getColumnIndex(Pictures.TYPE));
           int id = cursor.getInt(cursor.getColumnIndex(Pictures._ID));
            pic.setType(type);
            pic.setUri(Uri.parse(uri));
            pic.setTakePictureTime(time);
            pic.setId(id);

            picList.add(pic);
        }
        if(cursor != null){
            cursor.close();
        }
        return picList;
    }

    @Override
    public void updateLocalPicture(PictureDetail pic) {
         ContentValues values = new ContentValues();
        values.put(Pictures.PICTUREID,pic.getPictureId());
        if(pic.getUrl() != null){
            values.put(Pictures.URL,pic.getUrl());
            values.put(Pictures.USERPHOTOURL, pic.getUserPhotoUrl());
        }
        databaseHelper.getWritableDatabase().update(Pictures.TABLE,values,"id = ?",new String[]{pic.getId()+""});
    }

    @Override
    public PictureDetail queryByUri(String uri) {
        PictureDetail pic = new PictureDetail();
        Cursor cursor = databaseHelper.getReadableDatabase().query(Pictures.TABLE,new String[]{Pictures.PICTUREID},"uri = ?",new String[]{uri},null,null,null);
        if(cursor.moveToNext()){
               pic.setPictureId(cursor.getString(cursor.getColumnIndex(Pictures.PICTUREID)));
        }
        if(cursor != null){
            cursor.close();
        }
        return pic;
    }

    @Override
    public void modifyDeleteStatus(String pictureId) {
        Cursor cursor = databaseHelper.getReadableDatabase().query(Pictures.TABLE,new String[]{Pictures.PICTUREID},"pictureId = ?",new String[]{pictureId},null,null,null);
        if(cursor.moveToNext()){
            ContentValues values= new ContentValues();
            values.put(Pictures.ISDELETED,"1");
            databaseHelper.getWritableDatabase().update(Pictures.TABLE, values, "pictureId = ?", new String[]{pictureId});
        }
        if(cursor != null){
            cursor.close();
        }
    }

    @Override
    public List<PictureDetail> queryDeletedItems() {
        List<PictureDetail>  list = new ArrayList<PictureDetail>();
        Cursor cursor = databaseHelper.getReadableDatabase().query(Pictures.TABLE,new String[]{Pictures.PICTUREID},"isDeleted = ?",new String[]{"1"},null,null,null);
        while(cursor.moveToNext()){
            PictureDetail pic = new PictureDetail();
            String picId = cursor.getString(cursor.getColumnIndex(Pictures.PICTUREID));
            pic.setPictureId(picId);
            list.add(pic);
        }
        if(cursor != null){
            cursor.close();
        }
        return list;
    }

    @Override
    public List<String> queryAllPictureIds() {
        List<String>  list = new ArrayList<String>();
        Cursor cursor = databaseHelper.getReadableDatabase().query(Pictures.TABLE,new String[]{Pictures.PICTUREID},"pictureId is not null and url is not null",null,null,null,null);
        while(cursor.moveToNext()){
             String picId = cursor.getString(cursor.getColumnIndex(Pictures.PICTUREID));
             list.add(picId);
        }
        if(cursor != null){
            cursor.close();
        }
        return list;
    }

    @Override
    public void deletePictureByUri(String uri) {
        databaseHelper.getWritableDatabase().delete(Pictures.TABLE,"uri = ?",new String[]{uri});

    }

    @Override
    public PictureDetail queryByPath(String path) {
        PictureDetail pic = new PictureDetail();
        Cursor cursor = databaseHelper.getReadableDatabase().query(Pictures.TABLE,null,"path = ?",new String[]{path},null,null,null);
        if(cursor.moveToNext()){
            pic.setTakePictureTime(Long.parseLong(cursor.getString(cursor.getColumnIndex(Pictures.TAKEPICTURETIME))));
            String userPhotourl = cursor.getString(cursor.getColumnIndex(Pictures.USERPHOTOURL));
            String url = cursor.getString(cursor.getColumnIndex(Pictures.URL));
            pic.setUserPhotoUrl(userPhotourl == null ? "":userPhotourl);
            pic.setUrl(url != null ? url : "");
        }
        if(cursor != null){
            cursor.close();
        }
        return pic;
    }
}
