package com.wt.carcamera.view;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatTextView;

import com.wt.carcamera.R;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 播放倒数计时3-2-1的文本控件
 * @ Created by Zelin on 2021/8/6.
 */
public class CountDownTimerNow extends AppCompatTextView {

    private static final String TAG = "CountDownTimer2";

    private AnimatorSet mCounterAnimator;
    private Callback callback;


    private final AtomicBoolean isStopByCancel = new AtomicBoolean(false);
    private int mCounter = 0;

    public CountDownTimerNow(@NonNull Context context) {
        this(context, null);
    }

    public CountDownTimerNow(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, -1);
    }

    public CountDownTimerNow(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setTextColor(getResources().getColor(R.color.wt_white, null));
        setTextSize(TypedValue.COMPLEX_UNIT_PX, 228);
        setGravity(Gravity.CENTER);
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return true;
    }


    public void start(int repeatCount) {
        if (!isShown() || repeatCount <= 0) return;
        mCounter = repeatCount;
        if (mCounterAnimator == null) {
            mCounterAnimator = new AnimatorSet();
            ObjectAnimator scaleX = ObjectAnimator.ofFloat(this, "scaleX", 0, 1f);
            scaleX.setRepeatMode(ObjectAnimator.RESTART);
            scaleX.setRepeatCount(repeatCount - 1);
            scaleX.addListener(mRingCallback);
            ObjectAnimator scaleY = ObjectAnimator.ofFloat(this, "scaleY", 0, 1f);
            scaleY.setRepeatMode(ObjectAnimator.RESTART);
            scaleY.setRepeatCount(repeatCount);
            ObjectAnimator mNumberAlpha = ObjectAnimator.ofFloat(this, "alpha", 1f, 0f);
            mNumberAlpha.setRepeatMode(ObjectAnimator.RESTART);
            mNumberAlpha.setRepeatCount(repeatCount - 1);
            mCounterAnimator.setDuration(1000);
            mCounterAnimator.playTogether(scaleX, scaleY, mNumberAlpha);
        }
        mCounterAnimator.start();
        if (callback != null) callback.onCountDownStart();
    }

    public void cancel() {
        if (isStopByCancel.compareAndSet(false, true)) {
            if (mCounterAnimator != null) {
                mCounterAnimator.cancel();
                mCounterAnimator = null;
            }
        }

    }


    public void setCallback(Callback callback) {
        this.callback = callback;
    }


    AnimatorListenerAdapter mRingCallback = new AnimatorListenerAdapter() {
        @Override
        public void onAnimationStart(Animator animation) {
            super.onAnimationStart(animation);
            Log.i(TAG, String.format("onAnimationStart"));
            isStopByCancel.set(false);
            //显示倒计时数字
            setText(String.valueOf(mCounter));
        }

        @Override
        public void onAnimationEnd(Animator animation) {
            super.onAnimationEnd(animation);
            setVisibility(GONE);
            boolean cancel = isStopByCancel.get();
            Log.i(TAG, String.format("onAnimationEnd isStopByCancel = %s", cancel));
            if (callback != null) callback.onCountDownComplete(cancel);
            mCounterAnimator = null;
        }

        @Override
        public void onAnimationRepeat(Animator animation) {
            super.onAnimationRepeat(animation);
            //倒计时数字减小
            mCounter--;
            //显示倒计时数字
            setText(String.valueOf(mCounter));
            Log.i(TAG, String.format("onAnimationRepeat = %s", mCounter));
        }
    };


    public interface Callback {
        void onCountDownStart();

        void onCountDownComplete(boolean byCancel);
    }

}
