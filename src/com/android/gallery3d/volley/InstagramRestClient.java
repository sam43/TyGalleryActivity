package com.android.gallery3d.volley;


import org.apache.http.HttpVersion;
import org.apache.http.conn.params.ConnManagerParams;
import org.apache.http.conn.params.ConnPerRouteBean;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpProtocolParams;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.http.AndroidHttpClient;
import android.os.Build;

import com.android.gallery3d.api.ApiRequestListener;
import com.android.gallery3d.api.InstagramResp;
import com.android.gallery3d.app.GalleryAppImpl;
import com.android.volley.Network;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.BasicNetwork;
import com.android.volley.toolbox.DiskBasedCache;
import com.android.volley.toolbox.HttpClientStack;
import com.android.volley.toolbox.HttpStack;
import com.android.volley.toolbox.HurlStack;
import com.android.gallery3d.cache.ImageCacheManager;


public class InstagramRestClient {
	private static final String VERSION = "1.4.3";

    private static final int DEFAULT_MAX_CONNECTIONS = 10;
    private static final int DEFAULT_SOCKET_TIMEOUT = 10 * 1000;
    private static final int DEFAULT_SOCKET_BUFFER_SIZE = 8192;

    private static int maxConnections = DEFAULT_MAX_CONNECTIONS;
    private static int socketTimeout = DEFAULT_SOCKET_TIMEOUT;

	//taoxj add dor check login begin
	private static String sessionId = "";
	//taoxj add for check login end
	
    
    private static InstagramRestClient  instance;
	/**
	 * the queue :-)
	 */
	private static RequestQueue mRequestQueue;

	private static AsyncHttpClient client;
	
	
	private String productKey = null;
	private String udid = null;
	/**
	 * Nothing to see here.
	 */

	/**
	 * @param context
	 * 			application context
	 */
	public  void init(Context context) {
		    BasicHttpParams httpParams = new BasicHttpParams();

	        ConnManagerParams.setTimeout(httpParams, socketTimeout);
	        ConnManagerParams.setMaxConnectionsPerRoute(httpParams, new ConnPerRouteBean(maxConnections));
	        ConnManagerParams.setMaxTotalConnections(httpParams, DEFAULT_MAX_CONNECTIONS);

	        HttpConnectionParams.setSoTimeout(httpParams, socketTimeout);
	        HttpConnectionParams.setConnectionTimeout(httpParams, socketTimeout);
	        HttpConnectionParams.setTcpNoDelay(httpParams, true);
	        HttpConnectionParams.setSocketBufferSize(httpParams, DEFAULT_SOCKET_BUFFER_SIZE);

	        HttpProtocolParams.setVersion(httpParams, HttpVersion.HTTP_1_1);
	    //    HttpProtocolParams.setUserAgent(httpParams, String.format("android-async-http/%s (http://loopj.com/android-async-http)", VERSION));

	        SchemeRegistry schemeRegistry = new SchemeRegistry();
	        schemeRegistry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
	        schemeRegistry.register(new Scheme("https", SSLSocketFactory.getSocketFactory(), 443));
	        ThreadSafeClientConnManager cm = new ThreadSafeClientConnManager(httpParams, schemeRegistry);
	        client = new AsyncHttpClient(cm, httpParams);
		    mRequestQueue = newRequestQueue(context, new HttpClientStack(client));
		    
	}
	
	 public  RequestQueue newRequestQueue(Context context, HttpStack stack) {
	        String userAgent = "volley/0";
	        try {
	            String packageName = context.getPackageName();
	            PackageInfo info = context.getPackageManager().getPackageInfo(packageName, 0);
	            userAgent = packageName + "/" + info.versionCode;
	        } catch (NameNotFoundException e) {
	        }
 	        if (stack == null) {
	            if (Build.VERSION.SDK_INT >= 9) {
	                stack = new HurlStack();
	            } else {
	                // Prior to Gingerbread, HttpUrlConnection was unreliable.
	                // See: http://android-developers.blogspot.com/2011/09/androids-http-clients.html
	                stack = new HttpClientStack(AndroidHttpClient.newInstance(userAgent));
	            }
	        }

	        Network network = new BasicNetwork(stack);

	        RequestQueue queue = new RequestQueue(new DiskBasedCache(ImageCacheManager.getInstance().getDiskCache()), network);
	        queue.start();
	        return queue;
	    }
	
	
	public void setupHeader(){
		/*
		version ：客户端版本号
		deviceId：设备唯一号
		sessionId：（如果没有传空值，登录成功后，服务器会返回sessionId，代表用户的登录凭证，之后的所有请求都要携带sessionId）
		manufacture ：设备厂商
		mobileModel： 设备类型
		osVersion： 设备操作系统版本号
		userAgent：用户UA信息
		platform： 客户端平台，手机上全部为Phone，电脑上全部为PC，盒子上全部为Box
		scene： 场景,取值范围iOS/Android/Web/Wap/Box
		channel：渠道(友盟统计相关)
		jgPushId：极光推送ID(极光推送相关) */
		
		
		 
 		client.addHeader("version","1.0");
		client.addHeader("deviceId","123456");
		client.addHeader("sessionId","");
		client.addHeader("userAgent","android-async-http/1.4.3 (http://loopj.com/android-async-http)");
		client.addHeader("platform","Box");
		client.addHeader("scene","Box");
		client.addHeader("channel", "haha");
 		  
 	}

	public void setSessionId(String session){
		client.addHeader("sessionId",session);
		sessionId = session;
	}
	public AsyncHttpClient getAsyncHttpClient () {
		return client;
	}
	
	/**
	 * @return
	 * 		instance of the queue
	 * @throws
	 * 		IllegalStatException if init has not yet been called
	 */
	public  RequestQueue getRequestQueue() {
//	    if (mRequestQueue != null) {
//	        return mRequestQueue;
//	    } else {
//	        throw new IllegalStateException("Not initialized");
//	    }
		if (mRequestQueue == null) {
			init(GalleryAppImpl.getContext());
		}
		return mRequestQueue;
	}
	
	 

	
	public static InstagramRestClient  getInstance() {
		if (instance == null) {
			synchronized (InstagramRestClient.class) {
				if (instance == null) {
					instance = new InstagramRestClient();
					instance.setupHeader();
				}
			}
		}
		
		return instance;
	}

	//taoxj add for check login
	public static String getSeesionId(){
		return sessionId;
	}
	 public <T extends InstagramResp> void postData(int method,String url, RequestParams params, JSONParser<T> parser, ApiRequestListener<T> listener) {
		  //InstagramHandler<T> handler = new InstagramHandler<T>(Method.POST, url , params, parser, listener);
		  
		 InstagramRequest<T> request  = new InstagramRequest<T>(method/*Method.POST*/, url , params, parser, listener);
		 getRequestQueue().add(request);
	  }
	  
	private  InstagramRestClient() {
		init(GalleryAppImpl.getContext());
	}
	
}
