package com.android.gallery3d.cache;

import java.io.File;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.widget.ImageView;

import com.android.gallery3d.volley.InstagramRestClient;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.ImageLoader.ImageCache;
import com.android.volley.toolbox.ImageLoader.ImageListener;
import com.android.volley.toolbox.NetworkImageView;

/**
 * Implementation of volley's ImageCache interface. This manager tracks the
 * application image loader and cache.
 * 
 * Volley recommends an L1 non-blocking cache which is the default MEMORY
 * CacheType.
 * 
 * @author Trey Robinson
 * 
 */
public class ImageCacheManager {

	/**
	 * Volley recommends in-memory L1 cache but both a disk and memory cache are
	 * provided. Volley includes a L2 disk cache out of the box but you can
	 * technically use a disk cache as an L1 cache provided you can live with
	 * potential i/o blocking.
	 * 
	 */
	public enum CacheType {
		DISK, MEMORY
	}

	private static ImageCacheManager mInstance;

	/**
	 * Volley image loader
	 */
	private ImageLoader mImageLoader;

	/**
	 * Image cache implementation
	 */
	private ImageCache mImageCache;

	private File cacheDir;

	/**
	 * @return instance of the cache manager
	 */
	public static ImageCacheManager getInstance() {
		if (mInstance == null)
			mInstance = new ImageCacheManager();

		return mInstance;
	}

	/**
	 * Initializer for the manager. Must be called prior to use.
	 * 
	 * @param context
	 *            application context
	 * @param uniqueName
	 *            name for the cache location
	 * @param cacheSize
	 *            max size for the cache
	 * @param compressFormat
	 *            file type compression format.
	 * @param quality
	 */
	public void init(Context context, String uniqueName, int cacheSize, CompressFormat compressFormat, int quality, CacheType type) {
		switch (type) {
		case DISK:
			mImageCache = new DiskLruImageCache(context, uniqueName, cacheSize, compressFormat, quality);
			break;
		case MEMORY:
			mImageCache = new BitmapLruImageCache(cacheSize);
		default:
			mImageCache = new BitmapLruImageCache(cacheSize);
			break;
		}

		if (android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED)) {
			cacheDir = new File(android.os.Environment.getExternalStorageDirectory(), "multigold/ImageCache");
		} else {
			cacheDir = context.getCacheDir();
		}
		if (!cacheDir.exists()) {
			cacheDir.mkdirs();
		}

		mImageLoader = new ImageLoader(InstagramRestClient.getInstance().getRequestQueue(), mImageCache);
	}

	public File getDiskCache() {
		return cacheDir;
	}

	public Bitmap getBitmap(String url) {
		try {
			return mImageCache.getBitmap(createKey(url));
		} catch (NullPointerException e) {
			throw new IllegalStateException("Disk Cache Not initialized");
		}
	}

	public void putBitmap(String url, Bitmap bitmap) {
		try {
			mImageCache.putBitmap(createKey(url), bitmap);
		} catch (NullPointerException e) {
			throw new IllegalStateException("Disk Cache Not initialized");
		}
	}

	/**
	 * Executes and image load
	 * 
	 * @param url
	 *            location of image
	 * @param listener
	 *            Listener for completion
	 */
	private void displayImageView(String url, ImageListener listener, int maxWidth, int maxHeight) {
		mImageLoader.get(url, listener,maxWidth,maxHeight);
	}

	public void displayImage(String uri, ImageView imageView, int defaultResId, int maxWidth, int maxHeight) {
		if(imageView instanceof  NetworkImageView) {
			NetworkImageView ig = (NetworkImageView) imageView;
			ig.setImageUrl(uri, mImageLoader);
			ig.setDefaultImageResId(defaultResId);
			ig.setErrorImageResId(defaultResId);
		}else {
			displayImageView(uri, ImageLoader.getImageListener(imageView, defaultResId, 0),maxWidth,maxHeight);
		}
		
	}
	private String getHeaderUrl(int uid) {
		return String.format("http://ucenter.whatua.com/avatar.php?uid=%d&size=48*48", uid);
	}
	public void displayHeaderImage(int uid, ImageView imageView, int defaultResId,int maxWidth,int maxHeight) {
		displayImage(getHeaderUrl(uid), imageView, defaultResId,maxWidth,maxHeight);
	}
	
	
	
	public Bitmap getSampleBitmap(String file, int requireWidth, int requireHeight) {
		 // First decode with inJustDecodeBounds=true to check dimensions
	    final BitmapFactory.Options options = new BitmapFactory.Options();
	    options.inJustDecodeBounds = true;
	    BitmapFactory.decodeFile(file, options);

	    // Calculate inSampleSize
	    options.inSampleSize = calculateInSampleSize(options, requireWidth, requireHeight);

	    // Decode bitmap with inSampleSize set
	    options.inJustDecodeBounds = false;
	    return  BitmapFactory.decodeFile(file, options);
	}
	
	public static int calculateInSampleSize(
            BitmapFactory.Options options, int reqWidth, int reqHeight) {
    // Raw height and width of image
    final int height = options.outHeight;
    final int width = options.outWidth;
    int inSampleSize = 1;

    if (height > reqHeight || width > reqWidth) {

        // Calculate ratios of height and width to requested height and width
        final int heightRatio = Math.round((float) height / (float) reqHeight);
        final int widthRatio = Math.round((float) width / (float) reqWidth);

        // Choose the smallest ratio as inSampleSize value, this will guarantee
        // a final image with both dimensions larger than or equal to the
        // requested height and width.
        inSampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;
    }

    return inSampleSize;
}
	
	/**
	 * @return instance of the image loader
	 */
	public ImageLoader getImageLoader() {
		return mImageLoader;
	}

	/**
	 * Creates a unique cache key based on a url value
	 * 
	 * @param url
	 *            url to be used in key creation
	 * @return cache key value
	 */
	private String createKey(String url) {
		return getCacheKey(url,0,0);
	}
	
	   private static String getCacheKey(String url, int maxWidth, int maxHeight) {
	    	String sb =  new StringBuilder(url.length() + 12).append("#W").append(maxWidth)
	           .append("#H").append(maxHeight).append(url).toString();
	    	return String.valueOf(sb.hashCode());
	    }

}
