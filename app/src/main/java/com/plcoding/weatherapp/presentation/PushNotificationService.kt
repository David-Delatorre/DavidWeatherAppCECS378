package com.plcoding.weatherapp.presentation


import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.plcoding.weatherapp.R

class PushNotificationService: FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)

        /**
         * Called if the FCM registration token is updated. This may occur if the security of
         * the previous token had been compromised. Note that this is called when the
         * FCM registration token is initially generated so this is where you would retrieve the token.
         */

        // If you want to send messages to this application instance or
        // manage this apps subscriptions on the server side, send the
        // FCM registration token to your app server.
    }


    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        // Respond to received messages
        // Display notification
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "default_channel_id"
        val channelName = "Default Channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, channelName,
                NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        }

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setContentTitle(message.notification?.title)
            .setContentText(message.notification?.body)
            .setSmallIcon(R.drawable.ic_launcher_background)
            .setAutoCancel(true)

        notificationManager.notify(0, notificationBuilder.build())
    }
}