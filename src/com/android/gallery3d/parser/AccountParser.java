package com.android.gallery3d.parser;

import org.json.JSONException;
import org.json.JSONObject;

import android.telecom.Log;

import com.android.volley.VolleyError;
import com.android.gallery3d.volley.JSONParser;




public class AccountParser extends JSONParser<AccountResp>{

	@Override
	public AccountResp parse(JSONObject json) throws JSONException, VolleyError {
		AccountResp result = new AccountResp();
		result.setSeesion(json.getJSONObject("data").getString("sessionId"));
		return result;
	}

}
