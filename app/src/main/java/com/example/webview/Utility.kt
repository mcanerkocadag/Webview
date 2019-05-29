package com.example.webview

import android.content.Context
import android.net.NetworkInfo
import android.content.Context.CONNECTIVITY_SERVICE
import android.support.v4.content.ContextCompat.getSystemService
import android.net.ConnectivityManager


 class Utility{

     companion object{
         //TODO Checks if network available or not
         fun isNetworkAvailable(context: Context): Boolean {
             val connectivity = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

             if (connectivity == null) {
                 return false
             } else {
                 val info = connectivity.allNetworkInfo
                 if (info != null) {
                     for (i in info.indices) {
                         if (info[i].state == NetworkInfo.State.CONNECTED) {
                             return true
                         }
                     }
                 }
             }
             return false
         }
     }


}