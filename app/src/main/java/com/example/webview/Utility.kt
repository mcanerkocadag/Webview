package com.example.webview

import android.content.Context
import android.net.NetworkInfo
import android.net.ConnectivityManager
import java.net.MalformedURLException
import java.net.URL
import android.widget.Toast
import android.content.Intent
import androidx.core.content.ContextCompat.startActivity
import com.example.webview.webview.ExampleActivity


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
         fun getHost(url: String): String {
             try {
                 return URL(url).host
             } catch (e: MalformedURLException) {
                 e.printStackTrace()
             }

             return url
         }

         fun sendMail(context: Context) {
             val i = Intent(Intent.ACTION_SEND)
             i.type = "text/plain"
             i.putExtra(Intent.EXTRA_EMAIL, arrayOf("test@mail.com"))
             i.putExtra(Intent.EXTRA_SUBJECT, "Konu Baslığı")
             i.putExtra(Intent.EXTRA_TEXT, "Mail içeriği")
             try {
                 startActivity(context,Intent.createChooser(i, "Seçiminizi Yapınız..."),null)
             } catch (ex: android.content.ActivityNotFoundException) {
                 Toast.makeText(context, "Mail uygulaması kurulu değil", Toast.LENGTH_SHORT).show()
             }

         }
     }


}