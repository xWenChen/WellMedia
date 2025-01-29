package com.mustly.wellmedia.base

/**
 * description:
 *
 * date：    2023/5/20 15:13
 * version   1.0
 * modify by
 */
object PageRoute {
    const val FUNCTION_ACTIVITY = "function_activity"
    const val CAMERA2_SELECTOR_FRAGMENT = "selectorFragment"
    const val CAMERA2_RECORD_MODE_FRAGMENT = "recordModeFragment"
    const val CAMERA2_FILTER_FRAGMENT = "filterFragment"
    const val CAMERA2_PREVIEW_FRAGMENT = "previewFragment"

    const val AUDIO_MAIN_FRAGMENT = "audio_main_fragment"
    const val AUDIO_PLAY_FRAGMENT = "audio_play_fragment"
    const val AUDIO_RECORD_FRAGMENT = "audio_record_fragment"
    const val MP3_RECORD_FRAGMENT = "mp3_record_fragment"

    const val VIDEO_MAIN_FRAGMENT = "video_main_fragment"
    // VideoView 播放视频
    const val VIDEO_VIEW_PLAY = "video_view_play"
    // MediaPlayer 播放视频
    const val MEDIA_PLAYER_PLAY_VIDEO = "media_player_play_video"
    // MediaCodec 播放视频
    const val MEDIA_CODEC_PLAY_VIDEO = "media_codec_play_video"
    // CameraX 录制视频
    const val CAMERAX_RECORD_VIDEO = "camerax_record_video"
    // Camera2 拍照
    const val CAMERA2_TAKE_PHOTO = "camera2_take_photo"
    // Camera2 录制视频
    const val CAMERA2_RECORD_VIDEO = "camera2RecordVideo"

    const val IMAGE_MAIN_FRAGMENT = "image_main_fragment"

    object Param {
        const val KEY_FRAGMENT_TAG = "key_fragment_tag"
    }
}