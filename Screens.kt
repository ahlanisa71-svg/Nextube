package com.snapdl.app

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.documentfile.provider.DocumentFile
import coil.ImageLoader
import coil.decode.VideoFrameDecoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import kotlin.math.roundToInt

// =====================================================================
// 1) BERANDA / TRENDING + PINTASAN SITUS
// =====================================================================

private val SITES = listOf(
    "YouTube" to "https://m.youtube.com",
    "TikTok" to "https://www.tiktok.com",
    "Instagram" to "https://www.instagram.com",
    "Facebook" to "https://m.facebook.com",
    "X" to "https://x.com",
    "Reddit" to "https://www.reddit.com",
    "Pinterest" to "https://www.pinterest.com",
    "Vimeo" to "https://vimeo.com",
)

private val TRENDS = listOf(
    "Trending" to "trending",
    "Musik" to "music hits",
    "Gaming" to "gaming",
    "Berita" to "berita hari ini",
    "Film" to "movie trailer",
)

@Composable
fun HomeScreen(onSite: (String) -> Unit, onOpen: (String) -> Unit, onStatus: () -> Unit, onDownload: (String) -> Unit) {
    var chip by remember { mutableIntStateOf(0) }
    var videos by remember { mutableStateOf<List<Video>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var err by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(chip) {
        loading = true; err = null
        try { videos = Engine.search(TRENDS[chip].second) } catch (e: Exception) { err = e.message; videos = emptyList() }
        loading = false
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 120.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Column {
                Text("Nextube", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text("Unduh video tanpa watermark", color = Yellow)
            }
        }
        item {
            Column {
                Text("Pintasan situs", color = Color.White.copy(alpha = .8f), fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 6.dp))
                SITES.chunked(4).forEach { row ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { (n, u) -> SiteTile(n, Modifier.weight(1f)) { onSite(u) } }
                    }
                }
            }
        }
        item {
            Glass(Modifier.fillMaxWidth(), radius = 22.dp, accent = true, onClick = onStatus) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(40.dp).clip(CircleShape).background(Yellow), Alignment.Center) {
                        Text("W", color = Ink, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Status WhatsApp", color = Color.White, fontWeight = FontWeight.SemiBold)
                        Text("Simpan foto & video status yang sudah Anda lihat", color = Color.White.copy(alpha = .7f), fontSize = 12.sp)
                    }
                    Icon(Icons.Rounded.Download, null, tint = Yellow)
                }
            }
        }
        item {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TRENDS.forEachIndexed { i, (label, _) -> Chip(label, chip == i) { chip = i } }
            }
        }
        if (loading) item { Box(Modifier.fillMaxWidth().padding(24.dp), Alignment.Center) { CircularProgressIndicator(color = Yellow) } }
        err?.let { e -> item { Text("Gagal memuat: $e", color = Color(0xFFFFB4A8), fontSize = 13.sp) } }
        items(videos) { v -> VideoCard(v, onOpen = { onOpen(v.url) }, onDownload = { onDownload(v.url) }) }
    }
}

@Composable
private fun SiteTile(name: String, modifier: Modifier, onClick: () -> Unit) {
    Glass(modifier, radius = 18.dp, onClick = onClick) {
        Column(Modifier.fillMaxWidth().padding(vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(38.dp).clip(CircleShape).background(Yellow), Alignment.Center) {
                Text(name.first().toString(), color = Ink, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
            Spacer(Modifier.height(6.dp))
            Text(name, color = Color.White, fontSize = 11.sp, maxLines = 1)
        }
    }
}

// =====================================================================
// 2) PENCARIAN (atau tempel link langsung)
// =====================================================================

@Composable
fun SearchScreen(onOpen: (String) -> Unit, onDownload: (String) -> Unit) {
    var q by remember { mutableStateOf("") }
    var videos by remember { mutableStateOf<List<Video>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var err by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val focus = LocalFocusManager.current

    fun go() {
        val text = q.trim()
        if (text.isEmpty()) return
        focus.clearFocus()
        if (text.startsWith("http")) { onDownload(text); return }
        scope.launch {
            loading = true; err = null
            try { videos = Engine.search(text) } catch (e: Exception) { err = e.message }
            loading = false
        }
    }

    Column(Modifier.fillMaxSize().imePadding()) {
        Glass(Modifier.fillMaxWidth().padding(16.dp), radius = 50.dp) {
            TextField(
                value = q, onValueChange = { q = it }, singleLine = true,
                placeholder = { Text("Cari video atau tempel link...", color = Color.White.copy(alpha = .5f)) },
                colors = glassFieldColors(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { go() }),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp, 0.dp, 16.dp, 120.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (loading) item { Box(Modifier.fillMaxWidth().padding(24.dp), Alignment.Center) { CircularProgressIndicator(color = Yellow) } }
            err?.let { e -> item { Text("Gagal: $e", color = Color(0xFFFFB4A8), fontSize = 13.sp) } }
            items(videos) { v -> VideoCard(v, onOpen = { onOpen(v.url) }, onDownload = { onDownload(v.url) }) }
        }
    }
}

// =====================================================================
// 3) BROWSER BAWAAN + PEMBLOKIR IKLAN
// =====================================================================

private val AD_PATTERNS = listOf(
    "doubleclick.net", "googlesyndication.com", "googleadservices.com", "adservice.google",
    "googletagservices.com", "google-analytics.com", "googletagmanager.com", "/pagead/", "/api/stats/ads",
    "/ptracking", "adnxs.com", "taboola.com", "outbrain.com", "popads.net", "propellerads", "exoclick.com",
    "adsterra", "mgid.com", "criteo.", "pubmatic.com", "rubiconproject.com", "amazon-adsystem.com",
    "moatads.com", "scorecardresearch.com", "ads.tiktok.com", "an.facebook.com", "/tr?id=", "adform.net",
    "smartadserver", "yandex.ru/ads", "hotjar.com",
)

private const val AD_JS = """
(function(){
  if (window.__snapdl) return; window.__snapdl = 1;
  var st = document.createElement('style');
  st.textContent = '.ytp-ad-module,.ytp-ad-overlay-container,ytm-promoted-sparkles-web-renderer,ytm-companion-ad-renderer,ad-slot-renderer,.ad-container,[id^="google_ads"],ins.adsbygoogle{display:none!important}';
  document.head.appendChild(st);
  setInterval(function(){
    var b = document.querySelector('.ytp-ad-skip-button,.ytp-skip-ad-button,.ytp-ad-skip-button-modern,.ytp-ad-skip-button-container button');
    if (b) b.click();
    var p = document.querySelector('.html5-video-player.ad-showing, #movie_player.ad-showing');
    var v = document.querySelector('video');
    if (p && v && isFinite(v.duration) && v.duration > 0) { v.muted = true; v.currentTime = v.duration; }
  }, 400);
})();
"""

@SuppressLint("SetJavaScriptEnabled")
fun makeWebView(ctx: Context, onUrl: (String) -> Unit): WebView = WebView(ctx).apply {
    settings.javaScriptEnabled = true
    settings.domStorageEnabled = true
    settings.mediaPlaybackRequiresUserGesture = false
    settings.loadWithOverviewMode = true
    setBackgroundColor(android.graphics.Color.BLACK)
    webViewClient = object : WebViewClient() {
        override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
            if (Engine.settings.adblock) {
                val u = request.url.toString()
                if (AD_PATTERNS.any { u.contains(it) }) {
                    return WebResourceResponse("text/plain", "utf-8", ByteArrayInputStream(ByteArray(0)))
                }
            }
            return null
        }

        override fun onPageStarted(view: WebView, url: String, favicon: android.graphics.Bitmap?) { onUrl(url) }
        override fun doUpdateVisitedHistory(view: WebView, url: String, isReload: Boolean) { onUrl(url) }
        override fun onPageFinished(view: WebView, url: String) {
            if (Engine.settings.adblock) view.evaluateJavascript(AD_JS, null)
        }

        // jangan biarkan situs membuka aplikasi lain (intent://, market://, dll)
        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
            val s = request.url.scheme
            return s != "http" && s != "https"
        }
    }
}

@Composable
fun BrowserScreen(web: WebView, currentUrl: String, onDownload: (String) -> Unit, onExit: () -> Unit) {
    var text by remember { mutableStateOf(currentUrl) }
    val focus = LocalFocusManager.current
    LaunchedEffect(currentUrl) { text = currentUrl }

    BackHandler { if (web.canGoBack()) web.goBack() else onExit() }

    fun go() {
        val t = text.trim()
        if (t.isEmpty()) return
        focus.clearFocus()
        val target = when {
            t.startsWith("http://") || t.startsWith("https://") -> t
            t.contains(".") && !t.contains(" ") -> "https://$t"
            else -> "https://www.google.com/search?q=" + Uri.encode(t)
        }
        web.loadUrl(target)
    }

    Column(Modifier.fillMaxSize()) {
        Glass(Modifier.fillMaxWidth().padding(12.dp, 8.dp), radius = 50.dp) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton({ if (web.canGoBack()) web.goBack() }) { Icon(Icons.Rounded.ArrowBack, "Kembali", tint = Color.White) }
                IconButton({ if (web.canGoForward()) web.goForward() }) { Icon(Icons.Rounded.ArrowForward, "Maju", tint = Color.White) }
                TextField(
                    value = text, onValueChange = { text = it }, singleLine = true,
                    colors = glassFieldColors(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                    keyboardActions = KeyboardActions(onGo = { go() }),
                    modifier = Modifier.weight(1f),
                )
                IconButton({ web.reload() }) { Icon(Icons.Rounded.Refresh, "Muat ulang", tint = Color.White) }
            }
        }
        Box(Modifier.fillMaxSize()) {
            AndroidView(
                factory = { (web.parent as? android.view.ViewGroup)?.removeView(web); web },
                modifier = Modifier.fillMaxSize(),
            )
            Glass(
                Modifier.align(Alignment.BottomEnd).padding(end = 18.dp, bottom = 100.dp).size(60.dp),
                radius = 30.dp, accent = true,
                onClick = { onDownload(web.url ?: currentUrl) },
            ) {
                Icon(Icons.Rounded.Download, "Unduh halaman ini", tint = Yellow, modifier = Modifier.align(Alignment.Center).size(28.dp))
            }
        }
    }
}

// =====================================================================
// 4) DAFTAR UNDUHAN (antrean, jeda, lanjutkan)
// =====================================================================

@Composable
fun DownloadsScreen(vaultOnly: Boolean, onPlay: (String) -> Unit) {
    val all by Engine.items.collectAsState()
    val list = all.filter { it.vault == vaultOnly }.reversed()

    if (list.isEmpty()) {
        Box(Modifier.fillMaxSize(), Alignment.Center) {
            Text(if (vaultOnly) "Folder privat kosong" else "Belum ada unduhan", color = Color.White.copy(alpha = .7f))
        }
        return
    }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 120.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(list, key = { it.id }) { d ->
            Glass(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (d.thumb.isNotBlank() && !vaultOnly) {
                            AsyncImage(
                                model = d.thumb, contentDescription = null,
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                modifier = Modifier.size(width = 80.dp, height = 48.dp).clip(RoundedCornerShape(10.dp)),
                            )
                            Spacer(Modifier.width(10.dp))
                        }
                        Column(Modifier.weight(1f)) {
                            Text(d.title, color = Color.White, fontWeight = FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis, fontSize = 14.sp)
                            Text(statusText(d), color = if (d.status == St.FAILED) Color(0xFFFFB4A8) else Yellow, fontSize = 12.sp, maxLines = 2)
                        }
                    }
                    if (d.status == St.RUNNING || d.status == St.PAUSED || d.status == St.QUEUED) {
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { d.progress }, modifier = Modifier.fillMaxWidth(),
                            color = Yellow, trackColor = Color.White.copy(alpha = .2f),
                        )
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        when (d.status) {
                            St.DONE -> IconButton({ d.uris.firstOrNull()?.let(onPlay) }) { Icon(Icons.Rounded.PlayArrow, "Putar", tint = Yellow) }
                            St.RUNNING -> IconButton({ Engine.pause(d.id) }) { Icon(Icons.Rounded.Pause, "Jeda", tint = Color.White) }
                            St.PAUSED, St.FAILED -> IconButton({ Engine.resume(d.id) }) { Icon(Icons.Rounded.Refresh, "Lanjutkan", tint = Yellow) }
                            St.QUEUED -> {}
                        }
                        IconButton({ Engine.remove(d.id) }) { Icon(Icons.Rounded.Delete, "Hapus", tint = Color.White.copy(alpha = .8f)) }
                    }
                }
            }
        }
    }
}

private fun statusText(d: DownloadItem) = when (d.status) {
    St.QUEUED -> "Dalam antrean"
    St.RUNNING -> "${(d.progress * 100).toInt()}%" + if (d.eta > 0) " · sisa ${d.eta}s" else ""
    St.PAUSED -> "Dijeda (${(d.progress * 100).toInt()}%) - tekan lanjutkan"
    St.DONE -> "Selesai" + if (d.vault) " · folder privat" else ""
    St.FAILED -> "Gagal: ${d.error}"
}

// =====================================================================
// 5) PROFIL / PENGATURAN
// =====================================================================

@Composable
fun ProfileScreen(onOpenVault: () -> Unit) {
    val ctx = LocalContext.current
    val s = Engine.settings
    val scope = rememberCoroutineScope()
    var pinDialog by remember { mutableIntStateOf(0) }   // 1 = atur PIN, 2 = masukkan PIN

    val folderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            ctx.contentResolver.takePersistableUriPermission(
                uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
            )
            s.setFolder(uri.toString())
        }
    }
    val videoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            var name = "video.mp4"
            ctx.contentResolver.query(uri, null, null, null, null)?.use { c ->
                val i = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (i >= 0 && c.moveToFirst()) name = c.getString(i)
            }
            Engine.convertToMp3(uri, name)
            Toast.makeText(ctx, "Konversi ke MP3 dimulai, lihat di tab Unduhan", Toast.LENGTH_LONG).show()
        }
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp, 8.dp, 16.dp, 120.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Profil & Pengaturan", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Color.White)

        SettingCard("Unduhan bersamaan: ${s.maxParallel}") {
            Slider(
                value = s.maxParallel.toFloat(), onValueChange = { s.setParallel(it.roundToInt()); Engine.pump() },
                valueRange = 1f..5f, steps = 3,
            )
        }
        SettingCard("Batas kecepatan") {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(0 to "Tanpa batas", 512 to "512 KB/s", 1024 to "1 MB/s", 2048 to "2 MB/s", 5120 to "5 MB/s")
                    .forEach { (v, l) -> Chip(l, s.speedKb == v) { s.setSpeed(v) } }
            }
            Text("Berlaku untuk unduhan berikutnya.", color = Color.White.copy(alpha = .55f), fontSize = 11.sp)
        }
        SettingCard("Kualitas bawaan") {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(360, 480, 720, 1080, 1440, 2160, 0).forEach { h ->
                    Chip(if (h == 0) "Terbaik" else "${h}p", s.defaultHeight == h) { s.setHeight(h) }
                }
            }
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip("Audio MP3", s.audioFmt == "mp3") { s.setAudioFmt("mp3") }
                Chip("Audio M4A", s.audioFmt == "m4a") { s.setAudioFmt("m4a") }
            }
        }
        SettingCard("Folder penyimpanan") {
            Text(
                if (s.folder.isEmpty()) "Bawaan: Download/SnapDL" else Uri.decode(s.folder).substringAfterLast(':'),
                color = Color.White.copy(alpha = .8f), fontSize = 13.sp,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip("Pilih folder", false) { folderPicker.launch(null) }
                if (s.folder.isNotEmpty()) Chip("Kembali ke bawaan", false) { s.setFolder("") }
            }
        }
        SettingCard("Browser") {
            ToggleRow("Pemblokir iklan", s.adblock) { s.setAdblock(it) }
            ToggleRow("Deteksi otomatis link yang disalin", s.clipDetect) { s.setClip(it) }
        }
        SettingCard("Folder privat (PIN)") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip("Buka folder privat", true) { pinDialog = if (s.pinHash.isEmpty()) 1 else 2 }
                if (s.pinHash.isNotEmpty()) Chip("Ganti PIN", false) { pinDialog = 1 }
            }
        }
        SettingCard("Alat") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip("MP4 → MP3", false) { videoPicker.launch("video/*") }
                Chip("Perbarui yt-dlp", false) {
                    scope.launch {
                        Toast.makeText(ctx, "Memperbarui...", Toast.LENGTH_SHORT).show()
                        Toast.makeText(ctx, Engine.updateYtDlp(), Toast.LENGTH_LONG).show()
                    }
                }
            }
            Text(
                "Jika unduhan tiba-tiba gagal, perbarui yt-dlp. Situs sering mengubah sistemnya.",
                color = Color.White.copy(alpha = .55f), fontSize = 11.sp,
            )
        }
        SettingCard("Laporan bug") {
            Chip("Laporkan bug lewat Gmail", false) { sendBugReport(ctx) }
            Text("Membuka Gmail dengan alamat tujuan sudah terisi: $BUG_EMAIL", color = Color.White.copy(alpha = .55f), fontSize = 11.sp)
        }
        SettingCard("Media sosial") {
            SocialRow({ InstagramIcon() }, "rifsy_am", "https://www.instagram.com/rifsy_am")
            SocialRow({ FacebookIcon() }, "El Mahmud Rifqi", "https://www.facebook.com/profile.php?id=61555140984359")
        }
        Text("Nextube 1.0", color = Color.White.copy(alpha = .45f), fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
    }

    when (pinDialog) {
        1 -> PinDialog("Atur PIN baru", { pinDialog = 0 }) { s.setPin(it); pinDialog = 0; onOpenVault() }
        2 -> PinDialog("Masukkan PIN", { pinDialog = 0 }) {
            if (s.checkPin(it)) { pinDialog = 0; onOpenVault() }
            else Toast.makeText(ctx, "PIN salah", Toast.LENGTH_SHORT).show()
        }
    }
}

@Composable
private fun SettingCard(title: String, content: @Composable () -> Unit) {
    Glass(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, color = Color.White, fontWeight = FontWeight.SemiBold)
            content()
        }
    }
}

// =====================================================================
// FOLDER PRIBADI & PEMUTAR
// =====================================================================

@Composable
fun VaultScreen(onClose: () -> Unit, onPlay: (String) -> Unit) {
    BackHandler(onBack = onClose)
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .78f)).statusBarsPadding()) {
        Column {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Lock, null, tint = Yellow)
                Spacer(Modifier.width(8.dp))
                Text("Folder Privat", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.weight(1f))
                IconButton(onClose) { Icon(Icons.Rounded.Close, "Tutup", tint = Color.White) }
            }
            DownloadsScreen(vaultOnly = true, onPlay = onPlay)
        }
    }
}

@Composable
fun PlayerScreen(uri: String, onClose: () -> Unit) {
    val ctx = LocalContext.current
    val player = remember {
        ExoPlayer.Builder(ctx).build().apply {
            setMediaItem(MediaItem.fromUri(Uri.parse(uri)))
            prepare()
            playWhenReady = true
        }
    }
    DisposableEffect(Unit) { onDispose { player.release() } }
    BackHandler(onBack = onClose)
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            factory = { c -> PlayerView(c).also { v -> v.player = player } },
            modifier = Modifier.fillMaxSize(),
        )
        IconButton(onClose, Modifier.statusBarsPadding().padding(8.dp)) { Icon(Icons.Rounded.Close, "Tutup", tint = Color.White) }
    }
}

// =====================================================================
// LAPORAN BUG & MEDIA SOSIAL
// =====================================================================

const val BUG_EMAIL = "rifqihisyammuddin429@gmail.com"

fun sendBugReport(ctx: Context) {
    val body = "Jelaskan bug yang Anda temui:\n\n\n----\nAplikasi: Nextube 1.0\n" +
        "HP: ${Build.MANUFACTURER} ${Build.MODEL}\nAndroid: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"
    val i = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:")).apply {
        putExtra(Intent.EXTRA_EMAIL, arrayOf(BUG_EMAIL))
        putExtra(Intent.EXTRA_SUBJECT, "Laporan bug Nextube")
        putExtra(Intent.EXTRA_TEXT, body)
    }
    try {
        ctx.startActivity(Intent(i).setPackage("com.google.android.gm"))   // langsung ke Gmail
    } catch (_: Exception) {
        try { ctx.startActivity(Intent.createChooser(i, "Kirim laporan bug")) }
        catch (_: Exception) { Toast.makeText(ctx, "Tidak ada aplikasi email", Toast.LENGTH_LONG).show() }
    }
}

@Composable
private fun SocialRow(icon: @Composable () -> Unit, label: String, url: String) {
    val ctx = LocalContext.current
    Glass(
        Modifier.fillMaxWidth(), radius = 16.dp,
        onClick = { try { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) } catch (_: Exception) {} },
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            icon()
            Spacer(Modifier.width(12.dp))
            Text(label, color = Color.White, fontWeight = FontWeight.Medium)
        }
    }
}

// =====================================================================
// STATUS WHATSAPP
// =====================================================================

private data class StatusFile(val uri: Uri, val name: String, val video: Boolean, val time: Long)

private const val WA_START =
    "content://com.android.externalstorage.documents/document/primary%3AAndroid%2Fmedia%2Fcom.whatsapp%2FWhatsApp%2FMedia%2F.Statuses"
private const val WA_BIZ_START =
    "content://com.android.externalstorage.documents/document/primary%3AAndroid%2Fmedia%2Fcom.whatsapp.w4b%2FWhatsApp%20Business%2FMedia%2F.Statuses"

private fun resolveStatusDir(root: DocumentFile): DocumentFile? {
    if (root.name == ".Statuses") return root
    val paths = listOf(
        listOf(".Statuses"),
        listOf("Media", ".Statuses"),
        listOf("WhatsApp", "Media", ".Statuses"),
        listOf("com.whatsapp", "WhatsApp", "Media", ".Statuses"),
        listOf("WhatsApp Business", "Media", ".Statuses"),
        listOf("com.whatsapp.w4b", "WhatsApp Business", "Media", ".Statuses"),
    )
    for (p in paths) {
        var d: DocumentFile? = root
        for (n in p) d = d?.findFile(n)
        if (d != null && d.isDirectory) return d
    }
    return null
}

@Composable
fun StatusScreen(onClose: () -> Unit, onPlay: (String) -> Unit) {
    val ctx = LocalContext.current
    val s = Engine.settings
    val scope = rememberCoroutineScope()
    var files by remember { mutableStateOf<List<StatusFile>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var msg by remember { mutableStateOf<String?>(null) }
    var refresh by remember { mutableIntStateOf(0) }
    val loader = remember { ImageLoader.Builder(ctx).components { add(VideoFrameDecoder.Factory()) }.build() }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            ctx.contentResolver.takePersistableUriPermission(
                uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
            )
            s.setWaTree(uri.toString())
            refresh++
        }
    }
    BackHandler(onBack = onClose)

    LaunchedEffect(s.waTree, refresh) {
        if (s.waTree.isEmpty()) return@LaunchedEffect
        loading = true; msg = null
        val res = withContext(Dispatchers.IO) {
            try {
                val root = DocumentFile.fromTreeUri(ctx, Uri.parse(s.waTree))
                val dir = root?.let { resolveStatusDir(it) }
                dir?.listFiles()
                    ?.filter { val t = it.type ?: ""; t.startsWith("image/") || t.startsWith("video/") }
                    ?.map { StatusFile(it.uri, it.name ?: "status", (it.type ?: "").startsWith("video/"), it.lastModified()) }
                    ?.sortedByDescending { it.time }
            } catch (e: Exception) {
                null
            }
        }
        if (res == null) {
            files = emptyList()
            msg = "Folder status tidak ditemukan. Pilih folder .Statuses (atau Android/media/com.whatsapp)."
        } else {
            files = res
            if (res.isEmpty()) msg = "Belum ada status. Buka dulu status di WhatsApp, lalu tekan segarkan."
        }
        loading = false
    }

    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .8f)).statusBarsPadding()) {
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Status WhatsApp", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.weight(1f))
                IconButton({ refresh++ }) { Icon(Icons.Rounded.Refresh, "Segarkan", tint = Yellow) }
                IconButton(onClose) { Icon(Icons.Rounded.Close, "Tutup", tint = Color.White) }
            }
            if (s.waTree.isEmpty() || msg != null) {
                Glass(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            msg ?: "Beri akses ke folder status WhatsApp sekali saja. Status hanya muncul di sini jika sudah pernah Anda buka di WhatsApp.",
                            color = Color.White, fontSize = 13.sp,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Chip("WhatsApp", true) { picker.launch(Uri.parse(WA_START)) }
                            Chip("WA Business", false) { picker.launch(Uri.parse(WA_BIZ_START)) }
                        }
                        Text(
                            "Di layar berikutnya tekan Gunakan folder ini lalu Izinkan.",
                            color = Color.White.copy(alpha = .55f), fontSize = 11.sp,
                        )
                    }
                }
            }
            if (loading) Box(Modifier.fillMaxWidth().padding(24.dp), Alignment.Center) { CircularProgressIndicator(color = Yellow) }
            LazyVerticalGrid(
                GridCells.Fixed(2),
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 24.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                gridItems(files, key = { it.uri.toString() }) { f ->
                    Glass(Modifier.fillMaxWidth(), radius = 18.dp) {
                        Column {
                            Box(
                                Modifier.fillMaxWidth().aspectRatio(.8f).clickable { if (f.video) onPlay(f.uri.toString()) },
                            ) {
                                AsyncImage(
                                    model = f.uri, imageLoader = loader, contentDescription = null,
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize(),
                                )
                                if (f.video) {
                                    Icon(Icons.Rounded.PlayArrow, "Putar", tint = Color.White, modifier = Modifier.align(Alignment.Center).size(44.dp))
                                }
                            }
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                IconButton({
                                    scope.launch {
                                        val ok = withContext(Dispatchers.IO) { Engine.saveStatus(f.uri, f.name) }
                                        Toast.makeText(
                                            ctx, if (ok) "Tersimpan di folder unduhan" else "Gagal menyimpan", Toast.LENGTH_SHORT,
                                        ).show()
                                    }
                                }) { Icon(Icons.Rounded.Download, "Simpan", tint = Yellow) }
                            }
                        }
                    }
                }
            }
        }
    }
}
