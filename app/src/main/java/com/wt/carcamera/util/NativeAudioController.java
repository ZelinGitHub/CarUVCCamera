package com.wt.carcamera.util;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.util.Log;

import com.wt.carcamera.R;

import java.io.IOException;
import java.util.HashMap;


/**
 * @ Created by Zelin on 2021/2/4.
 */
public class NativeAudioController implements AudioController {
    private String TAG = "NativeAudioController";
    private AudioManager audioManager;
    private AudioFocusRequest audioFocusRequest;
    private Context mContext;
    private SoundPool mSoundPool;
    private HashMap<Integer, Integer> mSoundMap = new HashMap<>(2);

    public NativeAudioController(Context context) {
        mContext = context;
        initAudioManager(context);
        initSoundPool(context);
    }

    private void initSoundPool(Context context) {
        SoundPool.Builder builder = new SoundPool.Builder();
        //传入最多播放音频数量,
        builder.setMaxStreams(1);
        //AudioAttributes是一个封装音频各种属性的方法
        AudioAttributes.Builder attrBuilder = new AudioAttributes.Builder();
        //设置音频流的合适的属性
        attrBuilder.setLegacyStreamType(AudioManager.STREAM_MUSIC);
        //加载一个AudioAttributes
        builder.setAudioAttributes(attrBuilder.build());
        mSoundPool = builder.build();
        //异步需要等待加载完成，音频才能播放成功
        mSoundPool.setOnLoadCompleteListener((soundPool, sampleId, status) -> {
                    Log.i(TAG, "sound pool load complete sampleId=" + sampleId + ",status=" + status);
                    if (status == 0) {
                        mSoundPool.play(sampleId, 1, 1, 1, 0, 1);
                    }
                }
        );
    }

    public void initAudioManager(Context context) {
        audioManager = context.getSystemService(AudioManager.class);
        audioFocusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                .setWillPauseWhenDucked(true)
                .setAcceptsDelayedFocusGain(false)
                .setAudioAttributes(
                        new AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_MEDIA)
                                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                .build()
                )
                .setOnAudioFocusChangeListener(new AudioManager.OnAudioFocusChangeListener() {
                    @Override
                    public void onAudioFocusChange(int focusChange) {

                    }
                })
                .build();
    }

    private boolean requestAudioFocus() {
        int ret = audioManager.requestAudioFocus(audioFocusRequest);
        Log.i(TAG, "requestAudioFocus ret=" + ret);
        return ret == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
    }


    private void abandonAudioFocus() {
        audioManager.abandonAudioFocusRequest(audioFocusRequest);
    }

    @Override
    public void playSound(int rawId) {
        //第一个参数soundID
        //第二个参数leftVolume为左侧音量值（范围= 0.0到1.0）
        //第三个参数rightVolume为右的音量值（范围= 0.0到1.0）
        //第四个参数priority 为流的优先级，值越大优先级高，影响当同时播放数量超出了最大支持数时SoundPool对该流的处理
        //第五个参数loop 为音频重复播放次数，0为值播放一次，-1为无限循环，其他值为播放loop+1次
        //第六个参数 rate为播放的速率，范围0.5-2.0(0.5为一半速率，1.0为正常速率，2.0为两倍速率)
        if (!mSoundMap.containsKey(rawId)) {
            int valueId = mSoundPool.load(mContext, rawId, 1);
            Log.i(TAG, "put value=" + valueId);
            mSoundMap.put(rawId, valueId);
        } else {
            Integer soundId = mSoundMap.get(rawId);
            if (soundId == null) {
                Log.i(TAG, "not found sound id,raw id=" + rawId);
                return;
            }
            mSoundPool.play(soundId, 1, 1, 1, 0, 1);
        }
    }


    @Override
    public void playShutter(Context context) {
        if (!requestAudioFocus()) {
            return;
        }
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        audioManager.playSoundEffect(AudioManager.FX_KEYPRESS_INVALID);
        AudioAttributes attributes = new AudioAttributes.Builder().setLegacyStreamType(AudioManager.STREAM_SYSTEM).build();
        AssetFileDescriptor fileDescriptor = context.getResources().openRawResourceFd(R.raw.shuuter);
        MediaPlayer mediaPlayer = new MediaPlayer();
        try {
            mediaPlayer.setAudioAttributes(attributes);
            mediaPlayer.setDataSource(fileDescriptor);
            mediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                @Override
                public boolean onError(MediaPlayer mp, int what, int extra) {
                    releaseAudioFocus(fileDescriptor, mp);
                    return false;
                }
            });
            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    releaseAudioFocus(fileDescriptor, mp);
                }
            });
            mediaPlayer.prepare();
            mediaPlayer.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void releaseAudioFocus(AssetFileDescriptor file, MediaPlayer mp) {
        try {
            if (mp != null) {
                mp.stop();
                mp.release();
            }
            if (file != null) {
                file.close();
            }
            abandonAudioFocus();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @Override
    public int getIdentifyId() {
        return GOOGLE_NATIVE;
    }
}
