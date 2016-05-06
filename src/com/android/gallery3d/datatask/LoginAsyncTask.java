package com.android.gallery3d.datatask;

import android.os.AsyncTask;

import com.android.gallery3d.api.ApiRequestListener;
import com.android.gallery3d.database.PictureDetail;
import com.android.gallery3d.parser.AccountParser;
import com.android.gallery3d.parser.AccountResp;
import com.android.gallery3d.parser.PictureListParser;
import com.android.gallery3d.parser.PictureListResp;
import com.android.gallery3d.volley.GalleryApi;
import com.android.gallery3d.volley.InstagramPlusException;
import com.android.gallery3d.volley.InstagramRestClient;
import com.android.gallery3d.volley.RequestParams;
import com.android.volley.Request;

import java.util.List;

/**
 * Created by taoxj on 16-4-28.
 */
public class LoginAsyncTask extends AsyncTask<Void,Void,Void> {


    @Override
    protected Void doInBackground(Void... params) {
        //Login
        RequestParams rp = new RequestParams("{'telephone':'18610277013','password':'123456a'}");

        InstagramRestClient.getInstance().postData(Request.Method.POST, GalleryApi.login, rp, new AccountParser(), new ApiRequestListener<AccountResp>() {

            @Override
            public void onPreExecute() {
                // TODO Auto-generated method stub
                android.util.Log.i("koala", "prepare request login----");
            }

            @Override
            public void onComplete(int statusCode, AccountResp value) {
                // TODO Auto-generated method stub
                android.util.Log.i("koala", "complete---");
                InstagramRestClient.getInstance().setSessionId(value.getSeesion());
                //dowload cloud picture
                new DownloadAsyncTask().execute();
            }

            @Override
            public void onException(InstagramPlusException e) {
                // TODO Auto-generated method stub

            }
        });
        return null;
    }


}