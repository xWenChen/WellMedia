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
import com.mustly.wellmedia.utils.isPermissionGranted
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 音频录制。使用Android的MediaMuxer将MediaCodec创建的原始流打包成.mp4文件。优点：.mp4 中包含的 AAC 数据包不需要 ADTS 标头。
 *
 * MediaCodec不支持MP3，但支持AAC。
 *
 * 异步线程实现 获取PCM >>> PCM塞入MediaCodec >>> 从MediaCodec拿到AAC数据 >>> 使用MediaMuxer将AAC数据写入MP4。
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
    // 音频录制，获取 PCM 数据
    var recorder: AudioRecord? = null
    // PCM 转 AAC
    var encoder: MediaCodec? = null
    private var muxer: MediaMuxer? = null
    private val sample = 44100
    private var minBufferSize = 0
    private var isRecorderInit = false

    // 视频路径
    private val outputFile: File by lazy {
        File(
            "${MediaApplication.getAppContext().externalCacheDir}/音频录制/音频录制.mp4"
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

    @SuppressLint("ClickableViewAccessibility")
    override fun initView(rootView: View) {
        initEncoder()
        initMuxer()
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
                encoder?.stop()
                muxer?.stop()
            }
            recorder?.release()
            recorder = null

            encoder?.release()
            encoder = null

            muxer?.release()
            muxer = null
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
        //自己再setXXX设置数据
        val encodeFormat = MediaFormat.createAudioFormat(
            MediaFormat.MIMETYPE_AUDIO_AAC,
            sample,
            2, // 双声道
        )
        /**
         * 下面配置几个比较关键的参数
         */
        encodeFormat.setString(MediaFormat.KEY_MIME, MediaFormat.MIMETYPE_AUDIO_AAC)
        encodeFormat.setInteger(MediaFormat.KEY_CHANNEL_COUNT, 2)
        //配置比特率
        encodeFormat.setInteger(MediaFormat.KEY_BIT_RATE, 128000)
        encodeFormat.setInteger(MediaFormat.KEY_SAMPLE_RATE, sample)
        //配置AAC描述，AAC有很多规格LC是其中一个
        encodeFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
        encodeFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, minBufferSize)
        encodeFormat.setInteger(MediaFormat.KEY_CHANNEL_MASK, AudioFormat.CHANNEL_IN_STEREO)

        //初始化编码器
        encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
        try {//调用configure，进入configured状态
            encoder?.configure(encodeFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        } catch (e: Exception) {
            LogUtil.e(TAG, e)
        }
    }

    private fun initMuxer() {
        try {
            muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        } catch (e: Exception) {
            LogUtil.e(TAG, e)
        }
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

        val bufferInfo = MediaCodec.BufferInfo()
        var audioTrack = -1
        try {
            //调用start，进入start状态
            mRecorder.startRecording()
            encoder?.start()
            while (isRecording) {
                val inputBufferIndex = codec.dequeueInputBuffer( -1)
                if (inputBufferIndex < 0) {
                    LogUtil.e(TAG, "inputBufferIndex=$inputBufferIndex, return@withContext")
                    return@withContext
                }
                val inputBuffer = codec.getInputBuffer(inputBufferIndex) ?: continue
                inputBuffer.clear()
                // 从 AudioRecord 获取数据。
                val readSize = mRecorder.read(inputBuffer, minBufferSize)
                LogUtil.w(TAG, "写入pcm数据，input buffer size is ${inputBuffer.limit()}, read bytes is $readSize, minBufferSize is $minBufferSize.")
                //inputBuffer.limit(bytes.size);
                // 数据塞进 MediaCodec
                if (readSize <= 0) {
                    LogUtil.e(TAG, "发送 BUFFER_FLAG_END_OF_STREAM")
                    codec.queueInputBuffer(inputBufferIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                    isRecording = false
                } else {
                    codec.queueInputBuffer(inputBufferIndex, 0, readSize, System.nanoTime() / 1000, 0)
                }
                /*获取编码后的aac数据*/
                var outputBufferIndex = try {
                    codec.dequeueOutputBuffer(bufferInfo, -1)
                } catch (e: Exception) {
                    LogUtil.e(TAG, "获取aac数据失败", e)
                    MediaCodec.INFO_TRY_AGAIN_LATER
                }
                LogUtil.d(TAG, "获取aac数据...，outputBufferIndex=$outputBufferIndex，bufferInfoSize=${bufferInfo.size}")

                if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    audioTrack = muxer?.addTrack(codec.outputFormat) ?: continue
                    LogUtil.e(TAG, "format改变, audioTrack: $audioTrack")
                    if (audioTrack >= 0) {
                        LogUtil.e(TAG, "音频开始写入文件, audioTrack: $audioTrack")
                        muxer?.start()
                    }
                    continue
                }
                if (outputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    LogUtil.w(TAG, "结束aac音频文件写入...")
                    break
                }
                // 跳过配置相关的数据，只接受音频数据
                if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) continue

                val outBuffer = codec.getOutputBuffer(outputBufferIndex)
                if (bufferInfo.size != 0 && outBuffer != null) {
                    LogUtil.d(TAG, "写入aac数据到文件...，bufferInfo.offset=${bufferInfo.offset}，bufferInfo.size=${bufferInfo.size}")
                    outBuffer.position(bufferInfo.offset);
                    outBuffer.limit(bufferInfo.offset + bufferInfo.size);
                    muxer?.writeSampleData(audioTrack, outBuffer, bufferInfo)
                }
                codec.releaseOutputBuffer(outputBufferIndex, false)
                if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                    LogUtil.e(TAG, "END_OF_STREAM, audioTrack: $audioTrack")
                    break
                }
                LogUtil.d(TAG, "写入aac数据成功...")
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
            encoder?.stop()
            muxer?.stop()
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