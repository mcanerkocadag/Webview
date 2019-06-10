package com.example.webview

import androidx.core.app.ActivityCompat
import androidx.core.view.accessibility.AccessibilityEventCompat.setAction
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.startActivity
import android.content.Intent
import android.net.Uri
import android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS
import android.util.SparseIntArray
import android.os.Bundle
import android.provider.Settings
import androidx.annotation.Nullable
import androidx.appcompat.app.AppCompatActivity
import android.view.View
import android.widget.Toast
import com.google.android.material.snackbar.Snackbar


abstract class RuntimePermissionsActivity : AppCompatActivity() {

    private var mErrorString: SparseIntArray? = null

    override fun onCreate(@Nullable savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        mErrorString = SparseIntArray()

    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {

        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        var permissionCheck = PackageManager.PERMISSION_GRANTED

        for (permission in grantResults) {

            permissionCheck = permissionCheck + permission

        }

        if (grantResults.size > 0 && permissionCheck == PackageManager.PERMISSION_GRANTED) {

            onPermissionsGranted(requestCode)

        } else {

            Snackbar.make(
                findViewById<View>(android.R.id.content), mErrorString!!.get(requestCode),

                Snackbar.LENGTH_INDEFINITE
            ).setAction(
                "TAMAM"

            ) {
                val intent = Intent()

                intent.action = ACTION_APPLICATION_DETAILS_SETTINGS

                intent.addCategory(Intent.CATEGORY_DEFAULT)

                intent.data = Uri.parse("package:$packageName")

                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

                intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)

                intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)

                startActivity(intent)
            }.show()

        }

    }

    fun requestAppPermissions(
        requestedPermissions: Array<String>,

        stringId: Int, requestCode: Int
    ) {

        mErrorString!!.put(requestCode, stringId)

        var permissionCheck = PackageManager.PERMISSION_GRANTED

        var shouldShowRequestPermissionRationale = false

        for (permission in requestedPermissions) {

            permissionCheck = permissionCheck + ContextCompat.checkSelfPermission(this, permission)

            shouldShowRequestPermissionRationale =
                shouldShowRequestPermissionRationale || ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    permission
                )

        }

        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {

            if (!shouldShowRequestPermissionRationale) {

                Snackbar.make(
                    findViewById<View>(android.R.id.content), stringId,

                    Snackbar.LENGTH_INDEFINITE
                ).setAction(
                    "TAMAM"

                ) {
                    ActivityCompat.requestPermissions(
                        this@RuntimePermissionsActivity,
                        requestedPermissions,
                        requestCode
                    )
                }.show()

            } else {

                Toast.makeText(
                    this,
                    "Lütfen bu işlemi yapabilmek için uygulama-uygulama izinleri bölümünden izinlerini veriniz.",
                    Toast.LENGTH_LONG
                ).show()

                val intent = Intent()

                intent.action = ACTION_APPLICATION_DETAILS_SETTINGS

                intent.addCategory(Intent.CATEGORY_DEFAULT)

                intent.data = Uri.parse("package:$packageName")

                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

                intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)

                intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)

                startActivity(intent)

            }

        } else {

            onPermissionsGranted(requestCode)

        }

    }

    abstract fun onPermissionsGranted(requestCode: Int)

}

