package com.example.webview

import android.app.ProgressDialog
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast

class AcilacakSayfa : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_acilacak_sayfa)

        val openURL = intent.getStringExtra("openURL")
        Log.i("OneSignalExample","Sayfa acildi")
        val webView: WebView = findViewById(R.id.acilacak_webview)
        webView.settings.javaScriptEnabled = true
        val progressDialog: ProgressDialog = ProgressDialog.show(
            this, "Uyarı",
            "Sayfa Yükleniyor...", true
        )

        webView.webViewClient = object : WebViewClient() {

            // Sayfa Yüklenirken bir hata oluşursa kullanıcıyı uyarıyoruz.
            override fun onReceivedError(
                view: WebView, errorCode: Int,
                description: String, failingUrl: String
            ) {
                Toast.makeText(
                    applicationContext, "Sayfa Yüklenemedi!",
                    Toast.LENGTH_SHORT
                ).show()
            }

            // Sayfanın yüklenme işlemi bittiğinde progressDialog'u kapatıyoruz.
            override fun onPageFinished(view: WebView, url: String) {
                super.onPageFinished(view, url)
                if (progressDialog.isShowing)
                    progressDialog.dismiss()
            }

            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                view?.loadUrl(url)
                return true
            }
        }

        webView.loadUrl("" + openURL)

    }
}
