package com.wt.carcamera.util;

import androidx.annotation.IntDef;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

public interface Constant {

    String URI_PROVIDER = "content://com.wt.carcamera.gallery_provider/imageexif/";



    String ACTION_CAMERA_I_WANT_TO_TAKE_PICTURES = "我要拍照";
    String ACTION_CAMERA_TAKE = "321拍";
    String ACTION_CAMERA_RETAKE = "重拍";
    String ACTION_CAMERA_CANCEL_RETAKE = "不用了";

    String KEY_CAPTURE_CALLER = "capture_caller";
    String KEY_CAPTURE_ACTION = "capture_action";


    String TTS_OVER_SPEED = "为了您的安全，请在低速或停车时拍照";
    String TTS_CAMERA_DISCONNECTED = "摄像头连接异常，请检查摄像头";
    String TTS_CAMERA_SWITCH_OFF = "摄像头已关闭，请在车辆设置中开启";
    String TTS_OK = "好的";
    String TTS_EYES_CLOSED_REQUEST_RETRY_TAKE_PHOTO = "检查到闭眼是否虫拍？";


    String CALLER_CAMERA = "camera"; /* 拍照调用者*/
    String KEY_OPEN_ALBUM_CALLER = "caller"; /* 打开相册的方式*/
    String KEY_PATH = "source_path";


    /**
     * 文件类型
     */
    @Retention(RetentionPolicy.SOURCE)
    @Target({ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER})
    @IntDef({MediaType.IMAGE, MediaType.VIDEO})
    @interface MediaType {
        int IMAGE = 0; /* 图片 */
        int VIDEO = 1; /* 视频 */
    }

    /**
     * 摄像头行为
     */
    @Retention(RetentionPolicy.SOURCE)
    @Target({ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER})
    @IntDef({CaptureAction.TAKE_PHOTO, CaptureAction.RECORD_VIDEO})
    @interface CaptureAction {
        int TAKE_PHOTO = 0; /* 拍照*/
        int RECORD_VIDEO = 1; /* 录像*/
    }

    /**
     * 拍照触发/调用者
     */
    @Retention(RetentionPolicy.SOURCE)
    @Target({ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER})
    @IntDef({CaptureCaller.VOICE_SKILL, CaptureCaller.AI_MODE, CaptureCaller.AI_GESTURE,
            CaptureCaller.PHYSICAL_KEYS, CaptureCaller.REMOTE_CAPTURE})
    @interface CaptureCaller {
        int VOICE_SKILL = 0; /* 语音技能*/
        int AI_MODE = 1; /* AI 多模命令*/
        int AI_GESTURE = 2; /* AI 手势识别 */
        int PHYSICAL_KEYS = 3; /* 物理按键 */
        int REMOTE_CAPTURE = 4; /* 远程拍照/录像 */
    }

  String ARG_IMG_PATH = "ARG_IMG_PATH";

    int MESSAGE_CAPTURE_COMPLETE=0;
    int MESSAGE_CONNECT_ERROR=1;
    int MESSAGE_BEGIN_CONNECT=2;
    int MESSAGE_CONNECT_SUCCESS=3;

}
