package com.mustly.wellmedia.audio

import android.Manifest
import android.annotation.SuppressLint
import android.media.*
import android.view.MotionEvent
import android.view.View
import androidx.lifecycle.lifecycleScope
import com.mustly.wellmedia.MediaApplication
import com.mustly.wellmedia.R
import com.mustly.wellmedia.base.*
import com.mustly.wellmedia.databinding.FragmentAudioRecordBinding
import com.mustly.wellmedia.lib.annotation.Route
import com.mustly.wellmedia.lib.commonlib.log.LogUtil
import com.mustly.wellmedia.utils.MP3LameEncoder
import com.mustly.wellmedia.utils.isPermissionGranted
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.RandomAccessFile

/**
 * 音频录制。AndroidMediaCodec不支持MP3格式，使用Native将PCM编码为MP3格式。Native层使用LAME（一个开源的 MP3 编码库）。
 * */
@Route(PageRoute.MP3_RECORD_FRAGMENT)
class MP3AudioRecordFragment : BaseBindingFragment<FragmentAudioRecordBinding>() {
    companion object {
        const val TAG = "MP3AudioRecordFragment"
    }

    // 正在录音的标志位
    var isRecording = false
    // 音频录制，获取 PCM 数据
    var recorder: AudioRecord? = null
    var encoder: MP3LameEncoder? = null
    private val sample = 44100
    private var minBufferSize = 0
    private var isRecorderInit = false

    private val mFile: File by lazy {
        File(
            "${MediaApplication.getAppContext().externalCacheDir}/音频录制/音频录制为mp3.mp3"
        ).apply {
            this.parentFile?.let { dir ->
                if (!dir.exists()) {
                    dir.mkdirs()
                }
            }
            if (!this.exists()) {
                createNewFile()
            }
        }
    }

    // 视频路径
    private val outputFile: RandomAccessFile by lazy {
        RandomAccessFile(mFile, "rw")
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun initView(rootView: View) {
        initEncoder()
        // 设置点击事件
        binding.ivRecord.setOnTouchListener { v, event ->
            // 监听点击事件，触发动画，按下播放，松开结束
            if (event.action == MotionEvent.ACTION_DOWN) {
                startRecord()
            } else if (event.action == MotionEvent.ACTION_UP || event.action == MotionEvent.ACTION_CANCEL) {
                stopRecord()
            }
            true
        }
    }

    override fun onStop() {
        super.onStop()
        try {
            if (recorder?.state == AudioRecord.RECORDSTATE_RECORDING) {
                recorder?.stop()
                try {
                    outputFile.close()
                } catch (e: Exception) {
                    LogUtil.e(TAG, e)
                }
            }
            recorder?.release()
            recorder = null

            encoder?.destroy()
            encoder = null
        } catch (e: Exception) {
            LogUtil.e(TAG, e)
        }
        playAnim(false)
    }

    @SuppressLint("MissingPermission")
    private fun initRecorder() {
        minBufferSize = AudioRecord.getMinBufferSize(
            sample,
            AudioFormat.CHANNEL_IN_STEREO, // 双声道
            AudioFormat.ENCODING_PCM_16BIT,
        )
        recorder = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sample,
            AudioFormat.CHANNEL_IN_STEREO, // 双声道
            AudioFormat.ENCODING_PCM_16BIT,
            minBufferSize
        )
    }

    private fun initEncoder() {
        encoder = MP3LameEncoder()
    }

    private fun startRecord() = lifecycleScope.launch(Dispatchers.Main) {
        if (activity?.isPermissionGranted(Manifest.permission.RECORD_AUDIO) != true) {
            val isGranted = suspendRequestPermission(
                Manifest.permission.RECORD_AUDIO,
                "请求麦克风权限",
                "我们需要使用麦克风以进行录音。"
            )

            LogUtil.d(TAG, "申请麦克风权限，不录制。。。是否有授权：$isGranted")

            if (isGranted && !isRecorderInit) {
                // 有麦克风权限时，才能正常创建AudioRecord。
                initRecorder()
                isRecorderInit = true
            }

            return@launch
        }

        if (isRecording) {
            return@launch
        }
        isRecording = true
        if (!isRecorderInit) {
            // 有麦克风权限时，才能正常创建AudioRecord。
            initRecorder()
            isRecorderInit = true
        }
        playAnim(true)
        // 音频的读取、编码、写入操作需要异步执行。
        realStartRecord()
    }

    /**
     * 异步线程实现 获取PCM >>> PCM塞入MediaCodec >>> 从MediaCodec拿到AAC数据 >>> 使用MediaMuxer将AAC数据写入MP4。
     * */
    private suspend fun realStartRecord() = withContext(Dispatchers.IO) {
        val mRecorder = recorder ?: return@withContext
        val codec = encoder ?: return@withContext

        outputFile.setLength(0) // 清空数据

        try {
            //调用start，进入start状态
            mRecorder.startRecording()
            while (isRecording) {
                // 用 ByteBuffer 会报错，得使用 ByteArray。
                val inputArray = ByteArray(minBufferSize)
                val outputArray = ByteArray(minBufferSize)
                // 从 AudioRecord 获取数据。
                val readSize = mRecorder.read(inputArray, 0, minBufferSize)
                LogUtil.w(TAG, "写入pcm数据，input buffer, read bytes is $readSize, minBufferSize is $minBufferSize.")
                // 编码得到 mp3 数据
                var mp3Size = 0
                if (readSize <= 0) {
                    LogUtil.e(TAG, "结束录制")
                } else {
                    mp3Size = codec.nativeEncodeInterleaved(inputArray, readSize / 2 / 2, outputArray)
                }
                LogUtil.d(TAG, "获取mp3数据...，inputBuffer.size=$readSize，outputArray=${outputArray.size}")
                if (outputArray.isNotEmpty() && mp3Size > 0) {
                    outputFile.seek(outputFile.length())
                    outputFile.write(outputArray, 0, mp3Size)
                    LogUtil.d(TAG, "写入aac数据成功...，写入尺寸：$mp3Size")
                }
            }
        } catch (e: Exception) {
            LogUtil.e(TAG, e)
        }
    }

    private fun stopRecord() = lifecycleScope.launch(Dispatchers.Main) {
        isRecording = false
        playAnim(false)
        try {
            LogUtil.w(TAG, "停止录音...")
            recorder?.stop()
        } catch (e: Exception) {
            LogUtil.e(TAG, e)
        }
    }

    private fun playAnim(isPlay: Boolean) {
        binding.ivRecord.setImageResource(if (isPlay) R.drawable.ic_circle_mic else R.drawable.ic_mic)
        binding.audioRecordAnimView.apply {
            visibility = if (isPlay) View.VISIBLE else View.INVISIBLE
            playAnim(isPlay)
        }
    }

    override fun initData(rootView: View) {

    }

    /**
     * 查询是否支持 AAC 格式
     * */
    fun isSupportAAC(): Boolean {
        val mediaCount = MediaCodecList.getCodecCount()
        for (i in 0 until mediaCount) {
            val codecInfoAt = MediaCodecList.getCodecInfoAt(i)
            if (!codecInfoAt.isEncoder)
                continue
            val supportedTypes = codecInfoAt.supportedTypes
            for (element in supportedTypes) {
                if (element.equals(MediaFormat.MIMETYPE_AUDIO_AAC, true)) {
                    return true
                }
            }
        }
        return false
    }
}