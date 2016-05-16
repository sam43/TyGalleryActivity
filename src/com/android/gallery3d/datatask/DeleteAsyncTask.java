package com.android.gallery3d.datatask;

import android.os.AsyncTask;
import android.util.Log;

import com.android.gallery3d.api.ApiRequestListener;
import com.android.gallery3d.app.GalleryAppImpl;
import com.android.gallery3d.database.PictureDAO;
import com.android.gallery3d.database.PictureDAOImpl;
import com.android.gallery3d.database.PictureDetail;
import com.android.gallery3d.parser.DeleteParser;
import com.android.gallery3d.parser.DeleteResp;
import com.android.gallery3d.volley.GalleryApi;
import com.android.gallery3d.volley.InstagramPlusException;
import com.android.gallery3d.volley.InstagramRestClient;
import com.android.gallery3d.volley.RequestParams;
import com.android.volley.Request;

import java.util.List;

/**
 * Created by taoxj on 16-5-4.
 */
public class DeleteAsyncTask extends AsyncTask<Void,Void,Void> {
    private PictureDAO dao;

    public DeleteAsyncTask(){
        this.dao = new PictureDAOImpl(GalleryAppImpl.getContext());
    }

    @Override
    protected void onPreExecute() {
        Log.i("koala","prepare to execute delete task");
    }

    @Override
    protected Void doInBackground(Void... params) {
         List<PictureDetail> list = dao.queryDeletedItems();
        for(int i=0;i<list.size();i++){
           delete(list.get(i).getPictureId());
        }
         return null;
    }


    public void delete(final String pictureId){
        //get image id
         RequestParams rp = new RequestParams("{'telephone':'"+GalleryApi.tel+"','pictureId':'" +  pictureId + "'}");
        InstagramRestClient.getInstance().postData(Request.Method.POST, GalleryApi.delete, rp, new DeleteParser(), new ApiRequestListener<DeleteResp>(){

            @Override
            public void onPreExecute() {
                // TODO Auto-generated method stub
            }

            @Override
            public void onComplete(int statusCode, DeleteResp  value) {
                boolean isDeleted = value.isSuccess();
                if(isDeleted){
                    //delete database item
                    dao.deletePicture(pictureId);
                }
            }

            @Override
            public void onException(InstagramPlusException e) {
                // TODO Auto-gener
            }});
    }
}
