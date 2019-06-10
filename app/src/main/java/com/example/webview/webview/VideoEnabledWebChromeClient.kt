package com.example.webview.webview

import android.media.MediaPlayer
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.webkit.WebChromeClient


open class VideoEnabledWebChromeClient : WebChromeClient, MediaPlayer.OnPreparedListener, MediaPlayer.OnCompletionListener,
    MediaPlayer.OnErrorListener {

    private var activityNonVideoView: View? = null
    private var activityVideoView: ViewGroup? = null
    private var loadingView: View? = null
    private var webView: VideoEnabledWebView? = null

    /**
     * Indicates if the video is being displayed using a custom view (typically full-screen)
     * @return true it the video is being displayed using a custom view (typically full-screen)
     */
    var isVideoFullscreen: Boolean = false
        private set // Indicates if the video is being displayed using a custom view (typically full-screen)
    private var videoViewContainer: FrameLayout? = null
    private var videoViewCallback: CustomViewCallback? = null

    private var toggledFullscreenCallback: ToggledFullscreenCallback? = null

    interface ToggledFullscreenCallback {
        fun toggledFullscreen(fullscreen: Boolean)
    }

    /**
     * Never use this constructor alone.
     * This constructor allows this class to be defined as an inline inner class in which the user can override methods
     */
    constructor() {}

    /**
     * Builds a video enabled WebChromeClient.
     * @param activityNonVideoView A View in the activity's layout that contains every other view that should be hidden when the video goes full-screen.
     * @param activityVideoView A ViewGroup in the activity's layout that will display the video. Typically you would like this to fill the whole layout.
     */
    constructor(activityNonVideoView: View, activityVideoView: ViewGroup) {
        this.activityNonVideoView = activityNonVideoView
        this.activityVideoView = activityVideoView
        this.loadingView = null
        this.webView = null
        this.isVideoFullscreen = false
    }

    /**
     * Builds a video enabled WebChromeClient.
     * @param activityNonVideoView A View in the activity's layout that contains every other view that should be hidden when the video goes full-screen.
     * @param activityVideoView A ViewGroup in the activity's layout that will display the video. Typically you would like this to fill the whole layout.
     * @param loadingView A View to be shown while the video is loading (typically only used in API level <11). Must be already inflated and not attached to a parent view.
     */
    constructor(activityNonVideoView: View, activityVideoView: ViewGroup, loadingView: View) {
        this.activityNonVideoView = activityNonVideoView
        this.activityVideoView = activityVideoView
        this.loadingView = loadingView
        this.webView = null
        this.isVideoFullscreen = false
    }

    /**
     * Builds a video enabled WebChromeClient.
     * @param activityNonVideoView A View in the activity's layout that contains every other view that should be hidden when the video goes full-screen.
     * @param activityVideoView A ViewGroup in the activity's layout that will display the video. Typically you would like this to fill the whole layout.
     * @param loadingView A View to be shown while the video is loading (typically only used in API level <11). Must be already inflated and not attached to a parent view.
     * @param webView The owner VideoEnabledWebView. Passing it will enable the VideoEnabledWebChromeClient to detect the HTML5 video ended event and exit full-screen.
     * Note: The web page must only contain one video tag in order for the HTML5 video ended event to work. This could be improved if needed (see Javascript code).
     */
    constructor(
        activityNonVideoView: View,
        activityVideoView: ViewGroup,
        loadingView: View,
        webView: VideoEnabledWebView
    ) {
        this.activityNonVideoView = activityNonVideoView
        this.activityVideoView = activityVideoView
        this.loadingView = loadingView
        this.webView = webView
        this.isVideoFullscreen = false
    }

    /**
     * Set a callback that will be fired when the video starts or finishes displaying using a custom view (typically full-screen)
     * @param callback A VideoEnabledWebChromeClient.ToggledFullscreenCallback callback
     */
    fun setOnToggledFullscreen(callback: ToggledFullscreenCallback) {
        this.toggledFullscreenCallback = callback
    }

    override fun onShowCustomView(view: View, callback: CustomViewCallback) {
        if (view is FrameLayout) {
            // A video wants to be shown
            val frameLayout = view as FrameLayout
            val focusedChild = frameLayout.focusedChild

            // Save video related variables
            this.isVideoFullscreen = true
            this.videoViewContainer = frameLayout
            this.videoViewCallback = callback

            // Hide the non-video view, add the video view, and show it
            activityNonVideoView!!.setVisibility(View.INVISIBLE)
            activityVideoView!!.addView(
                videoViewContainer,
                ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            )
            activityVideoView!!.visibility = View.VISIBLE

            if (focusedChild is android.widget.VideoView) {
                // android.widget.VideoView (typically API level <11)

                // Handle all the required events
                focusedChild.setOnPreparedListener(this)
                focusedChild.setOnCompletionListener(this)
                focusedChild.setOnErrorListener(this)
            } else {
                // Other classes, including:
                // - android.webkit.HTML5VideoFullScreen$VideoSurfaceView, which inherits from android.view.SurfaceView (typically API level 11-18)
                // - android.webkit.HTML5VideoFullScreen$VideoTextureView, which inherits from android.view.TextureView (typically API level 11-18)
                // - com.android.org.chromium.content.browser.ContentVideoView$VideoSurfaceView, which inherits from android.view.SurfaceView (typically API level 19+)

                // Handle HTML5 video ended event only if the class is a SurfaceView
                // Test case: TextureView of Sony Xperia T API level 16 doesn't work fullscreen when loading the javascript below
                if (webView != null && webView!!.getSettings().getJavaScriptEnabled() && focusedChild is SurfaceView) {
                    // Run javascript code that detects the video end and notifies the Javascript interface
                    var js = "javascript:"
                    js += "var _ytrp_html5_video_last;"
                    js += "var _ytrp_html5_video = document.getElementsByTagName('video')[0];"
                    js += "if (_ytrp_html5_video != undefined && _ytrp_html5_video != _ytrp_html5_video_last) {"
                    run {
                        js += "_ytrp_html5_video_last = _ytrp_html5_video;"
                        js += "function _ytrp_html5_video_ended() {"
                        run {
                            js += "_VideoEnabledWebView.notifyVideoEnd();" // Must match Javascript interface name and method of VideoEnableWebView
                        }
                        js += "}"
                        js += "_ytrp_html5_video.addEventListener('ended', _ytrp_html5_video_ended);"
                    }
                    js += "}"
                    webView!!.loadUrl(js)
                }
            }

            // Notify full-screen change
            if (toggledFullscreenCallback != null) {
                toggledFullscreenCallback!!.toggledFullscreen(true)
            }
        }
    }

    override fun onShowCustomView(
        view: View,
        requestedOrientation: Int,
        callback: CustomViewCallback
    ) // Available in API level 14+, deprecated in API level 18+
    {
        onShowCustomView(view, callback)
    }

    override fun onHideCustomView() {
        // This method should be manually called on video end in all cases because it's not always called automatically.
        // This method must be manually called on back key press (from this class' onBackPressed() method).

        if (isVideoFullscreen) {
            // Hide the video view, remove it, and show the non-video view
            activityVideoView!!.visibility = View.INVISIBLE
            activityVideoView!!.removeView(videoViewContainer)
            activityNonVideoView!!.setVisibility(View.VISIBLE)

            // Call back (only in API level <19, because in API level 19+ with chromium webview it crashes)
            if (videoViewCallback != null && !videoViewCallback!!.javaClass.name.contains(".chromium.")) {
                videoViewCallback!!.onCustomViewHidden()
            }

            // Reset video related variables
            isVideoFullscreen = false
            videoViewContainer = null
            videoViewCallback = null

            // Notify full-screen change
            if (toggledFullscreenCallback != null) {
                toggledFullscreenCallback!!.toggledFullscreen(false)
            }
        }
    }

    override fun getVideoLoadingProgressView() // Video will start loading
            : View? {
        if (loadingView != null) {
            loadingView!!.setVisibility(View.VISIBLE)
            return loadingView
        } else {
            return super.getVideoLoadingProgressView()
        }
    }

    override fun onPrepared(mp: MediaPlayer) // Video will start playing, only called in the case of android.widget.VideoView (typically API level <11)
    {
        if (loadingView != null) {
            loadingView!!.setVisibility(View.GONE)
        }
    }

    override fun onCompletion(mp: MediaPlayer) // Video finished playing, only called in the case of android.widget.VideoView (typically API level <11)
    {
        onHideCustomView()
    }

    override fun onError(
        mp: MediaPlayer,
        what: Int,
        extra: Int
    ) // Error while playing video, only called in the case of android.widget.VideoView (typically API level <11)
            : Boolean {
        return false // By returning false, onCompletion() will be called
    }

    /**
     * Notifies the class that the back key has been pressed by the user.
     * This must be called from the Activity's onBackPressed(), and if it returns false, the activity itself should handle it. Otherwise don't do anything.
     * @return Returns true if the event was handled, and false if was not (video view is not visible)
     */
    fun onBackPressed(): Boolean {
        if (isVideoFullscreen) {
            onHideCustomView()
            return true
        } else {
            return false
        }
    }



}