package com.example.sunwoo.blackbox;

import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.Timer;
import java.util.TimerTask;

public class PlayVideo extends AppCompatActivity implements
        SeekBar.OnSeekBarChangeListener,
        MediaPlayer.OnBufferingUpdateListener,
        MediaPlayer.OnCompletionListener,
        MediaPlayer.OnPreparedListener,
        MediaPlayer.OnVideoSizeChangedListener,
        SurfaceHolder.Callback{

    private final Timer timer = new Timer();

    private int mVideoWidth;
    private int mVideoHeight;

    private boolean mIsVideoSizeKnown = false;
    private boolean mIsVideoReadyToBePlayed = false;

    private MediaPlayer mMediaPlayer;
    private SurfaceView mPreview;
    private String path;
    private SeekBar mSeekBar;

    private TextView info;
    private LayoutInflater controlInflater = null;

    private SQLiteDatabase myDB;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.playvideo);

        Intent intent = getIntent();
        path = intent.getStringExtra("video_path");
        Log.e("Path", path);

        mPreview = (SurfaceView) findViewById(R.id.videoview);
        mPreview.getHolder().addCallback(this);
        mPreview.getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        mSeekBar = (SeekBar) findViewById(R.id.seekbar);
        mSeekBar.setOnSeekBarChangeListener(this);

        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                int currentPosition = mMediaPlayer.getCurrentPosition();
                int duration = mMediaPlayer.getDuration();
                int per = (int) Math.floor((currentPosition * 100) / duration);

                mSeekBar.setProgress(per);

                if(per == 100)
                    timer.cancel();
            }
        }, 1000, 1000);

        controlInflater = LayoutInflater.from(getApplicationContext());
        View viewControl = controlInflater.inflate(R.layout.playvideo_control, null);
        ViewGroup.LayoutParams layoutParamsControl =
                new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT);
        this.addContentView(viewControl, layoutParamsControl);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        playVideo();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        mMediaPlayer.pause();

        int pos = mMediaPlayer.getDuration() * seekBar.getProgress() / 100;
        mMediaPlayer.seekTo(pos);
        mMediaPlayer.start();
    }

    private void playVideo() {
        doCleanUp();

        try {
            mMediaPlayer = new MediaPlayer();
            mMediaPlayer.setDataSource(path);
            mMediaPlayer.setDisplay(mPreview.getHolder());
            mMediaPlayer.prepare();
            mMediaPlayer.setOnBufferingUpdateListener(this);
            mMediaPlayer.setOnCompletionListener(this);
            mMediaPlayer.setOnPreparedListener(this);
            mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mMediaPlayer.setOnVideoSizeChangedListener(this);
        }
        catch (Exception e) {
        }
    }

    private void startVideoPlayback() {
        mPreview.getHolder().setFixedSize(mVideoWidth, mVideoHeight);
        mMediaPlayer.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if(mMediaPlayer != null) {
            mMediaPlayer.release();
            mMediaPlayer = null;
            timer.cancel();
        }
    }

    @Override
    public void onBufferingUpdate(MediaPlayer mp, int percent) {

    }

    @Override
    public void onCompletion(MediaPlayer mp) {

    }

    @Override
    public void onPrepared(MediaPlayer mediaPlayer) {
        mVideoWidth = mMediaPlayer.getVideoWidth();
        mVideoHeight = mMediaPlayer.getVideoHeight();

        if(mVideoWidth != 0 && mVideoHeight != 0) {
            mPreview.getHolder().setFixedSize(mVideoWidth, mVideoHeight);
            mMediaPlayer.start();
        }

        mIsVideoReadyToBePlayed = true;

        if(mIsVideoReadyToBePlayed && mIsVideoSizeKnown) {
            startVideoPlayback();
        }
    }

    @Override
    public void onVideoSizeChanged(MediaPlayer mp, int width, int height) {
        if(width == 0 || height == 0) {
            return;
        }

        mIsVideoSizeKnown = true;
        mVideoWidth = width;
        mVideoHeight = height;

        if(mIsVideoReadyToBePlayed && mIsVideoSizeKnown) {
            startVideoPlayback();
        }
    }

    private void doCleanUp() {
        mVideoWidth = 0;
        mVideoHeight = 0;
        mIsVideoReadyToBePlayed = false;
        mIsVideoSizeKnown = false;
    }
}
