package com.mustly.wellmedia.video.camera2

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.MediaFormat
import android.os.Bundle
import android.util.Size
import android.view.LayoutInflater
import android.view.SurfaceHolder
import android.view.View
import android.view.ViewGroup
import com.example.android.camera2.video.EncoderWrapper
import com.mustly.wellmedia.base.BaseFragment
import com.mustly.wellmedia.base.PageRoute
import com.mustly.wellmedia.databinding.FragmentCamera2PreviewBinding
import com.mustly.wellmedia.lib.annotation.Route
import com.mustly.wellmedia.lib.commonlib.log.LogUtil
import com.mustly.wellmedia.lib.commonlib.utils.getPreviewOutputSize
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * description:
 *
 * 一些基本用法讲解见 [com.mustly.wellmedia.image.Camera2TakePhotoFragment] 页面，
 * Camera2 预览和录制视频界面只讲解与录像存在差异的地方
 *
 * @author   wchenzhang
 * date：    2023/5/20 16:08
 * version   1.0
 * modify by
 */
@Route(PageRoute.CAMERA2_PREVIEW_FRAGMENT)
class PreviewFragment : BaseFragment() {
    companion object {
        const val TAG = "PreviewFragment"

        // 码率，单位 bps(bit per second)
        private const val RECORDER_VIDEO_BITRATE: Int = 10_000_000
    }

    private lateinit var binding: FragmentCamera2PreviewBinding
    private lateinit var args: CameraInfo

    private val cameraManager: CameraManager by lazy {
        requireContext().getSystemService(Context.CAMERA_SERVICE) as CameraManager
    }

    private val characteristics: CameraCharacteristics by lazy {
        cameraManager.getCameraCharacteristics(args.cameraId)
    }

    private val pipeline: Pipeline by lazy {
        if (args.useHardware) {
            HardwarePipeline(args.size.width, args.size.height, args.fps, args.filterOn,
                characteristics, encoder, binding.viewFinder)
        } else {
            SoftwarePipeline(args.size.width, args.size.height, args.fps, args.filterOn,
                characteristics, encoder, binding.viewFinder)
        }
    }

    private val encoder: EncoderWrapper by lazy { createEncoder() }

    // 相机角度: 0, 90, 180, 或者 270
    private val orientation: Int by lazy {
        characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION) ?: 0
    }

    private val outputFile: File by lazy { createFile(requireContext(), "mp4") }

    override fun createView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentCamera2PreviewBinding.inflate(inflater)

        return binding.root
    }

    override fun initData(rootView: View) {
        parseArgs()

        binding.viewFinder.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) = realInit(holder)


            override fun surfaceChanged(
                holder: SurfaceHolder,
                format: Int,
                width: Int,
                height: Int
            ) = Unit

            override fun surfaceDestroyed(holder: SurfaceHolder) = Unit
        })
    }

    private fun parseArgs() {
        val intent = activity?.intent ?: throw IllegalArgumentException("Activity Intent is null")

        intent.apply {
            args = CameraInfo(
                "",
                getStringExtra(Constants.cameraId) ?: "",
                Size(
                    getIntExtra(Constants.width, 1920),
                    getIntExtra(Constants.height, 1080),
                ),
                getIntExtra(Constants.fps, 30),
                getBooleanExtra(Constants.previewStabilization, false),
                getBooleanExtra(Constants.useHardware, true),
                getBooleanExtra(Constants.filterOn, false),
            )
        }
    }

    private fun realInit(holder: SurfaceHolder) {
        // 选择合适的尺寸，并配置 surface
        val previewSize = getPreviewOutputSize(
            binding.viewFinder.display,
            characteristics,
            SurfaceHolder::class.java
        )

        LogUtil.d(TAG, "View finder size: ${binding.viewFinder.width} x ${binding.viewFinder.height}")
        LogUtil.d(TAG, "Selected preview size: $previewSize")

        binding.viewFinder.setAspectRatio(previewSize.width, previewSize.height)

        pipeline.setPreviewSize(previewSize)
    }

    private fun createEncoder(): EncoderWrapper {
        val videoEncoder = MediaFormat.MIMETYPE_VIDEO_AVC

        val codecProfile = -1 // 取代值 MediaCodecInfo.CodecProfileLevel.HEVCProfileMain10


        var width = args.size.width
        var height = args.size.height
        var orientationHint = orientation

        if (args.useHardware) {
            if (orientation == 90 || orientation == 270) {
                // 宽高取反
                width = args.size.height
                height = args.size.width
            }
            orientationHint = 0
        }

        return EncoderWrapper(width, height, RECORDER_VIDEO_BITRATE, args.fps,
            orientationHint, videoEncoder, codecProfile, outputFile)
    }

    private fun createFile(context: Context, extension: String): File {
        val sdf = SimpleDateFormat("yyyy_MM_dd_HH_mm_ss_SSS", Locale.CHINA)
        return File(context.filesDir, "VID_${sdf.format(Date())}.$extension")
    }
}