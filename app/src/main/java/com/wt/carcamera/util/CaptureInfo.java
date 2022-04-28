package com.wt.carcamera.util;

import android.text.TextUtils;

import androidx.annotation.NonNull;

/**
 * @ Created by Zelin on 2021/8/9.
 */
public class CaptureInfo {

    private String path;

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }


    public boolean isVideo() {
        return !TextUtils.isEmpty(path) && path.toLowerCase().endsWith(".mp4");
    }

    @NonNull
    @Override
    public String toString() {
        return "CaptureInfo{" +
                "path='" + path + '\'' +
                ",isVideo=" + isVideo() +
                '}';
    }
}
