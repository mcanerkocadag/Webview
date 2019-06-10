package com.example.webview.onesignal;

import android.app.Service;
import android.util.Log
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;

 class MyFirebaseInstanceIDService : FirebaseInstanceIdService() {

     override fun onTokenRefresh() {
         val token = FirebaseInstanceId.getInstance().token
         Log.d("", "Token: " + token!!)

         sendRegistrationToServer(token)
     }
     private fun sendRegistrationToServer(token: String) {
         // token'ı servise gönderme işlemlerini bu methodda yapmalısınız
     }

 }
