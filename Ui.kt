package com.snapdl.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import coil.compose.AsyncImage

val Yellow = Color(0xFFFFD60A)
val Ink = Color(0xFF0A0A0A)

@Composable
fun SnapTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Yellow, onPrimary = Ink,
            background = Color.Transparent, surface = Color.Transparent,
            onSurface = Color.White, onBackground = Color.White,
        ),
        content = content,
    )
}

/** Latar: gradasi hitam -> kuning -> putih lembut, dengan cahaya yang di-blur. */
@Composable
fun AppBackground(content: @Composable BoxScope.() -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF050505), Color(0xFF17130A), Color(0xFF4A3B00), Color(0xFF9C8416)),
                ),
            ),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .blur(40.dp)
                .drawBehind {
                    val w = size.width
                    val h = size.height
                    drawCircle(
                        Brush.radialGradient(listOf(Yellow.copy(alpha = .60f), Color.Transparent), Offset(w * .85f, h * .10f), w * .8f),
                        radius = w * .8f, center = Offset(w * .85f, h * .10f),
                    )
                    drawCircle(
                        Brush.radialGradient(listOf(Color.White.copy(alpha = .38f), Color.Transparent), Offset(w * .05f, h * .88f), w * .75f),
                        radius = w * .75f, center = Offset(w * .05f, h * .88f),
                    )
                    drawCircle(
                        Brush.radialGradient(listOf(Yellow.copy(alpha = .30f), Color.Transparent), Offset(w * .15f, h * .45f), w * .55f),
                        radius = w * .55f, center = Offset(w * .15f, h * .45f),
                    )
                },
        )
        content()
    }
}

/** Panel kaca: transparan, tepi terang, di atas latar yang sudah blur. */
@Composable
fun Glass(
    modifier: Modifier = Modifier,
    radius: Dp = 22.dp,
    accent: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val shape = RoundedCornerShape(radius)
    val fill = if (accent) listOf(Yellow.copy(alpha = .38f), Yellow.copy(alpha = .14f))
    else listOf(Color.White.copy(alpha = .20f), Color.White.copy(alpha = .06f))
    Box(
        modifier
            .clip(shape)
            .background(Brush.linearGradient(fill))
            .border(
                BorderStroke(1.dp, Brush.linearGradient(listOf(Color.White.copy(alpha = .55f), Color.White.copy(alpha = .08f), Yellow.copy(alpha = .40f)))),
                shape,
            )
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        content = content,
    )
}

@Composable
fun Chip(text: String, selected: Boolean, onClick: () -> Unit) {
    Glass(radius = 50.dp, accent = selected, onClick = onClick) {
        Text(
            text, color = if (selected) Yellow else Color.White, fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
        )
    }
}

@Composable
fun glassFieldColors(): TextFieldColors = TextFieldDefaults.colors(
    focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent,
    focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent,
    focusedTextColor = Color.White, unfocusedTextColor = Color.White,
    cursorColor = Yellow,
)

@Composable
fun VideoCard(v: Video, onOpen: () -> Unit, onDownload: () -> Unit) {
    Glass(Modifier.fillMaxWidth(), onClick = onOpen) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = v.thumb, contentDescription = null,
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                modifier = Modifier.size(width = 120.dp, height = 68.dp).clip(RoundedCornerShape(12.dp)).background(Color.Black.copy(alpha = .3f)),
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(v.title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(
                    listOf(v.uploader, fmtDur(v.durationSec)).filter { it.isNotBlank() }.joinToString(" · "),
                    color = Color.White.copy(alpha = .65f), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
            Glass(Modifier.size(44.dp), radius = 50.dp, accent = true, onClick = onDownload) {
                Icon(Icons.Rounded.Download, "Unduh", tint = Yellow, modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}

@Composable
fun PinDialog(title: String, onDismiss: () -> Unit, onOk: (String) -> Unit) {
    var t by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1C1800),
        titleContentColor = Yellow,
        textContentColor = Color.White,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = t,
                onValueChange = { if (it.length <= 6 && it.all(Char::isDigit)) t = it },
                singleLine = true,
                label = { Text("PIN 4-6 angka") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            )
        },
        confirmButton = { TextButton(onClick = { if (t.length >= 4) onOk(t) }) { Text("OK", color = Yellow) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal", color = Color.White) } },
    )
}

private val HEIGHTS = listOf(360, 480, 720, 1080, 1440, 2160, 0)

/**
 * Pop-up unduhan. Dipakai dari menu "Bagikan" (ShareActivity), tombol unduh di daftar,
 * browser, dan banner link yang disalin.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DownloadPopup(url: String, onClose: () -> Unit, onStarted: () -> Unit) {
    val s = Engine.settings
    var info by remember { mutableStateOf<Info?>(null) }
    var audio by remember { mutableStateOf(false) }
    var height by remember { mutableIntStateOf(s.defaultHeight) }
    var fmt by remember { mutableStateOf(s.audioFmt) }
    var playlist by remember {
        mutableStateOf((url.contains("list=") && !url.contains("watch?v=")) || url.contains("/playlist"))
    }
    var vault by remember { mutableStateOf(false) }

    LaunchedEffect(url) { info = Engine.info(url) }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = .55f))
            .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }, onClick = onClose),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Glass(Modifier.fillMaxWidth().padding(12.dp).navigationBarsPadding(), radius = 28.dp, onClick = {}) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val i = info
                    if (i == null) {
                        CircularProgressIndicator(Modifier.size(28.dp), color = Yellow, strokeWidth = 3.dp)
                        Spacer(Modifier.width(12.dp))
                        Text("Mengambil info video...", color = Color.White)
                    } else {
                        if (i.thumb.isNotBlank()) {
                            AsyncImage(
                                model = i.thumb, contentDescription = null,
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                modifier = Modifier.size(width = 96.dp, height = 56.dp).clip(RoundedCornerShape(10.dp)),
                            )
                            Spacer(Modifier.width(10.dp))
                        }
                        Column {
                            Text(i.title, color = Color.White, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            if (i.uploader.isNotBlank()) Text(i.uploader, color = Color.White.copy(alpha = .65f), fontSize = 12.sp)
                        }
                    }
                }

                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Chip("Video", !audio) { audio = false }
                    Chip("Audio saja", audio) { audio = true }
                }
                if (audio) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Chip("MP3", fmt == "mp3") { fmt = "mp3" }
                        Chip("M4A", fmt == "m4a") { fmt = "m4a" }
                    }
                } else {
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        HEIGHTS.forEach { h -> Chip(if (h == 0) "Terbaik" else "${h}p", height == h) { height = h } }
                    }
                }
                ToggleRow("Unduh seluruh playlist", playlist) { playlist = it }
                ToggleRow(
                    if (s.pinHash.isEmpty()) "Simpan di folder privat (atur PIN di Profil)" else "Simpan di folder privat",
                    vault, enabled = s.pinHash.isNotEmpty(),
                ) { vault = it }

                Glass(Modifier.fillMaxWidth().height(52.dp), radius = 26.dp, accent = true, onClick = {
                    val i = info
                    Engine.enqueue(url, i?.title ?: url, i?.thumb ?: "", audio, fmt, height, playlist, vault)
                    onStarted()
                }) {
                    Row(Modifier.align(Alignment.Center), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Download, null, tint = Yellow)
                        Spacer(Modifier.width(8.dp))
                        Text("Unduh", color = Yellow, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun ToggleRow(label: String, checked: Boolean, enabled: Boolean = true, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Color.White.copy(alpha = if (enabled) 1f else .5f), modifier = Modifier.weight(1f), fontSize = 14.sp)
        Switch(
            checked = checked, onCheckedChange = onChange, enabled = enabled,
            colors = SwitchDefaults.colors(checkedThumbColor = Ink, checkedTrackColor = Yellow),
        )
    }
}


/** Logo aplikasi (sama dengan ikon): kotak kaca + panah unduh. [arrow] 0..1 untuk animasi masuk. */
@Composable
fun LogoMark(modifier: Modifier = Modifier, arrow: Float = 1f) {
    Canvas(modifier) {
        val u = size.minDimension / 180f
        drawRoundRect(
            Brush.linearGradient(listOf(Color(0xFF050505), Color(0xFF4A3B00), Yellow), Offset.Zero, Offset(size.width, size.height)),
            cornerRadius = CornerRadius(42 * u, 42 * u),
        )
        drawCircle(
            Brush.radialGradient(listOf(Yellow.copy(alpha = .5f), Color.Transparent), Offset(140 * u, 30 * u), 70 * u),
            radius = 70 * u, center = Offset(140 * u, 30 * u),
        )
        val tl = Offset(38 * u, 30 * u)
        val sz = Size(104 * u, 120 * u)
        val cr = CornerRadius(28 * u, 28 * u)
        drawRoundRect(Color.White.copy(alpha = .18f), tl, sz, cr)
        drawRoundRect(Color.White.copy(alpha = .6f), tl, sz, cr, style = Stroke(2.5f * u))
        val dy = (1f - arrow) * -40f * u
        val p = Path().apply {
            moveTo(78 * u, 56 * u + dy); lineTo(102 * u, 56 * u + dy); lineTo(102 * u, 90 * u + dy)
            lineTo(120 * u, 90 * u + dy); lineTo(90 * u, 122 * u + dy); lineTo(60 * u, 90 * u + dy)
            lineTo(78 * u, 90 * u + dy); close()
        }
        drawPath(p, Yellow.copy(alpha = arrow))
        drawRoundRect(Color.White, Offset(62 * u, 130 * u), Size(56 * u, 7 * u), CornerRadius(3.5f * u, 3.5f * u))
    }
}

/** Animasi pembuka sekitar 2,7 detik: logo membesar halus, panah turun, nama muncul, lalu memudar. */
@Composable
fun SplashOverlay(onDone: () -> Unit) {
    val scale = remember { Animatable(.55f) }
    val appear = remember { Animatable(0f) }
    val arrow = remember { Animatable(0f) }
    val label = remember { Animatable(0f) }
    val out = remember { Animatable(1f) }
    LaunchedEffect(Unit) {
        launch { appear.animateTo(1f, tween(600)) }
        launch { scale.animateTo(1f, spring(dampingRatio = .55f, stiffness = Spring.StiffnessLow)) }
        launch { delay(600); arrow.animateTo(1f, tween(900, easing = FastOutSlowInEasing)) }
        launch { delay(1000); label.animateTo(1f, tween(700)) }
        delay(2300)
        out.animateTo(0f, tween(400))
        onDone()
    }
    Box(
        Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = out.value }
            .background(Brush.verticalGradient(listOf(Color(0xFF050505), Color(0xFF17130A), Color(0xFF4A3B00), Color(0xFF9C8416))))
            .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {},
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            LogoMark(
                Modifier.size(150.dp).graphicsLayer { scaleX = scale.value; scaleY = scale.value; alpha = appear.value },
                arrow.value,
            )
            Spacer(Modifier.height(22.dp))
            Text(
                "Nextube", color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.graphicsLayer { alpha = label.value; translationY = (1f - label.value) * 24f },
            )
            Text(
                "Unduh tanpa watermark", color = Yellow, fontSize = 13.sp,
                modifier = Modifier.graphicsLayer { alpha = label.value },
            )
        }
    }
}

@Composable
fun InstagramIcon(modifier: Modifier = Modifier.size(28.dp)) {
    Canvas(modifier) {
        val s = size.minDimension
        val w = s * .09f
        val brush = Brush.linearGradient(
            listOf(Color(0xFFFEDA75), Color(0xFFFA7E1E), Color(0xFFD62976), Color(0xFF962FBF)),
            Offset(0f, s), Offset(s, 0f),
        )
        drawRoundRect(brush, Offset(w, w), Size(s - 2 * w, s - 2 * w), CornerRadius(s * .28f, s * .28f), style = Stroke(w))
        drawCircle(brush, radius = s * .21f, center = Offset(s / 2, s / 2), style = Stroke(w))
        drawCircle(brush, radius = w * .8f, center = Offset(s * .76f, s * .24f))
    }
}

@Composable
fun FacebookIcon(modifier: Modifier = Modifier.size(28.dp)) {
    Box(modifier.clip(CircleShape).background(Color(0xFF1877F2)), contentAlignment = Alignment.Center) {
        Text("f", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
    }
}
