package com.mustly.wellmedia.video

import android.content.Context
import android.media.*
import android.net.Uri
import android.os.Build
import android.view.Surface
import android.view.SurfaceHolder
import android.view.View
import android.widget.SeekBar
import androidx.lifecycle.lifecycleScope
import com.mustly.wellmedia.R
import com.mustly.wellmedia.base.BaseFragment
import com.mustly.wellmedia.base.PageRoute
import com.mustly.wellmedia.databinding.FragmentMediaCodecVideoBinding
import com.mustly.wellmedia.lib.annotation.Route
import com.mustly.wellmedia.lib.medialib.base.PlayState
import com.mustly.wellmedia.lib.medialib.base.isPlayState
import com.mustly.wellmedia.utils.HardwareDecoder
import com.mustly.wellmedia.utils.runResult
import com.mustly.wellmedia.utils.stringRes
import com.mustly.wellmedia.utils.uriPath
import kotlinx.coroutines.*

/**
 * MediaCodec 播放视频
 *
 * MediaCodec 解码音视频数据流，surface 播放视频，AudioTrack 播放音频
 *
 * MediaCodec 架构：https://developer.android.com/images/media/mediacodec_buffers.svg
 * MediaCodec 状态图：https://developer.android.com/images/media/mediacodec_states.svg
 * MediaCodec 文章介绍：https://www.jianshu.com/p/7cdf5b495ada
 * MediaCodec 解码 mp4 介绍：https://juejin.cn/post/7085658254641987615
 *
 * MediaCodec 使用流程：https://upload-images.jianshu.io/upload_images/6857764-95c08618974be63f.png
 *
 * MediaCodec 一般和MediaExtractor(音视频轨道信息提取)、MediaSync(音视频同步)、MediaMuxer(音视频混合操作)、
 * MediaCrypto(音视频加解密)、MediaDrm(音视频数字签名)、Image(获取 raw 视频图像信息)、Surface、
 * AudioTrack(音频轨道)搭配使用
 * */
@Route(PageRoute.MEDIA_CODEC_PLAY_VIDEO)
class MediaCodecVideoFragment : BaseFragment<FragmentMediaCodecVideoBinding>(R.layout.fragment_media_codec_video) {
    companion object {
        const val TAG = "MediaCodecVideoFragment"
    }

    private var playState = PlayState.UNINITIALIZED

    private var isSeekBarChanging = false

    private var scheduledJob: Job? = null

    override fun initView(rootView: View) {
        binding.svVideo.holder.addCallback(object : SurfaceHolder.Callback2 {
            override fun surfaceCreated(holder: SurfaceHolder) {
                lifecycleScope.runResult(
                    doOnIo = {
                        decodeVideo(holder.surface)
                        //decodeAudio()
                    }
                )
            }

            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
                // 跳转到上次播放的位置进行播放
            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {
                // 记录当前正在播放的位置
            }

            override fun surfaceRedrawNeeded(holder: SurfaceHolder) {}
        })

        binding.tvCurrentTime.text = R.string.zero_time_text.stringRes
        binding.tvTimeEnd.text = R.string.zero_time_text.stringRes

        binding.sbProgress.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                // binding.tvCurrentTime.text = mediaPlayer.currentPosition.formattedTime()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
                // 通知用户已经开始一个触摸拖动手势
                isSeekBarChanging = true
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                // 通知用户触摸手势已经结束
                isSeekBarChanging = false
                /*mediaPlayer.seekTo(seekBar.progress)
                if (mediaPlayer.notPlay()) {
                    startPlay()
                }*/
                binding.tvCurrentTime.text = seekBar.progress.formattedTime()
            }
        })
        binding.btnPlay.setOnClickListener {
            startPlay()
        }
        binding.btnPause.setOnClickListener {
            if (playState.isPlayState(PlayState.PLAYING)) {
                stopPlay(true)
            }
        }
        binding.btnReset.setOnClickListener {
            if (playState.isPlayState(PlayState.ERROR)) {
                //mediaPlayer.prePareAndStart()
            } else {
                stopPlay()
                startPlay()
            }
        }
    }

    override fun initData(context: Context) {
        /*mediaPlayer.apply {
            mediaPlayer.reset()
            // 循环播放
            mediaPlayer.isLooping = true
            setScreenOnWhilePlaying(true)
            setDataSource(context, Uri.parse(R.raw.tanaka_asuka.uriPath()))

            setOnVideoSizeChangedListener { mMediaPlayer, width, height ->
                changeViewSize(width, height)
            }

            prePareAndStart()
            *//*setOnPreparedListener {

            }
            setOnErrorListener { _, what, extra ->
                Log.e(TAG, "视频解析异常", IllegalStateException("parse error: what=$what, extra=$extra"))
                true
            }
            setOnCompletionListener {
                // OnErrorListener 返回 false 时，会调用这个接口
            }
            prepareAsync()*//*
        }*/
    }

    private fun changeViewSize(videoWidth: Int, videoHeight: Int) {
        if (videoWidth <= 0 || videoHeight <= 0) {
            return
        }

        binding.svVideo.post {
            val viewWidth = binding.svVideo.measuredWidth
            val viewHeight = (videoHeight.toFloat() / videoWidth * viewWidth).toInt()
            val lp = binding.svVideo.layoutParams
            lp.width = viewWidth
            lp.height = viewHeight
            binding.svVideo.layoutParams = lp
        }
    }

    private fun MediaPlayer.prePareAndStart() {
        lifecycleScope.runResult(
            doOnIo = {
                prepare()
            },
            doOnSuccess = {
                playState = PlayState.PREPARED
                realStartPlay()
            },
            doOnFailure = {
                playState = PlayState.ERROR
            }
        )
    }

    override fun onResume() {
        super.onResume()

        startPlay()
    }

    override fun onPause() {
        super.onPause()

        stopPlay(true)
    }

    override fun onDestroy() {
        super.onDestroy()

        /*if (mediaPlayer.isPlaying) {
            mediaPlayer.stop()
        }

        mediaPlayer.release()*/

        stopCheckTime()
    }

    private fun MediaPlayer.notPlay(): Boolean {
        return playState.isPlayState(PlayState.PREPARED) || playState.isPlayState(PlayState.PAUSED)
    }

    // 毫秒转成格式化的时间字符串
    private fun Int.formattedTime(): String {
        if (this <= 0) {
            return "00:00"
        }

        val minutes = this / 1000 / 60
        val seconds = this / 1000 % 60

        return "${checkTimeText(minutes)}:${checkTimeText(seconds)}"
    }

    private fun checkTimeText(timeNumber: Int) = if (timeNumber in 0..9) {
        "0$timeNumber"
    } else {
        "$timeNumber"
    }

    private fun startCheckTime(action: () -> Unit) {
        stopCheckTime()
        scheduledJob = lifecycleScope.launch(Dispatchers.IO) {
            while (true) {
                withContext(Dispatchers.Main) {
                    action.invoke()
                }
                // 500 毫秒，刷新一次计时
                delay(500)
            }
        }
    }

    private fun startPlay() {
        if (playState == PlayState.PLAYING) {
            return
        }

        /*if (playState.isPlayState(PlayState.UNINITIALIZED)) {
            mediaPlayer.prePareAndStart()
            return
        }
        if (playState.isPlayState(PlayState.COMPLETED)) {
            mediaPlayer.seekTo(0)
        }
        if (mediaPlayer.notPlay()) {
            realStartPlay()
        }*/
    }

    private fun realStartPlay() {
        /*binding.sbProgress.max = mediaPlayer.duration
        binding.tvTimeEnd.text = mediaPlayer.duration.formattedTime()
        mediaPlayer.start()
        startCheckTime {
            binding.tvCurrentTime.text = mediaPlayer.currentPosition.formattedTime()
            binding.sbProgress.progress = mediaPlayer.currentPosition
        }
        playState = PlayState.PLAYING*/
    }

    private fun stopPlay(isPaused: Boolean = false) {
        /*if (isPaused) {
            mediaPlayer.pause()
            playState = PlayState.PAUSED
        } else {
            mediaPlayer.stop()
            playState = PlayState.UNINITIALIZED
        }
        stopCheckTime()*/
    }

    private fun stopCheckTime() {
        scheduledJob?.apply {
            if (!isCancelled) {
                cancel()
            }
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
    private fun decodeVideo(surface: Surface) = HardwareDecoder(true).apply {
        // 1. 获取 MediaExtractor
        mExtractor?.setDataSource(
            requireContext(),
            Uri.parse(R.raw.tanaka_asuka.uriPath()),
            null
        )
        // 2. 找到视频相关信息
        var videoFormat: MediaFormat? = null
        var mimeType = ""
        mExtractor?.trackCount?.let { count ->
            (0 until count).forEach {
                videoFormat = mExtractor?.getTrackFormat(it)
                mimeType = videoFormat?.getString(MediaFormat.KEY_MIME) ?: ""
                if (mimeType.startsWith("video/")) {
                    // 选择通道
                    mExtractor?.selectTrack(it)
                    // 赋值
                    videoFormat = mExtractor?.getTrackFormat(it)
                }
            }
        }

        // 获取视频信息
        /*val width = videoFormat?.getInteger(MediaFormat.KEY_WIDTH) ?: 0
        val height = videoFormat?.getInteger(MediaFormat.KEY_HEIGHT) ?: 0
        val time = (videoFormat?.getLong(MediaFormat.KEY_DURATION) ?: 0) / 1000000 */

        // 3. 创建视频解码器
        val videoDecoder = MediaCodec.createDecoderByType(mimeType)

        videoDecoder.configure(videoFormat, surface, null, 0)
        videoDecoder.start()

        val startMs = System.currentTimeMillis()
        var outputDone = false
        var inputDone = false

        while (!outputDone) {
            if (!inputDone) {
                videoDecoder.inputData(mExtractor)?.apply {
                    inputDone = this
                }
            }
            videoDecoder.outputData(startMs)?.apply {
                outputDone = this
            }
        }

        mExtractor?.release()
        videoDecoder.stop()
        videoDecoder.release()
    }

    /**
     * 使用协程同步解码音频
     * */
    private fun decodeAudio() = HardwareDecoder(false).apply {
        // 1. 获取 MediaExtractor
        mExtractor?.setDataSource(
            requireContext(),
            Uri.parse(R.raw.tanaka_asuka.uriPath()),
            null
        )
        // 2. 找到音频相关信息
        var audioFormat: MediaFormat? = null
        var mimeType = ""
        mExtractor?.trackCount?.let { count ->
            (0 until count).forEach {
                audioFormat = mExtractor?.getTrackFormat(it)
                mimeType = audioFormat?.getString(MediaFormat.KEY_MIME) ?: ""
                if (mimeType.startsWith("audio/")) {
                    // 选择通道
                    mExtractor?.selectTrack(it)
                    // 赋值
                    audioFormat = mExtractor?.getTrackFormat(it)
                }
            }
        }
        // 采样率
        val sampleRate = audioFormat?.getInteger(MediaFormat.KEY_SAMPLE_RATE) ?: 0
        // 声道数
        val channelCount = audioFormat?.getInteger(MediaFormat.KEY_CHANNEL_COUNT) ?: 0
        // 采样位数
        val sampleWidth = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            audioFormat?.getInteger(MediaFormat.KEY_PCM_ENCODING) ?: AudioFormat.ENCODING_PCM_16BIT
        } else {
            AudioFormat.ENCODING_PCM_16BIT
        }

        // 3. 创建音频播放器
        // 初始化 AudioTrack
        val minBufferSize = AudioTrack.getMinBufferSize(sampleRate, channelCount, sampleWidth)
        // 说明 https://stackoverflow.com/questions/50866991/android-audiotrack-playback-fast
        val audioTrack = AudioTrack(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build(),
            AudioFormat.Builder()
                .setSampleRate(sampleRate)
                .setChannelMask(channelCount)
                .setEncoding(sampleWidth)
                .build(),
            minBufferSize,
            AudioTrack.MODE_STREAM,
            AudioManager.AUDIO_SESSION_ID_GENERATE
        )
        audioTrack.play()

        // 4. 创建音频解码器
        val audioDecoder = MediaCodec.createDecoderByType(mimeType)

        audioDecoder.configure(audioFormat, null, null, 0)
        audioDecoder.start()

        val audioBufferInfo = MediaCodec.BufferInfo()
        val startMs = System.currentTimeMillis()
        var inputDone = false
        var outputDone = false

        while (!outputDone) {
            // 将资源传递到解码器
            if (!inputDone) {
                audioDecoder.inputData(mExtractor)?.apply {
                    inputDone = this
                }
            }
            outputDone = audioDecoder.outputAudioData(audioTrack, audioBufferInfo, startMs)
        }

        mExtractor?.release()
        audioDecoder.stop()
        audioDecoder.release()
        audioTrack.stop()
        audioTrack.release()
    }

    /**
     * 原始数据写入解码器
     * */
    private fun MediaCodec.inputData(mExtractor: MediaExtractor?): Boolean? {
        // 1. dequeue: 出列，拿到一个输入缓冲区的index，因为有好几个缓冲区来缓冲数据. -1表示暂时没有可用的
        val inputBufferIndex = dequeueInputBuffer(HardwareDecoder.TIMEOUT)
        if (inputBufferIndex < 0) return null

        // 2. 使用返回的 inputBuffer 的 index 得到一个ByteBuffer，可以放数据了
        val inputBuffer = getInputBuffer(inputBufferIndex) ?: return null
        // 3. 往 InputBuffer 里面写入数据。返回的是写入的实际数据量，-1 表示已全部写入
        val sampleSize = mExtractor?.readSampleData(inputBuffer, 0) ?: -1
        // 4. 数据入队
        if (sampleSize >= 0) {
            // 填充好的数据写入 InputBuffer，分别设置 size 和 sampleTime
            // 这里 sampleTime 不一定是顺序来的，所以需要缓冲区来调节顺序
            queueInputBuffer(inputBufferIndex, 0, sampleSize, mExtractor?.sampleTime ?: 0, 0)
            // 在 MediaExtractor 执行完一次 readSampleData 方法后，
            // 需要调用 advance() 去跳到下一个 sample， 然后再次读取数据(读取下次采样视频帧)
            mExtractor?.advance()
            return false
        } else {
            queueInputBuffer(inputBufferIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
            return true
        }
    }

    /**
     * 从解码器获取解码后的数据
     * */
    private fun MediaCodec.outputData(startMs: Long): Boolean? {
        val videoBufferInfo = MediaCodec.BufferInfo()
        // 等待 10 秒
        val outputBufferIndex = dequeueOutputBuffer(videoBufferInfo, HardwareDecoder.TIMEOUT)
        if (outputBufferIndex < 0) {
            return null
        }

        // 直接渲染到 Surface 时使用不到 outputBuffer
        // ByteBuffer outputBuffer = videoCodec.getOutputBuffer(outputBufferIndex);
        // 如果缓冲区里的展示时间(PTS) > 当前视频播放的进度，就休眠一下(视频解析过快，需要缓缓)
        sleep(videoBufferInfo, startMs)
        // 将该ByteBuffer释放掉，以供缓冲区的循环使用
        releaseOutputBuffer(outputBufferIndex, true)

        return videoBufferInfo.flags.and(MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0
    }

    /**
     * 从解码器获取解码后的音频数据
     * */
    private fun MediaCodec.outputAudioData(
        audioTrack: AudioTrack,
        audioBufferInfo: MediaCodec.BufferInfo,
        startMs: Long
    ): Boolean {
        // 等待 10 秒
        val outputBufferIndex = dequeueOutputBuffer(audioBufferInfo, startMs)
        if (outputBufferIndex < 0 || audioBufferInfo.size <= 0) {
            return false
        }

        val byteBuffer = getOutputBuffer(outputBufferIndex) ?: return false
        val pcmData = ByteArray(audioBufferInfo.size)
        // 如果缓冲区里的展示时间(PTS) > 当前音频播放的进度，就休眠一下(音频解析过快，需要缓缓)
        sleep(audioBufferInfo, startMs)
        byteBuffer.position(0)
        byteBuffer.get(pcmData, 0, audioBufferInfo.size)
        byteBuffer.clear()
        // audioTrack.write(pcmData, 0, audioBufferInfo.size);//用这个写法会导致少帧？
        // 数据写入播放器
        audioTrack.write(pcmData, audioBufferInfo.offset, audioBufferInfo.offset + audioBufferInfo.size)
        // 将该ByteBuffer释放掉，以供缓冲区的循环使用
        releaseOutputBuffer(outputBufferIndex, true)

        return audioBufferInfo.flags.and(MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0
    }

    /**
     * 使用异步模式解码视频
     * */
    private fun decodeVideoAsync() {

    }

    private fun sleep(mediaBufferInfo: MediaCodec.BufferInfo, startMs: Long) {
        // videoBufferInfo.presentationTimeUs / 1000  PTS 视频的展示时间戳(相对时间)
        val ffTime = startMs + mediaBufferInfo.presentationTimeUs / 1000 - System.currentTimeMillis()
        if (ffTime > 0) {
            Thread.sleep(ffTime)
        }
    }
}