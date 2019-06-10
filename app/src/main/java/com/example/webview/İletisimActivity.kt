package com.example.webview

import android.Manifest
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.RelativeLayout
import androidx.appcompat.app.ActionBar
import androidx.appcompat.widget.Toolbar
import androidx.constraintlayout.widget.Group
import androidx.core.view.GravityCompat
import com.example.webview.model.GlobalParameter
import com.google.android.material.snackbar.Snackbar


class İletisimActivity : RuntimePermissionsActivity() {


    private val REQUEST_PERMISSION_CALL_PHONE = 3

    private lateinit var toolbar: Toolbar
    private lateinit var toolbarContainer: RelativeLayout

    private lateinit var araGroup: Group
    private lateinit var mailGroup: Group

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_iletisim)

        araGroup = findViewById(R.id.ara_group)
        araGroup.setOnClickListener {

            requestAppPermissions(
                arrayOf(Manifest.permission.CALL_PHONE),
                R.string.permissions_call_phone_txt,
                REQUEST_PERMISSION_CALL_PHONE
            )
        }

        mailGroup = findViewById(R.id.mail_group)
        mailGroup.setOnClickListener {
            Utility.sendMail(this)
        }

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
        toolbar.title = resources.getString(R.string.iletisim)
        var actionbar: ActionBar? = supportActionBar
        actionbar?.setDisplayHomeAsUpEnabled(true)
        actionbar?.setDisplayShowHomeEnabled(true)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onPermissionsGranted(requestCode: Int) {
        if (REQUEST_PERMISSION_CALL_PHONE == requestCode) {

            val intent =
                Intent(Intent.ACTION_CALL, Uri.parse("tel:" + "" + resources.getString(R.string.iletisim_numara)))
            startActivity(intent)
        }

    }

}
