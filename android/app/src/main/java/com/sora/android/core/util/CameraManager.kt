package com.sora.android.core.util

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CameraManager @Inject constructor() {

    fun createImageFile(context: Context): Pair<File, Uri> {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = context.getExternalFilesDir("Pictures")

        val imageFile = File.createTempFile(
            "SORA_${timeStamp}_",
            ".jpg",
            storageDir
        )

        val imageUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            imageFile
        )

        return Pair(imageFile, imageUri)
    }

    fun deleteImageFile(file: File) {
        if (file.exists()) {
            file.delete()
        }
    }
}
