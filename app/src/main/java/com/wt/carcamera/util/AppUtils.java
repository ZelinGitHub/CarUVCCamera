package com.wt.carcamera.util;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.View;

import java.io.File;
import java.io.FileInputStream;
import java.security.MessageDigest;
import java.util.Locale;

public class AppUtils {

    private static final String TAG = "AppUtils";
    private static final String PHOTO_CAMERA_START = "PHOTO_CARMERA_START";
    private static final String PHOTO_CAMERA_STOP = "PHOTO_CARMERA_STOP";

    public AppUtils() {
    }
    //隐藏状态栏
    public static void hideStatusBar(View decorView) {
        int visibility = View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        decorView.setSystemUiVisibility(visibility);
        Log.i(TAG, "The status bar is hidden!");
    }

    public static String getAppName(Context context, String packageName) {
        try {
            PackageManager packageManager = context.getPackageManager();
            PackageInfo packageInfo = packageManager.getPackageInfo(packageName, 0);
            ApplicationInfo applicationInfo = packageInfo.applicationInfo;
            return applicationInfo.loadLabel(packageManager).toString();
//            int res = packageInfo.applicationInfo.labelRes;
//            return context.getResources().getString(res);
        } catch (Exception exception) {
            Log.e(TAG, "getAppName : ", exception);
            return null;
        }
    }

    public static String getVersionName(Context con) {
        try {
            PackageManager packageManager = con.getPackageManager();
            PackageInfo packageInfo = packageManager.getPackageInfo(con.getPackageName(), 0);
            return packageInfo.versionName;
        } catch (Exception exception) {
            Log.e(TAG, "getVersionName : ", exception);
            return null;
        }
    }

    public static int getVersionCode(Context context) {
        try {
            PackageManager packageManager = context.getPackageManager();
            PackageInfo packageInfo = packageManager.getPackageInfo(context.getPackageName(), 0);
            return packageInfo.versionCode;
        } catch (Exception exception) {
            Log.e(TAG, "getVersionCode : ", exception);
            return 0;
        }
    }

    public static String getPackageName(Context context) {
        return context.getPackageName();
    }

    public static Bitmap getBitmap(Context context) {
        PackageManager packageManager = null;
        ApplicationInfo info = null;

        try {
            packageManager = context.getApplicationContext().getPackageManager();
            info = packageManager.getApplicationInfo(context.getPackageName(), 0);
            Drawable drawable = packageManager.getApplicationIcon(info);
            BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
            return bitmapDrawable.getBitmap();
        } catch (Exception exception) {
            Log.e(TAG, "getBitmap : ", exception);
            return null;
        }
    }


    public static String getFileMD5(File file) {
        if (!file.isFile()) {
            return null;
        }
        MessageDigest digest = null;
        FileInputStream in = null;
        byte buffer[] = new byte[1024];
        int len;
        try {
            digest = MessageDigest.getInstance("MD5");
            in = new FileInputStream(file);
            while ((len = in.read(buffer, 0, 1024)) != -1) {
                digest.update(buffer, 0, len);
            }
            in.close();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return bytesToHexString(digest.digest());
    }

    public static String bytesToHexString(byte[] src) {
        StringBuilder stringBuilder = new StringBuilder("");
        if (src == null || src.length <= 0) {
            return null;
        }
        for (int i = 0; i < src.length; i++) {
            int v = src[i] & 0xFF;
            String hv = Integer.toHexString(v);
            if (hv.length() < 2) {
                stringBuilder.append(0);
            }
            stringBuilder.append(hv);
        }
        return stringBuilder.toString();
    }


    public static String getSHA1Signature(Context context) {
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), PackageManager.GET_SIGNATURES);
            byte[] bytes = packageInfo.signatures[0].toByteArray();
            MessageDigest sha1 = MessageDigest.getInstance("SHA1");
            byte[] digest = sha1.digest(bytes);
            StringBuilder stringBuilder = new StringBuilder();
            int length = digest.length;

            for (int i = 0; i < length; ++i) {
                String str = Integer.toHexString(255 & digest[i]).toUpperCase(Locale.US);
                if (str.length() == 1) {
                    stringBuilder.append("0");
                }

                stringBuilder.append(str);
                if (i < length - 1) {
                    stringBuilder.append(":");
                }
            }

            return stringBuilder.toString();
        } catch (Exception exception) {
            Log.e(TAG, "getSHA1Signature : ", exception);
            return null;
        }
    }

    //top是否是当前应用
    public static boolean isAppPreferred(Context context, String packageName) {
        return getTopPackageName(context).equals(packageName);
    }

    public static String getTopPackageName(Context context) {
        String topActivity = "";
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if (activityManager == null) {
            return "";
        }
        ComponentName cn = activityManager.getRunningTasks(1).get(0).topActivity;
        topActivity = cn.getPackageName();
        Log.i("mmmm", "11111top running app is : " + topActivity);
        return topActivity;
    }




}
