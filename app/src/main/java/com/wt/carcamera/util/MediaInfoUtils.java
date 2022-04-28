package com.wt.carcamera.util;

import android.text.TextUtils;

/**
 * @Description：
 * @ Created by Zelin on 2021/8/10.
 */
public class MediaInfoUtils {

    public static int getMediaTypeByPath(String path) {
        if (!TextUtils.isEmpty(path) && path.toLowerCase().endsWith(".mp4")) {
            return Constant.MediaType.VIDEO;
        } else {
            return Constant.MediaType.IMAGE;
        }
    }
}
