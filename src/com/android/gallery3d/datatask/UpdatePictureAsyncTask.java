package com.android.gallery3d.datatask;

import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import com.android.gallery3d.app.GalleryAppImpl;
import com.android.gallery3d.database.PictureDAO;
import com.android.gallery3d.database.PictureDAOImpl;

/**
 * Created by taoxj on 16-5-3.
 */
public class UpdatePictureAsyncTask extends AsyncTask<Uri,Void,Boolean> {
    private PictureDAO dao;

    public UpdatePictureAsyncTask(){
        this.dao = new PictureDAOImpl(GalleryAppImpl.getContext());
    }

    @Override
    protected Boolean doInBackground(Uri... params) {
        Uri mBaseUri = params[0];
        dao.updatePictures(mBaseUri);
        return true;
    }

    @Override
    protected void onPostExecute(Boolean aBoolean) {
        //update database finished,upload the local picture
        Log.i("koala","prepare to upload local picture == " + aBoolean);
         if(aBoolean){
             new UploadAsyncTask().execute();
         }
    }
}
