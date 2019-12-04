package com.walixiwa.aplayer;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.aplayer.APlayerAndroid;
import com.walixiwa.aplayer.tools.PositionManager;
import com.walixiwa.player.R;

import java.lang.ref.WeakReference;

import me.jessyan.autosize.AutoSizeCompat;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

public class APlayerActivity extends AppCompatActivity implements View.OnClickListener {
    private APlayerAndroid aPlayer = null;
    private SeekBar seekBar;
    private TextView mTvPosition;
    private TextView mTvDuration;
    private TextView mTvInfo;
    private ImageView mIvPlay;
    private TextView mTvTitle;
    private LinearLayout mLlHeaderBar;
    private LinearLayout mLlCtrlBar;
    private TextView mTvSubTitle;
    private TextView mTvSpeed;
    private LinearLayout mLlCaching;
    private ImageView mIvLock;
    private ImageView mIvRotate;

    private Handler mainUIHandler;
    private boolean mIsNeedUpdateUIProgress = false;        /* 标志位，是否更新UI */
    private boolean mIsTouchingSeekBar = false;        /* 标志位,是否在滑动进度 */

    private boolean mIsSystemCallPause = false;
    private TextView cachingProgressHint = null;

    private int totalPosition;
    private int currentPosition;
    private int currentMovePosition;
    private float pressedX;
    private float pressedY;
    private float movedY;
    private int currentBrightness;
    private int currentVolume;
    private int pressedBrightness;
    private int pressedVolume;
    private int maxVolume;
    private int action;
    private AudioManager audioManager;
    private boolean locker = false;
    private int width;
    private int height;

    private String url = "";

    private boolean isLive;

    private int resultCode = -1;

    private boolean showToast = false;

    private int position = 0;

/*    @Override
    public View onCreateView(View parent, String name, Context context, AttributeSet attrs) {
        return super.onCreateView(parent, name, context, attrs);
    }

    @Override
    public View onCreateView(String name, Context context, AttributeSet attrs) {
        return super.onCreateView(name, context, attrs);
    }*/

    @Override
    public Resources getResources() {
        //需要升级到 v1.1.2 及以上版本才能使用 AutoSizeCompat
        AutoSizeCompat.autoConvertDensityOfGlobal(super.getResources());//如果没有自定义需求用这个方法
        AutoSizeCompat.autoConvertDensity(super.getResources(), 420, false);//如果有自定义需求就用这个方法
        return super.getResources();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_aplayer);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        initAPlayer();
        hideBottomUIMenu();
        Intent intent = getIntent();
        resultCode = getIntent().getIntExtra("resultCode", -1);
        mTvTitle.setText(intent.getStringExtra("title"));
        url = intent.getStringExtra("url");
        isLive = intent.getBooleanExtra("isLive", false);
        showToast = intent.getBooleanExtra("showToast", false);
        position = intent.getIntExtra("position", 0);
        aPlayer.open(url);
        //setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);    /* 强制为横屏 */
    }


    private void initAPlayer() {
        SurfaceView holderView = findViewById(R.id.holderView);
        mLlHeaderBar = findViewById(R.id.header_bar);
        mLlCtrlBar = findViewById(R.id.ctrl_bar);
        mTvSubTitle = findViewById(R.id.tv_sub_title);
        mLlCaching = findViewById(R.id.caching);
        mIvPlay = findViewById(R.id.v_play);
        mIvPlay.setOnClickListener(this);
        findViewById(R.id.v_back).setOnClickListener(this);
        mIvRotate = findViewById(R.id.v_rotate);
        mIvRotate.setOnClickListener(this);
        mIvLock = findViewById(R.id.v_player_lock);
        mIvLock.setOnClickListener(this);
        cachingProgressHint = findViewById(R.id.loading_text);

        mTvSpeed = findViewById(R.id.tv_speed);
        //初始化滑动手势配置
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        this.width = displayMetrics.widthPixels;
        this.height = displayMetrics.heightPixels;

        this.audioManager = ((AudioManager) getSystemService(AUDIO_SERVICE));
        if (audioManager != null) {
            int j = this.audioManager.getStreamMaxVolume(3);
            this.maxVolume = j;
            this.maxVolume *= 6;
        }
        float f = this.currentVolume * 6;
        try {
            int k = Settings.System.getInt(getContentResolver(), "screen_brightness");
            f = 1.0F * k / 255.0F;
        } catch (Settings.SettingNotFoundException localSettingNotFoundException) {
            localSettingNotFoundException.printStackTrace();
        }
        this.currentBrightness = ((int) (f * 100.0F));
        this.pressedBrightness = ((int) (f * 100.0F));

        seekBar = findViewById(R.id.seekbar);
        mTvPosition = findViewById(R.id.tv_position);
        mTvDuration = findViewById(R.id.tv_duration);
        mTvInfo = findViewById(R.id.tv_info);
        mTvTitle = findViewById(R.id.v_title);
        aPlayer = new APlayerAndroid();
        aPlayer.setView(holderView);
        aPlayer.setOnOpenCompleteListener(b -> {
            if (b) {
                if (position != 0) {
                    aPlayer.setPosition(position);
                } else {
                    if (!isLive) {
                        int position = PositionManager.getInstance(this).getPosition(url);
                        if (position > 0) {
                            aPlayer.setPosition(position);
                        }
                    }
                }
                aPlayer.play();
            } else {
                mLlCaching.setVisibility(GONE);
            }
        });

        aPlayer.setOnOpenProgressListener(new APlayerAndroid.OnOpenProgressListener() {
            @Override
            public void onOpenProgress(int progress) {
                super.onOpenProgress(progress);
                int visibility = (progress == 100) ? View.INVISIBLE : VISIBLE;
                mLlCaching.setVisibility(visibility);
                String buff = "正在打开视频：" + progress + "%";
                cachingProgressHint.setText(buff);
            }
        });

        aPlayer.setOnPlayCompleteListener(s -> {
            switch (s) {
                case APlayerAndroid.PlayCompleteRet.PLAYRE_RESULT_COMPLETE:
                    if (!isLive) {
                        PositionManager.getInstance(this).delete(url);
                    }
                    url = null;
                    finishPlay();
                    break;
                case APlayerAndroid.PlayCompleteRet.PLAYRE_RESULT_CLOSE:
                    finishPlay();
                    break;
                case APlayerAndroid.PlayCompleteRet.PLAYRE_RESULT_OPENRROR:
                    if (showToast) {
                        Toast.makeText(APlayerActivity.this, "视频打开失败!", Toast.LENGTH_LONG).show();
                    }
                    break;
                case APlayerAndroid.PlayCompleteRet.PLAYRE_RESULT_DECODEERROR:
                    if (showToast) {
                        Toast.makeText(APlayerActivity.this, "视频解码失败!", Toast.LENGTH_LONG).show();
                    }
                    break;
                case APlayerAndroid.PlayCompleteRet.PLAYRE_RESULT_HARDDECODERROR:
                    if (showToast) {
                        Toast.makeText(APlayerActivity.this, "硬件解码失败!", Toast.LENGTH_LONG).show();
                    }
                    break;
                case APlayerAndroid.PlayCompleteRet.PLAYRE_RESULT_SEEKERROR:
                    if (showToast) {
                        Toast.makeText(APlayerActivity.this, "视频Seek失败!", Toast.LENGTH_LONG).show();
                    }
                    break;
                case APlayerAndroid.PlayCompleteRet.PLAYRE_RESULT_READEFRAMERROR:
                    if (showToast) {
                        Toast.makeText(APlayerActivity.this, "读取内存失败!", Toast.LENGTH_LONG).show();
                    }
                    break;
                case APlayerAndroid.PlayCompleteRet.PLAYRE_RESULT_CREATEGRAPHERROR:
                    if (showToast) {
                        Toast.makeText(APlayerActivity.this, "图形解码失败!", Toast.LENGTH_LONG).show();
                    }
                    break;
            }
        });

        aPlayer.setOnBufferListener(i -> {
            int visibility = (i == 100) ? View.INVISIBLE : VISIBLE;
            mLlCaching.setVisibility(visibility);
            String strProgress = "缓冲: " + i + "%";
            cachingProgressHint.setText(strProgress);
        });

        aPlayer.setOnPlayStateChangeListener((i, i1) -> {
            switch (i) {
                case APlayerAndroid.PlayerState.APLAYER_CLOSEING:
                    mLlCaching.setVisibility(VISIBLE);
                    cachingProgressHint.setText("正在关闭...");
                    break;
                case APlayerAndroid.PlayerState.APLAYER_PAUSED:
                case APlayerAndroid.PlayerState.APLAYER_PLAYING:
                    mLlCaching.setVisibility(GONE);
                    break;
                case APlayerAndroid.PlayerState.APLAYER_PAUSING:
                    mLlCaching.setVisibility(VISIBLE);
                    cachingProgressHint.setText("正在暂停...");
                    break;
                case APlayerAndroid.PlayerState.APLAYER_PLAY:
                case APlayerAndroid.PlayerState.APLAYER_READ:
                case APlayerAndroid.PlayerState.APLAYER_RESET:
                    break;
            }
            if (i == APlayerAndroid.PlayerState.APLAYER_PLAYING) {
                mIvPlay.setImageResource(R.drawable.v_play_pause);
                startUIUpdateThread();
            } else {
                mIvPlay.setImageResource(R.drawable.v_play_arrow);
                stopUIUpdateThread();
            }
            Log.e("info", "preState:" + i1 + " >> State:" + i);
        });

        aPlayer.setOnShowSubtitleListener(s -> mTvSubTitle.setText(s));

        aPlayer.setOnSystemPlayerFailListener(() -> {
            aPlayer.setConfig(APlayerAndroid.CONFIGID.HW_DECODER_USE, "1");
        });

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (null == aPlayer || !fromUser) {
                    return;
                }
                mIsTouchingSeekBar = true;
                userSeekPlayProgress(progress, seekBar.getMax());
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                mTvInfo.setVisibility(GONE);
                mIsTouchingSeekBar = false;
                startUIUpdateThread();
            }
        });

        mTvSpeed.setOnClickListener(v -> {
            if (currentSpeedIndex == 6) {
                currentSpeedIndex = 0;
            } else {
                currentSpeedIndex += 1;
            }
            aPlayer.setConfig(APlayerAndroid.CONFIGID.PLAY_SPEED, String.valueOf(getSpeedFromIndex(currentSpeedIndex)));

        });
        mainUIHandler = new MyHandler(this);
    }

    private int currentSpeedIndex = 2;

    private int getSpeedFromIndex(int index) {
        int ret = 0;
        if (index == 0) {
            ret = 50;
            mTvSpeed.setText("0.5倍速");
        } else if (index == 1) {
            ret = 75;
            mTvSpeed.setText("0.75倍速");
        } else if (index == 2) {
            ret = 100;
            mTvSpeed.setText("1.0倍速");
        } else if (index == 3) {
            ret = 125;
            mTvSpeed.setText("1.25倍速");
        } else if (index == 4) {
            ret = 150;
            mTvSpeed.setText("1.5倍速");
        } else if (index == 5) {
            ret = 175;
            mTvSpeed.setText("1.75倍速");
        } else if (index == 6) {
            ret = 200;
            mTvSpeed.setText("2.0倍速");
        }
        return ret;
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.v_play) {
            if (aPlayer != null) {
                if (aPlayer.getState() == APlayerAndroid.PlayerState.APLAYER_PLAYING) {
                    mIvPlay.setImageResource(R.drawable.v_play_arrow);
                    aPlayer.pause();
                } else {
                    mIvPlay.setImageResource(R.drawable.v_play_pause);
                    aPlayer.play();
                }
            }
        } else if (v.getId() == R.id.v_back) {
            finishPlay();
        } else if (v.getId() == R.id.v_rotate) {
            Configuration mConfiguration = this.getResources().getConfiguration();               /* 获取设置的配置信息 */
            int ori = mConfiguration.orientation;                           /* 获取屏幕方向 */
            if (ori == Configuration.ORIENTATION_LANDSCAPE) {
                /* 横屏 */
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);            /* 强制为竖屏 */
            } else if (ori == Configuration.ORIENTATION_PORTRAIT) {
                /* 竖屏 */
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);    /* 强制为横屏 */
            }
        } else if (v.getId() == R.id.v_player_lock) {
            if (!locker) {
                locker = true;
                hideCtrlBar();
                mIvLock.setImageResource(R.drawable.v_player_locked);
            } else {
                mIvLock.setImageResource(R.drawable.v_player_unlocked);
                locker = false;
                showBars();
            }
        }
    }

    private void hideCtrlBar() {
        Animation animation = AnimationUtils.loadAnimation(APlayerActivity.this, R.anim.anim_top_out);
        mLlHeaderBar.startAnimation(animation);
        mLlHeaderBar.setVisibility(GONE);
        Animation animation2 = AnimationUtils.loadAnimation(APlayerActivity.this, R.anim.anim_bottom_out);
        mLlCtrlBar.startAnimation(animation2);
        mLlCtrlBar.setVisibility(GONE);
        Animation animation3 = AnimationUtils.loadAnimation(APlayerActivity.this, R.anim.anim_left_out);
        mIvLock.startAnimation(animation3);
        mIvLock.setVisibility(GONE);

        mIvRotate.startAnimation(animation2);
        mIvRotate.setVisibility(GONE);
    }

    static class MyHandler extends Handler {
        WeakReference<APlayerActivity> mActivity;

        MyHandler(APlayerActivity activity) {
            mActivity = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            APlayerActivity activity = mActivity.get();
            if (msg.what == 1) {
                activity.mTvDuration.setText(updateTextViewWithTimeFormat(msg.arg2));
                activity.mTvPosition.setText(updateTextViewWithTimeFormat(msg.arg1));
                if (msg.arg1 > 0 && msg.arg2 >= 0) {
                    activity.seekBar.setMax(msg.arg2);
                    activity.seekBar.setProgress(msg.arg1);
                } else {
                    activity.seekBar.setProgress(0);
                }
            }
        }
    }

    private Thread mUpdateThread = null;

    private void startUIUpdateThread() {
        if (null == mUpdateThread) {
            mIsNeedUpdateUIProgress = true;
            mUpdateThread = new Thread(new UpdatePlayUIProcess());
            mUpdateThread.start();
        } else {
            Log.e("info", "null != mUpdateThread");
        }
    }

    private void stopUIUpdateThread() {
        mIsNeedUpdateUIProgress = false;
        mUpdateThread = null;
    }

    private class UpdatePlayUIProcess implements Runnable {
        @Override
        public void run() {
            while (mIsNeedUpdateUIProgress) {
                if (!mIsTouchingSeekBar) {
                    int currentPlayTime = 0;
                    int durationTime = 0;
                    if (null != aPlayer) {
                        currentPlayTime = aPlayer.getPosition();
                        durationTime = aPlayer.getDuration();
                    }
                    Message msg = mainUIHandler.obtainMessage(1, currentPlayTime / 1000, durationTime / 1000);
                    mainUIHandler.sendMessage(msg);
                }
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Log.e("info", e.toString());
                }
            }
        }
    }

    private static String updateTextViewWithTimeFormat(int i) {
        int i2 = (i % 3600) / 60;
        int i3 = i % 60;
        if (i / 3600 != 0) {
            String format = String.format("%02d:%02d:%02d", Integer.valueOf(i / 3600), Integer.valueOf(i2), Integer.valueOf(i3));
            return format;
        }
        String format = String.format("%02d:%02d", Integer.valueOf(i2), Integer.valueOf(i3));
        return format;
    }

    private boolean isOverSeekGate(int seekBarPositionMs, int currentPlayPosMs) {
        final int SEEK_MIN_GATE_MS = 1000;
        return Math.abs(currentPlayPosMs - seekBarPositionMs) > SEEK_MIN_GATE_MS;
    }

    private void userSeekPlayProgress(int seekPositionMs, int max) {
        int currentPlayPos = aPlayer.getPosition();
        boolean isChangeOverSeekGate = isOverSeekGate(seekPositionMs * 1000, currentPlayPos);
        if (!isChangeOverSeekGate) {
            /* 避免拖动粒度过细，拖动时频繁定位影响体验 */
            return;
        }
        mIsTouchingSeekBar = true;
        stopUIUpdateThread();
        seekBar.setProgress(seekPositionMs);
        mTvInfo.setText(String.format("%s/%s", updateTextViewWithTimeFormat(seekPositionMs), updateTextViewWithTimeFormat(max)));
        mTvInfo.setVisibility(VISIBLE);
        aPlayer.setPosition(seekPositionMs * 1000);
    }

    @Override
    public boolean onTouchEvent(MotionEvent motionEvent) {
        float x = motionEvent.getX();
        float y = motionEvent.getY();
        float f;
        switch (motionEvent.getAction()) {
            case MotionEvent.ACTION_DOWN:
                this.totalPosition = this.aPlayer.getDuration() / 1000;
                this.currentPosition = this.aPlayer.getPosition() / 1000;
                this.pressedX = x;
                this.pressedY = y;
                this.action = 1;
                this.currentVolume = this.audioManager.getStreamVolume(3);
                this.pressedVolume = (this.currentVolume * 6);
                this.movedY = y;
                break;
            case MotionEvent.ACTION_UP:
                new Handler().postDelayed(() -> {
                    mTvInfo.setVisibility(GONE);
                    switch (action) {
                        case 2:
                            if (aPlayer.getDuration() > 5 && !locker) {
                                aPlayer.setPosition(currentMovePosition * 1000);
                            }
                            break;
                        case 3:
                            pressedBrightness = currentBrightness;
                            break;
                        case 4:
                            break;
                        default:
                            onClickEmptyArea();
                            break;
                    }
                }, 100L);

            case MotionEvent.ACTION_MOVE:
                if (!locker) {
                    f = Math.abs(x - this.pressedX);
                    float abs = Math.abs(y - this.pressedY);
                    if (this.action == 1) {
                        if (f > 50.0f && abs < 50.0f) {
                            this.action = 2;
                        }
                        if (f < 50.0f && abs > 50.0f && ((double) this.pressedX) < ((double) this.width) * 0.25d) {
                            this.action = 3;
                        }
                        if (f < 50.0f && abs > 50.0f && ((double) this.pressedX) > ((double) this.width) * 0.75d) {
                            this.action = 4;
                        }
                    }
                    switch (this.action) {
                        case 2:
                            this.currentMovePosition = (int) ((float) ((((double) (((x - this.pressedX) / ((float) this.width)) * ((float) this.totalPosition))) * 0.3d) + ((double) this.currentPosition)));
                            if (this.currentMovePosition < 0) {
                                this.currentMovePosition = 0;
                            }
                            if (this.currentMovePosition > this.totalPosition) {
                                this.currentMovePosition = this.totalPosition;
                            }
                            this.mTvInfo.setVisibility(VISIBLE);
                            this.mTvInfo.setText(String.format("%s/%s", updateTextViewWithTimeFormat(this.currentMovePosition), updateTextViewWithTimeFormat(this.totalPosition)));
                            break;
                        case 3:
                            float f6 = (y - this.movedY) * 100.0F / this.height;

                            this.currentBrightness = (this.pressedBrightness - (int) f6);
                            if (this.currentBrightness > 100) {
                                this.currentBrightness = 100;
                            }
                            if (this.currentBrightness < 7) {
                                this.currentBrightness = 7;
                            }
                            this.mTvInfo.setVisibility(VISIBLE);
                            int j = (this.currentBrightness - 7) * 100 / 93;
                            this.mTvInfo.setText("亮度：" + j + "%");

                            setBrightness(this.currentBrightness);
                            break;
                        case 4:
                            float f7 = (y - this.movedY) * 100.0F / this.height;

                            int currentVolumeM = (this.pressedVolume - (int) f7);
                            if (currentVolumeM > this.maxVolume) {
                                currentVolumeM = this.maxVolume;
                            }
                            if (currentVolumeM < 0) {
                                currentVolumeM = 0;
                            }
                            this.mTvInfo.setVisibility(VISIBLE);
                            int k = currentVolumeM * 100 / this.maxVolume;
                            this.mTvInfo.setText("音量：" + k + "%");
                            int m = currentVolumeM / 6;
                            this.audioManager.setStreamVolume(3, m, 0);
                            break;
                        default:
                            break;
                    }
                }
                break;
        }
        return (true);
    }

    private void onClickEmptyArea() {
        if (locker) {
            if (mIvLock.getVisibility() != VISIBLE) {
                mIvLock.setVisibility(VISIBLE);
                Animation animation3 = AnimationUtils.loadAnimation(APlayerActivity.this, R.anim.anim_left_in);
                mIvLock.startAnimation(animation3);
            } else {
                Animation animation3 = AnimationUtils.loadAnimation(APlayerActivity.this, R.anim.anim_left_out);
                mIvLock.startAnimation(animation3);
                mIvLock.setVisibility(GONE);
            }
            return;
        }
        if (mLlHeaderBar.getVisibility() == GONE) {
            showBars();
        } else {
            hideBottomUIMenu();
            hideCtrlBar();
        }
    }

    private void showBars() {
        mLlHeaderBar.setVisibility(VISIBLE);
        Animation animation = AnimationUtils.loadAnimation(APlayerActivity.this, R.anim.anim_top_in);
        mLlHeaderBar.startAnimation(animation);

        mLlCtrlBar.setVisibility(VISIBLE);
        Animation animation2 = AnimationUtils.loadAnimation(APlayerActivity.this, R.anim.anim_bottom_in);
        mLlCtrlBar.startAnimation(animation2);

        mIvLock.setVisibility(VISIBLE);
        Animation animation3 = AnimationUtils.loadAnimation(APlayerActivity.this, R.anim.anim_left_in);
        mIvLock.startAnimation(animation3);

        mIvRotate.setVisibility(VISIBLE);
        mIvRotate.startAnimation(animation2);
    }

    public void setBrightness(int paramInt) {
        if (paramInt < 0) {
            paramInt = 0;
        }
        if (paramInt > 100) {
            paramInt = 100;
        }
        WindowManager.LayoutParams localLayoutParams = this.getWindow().getAttributes();
        localLayoutParams.screenBrightness = (1.0F * paramInt / 100.0F);
        this.getWindow().setAttributes(localLayoutParams);
        this.currentBrightness = paramInt;
    }

    /**
     * 隐藏虚拟按键，并且全屏
     */
    protected void hideBottomUIMenu() {
        /* 隐藏虚拟按键，并且全屏 */
        if (Build.VERSION.SDK_INT < 19) {
            View v = this.getWindow().getDecorView();
            v.setSystemUiVisibility(GONE);
        } else {
            View decorView = getWindow().getDecorView();
            int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_FULLSCREEN;
            decorView.setSystemUiVisibility(uiOptions);
        }
    }

    @Override
    protected void onPause() {
        int statue = aPlayer.getState();
        if (isPlay(statue)) {
            aPlayer.pause();
            mIsSystemCallPause = true;
        }
        if (url != null && !isLive) {
            PositionManager.getInstance(this).addPosition(url, aPlayer.getPosition());
        }
        /* 让播放进度UI更新线程退出 */
        stopUIUpdateThread();
        super.onPause();
    }

    @Override
    protected void onResume() {
        /* 普通播放，后台切换回来，恢复之前的播放状态 */
        if (mIsSystemCallPause) {
            aPlayer.play();
            mIsSystemCallPause = false;
        }
        startUIUpdateThread();
        super.onResume();
    }

    private void finishPlay() {
        if (url != null && !isLive) {
            PositionManager.getInstance(this).addPosition(url, aPlayer.getPosition());
        }
        aPlayer.close();
        aPlayer.destroy();
        setResult(resultCode);
        finish();
    }

    private boolean isPlay(int status) {
        return APlayerAndroid.PlayerState.APLAYER_PLAY == status || APlayerAndroid.PlayerState.APLAYER_PLAYING == status;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        this.width = displayMetrics.widthPixels;
        this.height = displayMetrics.heightPixels;
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            //AutoSize.autoConvertDensityOfGlobal(this);
        }
    }
}
