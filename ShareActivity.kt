package com.snapdl.app

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge

/**
 * Muncul di menu "Bagikan" aplikasi lain. Saat link dibagikan ke SnapDL,
 * langsung tampil pop-up unduhan di atas aplikasi asal (tanpa membuka SnapDL penuh).
 */
class ShareActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val text = intent.getStringExtra(Intent.EXTRA_TEXT) ?: intent.dataString ?: ""
        val url = URL_RE.find(text)?.value
        if (url == null) {
            Toast.makeText(this, "Tidak ada link di teks yang dibagikan", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        setContent {
            SnapTheme {
                BackHandler { finish() }
                DownloadPopup(
                    url = url,
                    onClose = { finish() },
                    onStarted = {
                        Toast.makeText(this, "Unduhan dimulai", Toast.LENGTH_SHORT).show()
                        finish()
                    },
                )
            }
        }
    }
}
