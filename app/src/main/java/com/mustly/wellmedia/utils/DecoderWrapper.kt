package com.mustly.wellmedia.utils

import android.content.Context
import android.net.Uri
import android.view.Surface
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.lifecycleScope
import com.mustly.wellmedia.R
import kotlinx.coroutines.Job

/**
 * description:
 *
 * @author   wchenzhang
 * date：    2022/8/9 16:19
 * version   1.0
 * modify by
 */
class DecoderWrapper(fileUri: Uri) {
    private var videoDecoder: HardwareDecoder? = null
    private var audioDecoder: HardwareDecoder? = null

    private var videoJob: Job? = null
    private var audioJob: Job? = null

    init {
        init(fileUri)
    }

    private fun init(fileUri: Uri) {
        videoDecoder = HardwareDecoder(true, fileUri)
        audioDecoder = HardwareDecoder(false, fileUri)
    }

    fun start(
        context: Context,
        lifecycleScope: LifecycleCoroutineScope,
        surface: Surface? = null
    ) {
        videoJob = lifecycleScope.runResult(
            doOnIo = {
                videoDecoder?.start(context, surface)
            }
        )
        audioJob = lifecycleScope.runResult(
            doOnIo = {
                audioDecoder?.start(context)
            }
        )
    }

    fun stop() {
        if (videoJob?.isActive == true) {
            videoJob?.cancel()
            videoJob = null
        }
        if (audioJob?.isActive == true) {
            audioJob?.cancel()
            audioJob = null
        }
        videoDecoder?.release()
        videoDecoder = null
        audioDecoder?.release()
        audioDecoder = null
    }
}