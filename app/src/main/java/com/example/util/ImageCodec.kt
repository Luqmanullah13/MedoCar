package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.roundToInt

object ImageCodec {
    private const val MAX_DIM_PX = 840
    private const val MAX_BASE64_LENGTH = 220_000

    fun uriToCompressedDataUri(context: Context, uri: Uri): String? {
        return try {
            val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, boundsOpts)
            }
            val origW = boundsOpts.outWidth
            val origH = boundsOpts.outHeight
            if (origW <= 0 || origH <= 0) return null

            var sampleSize = 1
            val maxOrig = max(origW, origH)
            while (maxOrig / sampleSize > MAX_DIM_PX * 2) {
                sampleSize *= 2
            }

            val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            val decoded = context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOpts)
            } ?: return null

            val scale = if (max(decoded.width, decoded.height) > MAX_DIM_PX) {
                MAX_DIM_PX.toFloat() / max(decoded.width, decoded.height).toFloat()
            } else 1f

            val scaled = if (scale < 1f) {
                Bitmap.createScaledBitmap(
                    decoded,
                    (decoded.width * scale).roundToInt().coerceAtLeast(1),
                    (decoded.height * scale).roundToInt().coerceAtLeast(1),
                    true
                )
            } else decoded

            var quality = 78
            var encodedBytes: ByteArray
            do {
                val out = ByteArrayOutputStream()
                scaled.compress(Bitmap.CompressFormat.JPEG, quality, out)
                encodedBytes = out.toByteArray()
                quality -= 10
            } while (encodedBytes.size * 1.4 > MAX_BASE64_LENGTH && quality >= 35)

            if (scaled !== decoded) scaled.recycle()
            decoded.recycle()

            val b64 = Base64.encodeToString(encodedBytes, Base64.NO_WRAP)
            val dataUri = "data:image/jpeg;base64,$b64"
            if (dataUri.length > MAX_BASE64_LENGTH) null else dataUri
        } catch (_: Exception) {
            null
        }
    }

    fun decodeBase64ToImageBitmap(raw: String?): ImageBitmap? {
        if (raw.isNullOrBlank()) return null
        return try {
            val pureB64 = raw.substringAfter("base64,", raw)
            val bytes = Base64.decode(pureB64, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
        } catch (_: Exception) {
            null
        }
    }
}
