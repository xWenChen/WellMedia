package com.mustly.wellmedia.utils

import android.content.Context
import android.media.*
import android.net.Uri
import android.view.Surface
import com.mustly.wellmedia.lib.commonlib.log.LogUtil
import com.mustly.wellmedia.lib.medialib.base.PlayState
import com.mustly.wellmedia.lib.medialib.base.bean.HardwareMediaInfo
import java.util.concurrent.locks.ReentrantLock

/**
 * description:
 *
 * MediaCodec 硬解码视频
 *
 * @author   wchenzhang
 * date：    2022/8/5 19:25
 * version   1.0
 * modify by
 */
class HardwareDecoder(
    val isVideo: Boolean, // 是否解码视频
    val fileUri: Uri // 文件的路径
) {
    companion object {
        const val TAG = "HardwareDecoder"
        const val TIMEOUT = 10000L
    }

    var lock = ReentrantLock()
    var state = PlayState.UNINITIALIZED
        set(value) {
            lock.lock()
            field = value
            lock.unlock()
        }

    /**
     * 音视频分离器
     * */
    var mExtractor: MediaExtractor = MediaExtractor()
    // 解码器
    lateinit var decoder: MediaCodec
    // 音频播放器，仅解码音频时有效
    lateinit var audioPlayer: AudioTrack

    // 只有视频用得上 surface
    var surface: Surface? = null

    var mediaInfo: HardwareMediaInfo? = null

    fun start(context: Context, surface: Surface? = null) {
        try {
            decode(context, surface)
        } catch (e: Exception) {
            LogUtil.e(TAG, e)
        }
    }

    /**
     * 使用协程同步解码视频
     *
     * 文章介绍：https://www.jianshu.com/p/b625dba0399a
     *
     * 当使用原始视频数据时，最好采用 Surface 作为输入源来替代 ByteBuffer，这样效率更高，因为 Surface 使用的更底层
     * 的视频数据，不会映射或复制到 ByteBuffer 缓冲区
     * */
    private fun decode(context: Context, surface: Surface? = null) {
        if (!configMedia(context)) {
            return
        }

        if (isVideo) {
            this.surface = surface
        } else {
            createAndConfigAudioPlayer()
        }

        createAndDecode()

        release()
    }

    private fun createAndConfigAudioPlayer() {
        // 采样率
        val sampleRate = mediaInfo!!.mediaFormat!!.getInteger(MediaFormat.KEY_SAMPLE_RATE)
        // 声道数
        val channelCount = mediaInfo!!.mediaFormat!!.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
        // 采样位数
        val sampleWidth = AudioFormat.ENCODING_PCM_16BIT

        // 3. 创建音频播放器
        // 初始化 AudioTrack
        val minBufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
            if (channelCount == 1) AudioFormat.CHANNEL_OUT_MONO else AudioFormat.CHANNEL_OUT_STEREO,
            sampleWidth
        )
        // 说明 https://stackoverflow.com/questions/50866991/android-audiotrack-playback-fast
        audioPlayer = AudioTrack(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build(),
            AudioFormat.Builder()
                .setSampleRate(sampleRate)
                .setChannelMask(if (channelCount == 1) AudioFormat.CHANNEL_OUT_MONO else AudioFormat.CHANNEL_OUT_STEREO)
                .setEncoding(sampleWidth)
                .build(),
            minBufferSize,
            AudioTrack.MODE_STREAM,
            AudioManager.AUDIO_SESSION_ID_GENERATE
        )
        audioPlayer.play()
    }

    private fun configMedia(context: Context): Boolean {
        // 1. 获取 MediaExtractor
        mExtractor.setDataSource(context, fileUri, null)
        // 2. 找到音频相关信息
        mediaInfo = findMediaFormat()
        if (mediaInfo!!.trackIndex < 0 || mediaInfo!!.mediaFormat == null) {
            state = PlayState.ERROR
            return false
        }
        mExtractor.selectTrack(mediaInfo!!.trackIndex)

        infoLog()

        return true
    }

    fun release() {
        mExtractor.release()
        decoder.release()
        if (!isVideo) {
            audioPlayer.stop()
            audioPlayer.release()
        }
        state = PlayState.UNINITIALIZED
    }

    /**
     * 只有视频才传入 surface
     * */
    private fun createAndDecode() {
        // 3. 创建解码器
        decoder = MediaCodec.createDecoderByType(mediaInfo!!.mimeType)
        // 4. 配置并开始解码
        if (isVideo) {
            decoder.configure(mediaInfo!!.mediaFormat, surface, null, 0)
        } else {
            decoder.configure(mediaInfo!!.mediaFormat, null, null, 0)
        }
        decoder.start()
        state = PlayState.PLAYING

        val startMs = System.currentTimeMillis()
        var inputDone = false
        var outputDone = false

        while (!outputDone) {
            // 将资源传递到解码器
            if (!inputDone) {
                inputDone = decoder.inputData(mExtractor)
            }
            outputDone = decoder.outputData(startMs)
        }
    }

    /**
     * 原始数据写入解码器
     * */
    private fun MediaCodec.inputData(mExtractor: MediaExtractor?): Boolean {
        if (!isPlaying()) {
            return true
        }
        // 1. dequeue: 出列，拿到一个输入缓冲区的index，因为有好几个缓冲区来缓冲数据. -1表示暂时没有可用的
        val inputBufferIndex = dequeueInputBuffer(TIMEOUT)
        if (inputBufferIndex < 0) return false

        // 2. 使用返回的 inputBuffer 的 index 得到一个ByteBuffer，可以放数据了
        val inputBuffer = getInputBuffer(inputBufferIndex) ?: return false
        // 3. 往 InputBuffer 里面写入数据。返回的是写入的实际数据量，-1 表示已全部写入
        val sampleSize = mExtractor?.readSampleData(inputBuffer, 0) ?: -1
        // 4. 数据入队
        return if (sampleSize >= 0) {
            // 填充好的数据写入 InputBuffer，分别设置 size 和 sampleTime
            // 这里 sampleTime 不一定是顺序来的，所以需要缓冲区来调节顺序
            queueInputBuffer(inputBufferIndex, 0, sampleSize, mExtractor?.sampleTime ?: 0, 0)
            // 在 MediaExtractor 执行完一次 readSampleData 方法后，
            // 需要调用 advance() 去跳到下一个 sample， 然后再次读取数据(读取下次采样视频帧)
            mExtractor?.advance()
            false
        } else {
            queueInputBuffer(inputBufferIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
            true
        }
    }

    /**
     * 从解码器获取解码后的音频数据
     * */
    private fun MediaCodec.outputData(startMs: Long): Boolean {
        if (!isPlaying()) {
            return true
        }

        val bufferInfo = MediaCodec.BufferInfo()
        // 等待 10 秒
        val outputBufferIndex = dequeueOutputBuffer(bufferInfo, TIMEOUT)
        if (outputBufferIndex < 0 || bufferInfo.size <= 0) {
            return false
        }

        if (!isVideo) {
            val byteBuffer = getOutputBuffer(outputBufferIndex) ?: return false
            val pcmData = ByteArray(bufferInfo.size)

            // 读取缓存到数组
            byteBuffer.position(0)
            byteBuffer.get(pcmData, 0, bufferInfo.size)
            byteBuffer.clear()
            // audioTrack.write(pcmData, 0, audioBufferInfo.size);//用这个写法会导致少帧？
            // 数据写入播放器
            audioPlayer.write(pcmData, bufferInfo.offset, bufferInfo.offset + bufferInfo.size)
        }
        // 直接渲染到 Surface 时使用不到 outputBuffer
        // ByteBuffer outputBuffer = videoCodec.getOutputBuffer(outputBufferIndex);
        // 如果缓冲区里的展示时间(PTS) > 当前音频播放的进度，就休眠一下(音频解析过快，需要缓缓)
        sleep(bufferInfo, startMs)
        // 将该ByteBuffer释放掉，以供缓冲区的循环使用
        releaseOutputBuffer(outputBufferIndex, true)

        return bufferInfo.flags.and(MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0
    }

    fun findMediaFormat(): HardwareMediaInfo {
        val prefix = if (isVideo) {
            "video/"
        } else {
            "audio/"
        }
        (0 until mExtractor.trackCount).forEach {
            val format = mExtractor.getTrackFormat(it)
            val mimeType = format.getString(MediaFormat.KEY_MIME) ?: ""
            if (mimeType.startsWith(prefix)) {
                return HardwareMediaInfo(mimeType, it, format)
            }
        }

        return HardwareMediaInfo("", -1, null)
    }

    fun isPlaying(): Boolean {
        return state == PlayState.PLAYING || state == PlayState.PAUSED
    }

    private fun sleep(mediaBufferInfo: MediaCodec.BufferInfo, startMs: Long) {
        // videoBufferInfo.presentationTimeUs / 1000  PTS 视频的展示时间戳(相对时间)
        val ffTime = startMs + mediaBufferInfo.presentationTimeUs / 1000 - System.currentTimeMillis()
        if (ffTime > 0) {
            Thread.sleep(ffTime)
        }
    }

    private fun infoLog() = mediaInfo?.apply {
        var logStr = "mimeType = $mimeType, duration = $duration"
        if (isVideo) {
            logStr = "$logStr, width = $width, height = $height"
        }

        LogUtil.d(TAG, "mediaInfo >>> $logStr")
    }
}