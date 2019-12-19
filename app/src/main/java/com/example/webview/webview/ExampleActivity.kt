package com.example.webview.webview

import android.Manifest
import android.annotation.SuppressLint
import android.app.*
import android.content.*
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.provider.MediaStore
import android.util.Log
import android.view.*
import android.webkit.*
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.comix.overwatch.HiveProgressView
import com.example.webview.*
import com.example.webview.R
import com.example.webview.model.GlobalParameter
import com.google.android.gms.ads.*
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*


class ExampleActivity : RuntimePermissionsActivity() {


    private lateinit var drawerLayout: DrawerLayout

    private lateinit var toolbar: Toolbar
    private lateinit var toolbarContainer: RelativeLayout


    private lateinit var webView: VideoEnabledWebView
    private var webChromeClient: VideoEnabledWebChromeClient? = null
    private val REQUEST_PERMISSION_READ_CONTACTS = 1

    private val REQUEST_PERMISSION_WRITE_EXTERNAL_STORAGE = 2
    private var URL = "https://www.google.com/"

    val REQUEST_ID_MULTIPLE_PERMISSIONS: Int = 1453
    var splashScreenDialog: Dialog? = null
    var broadcastReceiver: BroadcastReceiver? = null
    private var mAdView: AdView? = null
    lateinit var swipeRefreshLayout: SwipeRefreshLayout

    // admob
    private var mInterstitialAd: InterstitialAd? = null
    private var webSiteEntryCount: Int = 0
    private var admobSplashRetryCount: Int = 2

    // file upload
    private var mCM: String = ""
    private var mUM: ValueCallback<Uri>? = null
    private var mUMA: ValueCallback<Array<Uri>>? = null
    private val FCR = 1
    private val FILECHOOSER_RESULTCODE = 1


    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {

        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_example)

        val openURL = intent.getStringExtra("openURL")
        if (openURL != null)
            URL = openURL

        if (GlobalParameter.settings.isDownloadFeature) {

            requestAppPermissions(
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                R.string.runtime_permissions_txt,
                REQUEST_PERMISSION_WRITE_EXTERNAL_STORAGE
            )
        }

        // Save the web view
        webView = findViewById(R.id.webView)

        // ************Reklam************
        MobileAds.initialize(this, resources.getString(R.string.admob_app_id))
        mAdView = findViewById(R.id.adView)
        val adRequest = AdRequest.Builder().build()
        mAdView?.loadAd(adRequest)

        if (!GlobalParameter.settings.showAdmob) {
            mAdView?.visibility = View.GONE
        }

        if (GlobalParameter.settings.showInterstitialAdmob) {

            mInterstitialAd = InterstitialAd(this)
            mInterstitialAd?.adUnitId = resources.getString(R.string.admob_interstitial_unit_ıd)
            mInterstitialAd?.loadAd(AdRequest.Builder().build())
            mInterstitialAd?.adListener = object : AdListener() {
                override fun onAdClosed() {
                    mInterstitialAd?.loadAd(AdRequest.Builder().build())
                    webSiteEntryCount = 0
                }
            }
        }
        //*************Reklam Bitiş****************

        swipeRefreshLayout = findViewById(R.id.swipeContainer)
        swipeRefreshLayout.setOnRefreshListener {
            webView.reload()
            swipeRefreshLayout.isRefreshing = false
        }

        webViewVideoİnit()

        // Video full ekran yapma ayarı
        if (GlobalParameter.settings.isVideoFullScreen) {

            webChromeClient?.let { webView.setWebChromeClient(it) }
            // Call private class InsideWebViewClient
            webView.webViewClient = InsideWebViewClient()
        }

        // Splash gösterme
        if (GlobalParameter.settings.showSplashScreen) {

            showSplash()
        }

        webViewSettings()
        offlineLoad()
        webviewLoadListener()

        if (GlobalParameter.settings.isDownloadFeature) {
            // dosya indirme ve dinleme
            registerReceiver(onDownloadComplete, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
            showWebViewDownloader()
            // dosya indirme ve dinleme bitiş
        }

        registerForContextMenu(webView)
        // Navigate anywhere you want, but consider that this classes have only been tested on YouTube's mobile site
        if (savedInstanceState == null)
            webView.loadUrl(URL)
    }

    private fun webViewVideoİnit() {

        // Initialize the VideoEnabledWebChromeClient and set event handlers
        val nonVideoLayout = findViewById<RelativeLayout>(R.id.nonVideoLayout) // Your own view, read class comments
        val videoLayout = findViewById<ViewGroup>(R.id.videoLayout) // Your own view, read class comments

        val loadingView =
            layoutInflater.inflate(R.layout.view_loading_video, null) // Your own view, read class comments
        webChromeClient = object : VideoEnabledWebChromeClient(
            nonVideoLayout,
            videoLayout,
            loadingView,
            webView
        ) // See all available constructors...
        {
            // Subscribe to standard events, such as onProgressChanged()...
            override fun onProgressChanged(view: WebView, progress: Int) {
                // Your code...
            }


            override fun onShowFileChooser(
                webView: WebView,
                filePathCallback: ValueCallback<Array<Uri>>,
                fileChooserParams: WebChromeClient.FileChooserParams
            ): Boolean {

                if (!GlobalParameter.settings.isUploadFeature) {
                    super.onShowFileChooser(webView, filePathCallback, fileChooserParams)
                    return true
                }

                if (mUMA != null) {
                    mUMA?.onReceiveValue(null)
                }
                mUMA = filePathCallback
                var takePictureIntent: Intent? = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                if (takePictureIntent?.resolveActivity(this@ExampleActivity.packageManager) != null) {
                    var photoFile: File? = null
                    try {
                        photoFile = createImageFile()
                        takePictureIntent.putExtra("PhotoPath", mCM)
                    } catch (ex: IOException) {
                        Log.e("Webview", "Image file creation failed", ex)
                    }
                    if (photoFile != null) {
                        mCM = "file:" + photoFile.absolutePath
                        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photoFile))
                    } else {
                        takePictureIntent = null
                    }
                }
                val contentSelectionIntent = Intent(Intent.ACTION_GET_CONTENT)
                contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE)
                contentSelectionIntent.type = "*/*"
                val intentArray: Array<Intent>
                if (takePictureIntent != null) {
                    intentArray = arrayOf<Intent>(takePictureIntent)
                } else {
                    intentArray = emptyArray()
                }
                val chooserIntent = Intent(Intent.ACTION_CHOOSER)
                chooserIntent.putExtra(Intent.EXTRA_INTENT, contentSelectionIntent)
                chooserIntent.putExtra(Intent.EXTRA_TITLE, "Image Chooser")
                chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentArray)
                startActivityForResult(chooserIntent, FCR)
                return true
            }

            fun openFileChooser(uploadMsg: ValueCallback<Uri>) {
                if (!GlobalParameter.settings.isUploadFeature) {
                    return
                }
                this.openFileChooser(uploadMsg, "*/*")
            }

            fun openFileChooser(uploadMsg: ValueCallback<Uri>, acceptType: String) {
                if (!GlobalParameter.settings.isUploadFeature) {
                    return
                }
                this.openFileChooser(uploadMsg, acceptType, null!!)
            }

            fun openFileChooser(uploadMsg: ValueCallback<Uri>, acceptType: String, capture: String) {


                if (!GlobalParameter.settings.isUploadFeature) {
                    return
                }
                val i = Intent(Intent.ACTION_GET_CONTENT)
                i.addCategory(Intent.CATEGORY_OPENABLE)
                i.type = "*/*"
                this@ExampleActivity.startActivityForResult(
                    Intent.createChooser(i, "File Browser"),
                    FILECHOOSER_RESULTCODE
                )
            }
        }

        webChromeClient!!.setOnToggledFullscreen(object : VideoEnabledWebChromeClient.ToggledFullscreenCallback {
            @SuppressLint("ObsoleteSdkInt")
            override fun toggledFullscreen(fullscreen: Boolean) {
                // Your code to handle the full-screen change, for example showing and hiding the title bar. Example:
                if (fullscreen) {
                    val attrs = window.attributes
                    attrs.flags = attrs.flags or WindowManager.LayoutParams.FLAG_FULLSCREEN
                    attrs.flags = attrs.flags or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                    window.attributes = attrs
                    if (Build.VERSION.SDK_INT >= 14) {

                        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LOW_PROFILE
                    }
                } else {
                    val attrs = window.attributes
                    attrs.flags = attrs.flags and WindowManager.LayoutParams.FLAG_FULLSCREEN.inv()
                    attrs.flags = attrs.flags and WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON.inv()
                    window.attributes = attrs
                    if (Build.VERSION.SDK_INT >= 14) {

                        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
                    }
                }

            }
        })
    }

    private fun webViewSettings() {
        webView.settings.javaScriptEnabled = true
        webView.settings?.domStorageEnabled = true
        webView.settings?.setAppCachePath("caches")
        webView.settings?.setAppCacheEnabled(true)
        webView.settings?.savePassword = true
        webView.settings?.saveFormData = true
        webView.settings?.builtInZoomControls = true //zoom yapılmasına izin verir
        webView.settings?.setSupportZoom(true)
        webView.settings?.javaScriptCanOpenWindowsAutomatically = true
        webView.settings?.allowFileAccess = true
        webView.settings?.domStorageEnabled = true
        webView.settings?.setRenderPriority(WebSettings.RenderPriority.HIGH)
        //webView.isVideoFullscreen = false

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            // Chronium özelliği ile donanım hız ayarlamasını etkinleştirir
            webView.setLayerType(View.LAYER_TYPE_HARDWARE, null)
        } else {
            // eski versiyonlar için donanım hız ayarlamasını etkisizleştirir
            webView.setLayerType(View.LAYER_TYPE_SOFTWARE, null)
        }
    }

    private fun webviewLoadListener() {
//        val progressDialog: ProgressDialog = ProgressDialog.show(
//            this, "Lütfen Bekleyin",
//            "Sayfa Yükleniyor...", true
//        )
        findViewById<HiveProgressView>(R.id.hive_progress).visibility = View.VISIBLE
        webView.webViewClient = object : WebViewClient() {

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                Log.i("Test", "onPageStarted")
                webSiteEntryCount++
                mInterstitialAd?.let {
                    if (it.isLoaded && webSiteEntryCount % admobSplashRetryCount == 0) {
                        mInterstitialAd?.show()
                    }
                }

                super.onPageStarted(view, url, favicon)
            }

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
//                if (progressDialog.isShowing)
//                    progressDialog.dismiss()

                findViewById<HiveProgressView>(R.id.hive_progress).visibility = View.GONE

                showToolbar(url)
                configureNavigationDrawer()
                Log.i("Test", "PageFinished")
            }

            override fun onPageCommitVisible(view: WebView?, url: String?) {
                Log.i("Test", "onPageCommitVisible")
                super.onPageCommitVisible(view, url)
            }

            override fun shouldOverrideUrlLoading(view: WebView, url: String?): Boolean {
                findViewById<HiveProgressView>(R.id.hive_progress).visibility = View.VISIBLE
                view.loadUrl(url)

                return true
            }
        }

        webView.setDownloadListener { url, userAgent, contentDisposition, mimetype, contentLength ->
            var request = DownloadManager.Request(Uri.parse(url))
            request.allowScanningByMediaScanner()

            request.setNotificationVisibility(
                DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED
            )

            request.setDestinationInExternalPublicDir(
                Environment.DIRECTORY_DOWNLOADS,    //Download folder
                "download"
            )                        //Name of file


            var dm: DownloadManager = getSystemService(
                DOWNLOAD_SERVICE
            ) as DownloadManager

            dm.enqueue(request)
        }
    }

    private fun showToolbar(url: String) {

        toolbar = findViewById(R.id.toolbar)
        toolbarContainer = findViewById(R.id.toolbar_container)
        if (!GlobalParameter.settings.showToolbar) {

            toolbar.visibility = View.GONE
            toolbarContainer.visibility = View.GONE
            return
        }

        toolbar.visibility = View.VISIBLE
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        toolbar.title = webView.title
        toolbar.subtitle = Utility.getHost(url)

        if (GlobalParameter.settings.showDrawerMenu) {
            var actionbar: ActionBar? = supportActionBar
            actionbar?.setHomeAsUpIndicator(R.drawable.ic_drawer_ios)
            actionbar?.setDisplayHomeAsUpEnabled(true)
        }

    }

    fun configureNavigationDrawer() {
        drawerLayout = findViewById(R.id.drawer_layout)
        var navView: NavigationView = findViewById(R.id.navigation)
        var nav_Menu: Menu = navView.menu

        if (!GlobalParameter.settings.showDrawerMenu) {
            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
        }


        // Nav Items
        nav_Menu.findItem(R.id.nav_1).isVisible = false
        nav_Menu.findItem(R.id.nav_2).isVisible = false
        nav_Menu.findItem(R.id.nav_3).isVisible = false
        nav_Menu.findItem(R.id.nav_4).isVisible = false
        nav_Menu.findItem(R.id.nav_5).isVisible = false
        nav_Menu.findItem(R.id.nav_6).isVisible = false
        nav_Menu.findItem(R.id.nav_7).isVisible = false
        nav_Menu.findItem(R.id.nav_8).isVisible = false
        nav_Menu.findItem(R.id.nav_9).isVisible = false
        nav_Menu.findItem(R.id.nav_10).isVisible = false
        loop@ for ((index, value) in GlobalParameter.settings.menuSettings.withIndex()) {

            when (index) {

                0 -> {
                    // Nav Items
                    nav_Menu.findItem(R.id.nav_1).isVisible = true
                    nav_Menu.findItem(R.id.nav_1).title = value.title
                }
                1 -> {

                    // Nav Items
                    nav_Menu.findItem(R.id.nav_2).isVisible = true
                    nav_Menu.findItem(R.id.nav_2).title = value.title
                }
                2 -> {

                    // Nav Items
                    nav_Menu.findItem(R.id.nav_3).isVisible = true
                    nav_Menu.findItem(R.id.nav_3).title = value.title
                }
                3 -> {

                    // Nav Items
                    nav_Menu.findItem(R.id.nav_4).isVisible = true
                    nav_Menu.findItem(R.id.nav_4).title = value.title
                }
                4 -> {

                    // Nav Items
                    nav_Menu.findItem(R.id.nav_5).isVisible = true
                    nav_Menu.findItem(R.id.nav_5).title = value.title
                }
                5 -> {

                    // Nav Items
                    nav_Menu.findItem(R.id.nav_6).isVisible = true
                    nav_Menu.findItem(R.id.nav_6).title = value.title
                }
                6 -> {

                    // Nav Items
                    nav_Menu.findItem(R.id.nav_7).isVisible = true
                    nav_Menu.findItem(R.id.nav_7).title = value.title
                }
                7 -> {

                    // Nav Items
                    nav_Menu.findItem(R.id.nav_8).isVisible = true
                    nav_Menu.findItem(R.id.nav_8).title = value.title
                }
                8 -> {

                    // Nav Items
                    nav_Menu.findItem(R.id.nav_9).isVisible = true
                    nav_Menu.findItem(R.id.nav_9).title = value.title
                }
                9 -> {

                    // Nav Items
                    nav_Menu.findItem(R.id.nav_10).isVisible = true
                    nav_Menu.findItem(R.id.nav_10).title = value.title
                }

                else -> {
                    continue@loop
                }
            }
        }


        nav_Menu.findItem(R.id.contact_container).isVisible = true

        navView.setNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.nav_1 -> {
                    webView.loadUrl(GlobalParameter.settings.menuSettings[0].url)
                }
                R.id.nav_2 -> {
                    webView.loadUrl(GlobalParameter.settings.menuSettings[1].url)
                }
                R.id.nav_3 -> {
                    webView.loadUrl(GlobalParameter.settings.menuSettings[2].url)
                }
                R.id.nav_4 -> {
                    webView.loadUrl(GlobalParameter.settings.menuSettings[3].url)
                }
                R.id.nav_5 -> {
                    webView.loadUrl(GlobalParameter.settings.menuSettings[4].url)
                }
                R.id.nav_6 -> {
                    webView.loadUrl(GlobalParameter.settings.menuSettings[5].url)
                }
                R.id.nav_7 -> {
                    webView.loadUrl(GlobalParameter.settings.menuSettings[6].url)
                }
                R.id.nav_8 -> {
                    webView.loadUrl(GlobalParameter.settings.menuSettings[7].url)
                }
                R.id.nav_9 -> {
                    webView.loadUrl(GlobalParameter.settings.menuSettings[8].url)
                }
                R.id.nav_10 -> {
                    webView.loadUrl(GlobalParameter.settings.menuSettings[9].url)
                }
                R.id.nav_iletisim -> {

                    var i = Intent(this, İletisimActivity::class.java)
                    startActivity(i)
                }
                R.id.nav_hakkimizda -> {
                    var i = Intent(this, HakkindaActivity::class.java)
                    startActivity(i)
                }
                else -> {
                    drawerLayout.closeDrawers()
                    false
                }

            }
            drawerLayout.closeDrawers()
            true
        }
    }


    private inner class InsideWebViewClient : WebViewClient() {
        override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
            //view.loadUrl(url)
            view.loadUrl(url)
            return true
        }

        override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)
            Log.i("Test", "PageFinished")
        }
    }

    private fun showWebViewDownloader() {
        webView.setDownloadListener { url, userAgent, contentDisposition, mimetype, contentLength ->
            //getting file name from url.
            val filename = URLUtil.guessFileName(url, contentDisposition, mimetype)
            //Alertdialog
            val builder = AlertDialog.Builder(this@ExampleActivity)
            //title for AlertDialog
            builder.setTitle("Uyarı")
            //message of AlertDialog
            builder.setMessage("$filename isimli dosyayı indirmek istediğinize emin misiniz ?")
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
                        val builder = AlertDialog.Builder(this@ExampleActivity)
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

    private fun showSplash() {

        // Remember that you should never show the action bar if the
        // status bar is hidden, so hide that too if necessary.
        splashScreenDialog = Dialog(this, R.style.SplashAppTheme)
        splashScreenDialog?.setContentView(R.layout.splash)
        //var hiveProgressView:HiveProgressView = splashScreenDialog!!.findViewById(R.id.hive_progress)
        //hiveProgressView.color =

        splashScreenDialog!!.show()
        val handler = Handler()
        handler.postDelayed({
            splashScreenDialog?.dismiss()
        }, 3000)
    }

    override fun onPermissionsGranted(requestCode: Int) {
        if (requestCode == REQUEST_PERMISSION_READ_CONTACTS) {

            Toast.makeText(this, "Permissions Received for reading contacts.", Toast.LENGTH_LONG).show()

        } else if (requestCode == REQUEST_PERMISSION_WRITE_EXTERNAL_STORAGE) {

            //Toast.makeText(this, "İzin verildi.Teşekkürler...", Toast.LENGTH_LONG).show()

        }

    }

    /**
    override fun onCreateContextMenu(contextMenu:ContextMenu, view:View, contextMenuInfo:ContextMenu.ContextMenuInfo) {
    super.onCreateContextMenu(contextMenu, view, contextMenuInfo)
    val webViewHitTestResult = webView.getHitTestResult()
    if ((webViewHitTestResult.getType() === WebView.HitTestResult.IMAGE_TYPE || webViewHitTestResult.getType() === WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE))
    {
    contextMenu.setHeaderTitle("Download Image...")
    contextMenu.setHeaderIcon(R.drawable.ic_search_black_24dp)
    contextMenu.add(0, 1, 0, "Click to download")
    .setOnMenuItemClickListener {
    val DownloadImageURL = webViewHitTestResult.getExtra()
    if (URLUtil.isValidUrl(DownloadImageURL)) {
    val mRequest = DownloadManager.Request(Uri.parse(DownloadImageURL))
    mRequest.allowScanningByMediaScanner()
    mRequest.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
    val mDownloadManager = getSystemService(DOWNLOAD_SERVICE) as DownloadManager
    mDownloadManager.enqueue(mRequest)
    Toast.makeText(this@ExampleActivity, "Image Downloaded Successfully...", Toast.LENGTH_LONG).show()
    } else {
    Toast.makeText(this@ExampleActivity, "Sorry.. Something Went Wrong...", Toast.LENGTH_LONG).show()
    }
    false
    }
    }
    }
     **/

    override fun onDestroy() {
        unregisterReceiver(onDownloadComplete)
        super.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        webView.saveState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle?) {
        super.onRestoreInstanceState(savedInstanceState)
        webView.restoreState(savedInstanceState)
    }


    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.right_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {

        when (item?.itemId) {
            R.id.menu_copylink -> {
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager?
                clipboard!!.text = webView.url
                val snackbar = Snackbar.make(
                    webView, webView.url,
                    Snackbar.LENGTH_LONG
                )
                val snackbarView = snackbar.view
                snackbarView.setBackgroundColor(Color.RED)
                snackbar.show()
            }
            R.id.menu_share -> {
                val sendIntent = Intent()
                sendIntent.action = Intent.ACTION_SEND
                sendIntent.putExtra(Intent.EXTRA_TEXT, webView.url)
                sendIntent.type = "text/plain"
                startActivity(Intent.createChooser(sendIntent, "Seçimini Yap"))
            }
            R.id.menu_search -> {
                webView.showFindDialog("", true)
            }
            R.id.menu_refresh -> {
                webView.reload()
            }
            android.R.id.home -> {
                drawerLayout.openDrawer(GravityCompat.START)
            }
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)
        if (Build.VERSION.SDK_INT >= 21) {
            var results: Array<Uri>? = null
            //Check if response is positive
            if (resultCode == Activity.RESULT_OK) {
                if (requestCode == FCR) {
                    if (null == mUMA) {
                        return
                    }
                    if (intent == null) {
                        //Capture Photo if no image available
                        if (mCM != null) {
                            results = arrayOf<Uri>(Uri.parse(mCM))
                        }
                    } else {
                        val dataString = intent.dataString
                        if (dataString != null) {
                            results = arrayOf<Uri>(Uri.parse(dataString))
                        }
                    }
                }
            }
            mUMA?.onReceiveValue(results)
            mUMA = null
        } else {
            if (requestCode == FCR) {
                if (null == mUM) return
                val result = if (intent == null || resultCode != RESULT_OK) null else intent.data
                mUM?.onReceiveValue(result)
                mUM = null
            }
        }
    }

    // Create an image file
    @Throws(IOException::class)
    private fun createImageFile(): File {
        @SuppressLint("SimpleDateFormat") val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val imageFileName = "img_" + timeStamp + "_"
        val storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(imageFileName, ".jpg", storageDir)
    }
}