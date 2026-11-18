package com.snapdl.app

import android.app.Application
import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import androidx.core.content.ContextCompat
import androidx.documentfile.provider.DocumentFile
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

/**
 * Pusat semua logika unduhan: yt-dlp (+ffmpeg) dijalankan lewat library youtubedl-android.
 * Video TikTok/dll diambil dari sumber aslinya oleh yt-dlp, jadi tanpa watermark.
 */
object Engine {
    lateinit var app: Application
    lateinit var settings: Settings

    val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    val items = MutableStateFlow<List<DownloadItem>>(emptyList())

    /** 0 = sedang menyiapkan, 1 = siap, 2 = gagal */
    val state = MutableStateFlow(0)
    val initError = MutableStateFlow("")

    private val lock = Any()
    private val jobs = HashMap<String, Job>()

    fun init(a: Application) {
        app = a
        settings = Settings(a)
        loadItems()
        scope.launch {
            try {
                YoutubeDL.getInstance().init(a)
                FFmpeg.getInstance().init(a)
                state.value = 1
                pump()
                // update yt-dlp otomatis di latar belakang (situs sering berubah)
                try { YoutubeDL.getInstance().updateYoutubeDL(a) } catch (_: Exception) {}
            } catch (e: Exception) {
                initError.value = e.message ?: "init gagal"
                state.value = 2
            }
        }
    }

    suspend fun awaitReady() {
        state.first { it != 0 }
        if (state.value == 2) error("Mesin unduh gagal dimulai: ${initError.value}")
    }

    // ---------- pencarian & info ----------

    suspend fun search(query: String): List<Video> = withContext(Dispatchers.IO) {
        awaitReady()
        val r = YoutubeDLRequest("ytsearch15:$query")
        r.addOption("-J")
        r.addOption("--flat-playlist")
        r.addOption("--no-warnings")
        val o = JSONObject(YoutubeDL.getInstance().execute(r).out)
        val a = o.optJSONArray("entries") ?: return@withContext emptyList()
        (0 until a.length()).mapNotNull { i ->
            val e = a.optJSONObject(i) ?: return@mapNotNull null
            val id = e.str("id")
            val url = e.str("url").ifBlank { "https://www.youtube.com/watch?v=$id" }
            Video(
                url = url,
                title = e.str("title"),
                thumb = "https://i.ytimg.com/vi/$id/hqdefault.jpg",
                uploader = e.str("uploader").ifBlank { e.str("channel") },
                durationSec = e.optLong("duration"),
            )
        }
    }

    suspend fun info(url: String): Info = withContext(Dispatchers.IO) {
        try {
            awaitReady()
            val r = YoutubeDLRequest(url)
            r.addOption("-J")
            r.addOption("--no-playlist")
            r.addOption("--no-warnings")
            val o = JSONObject(YoutubeDL.getInstance().execute(r).out)
            Info(o.str("title").ifBlank { url }, o.str("thumbnail"), o.str("uploader"))
        } catch (e: Exception) {
            Info(url, "", "")
        }
    }

    suspend fun updateYtDlp(): String = withContext(Dispatchers.IO) {
        try {
            awaitReady()
            YoutubeDL.getInstance().updateYoutubeDL(app)?.toString() ?: "Selesai"
        } catch (e: Exception) {
            "Gagal: ${e.message}"
        }
    }

    // ---------- antrean ----------

    fun enqueue(
        url: String, title: String, thumb: String, audio: Boolean, audioFmt: String,
        height: Int, playlist: Boolean, vault: Boolean,
    ) {
        val item = DownloadItem(
            id = UUID.randomUUID().toString().take(8), url = url, title = title, thumb = thumb,
            audio = audio, audioFmt = audioFmt, height = height, playlist = playlist, vault = vault,
        )
        items.update { it + item }
        save()
        startService()
        pump()
    }

    /** Konversi video lokal (mp4 dll) ke MP3 lewat yt-dlp+ffmpeg. */
    fun convertToMp3(uri: Uri, displayName: String) {
        scope.launch {
            val dir = File(app.filesDir, "conv").apply { mkdirs() }
            val src = File(dir, UUID.randomUUID().toString().take(6) + "_" + displayName.replace('/', '_'))
            app.contentResolver.openInputStream(uri)?.use { i -> src.outputStream().use { i.copyTo(it) } }
            enqueue("file://${src.absolutePath}", displayName.substringBeforeLast('.'), "", true, "mp3", 0, false, false)
        }
    }

    private fun startService() {
        try {
            ContextCompat.startForegroundService(app, Intent(app, DownloadService::class.java))
        } catch (_: Exception) {
        }
    }

    fun pump() {
        val toStart = synchronized(lock) {
            if (state.value != 1) return@synchronized emptyList()
            val running = items.value.count { it.status == St.RUNNING }
            val free = (settings.maxParallel - running).coerceAtLeast(0)
            val q = items.value.filter { it.status == St.QUEUED }.take(free)
            q.forEach { mutate(it.id) { copy(status = St.RUNNING, error = "") } }
            q
        }
        toStart.forEach { item ->
            jobs[item.id] = scope.launch {
                runItem(item.id)
                pump()
            }
        }
    }

    fun pause(id: String) {
        mutate(id) { copy(status = St.PAUSED) }
        try { YoutubeDL.getInstance().destroyProcessById(id) } catch (_: Exception) {}
    }

    /** Lanjutkan / coba lagi. yt-dlp melanjutkan dari file .part sehingga unduhan tidak mulai dari nol. */
    fun resume(id: String) {
        mutate(id) { copy(status = St.QUEUED, error = "") }
        startService()
        pump()
    }

    fun remove(id: String) {
        val item = items.value.firstOrNull { it.id == id } ?: return
        items.update { l -> l.filter { it.id != id } }
        save()
        try { YoutubeDL.getInstance().destroyProcessById(id) } catch (_: Exception) {}
        scope.launch {
            File(app.filesDir, "tmp/$id").deleteRecursively()
            item.uris.forEach { deleteUri(it) }
        }
    }

    // ---------- eksekusi ----------

    private fun runItem(id: String) {
        val item = items.value.firstOrNull { it.id == id } ?: return
        val tmp = File(app.filesDir, "tmp/$id").apply { mkdirs() }
        try {
            val r = YoutubeDLRequest(item.url)
            r.addOption(if (item.playlist) "--yes-playlist" else "--no-playlist")
            r.addOption("--no-mtime")
            r.addOption("--no-warnings")
            if (item.url.startsWith("file://")) r.addOption("--enable-file-urls")

            if (item.audio) {
                r.addOption("-f", "bestaudio/best")
                r.addOption("-x")
                r.addOption("--audio-format", item.audioFmt)
                r.addOption("--audio-quality", "0")
            } else {
                val h = item.height
                if (h > 0) r.addOption("-f", "bv*[height<=$h]+ba/b[height<=$h]/b")
                else r.addOption("-f", "bv*+ba/b")
                r.addOption("-S", "vcodec:h264,res,acodec:m4a")
                r.addOption("--merge-output-format", "mp4")
            }
            if (settings.speedKb > 0) r.addOption("-r", "${settings.speedKb}K")
            r.addOption("-o", tmp.absolutePath + "/%(title).80s [%(id)s].%(ext)s")

            YoutubeDL.getInstance().execute(r, id) { p, eta, _ ->
                mutate(id, persist = false) { copy(progress = if (p < 0) 0f else p / 100f, eta = eta) }
            }

            val files = tmp.listFiles()?.filter {
                it.isFile && !it.name.endsWith(".part") && !it.name.endsWith(".ytdl") &&
                    !it.name.endsWith(".json") && !Regex("\\.f\\d+\\.").containsMatchIn(it.name)
            } ?: emptyList()
            if (files.isEmpty()) error("File hasil unduhan tidak ditemukan")

            val uris = files.map { if (item.vault) toVault(it) else export(it) }
            tmp.deleteRecursively()
            if (item.url.startsWith("file://")) File(item.url.removePrefix("file://")).delete()
            mutate(id) { copy(status = St.DONE, progress = 1f, uris = uris) }
        } catch (e: Exception) {
            val cur = items.value.firstOrNull { it.id == id } ?: return   // sudah dihapus
            if (cur.status != St.PAUSED) {
                mutate(id) { copy(status = St.FAILED, error = (e.message ?: "Gagal").take(240)) }
            }
        }
    }

    // ---------- penyimpanan ----------

    private fun mimeOf(name: String): String {
        val ext = name.substringAfterLast('.', "").lowercase()
        return when (ext) {
            "mp3" -> "audio/mpeg"
            "m4a" -> "audio/mp4"
            "mkv" -> "video/x-matroska"
            else -> MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "application/octet-stream"
        }
    }

    private fun toVault(f: File): String {
        val dir = File(app.filesDir, "vault").apply { mkdirs() }
        val t = File(dir, f.name)
        if (!f.renameTo(t)) { f.copyTo(t, overwrite = true); f.delete() }
        return Uri.fromFile(t).toString()
    }

    fun export(f: File): String {
        val mime = mimeOf(f.name)
        val tree = settings.folder
        if (tree.isNotEmpty()) {
            val dir = DocumentFile.fromTreeUri(app, Uri.parse(tree)) ?: error("Folder tujuan tidak valid")
            val d = dir.createFile(mime, f.nameWithoutExtension) ?: error("Gagal membuat file di folder tujuan")
            app.contentResolver.openOutputStream(d.uri)!!.use { o -> f.inputStream().use { it.copyTo(o) } }
            return d.uri.toString()
        }
        return if (Build.VERSION.SDK_INT >= 29) {
            val v = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, f.name)
                put(MediaStore.MediaColumns.MIME_TYPE, mime)
                put(MediaStore.MediaColumns.RELATIVE_PATH, "Download/Nextube")
            }
            val u = app.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, v)
                ?: error("Gagal menyimpan ke Download")
            app.contentResolver.openOutputStream(u)!!.use { o -> f.inputStream().use { it.copyTo(o) } }
            u.toString()
        } else {
            val d = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "Nextube")
                .apply { mkdirs() }
            val t = File(d, f.name)
            f.copyTo(t, overwrite = true)
            Uri.fromFile(t).toString()
        }
    }

    /** Salin status WhatsApp (content uri) ke folder unduhan. */
    fun saveStatus(uri: Uri, name: String): Boolean = try {
        val f = File(app.cacheDir, name.replace('/', '_'))
        app.contentResolver.openInputStream(uri)!!.use { i -> f.outputStream().use { i.copyTo(it) } }
        export(f)
        f.delete()
        true
    } catch (e: Exception) {
        false
    }

    private fun deleteUri(s: String) {
        val u = Uri.parse(s)
        if (u.scheme == "file") { File(u.path ?: return).delete(); return }
        try { DocumentsContract.deleteDocument(app.contentResolver, u); return } catch (_: Exception) {}
        try { app.contentResolver.delete(u, null, null) } catch (_: Exception) {}
    }

    // ---------- state ----------

    fun mutate(id: String, persist: Boolean = true, f: DownloadItem.() -> DownloadItem) {
        items.update { l -> l.map { if (it.id == id) it.f() else it } }
        if (persist) save()
    }

    private fun save() {
        val arr = JSONArray(items.value.map { it.toJson() })
        settings.sp.edit().putString("items", arr.toString()).apply()
    }

    private fun loadItems() {
        val s = settings.sp.getString("items", null) ?: return
        try {
            val a = JSONArray(s)
            items.value = (0 until a.length()).map { DownloadItem.fromJson(a.getJSONObject(it)) }
                .map { if (it.status == St.RUNNING || it.status == St.QUEUED) it.copy(status = St.PAUSED) else it }
        } catch (_: Exception) {
        }
    }
}
