# Nextube

Aplikasi Android pribadi untuk mengunduh video (YouTube, TikTok tanpa watermark, Instagram, dll.).
Kotlin + Jetpack Compose, mesin unduh: yt-dlp + ffmpeg (library `youtubedl-android`).

## Build lewat GitHub (tanpa Android Studio)

1. Buat repo baru di GitHub, lalu upload seluruh isi folder ini (atau `git push`).
2. Buka tab **Actions** -> workflow **Build APK** berjalan otomatis (atau klik *Run workflow*).
3. Setelah hijau, buka run-nya -> bagian **Artifacts** -> unduh `SnapDL-debug-apk` -> ekstrak -> pasang `app-debug.apk` di HP.
4. Opsional: buat tag `v1.0` agar APK dilampirkan di halaman **Releases**.

Jika build gagal, salin log error dari tab Actions dan kirim ke saya.

## Cara pakai

- **Bagikan link**: di TikTok/YouTube/IG tekan *Bagikan* -> pilih **SnapDL** -> pop-up unduhan langsung muncul.
- **Salin link**: buka SnapDL, banner "Link terdeteksi" muncul, ketuk untuk unduh.
- **Tab Cari**: cari video atau tempel link.
- **Tab Browser**: jelajah situs, tekan tombol kuning untuk mengunduh halaman yang sedang dibuka.
- **Profil**: folder penyimpanan, batas unduhan bersamaan & kecepatan, kualitas bawaan, PIN folder privat, MP4 -> MP3, perbarui yt-dlp.

## Fitur tambahan
- Animasi pembuka logo (sekitar 2,7 detik) dan transisi geser antar menu bawah.
- **Status WhatsApp**: kartu di Beranda. Beri akses folder `.Statuses` sekali, lalu simpan foto/video status yang sudah pernah dibuka di WhatsApp.
- **Profil**: Laporan bug (membuka Gmail ke rifqihisyammuddin429@gmail.com) dan tautan media sosial (Instagram rifsy_am, Facebook El Mahmud Rifqi).
- Ganti nama aplikasi: edit `app_name` di `app/src/main/res/values/strings.xml` (dan teks "Nextube" di `Ui.kt`, `Screens.kt`).

## Catatan

- Jika unduhan mendadak gagal: Profil -> **Perbarui yt-dlp**.
- Instagram/Facebook untuk konten privat atau yang meminta login bisa gagal (butuh cookies).
- Pemblokir iklan di browser menyaring domain iklan dan melewati iklan YouTube lewat skrip; tidak 100% sempurna.
- Hanya untuk pemakaian pribadi. Patuhi hak cipta dan ketentuan layanan tiap platform.
