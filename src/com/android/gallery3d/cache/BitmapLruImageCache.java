package com.android.gallery3d.cache;

import com.android.volley.toolbox.ImageLoader.ImageCache;

import android.graphics.Bitmap;
import android.support.v4.util.LruCache;
import android.util.Log;

/**
 * Basic LRU Memory cache.
 * 
 * @author Trey Robinson
 *
 */
public class BitmapLruImageCache extends LruCache<String, Bitmap> implements ImageCache{
	
	private final String TAG = this.getClass().getSimpleName();
	
	public BitmapLruImageCache(int maxSize) {
		super(maxSize);
	}
	
	@Override
	protected int sizeOf(String key, Bitmap value) {
		int size =  value.getRowBytes() * value.getHeight() / 1024;
		return size;
	}
	
	@Override
	public Bitmap getBitmap(String url) {
		Log.e(TAG, "Retrieved item from Mem Cache");
		return get(url);
	}
 
	@Override
	public void putBitmap(String url, Bitmap bitmap) {
		Log.e(TAG, "Added item to Mem Cache");
		put(url, bitmap);
	}
}
