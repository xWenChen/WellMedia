package com.mustly.medialib.base.bean

import com.mustly.medialib.base.MediaType
import com.mustly.medialib.video.base.BitrateMode

/**
 * 媒体轨道的数据
 * */
open class MediaTrackInfo {
    /**
     * 媒体轨道的索引
     * */
    var index = -1
    /**
     * 媒体轨道的类型
     * */
    open var mediaType = MediaType.UNKNOWN
    /**
     * 媒体的 MIME TYPE
     * */
    var mimeType = ""
    /**
     * 比特率模式
     * */
    var bitrateMode = BitrateMode.UNKNOWN
    /**
     * 比特率，单位 bits/s
     * */
    var bitrate = 0
    /**
     * 时长，单位 ms
     * */
    var duration = 0L
    /**
     * 帧率
     * */
    var frameRate = 30
    /**
     * 视频捕获率，可以理解成视频录制，摄像头采集画面信息时的帧率
     * */
    var captureRate = 30
    /**
     * 关键帧时长间隔，单位毫秒
     * */
    var iFrameInterval = -1
    /**
     * 帧内刷新周期
     *
     * intra refresh 技术：就是把 I 帧分配到每个 P 帧里，一条一条发 P 帧，最后根据一个周期组合成一个 I 帧。
     * 一般情况下的 P 帧，因为有帧间预测，所以可以很小，而 intra refresh 会强制使用帧内预测，一部分 I 帧信息塞到
     * P 帧里，所以 intra refresh 生成的 P 帧，相比于一般的 P 帧，大小要大些。但是有这样的好处：
     *     1. 可以更轻易地保证每个独立的帧的大小都不超过 TCP 或 UDP 包的最大大小，传输更平滑。如果使用 I 帧 + P 帧，
     *        I 帧一般是不能放在一个 UDP 里发送的，需要分片，这样可能会导致延时(I 帧分两个包发送)。
     *        使用这个 intra refresh 生成的每个帧的大小都是相差不大的，传输更平滑流畅，不容易出现突然的卡顿。
     *     2. 对抗丢包方面更具有稳健性。如果丢了一个 I 帧的任何一个分片，后面周期内的图像都是花的；
     *        而如果只丢了一个 intra refresh 的 P 帧，则相当于只有一小条 I 帧被丢了。周期内的其他图像可以是正常的
     *
     * 说明：帧间预测一般是指时间维度上的预测，而帧内预测一般是指空间维度上的预测
     * */
    var intraRefreshPeriod = -1
}