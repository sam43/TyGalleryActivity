package com.android.gallery3d.api;

import com.android.volley.VolleyError;
import com.android.gallery3d.volley.InstagramPlusException;

public interface  ApiRequestListener<T>{
	
	public void onPreExecute();
	public void onComplete(int statusCode, T value);
	public void onException(InstagramPlusException e);
	
}
 