package com.android.gallery3d.parser;

import android.util.Log;

import com.android.gallery3d.volley.JSONParser;
import com.android.volley.VolleyError;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by taoxj on 16-5-3.
 */
public class UploadParser extends JSONParser<UploadResp> {

    @Override
    public UploadResp parse(JSONObject json) throws JSONException, VolleyError {
        UploadResp resp= new UploadResp();
        String code = json.getString("code");
        if(code.equals("0000")){
            String id = json.getJSONObject("data").getString("pictuerId");
            resp.setIsSuccess(true);
            resp.setPictureId(id);
        }else{
            resp.setIsSuccess(false);
        }
        return resp;
    }
}
