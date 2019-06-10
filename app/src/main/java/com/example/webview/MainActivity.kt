package com.example.webview

import android.Manifest
import android.app.ProgressDialog
import android.os.Bundle
import android.widget.Toast
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import androidx.appcompat.app.AlertDialog
import android.app.Dialog
import android.app.DownloadManager
import android.content.*
import android.view.KeyEvent
import android.content.BroadcastReceiver
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.view.View
import kotlinx.android.synthetic.main.activity_main.*
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import android.webkit.*


class MainActivity : RuntimePermissionsActivity() {


    private val REQUEST_PERMISSION_READ_CONTACTS = 1

    private val REQUEST_PERMISSION_WRITE_EXTERNAL_STORAGE = 2
    private val URL = "https://www.youtube.com"

    val REQUEST_ID_MULTIPLE_PERMISSIONS: Int = 1453
    var splashScreenDialog: Dialog? = null
    var broadcastReceiver: BroadcastReceiver? = null
    private var mAdView: AdView? = null
    lateinit var swipeRefreshLayout: SwipeRefreshLayout
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //if (!checkAndRequestPermissions()) {
        //    return
        //}

        requestAppPermissions(
            arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
            R.string.runtime_permissions_txt,
            REQUEST_PERMISSION_WRITE_EXTERNAL_STORAGE
        )


        val webView: WebView = findViewById(R.id.webView)

        MobileAds.initialize(this, resources.getString(R.string.admob_app_id))
        mAdView = findViewById(R.id.adView)
        val adRequest = AdRequest.Builder().build()
        mAdView?.loadAd(adRequest)

        swipeRefreshLayout = findViewById(R.id.swipeContainer)
        swipeRefreshLayout.setOnRefreshListener {
            webView.reload()
            swipeRefreshLayout.isRefreshing = false
        }



        showSplash()
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.setAppCachePath("caches")
        webView.settings.setAppCacheEnabled(true)
        webView.settings.savePassword = true
        webView.settings.saveFormData = true
        webView.settings.builtInZoomControls = true //zoom yapılmasına izin verir
        webView.settings.setSupportZoom(true)
        webView.settings.javaScriptCanOpenWindowsAutomatically = true
        webView.settings.allowFileAccess = true
        webView.settings.domStorageEnabled = true
        webView.settings.setRenderPriority(WebSettings.RenderPriority.HIGH)

        registerForContextMenu(webView)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            // Chronium özelliği ile donanım hız ayarlamasını etkinleştirir
            webView.setLayerType(View.LAYER_TYPE_HARDWARE, null)
        } else {
            // eski versiyonlar için donanım hız ayarlamasını etkisizleştirir
            webView.setLayerType(View.LAYER_TYPE_SOFTWARE, null)
        }
        offlineLoad()
        val progressDialog: ProgressDialog = ProgressDialog.show(
            this, "Lütfen Bekleyin",
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


        // dosya indirme ve dinleme
        registerReceiver(onDownloadComplete, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
        showWebViewDownloader()
        // dosya indirme ve dinleme bitiş

        webView.loadUrl(URL)

    }

    private fun showWebViewDownloader() {
        webView.setDownloadListener { url, userAgent, contentDisposition, mimetype, contentLength ->
            //getting file name from url.
            val filename = URLUtil.guessFileName(url, contentDisposition, mimetype)
            //Alertdialog
            val builder = AlertDialog.Builder(this@MainActivity)
            //title for AlertDialog
            builder.setTitle("Uyarı")
            //message of AlertDialog
            builder.setMessage("$filename" + " isimli dosyayı indirmek istediğinize emin misiniz ?")
            //When YES button clicks
            builder.setPositiveButton("Evet") { dialog, which ->
                //DownloadManager.Request created with url.
                val request = DownloadManager.Request(Uri.parse(url))
                //cookie
                val cookie = CookieManager.getInstance().getCookie(url)
                //Add cookie and User-Agent to request
                request.addRequestHeader("Cookie", cookie)
                request.addRequestHeader("User-Agent", userAgent)
                //file scanned by MediaScannar
                request.allowScanningByMediaScanner()
                //Download is visible and its progress, after completion too.
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                //DownloadManager created
                val downloadmanager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                //Saving file in Download folder
                request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename)
                //download enqued

                Toast.makeText(this, "indiriliyor", Toast.LENGTH_LONG).show()
                downloadmanager.enqueue(request)
            }
            builder.setNegativeButton("İptal")
            { dialog, which ->
                //cancel the dialog if Cancel clicks
                dialog.cancel()
            }

            val dialog: AlertDialog = builder.create()
            //alertdialog shows
            dialog.show()
        }
    }


    private val onDownloadComplete: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Toast.makeText(context, "indirildi", Toast.LENGTH_LONG).show()
        }

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

    private fun checkAndRequestPermissions(): Boolean {
        /**
        val permissionINTERNET = ContextCompat.checkSelfPermission(this, android.Manifest.permission.INTERNET)
        val permissionACCESS_NETWORK_STATE =
        ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_NETWORK_STATE)
        val permissionACCESS_FINE_LOCATION =
        ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
        val permissionACCESS_VIBRATE = ContextCompat.checkSelfPermission(this, android.Manifest.permission.VIBRATE)
        val permissionCAMERA = ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA)
        val permissionACCESS_LOCATION_EXTRA_COMMANDS =
        ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_LOCATION_EXTRA_COMMANDS)
        val permissionACCESS_COARSE_LOCATION =
        ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION)
        val permissionWRITE_EXTERNAL_STORAGE =
        ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
        val permissionACCESS_WIFI_STATE =
        ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_WIFI_STATE)
        val permissionCHANGE_WIFI_STATE =
        ContextCompat.checkSelfPermission(this, android.Manifest.permission.CHANGE_WIFI_STATE)
        val permissionREAD_PHONE_STATE =
        ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_PHONE_STATE)
        val permissionRECEIVE_BOOT_COMPLETED =
        ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECEIVE_BOOT_COMPLETED)
        val permissionBLUETOOTH_ADMIN =
        ContextCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_ADMIN)
        val permissionBLUETOOTH = ContextCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH)
        var listPermissionsNeeded: ArrayList<String> = ArrayList<String>()
        if (permissionINTERNET != PackageManager.PERMISSION_GRANTED) {
        listPermissionsNeeded.add(android.Manifest.permission.INTERNET);
        }
        if (permissionACCESS_NETWORK_STATE != PackageManager.PERMISSION_GRANTED) {
        listPermissionsNeeded.add(android.Manifest.permission.ACCESS_NETWORK_STATE);
        }
        if (permissionACCESS_FINE_LOCATION != PackageManager.PERMISSION_GRANTED) {
        listPermissionsNeeded.add(android.Manifest.permission.ACCESS_FINE_LOCATION);
        }
        if (permissionCAMERA != PackageManager.PERMISSION_GRANTED) {
        listPermissionsNeeded.add(android.Manifest.permission.CAMERA);
        }
        if (permissionACCESS_VIBRATE != PackageManager.PERMISSION_GRANTED) {
        listPermissionsNeeded.add(android.Manifest.permission.VIBRATE);
        }
        if (permissionACCESS_COARSE_LOCATION != PackageManager.PERMISSION_GRANTED) {
        listPermissionsNeeded.add(android.Manifest.permission.ACCESS_COARSE_LOCATION);
        }
        if (permissionINTERNET != PackageManager.PERMISSION_GRANTED) {
        listPermissionsNeeded.add(android.Manifest.permission.INTERNET);
        }
        if (permissionACCESS_LOCATION_EXTRA_COMMANDS != PackageManager.PERMISSION_GRANTED) {
        listPermissionsNeeded.add(android.Manifest.permission.ACCESS_LOCATION_EXTRA_COMMANDS);
        }
        if (permissionWRITE_EXTERNAL_STORAGE != PackageManager.PERMISSION_GRANTED) {
        listPermissionsNeeded.add(android.Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        if (permissionACCESS_WIFI_STATE != PackageManager.PERMISSION_GRANTED) {
        listPermissionsNeeded.add(Manifest.permission.ACCESS_WIFI_STATE);
        }
        if (permissionCHANGE_WIFI_STATE != PackageManager.PERMISSION_GRANTED) {
        listPermissionsNeeded.add(Manifest.permission.CHANGE_WIFI_STATE);
        }
        if (permissionREAD_PHONE_STATE != PackageManager.PERMISSION_GRANTED) {
        listPermissionsNeeded.add(Manifest.permission.READ_PHONE_STATE);
        }
        if (permissionRECEIVE_BOOT_COMPLETED != PackageManager.PERMISSION_GRANTED) {
        listPermissionsNeeded.add(Manifest.permission.RECEIVE_BOOT_COMPLETED);
        }
        if (permissionBLUETOOTH_ADMIN != PackageManager.PERMISSION_GRANTED) {
        listPermissionsNeeded.add(Manifest.permission.BLUETOOTH_ADMIN);
        }
        if (permissionBLUETOOTH != PackageManager.PERMISSION_GRANTED) {
        listPermissionsNeeded.add(Manifest.permission.BLUETOOTH);
        }
         **/
        val permissionWRITE_EXTERNAL_STORAGE =
            ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
        var listPermissionsNeeded: ArrayList<String> = ArrayList<String>()
        if (permissionWRITE_EXTERNAL_STORAGE != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(android.Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        if (!listPermissionsNeeded.isEmpty()) {
            val array = arrayOfNulls<String>(listPermissionsNeeded.size)
            ActivityCompat.requestPermissions(
                this,
                listPermissionsNeeded.toArray(array),
                REQUEST_ID_MULTIPLE_PERMISSIONS
            )
            return false
        }
        return true
    }

//    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
//        if (requestCode != 1453) {
//
//            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
//            return
//        }
//
//        for ((indis, permission) in permissions.withIndex()) {
//            if (permission == "android.permission.WRITE_EXTERNAL_STORAGE") {
//                if (grantResults[indis] != 0) {
//                    checkAndRequestPermissions()
//                    return
//                }
//                recreate()
//            }
//        }
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
//    }

    override fun onPermissionsGranted(requestCode: Int) {
        if (requestCode == REQUEST_PERMISSION_READ_CONTACTS) {

            Toast.makeText(this, "Permissions Received for reading contacts.", Toast.LENGTH_LONG).show();

        } else if (requestCode == REQUEST_PERMISSION_WRITE_EXTERNAL_STORAGE) {

            Toast.makeText(this, "İzin verildi.Teşekkürler...", Toast.LENGTH_LONG).show();

        }

    }

    override fun onDestroy() {
        unregisterReceiver(onDownloadComplete)
        super.onDestroy()
    }

}

