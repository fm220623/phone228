package com.example.financemanager.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import java.io.File

object ImageUtils {

    fun showToast(context: Context, message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    fun loadBitmapFromUri(context: Context, uriString: String?): Bitmap? {
        if (uriString.isNullOrEmpty()) return null

        return try {
            val uri = Uri.parse(uriString)
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BitmapFactory.decodeStream(inputStream)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun isDemoPhoto(uriString: String?): Boolean {
        return uriString?.startsWith("camera_photo_") == true
    }
}