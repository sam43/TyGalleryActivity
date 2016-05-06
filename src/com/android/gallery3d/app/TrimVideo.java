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

package com.android.gallery3d.app;

import android.app.ActionBar;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import com.android.gallery3d.R;
import com.android.gallery3d.util.SaveVideoFileInfo;
import com.android.gallery3d.util.SaveVideoFileUtils;

import java.io.File;
import java.io.IOException;
//TY liuyuchuan add begin for PROD103707450
import android.view.WindowManager;
import android.graphics.drawable.ColorDrawable;
//TY liuyuchuan add begin for PROD103707450

public class TrimVideo extends Activity implements
        MediaPlayer.OnErrorListener,
        MediaPlayer.OnCompletionListener,
        ControllerOverlay.Listener {

    private VideoView mVideoView;
    private TextView mSaveVideoTextView;
    private TrimControllerOverlay mController;
    private Context mContext;
    private Uri mUri;
    private final Handler mHandler = new Handler();
    public static final String TRIM_ACTION = "com.android.camera.action.TRIM";

    public ProgressDialog mProgress;
    /*TIANYURD:Lizhy 20150605 modify for PROD103743382 start*/
    private static int mTrimStartTime = 0;
    private static int mTrimEndTime = 0;
    private boolean mRepalyFlag = false;
    /*TIANYURD:Lizhy 20150605 modify for PROD103743382 end*/
/*TIANYURD:yanghao modify for PROD102932557 start*/
    private boolean mCheckTrimStartTime;
/*TIANYURD:yanghao modify for PROD102932557 end*/

    /*TIANYURD:Lizhy 20150605 modify for PROD103743382 start*/
    private static int mVideoPosition = 0;
    /*TIANYURD:Lizhy 20150605 modify for PROD103743382 end*/
    public static final String KEY_TRIM_START = "trim_start";
    public static final String KEY_TRIM_END = "trim_end";
    public static final String KEY_VIDEO_POSITION = "video_pos";
    private boolean mHasPaused = false;

    private String mSrcVideoPath = null;
    private static final String TIME_STAMP_NAME = "'TRIM'_yyyyMMdd_HHmmss";
    private SaveVideoFileInfo mDstFileInfo = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        mContext = getApplicationContext();
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_ACTION_BAR);
        requestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
        //TY liuyuchuan add begin for PROD103707450
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        //TY liuyuchuan add end for PROD103707450

        ActionBar actionBar = getActionBar();
/*TAINYURD:yanghao mofify for PROD103076856 start*/
/*
        int displayOptions = ActionBar.DISPLAY_SHOW_HOME;
        actionBar.setDisplayOptions(0, displayOptions);
        displayOptions = ActionBar.DISPLAY_SHOW_CUSTOM;
*/
        int displayOptions = ActionBar.DISPLAY_SHOW_CUSTOM;
/*TAINYURD:yanghao mofify for PROD103076856 end*/        
        actionBar.setDisplayOptions(displayOptions, displayOptions);
        actionBar.setCustomView(R.layout.trim_menu);
        //TY liuyuchuan add begin for PROD103707450
        actionBar.setBackgroundDrawable(new ColorDrawable(
                getResources().getColor(R.color.trim_video_actionbar_color)));
        //TY liuyuchuan add end for PROD103707450

        mSaveVideoTextView = (TextView) findViewById(R.id.start_trim);
        mSaveVideoTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                trimVideo();
            }
        });
        mSaveVideoTextView.setEnabled(false);

        Intent intent = getIntent();
        mUri = intent.getData();
        mSrcVideoPath = intent.getStringExtra(PhotoPage.KEY_MEDIA_ITEM_PATH);
        setContentView(R.layout.trim_view);
        View rootView = findViewById(R.id.trim_view_root);

        mVideoView = (VideoView) rootView.findViewById(R.id.surface_view);

        mController = new TrimControllerOverlay(mContext);
        ((ViewGroup) rootView).addView(mController.getView());
        mController.setListener(this);
        mController.setCanReplay(true);

        mVideoView.setOnErrorListener(this);
        mVideoView.setOnCompletionListener(this);
        mVideoView.setVideoURI(mUri);

        playVideo();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mHasPaused) {
            mVideoView.seekTo(mVideoPosition);
            mVideoView.resume();
            mHasPaused = false;
        }
        mHandler.post(mProgressChecker);
    }

    @Override
    public void onPause() {
        mHasPaused = true;
        mHandler.removeCallbacksAndMessages(null);
        mVideoPosition = mVideoView.getCurrentPosition();
        mVideoView.suspend();
        super.onPause();
    }

    @Override
    public void onStop() {
        if (mProgress != null) {
            mProgress.dismiss();
            mProgress = null;
        }
        /*TIANYURD:Lizhy 20150605 add for PROD103743382 start*/
        mTrimStartTime = 0;
        mTrimEndTime = 0;
        mVideoPosition = 0;
        /*TIANYURD:Lizhy 20150605 add for PROD103743382 end*/
        super.onStop();
    }

    @Override
    public void onDestroy() {
        mVideoView.stopPlayback();
        super.onDestroy();
    }

    private final Runnable mProgressChecker = new Runnable() {
        @Override
        public void run() {
            int pos = setProgress();
            mHandler.postDelayed(mProgressChecker, 200 - (pos % 200));
        }
    };

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putInt(KEY_TRIM_START, mTrimStartTime);
        savedInstanceState.putInt(KEY_TRIM_END, mTrimEndTime);
        savedInstanceState.putInt(KEY_VIDEO_POSITION, mVideoPosition);
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        mTrimStartTime = savedInstanceState.getInt(KEY_TRIM_START, 0);
        mTrimEndTime = savedInstanceState.getInt(KEY_TRIM_END, 0);
        mVideoPosition = savedInstanceState.getInt(KEY_VIDEO_POSITION, 0);
/*TIANYURD:yanghao modify for PROD102932557 start*/
        mSaveVideoTextView.setEnabled(isModified());
/*TIANYURD:yanghao modify for PROD102932557 end*/
    }

    // This updates the time bar display (if necessary). It is called by
    // mProgressChecker and also from places where the time bar needs
    // to be updated immediately.
    private int setProgress() {
        mVideoPosition = mVideoView.getCurrentPosition();
        // If the video position is smaller than the starting point of trimming,
        // correct it.
/*TIANYURD:yanghao modify for PROD102932557 start*/
/*
        if (mVideoPosition < mTrimStartTime) {
            mVideoView.seekTo(mTrimStartTime);
            mVideoPosition = mTrimStartTime;
        }
*/
        /*TIANYURD:Lizhy 20150609 modify for PROD103743382 start*/
        //if (mCheckTrimStartTime && (mVideoPosition < mTrimStartTime)) {
        if ((mCheckTrimStartTime || mRepalyFlag) && (mVideoPosition < mTrimStartTime)) {	
            mVideoPosition = mTrimStartTime;
            mVideoView.seekTo(mTrimStartTime);
            if(mRepalyFlag){
              mRepalyFlag = false;
            }
        }
        mCheckTrimStartTime = false;
/*TIANYURD:yanghao modify for PROD102932557 end*/
        // If the position is bigger than the end point of trimming, show the
        // replay button and pause.
        //if (mVideoPosition >= mTrimEndTime && mTrimEndTime > 0) {
        if ( (mTrimEndTime-mVideoPosition)<= 200 && mTrimEndTime > 0) {
            //TIANYU hanpengzhe 20150706 modify for PROD103857104 begin
            //if (mVideoPosition >= mTrimEndTime) {
            if (mVideoPosition > mTrimEndTime) {
            //TIANYU hanpengzhe 20150706 modify for PROD103857104 end
                mVideoView.seekTo(mTrimEndTime);
                mVideoPosition = mTrimEndTime;
            }
            mController.showEnded();
            mVideoView.pause();
        }
        /*TIANYURD:Lizhy 20150609 modify for PROD103743382 end*/
		
        int duration = mVideoView.getDuration();
        if (duration > 0 && mTrimEndTime == 0) {
            mTrimEndTime = duration;
        }
        mController.setTimes(mVideoPosition, duration, mTrimStartTime, mTrimEndTime);
        // Enable save if there's modifications
        mSaveVideoTextView.setEnabled(isModified());
        return mVideoPosition;
    }

    private void playVideo() {
        mVideoView.start();
/*TIANYURD:yanghao modify for PROD102932557 start*/
        mCheckTrimStartTime = true;
/*TIANYURD:yanghao modify for PROD102932557 end*/
        mController.showPlaying();
        setProgress();
    }

    private void pauseVideo() {
        mVideoView.pause();
        mController.showPaused();
    }


    private boolean isModified() {
        int delta = mTrimEndTime - mTrimStartTime;

        // Considering that we only trim at sync frame, we don't want to trim
        // when the time interval is too short or too close to the origin.
        if (delta < 100 || Math.abs(mVideoView.getDuration() - delta) < 100) {
            return false;
        } else {
            return true;
        }
    }

    private void trimVideo() {

        mDstFileInfo = SaveVideoFileUtils.getDstMp4FileInfo(TIME_STAMP_NAME,
                getContentResolver(), mUri, getString(R.string.folder_download));
        final File mSrcFile = new File(mSrcVideoPath);

        showProgressDialog();

        new Thread(new Runnable() {
            @Override
            public void run() {
                boolean hasError = false;
                try {
                    VideoUtils.startTrim(mSrcFile, mDstFileInfo.mFile,
                            mTrimStartTime, mTrimEndTime);
                    // Update the database for adding a new video file.
                    SaveVideoFileUtils.insertContent(mDstFileInfo,
                            getContentResolver(), mUri);
                } catch (IOException e) {
                    hasError = true;
                    e.printStackTrace();
                } catch (IllegalStateException e) {
                    hasError = true;
                    e.printStackTrace();
                }
                //If the exception happens,just notify the UI and avoid the crash.
                if (hasError){
                    mHandler.post(new Runnable(){
                        @Override
                        public void run(){
                            Toast.makeText(getApplicationContext(),
                                getString(R.string.fail_trim),
                                Toast.LENGTH_SHORT)
                                .show();
                            if (mProgress != null) {
                                mProgress.dismiss();
                                mProgress = null;
                            }
                        }
                    });
                    return;
                }
                // After trimming is done, trigger the UI changed.
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        //TY xumw 20160119 modify "save_into" to "saved_success" for PROD104153559
                        Toast.makeText(getApplicationContext(),
                            getString(R.string.saved_success/*save_into*/, mDstFileInfo.mFolderName),
                            Toast.LENGTH_SHORT)
                            .show();
                        // TODO: change trimming into a service to avoid
                        // this progressDialog and add notification properly.
                        if (mProgress != null) {
                            mProgress.dismiss();
                            mProgress = null;
                            //TY zhencc 20150618 delete for PROD103805868 begin
                            // Show the result only when the activity not stopped.
                            /*Intent intent = new Intent(android.content.Intent.ACTION_VIEW);
                            intent.setDataAndType(Uri.fromFile(mDstFileInfo.mFile), "video/*");
                            intent.putExtra(MediaStore.EXTRA_FINISH_ON_COMPLETION, false);
                            startActivity(intent);
                            finish();*/
                            //TY zhencc 20150618 delete for PROD103805868 end
                        }
                    }
                });
            }
        }).start();
    }

    private void showProgressDialog() {
        // create a background thread to trim the video.
        // and show the progress.
        mProgress = new ProgressDialog(this);
        mProgress.setTitle(getString(R.string.trimming));
        mProgress.setMessage(getString(R.string.please_wait));
        // TODO: make this cancelable.
        mProgress.setCancelable(false);
        mProgress.setCanceledOnTouchOutside(false);
        mProgress.show();
    }

    @Override
    public void onPlayPause() {
        if (mVideoView.isPlaying()) {
            pauseVideo();
        } else {
            playVideo();
        }
    }

    @Override
    public void onSeekStart() {
        pauseVideo();
    }

    @Override
    public void onSeekMove(int time) {
        mVideoView.seekTo(time);
    }

    @Override
    public void onSeekEnd(int time, int start, int end) {
        mVideoView.seekTo(time);
        mTrimStartTime = start;
        mTrimEndTime = end;
        setProgress();
        // Enable save if there's modifications
/*TIANYURD:yanghao modify for PROD102932557 start*/
        mSaveVideoTextView.setEnabled(isModified());
/*TIANYURD:yanghao modify for PROD102932557 end*/        
    }

    @Override
    public void onShown() {
    }

    @Override
    public void onHidden() {
    }

    @Override
    public boolean onIsRTSP() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void onReplay() {
        /*TIANYURD:Lizhy 20150605 add for PROD103743382 start*/
        mRepalyFlag = true;
        /*TIANYURD:Lizhy 20150605 add for PROD103743382 end*/
        mVideoView.seekTo(mTrimStartTime);
        playVideo();
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        mController.showEnded();
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        return false;
    }
}
