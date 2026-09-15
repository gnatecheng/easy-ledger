package com.qingjizhang.app.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import kotlin.math.max

class ReceiptStore(private val context: Context) {
    val dir: File = File(context.filesDir, "receipts").apply { mkdirs() }

    fun file(name: String): File = File(dir, name)

    fun exists(name: String?): Boolean = !name.isNullOrBlank() && file(name).exists()

    fun captureUri(): Pair<Uri, String> {
        val name = "cap_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}.jpg"
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file(name))
        return uri to name
    }

    fun importFromUri(uri: Uri): String? {
        val name = "r_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}.jpg"
        val out = file(name)
        return try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                val bytes = input.readBytes()
                compress(bytes, out)
            } ?: return null
            name
        } catch (_: Exception) {
            out.delete()
            null
        }
    }

    fun finalizeCapture(name: String): String? {
        val src = file(name)
        if (!src.exists()) return null
        return try {
            val bytes = src.readBytes()
            compress(bytes, src)
            name
        } catch (_: Exception) {
            name.takeIf { src.exists() }
        }
    }

    fun delete(name: String?) {
        if (name.isNullOrBlank()) return
        file(name).delete()
    }

    fun deleteAll() {
        dir.listFiles()?.forEach { it.delete() }
    }

    fun decodeThumb(name: String?, maxPx: Int = 256): Bitmap? {
        val f = name?.let { file(it) }?.takeIf { it.exists() } ?: return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(f.absolutePath, bounds)
        val sample = max(1, max(bounds.outWidth, bounds.outHeight) / maxPx)
        return BitmapFactory.decodeFile(
            f.absolutePath,
            BitmapFactory.Options().apply { inSampleSize = sample },
        )
    }

    private fun compress(bytes: ByteArray, out: File) {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        val longest = max(bounds.outWidth, bounds.outHeight).coerceAtLeast(1)
        val sample = if (longest > 1600) longest / 1600 else 1
        val bmp = BitmapFactory.decodeByteArray(
            bytes, 0, bytes.size,
            BitmapFactory.Options().apply { inSampleSize = sample },
        ) ?: run {
            out.writeBytes(bytes)
            return
        }
        FileOutputStream(out).use { fos ->
            bmp.compress(Bitmap.CompressFormat.JPEG, 85, fos)
        }
        if (!bmp.isRecycled) bmp.recycle()
    }
}
