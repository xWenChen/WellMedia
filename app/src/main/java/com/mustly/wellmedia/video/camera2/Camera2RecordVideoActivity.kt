package com.mustly.wellmedia.video.camera2

import android.util.Log
import com.mustly.wellmedia.R
import com.mustly.wellmedia.base.BaseActivity
import com.mustly.wellmedia.base.PageRoute
import com.mustly.wellmedia.databinding.ActivityCamera2RecordVideoBinding
import com.mustly.wellmedia.lib.annotation.Route
import com.mustly.wellmedia.utils.addFragment
import com.mustly.wellmedia.utils.commitTransaction
/*
 * Camera2 录制视频
 *
 * https://developer.android.com/training/camera2
 *
 * 代码参考自：https://github.com/android/camera-samples/tree/main/Camera2Video
 *
 * 相机的图像方向，通常和手机的方向相差 90 度，设备必须旋转 SENSOR_ORIENTATION 度数(可选度数：0、90、180、270)，
 * 前置摄像头逆时针选项，后置摄像头顺时针旋转：
 *  https://developer.android.com/static/images/training/camera/camera2/camera-preview/camera_preview_portrait_front_facing.png
 *
 * 摄像头图像的旋转度数，需要是 SENSOR_ORIENTATION + 设备旋转度数
 *
 * Camera2 使用步骤：
 *  1. 获取 CameraManager
 *  2. 获取 CameraManager#CameraCharacteristics
 *  3. 打开相机，获取 CameraDevice 并设置回调
 *
 * Camera2 使用第一步 >>> 获取目标 surface，surface 可能来自于不同地方
 *      1. 如果想把图像直接展示给用户，可以使用 SurfaceView 的 surface
 *      2. 如果想要读取每一帧图像或者执行逐帧分析，可以使用 ImageReader 的 surface
 *      3. 如果进行并行处理，可以使用 RenderScript.Allocation 的 surface
 *      4. 尽管不推荐使用(基于可维护性考虑)，也可以使用 OpenGL Texture 或 TextureView 的 surface
 */
@Route(PageRoute.CAMERA2_RECORD_VIDEO_ACTIVITY)
class Camera2RecordVideoActivity : BaseActivity<ActivityCamera2RecordVideoBinding>() {
    companion object {
        const val TAG = "Camera2RecordVideoActivity"
    }

    private var curFragmentTag  = ""

    override fun preParseData() {
        var tempTag = intent?.getStringExtra(PageRoute.Param.KEY_FRAGMENT_TAG)
        if(tempTag.isNullOrEmpty()) {
            Log.e(TAG, "fragment tag is null, can not create a fragment")
            tempTag = PageRoute.AUDIO_PLAY_FRAGMENT
        }
        curFragmentTag = tempTag
    }

    override fun initView() {
        supportFragmentManager.commitTransaction {
            addFragment(getFragmentContainerId(), curFragmentTag)
        }
    }

    override fun initData() {

    }

    private fun getFragmentContainerId(): Int {
        return R.id.fcvFunction
    }
}