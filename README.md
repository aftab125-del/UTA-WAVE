<div align="center">

# 🌊 UTA-WAVE

### *A Universe of Music — Cinematic, Ad-Free, High-Fidelity Music Streaming & Offline Player for Android*

[![Platform](https://img.shields.io/badge/Platform-Android%208.0%2B%20(API%2026%2B)-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://android.com)
[![Language](https://img.shields.io/badge/Language-Kotlin%202.0-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![UI Toolkit](https://img.shields.io/badge/UI-Jetpack%20Compose%20%26%20Material%203-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Audio Engine](https://img.shields.io/badge/Audio-AndroidX%20Media3%20ExoPlayer-FF6F00?style=for-the-badge&logo=google&logoColor=white)](https://developer.android.com/guide/topics/media/media3)
[![Shaders](https://img.shields.io/badge/Optics-AGSL%20Liquid%20Glass-EC4899?style=for-the-badge)](https://developer.android.com/develop/ui/views/graphics/agsl)
[![License](https://img.shields.io/badge/License-GPL%20v3-blue.style=for-the-badge)](LICENSE)

<br/>

<p align="center">
  <b>UTA-WAVE</b> is a next-generation music client engineered from the ground up for audiophiles, aesthetic purists, and music enthusiasts. Combining hardware-accelerated AGSL glass refraction, Apple Music-inspired cinematic playback sheets, offline-first library resilience, built-in acoustic song recognition, and direct AI curation powered by Google Gemini 2.0 Flash.
</p>

---

</div>

## ✨ Key Feature Highlights

### 🍎 1. Apple Music-Inspired Cinematic Player
- **Fluid Multi-Mesh Background**: Real-time atmospheric color mesh dynamically extracted from album cover palettes with smooth animated blur transitions.
- **Apple Control Deck**: Signature Apple-style fluid playback controls, progressive volume scrubbing, and output device switcher.
- **Turntable Vinyl Disc**: Optional rotating vinyl disc animation synchronized with playback state.
- **Squiggly & Custom Seek Bars**: Dynamic animated squiggly waveforms, continuous sleek lines, and haptic feedback scrubbing.
- **Gesture-Driven UX**: Swipe left/right across artwork or miniplayer to jump tracks; swipe down smoothly to collapse.

---

### 🔮 2. Liquid Glass & Physical Optics Engine
- **Hardware-Accelerated AGSL Shaders**: Physics-based ray refraction that dynamically bends and distorts live UI content behind floating navigation pills and docked players.
- **Chromatic Aberration**: 7-band rainbow spectral fringing along capsule contours for authentic glass realism.
- **Lens Geometry Controls**: Customizable curvature bevel height, signed-distance-field (SDF) displacement amount, and depth-of-field diffusion blur (10dp to 40dp).
- **Permanent Across UI**: Fully integrated into the **Floating MiniPlayer**, **Floating Navigation Bar**, and **Fullscreen Player Backdrops**.
- **MiniPlayer Optics Customization**:
  - *Background Styles*: Liquid Glass Refraction, Breathing Neon Mesh, Frosted Acrylic Blur, and Solid Elevated Glass.
  - *Progress Track Styles*: Live Waveform (Squiggly), Sleek Linear, Glowing Neon Track, and Hidden (Edge-to-Edge).
  - *Glow Radiance*: Configurable glow intensity slider (0% to 100%).
- **Floating Navigation Bar Customization**:
  - *Pill Shape*: Full-width stadium floating bar or compact dynamic capsule.
  - *Auto-Hide on Scroll*: Seamlessly glides bottom navigation away when scrolling through feeds.
  - *Translucency & Frosted Blur*: Fine-tuned glass opacity and diffusive depth sliders.

---

### 🎧 3. Audiophile Playback Engine
- **AndroidX Media3 ExoPlayer Architecture**: Rock-solid foreground playback service (`WaveAudioService`) with full wake-lock and audio focus handling.
- **Native Media Notification & Lockscreen Player**:
  - High-res dynamic album artwork with color-matched background.
  - Interactive scrubbable progress bar.
  - Full playback controls: Previous, Play/Pause, Next, Seek, and custom actions (Shuffle, Repeat, Radio, Like).
- **High-Resolution & Bit-Perfect USB DAC Mode**: Pure direct bit-perfect audio stream pass-through for external DACs and audiophile hardware.
- **Seamless Playback**: True gapless audio playback with customizable crossfade (0s - 10s) and skip silence detection.
- **Audio Normalization**: Integrated loudness normalization and volume leveling preventing sudden volume spikes.
- **10-Band Graphic Equalizer**: Custom parametric equalizer with Bass Boost and 3D Virtualizer audio effect processors.
- **Smart Preloading & Cache**: Intelligent preloading of upcoming queue tracks for zero-latency, instant song changes.

---

### 🧠 4. AI Hub & Intelligent Music Studio
- **Google Gemini 2.0 Flash Integration**: Official free API key support delivering ultra-fast (< 1s) responses and structured JSON outputs.
- **BYO Key Provider Selector**: Seamless switching between **Google Gemini 2.0 Flash (Recommended)**, **OpenRouter (DeepSeek R1, GPT-4o, Claude 3.5)**, and a **Free Neural Cloud** fallback (no API key needed).
- **In-App Latency & Health Benchmarking**: "Test Connection" button with real-time ping diagnostics.
- **AI Playlist Studio Sandbox**:
  - Natural language prompt-to-playlist generator (e.g. *"Late night cyberpunk synth drive with female vocals"*).
  - Preset quick-mood chips (*"Cyberpunk Drive"*, *"Rainy Day Acoustic"*, *"Tokyo City Pop"*, *"Intense Gym"*, *"Lo-Fi Focus"*).
  - Configurable song count slider (5 to 25 tracks).
  - 1-tap save: Directly resolves tracks and saves curated playlists into your Library!
- **AI Lyrics Intelligence**: Real-time bilingual translation for foreign songs and phonetic romanization / transliteration (Japanese Romaji, Korean Revised Romanization, Hindi Latin script).

---

### 🔍 5. Acoustic Song Recognition (Shazam DejaVu Engine)
- **Built-in Music Recognition**: Identify any song playing in the ambient room in seconds without third-party apps.
- **Native 16.0 kHz Acoustic Recording**: Captures audio directly at 16,000 Hz, eliminating software downsampling phase distortion and preserving exact FFT bin frequencies.
- **Voice Recognition Audio Source**: Prioritizes `VOICE_RECOGNITION` and `UNPROCESSED` sources over standard `MIC`, bypassing telephone high-pass bandpass filters (< 300Hz) and preserving musical bass and overtones.
- **Pure-Kotlin DejaVu Implementation**: Reverse-engineered acoustic fingerprinting algorithm with 2048-point Hanning window FFTs, peak spreading, sub-bin quadratic interpolation, 4-band frequency chunking, and CRC32 packaging.
- **Instant InnerTube Resolution**: Resolves identified songs directly into high-res streamable tracks with official audio matching and 1-tap playback.

---

### 📊 6. Listening Summary & Stats Center
- **Today's Minutes Played**: Prominent hero card tracking exact minutes listened today with trending pulse indicator.
- **Multi-Slot Time Selector**:
  - 📅 **Last 7 Days**
  - 📅 **Last 15 Days**
  - 📅 **Last 30 Days**
  - 🏆 **All Time** (Total time listened since installing the APK)
- **Visual Analytics**:
  - 7-bar daily listening distribution chart with peak-day highlight.
  - **Most Played Song Spotlight Card** with album artwork, title, artist, play count, and 1-tap play.
  - **Top Artist & Top Album** side-by-side cards.
  - **Top 5 Songs Leaderboard** with numbered rankings and replay counters.
  - **Top Artists Breakdown** with proportional percentage progress bars.
  - **Sonic Vibe & Top Styles** breakdown.
- **Anti-Spam Playback Tracking**: Logs real elapsed playback time in 15-second intervals, preventing duplicate counts on rapid pause/resume.

---

### 📴 7. Complete Offline Mode & Download Manager
- **Zero-Network Library Resilience**: App opens instantly in offline / airplane mode without internet hangs, immediately presenting downloaded library tracks and playlists.
- **Multi-Quality Downloads**: Choose between Lossless Opus, High (256 kbps), and Standard (128 kbps).
- **Wi-Fi Protection**: "Download over Wi-Fi only" toggle to protect mobile data.
- **Auto-Download on Like**: Automatically download liked songs for offline preservation.
- **Local Audio Player**: Scans and plays offline files directly from local device storage.

---

### 🎙 8. Synchronized Karaoke Lyrics
- **Multi-Source Provider Hierarchy**: Real-time synced lyrics fetched from **LRCLIB**, **YouTube / Musixmatch**, and local LRC files.
- **Line-by-Line Synchronized Scrolling**: Auto-scrolling lyrics with click-to-seek timestamp navigation.
- **Word-by-Word Animation**: Fluid kinetic typography animations with dynamic blur effects.
- **Apple Music-Style Blurred Backdrops**: Smooth Gaussian background blur behind active lyric lines.

---

### 📻 9. Continuous AutoMix & Artist Radio
- **Endless Radio Generation**: Seed infinite, dynamic streaming radio from any track or artist.
- **AutoMix Queue Drawer**: Queue drawer with toggle button to expand or collapse auto-suggested similar tracks.
- **Smart Queue Persistence**: Queue restores seamlessly across app restarts and app clears.

---

### 🌐 10. Listen Together (Synchronized Social Listening)
- **Real-Time Room Synchronization**: Listen in sync with friends via lightweight WebSocket architecture.
- **Dual Room Modes**:
  - *Host Sync*: Host controls song selection, seeking, and playback state.
  - *Collaborative*: Anyone in the room can add songs, reorder queue, and control playback.
- **Auto Re-Sync**: Millisecond-accurate timestamp resynchronization on track drifts or latency spikes.

---

### 🎨 11. Customization & Theming
- **Theme Modes**: Obsidian Dark, True AMOLED Pitch Black (OLED battery saver), Clean Light, and Follow System.
- **Dynamic Color Palettes**: Material You dynamic palette extraction from current track artwork or device wallpaper.
- **Accent Colors**: Blush Pink (`#F2AEC7`), Electric Violet (`#A855F7`), Cyber Sky Blue (`#38BDF8`), Emerald Green (`#10B981`), Amber Gold (`#F59E0B`), and Crimson.
- **Custom Font Selector**: Switch between *Google Sans*, *Outfit*, *Plus Jakarta Sans*, *Sans Flex*, or load custom TTF/OTF fonts.
- **Custom App Icons & Shapes**: Built-in icon presets (Echo Pink, Cyber Gold, Neon Blue, Crimson, Sakura) and customizable launcher shapes (Squircle, Circle, Rounded).

---

### 🔄 12. Playlist Importer & Backup
- **Spotify & YouTube Music Importer**: Paste any public playlist URL to automatically resolve and import songs into your local library.
- **JSON Backup & Restore**: One-tap export and import of your entire music library, favorites, playlists, and listening history.

---

## 🛠 Tech Stack & Architecture

| Layer | Technologies |
|---|---|
| **Language** | Kotlin 2.0 (100% strict type-safe Kotlin) |
| **UI Toolkit** | Jetpack Compose, Material 3, Accompanist, Foundation |
| **Graphics & Shaders** | AGSL (Android Graphics Shading Language), RuntimeShader, RenderEffect |
| **Audio Pipeline** | AndroidX Media3 1.5 (ExoPlayer), AudioAttributes, SonicAudioProcessor |
| **Networking** | OkHttp 4.12, Kotlinx Coroutines, WebSockets |
| **Data Engine** | YouTube Music InnerTube Client, LRCLIB API, Shazam Cloud Discovery |
| **Database** | SQLite, Room-inspired DAO abstractions, Flow-based reactive queries |
| **State Management** | StateFlow, SharedFlow, Compose State, DataStore Preferences |
| **Architecture** | Unidirectional Data Flow (UDF), Clean Architecture, Domain Boundaries |

---

## 🚀 Building from Source

### Prerequisites
1. Android Studio Ladybug / Meerkat (or newer)
2. JDK 17 or JDK 21
3. Android SDK 35 (compileSdk 35, minSdk 26, targetSdk 35)

### Build Instructions

```bash
# 1. Clone the repository
git clone https://github.com/aftab125-del/UTA-WAVE.git
cd UTA-WAVE

# 2. Build Debug APK
./gradlew assembleDebug

# 3. Locate compiled APK
# The output APK will be generated at:
# app/build/outputs/apk/debug/app-debug.apk
```

To install directly to a connected Android device or emulator:
```bash
./gradlew installDebug
```

---

## 🔒 Privacy & Ad-Free Guarantee

- 🚫 **Zero Advertisements**: Pure, uninterrupted listening experience.
- 🚫 **No Tracking / Telemetry**: No third-party ad networks, trackers, or commercial telemetry SDKs.
- 💾 **Local-First Storage**: Your listening history, playlists, and statistics stay encrypted in your local SQLite database on your device.
- 🔑 **Bring Your Own Keys (Optional)**: AI features use your own private Gemini or OpenRouter API keys stored securely in local app preferences.

---

<div align="center">

Crafted with ❤️ for music lovers everywhere.

**[UTA-WAVE GitHub Repository](https://github.com/aftab125-del/UTA-WAVE)**

</div>
