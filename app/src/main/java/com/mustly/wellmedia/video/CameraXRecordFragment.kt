package com.mustly.wellmedia.video

import android.Manifest
import android.content.Context
import android.net.Uri
import android.os.Build
import android.view.Surface
import android.view.SurfaceHolder
import android.view.View
import android.widget.SeekBar
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.mustly.wellmedia.R
import com.mustly.wellmedia.base.BaseFragment
import com.mustly.wellmedia.base.PageRoute
import com.mustly.wellmedia.databinding.FragmentCameraxRecordBinding
import com.mustly.wellmedia.databinding.FragmentMediaCodecVideoBinding
import com.mustly.wellmedia.lib.annotation.Route
import com.mustly.wellmedia.lib.commonlib.log.LogUtil
import com.mustly.wellmedia.lib.medialib.base.PlayState
import com.mustly.wellmedia.lib.medialib.base.isPlayState
import com.mustly.wellmedia.utils.*
import kotlinx.coroutines.*

/**
 * MediaCodec + CameraX 录制视频
 * */
@Route(PageRoute.MEDIA_CODEC_PLAY_VIDEO)
class CameraXRecordFragment : BaseFragment<FragmentCameraxRecordBinding>() {
    companion object {
        const val TAG = "CameraXRecordFragment"

        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"

        private const val REQUEST_CODE_PERMISSIONS = 10

        private val REQUIRED_PERMISSIONS = mutableListOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        ).apply {
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }.toTypedArray()
    }

    private var videoCapture: VideoCapture<Recorder>? = null
    private var recording: Recording? = null

    private var isInitialized = false

    override fun initView(rootView: View) {
        binding.btnVideoRecord.setOnClickListener {
            if (isInitialized) {
                recordVideo()
            }
        }
    }

    override fun initData(context: Context) {
        checkAndRequestPermissions(REQUIRED_PERMISSIONS) { resultMap ->
            if (resultMap.all { it.value }) {
                isInitialized = true
                startCamera()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    private fun startCamera() {
        val realActivity = activity ?: return

        val cameraProviderFuture = ProcessCameraProvider.getInstance(realActivity)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get() // 相机实例

            // 预览
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.preview.surfaceProvider)
            }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA // 摄像头

            try {
                cameraProvider.unbindAll() // 解绑
                cameraProvider.bindToLifecycle(this, cameraSelector, preview) // 绑定
            } catch(exc: Exception) {
                LogUtil.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(realActivity))
    }

    private fun recordVideo() {

    }
}