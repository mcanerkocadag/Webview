package com.example.webview

import android.app.ProgressDialog
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import android.app.AlertDialog
import android.app.Dialog
import android.content.*
import android.view.KeyEvent
import android.content.BroadcastReceiver
import android.os.Handler
import android.webkit.WebSettings
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {

    private val URL = "http://www.haberyirmibir.com/"
    var splashScreenDialog: Dialog? = null
    var broadcastReceiver: BroadcastReceiver? = null
    private var mAdView: AdView? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val webView: WebView = findViewById(R.id.webView)

        MobileAds.initialize(this, resources.getString(R.string.admob_app_id))
        mAdView = findViewById(R.id.adView)
        val adRequest = AdRequest.Builder().build()
        mAdView?.loadAd(adRequest)


        showSplash()
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.setAppCachePath("caches")
        webView.settings.setAppCacheEnabled(true)
        webView.settings.savePassword = true
        webView.settings.saveFormData = true
        offlineLoad()
        val progressDialog: ProgressDialog = ProgressDialog.show(
            this, "Bilgi",
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

        webView.loadUrl(URL)

    }

    private fun offlineLoad() {
        webView.settings.setAppCacheMaxSize(5 * 1024 * 1024) // 5MB Size of storage that it will take
        webView.settings.setAppCachePath(applicationContext.cacheDir.absolutePath)
        webView.settings.allowFileAccess = true
        webView.settings.setAppCacheEnabled(true)
        webView.settings.cacheMode = WebSettings.LOAD_DEFAULT // load online by default

        if (!Utility.isNetworkAvailable(applicationContext)) { // loading offline
            webView.settings.cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (event.action === KeyEvent.ACTION_DOWN) {
            when (keyCode) {
                KeyEvent.KEYCODE_BACK -> {
                    if (webView.canGoBack()) {
                        webView.goBack()
                    } else {
                        val builder = AlertDialog.Builder(this@MainActivity)
                        builder.setTitle("Emin misin?")
                        builder.setMessage("Çıkmak istiyor musun?")
                        builder.setPositiveButton(
                            "Evet",
                            DialogInterface.OnClickListener { dialog, which -> finish() })
                        builder.setNegativeButton("Hayır",
                            DialogInterface.OnClickListener { dialog, which -> dialog.cancel() })
                        builder.show()
                    }
                    return true
                }
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun checkInternet() {
        val intentFilter = IntentFilter("android.net.conn.CONNECTIVITY_CHANGE")
        broadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (Utility.isNetworkAvailable(baseContext)) {
                    Toast.makeText(context, "Bağlantı Sağlandı", Toast.LENGTH_SHORT).show()
                    return
                } else {
                    Toast.makeText(context, "Bağlantı Sorunu", Toast.LENGTH_SHORT).show()
                }
            }
        }
        registerReceiver(broadcastReceiver, intentFilter)
    }

    fun showSplash() {
        splashScreenDialog = Dialog(this, R.style.AppTheme)
        splashScreenDialog?.setContentView(R.layout.splash)
        splashScreenDialog!!.show()
        val handler = Handler()
        handler.postDelayed(Runnable { splashScreenDialog?.dismiss() }, 3000)
    }

}
