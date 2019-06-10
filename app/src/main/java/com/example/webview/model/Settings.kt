package com.example.webview.model

data class Settings(
    var showToolbar: Boolean = false,
    var showAdmob: Boolean = false,
    var showInterstitialAdmob: Boolean = false,
    var isVideoFullScreen: Boolean = false,
    var showSplashScreen: Boolean = false,
    var isDownloadFeature: Boolean = false,
    var isUploadFeature: Boolean = false,
    var showDrawerMenu: Boolean = false,
    var menuSettings: ArrayList<MenuSetting> = ArrayList()
)