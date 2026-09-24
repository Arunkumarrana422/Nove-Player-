package com.example.player

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class MediaActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            "ACTION_PLAY" -> AudioPlayerManager.activeInstance?.play()
            "ACTION_PAUSE" -> AudioPlayerManager.activeInstance?.pause()
            "ACTION_PREV" -> AudioPlayerManager.activeInstance?.previousSong()
            "ACTION_NEXT" -> AudioPlayerManager.activeInstance?.nextSong()
            "ACTION_STOP" -> {
                AudioPlayerManager.activeInstance?.stopAndDismiss()
                val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
                nm.cancel(1001)
            }
            "VIDEO_PLAY" -> NovaPlayerManager.activeInstance?.play()
            "VIDEO_PAUSE" -> NovaPlayerManager.activeInstance?.pause()
            "VIDEO_STOP" -> {
                NovaPlayerManager.activeInstance?.stopAndDismiss()
                val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
                nm.cancel(1002)
            }
        }
    }
}
