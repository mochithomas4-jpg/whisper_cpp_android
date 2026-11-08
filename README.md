# Whisper SRT - Android On-Device Real-Time Transcription

Aplikasi Android native berperforma tinggi untuk transkripsi audio ke teks dan pembuatan subtitle format **SubRip (.srt)** secara real-time langsung di perangkat (*100% on-device & offline*), ditenagai oleh mesin [whisper.cpp](https://github.com/ggerganov/whisper.cpp) dan basis implementasi dari [Aatricks/llmedge-examples](https://github.com/Aatricks/llmedge-examples).

---

## 🌟 Fitur Unggulan

### 1. Transkripsi Berkas Audio ke Subtitle (.SRT)
- **Multi-Format**: Mendukung format audio populer seperti `MP3`, `WAV`, `M4A`, `AAC`, `OGG`, `FLAC`, dan `OPUS`.
- **Native Audio Decoder**: Menggunakan `MediaExtractor` dan `MediaCodec` bawaan Android untuk mendecode berkas audio secara asinkron dan me-resample ke format 16.000 Hz 1-channel (mono) float32 yang dibutuhkan Whisper.
- **Visual Waveform & Pemutar Audio**: Dilengkapi pemutar audio terintegrasi dengan gelombang suara statis yang dapat di-scrub/seek ke timestamp tertentu.

### 2. Transkripsi Suara Real-time (Mikrofon)
- Rekam pembicaraan langsung menggunakan mikrofon perangkat.
- Visualisasi gelombang suara (*live waveform*) yang berdenyut sesuai amplitude dan volume suara input.
- Timer perekaman presisi hingga milidetik.
- Pratinjau hasil rekaman dan tombol sekali sentuh untuk mengubah rekaman menjadi subtitle `.srt`.

### 3. Editor Subtitle & Ekspor Beragam
- **3 Mode Tampilan**:
  - **Kartu Segmen**: Daftar kartu interaktif berisi nomor urut segmen, badge rentang waktu (`00:00:01,200 ➔ 00:00:04,500`), teks subtitle, serta tombol edit dan hapus.
  - **Format .SRT**: Teks mentah format SRT standar industri yang siap disalin.
  - **Teks Polos**: Teks transkripsi bersih tanpa penanda waktu.
- **Pencarian Cepat**: Filter pencarian instan untuk menemukan kata tertentu dalam subtitle.
- **Editor Segmen**: Ketuk segmen mana saja untuk mengoreksi teks atau memodifikasi waktu mulai dan selesai.
- **Simpan & Bagikan**:
  - Simpan berkas `.srt` ke penyimpanan internal/folder Downloads menggunakan Android Storage Access Framework (SAF).
  - Bagikan subtitle langsung ke aplikasi lain (WhatsApp, Telegram, Email, Google Drive, dsb.) via Android Share Sheet.

### 4. Manajemen 30 Pilihan Model Whisper
Aplikasi menyediakan 30 pilihan model Whisper resmi dari Hugging Face yang dapat diunduh langsung in-app:
- **Tiny Series**: `tiny`, `tiny.en`, `tiny-q5_1`, `tiny.en-q5_1`, `tiny-q8_0` (Ukuran: 31 MB – 75 MB)
- **Base Series**: `base`, `base.en`, `base-q5_1`, `base.en-q5_1`, `base-q8_0` (Ukuran: 57 MB – 142 MB)
- **Small Series**: `small`, `small.en`, `small.en-tdrz` (Tinydiarize), `small-q5_1`, `small.en-q5_1`, `small-q8_0` (Ukuran: 182 MB – 466 MB)
- **Medium Series**: `medium`, `medium.en`, `medium-q5_0`, `medium.en-q5_0`, `medium-q8_0` (Ukuran: 515 MB – 1.5 GB)
- **Large Series**: `large-v1`, `large-v2`, `large-v2-q5_0`, `large-v2-q8_0`, `large-v3`, `large-v3-q5_0`, `large-v3-turbo`, `large-v3-turbo-q5_0`, `large-v3-turbo-q8_0` (Ukuran: 547 MB – 2.9 GB)
- **Dukungan Model Kustom**: Tombol impor berkas model `.bin` atau `.gguf` lokal dari memori smartphone.
- Pengunduhan dilengkapi persentase progres, indikator kecepatan unduh (*KB/s*), dan opsi hapus untuk menghemat ruang memori.

### 5. Pengaturan Akselerasi Hardware & Inferensi
- **Pilihan Backend Komputasi**:
  - **CPU (Multi-Core ARM)**: Eksekusi universal dan stabil di semua chipset.
  - **GPU (OpenCL / Vulkan)**: Akselerasi kartu grafis hardware untuk waktu inferensi lebih singkat.
  - **NPU / Neural Engine**: Unit pemrosesan neural tensor akselerator.
- **Jumlah Thread Prosesor**: Slider untuk mengatur thread CPU (1 hingga 16 threads, dengan preset hemat daya, optimal, atau maksimal).
- **Flash Attention**: Optimasi self-attention untuk kecepatan transkripsi lebih tinggi dan efisiensi memori RAM.
- **Token Timestamps**: Perhitungan penanda waktu level kata untuk akurasi subtitle yang presisi.
- **Beam Search & Temperature**: Pilihan kualitas penelusuran (Greedy vs Beam Size 1–5).

---

## ⚡ Optimalisasi Ukuran Aplikasi

Sesuai instruksi untuk meminimalkan ukuran APK dan hanya menyisakan Whisper:
- Seluruh pustaka native berat yang tidak diperlukan telah **dibuang**:
  - Stable Diffusion (`libsdcpp.so`, ~82 MB)
  - SmolLM / LLM Text (`libsmollm*.so`, ~82 MB)
  - Bark TTS (`libbark_jni.so`, ~1 MB)
  - Vision & Wan Video assets
- Pustaka native Whisper yang dipertahankan hanya **~4.5 MB** (`libwhisper_jni.so`, `libomp.so`, `libggufreader.so`).
- Dependensi eksternal yang tidak relevan (Room compiler, Firebase AI, Retrofit, Moshi) dinonaktifkan sehingga ukuran file APK sangat ramping dan waktu kompilasi cepat.

---

## 📁 Struktur Direktori

```
WhisperSRT/
├── app/
│   ├── build.gradle.kts           # Konfigurasi modul aplikasi, NDK ABI filters, dan packaging
│   ├── libs/
│   │   └── llmedge-whisper.aar    # Library AAR native Whisper hasil optimasi (4.5 MB)
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── java/com/example/
│       │   │   ├── MainActivity.kt               # Entrypoint & M3 Scaffold 5-tab navigation
│       │   │   ├── audio/
│       │   │   │   ├── AudioDecoder.kt           # Decoder MediaCodec/Extractor ke PCM 16kHz
│       │   │   │   ├── AudioRecorder.kt          # Perekam mikrofon 16kHz & amplitude tracker
│       │   │   │   └── AudioPlayer.kt            # Pemutar audio pratinjau Uri & PCM Float
│       │   │   ├── engine/
│       │   │   │   └── WhisperManager.kt         # JNI bridge, loader model, dan downloader
│       │   │   ├── model/
│       │   │   │   ├── SubtitleSegment.kt        # Model data segmen SRT, parser & generator
│       │   │   │   └── WhisperModelInfo.kt       # Metadata 30 model dan WhisperSettings
│       │   │   ├── ui/
│       │   │   │   ├── components/
│       │   │   │   │   ├── WaveformVisualizer.kt # Visualisasi gelombang audio
│       │   │   │   │   └── SegmentEditorDialog.kt# Dialog penyunting teks & waktu segmen
│       │   │   │   ├── screens/
│       │   │   │   │   ├── AudioFileScreen.kt    # Layar transkripsi berkas audio
│       │   │   │   │   ├── LiveMicScreen.kt      # Layar rekam mikrofon langsung
│       │   │   │   │   ├── SrtResultScreen.kt    # Layar hasil subtitle & ekspor
│       │   │   │   │   ├── ModelsScreen.kt       # Layar katalog 30 model Whisper
│       │   │   │   │   └── SettingsScreen.kt     # Layar pengaturan CPU/GPU/NPU & threads
│       │   │   │   └── theme/                    # Material Design 3 central theme
│       │   │   └── viewmodel/
│       │   │       └── WhisperViewModel.kt       # State management terpusat
│       │   └── res/
│       │       ├── drawable/                     # Ikon adaptive launcher kustom
│       │       └── values/strings.xml
│       └── test/java/com/example/
│           ├── ExampleUnitTest.kt
│           └── ExampleRobolectricTest.kt         # Pengujian logika SRT & context
├── gradle/
│   └── wrapper/
│       ├── gradle-wrapper.jar
│       └── gradle-wrapper.properties
├── gradlew                        # Script Gradle Wrapper (Linux / macOS)
├── gradlew.bat                    # Script Gradle Wrapper (Windows)
├── build.gradle.kts
├── settings.gradle.kts
└── README.md
```

---

## 🛠️ Persyaratan Sistem

- **Sistem Operasi Android**: Android 11.0+ (API Level 30 atau lebih tinggi).
- **Arsitektur CPU**: `arm64-v8a` (perangkat Android fisik 64-bit) atau `x86_64` (emulator).
- **Java Development Kit (JDK)**: JDK 17 atau JDK 21.
- **Gradle**: Versi 8.11+ / 9.x.

---

## 🚀 Cara Build & Menjalankan

### 1. Menggunakan Gradle Wrapper (Terminal)

#### Di Linux / macOS:
```bash
# Berikan izin eksekusi jika diperlukan
chmod +x gradlew

# Membangun APK versi Debug
./gradlew assembleDebug

# Menjalankan pengujian unit
./gradlew testDebugUnitTest
```

#### Di Windows (Command Prompt / PowerShell):
```cmd
# Membangun APK versi Debug
gradlew.bat assembleDebug

# Menjalankan pengujian unit
gradlew.bat testDebugUnitTest
```

Setelah proses kompilasi selesai, berkas APK berada di:
```
app/build/outputs/apk/debug/app-debug.apk
```

### 2. Menggunakan Android Studio
1. Buka Android Studio (Meerkat, Ladybug, atau Koala).
2. Pilih **Open an Existing Project** dan arahkan ke direktori proyek ini.
3. Tunggu proses Gradle Sync selesai.
4. Pilih target perangkat (*device* fisik atau emulator dengan arsitektur `arm64-v8a` / `x86_64`).
5. Klik tombol **Run** (ikon segitiga hijau) atau tekan `Shift + F10`.

---


## 📄 Lisensi & Atribusi

Proyek ini dibangun menggunakan:
- [whisper.cpp](https://github.com/ggerganov/whisper.cpp) oleh Georgi Gerganov (Lisensi MIT).
- [llmedge](https://github.com/Aatricks/llmedge) & [llmedge-examples](https://github.com/Aatricks/llmedge-examples) oleh Aatricks (Lisensi Apache 2.0).
- Jetpack Compose & Material 3 oleh Google Android Open Source Project (AOSP).
