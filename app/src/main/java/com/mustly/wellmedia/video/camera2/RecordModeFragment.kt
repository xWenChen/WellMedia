package com.mustly.wellmedia.video.camera2

import android.os.Bundle
import android.util.Size
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
 * 录制的处理方式：两个独立的相机流或一个 TEMPLATE_PREVIEW 流
 *
 * @author   wchenzhang
 * date：    2023/5/20 15:29
 * version   1.0
 * modify by
 */
@Route(PageRoute.CAMERA2_RECORD_MODE_FRAGMENT)
class RecordModeFragment : BaseFragment() {
    private lateinit var args: CameraInfo

    override fun createView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = RecyclerView(requireContext())

    override fun initData(rootView: View) {
        parseArgs()

        rootView as RecyclerView
        rootView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            val recordModeList = enumerateRecordModes()
            val layoutId = android.R.layout.simple_list_item_1
            adapter = GenericListAdapter(recordModeList, itemLayoutId = layoutId) { view, item, _ ->
                val (name, isSingleStream) = item
                view.findViewById<TextView>(android.R.id.text1).text = name
                view.setOnClickListener { onSelectConfig(isSingleStream) }
            }
        }
    }

    private fun onSelectConfig(isSingleStream: Boolean) {
        // 一个数据流时，需要使用 MediaCodec 硬件编码
        // 两个数据流时，需要使用 ffmpeg 软件编码，不开启人像滤镜
        if (isSingleStream) {
            Router.go(requireContext(), PageRoute.CAMERA2_FILTER_FRAGMENT) {
                putExtra(Constants.cameraId, args.cameraId)
                putExtra(Constants.width, args.size.width)
                putExtra(Constants.height, args.size.height)
                putExtra(Constants.fps, args.fps)
                putExtra(Constants.previewStabilization, args.previewStabilization)
            }
        } else {
            Router.go(requireContext(), PageRoute.CAMERA2_PREVIEW_FRAGMENT) {
                putExtra(Constants.cameraId, args.cameraId)
                putExtra(Constants.width, args.size.width)
                putExtra(Constants.height, args.size.height)
                putExtra(Constants.fps, args.fps)
                putExtra(Constants.previewStabilization, args.previewStabilization)
                putExtra(Constants.useHardware, false)
                putExtra(Constants.filterOn, false)
            }
        }
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
                getBooleanExtra(Constants.previewStabilization, false)
            )
        }
    }

    private fun enumerateRecordModes(): List<Pair<String, Boolean>> {
        val recordModeList: MutableList<Pair<String, Boolean>> = mutableListOf()
        // name, value 代表是否一个数据流
        // 一个数据流时，需要使用 MediaCodec 硬件编码
        // 两个数据流时，需要使用 ffmpeg 软件编码，不开启人像滤镜
        recordModeList.add(Pair("Multi-stream", false))
        recordModeList.add(Pair("Single-stream", true))

        return recordModeList
    }
}