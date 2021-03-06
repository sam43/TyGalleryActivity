/*
 * Copyright (C) 2011 The Android Open Source Project
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
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.android.gallery3d.R;
import com.android.gallery3d.app.CommonControllerOverlay.State;
import org.codeaurora.gallery3d.ext.IContrllerOverlayExt;
import org.codeaurora.gallery3d.video.IControllerRewindAndForward;
import org.codeaurora.gallery3d.video.IControllerRewindAndForward.IRewindAndForwardListener;
import org.codeaurora.gallery3d.video.ExtensionHelper;
import org.codeaurora.gallery3d.video.ScreenModeManager;
import org.codeaurora.gallery3d.video.ScreenModeManager.ScreenModeListener;
/*TIANYURD:Lizhy 20150324 add for videoplaer start*/
import android.view.GestureDetector;
import android.view.Display;
import android.media.AudioManager;
import android.view.WindowManager;
import android.widget.SeekBar;
import android.view.LayoutInflater;
import android.widget.SeekBar.OnSeekBarChangeListener;
/*TIANYURD:Lizhy 20150324 add for videoplaer end*/	


/**
 * The playback controller for the Movie Player.
 */
public class MovieControllerOverlay extends CommonControllerOverlay implements
        AnimationListener {

    private static final String TAG = "Gallery3D/MovieControllerOverlay";
    private static final boolean LOG = true;

    private ScreenModeManager mScreenModeManager;
    private ScreenModeExt mScreenModeExt = new ScreenModeExt();
    private ControllerRewindAndForwardExt mControllerRewindAndForwardExt = new ControllerRewindAndForwardExt();
    private OverlayExtension mOverlayExt = new OverlayExtension();
    private boolean hidden;

    private final Handler handler;
    private final Runnable startHidingRunnable;
    private final Animation hideAnimation;

    private boolean enableRewindAndForward = false;
    private Context mContext;
    /*TIANYURD:Lizhy 20150327 add for videoplayer start*/
    private GestureDetector mGestureDector;
    private AudioManager audioManager;
    private static float mBrightness = -1f;
    private ViewGroup mView;
    /*TIANYURD:Lizhy 20150327 add for videoplayer end*/
	
    public MovieControllerOverlay(Context context) {
        super(context);
        mContext = context;
        handler = new Handler();
        startHidingRunnable = new Runnable() {
                @Override
            public void run() {
                startHiding();
            }
        };

        hideAnimation = AnimationUtils.loadAnimation(context, R.anim.player_out);
        hideAnimation.setAnimationListener(this);
/*TIANYURD:yanghao modify for PROD102201099 start*/
        enableRewindAndForward = false;   
/*TIANYURD:yanghao modify for PROD102201099 end*/ 
        /*TIANYURD:Lizhy 20150323 add start for videoplayer start*/
        /*
        mGestureDector = new GestureDetector(mContext, new MyGestureListener());
        audioManager = (AudioManager)mContext.getSystemService(Context.AUDIO_SERVICE);
        mSeekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) { 
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                 float percent;
                 if(progress == 0){
                    percent = 0.01f;
                 }else if(progress == 1000){
                    percent = 1.0f;
                 }else{
                    percent = progress/1000;
            }

                 WindowManager.LayoutParams attrs = ((MovieActivity)mContext).getWindow().getAttributes();
                 attrs.screenBrightness = percent;
                 ((MovieActivity)mContext).getWindow().setAttributes(attrs);
            }
        });
        */
        /*TIANYURD:Lizhy 20150323 add start for videoplayer end*/

        Log.v(TAG, "enableRewindAndForward is " + enableRewindAndForward);
        mControllerRewindAndForwardExt.init(context);
        mScreenModeExt.init(context, mTimeBar);
        mBackground.setClickable(true);
        hide();
    }

    public void showPlaying() {
        if (!mOverlayExt.handleShowPlaying()) {
            mState = State.PLAYING;
            showMainView(mPlayPauseReplayView);
        }
        if (LOG) {
            Log.v(TAG, "showPlaying() state=" + mState);
        }
    }

    public void showPaused() {
        if (!mOverlayExt.handleShowPaused()) {
            mState = State.PAUSED;
            showMainView(mPlayPauseReplayView);
        }
        if (LOG) {
            Log.v(TAG, "showPaused() state=" + mState);
        }
    }

    public void showEnded() {
        mOverlayExt.onShowEnded();
        mState = State.ENDED;
        showMainView(mPlayPauseReplayView);
        if (LOG) {
            Log.v(TAG, "showEnded() state=" + mState);
        }
    }

    public void showLoading() {
        mOverlayExt.onShowLoading();
        mState = State.LOADING;
        showMainView(mLoadingView);
        if (LOG) {
            Log.v(TAG, "showLoading() state=" + mState);
        }
    }

    public void showErrorMessage(String message) {
        mOverlayExt.onShowErrorMessage(message);
        mState = State.ERROR;
        int padding = (int) (getMeasuredWidth() * ERROR_MESSAGE_RELATIVE_PADDING);
        mErrorView.setPadding(padding, mErrorView.getPaddingTop(), padding,
                mErrorView.getPaddingBottom());
        mErrorView.setText(message);
        showMainView(mErrorView);
    }

    @Override
    protected void createTimeBar(Context context) {
        mTimeBar = new TimeBar(context, this);
    }

    @Override
    public void hide() {
        boolean wasHidden = hidden;
        hidden = true;
        mPlayPauseReplayView.setVisibility(View.INVISIBLE);
        mLoadingView.setVisibility(View.INVISIBLE);
        if (!mOverlayExt.handleHide()) {
            setVisibility(View.INVISIBLE);
        }
        mBackground.setVisibility(View.INVISIBLE);
        mTimeBar.setVisibility(View.INVISIBLE);
        mScreenModeExt.onHide();
        if (enableRewindAndForward) {
            mControllerRewindAndForwardExt.onHide();
        }
        setFocusable(true);
        requestFocus();
        if (mListener != null && wasHidden != hidden) {
            mListener.onHidden();
        }
    }

    private void showMainView(View view) {
        mMainView = view;
        mErrorView.setVisibility(mMainView == mErrorView ? View.VISIBLE
                : View.INVISIBLE);
        mLoadingView.setVisibility(mMainView == mLoadingView ? View.VISIBLE
                : View.INVISIBLE);
        mPlayPauseReplayView
                .setVisibility(mMainView == mPlayPauseReplayView ? View.VISIBLE
                        : View.INVISIBLE);
        /*TIANYURD:Lizhy 20150401 add for videoplayer start*/
		/*
        mBrightnessPanelView
			.setVisibility(mMainView == mBrightnessPanelView ? View.VISIBLE
					: View.INVISIBLE);
	  */
        /*TIANYURD:Lizhy 20150401 add for videoplayer end*/
        mOverlayExt.onShowMainView(view);
        show();
    }

    @Override
    public void show() {
        boolean wasHidden = hidden;
        hidden = false;
        updateViews();
        setVisibility(View.VISIBLE);
        setFocusable(false);
        if (mListener != null && wasHidden != hidden) {
            mListener.onShown();
        }
        maybeStartHiding();
    }

    private void maybeStartHiding() {
        cancelHiding();
        if (mState == State.PLAYING) {
            handler.postDelayed(startHidingRunnable, 2500);
        }
    }

    private void startHiding() {
        if (mOverlayExt.canHidePanel()) {
            startHideAnimation(mBackground);
            startHideAnimation(mTimeBar);
            mScreenModeExt.onStartHiding();
            if (enableRewindAndForward) {
                mControllerRewindAndForwardExt.onStartHiding();
            }
        }
        startHideAnimation(mPlayPauseReplayView);
		/*TIANYURD:Lizhy 20150401 add for videoplayer start*/
		//mBrightnessPanelView.setVisibility(View.GONE);
		/*TIANYURD:Lizhy 20150401 add for videoplayer end*/
    }

    private void startHideAnimation(View view) {
        if (view.getVisibility() == View.VISIBLE) {
            view.startAnimation(hideAnimation);
        }
    }

    private void cancelHiding() {
        handler.removeCallbacks(startHidingRunnable);
        if (mOverlayExt.canHidePanel()) {
            mBackground.setAnimation(null);
            mTimeBar.setAnimation(null);
            mScreenModeExt.onCancelHiding();
            if (enableRewindAndForward) {
                mControllerRewindAndForwardExt.onCancelHiding();
            }
        }
        mPlayPauseReplayView.setAnimation(null);
    }

    @Override
    public void onAnimationStart(Animation animation) {
        // Do nothing.
    }

    @Override
    public void onAnimationRepeat(Animation animation) {
        // Do nothing.
    }

    @Override
    public void onAnimationEnd(Animation animation) {
        hide();
    }

    public void onClick(View view) {
        if (LOG) {
            Log.v(TAG, "onClick(" + view + ") listener=" + mListener
                    + ", state=" + mState + ", canReplay=" + mCanReplay);
        }
        if (mListener != null) {
            if (view == mPlayPauseReplayView) {
                if (mState == State.ENDED) {
                    if (mCanReplay) {
                        mListener.onReplay();
                    }
                } else if (mState == State.PAUSED || mState == State.PLAYING) {
                    mListener.onPlayPause();
                    // set view disabled (play/pause asynchronous processing)
                    setViewEnabled(true);
                }
            }
        } else {
            mScreenModeExt.onClick(view);
            if (enableRewindAndForward) {
                mControllerRewindAndForwardExt.onClick(view);
            }
        }
    }

    /*
     * set view enable (non-Javadoc)
     * @see com.android.gallery3d.app.ControllerOverlay#setViewEnabled(boolean)
     */
    @Override
    public void setViewEnabled(boolean isEnabled) {
        if (mListener.onIsRTSP()) {
            Log.v(TAG, "setViewEnabled is " + isEnabled);
            mOverlayExt.setCanScrubbing(isEnabled);
            mPlayPauseReplayView.setEnabled(isEnabled);
            if (enableRewindAndForward) {
                mControllerRewindAndForwardExt.setViewEnabled(isEnabled);
            }
        }
    }

    /*
     * set play pause button from disable to normal (non-Javadoc)
     * @see
     * com.android.gallery3d.app.ControllerOverlay#setPlayPauseReplayResume(
     * void)
     */
    @Override
    public void setPlayPauseReplayResume() {
        if (mListener.onIsRTSP()) {
            Log.v(TAG, "setPlayPauseReplayResume is enabled is true");
            mPlayPauseReplayView.setEnabled(true);
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (hidden) {
            show();
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (super.onTouchEvent(event)) {
            return true;
        }

        if (hidden) {
            show();
            return true;
        }
        /*TIANYURD:Lizhy 20150401 add for videoplayer start*/
		/*
        if(mGestureDector.onTouchEvent(event)){
            return true;
        }
        */
        /*TIANYURD:Lizhy 20150401 add for videoplayer end*/
		
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                cancelHiding();
                // you can click play or pause when view is resumed
                // play/pause asynchronous processing
                if ((mState == State.PLAYING || mState == State.PAUSED)
                        && mOverlayExt.mEnableScrubbing) {
                    mListener.onPlayPause();
                }
                break;
            case MotionEvent.ACTION_UP:
                maybeStartHiding();
                break;
        }
        return true;
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        int width = ((MovieActivity) mContext).getWindowManager().getDefaultDisplay().getWidth();
        Rect insets = mWindowInsets;
        int pl = insets.left; // the left paddings
        int pr = insets.right;
        int pt = insets.top;
        int pb = insets.bottom;

        int h = bottom - top;
        int w = right - left;
        boolean error = mErrorView.getVisibility() == View.VISIBLE;

        int y = h - pb;
        // Put both TimeBar and Background just above the bottom system
        // component.
        // But extend the background to the width of the screen, since we don't
        // care if it will be covered by a system component and it looks better.

        // Needed, otherwise the framework will not re-layout in case only the
        // padding is changed
        /*TIANYURD:Lizhy 20150410 modify  for TOS3.0 start*/
        //if (enableRewindAndForward) {
/*TIANYURD:yanghao modify for PROD102931495 start*/
/*
            mBackground.layout(0, y - mTimeBar.getPreferredHeight() - 80, w, y);
            mTimeBar.layout(pl, y - mTimeBar.getPreferredHeight() - 80, w - pr,
                    y - mTimeBar.getBarHeight());
*/
           // mBackground.layout(0, y - mTimeBar.getPreferredHeight() - 100, w, y);
           // mTimeBar.layout(pl, y - mTimeBar.getPreferredHeight() - 100, w - pr,
           //         y - mTimeBar.getBarHeight()+20);
/*TIANYURD:yanghao modify for PROD102931495 end*/			
          //  mControllerRewindAndForwardExt.onLayout(0, width, y);
        //} else 
		{
		
	  	mBackground.layout(0, y - mTimeBar.getPreferredHeight()+4, w, y);
	    mTimeBar.layout(pl-8, y - mTimeBar.getPreferredHeight() -13, w - pr+20,
				   y - mTimeBar.getBarHeight()+90);
           // mBackground.layout(0, y - mTimeBar.getBarHeight()+200, w, y);
          //  mTimeBar.layout(pl, y - mTimeBar.getPreferredHeight(),
          //          w - pr - mScreenModeExt.getAddedRightPadding(), y);
        }
        /*TIANYURD:Lizhy 20150410 modify  for TOS3.0 end*/
        mScreenModeExt.onLayout(w, pr, y);
        // Put the play/pause/next/ previous button in the center of the screen
        layoutCenteredView(mPlayPauseReplayView, 0, 0, w, h);

        if (mMainView != null) {
            layoutCenteredView(mMainView, 0, 0, w, h);
        }
    }

    @Override
    protected void updateViews() {
        if (hidden) {
            return;
        }
        mBackground.setVisibility(View.VISIBLE);
        mTimeBar.setVisibility(View.VISIBLE);
        mPlayPauseReplayView.setImageResource(
                mState == State.PAUSED ? R.drawable.videoplayer_play :
                        mState == State.PLAYING ? R.drawable.videoplayer_pause :
                                R.drawable.videoplayer_reload);
        mScreenModeExt.onShow();
        if (enableRewindAndForward) {
            /*TIANYURD:Lizhy 20150325 remove this for videoplayer start*/
            //mControllerRewindAndForwardExt.onShow();
            /*TIANYURD:Lizhy 20150325 remove this for videoplayer end*/
        }
        /*TIANYURD:Lizhy 20150325 modify for videoplayer start*/
        if (!mOverlayExt.handleUpdateViews()) {
            mPlayPauseReplayView.setVisibility(
                    (mState != State.LOADING && mState != State.ERROR &&
                    !(mState == State.ENDED && !mCanReplay /*&& mBrightnessPanelView.getVisibility() != View.VISIBLE*/))
                            ? View.VISIBLE : View.GONE);
        }
        /*TIANYURD:Lizhy 20150325 modify  for videoplayer end*/
        requestLayout();
        if (LOG) {
            Log.v(TAG, "updateViews() state=" + mState + ", canReplay="
                    + mCanReplay);
        }
    }

    // TimeBar listener

    @Override
    public void onScrubbingStart() {
        cancelHiding();
        super.onScrubbingStart();
    }

    @Override
    public void onScrubbingMove(int time) {
        cancelHiding();
        super.onScrubbingMove(time);
    }

    @Override
    public void onScrubbingEnd(int time, int trimStartTime, int trimEndTime) {
        maybeStartHiding();
        super.onScrubbingEnd(time, trimStartTime, trimEndTime);
    }

    public void setScreenModeManager(ScreenModeManager manager) {
        mScreenModeManager = manager;
        if (mScreenModeManager != null) {
            mScreenModeManager.addListener(mScreenModeExt);
        }
        if (LOG) {
            Log.v(TAG, "setScreenModeManager(" + manager + ")");
        }
    }

    public void setDefaultScreenMode() {
        mScreenModeManager.setScreenMode(ScreenModeManager.SCREENMODE_BIGSCREEN);
    }

    public IContrllerOverlayExt getOverlayExt() {
        return mOverlayExt;
    }

    public IControllerRewindAndForward getControllerRewindAndForwardExt() {
        if (enableRewindAndForward) {
            return mControllerRewindAndForwardExt;
        }
        return null;
    }

    private class OverlayExtension implements IContrllerOverlayExt {
        private State mLastState;
        private String mPlayingInfo;
        // for pause feature
        private boolean mCanPause = true;
        private boolean mEnableScrubbing = false;
        // for only audio feature
        private boolean mAlwaysShowBottom;

        @Override
        public void showBuffering(boolean fullBuffer, int percent) {
            if (LOG) {
                Log.v(TAG, "showBuffering(" + fullBuffer + ", " + percent
                        + ") " + "lastState=" + mLastState + ", state=" + mState);
            }
            if (fullBuffer) {
                // do not show text and loading
                mTimeBar.setSecondaryProgress(percent);
                return;
            }
            if (mState == State.PAUSED || mState == State.PLAYING) {
                mLastState = mState;
            }
            if (percent >= 0 && percent < 100) { // valid value
                mState = State.BUFFERING;
                String text = "media controller buffering";
                mTimeBar.setInfo(text);
                showMainView(mLoadingView);
            } else if (percent == 100) {
                mState = mLastState;
                mTimeBar.setInfo(null);
                showMainView(mPlayPauseReplayView);// restore play pause state
            } else { // here to restore old state
                mState = mLastState;
                mTimeBar.setInfo(null);
            }
        }

        // set buffer percent to unknown value
        public void clearBuffering() {
            if (LOG) {
                Log.v(TAG, "clearBuffering()");
            }
            mTimeBar.setSecondaryProgress(TimeBar.UNKNOWN);
            showBuffering(false, TimeBar.UNKNOWN);
        }

        public void showReconnecting(int times) {
            clearBuffering();
            mState = State.RETRY_CONNECTING;
            int msgId = R.string.videoview_error_text_cannot_connect_retry;
            String text = getResources().getString(msgId, times);
            mTimeBar.setInfo(text);
            showMainView(mLoadingView);
            if (LOG) {
                Log.v(TAG, "showReconnecting(" + times + ")");
            }
        }

        public void showReconnectingError() {
            clearBuffering();
            mState = State.RETRY_CONNECTING_ERROR;

            String text = "can not connect to server";
            mTimeBar.setInfo(text);
            showMainView(mPlayPauseReplayView);
            if (LOG) {
                Log.v(TAG, "showReconnectingError()");
            }
        }

        public void setPlayingInfo(boolean liveStreaming) {
            int msgId;
            // TODO
            if (liveStreaming) {
                msgId = R.string.media_controller_live;
            } else {
                msgId = R.string.media_controller_playing;
            }
            mPlayingInfo = getResources().getString(msgId);
            if (LOG) {
                Log.v(TAG, "setPlayingInfo(" + liveStreaming
                        + ") playingInfo=" + mPlayingInfo);
            }
        }

        public void setCanPause(boolean canPause) {
            this.mCanPause = canPause;
            if (LOG) {
                Log.v(TAG, "setCanPause(" + canPause + ")");
            }
        }

        public void setCanScrubbing(boolean enable) {
            mEnableScrubbing = enable;
            mTimeBar.setScrubbing(enable);
            if (LOG) {
                Log.v(TAG, "setCanScrubbing(" + enable + ")");
            }
        }

        public void setBottomPanel(boolean alwaysShow, boolean foreShow) {
            mAlwaysShowBottom = alwaysShow;
            if (!alwaysShow) { // clear background
                setBackgroundDrawable(null);
                setBackgroundColor(Color.TRANSPARENT);
            } else {
                setBackgroundResource(R.drawable.media_default_bkg);
                if (foreShow) {
                    setVisibility(View.VISIBLE);
                }
            }
            if (LOG) {
                Log.v(TAG, "setBottomPanel(" + alwaysShow + ", " + foreShow
                        + ")");
            }
        }

        public boolean isPlayingEnd() {
            if (LOG) {
                Log.v(TAG, "isPlayingEnd() state=" + mState);
            }
            boolean end = false;
            if (State.ENDED == mState || State.ERROR == mState
                    || State.RETRY_CONNECTING_ERROR == mState) {
                end = true;
            }
            return end;
        }

        public boolean handleShowPlaying() {
            if (mState == State.BUFFERING) {
                mLastState = State.PLAYING;
                return true;
            }
            return false;
        }

        public boolean handleShowPaused() {
            mTimeBar.setInfo(null);
            if (mState == State.BUFFERING) {
                mLastState = State.PAUSED;
                return true;
            }
            return false;
        }

        public void onShowLoading() {
            // TODO
            int msgId = R.string.media_controller_connecting;
            String text = getResources().getString(msgId);
            mTimeBar.setInfo(text);
        }

        public void onShowEnded() {
            clearBuffering();
            mTimeBar.setInfo(null);
        }

        public void onShowErrorMessage(String message) {
            clearBuffering();
        }

        public boolean handleUpdateViews() {
            mPlayPauseReplayView
                    .setVisibility((mState != State.LOADING
                            && mState != State.ERROR
                            && mState != State.BUFFERING
                            && mState != State.RETRY_CONNECTING && !(mState != State.ENDED
                            && mState != State.RETRY_CONNECTING_ERROR && !mCanPause))
                            // for live streaming
                            ? View.VISIBLE
                            : View.GONE);

            if (mPlayingInfo != null && mState == State.PLAYING) {
                mTimeBar.setInfo(mPlayingInfo);
            }
            return true;
        }

        public boolean handleHide() {
            return mAlwaysShowBottom;
        }

        public void onShowMainView(View view) {
            if (LOG) {
                Log.v(TAG, "showMainView(" + view + ") errorView="
                        + mErrorView + ", loadingView=" + mLoadingView
                        + ", playPauseReplayView=" + mPlayPauseReplayView);
                Log.v(TAG, "showMainView() enableScrubbing="
                        + mEnableScrubbing + ", state=" + mState);
            }
            if (mEnableScrubbing
                    && (mState == State.PAUSED || mState == State.PLAYING)) {
                mTimeBar.setScrubbing(true);
            } else {
                mTimeBar.setScrubbing(false);
            }
        }

        public boolean canHidePanel() {
            return !mAlwaysShowBottom;
        }

        /*TIANYURD:Lizhy 20150326 add for videoplayer start*/
        public void setHidePanel(boolean mFlag) {
            mAlwaysShowBottom = mFlag;
        }
        /*TIANYURD:Lizhy 20150326 add for videoplayer end*/
    };

    class ScreenModeExt implements View.OnClickListener, ScreenModeListener {
        // for screen mode feature
        private ImageView mScreenView;
        private int mScreenPadding;
        private int mScreenWidth;

        private static final int MARGIN = 10; // dip
        private ViewGroup mParent;
        private ImageView mSeprator;

        void init(Context context, View myTimeBar) {
            DisplayMetrics metrics = context.getResources().getDisplayMetrics();
            int padding = (int) (metrics.density * MARGIN);
            /*TIANYURD:Lizhy 20150410 modify for TOS3.0 start*/
            //myTimeBar.setPadding(padding, 0, padding, 0);
            myTimeBar.setPadding(36, 0, 120, 0);
            /*TIANYURD:Lizhy 20150410 modify for TOS3.0 end*/
            LayoutParams wrapContent =
                    new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
            // add screenView
            mScreenView = new ImageView(context);
            // default next screen mode
            mScreenView.setImageResource(R.drawable.ic_media_fullscreen);
            mScreenView.setScaleType(ScaleType.CENTER);
            mScreenView.setFocusable(true);
            mScreenView.setClickable(true);
            mScreenView.setOnClickListener(this);
            /*TIANYURD:Lizhy 20150410 add for TOS3.0 start*/
            //mScreenView.setPadding(36,0,45,80);
            mScreenView.setPadding(36,40,45,36);
            /*TIANYURD:Lizhy 20150410 add for TOS3.0 end*/
            addView(mScreenView, wrapContent);

            if (enableRewindAndForward) {
            /*TIANYURD:Lizhy 20150410 remove for TOS3.0 start*/
            /*
                Log.v(TAG, "ScreenModeExt enableRewindAndForward");
                mSeprator = new ImageView(context);
                // default next screen mode
                mSeprator.setImageResource(R.drawable.ic_separator_line);
                mSeprator.setScaleType(ScaleType.CENTER);
                mSeprator.setFocusable(true);
                mSeprator.setClickable(true);
                mSeprator.setOnClickListener(this);
                addView(mSeprator, wrapContent);
                */
            /*TIANYURD:Lizhy 20150410 remove for TOS3.0 end*/
            } else {
                Log.v(TAG, "ScreenModeExt unenableRewindAndForward");
            }

            // for screen layout
            Bitmap screenButton = BitmapFactory.decodeResource(context.getResources(),
                    R.drawable.ic_media_bigscreen);
            mScreenWidth = screenButton.getWidth();
            mScreenPadding = (int) (metrics.density * MARGIN);
            screenButton.recycle();
        }

        private void updateScreenModeDrawable() {
            int screenMode = mScreenModeManager.getNextScreenMode();
            if (screenMode == ScreenModeManager.SCREENMODE_BIGSCREEN) {
                mScreenView.setImageResource(R.drawable.ic_media_bigscreen);
            } else if (screenMode == ScreenModeManager.SCREENMODE_FULLSCREEN) {
                mScreenView.setImageResource(R.drawable.ic_media_fullscreen);
            } else {
                mScreenView.setImageResource(R.drawable.ic_media_cropscreen);
            }
        }

        @Override
        public void onClick(View v) {
            if (v == mScreenView && mScreenModeManager != null) {
                mScreenModeManager.setScreenMode(mScreenModeManager
                        .getNextScreenMode());
                show();
            }
        }

        public void onStartHiding() {
            startHideAnimation(mScreenView);
        }

        public void onCancelHiding() {
            mScreenView.setAnimation(null);
        }

        public void onHide() {
            mScreenView.setVisibility(View.INVISIBLE);
            /*TIANYURD:Lizhy 20150410 remove for TOS3.0 start*/
			/*
            if (enableRewindAndForward) {
                mSeprator.setVisibility(View.INVISIBLE);
            }
            */
            /*TIANYURD:Lizhy 20150410 remove for TOS3.0 end*/
        }

        public void onShow() {
            mScreenView.setVisibility(View.VISIBLE);
            /*TIANYURD:Lizhy 20150410 remove for TOS3.0 start*/
            //if (enableRewindAndForward) {
            //    mSeprator.setVisibility(View.VISIBLE);
            //}
            /*TIANYURD:Lizhy 20150410 remove for TOS3.0 end*/
        }

        public void onLayout(int width, int paddingRight, int yPosition) {
            // layout screen view position
            int sw = getAddedRightPadding();
            mScreenView.layout(width - paddingRight - sw, yPosition
                    - mTimeBar.getPreferredHeight(), width - paddingRight,
                    yPosition);
			
            /*TIANYURD:Lizhy 20150410 remove for TOS3.0 start*/
			/*
            if (enableRewindAndForward) {
                mSeprator.layout(width - paddingRight - sw - 22, yPosition
                        - mTimeBar.getPreferredHeight(), width - paddingRight - sw - 20,
                        yPosition);
            }
            */
            /*TIANYURD:Lizhy 20150410 remove for TOS3.0 end*/
        }

        public int getAddedRightPadding() {
            return mScreenPadding * 2 + mScreenWidth;
        }

        @Override
        public void onScreenModeChanged(int newMode) {
            updateScreenModeDrawable();
        }
    }

    class ControllerRewindAndForwardExt implements View.OnClickListener,
            IControllerRewindAndForward {
        private LinearLayout mContollerButtons;
        private ImageView mStop;
        private ImageView mForward;
        private ImageView mRewind;
        private IRewindAndForwardListener mListenerForRewind;
        private int mButtonWidth;
        private static final int BUTTON_PADDING = 40;
        private int mTimeBarHeight = 0;

        void init(Context context) {
            Log.v(TAG, "ControllerRewindAndForwardExt init");
            mTimeBarHeight = mTimeBar.getPreferredHeight();
            Bitmap button = BitmapFactory.decodeResource(context.getResources(),
                    R.drawable.ic_menu_forward);
            mButtonWidth = button.getWidth();
            button.recycle();

            mContollerButtons = new LinearLayout(context);
            LinearLayout.LayoutParams wrapContent = new LinearLayout.LayoutParams(
                    getAddedRightPadding(), mTimeBarHeight);
            mContollerButtons.setHorizontalGravity(LinearLayout.HORIZONTAL);
            mContollerButtons.setVisibility(View.VISIBLE);
            mContollerButtons.setGravity(Gravity.CENTER);

            LinearLayout.LayoutParams buttonParam = new LinearLayout.LayoutParams(
                    mTimeBarHeight, mTimeBarHeight);
            mRewind = new ImageView(context);
            mRewind.setImageResource(R.drawable.icn_media_rewind);
            mRewind.setScaleType(ScaleType.CENTER);
            mRewind.setFocusable(true);
            mRewind.setClickable(true);
            mRewind.setOnClickListener(this);
            mContollerButtons.addView(mRewind, buttonParam);

            mStop = new ImageView(context);
            mStop.setImageResource(R.drawable.icn_media_stop);
            mStop.setScaleType(ScaleType.CENTER);
            mStop.setFocusable(true);
            mStop.setClickable(true);
            mStop.setOnClickListener(this);
            LinearLayout.LayoutParams stopLayoutParam = new LinearLayout.LayoutParams(
                    mTimeBarHeight, mTimeBarHeight);
/*TIANYURD:yanghao modify for CQWB00014192 start*/            
            //stopLayoutParam.setMargins(BUTTON_PADDING, 0, BUTTON_PADDING, 0);
            stopLayoutParam.setMargins(0, 0, 0, 0);
/*TIANYURD:yanghao modify for CQWB00014192 end*/
            mContollerButtons.addView(mStop, stopLayoutParam);

            mForward = new ImageView(context);
            mForward.setImageResource(R.drawable.icn_media_forward);
            mForward.setScaleType(ScaleType.CENTER);
            mForward.setFocusable(true);
            mForward.setClickable(true);
            mForward.setOnClickListener(this);
            mContollerButtons.addView(mForward, buttonParam);
            addView(mContollerButtons, wrapContent);
        }

        @Override
        public void onClick(View v) {
            if (v == mStop) {
                Log.v(TAG, "ControllerRewindAndForwardExt onClick mStop");
                mListenerForRewind.onStopVideo();
            } else if (v == mRewind) {
                Log.v(TAG, "ControllerRewindAndForwardExt onClick mRewind");
                mListenerForRewind.onRewind();
            } else if (v == mForward) {
                Log.v(TAG, "ControllerRewindAndForwardExt onClick mForward");
                mListenerForRewind.onForward();
            }
        }

        public void onStartHiding() {
            Log.v(TAG, "ControllerRewindAndForwardExt onStartHiding");
            startHideAnimation(mContollerButtons);
        }

        public void onCancelHiding() {
            Log.v(TAG, "ControllerRewindAndForwardExt onCancelHiding");
            mContollerButtons.setAnimation(null);
        }

        public void onHide() {
            Log.v(TAG, "ControllerRewindAndForwardExt onHide");
            mContollerButtons.setVisibility(View.INVISIBLE);
        }

        public void onShow() {
            Log.v(TAG, "ControllerRewindAndForwardExt onShow");
            mContollerButtons.setVisibility(View.VISIBLE);
        }
        public void onLayout(int l, int r, int b) {
            Log.v(TAG, "ControllerRewindAndForwardExt onLayout");
            int cl = (r - l - getAddedRightPadding()) / 2;
            int cr = cl + getAddedRightPadding();
            mContollerButtons.layout(cl, b - mTimeBar.getPreferredHeight(), cr, b);
        }

        public int getAddedRightPadding() {
            return mTimeBarHeight * 3 + BUTTON_PADDING * 2;
        }

        @Override
        public void setIListener(IRewindAndForwardListener listener) {
            Log.v(TAG, "ControllerRewindAndForwardExt setIListener " + listener);
            mListenerForRewind = listener;
        }

        @Override
        public void showControllerButtonsView(boolean canStop, boolean canRewind, boolean canForward) {
            Log.v(TAG, "ControllerRewindAndForwardExt showControllerButtonsView " + canStop
                    + canRewind + canForward);
            // show ui
            mStop.setEnabled(canStop);
            mRewind.setEnabled(canRewind);
            mForward.setEnabled(canForward);
        }

        @Override
        public void setListener(Listener listener) {
            setListener(listener);
        }

        @Override
        public boolean getPlayPauseEanbled() {
            return mPlayPauseReplayView.isEnabled();
        }

        @Override
        public boolean getTimeBarEanbled() {
            return mTimeBar.getScrubbing();
        }

        @Override
        public void setCanReplay(boolean canReplay) {
            setCanReplay(canReplay);
        }

        @Override
        public View getView() {
            return mContollerButtons;
        }

        @Override
        public void show() {
            show();
        }

        @Override
        public void showPlaying() {
            showPlaying();
        }

        @Override
        public void showPaused() {
            showPaused();
        }

        @Override
        public void showEnded() {
            showEnded();
        }

        @Override
        public void showLoading() {
            showLoading();
        }

        @Override
        public void showErrorMessage(String message) {
            showErrorMessage(message);
        }

        public void setTimes(int currentTime, int totalTime, int trimStartTime, int trimEndTime) {
            setTimes(currentTime, totalTime, 0, 0);
        }

        public void setPlayPauseReplayResume() {
        }

        public void setViewEnabled(boolean isEnabled) {
            // TODO Auto-generated method stub
            Log.v(TAG, "ControllerRewindAndForwardExt setViewEnabled is " + isEnabled);
            mRewind.setEnabled(isEnabled);
            mForward.setEnabled(isEnabled);
        }

        /*TIANYURD:Lizhy 20150325 add for videoplayer start*/
        public void onForward(){
			mListenerForRewind.onForward();
        }

        public void onRewind(){
			mListenerForRewind.onRewind();
        }

        /*TIANYURD:Lizhy 20150325 add for videoplayer emd*/
    }

	/*TIANYURD:Lizhy 20150323 add for videoplayer start*/
	private class MyGestureListener extends GestureDetector.SimpleOnGestureListener{
         
	    @Override
		public boolean onDown(MotionEvent event) {
			Log.d("--Lizhy--","--the gestureListener onTouchEvent--");

			 //switch(event.getAction()){
             // case MotionEvent.ACTION_DOWN:
			mPlayPauseReplayView.setVisibility(View.GONE);
			//mTimeBar.setVisibility(View.VISIBLE);
			return true;
		}
	
		@Override
    	public boolean onScroll(MotionEvent e1, MotionEvent e2,
			float distanceX, float distanceY) {

            float mOldX = e1.getX(), mOldY = e1.getY();
            float mNewX = e2.getX(), mNewY = e2.getY();
		
            Display  mDsp = ((MovieActivity) mContext).getWindowManager().getDefaultDisplay();
            int width = mDsp.getWidth();
            int height = mDsp.getHeight();

            if(mNewX - mOldX > 100 && mNewY - mOldY <20){
			   tyShowVideoForwardAndRewind(true);
            }else if(mOldX - mNewX > 100 && mOldY - mNewY <20){
			   tyShowVideoForwardAndRewind(false);
            }else if((mOldY - mNewY)>100 /*&& (mNewX - mOldX < 20) */
                     && mNewX < width/2 && mOldX < width/2){
              //modify brightness
			  mOverlayExt.setHidePanel(false);
              startHiding();
			  Log.d("--Lizhy--","--the brightness brighter--");
			  tyShowBrightnessSlider(0.1f);
            }else if((mNewY - mOldY)>100 /*&& (mNewX - mOldX < 20) */
                     && mNewX < width/2 && mOldX < width/2){
              //modify brightness
              mOverlayExt.setHidePanel(false);
              startHiding();
			  Log.d("--Lizhy--","--the brightness lower--");
              tyShowBrightnessSlider(-0.1f);
			}else if((mOldY - mNewY)>100 && (mNewX - mOldX < 20) 
                     && mNewX > width/2 && mOldX > width/2){
              //modify volumn
			  mOverlayExt.setHidePanel(false);
              startHiding();
			  tyShowMediaVolumnSlider(true);
            }else if((mNewY - mOldY)>100 && (mNewX - mOldX < 20) 
                     && mNewX > width/2 && mOldX > width/2){
				//modify volumn
			  mOverlayExt.setHidePanel(false);
			  startHiding();
			  tyShowMediaVolumnSlider(false);
            }
		
            return false;
	    }


        public void tyShowVideoForwardAndRewind(boolean isFlag){
            if(isFlag){
               mControllerRewindAndForwardExt.onForward();
            }else{
               mControllerRewindAndForwardExt.onRewind();
            }
        }

		public void tyShowMediaVolumnSlider(boolean isFlag){
            if(isFlag){
               audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC,
				  AudioManager.ADJUST_RAISE,AudioManager.FLAG_SHOW_UI);
            }else{
               audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC,
				  AudioManager.ADJUST_LOWER,AudioManager.FLAG_SHOW_UI);
            }
		}


		public void tyShowBrightnessSlider(float percent){
			Log.d("--Lizhy--","--the Brightness slider---");
			Log.d("--Lizhy--","--the mBrightness === = "+ mBrightness);
			float mBrightValue;
			if(mBrightness < 0){
              mBrightness = ((MovieActivity)mContext).getWindow().getAttributes().screenBrightness;
			  if(mBrightness <= 0.00f){
			  	mBrightness = 0.50f;
			  }
			  if(mBrightness <= 0.01f){
				mBrightness = 0.01f;
			  }
			}
			mBrightValue = mBrightness*1000;
			mSeekBar.setProgress((int)mBrightValue);
			showMainView(mBrightnessPanelView);
            if(mBrightnessPanelView.getVisibility() == View.VISIBLE){
              mPlayPauseReplayView.setVisibility(View.GONE);
            }
			Log.d("--Lizhy--","--@@@@ the mBrightness = "+ mBrightness);
			WindowManager.LayoutParams attrs = ((MovieActivity)mContext).getWindow().getAttributes();
			attrs.screenBrightness = mBrightness + percent;
			Log.d("--Lizhy--","--the Brightness is = "+ attrs.screenBrightness);
			if(attrs.screenBrightness > 1.0f){
				attrs.screenBrightness = 1.0f;
				mBrightValue = attrs.screenBrightness*1000;
				mSeekBar.setProgress((int)mBrightValue);
			}else if (attrs.screenBrightness < 0.01f){
				attrs.screenBrightness = 0.01f;
				mSeekBar.setProgress(0);
 			}
			mBrightness = attrs.screenBrightness;
		   ((MovieActivity)mContext).getWindow().setAttributes(attrs);


		}
      }
}
