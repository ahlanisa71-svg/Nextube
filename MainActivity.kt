package com.snapdl.app

import android.Manifest
import android.content.ClipboardManager
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.core.content.ContextCompat

val URL_RE = Regex("https?://\\S+")

class MainActivity : ComponentActivity() {
    private val clip = mutableStateOf<String?>(null)
    private var lastClip: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }
        setContent { SnapTheme { SnapApp(clip) } }
    }

    // Android hanya mengizinkan baca clipboard saat aplikasi sedang fokus
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) readClip()
    }

    private fun readClip() {
        if (!Engine.settings.clipDetect) return
        val cm = getSystemService(ClipboardManager::class.java) ?: return
        val c = cm.primaryClip ?: return
        if (c.itemCount == 0) return
        val t = c.getItemAt(0).coerceToText(this)?.toString() ?: return
        val u = URL_RE.find(t)?.value ?: return
        if (u != lastClip) { lastClip = u; clip.value = u }
    }
}

@Composable
fun SnapApp(clip: MutableState<String?>) {
    val ctx = LocalContext.current
    var tab by remember { mutableIntStateOf(0) }
    var browserUrl by remember { mutableStateOf("https://m.youtube.com") }
    var popup by remember { mutableStateOf<String?>(null) }
    var playing by remember { mutableStateOf<String?>(null) }
    var vault by remember { mutableStateOf(false) }
    var statusOpen by remember { mutableStateOf(false) }
    var splash by rememberSaveable { mutableStateOf(true) }

    val web = remember { makeWebView(ctx) { browserUrl = it } }
    LaunchedEffect(Unit) { web.loadUrl("https://m.youtube.com") }
    DisposableEffect(Unit) { onDispose { web.destroy() } }

    fun openInBrowser(url: String) { web.loadUrl(url); tab = 2 }

    AppBackground {
        Box(Modifier.fillMaxSize().statusBarsPadding()) {
            // transisi geser + pudar antar menu bawah
            AnimatedContent(
                targetState = tab,
                modifier = Modifier.fillMaxSize(),
                label = "tab",
                transitionSpec = {
                    val dir = if (targetState > initialState) 1 else -1
                    (fadeIn(tween(320)) + slideInHorizontally(tween(320)) { it / 5 * dir }) togetherWith
                        (fadeOut(tween(220)) + slideOutHorizontally(tween(320)) { -it / 5 * dir })
                },
            ) { t ->
                when (t) {
                    0 -> HomeScreen(::openInBrowser, ::openInBrowser, { statusOpen = true }) { popup = it }
                    1 -> SearchScreen(::openInBrowser) { popup = it }
                    2 -> BrowserScreen(web, browserUrl, { popup = it }, { tab = 0 })
                    3 -> DownloadsScreen(vaultOnly = false) { playing = it }
                    else -> ProfileScreen { vault = true }
                }
            }
        }

        BottomNav(tab, { tab = it }, Modifier.align(Alignment.BottomCenter))

        // banner link yang disalin
        val c = clip.value
        if (c != null && popup == null) {
            Glass(
                Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(16.dp).fillMaxWidth(),
                radius = 22.dp, accent = true, onClick = { popup = c; clip.value = null },
            ) {
                Row(Modifier.padding(start = 16.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Download, null, tint = Yellow)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Link terdeteksi - ketuk untuk unduh", color = Yellow, fontSize = 13.sp)
                        Text(c, color = Color.White.copy(alpha = .8f), fontSize = 11.sp, maxLines = 1)
                    }
                    IconButton({ clip.value = null }) { Icon(Icons.Rounded.Close, "Tutup", tint = Color.White) }
                }
            }
        }

        if (popup != null) {
            val u = popup!!
            DownloadPopup(u, onClose = { popup = null }, onStarted = { popup = null; tab = 3 })
            BackHandler { popup = null }
        }
        if (vault) VaultScreen(onClose = { vault = false }) { playing = it }
        if (statusOpen) StatusScreen(onClose = { statusOpen = false }) { playing = it }
        playing?.let { PlayerScreen(it) { playing = null } }
        if (splash) SplashOverlay { splash = false }
    }
}

@Composable
private fun BottomNav(tab: Int, onTab: (Int) -> Unit, modifier: Modifier) {
    val items = listOf(
        Icons.Rounded.Home to "Beranda", Icons.Rounded.Search to "Cari", Icons.Rounded.Public to "Browser",
        Icons.Rounded.Download to "Unduhan", Icons.Rounded.Person to "Profil",
    )
    Glass(modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp).navigationBarsPadding(), radius = 28.dp) {
        Row(Modifier.fillMaxWidth().padding(6.dp), horizontalArrangement = Arrangement.SpaceAround) {
            items.forEachIndexed { i, (icon, label) ->
                val sel = i == tab
                val bg by animateColorAsState(if (sel) Yellow.copy(alpha = .18f) else Color.Transparent, tween(250), label = "nav")
                Column(
                    Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .background(bg)
                        .clickable { onTab(i) }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(icon, label, tint = if (sel) Yellow else Color.White.copy(alpha = .75f))
                    Text(label, fontSize = 10.sp, color = if (sel) Yellow else Color.White.copy(alpha = .75f))
                }
            }
        }
    }
}
