package com.android.gallery3d.volley;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Response;
import com.android.volley.Response.Listener;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.gallery3d.api.ApiRequestListener;
import com.android.gallery3d.api.InstagramResp;

/**
 * Wrapper for Volley requests to facilitate parsing of json responses. 
 * 
 * @param <T>
 */
public class InstagramRequest<T  extends InstagramResp> extends BaseRequest<T> {
	/**
	 * Gson parser 
	 */
	//private final Gson mGson;
	
	/**
	 * Class type for the response
	 *///
	//private final Class<T> mClass;
	
	
	/**
	 * Callback for response delivery 
	 */
	
	
	
	private JSONParser<T> parser;
	
	
	
	/**
	 * @param method
	 * 		Request type.. Method.GET etc
	 * @param url
	 * 		path for the requests
	 * @param objectClass
	 * 		expected class type for the response. Used by gson for serialization.
	 * @param listener
	 * 		handler for the response
	 * @param errorListener
	 * 		handler for errors
	 */
    public InstagramRequest(int method
						, String url
						,RequestParams params
						,JSONParser<T> parser
						,ApiRequestListener<T> listener) {
		
		super(method, url,params,  createMyReqSuccessListener(listener), createMyReqErrorListener(listener));
		this.parser  = parser;
		
		if(listener != null) {
			listener.onPreExecute();
		}
		
	}
	

	
	 private static <T  extends InstagramResp>   Listener<T>   createMyReqSuccessListener(final ApiRequestListener<T> listener) {
	        return new Listener<T>() {
	            @Override
	            public void onResponse(T response) {
	            	if(listener != null) {
	            		listener.onComplete(200, response);
	            	}
	            }
	        };
	    }
	    
	    
	    private  static <T  extends InstagramResp> Response.ErrorListener createMyReqErrorListener(final ApiRequestListener<T> listener) {
	        return new Response.ErrorListener() {
	            @Override
	            public void onErrorResponse(VolleyError error) {
	            	if(listener != null) {
	            		listener.onException(onFailure(error));
	            	}
	            }
	        };
	    }
	    
		public static InstagramPlusException onFailure(VolleyError volleyError) {
			InstagramPlusException exception = null;
			Throwable error = volleyError.getCause();
			if (volleyError instanceof InstagramPlusException) {
				exception  = (InstagramPlusException) volleyError;
			} else if (error instanceof IOException) {
				exception = new InstagramPlusException("");
			} else {
				exception = new InstagramPlusException(400, "");
			}
			return exception;
		}
	    
	    
	@Override
	protected Response<T> parseNetworkResponse(NetworkResponse response) {
		try {
			
 			String json = new String(response.data, HttpHeaderParser.parseCharset(response.headers));
			Log.i("koala","result = " + json);
			JSONObject jsonObject  =  new JSONObject(json);

 			if(jsonObject.has("error")){
 				String data = jsonObject.getJSONArray("error").getJSONObject(0).getString("error");
 				 return Response.error(new InstagramPlusException(data));
 			}else{
 			return Response.success( parser.parse(jsonObject),
		            HttpHeaderParser.parseCacheHeaders(response));
 			}
 		} catch (UnsupportedEncodingException e) {
			return Response.error(new ParseError(e));
		} catch (JSONException e) {
			Log.i("koala",e.getMessage());
 			return Response.error(new ParseError(e));
		} catch (VolleyError e) {
 			return Response.error(new ParseError(e));
		}
	}
}
