package com.example.webview

import android.app.Application
import com.onesignal.OneSignal
import android.content.Intent
import android.R.attr.action
import com.onesignal.OSNotificationAction
import org.json.JSONObject
import com.onesignal.OSNotificationOpenResult
import android.R.attr.smallIcon
import android.util.Log
import com.onesignal.OSNotification


class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        OneSignal.startInit(this)
            .setNotificationReceivedHandler(ExampleNotificationReceivedHandler())
            .setNotificationOpenedHandler(ExampleNotificationOpenedHandler())
            .inFocusDisplaying(OneSignal.OSInFocusDisplayOption.Notification)
            .unsubscribeWhenNotificationsAreDisabled(true)
            .init()
    }

    private inner class ExampleNotificationReceivedHandler : OneSignal.NotificationReceivedHandler {
        override fun notificationReceived(notification: OSNotification) {
            val data = notification.payload.additionalData
            val notificationID = notification.payload.notificationID
            val title = notification.payload.title
            val body = notification.payload.body
            val smallIcon = notification.payload.smallIcon
            val largeIcon = notification.payload.largeIcon
            val bigPicture = notification.payload.bigPicture
            val smallIconAccentColor = notification.payload.smallIconAccentColor
            val sound = notification.payload.sound
            val ledColor = notification.payload.ledColor
            val lockScreenVisibility = notification.payload.lockScreenVisibility
            val groupKey = notification.payload.groupKey
            val groupMessage = notification.payload.groupMessage
            val fromProjectNumber = notification.payload.fromProjectNumber
            val rawPayload = notification.payload.rawPayload

            val customKey: String?

            Log.i("OneSignalExample", "NotificationID received: $notificationID")

            if (data != null) {
                customKey = data.optString("customkey", null)
                if (customKey != null)
                    Log.i("OneSignalExample", "customkey set with value: $customKey")
            }
        }
    }


    private inner class ExampleNotificationOpenedHandler : OneSignal.NotificationOpenedHandler {
        // This fires when a notification is opened by tapping on it.
        override fun notificationOpened(result: OSNotificationOpenResult) {
            val actionType = result.action.type
            val data = result.notification.payload.additionalData
            val launchUrl = result.notification.payload.launchURL // update docs launchUrl

            val customKey: String?
            var openURL: String? = null
            var activityToLaunch: Any = AcilacakSayfa::class.java

            if (data != null) {
                customKey = data.optString("customkey", null)
                openURL = data.optString("openURL", null)

                if (customKey != null)
                    Log.i("OneSignalExample", "customkey set with value: $customKey")

                if (openURL != null)
                    Log.i("OneSignalExample", "openURL to webview with URL value: $openURL")
            }

            if (actionType == OSNotificationAction.ActionType.ActionTaken) {
                Log.i("OneSignalExample", "Button pressed with id: " + result.action.actionID)

                if (result.action.actionID == "id1") {
                    Log.i("OneSignalExample", "button id called: " + result.action.actionID)
                    activityToLaunch = MainActivity::class.java
                } else
                    Log.i("OneSignalExample", "button id called: " + result.action.actionID)
            }

            val intent = Intent(applicationContext, activityToLaunch as Class<*>)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
            intent.putExtra("openURL", openURL)
            Log.i("OneSignalExample", "openURL = " + openURL!!)
            startActivity(intent)

        }
    }
}