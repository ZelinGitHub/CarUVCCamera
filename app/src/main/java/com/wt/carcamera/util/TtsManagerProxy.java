package com.wt.carcamera.util;

import android.content.Context;

import com.tencent.tts.client.TtsManager;

/**
 * @Description：
 * @ Created by Zelin on 2021/8/9.
 */
public class TtsManagerProxy {

    public static void init(Context context) {
        TtsManager.getInstance().init(context);
    }

    public static void tts(String text) {
        TtsManager.getInstance().ttsSpeak(text);
    }
}
