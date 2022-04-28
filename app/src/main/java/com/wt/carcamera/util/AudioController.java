package com.wt.carcamera.util;

import android.content.Context;

import androidx.annotation.RawRes;

/**
 * @ Created by Zelin on 2021/3/10.
 */
public interface AudioController extends Identify {
    /**
     * 播放
     */
    void playSound(@RawRes int resID);

    void playShutter(Context context);
}
