package com.falah.cascade

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.provider.OpenableColumns

data class Media(
    val id: Long,
    val uri: Uri,
    val title: String,
    val durationMs: Long,
    val sizeBytes: Long,
    val isAudio: Boolean,
    val dateAdded: Long,
    val folderPath: String = "",
)

data class Folder(val path: String, val name: String, val items: List<Media>)

/** Hands the playlist from the home screen to the player screen. */
object Queue {
    var items: List<Media> = emptyList()
    var index: Int = 0
}

object Library {
    fun load(ctx: Context): List<Media> {
        val out = ArrayList<Media>()
        query(ctx, MediaStore.Video.Media.EXTERNAL_CONTENT_URI, false, out)
        query(ctx, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, true, out)
        return out
    }

    private fun query(ctx: Context, base: Uri, audio: Boolean, out: MutableList<Media>) {
        val proj = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            "duration",
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.DATE_ADDED,
            "_data",
        )
        val sel = if (audio) "is_music != 0 OR duration > 60000" else null
        try {
            ctx.contentResolver.query(base, proj, sel, null, null)?.use { c ->
                while (c.moveToNext()) {
                    val id = c.getLong(0)
                    out += Media(
                        id = id,
                        uri = ContentUris.withAppendedId(base, id),
                        title = c.getString(1).orEmpty().substringBeforeLast('.'),
                        durationMs = c.getLong(2),
                        sizeBytes = c.getLong(3),
                        isAudio = audio,
                        dateAdded = c.getLong(4),
                        folderPath = c.getString(5).orEmpty().substringBeforeLast('/', ""),
                    )
                }
            }
        } catch (_: Exception) {
        }
    }
}

/** Theme, seek step and per-video watch progress. */
class Prefs(ctx: Context) {
    private val sp = ctx.getSharedPreferences("cascade", Context.MODE_PRIVATE)

    var theme: Int // 0 system, 1 light, 2 dark
        get() = sp.getInt("theme", 0)
        set(v) = sp.edit().putInt("theme", v).apply()

    var seekSec: Int
        get() = sp.getInt("seek", 10)
        set(v) = sp.edit().putInt("seek", v).apply()

    fun save(id: String, pos: Long, dur: Long) {
        sp.edit().putLong("p:$id", pos).putLong("d:$id", dur).putLong("t:$id", System.currentTimeMillis()).apply()
    }

    /** The most recently played item that still exists in the library. */
    fun last(all: List<Media>): Media? {
        val byUri = all.associateBy { it.uri.toString() }
        return sp.all.keys
            .filter { it.startsWith("t:") && byUri.containsKey(it.substring(2)) }
            .maxByOrNull { sp.getLong(it, 0L) }
            ?.let { byUri[it.substring(2)] }
    }

    fun pos(id: String): Long = sp.getLong("p:$id", 0L)
    fun dur(id: String): Long = sp.getLong("d:$id", 0L)

    /** Recently watched, unfinished videos with progress 0..1. */
    fun recent(all: List<Media>): List<Pair<Media, Float>> {
        val byUri = all.associateBy { it.uri.toString() }
        return sp.all.keys
            .filter { it.startsWith("t:") }
            .sortedByDescending { sp.getLong(it, 0L) }
            .mapNotNull { k ->
                val id = k.substring(2)
                val m = byUri[id] ?: return@mapNotNull null
                if (m.isAudio) return@mapNotNull null
                val p = pos(id)
                val d = if (m.durationMs > 0) m.durationMs else dur(id)
                if (p < 5000 || d <= 0 || p > d * 0.97) null else m to (p.toFloat() / d)
            }
            .take(10)
    }
}

fun displayName(ctx: Context, uri: Uri): String {
    var n: String? = null
    if (uri.scheme == "content") {
        try {
            ctx.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
                if (it.moveToFirst()) n = it.getString(0)
            }
        } catch (_: Exception) {
        }
    }
    val raw = n ?: uri.lastPathSegment ?: "Video"
    return raw.substringAfterLast('/').substringBeforeLast('.')
}

fun isAudioExt(s: String): Boolean {
    val p = s.substringBefore('?').lowercase()
    return listOf("mp3", "m4a", "aac", "flac", "wav", "ogg", "opus", "wma", "amr", "mka").any { p.endsWith(".$it") }
}

fun fmt(ms: Long): String {
    val s = (ms.coerceAtLeast(0L)) / 1000
    val h = s / 3600
    val m = (s % 3600) / 60
    val sec = s % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, sec) else "%02d:%02d".format(m, sec)
}

fun humanSize(b: Long): String = when {
    b >= (1L shl 30) -> "%.1f GB".format(b / (1L shl 30).toDouble())
    b >= (1L shl 20) -> "${b shr 20} MB"
    else -> "${b shr 10} KB"
}
