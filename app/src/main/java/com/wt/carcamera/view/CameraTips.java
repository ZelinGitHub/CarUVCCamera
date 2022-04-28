package com.wt.carcamera.view;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;

import androidx.constraintlayout.widget.ConstraintLayout;

import com.wt.carcamera.R;


/**
 * @ Created by Zelin on 2021/8/2.
 */
public class CameraTips extends ConstraintLayout {

    public static final int PREPARING = 0;
    public static final int DISCONNECTED = 2;
    public static final int NONE = 3;
    private int currentState = NONE;

    public CameraTips(Context context) {
        this(context,null);
    }

    public CameraTips(Context context, AttributeSet attrs) {
        this(context, attrs,0);
    }

    public CameraTips(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }


    public void showErrorTips(int error) {
        removeAllViews();
        if (currentState == error) {
            Log.w("CameraErrorTips", "ignore ,currentState:" + currentState + "  error:" + error);
            return;
        }
        currentState = error;
        switch (error) {
            case PREPARING:
                displayLoading();
                break;
            case DISCONNECTED:
                displayDisconnected();
                break;
        }
    }

    //正在加载中
    private void displayLoading() {
        LayoutInflater.from(getContext()).inflate(R.layout.layout_camera_preparing_tips, this);
    }

    private void displayDisconnected() {
        LayoutInflater.from(getContext()).inflate(R.layout.layout_camera_disconnected_tips, this);
    }

}
