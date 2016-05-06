package com.android.gallery3d.volley;


import org.json.JSONException;
import org.json.JSONObject;

import com.android.volley.VolleyError;

public abstract class JSONParser<T>{
	public abstract T parse(JSONObject json) throws JSONException, VolleyError;
	
}
