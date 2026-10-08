package com.liftley.intelligate.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.math.roundToInt

class PlateImageProcessor(private val context: Context) {
    private val directory get() = File(context.cacheDir, "captures").apply { mkdirs() }

    init {
        // A fresh process never resumes old captures.
        directory.listFiles()?.forEach { it.delete() }
    }

    fun createCaptureUri(): Uri {
        val file = File.createTempFile("plate_", ".jpg", directory)
        return FileProvider.getUriForFile(context, "${context.packageName}.files", file)
    }

    /** Keeps the full photo. ImageDecoder corrects EXIF orientation while bounding memory usage. */
    suspend fun prepare(uri: Uri): ByteArray = withContext(Dispatchers.IO) {
        val bitmap = ImageDecoder.decodeBitmap(
            ImageDecoder.createSource(
                context.contentResolver,
                uri
            )
        ) { decoder, info, _ ->
            val scale = minOf(1f, 2048f / maxOf(info.size.width, info.size.height))
            decoder.setTargetSize(
                (info.size.width * scale).roundToInt().coerceAtLeast(1),
                (info.size.height * scale).roundToInt().coerceAtLeast(1),
            )
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        }
        try {
            for (quality in listOf(90, 85, 80)) {
                val output = ByteArrayOutputStream()
                check(bitmap.compress(Bitmap.CompressFormat.JPEG, quality, output))
                if (output.size() <= 1_000_000) return@withContext output.toByteArray()
            }
            error("This photo is too large. Move closer to the plate and retake it.")
        } finally {
            bitmap.recycle()
        }
    }

    fun deleteCapture(uri: Uri) {
        runCatching { context.contentResolver.delete(uri, null, null) }
    }
}

