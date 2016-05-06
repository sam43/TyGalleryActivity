/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.gallery3d.app;

import android.content.Context;
import android.content.res.Resources;
import android.os.Looper;

import com.android.gallery3d.data.DataManager;
import com.android.gallery3d.util.ThreadPool;
/*TIANYU: yuxin add begin for New Design Gallery*/
import com.android.gallery3d.util.PanoramaViewHelper;
import com.android.gallery3d.ui.GLRoot;
import com.android.gallery3d.ui.TyBaseFragment;
import com.android.gallery3d.app.StateManager;
import com.android.gallery3d.app.TransitionStore;
import com.android.gallery3d.app.GalleryActionBar;
import com.android.gallery3d.app.TyGalleryBottomBar;
import com.android.gallery3d.app.OrientationManager;
import java.util.ArrayList;
import android.net.Uri;
import android.view.View;
/*TIANYU: yuxin add end for New Design Gallery*/

public interface GalleryContext {
    public DataManager getDataManager();

    public Context getAndroidContext();

    public Looper getMainLooper();
    public Resources getResources();
    public ThreadPool getThreadPool();
    /*TIANYU: yuxin add begin for New Design Gallery*/
    public GLRoot getGLRoot();
    public StateManager getStateManager();
    public TransitionStore getTransitionStore();
    public GalleryActionBar getGalleryActionBar();
    public TyGalleryBottomBar getGalleryBottomBar();
    public ThreadPool getBatchServiceThreadPoolIfAvailable();
    public OrientationManager getOrientationManager();
    public PanoramaViewHelper getPanoramaViewHelper();
    public boolean isFullscreen();
    public void printSelectedImage(Uri uri);
    public View getGalleryAssignView(int resId);
    public ArrayList<StateManager> getOtherStateManagers(StateManager sm);
    public ArrayList<TyBaseFragment> getAllFragment();
    public String getGLTag(int key);
    /*TIANYU: yuxin add begin for New Design Gallery*/
}
