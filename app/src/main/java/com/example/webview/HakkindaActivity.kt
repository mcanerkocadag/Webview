package com.example.webview

import android.os.Bundle
import android.view.View
import android.widget.RelativeLayout
import androidx.appcompat.app.ActionBar
import androidx.appcompat.widget.Toolbar
import com.example.webview.model.GlobalParameter

class HakkindaActivity : RuntimePermissionsActivity() {

    private lateinit var toolbar: Toolbar
    private lateinit var toolbarContainer: RelativeLayout


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_hakkinda)

        showToolbar()
    }

    private fun showToolbar() {

        toolbar = findViewById(R.id.toolbar)
        toolbarContainer = findViewById(R.id.toolbar_container)
        if (!GlobalParameter.settings.showToolbar) {

            toolbar.visibility = View.GONE
            toolbarContainer.visibility = View.GONE
            return
        }

        toolbar.visibility = View.VISIBLE
        setSupportActionBar(toolbar)
        toolbar.title = resources.getString(R.string.hakkinda).toString()
        var actionbar: ActionBar? = supportActionBar
        actionbar?.setDisplayHomeAsUpEnabled(true)
        actionbar?.setDisplayShowHomeEnabled(true)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onPermissionsGranted(requestCode: Int) {}
}
