package com.plcoding.weatherapp.presentation


import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

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
        println("Refreshed token: $token")
    }


    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        // Respond to received messages
    }
}