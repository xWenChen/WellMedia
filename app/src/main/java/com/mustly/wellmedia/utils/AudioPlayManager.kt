package com.mustly.wellmedia.utils

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.audiofx.Visualizer
import android.net.Uri
import com.mustly.wellmedia.R
import com.mustly.wellmedia.lib.commonlib.log.LogUtil
import com.mustly.wellmedia.lib.medialib.base.bean.HardwareMediaInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 音频播放管理器
 *
 * AudioTrack 有两种数据加载模式（MODE_STREAM 和 MODE_STATIC）， 对应着两种完全不同的使用场景。
 *
 * MODE_STREAM：在这种模式下，通过 write 一次次把音频数据写到 AudioTrack 中。这和平时通过 write 调用往文件中写数据类似，但这种方式每次都需要把数据从用户提供的 Buffer 中拷贝到 AudioTrack 内部的 Buffer 中，在一定程度上会使引起延时。为解决这一问题，AudioTrack 就引入了第二种模式。
 * MODE_STATIC：在这种模式下，只需要在 play 之前通过一次 write 调用，把所有数据传递到 AudioTrack 中的内部缓冲区，后续就不必再传递数据了。这种模式适用于像铃声这种内存占用较小、延时要求较高的文件。但它也有一个缺点，就是一次 write 的数据不能太多，否则系统无法分配足够的内存来存储全部数据。
 *
 * 在 AudioTrack 构造函数中，会接触到 AudioManager.STREAM_MUSIC 这个参数。它的含义与 Android 系统对音频流的管理和分类有关。Android 将系统的声音分为好几种流类型，下面是几个常见的：
 *
 * STREAM_ALARM：警告声
 * STREAM_MUSIC：音乐声，例如 music 等
 * STREAM_RING：铃声
 * STREAM_SYSTEM：系统声音，例如低电提示音，锁屏音等
 * STREAM_VOICE_CALL：通话声
 *
 * @author wchenzhang
 * @date 2025/01/18 16:37:44
 */
object AudioPlayManager {
    private const val TAG = "AudioPlayManager"
    const val SAMPLE_RATE  = 44100 // 采样率 44100Hz，所有设备都支持
    const val CHANNEL = AudioFormat.CHANNEL_OUT_STEREO // 双声道
    const val BIT_DEPTH = AudioFormat.ENCODING_PCM_16BIT // 两个字节的位深

    val defaultInfo = Triple(SAMPLE_RATE, CHANNEL, BIT_DEPTH)

    /**
     * 是否启用 AudioTrack，true 表示启用 AudioTrack，false 表示使用 MediaPlayer
     * */
    var useAudioTrack = true

    var audioTrack: AudioTrack? = null

    // 缓冲区字节大小
    private var mBufferSizeInBytes = 0

    var playState = AudioTrack.STATE_UNINITIALIZED

    var visualizer: Visualizer? = null

    fun init(context: Context?) {
        val info = findMp3Info(context, Uri.parse(R.raw.Never_Be_Alone.uriPath()))
        val rate = info.sampleRate.let { if (it <= 0) SAMPLE_RATE else it }
        val track = info.voiceTrack
        val depth = info.sampleDepth.let { if (it <=0) BIT_DEPTH else it }
        if (useAudioTrack) {
            // 初始化 AudioTrack
            mBufferSizeInBytes = AudioTrack.getMinBufferSize(rate, track, depth)
            // 说明 https://stackoverflow.com/questions/50866991/android-audiotrack-playback-fast
            audioTrack = AudioTrack(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build(),
                AudioFormat.Builder()
                    .setSampleRate(rate)
                    .setChannelMask(track)
                    .setEncoding(depth)
                    .build(),
                mBufferSizeInBytes,
                AudioTrack.MODE_STREAM,
                AudioManager.AUDIO_SESSION_ID_GENERATE
            )
            playState = AudioTrack.STATE_INITIALIZED
        }
    }

    suspend fun start(context: Context?) {
        context ?: return
        if (playState == AudioTrack.PLAYSTATE_PLAYING) {
            LogUtil.w(TAG, "正在播放...")
            return
        }
        LogUtil.d(TAG, "开始播放...")
        withContext(Dispatchers.IO) {
            try {
                playAudioData(context)
            } catch (e: Exception) {
                LogUtil.e(TAG, e)
            }
        }
    }

    /**
     * 继续播放
     */
    fun play() {
        if (playState == AudioTrack.PLAYSTATE_PLAYING) {
            LogUtil.d(TAG, "正在播放...")
            return
        }
        LogUtil.d(TAG, "继续播放...")
        playState = AudioTrack.PLAYSTATE_PLAYING
        try {
            audioTrack?.play()
        } catch (e: Exception) {
            LogUtil.e(TAG, e)
        }
    }

    /**
     * 暂停播放
     */
    fun pause() {
        if (playState == AudioTrack.PLAYSTATE_PAUSED) {
            LogUtil.d(TAG, "无需暂停播放...")
            return
        }
        LogUtil.d(TAG, "暂停播放...")
        playState = AudioTrack.PLAYSTATE_PAUSED
        try {
            audioTrack?.pause()
        } catch (e: Exception) {
            LogUtil.e(TAG, e)
        }
    }

    /**
     * 停止播放
     */
    fun stop() {
        if (playState == AudioTrack.PLAYSTATE_STOPPED) {
            LogUtil.d(TAG, "无需停止播放...")
            return
        }
        LogUtil.d(TAG, "停止播放...")
        playState = AudioTrack.PLAYSTATE_STOPPED
        try {
            audioTrack?.stop()
        } catch (e: Exception) {
            LogUtil.e(TAG, e)
        }
    }

    /**
     * 释放资源
     */
    fun release() {
        LogUtil.d(TAG, "结束播放")
        playState = AudioTrack.STATE_INITIALIZED
        try {
            audioTrack?.release()
            releaseVisualizer()
            audioTrack = null
        } catch (e: Exception) {
            LogUtil.e(TAG, e)
        }
    }

    /**
     * 播放 PCM 音频
     */
    private fun playAudioData(context: Context) {
        context.resources.openRawResource(R.raw.Never_Be_Alone).use {
            val bytes = ByteArray(mBufferSizeInBytes)
            var length = 0
            audioTrack?.play()
            playState = AudioTrack.PLAYSTATE_PLAYING
            try {
                while (length != -1) {
                    length = it.read(bytes)
                    if (length != -1) {
                        audioTrack?.write(bytes, 0, length)
                    }
                }
            } catch (e: Exception) {
                LogUtil.e(TAG, e)
                audioTrack?.stop()
                playState = AudioTrack.PLAYSTATE_STOPPED
            }
        }
    }

    private fun findMp3Info(context: Context?, fileUri: Uri?): HardwareMediaInfo {
        val info = HardwareMediaInfo("", -1, null)
        context ?: return info
        fileUri ?: return info
        MediaExtractor().apply {
            setDataSource(context, fileUri, null)
            (0 until trackCount).forEach {
                val format = getTrackFormat(it)
                val mimeType = format.getString(MediaFormat.KEY_MIME) ?: ""
                if (mimeType.startsWith("audio/")) {
                    return HardwareMediaInfo(mimeType, it, format)
                }
            }

            release()
            return info
        }
    }

    // Waveform data(水波纹数据): 使用 getWaveForm(byte[])方法连续 8 位（无符号）单声道采样
    // Frequency data(频域数据): 使用 getFft(byte[])方法实现 8 位幅度 FFT
    fun initVisualizer() {
        val sessionId = audioTrack?.audioSessionId ?: return LogUtil.e(TAG, "initVisualizer fail, id is null. ")
        visualizer = Visualizer(sessionId)
        // 通过setCaptureSize函数设置采样率大小，其大小我们一般通过getCaptureSizeRange函数来获取。
        // getCaptureSizeRange函数返回两个int类型数组，第一个表示最小值，第二个表示最大值,用来表示采样值的范围。
        visualizer?.captureSize = Visualizer.getCaptureSizeRange()[1]
        // 通过setDataCaptureListener获取采样数据回调，setDataCaptureListener(listener, int rate, boolean waveform, boolean fft)
        // OnDataCaptureListener拥有onWaveFormDataCapture和onFftDataCapture两个函数，前者回调波形数据，后者回调傅里叶变换后数据。
        // rate 采样的频率，设置范围在0~Visualizer.getMaxCaptureRate() 。
        // waveform 是否返回波形数据，false的话，OnDataCaptureListener的onWaveFormDataCapture函数不会有回调。
        // fft 是否返回傅里叶变换后数据，false的话，OnDataCaptureListener的onFftDataCapture函数不会有回调。
        visualizer?.setDataCaptureListener(
            object : Visualizer.OnDataCaptureListener {
                override fun onWaveFormDataCapture(visualizer: Visualizer?, waveform: ByteArray?, samplingRate: Int) {
                    // 波形回调
                }

                override fun onFftDataCapture(visualizer: Visualizer?, fft: ByteArray?, samplingRate: Int) {
                    // 快速傅里叶回调

                }
            },
            Visualizer.getMaxCaptureRate() / 2,
            false,
            true
        )

        enableVisualizer(true)
    }

    /**
     * 设置开始或者停止采样。退出界面时应停止。
     *
     * @param enable 表示开始或者停止采样。
     * */
    fun enableVisualizer(enable: Boolean) {
        visualizer?.enabled = enable
    }

    fun releaseVisualizer() {
        LogUtil.d(TAG, "结束波形监听...")
        try {
            enableVisualizer(false)
            visualizer = null
        } catch (e: Exception) {
            LogUtil.e(TAG, e)
        }
    }
}