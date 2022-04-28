package com.wt.carcamera.ui.activity;


import static com.wt.carcamera.util.AppUtils.hideStatusBar;
import static com.wt.carcamera.util.Constant.*;


import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.PointF;
import android.graphics.SurfaceTexture;
import android.hardware.usb.UsbDevice;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.RawRes;

import com.bumptech.glide.GenericTransitionOptions;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.transition.ViewPropertyTransition;
import com.serenegiant.common.BaseActivity;
import com.serenegiant.usb.DeviceFilter;
import com.serenegiant.usb.USBMonitor;
import com.serenegiant.usb.UVCCamera;
import com.serenegiant.usbcameracommon.OnCaptureListener;
import com.serenegiant.usbcameracommon.UVCCameraHandler;
import com.serenegiant.widget.CameraViewInterface;
import com.serenegiant.widget.UVCCameraTextureView;
import com.wt.carcamera.R;
import com.wt.carcamera.util.AudioController;
import com.wt.carcamera.util.Constant;
import com.wt.carcamera.util.NativeAudioController;
import com.wt.carcamera.util.OnNoDoubleClickListener;
import com.wt.carcamera.util.TtsManagerProxy;
import com.wt.carcamera.view.CameraTips;
import com.wt.carcamera.view.CountDownTimerNow;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @ Created by Zelin on 2022/4/25.
 */
public class MainActivity extends BaseActivity {
    private static final String TAG = "MainActivity";

//AbsUVCCameraHandler: supportedSize:{"formats":[{"index":1,"type":6,"default":1,"size":["1280x720","640x480","320x240"]},{"index":2,"type":4,"default":1,"size":["1280x720","640x480","320x240"]}]}
    /**
     * preview resolution(width)
     * if your camera does not support specific resolution and mode,
     * {@link UVCCamera#setPreviewSize(int, int, int)} throw exception
     */
    private static final int PREVIEW_WIDTH = 1280;
    /**
     * preview resolution(height)
     * if your camera does not support specific resolution and mode,
     * {@link UVCCamera#setPreviewSize(int, int, int)} throw exception
     */
    private static final int PREVIEW_HEIGHT = 720;
    /**
     * preview mode
     * if your camera does not support specific resolution and mode,
     * {@link UVCCamera#setPreviewSize(int, int, int)} throw exception
     * 0:YUYV, other:MJPEG
     */
    private static final int PREVIEW_MODE = UVCCamera.FRAME_FORMAT_MJPEG;

    /**
     * for accessing USB
     */
    private USBMonitor mUSBMonitor;
    /**
     * Handler to execute camera releated methods sequentially on private thread
     */
    private UVCCameraHandler mCameraHandler;
    /**
     * for camera preview display
     */
    private CameraViewInterface mUVCCameraView;
    //马甲
    private UVCCameraTextureView camera_view;

    private Surface mSurface;

    private CameraTips camera_error_tips;
    private ImageView iv_capture;
    private ImageView iv_to_album;
    private View v_cover;
    private ImageView iv_capture_anim;
    private ImageView iv_exit;
    //321动画控件
    private CountDownTimerNow cdtn_take_picture_counter;
    private String mPath;
    private final PointF mPointF = new PointF();
    private AnimatorSet mCaptureDownScaleAnimatorSet;
    //321 倒计时动画执行监听
    private final CountDownTimerNow.Callback mCountDownTimerCallback = new MyCountDownTimerNow();
    private final USBMonitor.OnDeviceConnectListener mOnDeviceConnectListener = new MyOnDeviceConnectListener();
    private final AtomicBoolean mIsCameraPrepared = new AtomicBoolean(false); /* 摄像头是否初始化完成 */
    private final AtomicBoolean mIsTakingPhoto = new AtomicBoolean(false); /*是否正在拍照中 */
    private AudioController mAudioController;
    public final AtomicBoolean mIsNeedTakePhotoNow = new AtomicBoolean(false); /* 是否需要在切换到拍照模式时，立刻触发拍照*/
    private MyHandler mMyHandler;

    //    private final CompositeDisposable mCompositeDisposable = new CompositeDisposable();
    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initViews();
        initData();
        Intent intent = getIntent();
        if (intent != null) {
            processGestureCaptureRequest(intent);
        }
    }


    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Log.d(TAG, "onNewIntent");
        setIntent(intent);
        if (intent != null) {
            processGestureCaptureRequest(intent);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.v(TAG, "onStart");
        mUSBMonitor.register();
        if (mUVCCameraView != null)
            mUVCCameraView.onResume();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
//        //每次onResume都会执行显示最后一张照片缩略图的操作
//        getDbLastPhotoThumbnail();
        hideStatusBar(getWindow().getDecorView());
    }


    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");
        cdtn_take_picture_counter.cancel();
    }


    @Override
    protected void onStop() {
        super.onStop();
        Log.v(TAG, "onStop");
        queueEvent(new Runnable() {
            @Override
            public void run() {
                mCameraHandler.close();
            }
        }, 0);
        if (mUVCCameraView != null)
            mUVCCameraView.onPause();
        mUSBMonitor.unregister();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.v(TAG, "onDestroy");
        //Remove any pending posts of callbacks and sent messages whose obj is token.
        // If token is null, all callbacks and messages will be removed.
        mMyHandler.removeCallbacksAndMessages(null);
        if (mCameraHandler != null) {
            mCameraHandler.release();
            mCameraHandler = null;
        }
        if (mUSBMonitor != null) {
            mUSBMonitor.destroy();
            mUSBMonitor = null;
        }
        mUVCCameraView = null;
//        dispose();
    }

    private void processGestureCaptureRequest(Intent intent) {
        boolean isTakeCmd = intent.getBooleanExtra(ACTION_CAMERA_TAKE, false);
        mIsNeedTakePhotoNow.set(isTakeCmd);
    }


    private void initViews() {
        v_cover = findViewById(R.id.v_cover);
        iv_to_album = findViewById(R.id.iv_to_album);
        iv_exit = findViewById(R.id.iv_exit);
        iv_capture = findViewById(R.id.iv_capture);
        iv_capture_anim = findViewById(R.id.iv_capture_anim);
        camera_error_tips = findViewById(R.id.camera_error_tips);
        cdtn_take_picture_counter = findViewById(R.id.cdtn_take_picture_counter);
        camera_view = findViewById(R.id.camera_view);
        mUVCCameraView = (CameraViewInterface) camera_view;
    }

    /**
     * 初始化控件
     */
    @SuppressLint("ClickableViewAccessibility")
    private void initData() {
        mMyHandler = new MyHandler(getMainLooper(), this);
        mAudioController = new NativeAudioController(this);
        //退出拍照应用
        iv_exit.setOnClickListener(new OnNoDoubleClickListener() {
            @Override
            protected void onNoDoubleClick(View v) {
                finish();
            }
        });
        //点击拍照
        iv_capture.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                switch (motionEvent.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        startCaptureDownAnim();
                        break;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        AnimatorSet captureDownScaleAnimatorSet = mCaptureDownScaleAnimatorSet;
                        if (captureDownScaleAnimatorSet != null) {
                            if (captureDownScaleAnimatorSet.isRunning()) {
                                captureDownScaleAnimatorSet.cancel();
                            } else {
                                startCaptureUpAnim();
                            }
                        }
                }
                return true;
            }

        });
        //进入相册
        iv_to_album.setOnClickListener(new OnNoDoubleClickListener() {
            @Override
            protected void onNoDoubleClick(View v) {
                toImageDetailAty(MainActivity.this);
            }
        });
        //设置返回按钮的颜色
//        VectorDrawable drawable2 = (VectorDrawable) getDrawable(R.drawable.ic_close_outlined);
//        drawable2.mutate();
//        drawable2.setTint(getResources().getColor(R.color.wt_system_white_700_color));
//        drawable2.setBounds(0, 0, drawable2.getMinimumWidth(), drawable2.getMinimumHeight());
//        iv_exit.setImageDrawable(drawable2);
        initCaptureDownScaleAnimator();
        mUVCCameraView.setAspectRatio(PREVIEW_WIDTH / (float) PREVIEW_HEIGHT);
        //解决预览镜像问题
        camera_view.setScaleX(-1F);
        mUSBMonitor = new USBMonitor(this, mOnDeviceConnectListener);
        mCameraHandler = UVCCameraHandler.createHandler(this, mUVCCameraView,
                2, PREVIEW_WIDTH, PREVIEW_HEIGHT, PREVIEW_MODE, new OnCaptureListener() {
                    @Override
                    public void onCompleteCapture(String path) {
                        Log.i(TAG, "onCompleteCapture, path: " + path);
                        Message message = mMyHandler.obtainMessage();
                        message.what = MESSAGE_CAPTURE_COMPLETE;
                        message.obj = path;
                        mMyHandler.sendMessage(message);
                    }
                });

    }

    private void initCaptureDownScaleAnimator() {
        if (mCaptureDownScaleAnimatorSet == null) {
            mPointF.x = 1;
            mPointF.y = 1;
            ObjectAnimator scaleX = ObjectAnimator.ofFloat(iv_capture, "scaleX", mPointF.x, 0.8F);
            ObjectAnimator scaleY = ObjectAnimator.ofFloat(iv_capture, "scaleY", mPointF.y, 0.8F);
            mCaptureDownScaleAnimatorSet = new AnimatorSet();
            mCaptureDownScaleAnimatorSet.setDuration(800);
            DecelerateInterpolator interpolator = new DecelerateInterpolator();
            mCaptureDownScaleAnimatorSet.setInterpolator(interpolator);
            mCaptureDownScaleAnimatorSet.playTogether(scaleX, scaleY);
            mCaptureDownScaleAnimatorSet.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationCancel(Animator animation) {
                    super.onAnimationEnd(animation);
                    mPointF.x = iv_capture.getScaleX();
                    mPointF.y = iv_capture.getScaleY();
                    startCaptureUpAnim();
                }
            });
        }
    }


    /**
     * 显示最后拍摄的照片
     *
     * @param path 照片路径
     */
    public void displayLastPicture(String path) {
        Log.d(TAG, "display last picture, path: " + path);
        setImgOnToAlbum(path);
    }

    private void startPreview() {
        Log.i(TAG, "Start preview.");
        final SurfaceTexture st = mUVCCameraView.getSurfaceTexture();
        if (mSurface != null) {
            mSurface.release();
        }
        mSurface = new Surface(st);
        mCameraHandler.startPreview(mSurface);
    }


    /**
     * 摄像头开始初始化
     * 摄像头初始化完成
     * 摄像头异常
     * 都会调用这个方法
     *
     * @param status 状态
     */

    public void showTips(int status) {
        camera_error_tips.showErrorTips(status);
        if (status == CameraTips.NONE) {
            v_cover.setVisibility(View.GONE);
            Log.i(TAG, "可以操作，status：NONE");
        } else {
            switch (status) {
                case 0:
                    Log.i(TAG, "禁止操作，status：PREPARING");
                    break;
                case 2:
                    Log.i(TAG, "禁止操作，status：DISCONNECTED");
                    break;
            }
            v_cover.setVisibility(getResources().getColor(R.color.blackB3000000));
            v_cover.setVisibility(View.VISIBLE);
        }
    }


    /**
     * 拍照321 倒计时动画
     *
     * @param startup 是否启动倒计时
     * @param count   倒计时数字
     */
    public void startupTakePictureCountDown(boolean startup, int count) {
        if (startup) {
            //显示倒计时控件
            cdtn_take_picture_counter.setVisibility(View.VISIBLE);
            //设置动画的回调
            cdtn_take_picture_counter.setCallback(mCountDownTimerCallback);
            //开启动画
            cdtn_take_picture_counter.start(count);
        } else {
            //取消动画
            cdtn_take_picture_counter.cancel();
        }
    }


    private void startCaptureDownAnim() {
        mPointF.x = 1;
        mPointF.y = 1;
        AnimatorSet captureDownScaleAnimatorSet = mCaptureDownScaleAnimatorSet;
        if (captureDownScaleAnimatorSet != null) {
            captureDownScaleAnimatorSet.start();
        }
    }


    private void startCaptureUpAnim() {
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(iv_capture, "scaleX", mPointF.x, 1F);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(iv_capture, "scaleY", mPointF.y, 1F);
        AnimatorSet scaleAnimatorSet = new AnimatorSet();
        scaleAnimatorSet.setDuration(200);
        scaleAnimatorSet.playTogether(scaleX, scaleY);
        scaleAnimatorSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                proxyCapture(0);
            }
        });
        scaleAnimatorSet.start();
    }


    /**
     * 拍摄
     *
     * @param delayTime 倒计时开始的数字
     */
    public void proxyCapture(int delayTime) {
        if (!isCameraPreparedButIdle()) {
            return;
        }
        //判断是否正在拍摄照片
        if (mIsTakingPhoto.compareAndSet(false, true)) {
            //超时时间大于0
            if (delayTime > 0) {
                //参数1是否启动倒计时
                //参数2倒计时开始的数字
                startupTakePictureCountDown(true, delayTime);
                //超时时间为0
            } else {
                //执行拍照
                doTakePicture();
            }
        } else {
            Log.i(TAG, "is taking photo now");
        }
    }


    /**
     * 通过命令拍摄照片
     */
    public void takePhotoByCmd() {
        //摄像头初始完成且处于闲置状态
        if (isCameraPreparedButIdle()) {
            Log.e(TAG, "takePhotoByCmd | Camera is prepared and on idle state");
            //拍照
            proxyCapture(3);
        } else {
            Log.e(TAG, "takePhotoByCmd | Camera is not prepared or on idle state");
        }
    }

    /**
     * 执行拍摄照片
     */
    public void doTakePicture() {
        //播放快门音
//        playSoundPool(R.raw.shuuter);
        //拍照
        if (mCameraHandler.isOpened()) {
            mCameraHandler.captureStill();
        } else {
            Log.e(TAG, "The CameraHandler is not opened! ");
        }
    }

    public void playSoundPool(@RawRes int resId) {
        mAudioController.playSound(resId);
    }

    /**
     * 摄像头初始完成且处于闲置状态
     */
    private boolean isCameraPreparedButIdle() {
        return mIsCameraPrepared.get()
                && !mIsTakingPhoto.get();
    }


//    /**
//     * 得到最后一张照片的缩略图
//     */
//    public void getDbLastPhotoThumbnail() {
//        subscribeWith(Observable.just(ImageExif.class).map(new Function<Class<ImageExif>, ImageExif>() {
//            @Override
//            public ImageExif apply(@io.reactivex.annotations.NonNull Class<ImageExif> imageExifClass) throws Exception {
//                ImageExif last = LitePal.findLast(ImageExif.class);
//                try {
//                    String path = last.getPath();
//                    if (!TextUtils.isEmpty(path)) {
//                        while (!LitePal.findAll(ImageExif.class).isEmpty()) {
//                            Log.d(TAG, "apply: last path:" + path);
//                            File file = new File(path);
//                            if (!file.exists()) {
//                                LitePal.delete(ImageExif.class, last.getId());
//                                last = LitePal.findLast(ImageExif.class);
//                                if (last != null) {
//                                    path = last.getPath();
//                                } else {
//                                    break;
//                                }
//                            } else {
//                                break;
//                            }
//                        }
//                    }
//                } catch (Exception exception) {
//                    Log.i(TAG, exception.getMessage());
//                }
//                Log.i(TAG, "last picture: " + last);
//                if (last != null) {
//                    return last;
//                } else {
//                    return new ImageExif();
//                }
//            }
//        }), new EndDisposable<ImageExif>() {
//            @Override
//            public void onResponse(ImageExif imageExif) {
//                Log.i(TAG, "getDbLastPhotoThumbnail onResponse: " + imageExif.toString());
//                displayLastPicture(imageExif.getPath());
//            }
//        });
//    }
//
//    public <T> Observable<T> observerOnUiThread(Observable<T> observable) {
//        return observable
//                .subscribeOn(Schedulers.io())
//                .observeOn(AndroidSchedulers.mainThread());
//    }
//
//    public <T, E extends Observer<? super T>> void subscribeWith(Observable<T> observable, E disposable) {
//        E e = observerOnUiThread(observable)
//                .subscribeWith(disposable);
//        registerDisposable((Disposable) e);
//    }

//    @Override
//    public void registerDisposable(Disposable disposable) {
//        if (disposable != null) {
//            mCompositeDisposable.add(disposable);
//        }
//    }
//
//    @Override
//    public void dispose() {
//        mCompositeDisposable.dispose();
//    }


    public void completeCapture(String path) {
        mPath = path;
        setImgOnToAlbum(path);
//        doCaptureAnim(path);
//        insertMediaFileIntoDb(path);
        if (mIsTakingPhoto.compareAndSet(true, false)) {
            Log.i(TAG, "onCompleteCapture, mIsTakingPhoto is  true, update false.");
        } else {
            Log.e(TAG, "onCompleteCapture, mIsTakingPhoto is  false.");
        }
    }


//    public void insertMediaFileIntoDb(String path) {
//        subscribeWith(Observable.just(new ImageExif()).map(new Function<ImageExif, Boolean>() {
//                    @Override
//                    public Boolean apply(@NonNull ImageExif imageExif) throws Exception {
//                        imageExif.setPath(path);
//                        imageExif.setMediatype(MediaInfoUtils.getMediaTypeByPath(path));
//                        imageExif.setTimestamp(System.currentTimeMillis());
//                        return imageExif.save();
//                    }
//                })
//                , new EndDisposable<Boolean>() {
//                    @Override
//                    public void onResponse(Boolean saveSuccessful) {
//                        Log.i(TAG, "Is save successful: " + saveSuccessful);
//                    }
//                }
//        );
//
//    }

    public void doCaptureAnim(String path) {
        iv_capture_anim.setVisibility(View.VISIBLE);
        Glide.with(MainActivity.this).load(path)
                .transition(GenericTransitionOptions.with(new ViewPropertyTransition.Animator() {
                    @Override
                    public void animate(View view) {
                        startCaptureAnim(path);
                    }
                }))
                .into(iv_capture_anim);

    }

    private void startCaptureAnim(String path) {
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(iv_capture_anim, "scaleX", 1F, (136F / 2560F));
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(iv_capture_anim, "scaleY", 1F, (136F / 1920F));
        //2560/2-(252+136/2)=960
        //1440/2+92+(136/2)=880
        ObjectAnimator translationX = ObjectAnimator.ofFloat(iv_capture_anim, "translationX", -960);
        ObjectAnimator translationY = ObjectAnimator.ofFloat(iv_capture_anim, "translationY", 880);
        AnimatorSet scaleAnimatorSet = new AnimatorSet();
        AnimatorSet translationAnimatorSet = new AnimatorSet();
        scaleAnimatorSet.setDuration(200);
        translationAnimatorSet.setDuration(400);
        scaleAnimatorSet.playTogether(scaleX, scaleY);
        translationAnimatorSet.playTogether(translationX, translationY);
        scaleAnimatorSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                Log.i(TAG, "scaleAnimatorSet end");
                translationAnimatorSet.start();
            }
        });
        translationAnimatorSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                Log.i(TAG, "translationAnimatorSet end");
                Log.d(TAG, "display last picture, path: " + path);
                iv_capture_anim.setVisibility(View.GONE);
                iv_capture_anim.setScaleX(1F);
                iv_capture_anim.setScaleY(1F);
                iv_capture_anim.setTranslationX(0F);
                iv_capture_anim.setTranslationY(0F);
                setImgOnToAlbum(path);
            }
        });
        scaleAnimatorSet.start();
    }

    private void setImgOnToAlbum(String path) {
        Log.i(TAG, "Use glide to load image, path: " + path);
        if (null != path) {
            iv_to_album.setScaleType(ImageView.ScaleType.FIT_XY);
            RoundedCorners corners = new RoundedCorners(8);
            RequestOptions requestOptions = RequestOptions.bitmapTransform(corners);
            Glide.with(MainActivity.this)
                    .load(path)
                    .apply(requestOptions)
//                    .placeholder(R.drawable.wt_loading_drawable)
                    .into(iv_to_album);
        } else {
            iv_to_album.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            Glide.with(MainActivity.this)
                    .load(R.drawable.ic_media_image)
                    .into(iv_to_album);
        }
    }

    private void toImageDetailAty(Context context) {
        Intent intent = new Intent(context, ImgDetailActivity.class);
        intent.putExtra(ARG_IMG_PATH, mPath);
        context.startActivity(intent);
    }


    private void updateConnectBeginUI() {
        showTips(CameraTips.PREPARING);
        Surface surface = mUVCCameraView.getSurface();
        if (surface != null) {
            connect();
        } else {
            mUVCCameraView.setCallback(new CameraViewInterface.Callback() {
                @Override
                public void onSurfaceCreated(CameraViewInterface view, Surface surface) {
                    Log.i(TAG, "onSurfaceCreated");
                    connect();
                }

                @Override
                public void onSurfaceChanged(CameraViewInterface view, Surface surface, int width, int height) {
                    Log.i(TAG, "onSurfaceChanged");
                }

                @Override
                public void onSurfaceDestroy(CameraViewInterface view, Surface surface) {
                    Log.i(TAG, "onSurfaceDestroy");
                    if (mCameraHandler != null) {
                        mCameraHandler.close();
                    }
                }
            });
        }
    }

    private void connect() {
        final List<DeviceFilter> filter = DeviceFilter.getDeviceFilters(this, com.serenegiant.uvccamera.R.xml.device_filter);
        List<UsbDevice> usbDevices = mUSBMonitor.getDeviceList(filter.get(0));
        if (!usbDevices.isEmpty()) {
            UsbDevice usbDevice = usbDevices.get(0);
            mUSBMonitor.requestPermission(usbDevice);
        } else {
            Log.i(TAG, "usbDevices is empty");
            mIsCameraPrepared.set(false);
            mIsTakingPhoto.set(false);
            mMyHandler.sendEmptyMessage(MESSAGE_CONNECT_ERROR);
        }
    }

    private void updateConnectSuccessUI() {
        showTips(CameraTips.NONE);
        //需要在创建完Session后立即拍照
        //手势拍照 也在这里
        if (mIsNeedTakePhotoNow.compareAndSet(true, false)) {
            Log.i(TAG, "Camera is connected, take a photo now!");
            proxyCapture(3);//原来是3
        } else {
            Log.i(TAG, "Camera is connected.");
        }
    }

    private void updateConnectErrorUI() {
        //摄像头连接异常
        showTips(CameraTips.DISCONNECTED);
        TtsManagerProxy.tts(Constant.TTS_CAMERA_DISCONNECTED);
    }

    class MyOnDeviceConnectListener implements USBMonitor.OnDeviceConnectListener {

        /**
         * 设备附加后调用
         * called when device attached
         *
         * @param device device
         */
        @Override
        public void onAttach(final UsbDevice device) {
            Log.i(TAG, "USB_DEVICE_ATTACHED");
            mIsCameraPrepared.set(false);
            mMyHandler.sendEmptyMessage(MESSAGE_BEGIN_CONNECT);
        }


        /**
         * 设备打开后调用
         * called after device opened
         *
         * @param device    device
         * @param ctrlBlock ctrlBlock
         * @param createNew createNew
         */
        @Override
        public void onConnect(final UsbDevice device, final USBMonitor.UsbControlBlock ctrlBlock, final boolean createNew) {
            Log.i(TAG, "USB_DEVICE_CONNECTED");
            mCameraHandler.open(ctrlBlock);
            mIsCameraPrepared.set(true);
            startPreview();
            mMyHandler.sendEmptyMessage(MESSAGE_CONNECT_SUCCESS);
        }

        /**
         * 设备移除或关闭后调用
         * called when USB device removed or its power off (this callback is called after device closing)
         *
         * @param device    设备
         * @param ctrlBlock ctrlBlock
         */
        @Override
        public void onDisconnect(final UsbDevice device, final USBMonitor.UsbControlBlock ctrlBlock) {
            Log.i(TAG, "USB_DEVICE_DISCONNECTED");
            if (mCameraHandler != null) {
                mCameraHandler.close();
            }
            mIsCameraPrepared.set(false);
            mIsTakingPhoto.set(false);
        }

        /**
         * called when device dettach(after onDisconnect)
         *
         * @param device device
         */
        @Override
        public void onDettach(final UsbDevice device) {
            Log.i(TAG, "USB_DEVICE_DETACHED");
        }

        /**
         * called when canceled or could not get permission from user
         *
         * @param device device
         */
        @Override
        public void onCancel(final UsbDevice device) {
            Log.i(TAG, "USB_DEVICE_CANCELED");
            mIsCameraPrepared.set(false);
            mIsTakingPhoto.set(false);
            mMyHandler.sendEmptyMessage(MESSAGE_CONNECT_ERROR);
        }
    }


    class MyCountDownTimerNow implements CountDownTimerNow.Callback {
        @Override
        public void onCountDownComplete(boolean byCancel) {
            Log.i(TAG, "onCountDownComplete");
            if (!byCancel) {
                //执行拍照
                doTakePicture();
            }
        }

        @Override
        public void onCountDownStart() {
            Log.i(TAG, "onCountDownStart");
        }
    }

    static class MyHandler extends Handler {
        WeakReference<MainActivity> mMainActivityWeakReference;

        public MyHandler(@NonNull Looper looper, MainActivity mainActivity) {
            super(looper);
            mMainActivityWeakReference = new WeakReference<MainActivity>(mainActivity);
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            MainActivity mainActivity = mMainActivityWeakReference.get();
            if (mainActivity != null) {
                switch (msg.what) {
                    case MESSAGE_CAPTURE_COMPLETE:
                        String path = (String) msg.obj;
                        mainActivity.completeCapture(path);
                        break;
                    case MESSAGE_CONNECT_ERROR:
                        mainActivity.updateConnectErrorUI();
                        break;
                    case MESSAGE_BEGIN_CONNECT:
                        mainActivity.updateConnectBeginUI();
                        break;
                    case MESSAGE_CONNECT_SUCCESS:
                        mainActivity.updateConnectSuccessUI();
                        break;
                }
            }
        }
    }

}
