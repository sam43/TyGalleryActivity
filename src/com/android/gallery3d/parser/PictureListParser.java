package com.android.gallery3d.parser;

import android.util.Log;

import com.android.gallery3d.database.PictureDetail;
import com.android.gallery3d.database.Pictures;
import com.android.gallery3d.volley.JSONParser;
import com.android.volley.VolleyError;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by taoxj on 16-4-28.
 */
public class PictureListParser extends JSONParser<PictureListResp> {
    public   SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    @Override
    public PictureListResp parse(JSONObject json) throws JSONException, VolleyError {
        PictureListResp resp = new PictureListResp();
         List<PictureDetail> list = new ArrayList<PictureDetail>();
        JSONArray jsonArray = json.getJSONObject("data").getJSONArray("familyAlbumPictureList");
        for(int i = 0 ;i<jsonArray.length();i++){
            JSONObject obj = (JSONObject) jsonArray.get(i);
            PictureDetail p = new PictureDetail();
            p.setPictureId(obj.getString("pictureId"));
            String time = obj.getString("takePicktureTime");
            long takenTime = 0;
            try {
                takenTime  = sdf.parse(time).getTime();
            } catch (ParseException e) {
                e.printStackTrace();
            }
            p.setTakePictureTime(takenTime);
            p.setUrl(obj.getString("pictureUrl"));
            p.setUserPhotoUrl(obj.getString("userAvatar"));
            int index = p.getUrl().lastIndexOf(".");
            //Log.i("koala","url = " + p.getUrl() + ",length = " + type.length );
            p.setType(p.getUrl().substring(index + 1));
            list.add(p);
        }
        resp.setPicturesList(list);
        return resp;
    }
}
