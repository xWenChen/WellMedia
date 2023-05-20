package com.mustly.wellmedia.video.camera2

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.MediaRecorder
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mustly.wellmedia.base.BaseFragment
import com.mustly.wellmedia.base.PageRoute
import com.mustly.wellmedia.lib.annotation.Route
import com.mustly.wellmedia.lib.commonlib.route.Router

/**
 * description:
 *
 * 相机配置(尺寸、fps)选择界面
 *
 * @author   wchenzhang
 * date：    2023/5/20 14:14
 * version   1.0
 * modify by
 */
@Route(PageRoute.CAMERA2_SELECTOR_FRAGMENT)
class SelectorFragment : BaseFragment() {
    override fun createView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = RecyclerView(requireContext())

    override fun initData(rootView: View) {
        rootView as RecyclerView

        rootView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            val cameraManager = requireContext().getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val cameraList = enumerateVideoCameras(cameraManager)

            val layoutId = android.R.layout.simple_list_item_1

            adapter = GenericListAdapter(cameraList, itemLayoutId = layoutId) { view, item, _ ->
                view.findViewById<TextView>(android.R.id.text1).text = item.name
                view.setOnClickListener { onSelectConfig(item) }
            }
        }
    }

    private fun onSelectConfig(item: CameraInfo) {
        Router.go(requireContext(), PageRoute.CAMERA2_RECORD_MODE_FRAGMENT) {
            putExtra(Constants.cameraId, item.cameraId)
            putExtra(Constants.width, item.size.width)
            putExtra(Constants.height, item.size.height)
            putExtra(Constants.fps, item.fps)
            putExtra(Constants.previewStabilization, false)
        }

        // Android API 33 的新特性，暂时隐藏
        /*var dynamicRangeProfiles: DynamicRangeProfiles? = null
        var supportsPreviewStabilization = false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val characteristics = cameraManager.getCameraCharacteristics(item.cameraId)
            dynamicRangeProfiles = characteristics.get(
                CameraCharacteristics.REQUEST_AVAILABLE_DYNAMIC_RANGE_PROFILES
            )
            val previewStabilizationModes = characteristics.get(
                CameraCharacteristics.CONTROL_AVAILABLE_VIDEO_STABILIZATION_MODES
            )!!
            supportsPreviewStabilization = previewStabilizationModes.contains(
                CameraMetadata.CONTROL_VIDEO_STABILIZATION_MODE_PREVIEW_STABILIZATION
            )
        }

        // If possible, navigate to a second selector for picking a dynamic range.
        // Otherwise continue on to video recording.
        if (dynamicRangeProfiles != null) {
            navController.navigate(
                SelectorFragmentDirections.actionSelectorToDynamicRange(
                    item.cameraId, item.size.width, item.size.height, item.fps
                )
            )
        } else
        if (supportsPreviewStabilization) {
            navController.navigate(
                SelectorFragmentDirections.actionSelectorToPreviewStabilization(
                    item.cameraId, item.size.width, item.size.height, item.fps,
                    DynamicRangeProfiles.STANDARD
                )
            )
        } else
        if (Build.VERSION.SDK_INT >= 29) {
            navController.navigate(
                startFunctionActivity()
                    item.cameraId, item.size.width, item.size.height, item.fps,
                    DynamicRangeProfiles.STANDARD, /*previewStabilization*/ false
            )
        } else {
            navController.navigate(
                SelectorFragmentDirections.actionSelectorToPreview(
                    item.cameraId, item.size.width, item.size.height, item.fps,
                    DynamicRangeProfiles.STANDARD, /*previewStabilization*/ false,
                    false, false
                )
            )
        }*/
    }

    private fun enumerateVideoCameras(cameraManager: CameraManager): List<CameraInfo> {
        val availableCameras: MutableList<CameraInfo> = mutableListOf()

        // 遍历所有相机
        for (id in cameraManager.cameraIdList) {
            val characteristics = cameraManager.getCameraCharacteristics(id)
            val orientation = lensOrientationString(
                characteristics.get(CameraCharacteristics.LENS_FACING))

            // 查询相机支持的能力
            val capabilities = characteristics.get(
                CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES) ?: continue
            // 查询相机支持的配置
            val cameraConfig = characteristics.get(
                CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP) ?: continue

            // 相机不向后兼容(低版本)
            if (!capabilities.contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_BACKWARD_COMPATIBLE)) {
                continue
            }

            // 查询相机框架支持的格式
            val targetClass = MediaRecorder::class.java

            val outputSizes = cameraConfig.getOutputSizes(targetClass) ?: continue
            // 列出每种尺寸的 FPS
            for (size in outputSizes) {
                // getOutputMinFrameDuration 获取帧持续时间(从上一帧开始到下一帧开始的时间间隔)，单位纳秒
                // 1秒 = 1000毫秒，1毫秒 = 1000微秒，1微秒 = 1000 纳秒，1s = 1000ms * 1000us * 1000ns
                val secondsPerFrame = cameraConfig.getOutputMinFrameDuration(targetClass, size) / 1_000_000_000.0
                // 获取帧率：secondsPerFrame = 秒数(seconds) / 帧数(frames)
                val fps = if (secondsPerFrame > 0) (1.0 / secondsPerFrame).toInt() else 0
                val fpsLabel = if (fps > 0) "$fps" else "N/A"
                availableCameras.add(CameraInfo("$orientation ($id) $size $fpsLabel FPS", id, size, fps))
            }
        }

        return availableCameras
    }

    private fun lensOrientationString(value: Int?) = when (value) {
        CameraCharacteristics.LENS_FACING_BACK -> "Back" // 后置相机
        CameraCharacteristics.LENS_FACING_FRONT -> "Front" // 前置相机
        CameraCharacteristics.LENS_FACING_EXTERNAL -> "External" // 外置相机，朝向不固定，可前可后
        else -> "Unknown"
    }
}