package com.example.photoweather.utils

import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import android.widget.Toast
import java.io.*
import java.util.*


fun Context.showToast(msg:String){
    Toast.makeText(this,msg,Toast.LENGTH_SHORT).show()
}

fun Context.getImageUri(bitmap:Bitmap): Uri? {
    val bytes = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes)
    val path =
        MediaStore.Images.Media.insertImage(this.contentResolver,bitmap, "", null)
    val uri = Uri.parse(path)
    return uri
}

// Method to save an bitmap to a file
fun Context.bitmapToFile(bitmap:Bitmap): Uri {
    // Initialize a new file instance to save bitmap object
    var file = this.getDir("Images",Context.MODE_PRIVATE)
    file = File(file.parent)
    try{
        // Compress the bitmap and save in jpg format
        val stream: OutputStream = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.JPEG,100,stream)
        stream.flush()
        stream.close()
    }catch (e: IOException){
        e.printStackTrace()
    }
    file.deleteOnExit()
    // Return the saved bitmap uri
    return Uri.parse(file.absolutePath)
}