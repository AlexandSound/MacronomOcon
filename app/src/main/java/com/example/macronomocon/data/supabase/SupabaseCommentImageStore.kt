package com.example.macronomocon.data.supabase

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import com.example.macronomocon.data.repository.CommentImageStore
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.storage.storage
import io.ktor.http.ContentType
import java.io.ByteArrayOutputStream
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Comment photos in the public `comment-images` bucket, under the uploader's `user_id/` folder
 * (enforced by storage policies). Photos are downscaled and re-encoded as JPEG on the device,
 * which keeps uploads small and drops location and other metadata.
 */
class SupabaseCommentImageStore(
    private val client: SupabaseClient,
    private val resolver: ContentResolver
) : CommentImageStore {
    companion object {
        const val BUCKET = "comment-images"
        const val MAX_EDGE_PX = 1600
        const val JPEG_QUALITY = 85

        /** Matches the bucket's file_size_limit. */
        const val MAX_BYTES = 5 * 1024 * 1024
    }

    override suspend fun upload(localUri: String): String {
        val userId = requireNotNull(client.auth.currentUserOrNull()).id
        val bytes = jpeg(Uri.parse(localUri))
        require(bytes.size <= MAX_BYTES) { "Photo is too large" }
        val path = "$userId/${UUID.randomUUID()}.jpg"
        val bucket = client.storage.from(BUCKET)
        bucket.upload(path, bytes) { contentType = ContentType.Image.JPEG }
        return bucket.publicUrl(path)
    }

    private suspend fun jpeg(uri: Uri): ByteArray = withContext(Dispatchers.IO) {
        val bitmap = decode(uri)
        try {
            ByteArrayOutputStream().use {
                check(bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, it))
                it.toByteArray()
            }
        } finally {
            bitmap.recycle()
        }
    }

    private fun decode(uri: Uri): Bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        // ImageDecoder applies EXIF rotation, so camera photos come out upright.
        ImageDecoder.decodeBitmap(ImageDecoder.createSource(resolver, uri)) { decoder, info, _ ->
            val longest = maxOf(info.size.width, info.size.height)
            if (longest > MAX_EDGE_PX) {
                val scale = MAX_EDGE_PX.toDouble() / longest
                decoder.setTargetSize(
                    (info.size.width * scale).toInt().coerceAtLeast(1),
                    (info.size.height * scale).toInt().coerceAtLeast(1)
                )
            }
            // Hardware bitmaps cannot be compressed.
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        }
    } else {
        // Android 7–8: power-of-two downsampling; EXIF rotation is not applied.
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri).use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= MAX_EDGE_PX) sample *= 2
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        requireNotNull(
            resolver.openInputStream(uri).use { BitmapFactory.decodeStream(it, null, options) }
        ) { "Unreadable image" }
    }
}
