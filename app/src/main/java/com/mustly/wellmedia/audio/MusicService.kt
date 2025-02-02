package com.mustly.wellmedia.audio

import android.app.*
import android.content.Intent
import android.os.Build
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.mustly.wellmedia.R
import com.mustly.wellmedia.lib.commonlib.log.LogUtil
import com.mustly.wellmedia.utils.AudioPlayManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class MusicService : LifecycleService() {
    companion object {
        const val TAG = "MusicService"
        const val CHANNEL_ID = "1"
        const val NOTIFICATION_ID = 1
        const val STATE_PLAYING = "PLAYING" // 音乐播放中
        const val STATE_PAUSED = "paused" // 音乐已暂停
    }

    override fun onCreate() {
        super.onCreate()

        AudioPlayManager.init(this@MusicService)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (STATE_PLAYING.equals(intent?.action, true)) {
            // 处理按钮点击事件
            // 播放中，进行暂停
            updateNotification(false)
            AudioPlayManager.pause()
            LogUtil.d(TAG, "暂停音乐")
        } else if (STATE_PAUSED.equals(intent?.action, true)) {
            // 处理按钮点击事件
            // 暂停中，进行播放
            updateNotification(true)
            AudioPlayManager.play()

            LogUtil.d(TAG, "播放音乐")
        } else {
            // 初始化创建自定义通知
            val notification = createNotification(true)
            startForeground(NOTIFICATION_ID, notification)
            lifecycleScope.launch(Dispatchers.Main) {
                AudioPlayManager.start(this@MusicService)
            }
            LogUtil.d(TAG, "创建通知，播放音乐")
        }

        LogUtil.d(TAG, "执行onStartCommand...")
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        super.onDestroy()
        // 停止音频播放。
        AudioPlayManager.apply {
            stop()
            release()
        }
    }
    // 1、县创建通知组
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "音乐播放通知",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }
    private fun updateNotification(isPlay: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val notification = createNotification(isPlay)
            getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, notification)
        }
    }

    // 创建具体的通知
    private fun createNotification(isPlay: Boolean): Notification {
        // 加载自定义布局
        val remoteViews = RemoteViews(packageName, R.layout.item_music_play_notification)

        // 设置按钮点击事件
        val buttonIntent = Intent(this, MusicService::class.java)
        if (isPlay) {
            buttonIntent.action = STATE_PLAYING
            remoteViews.setImageViewResource(R.id.ivPlay, R.drawable.ic_music_play)
        } else {
            buttonIntent.action = STATE_PAUSED
            remoteViews.setImageViewResource(R.id.ivPlay, R.drawable.ic_music_pause)
        }
        val buttonPendingIntent = PendingIntent.getService(
            this,
            0,
            buttonIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        remoteViews.setOnClickPendingIntent(R.id.ivPlay, buttonPendingIntent)

        // 创建通知
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_app_icon) // 小图标（必须设置）
            .setCustomContentView(remoteViews) // 设置自定义布局
            .setPriority(NotificationCompat.PRIORITY_LOW)

        return builder.build()
    }
}