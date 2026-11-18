package com.snapdl.app

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest

enum class St { QUEUED, RUNNING, PAUSED, DONE, FAILED }

data class Video(
    val url: String,
    val title: String,
    val thumb: String,
    val uploader: String,
    val durationSec: Long,
)

data class Info(val title: String, val thumb: String, val uploader: String)

data class DownloadItem(
    val id: String,
    val url: String,
    val title: String,
    val thumb: String,
    val audio: Boolean,
    val audioFmt: String,
    val height: Int,          // 0 = terbaik
    val playlist: Boolean,
    val vault: Boolean,       // simpan di folder privat
    val status: St = St.QUEUED,
    val progress: Float = 0f,
    val eta: Long = 0,
    val uris: List<String> = emptyList(),
    val error: String = "",
) {
    fun toJson(): JSONObject = JSONObject()
        .put("id", id).put("url", url).put("title", title).put("thumb", thumb)
        .put("audio", audio).put("afmt", audioFmt).put("h", height)
        .put("pl", playlist).put("vault", vault).put("st", status.name)
        .put("uris", JSONArray(uris)).put("err", error)

    companion object {
        fun fromJson(o: JSONObject): DownloadItem {
            val a = o.getJSONArray("uris")
            return DownloadItem(
                id = o.getString("id"),
                url = o.getString("url"),
                title = o.getString("title"),
                thumb = o.optString("thumb"),
                audio = o.getBoolean("audio"),
                audioFmt = o.optString("afmt", "mp3"),
                height = o.getInt("h"),
                playlist = o.getBoolean("pl"),
                vault = o.getBoolean("vault"),
                status = St.valueOf(o.getString("st")),
                uris = (0 until a.length()).map { a.getString(it) },
                error = o.optString("err"),
            )
        }
    }
}

fun JSONObject.str(key: String): String = if (isNull(key)) "" else optString(key)

fun fmtDur(s: Long): String =
    if (s <= 0) "" else if (s >= 3600) "%d:%02d:%02d".format(s / 3600, (s % 3600) / 60, s % 60)
    else "%d:%02d".format(s / 60, s % 60)

/** Pengaturan aplikasi (disimpan di SharedPreferences, reaktif untuk Compose). */
class Settings(ctx: Context) {
    val sp = ctx.getSharedPreferences("snapdl", Context.MODE_PRIVATE)

    var maxParallel by mutableIntStateOf(sp.getInt("par", 2))
        private set
    var speedKb by mutableIntStateOf(sp.getInt("speed", 0))   // 0 = tanpa batas
        private set
    var defaultHeight by mutableIntStateOf(sp.getInt("h", 720))
        private set
    var audioFmt by mutableStateOf(sp.getString("afmt", "mp3") ?: "mp3")
        private set
    var folder by mutableStateOf(sp.getString("folder", "") ?: "")
        private set
    var adblock by mutableStateOf(sp.getBoolean("adblock", true))
        private set
    var clipDetect by mutableStateOf(sp.getBoolean("clip", true))
        private set
    var pinHash by mutableStateOf(sp.getString("pin", "") ?: "")
        private set
    var waTree by mutableStateOf(sp.getString("wa", "") ?: "")
        private set

    fun setParallel(v: Int) { maxParallel = v; sp.edit().putInt("par", v).apply() }
    fun setSpeed(v: Int) { speedKb = v; sp.edit().putInt("speed", v).apply() }
    fun setHeight(v: Int) { defaultHeight = v; sp.edit().putInt("h", v).apply() }
    fun setAudioFmt(v: String) { audioFmt = v; sp.edit().putString("afmt", v).apply() }
    fun setFolder(v: String) { folder = v; sp.edit().putString("folder", v).apply() }
    fun setAdblock(v: Boolean) { adblock = v; sp.edit().putBoolean("adblock", v).apply() }
    fun setClip(v: Boolean) { clipDetect = v; sp.edit().putBoolean("clip", v).apply() }
    fun setWaTree(v: String) { waTree = v; sp.edit().putString("wa", v).apply() }
    fun setPin(p: String) { pinHash = hash(p); sp.edit().putString("pin", pinHash).apply() }
    fun checkPin(p: String) = pinHash.isNotEmpty() && pinHash == hash(p)

    private fun hash(p: String) = MessageDigest.getInstance("SHA-256")
        .digest("snapdl:$p".toByteArray()).joinToString("") { "%02x".format(it) }
}
