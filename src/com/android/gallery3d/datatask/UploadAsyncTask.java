package com.android.gallery3d.datatask;

import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.util.Log;

import com.android.gallery3d.api.ApiRequestListener;
import com.android.gallery3d.app.GalleryAppImpl;
import com.android.gallery3d.database.PictureDAO;
import com.android.gallery3d.database.PictureDAOImpl;
import com.android.gallery3d.database.PictureDetail;
import com.android.gallery3d.parser.UploadParser;
import com.android.gallery3d.parser.UploadResp;
import com.android.gallery3d.util.BitmapUtils;
import com.android.gallery3d.util.NetworkUtils;
import com.android.gallery3d.volley.GalleryApi;
import com.android.gallery3d.volley.InstagramPlusException;
import com.android.gallery3d.volley.InstagramRestClient;
import com.android.gallery3d.volley.RequestParams;
import com.android.volley.Request;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Created by taoxj on 16-5-3.
 */
public class UploadAsyncTask extends AsyncTask<Void,Void,Void> {
    private PictureDAO dao;
    public static boolean isRunning= false;
    public UploadAsyncTask(){
        this.dao = new PictureDAOImpl(GalleryAppImpl.getContext());
    }
    public SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    @Override
    protected Void doInBackground(Void... params) {
        //上传本地照片
         if(isRunning){
            return null;
        }
             List<PictureDetail> picList  = dao.queryLocalPic();
             if(picList.size() >0){
                isRunning = true;
             }
            for(int i=0;i<picList.size();i++){
                PictureDetail pic = picList.get(i);
                int id = pic.getId();
                String jsonBody = "{";
                Bitmap photo = null;
                try {
                    photo = MediaStore.Images.Media.getBitmap(GalleryAppImpl.getContext().getContentResolver(), pic.getUri());
                    jsonBody += "'telephone':'"+GalleryApi.tel+"','picture':";
                    jsonBody += "'" + BitmapUtils.bitmapToBase64(photo)+  "'";
                    jsonBody += ",'pictureFormat':'" + pic.getType() + "','takePictureTime':'" + sdf.format(new Date(pic.getTakePictureTime())) + "'}";
                    RequestParams rp = new RequestParams(jsonBody);
                    if(i == picList.size() -1){
                        upload(rp,id,true);
                    }else{
                        upload(rp,id,false);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
         return null;
    }

    /**
     * 上传图片
     */
    public void upload(RequestParams rp,int id, final boolean isLast){
         final int pid =id;
        if(!NetworkUtils.isConnected()){
            isRunning = false;
        }
        InstagramRestClient.getInstance().postData(Request.Method.POST, GalleryApi.upload, rp, new UploadParser(), new ApiRequestListener<UploadResp>() {

            @Override
            public void onPreExecute() {
                // TODO Auto-generated method stub
            }

            @Override
            public void onComplete(int statusCode, UploadResp value) {
                if (value.isSuccess()) {
                    //upload success ,update local database
                    PictureDetail pic = new PictureDetail();
                    pic.setId(pid);
                    pic.setPictureId(value.getPictureId());
                    dao.updateLocalPicture(pic);
                    if(isLast){
                        isRunning = false;
                     }
                }

            }

            @Override
            public void onException(InstagramPlusException e) {
                // TODO Auto-gener
                isRunning = false;
             }
        });
    }
}
