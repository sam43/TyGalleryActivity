package com.android.gallery3d.parser;

import com.android.gallery3d.volley.JSONParser;
import com.android.volley.VolleyError;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by taoxj on 16-5-4.
 */
public class DeleteParser extends JSONParser<DeleteResp> {
    @Override
    public DeleteResp parse(JSONObject json) throws JSONException, VolleyError {
        DeleteResp resp = new DeleteResp();
        String code = json.getString("code");
        if("0000".equals(code)){
            resp.setIsSuccess(true);
        }else{
            resp.setIsSuccess(false);
        }
        return resp;
    }
}
