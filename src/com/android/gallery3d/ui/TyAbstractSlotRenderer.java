/*
 * Copyright (C) 2012 The Android Open Source Project
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

package com.android.gallery3d.ui;

import android.content.Context;
import android.graphics.Rect;

import com.android.gallery3d.R;
import com.android.gallery3d.glrenderer.FadeOutTexture;
import com.android.gallery3d.glrenderer.GLCanvas;
import com.android.gallery3d.glrenderer.NinePatchTexture;
import com.android.gallery3d.glrenderer.ResourceTexture;
import com.android.gallery3d.glrenderer.Texture;
import com.android.gallery3d.app.GalleryContext;


public abstract class TyAbstractSlotRenderer implements TySlotView.SlotRenderer {

    //private final ResourceTexture mVideoOverlay;
    //private final ResourceTexture mVideoPlayIcon;
    private final ResourceTexture mPanoramaIcon;
    private final NinePatchTexture mFramePressed;
    private final NinePatchTexture mFrameSelected;
    private ResourceTexture mTyVideoOverlay;
    //private ResourceTexture mTyFirstVideoOverlay;
    //private NinePatchTexture mFrameSelectionMode;

    private FadeOutTexture mFramePressedUp;

    protected TyAbstractSlotRenderer(GalleryContext galleryContext) {
        Context context = galleryContext.getAndroidContext();
        String glTag = galleryContext.getGLTag(R.id.ty_gl_tag);
        //mVideoOverlay = new ResourceTexture(context, R.drawable.ic_video_thumb, glTag);
        //mVideoPlayIcon = new ResourceTexture(context, R.drawable.ic_gallery_play, glTag);
        mPanoramaIcon = new ResourceTexture(context, R.drawable.ic_360pano_holo_light, glTag);
        mFramePressed = new NinePatchTexture(context, R.drawable.grid_pressed, glTag);
        mFrameSelected = new NinePatchTexture(context, R.drawable.ty_grid_selected, glTag);
        //mFrameSelectionMode = new NinePatchTexture(context, R.drawable.ty_grid_selection_mode, glTag);

        mTyVideoOverlay = new ResourceTexture(context, R.drawable.ty_ic_album_time_video_thumb, glTag);
        //mTyFirstVideoOverlay = new ResourceTexture(context, R.drawable.ty_ic_album_time_video_thumb_first, glTag);
    }

    protected void drawContent(GLCanvas canvas,
            Texture content, int width, int height, int rotation) {
        canvas.save(GLCanvas.SAVE_FLAG_MATRIX);

        // The content is always rendered in to the largest square that fits
        // inside the slot, aligned to the top of the slot.
        width = height = Math.min(width, height);
        if (rotation != 0) {
            canvas.translate(width / 2, height / 2);
            canvas.rotate(rotation, 0, 0, 1);
            canvas.translate(-width / 2, -height / 2);
        }

        // Fit the content into the box
        float scale = Math.min(
                (float) width / content.getWidth(),
                (float) height / content.getHeight());
        canvas.scale(scale, scale, 1);
        content.draw(canvas, 0, 0);

        canvas.restore();
    }

    protected void drawContent(GLCanvas canvas,
            Texture content, int width, int height, int rotation, int index) {
        canvas.save(GLCanvas.SAVE_FLAG_MATRIX);

        // The content is always rendered in to the largest square that fits
        // inside the slot, aligned to the top of the slot.
        if(index == 0){
            if (rotation != 0) {
                canvas.translate(width / 2, height / 2);
                canvas.rotate(rotation, 0, 0, 1);
                canvas.translate(-width / 2, -height / 2);
            }
        	float tyScalewidth = (float) width / content.getWidth();
        	float tyScaleheight = (float) height / content.getHeight();
        		canvas.scale(tyScalewidth, tyScaleheight, 1);
        }else{
            width = height = Math.min(width, height);
            if (rotation != 0) {
                canvas.translate(width / 2, height / 2);
                canvas.rotate(rotation, 0, 0, 1);
                canvas.translate(-width / 2, -height / 2);
            }

            // Fit the content into the box
            float scale = Math.min(
                    (float) width / content.getWidth(),
                    (float) height / content.getHeight());
            canvas.scale(scale, scale, 1);
        }
        content.draw(canvas, 0, 0);

        canvas.restore();
    }

    //protected void drawVideoOverlay(GLCanvas canvas, int width, int height) {
    //    // Scale the video overlay to the height of the thumbnail and put it
    //    // on the left side.
    //    ResourceTexture v = mVideoOverlay;
    //    float scale = (float) height / v.getHeight();
    //    int w = Math.round(scale * v.getWidth());
    //    int h = Math.round(scale * v.getHeight());
    //    v.draw(canvas, 0, 0, w, h);
    //
    //    int s = Math.min(width, height) / 6;
    //    mVideoPlayIcon.draw(canvas, (width - s) / 2, (height - s) / 2, s, s);
    //}

    protected void tyDrawVideoOverlay(GLCanvas canvas, int width, int height) {
    	mTyVideoOverlay.draw(canvas, 0, 0, width, height);
    }
    
    //protected void tyDrawFirstVideoOverlay(GLCanvas canvas, int width, int height) {
    //	mTyFirstVideoOverlay.draw(canvas, 0, 0, width, height);
    //}

    protected void drawPanoramaIcon(GLCanvas canvas, int width, int height) {
        int iconSize = Math.min(width, height) / 6;
        mPanoramaIcon.draw(canvas, (width - iconSize) / 2, (height - iconSize) / 2,
                iconSize, iconSize);
    }

    protected boolean isPressedUpFrameFinished() {
        if (mFramePressedUp != null) {
            if (mFramePressedUp.isAnimating()) {
                return false;
            } else {
                mFramePressedUp = null;
            }
        }
        return true;
    }
    
    protected void drawPressedUpFrame(GLCanvas canvas, int width, int height) {
        if (mFramePressedUp == null) {
            mFramePressedUp = new FadeOutTexture(mFramePressed);
        }
        drawFrame(canvas, mFramePressed.getPaddings(), mFramePressedUp, 0, 0, width, height);
    }

    protected void drawPressedFrame(GLCanvas canvas, int width, int height) {
        drawFrame(canvas, mFramePressed.getPaddings(), mFramePressed, 0, 0, width, height);
    }

    protected void drawSelectedFrame(GLCanvas canvas, int width, int height) {
        drawFrame(canvas, mFrameSelected.getPaddings(), mFrameSelected, 0, 0, width, height);
    }

    //protected void drawSelectionMode(GLCanvas canvas, int width, int height) {
    //    drawFrame(canvas, mFrameSelectionMode.getPaddings(), mFrameSelectionMode, 0, 0, width, height);
    //}
    


    protected static void drawFrame(GLCanvas canvas, Rect padding, Texture frame,
            int x, int y, int width, int height) {
        frame.draw(canvas, x - padding.left, y - padding.top, width + padding.left + padding.right,
                 height + padding.top + padding.bottom);
    }
}
