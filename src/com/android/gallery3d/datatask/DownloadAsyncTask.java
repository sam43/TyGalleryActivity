package com.android.gallery3d.datatask;

import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

import com.android.gallery3d.api.ApiRequestListener;
import com.android.gallery3d.app.GalleryAppImpl;
import com.android.gallery3d.data.DataManager;
import com.android.gallery3d.data.Path;
import com.android.gallery3d.database.PictureDAO;
import com.android.gallery3d.database.PictureDAOImpl;
import com.android.gallery3d.database.PictureDetail;
import com.android.gallery3d.parser.PictureListParser;
import com.android.gallery3d.parser.PictureListResp;
import com.android.gallery3d.volley.GalleryApi;
import com.android.gallery3d.volley.InstagramPlusException;
import com.android.gallery3d.volley.InstagramRestClient;
import com.android.gallery3d.volley.RequestParams;
import com.android.volley.Request;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by taoxj on 16-5-3.
 */
public class DownloadAsyncTask extends AsyncTask<Void,Void,Void> {
    private PictureDAO dao;
    public static final int PAGE_SIZE = 30;
    public  int STARTINDEX = 0;
    public  boolean isEnd = false;
    public ArrayList<String> cloudPictureIdSet = new ArrayList<String>();

    public DownloadAsyncTask(){
           this.dao = new PictureDAOImpl(GalleryAppImpl.getContext());
    }
    @Override
    protected Void doInBackground(Void... params) {
            //check local data to see if it is already exist
             getPicList();
             return null;
          }

    @Override
    protected void onPostExecute(Void aVoid) {
        new DeleteAsyncTask().execute();
    }

    public void getPicList(){
        RequestParams rp = new RequestParams("{'telephone':'"+GalleryApi.tel+"','startIndex':'" + STARTINDEX + "','pageSize':'" + PAGE_SIZE + "'}");
        InstagramRestClient.getInstance().postData(Request.Method.POST, GalleryApi.dowload, rp, new PictureListParser(), new ApiRequestListener<PictureListResp>() {

            @Override
            public void onPreExecute() {
                // TODO Auto-generated method stub
            }

            @Override
            public void onComplete(int statusCode, PictureListResp value) {
                // TODO Auto-generated method stub
                List<PictureDetail> list = value.getPicturesList();
                if(list.size() < PAGE_SIZE){
                            isEnd = true;
                }
                STARTINDEX += PAGE_SIZE;
                //dowload picture to local camera directory
                new DownloadPictureAsyncTask().execute(list);
            }

            @Override
            public void onException(InstagramPlusException e) {
                // TODO Auto-generated method stub

            }
        });
    }


    class DownloadPictureAsyncTask extends AsyncTask<List<PictureDetail>,Void,Void>{

        @Override
        protected Void doInBackground(List<PictureDetail>... params) {
            List<PictureDetail> list = params[0];
            for (int i = 0; i < list.size(); i++) {
                PictureDetail p = list.get(i);
                cloudPictureIdSet.add(p.getPictureId());
                PictureDetail pictureDetail =  dao.queryByPictureId(p.getPictureId());
                if (pictureDetail != null) {
                    if(pictureDetail.getUrl() == null){
                        Log.i("koala","update url and userphotourl" + p.getUrl());
                        pictureDetail.setUrl(p.getUrl());
                        pictureDetail.setUserPhotoUrl(p.getUserPhotoUrl());
                        dao.updateLocalPicture(pictureDetail);
                    }
                    continue;
                } else {
                    //not exist
                    int  lastStr = p.getUrl().lastIndexOf("/");
                    String fileName =  p.getUrl().substring(lastStr + 1);
                    File fDir = new File(Environment.getExternalStorageDirectory() + "/DCIM/Camera");
                    Log.i("koala","fdir path = " + fDir.getPath());
                    if(!fDir.exists()){
                        Log.i("koala","camera dir is not exsit");
                        if(!fDir.mkdir()){

                            Log.i("koala","make dir failed");
                        }
                    }
                    String fPath = fDir.getPath() + "/" + fileName;
                    try {
                        URL url = new URL(p.getUrl());
                        InputStream is = url.openStream();
                        File f = new File(fPath);
                        FileOutputStream os = new FileOutputStream(f);
                        byte[] buff = new byte[1024];
                        int hasRead = 0;
                        while ((hasRead = is.read(buff)) > 0) {
                            os.write(buff, 0, hasRead);
                        }
                        is.close();
                        os.close();

                        p.setPath(f.getPath());
                        //insert into database
                        dao.addPicture(p);
                        //send broascast
                        Uri picture_uri = Uri.fromFile(f);
                        GalleryAppImpl.getContext().sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, picture_uri));

                    } catch (Exception e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }
            if(isEnd){
                List<String> localPictureIds = dao.queryAllPictureIds();
                Log.i("koala","localPictureIds size " + localPictureIds.size());
                if( localPictureIds.removeAll(cloudPictureIdSet)){
                    Log.i("koala","localPictureIds to be deleted size " + localPictureIds.size());
                    for(int i = 0;i<localPictureIds.size();i++){
                        String picId = localPictureIds.get(i);
                        //local remove
                        DataManager dm = DataManager.from(GalleryAppImpl.getContext());
                        PictureDetail pd = dao.queryByPictureId(picId);
                        Path path = dm.findPathByUri(pd.getUri(),"image/" + pd.getType());
                         if (path != null) {
                            if(dm.getMediaObject(path) != null){
                                dm.getMediaObject(path).delete();
                                //delete from database
                                dao.deletePicture(picId);
                            }
                        }
                    }
                }
            }else{
                getPicList();
            }
            return null;
        }
    }
}

