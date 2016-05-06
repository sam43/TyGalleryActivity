package com.android.gallery3d.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.android.gallery3d.app.GalleryAppImpl;

/**
 * Created by taoxj on 16-5-4.
 */
public class NetworkUtils {
    private static ConnectivityManager manager;
    public static boolean isConnected(){
          manager = (ConnectivityManager) GalleryAppImpl.getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = manager.getActiveNetworkInfo();
        return (info != null && info.isAvailable());
    }
}
