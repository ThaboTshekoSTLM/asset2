package za.gov.municipal.ictasset.presentation.util

import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object AssetPhotoFiles {
    fun createPhotoFile(context: Context): File {
        val directory = File(
            context.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
            "asset_photos"
        ).also { it.mkdirs() }
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        return File(directory, "asset_$stamp.jpg")
    }

    fun uriFor(context: Context, file: File): Uri =
        FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
}
