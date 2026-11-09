package com.capcutmini.app.media.library

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class DeviceVideo(
    val id: Long,
    val uri: Uri,
    val displayName: String,
    val durationMs: Long,
    val sizeBytes: Long
)

/**
 * Lists videos visible through MediaStore (scoped storage).
 * Requires READ_MEDIA_VIDEO (API 33+) or READ_EXTERNAL_STORAGE (older).
 */
class LocalVideoLibrary(
    private val context: Context
) {
    suspend fun loadAllVideos(): Result<List<DeviceVideo>> = withContext(Dispatchers.IO) {
        runCatching {
            val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
            } else {
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            }
            val projection = arrayOf(
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.DISPLAY_NAME,
                MediaStore.Video.Media.DURATION,
                MediaStore.Video.Media.SIZE
            )
            val sort = "${MediaStore.Video.Media.DATE_ADDED} DESC"
            val items = mutableListOf<DeviceVideo>()
            context.contentResolver.query(
                collection,
                projection,
                null,
                null,
                sort
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
                val durCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    items += DeviceVideo(
                        id = id,
                        uri = ContentUris.withAppendedId(collection, id),
                        displayName = cursor.getString(nameCol) ?: "Video $id",
                        durationMs = cursor.getLong(durCol).coerceAtLeast(0L),
                        sizeBytes = cursor.getLong(sizeCol).coerceAtLeast(0L)
                    )
                }
            }
            items
        }
    }
}
