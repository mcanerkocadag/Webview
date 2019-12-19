package com.example.webview.onesignal

import android.content.Intent
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import android.media.RingtoneManager
import android.content.ContentResolver
import android.app.NotificationManager
import android.R
import android.app.PendingIntent
import android.content.Context
import android.net.Uri
import androidx.core.app.NotificationCompat
import com.example.webview.MainActivity


class MyFirebaseMessagingService : FirebaseMessagingService() {

    private val TAG = "MyFirebaseMsgService"

    private fun sendNotification(messageTitle: String?, messageBody: String?) {

        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = PendingIntent.getActivity(
            this, 0 /* Request code */, intent,
            PendingIntent.FLAG_ONE_SHOT
        )

        val pattern = longArrayOf(500, 500, 500, 500)//Titreşim ayarı

        val notificationBuilder = NotificationCompat.Builder(this)
            .setSmallIcon(R.drawable.ic_delete)
            .setContentTitle(messageTitle)
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setVibrate(pattern)
            .setContentIntent(pendingIntent)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        try {
            val alarmSound = Uri.parse(
                ContentResolver.SCHEME_ANDROID_RESOURCE
                        + "://" + this.packageName + "/raw/notification"
            )
            val r = RingtoneManager.getRingtone(this, alarmSound)
            r.play()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        notificationManager.notify(0 /* ID of notification */, notificationBuilder.build())
    }
}
