# Architecture: UTA-wave (A Clean-Room Native Android Clone of Echo Music)

- **App Name:** UTA-wave
- **Target APK ID:** `com.uta.wave`
- **Repository:** `E:\UTA-wave`
- **Platform:** Native Android (Kotlin + Jetpack Compose)
- **Architecture Pattern:** Clean MVVM with Unidirectional Data Flow (UDF)

---

## 1. Stack Choices & Justifications

| Layer | Choice | Why |
|---|---|---|
| **Language** | Kotlin 2.0+ | Modern standard for Android, type-safe, null-safe, first-class Coroutines |
| **UI Framework** | Jetpack Compose (Material 3) | Declarative UI, reactive state, seamless animation, zero XML overhead |
| **Audio Engine** | AndroidX Media3 (ExoPlayer 1.4+) | Google's premier media stack; background playback, audio focus, notification media session, HTTP 206 chunking |
| **Local Database** | Android Room 2.6 (SQLite) | Embedded on-device database; zero cloud fees, zero quota limits, reactive Flow queries |
| **HTTP Client** | OkHttp 4.12 | Industry standard HTTP engine; connection reuse, GZIP/Brotli compression, custom headers for InnerTube |
| **Image Loading** | Coil 2.6 (Compose) | Lightweight, Kotlin-first, built-in memory/disk LRU caching, bitmap transformations |
| **Color Extraction** | AndroidX Palette 1.0 | Fast asynchronous dominant color extraction from artwork for real-time Ambient Mode |
| **Dependency Injection** | Manual / Lightweight DI or Hilt | Predictable, zero reflection overhead, fast cold-startup (< 400ms) |
| **Build System** | Gradle 8.7+ with Kotlin DSL | Standard Android build pipeline compiling to `app-debug.apk` with zero paid tools |

---

## 2. Package & Module Structure

```text
com.uta.wave/
├── data/
│   ├── innertube/          # InnerTube API client (Search, Trending, Player Stream)
│   ├── lyrics/             # Multi-provider lyrics fetcher (LrcLib, KuGou)
│   ├── local/              # Room Database, DAOs, and Entities
│   │   ├── AppDatabase.kt
│   │   ├── TrackDao.kt
│   │   ├── PlaylistDao.kt
│   │   └── HistoryDao.kt
│   └── repository/         # Data repositories uniting local DB and network
│       ├── MusicRepository.kt
│       ├── PlaylistRepository.kt
│       └── LyricsRepository.kt
├── player/
│   ├── WaveMediaService.kt # Foreground MediaSessionService (Media3)
│   ├── PlaybackManager.kt  # High-level playback state holder & queue controller
│   └── StreamResolver.kt   # Direct stream URL extractor with client tiering
├── ui/
│   ├── components/         # Shared Compose components (MiniPlayer, TrackRow, etc.)
│   ├── explore/            # S01: Home / Trending charts
│   ├── search/             # S02: Autocomplete search screen
│   ├── player/             # S04: Fullscreen Ambient player & S05 Lyrics sheet
│   ├── library/            # S06: Playlists & Liked songs
│   ├── playlist/           # S07: Playlist details & reordering
│   ├── settings/           # S08: Settings, OLED mode, JSON Backup/Restore
│   ├── theme/              # UTA-VERSE Dark Futuristic Color Palette & Typography
│   └── navigation/         # Navigation Compose graph & bottom bar
└── MainActivity.kt         # Single-activity container with edge-to-edge display
```

---

## 3. Data Schema (Room SQLite)

See [`replica/schema.sql`](file:///e:/UTA-wave/replica/schema.sql) for full SQL definitions.
Tables:
1. `tracks` — Cached music tracks and metadata.
2. `playlists` — User-created playlists.
3. `playlist_tracks` — Ordered junction table between playlists and tracks.
4. `favorites` — User liked songs.
5. `history` — Listening history with play counts and timestamps.
6. `settings` — Key-value user preferences (OLED theme, audio quality, ambient speed).

---

## 4. Network Endpoints (Zero-Quota, Zero-Auth)

| Endpoint | Protocol / URL | Purpose | Auth / Quota |
|---|---|---|---|
| **Search** | `POST https://music.youtube.com/youtubei/v1/search` | Search songs, albums, artists | **$0** (No API key, unmetered) |
| **Suggestions** | `POST https://music.youtube.com/youtubei/v1/music/get_search_suggestions` | Instant autocomplete | **$0** (No API key, unmetered) |
| **Charts** | `POST https://music.youtube.com/youtubei/v1/browse` | Trending & Top Charts | **$0** (No API key, unmetered) |
| **Playback Stream** | `POST https://www.youtube.com/youtubei/v1/player` | Resolve direct CDN audio stream | **$0** (No API key, unmetered) |
| **Synced Lyrics** | `GET https://lrclib.net/api/get` | Fetch synchronized LRC timestamps | **$0** (Public community API) |

---

## 5. Critical Technical Constraints ("The Parts That Bite")

1. **Android Background Execution Limits:**
   - On Android 12+ (API 31+), background services are restricted.
   - **Solution:** We use `MediaSessionService` with a valid foreground notification and `FOREGROUND_SERVICE_MEDIA_PLAYBACK` permission so playback runs indefinitely with screen off.
2. **Audio Stream URL Expiration:**
   - Google Video stream URLs expire after ~6 hours.
   - **Solution:** Do not persist stream URLs in the database. Resolve dynamically on play, and refresh seamlessly on 403.
3. **Audio Focus Handling:**
   - Pausing when headphones are disconnected or when phone calls arrive.
   - **Solution:** Built-in Media3 audio focus management (`setAudioAttributes` with `USAGE_MEDIA` and `CONTENT_TYPE_MUSIC`).
4. **Smooth 60/120fps Ambient Glow:**
   - Heavy blur filters on high-res bitmaps can cause jank.
   - **Solution:** Downsample album art to 64x64 before extracting colors and computing radial gradient canvas brushes.

---

## 6. Build Order

1. **Milestone 1 (Scaffolding & Engine):** Initialize Gradle project, Gradle wrapper, AndroidManifest, dependencies (Compose, Media3, Room, OkHttp, Coil), and package setup.
2. **Milestone 2 (Data & Stream Engine):** Implement InnerTube network client (`StreamResolver`, `InnerTubeSearch`) and Room Database layer (`AppDatabase`, `TrackDao`, `PlaylistDao`).
3. **Milestone 3 (Playback Service):** Implement `WaveMediaService` (Media3 background audio + system notification).
4. **Milestone 4 (UI & Navigation):** App Shell, Bottom Navigation, Search screen, Home/Charts screen, Library screen.
5. **Milestone 5 (Ambient Player & Synced Lyrics):** Fullscreen Ambient Mode player with dynamic gradient glow and auto-scrolling synced lyrics.
6. **Milestone 6 (Verification & Compilation):** Compile debug APK with `./gradlew assembleDebug`, verify build artifact, and test install on emulator/device.
