package com.example.notes

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat

class NotificationsReceiver : BroadcastReceiver()
{
    override fun onReceive(context: Context, intent: Intent)
    {
        val notificationId = intent.getIntExtra(Notifications.notificationIdTag, 0)
        val contentData = intent.getStringExtra(Notifications.notificationTitleTag)

        val notification = NotificationCompat
            .Builder(context, Notifications.notificationChannelId)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentTitle(contentData)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(notificationId, notification)
    }
}