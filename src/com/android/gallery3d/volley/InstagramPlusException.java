package com.android.gallery3d.volley;

import com.android.volley.VolleyError;


public class InstagramPlusException extends VolleyError{
	
	private static final long serialVersionUID = 1L;
	
	private int code = 0;
	private String extraMsg;
	
	public InstagramPlusException(String msg){
		super(msg);
	}
	
	public InstagramPlusException(int code, String msg){
		super(msg);
		this.code = code;
	}
	
	public InstagramPlusException(int code, String msg, String extra){
		super(msg);
		this.code = code;
		this.extraMsg = extra;
	}
	
	public int getCode(){
		return this.code;
	}
	
	/*public String getExtraMsg(){
		return this.extraMsg;
	}*/
}
