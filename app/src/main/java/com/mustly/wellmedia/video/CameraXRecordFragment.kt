package com.mustly.wellmedia.video

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraMetadata
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.view.Surface
import android.view.SurfaceHolder
import android.view.View
import android.widget.SeekBar
import android.widget.Toast
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.core.impl.VideoCaptureConfig
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
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
import java.text.SimpleDateFormat
import java.util.*

/**
 * MediaCodec + CameraX 录制视频
 *
 * https://stackoverflow.com/questions/61934659/how-can-i-share-a-surface-between-mediacodec-encoder-and-camerax
 * */
@Route(PageRoute.CAMERAX_RECORD_VIDEO)
class CameraXRecordFragment : BaseFragment<FragmentCameraxRecordBinding>() {
    companion object {
        const val TAG = "CameraXRecordFragment"

        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"

        private val REQUIRED_PERMISSIONS = mutableListOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        ).apply {
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }.toTypedArray()
    }

    /**
     * 通过 QualitySelector 对象为 Recorder 配置视频分辨率
     * CameraX Recorder 支持以下预定义的视频分辨率 Qualities：
     *    Quality.UHD，适用于 4K 超高清视频大小 (2160p)
     *    Quality.FHD，适用于全高清视频大小 (1080p)
     *    Quality.HD，适用于高清视频大小 (720p)
     *    Quality.SD，适用于标清视频大小 (480p)
     *
     * 每个选项对应的确切视频大小取决于相机和编码器的功能。如需了解详情，请参阅 CamcorderProfile 的文档
     *
     * 使用 fromOrderedList() 提供几个首选分辨率，并包含一个后备策略，以备在不支持任何首选分辨率时使用
     * CameraX 可以根据所选相机的功能确定最佳后备匹配项。如需了解详情，请参阅 QualitySelector 的 FallbackStrategy specification
     *
     * 以下代码会请求支持的最高录制分辨率；如果所有请求分辨率都不受支持，则授权 CameraX 选择最接近 Quality.SD 分辨率的分辨率
     * */
    private val qualitySelector = QualitySelector.fromOrderedList(
        listOf(Quality.FHD, Quality.HD, Quality.SD),
        FallbackStrategy.lowerQualityOrHigherThan(Quality.SD)
    )

    private var videoCapture: VideoCapture<Recorder>? = null
    /**
     * 当调用 start() 函数时，Recorder 会返回 Recording 对象
     * 应用可以使用此 Recording 对象完成捕获或执行其他操作，例如暂停或恢复
     * Recorder 一次支持一个 Recording 对象。对前面的 Recording 对象调用 Recording.stop() 或 Recording.close()
     * 后，才可以开始新的录制
     *
     * 应用使用 Recorder.prepareRecording() 为 Recorder 配置 OutputOptions。Recorder 支持以下类型的 OutputOptions：
     *    FileDescriptorOutputOptions，用于捕获到 FileDescriptor 中。
     *    FileOutputOptions，用于捕获到 File 中。
     *    MediaStoreOutputOptions，用于捕获到 MediaStore 中。
     * 无论使用哪种 OutputOptions 类型，都能通过 setFileSizeLimit() 来设置文件大小上限
     * 其他选项特定于单个输出类型，例如 ParcelFileDescriptor 特定于 FileDescriptorOutputOptions
     *
     * prepareRecording() 会返回 PendingRecording 对象，该对象是一个中间对象，用于创建相应的 Recording 对象。
     * PendingRecording 是一个瞬态类，在大多数情况下应不可见，并且很少被应用缓存
     *
     * 应用可以进一步配置录制对象，例如：
     *    使用 withAudioEnabled() 启用音频。
     *    使用 start(Executor, Consumer<VideoRecordEvent>) 注册监听器，以接收视频录制事件
     *
     * 要开始录制，请调用 PendingRecording.start()。CameraX 会将 PendingRecording 转换为 Recording，
     * 将录制请求加入队列，并将新创建的 Recording 对象返回给应用。一旦在相应相机设备上开始录制，CameraX 就会
     * 发送 VideoRecordEvent.EVENT_TYPE_START 事件
     * */
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

        val recorder = Recorder.Builder()
            .setQualitySelector(qualitySelector)
            .build()
        videoCapture = VideoCapture.withOutput(recorder)

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
                // bindToLifecycle() 会返回一个 Camera 对象。如需详细了解如何控制相机输出（如变焦和曝光），
                // 请参阅此指南: https://developer.android.com/training/camerax/configuration#camera-output
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, videoCapture) // 绑定
            } catch(exc: Exception) {
                LogUtil.e(TAG, "Use case binding failed", exc)
            }

        }, Dispatchers.Main.asExecutor())
    }

    @SuppressLint("MissingPermission")
    private fun recordVideo() {
        val videoCapture = videoCapture ?: return

        val curRecording = recording
        if (curRecording != null) {
            // 停止当前的视频录制
            curRecording.stop()
            recording = null
            return
        }

        // 创建新的视频录制会话，并开始
        val name = SimpleDateFormat(FILENAME_FORMAT, Locale.CHINA)
            .format(System.currentTimeMillis())

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/CameraX-Video")
            }
        }

        val mediaStoreOutputOptions = MediaStoreOutputOptions
            .Builder(requireContext().contentResolver, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
            .setContentValues(contentValues)
            .build()

        recording = videoCapture.output.prepareRecording(requireContext(), mediaStoreOutputOptions)
            .apply {
                if (activity?.isPermissionGranted(REQUIRED_PERMISSIONS[1]) == true) {
                    withAudioEnabled()
                }
            }
            .start(Dispatchers.Main.asExecutor()) { recordEvent ->
                when(recordEvent) {
                    is VideoRecordEvent.Start -> {
                        binding.btnVideoRecord.text = R.string.stop_record.stringRes
                    }
                    is VideoRecordEvent.Finalize -> {
                        if (recordEvent.hasError()) {
                            recording?.close()
                            recording = null
                            Log.e(TAG, "Video capture ends with error: ${recordEvent.error}")
                        } else {
                            val msg = "视频录制成功，保存路径: ${recordEvent.outputResults.outputUri}"
                            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                            Log.d(TAG, msg)
                        }

                        binding.btnVideoRecord.text = R.string.start_record.stringRes
                    }
                }
            }
    }

    // 查询相机功能，然后使用 QualitySelector::from() 从受支持的分辨率中进行选择
    @androidx.camera.camera2.interop.ExperimentalCamera2Interop
    @Deprecated(message = "此功能不使用，仅是官方说明的摘录")
    private fun selectQuality(cameraProvider: ProcessCameraProvider) {
        val cameraInfo = cameraProvider.availableCameraInfos.filter {
            Camera2CameraInfo.from(it).getCameraCharacteristic(CameraCharacteristics.LENS_FACING) ==
                CameraMetadata.LENS_FACING_BACK
        }
        // QualitySelector.getSupportedQualities() 返回的功能肯定适用于 VideoCapture 用例或 VideoCapture
        // 和 Preview 用例的组合。与 ImageCapture 或 ImageAnalysis 用例绑定时，如果请求的相机不支持所需的组合，
        // CameraX 仍可能会绑定失败
        val supportedQualities = QualitySelector.getSupportedQualities(cameraInfo[0])
        val filteredQualities = listOf(Quality.UHD, Quality.FHD, Quality.HD, Quality.SD)
            .filter { supportedQualities.contains(it) }

        val qualitySelector = QualitySelector.from(filteredQualities[0])
        val recorder = Recorder.Builder().setQualitySelector(qualitySelector).build()
    }
}