package com.example.photoweather.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

class PermissionUtil {
    companion object {
        fun hasPermissions(context: Context): Boolean {
            val writePerm = ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            val camera = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)

            return camera == PackageManager.PERMISSION_GRANTED &&
                    writePerm == PackageManager.PERMISSION_GRANTED
        }
    }

}
