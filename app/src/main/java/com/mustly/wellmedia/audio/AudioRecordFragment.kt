package com.mustly.wellmedia.audio

import android.Manifest
import android.annotation.SuppressLint
import android.media.*
import android.view.MotionEvent
import android.view.View
import androidx.lifecycle.lifecycleScope
import com.mustly.wellmedia.R
import com.mustly.wellmedia.base.*
import com.mustly.wellmedia.databinding.FragmentAudioRecordBinding
import com.mustly.wellmedia.lib.annotation.Route
import com.mustly.wellmedia.lib.commonlib.log.LogUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 音频录制
 *
 * 参考自：https://stackoverflow.com/questions/19826809/encoding-aac-audio-using-audiorecord-and-mediacodec-on-android
 * */
@Route(PageRoute.AUDIO_RECORD_FRAGMENT)
class AudioRecordFragment : BaseBindingFragment<FragmentAudioRecordBinding>() {
    companion object {
        const val TAG = "AudioRecordFragment"
    }

    // 正在录音的标志位
    var isRecording = false
    lateinit var bytes: ByteArray
    // 音频录制，获取 PCM 数据
    var recorder: AudioRecord? = null
    // PCM 转 AAC
    var encoder: MediaCodec? = null
    private val sample = 44100
    private var minBufferSize = 0

    @SuppressLint("ClickableViewAccessibility")
    override fun initView(rootView: View) {
        initRecorder()
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
            }
            recorder?.release()
            recorder = null

            encoder?.stop()
            encoder?.release()
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
        bytes = ByteArray(minBufferSize)
        recorder = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sample,
            AudioFormat.CHANNEL_IN_STEREO, // 双声道
            AudioFormat.ENCODING_PCM_16BIT,
            minBufferSize
        )
    }

    private fun initEncoder() {
        //自己再setXXX设置数据
        val encodeFormat = MediaFormat.createAudioFormat(
            MediaFormat.MIMETYPE_AUDIO_AAC,
            sample,
            2, // 双声道
        )
        /**
         * 下面配置几个比较关键的参数
         */
        //配置比特率
        encodeFormat.setInteger(MediaFormat.KEY_BIT_RATE, 128000)
        //配置AAC描述，AAC有很多规格LC是其中一个
        encodeFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
        //配置最大输入大小，这里配置的是前面起算的大小的2倍
        encodeFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, minBufferSize * 2)

        //初始化编码器
        encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
        try {//调用configure，进入configured状态
            encoder?.configure(encodeFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            //调用start，进入starting状态
            encoder?.start()
        } catch (e: Exception) {
            LogUtil.e(TAG, e)
        }
    }

    private fun startRecord() = lifecycleScope.launch(Dispatchers.Main) {
        val isGranted = suspendRequestPermission(
            Manifest.permission.RECORD_AUDIO,
            "请求麦克风权限",
            "我们需要使用麦克风以进行录音。"
        )
        if (!isGranted) {
            return@launch
        }
        if (isRecording) {
            return@launch
        }
        isRecording = true
        playAnim(true)
    }

    private fun stopRecord() = lifecycleScope.launch(Dispatchers.Main) {
        if (!isRecording) {
            return@launch
        }
        isRecording = false
        playAnim(false)
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