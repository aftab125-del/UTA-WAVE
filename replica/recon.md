# Recon Map: Echo Music (Android APK) -> UTA-wave

- **Target App:** [Echo Music](https://github.com/EchoMusicApp/Echo-Music)
- **Clone Project:** **UTA-wave**
- **Platform:** Native Android (Kotlin + Jetpack Compose, minSdk 26, targetSdk 35)
- **Package Name:** `com.uta.wave`
- **Scope:** Clean-room rebuild of Echo Music's core loop, UI architecture, ambient player, synchronized lyrics, local Room database, and zero-quota InnerTube streaming pipeline.
- **For:** User's personal, ad-free, futuristic Android music player companion to UTA-VERSE.
- **Date:** 2026-10-23

---

## 1. Sources

| # | Source | URL | Notes |
|---|---|---|---|
| 1 | GitHub Repo | `https://github.com/EchoMusicApp/Echo-Music` | GPLv3 open-source architecture |
| 2 | InnerTube Specs | `https://music.youtube.com/youtubei/v1` | `WEB_REMIX` / `VISIONOS` unauthenticated client specs |
| 3 | Media3 / ExoPlayer Docs | `developer.android.com/media/media3` | AndroidX Media3 player service & session |
| 4 | Room Database Docs | `developer.android.com/training/data-storage/room` | Local SQLite ORM with Kotlin Coroutines Flow |
| 5 | LrcLib API Docs | `https://lrclib.net/docs` | Free synced LRC lyrics endpoint |

---

## 2. Core Loop

> **Search or discover any song across global music catalogs, tap to play instantly with immersive ambient visuals, view synchronized lyrics, and save playlists locally — with zero ads, zero accounts, zero cloud quotas, and zero paid tools.**

---

## 3. Screen Inventory

| ID | Screen | Route / How to Reach | Purpose | Key Components | States Seen |
|---|---|---|---|---|---|
| **S01** | **Home / Explore** | App launch / Bottom nav tab 1 | Discovery: Trending charts, quick picks, mood mixes | Top app bar, Carousel banner, Grid cards, Horizontal track rows, Bottom mini-player | Loading skeleton, filled, network error / retry, offline |
| **S02** | **Search & Autocomplete** | Bottom nav tab 2 / Search icon | Instant song/artist/album lookup with real-time suggestions | Search text field, Suggestion pill list, Filter chips (Songs, Videos, Artists, Albums), Result item cards | Empty (recent searches), typing/suggestions, results loading, filled, no results |
| **S03** | **Persistent Mini-Player** | Visible at bottom across all screens when playing | Quick playback glance & controls | Thumbnail, Title, Artist, Play/Pause toggle, Next button, Mini scrub progress line | Hidden (idle), active, loading/buffering, paused |
| **S04** | **Fullscreen Ambient Player** | Tap mini-player (swipe up) | Immersive playback with dynamic ambient lighting | Dominant color canvas blur, Artwork card/vinyl, Title/Artist, Seekbar with timestamps, Shuffle/Repeat, Like button, Lyrics toggle | Loading stream, playing, paused, scrubbing, buffering, error |
| **S05** | **Karaoke Lyrics Sheet** | Tap lyrics button on S04 (or swipe right) | Word/line synchronized lyrics with glowing current line | Syllable/line synchronized scroll list, Ambient backdrop, Romanization toggle, Provider credit | Fetching lyrics, synced lyrics active, plain/unsynced text, instrumental, no lyrics found |
| **S06** | **Library** | Bottom nav tab 3 | Local collection: Favorites, custom playlists, history | Library segmented tabs (Playlists, Songs, Artists, History), Playlist card grid, Create playlist FAB | Empty state with CTA, populated collection |
| **S07** | **Playlist Detail** | Tap any playlist from S06 | View and manage tracks within a playlist | Header artwork collaged, Title, Track count, Play All button, Shuffle button, Reorderable track list, Delete track swipe | Empty playlist, filled, reordering drag state |
| **S08** | **Settings & Library Backup**| Gear icon on top bar | Configure audio quality, ambient effects, and JSON backup | Setting preference toggles (High Quality Audio, Ambient Visuals, Pure Black OLED), Export JSON button, Import JSON picker | Default settings, export success toast, import file picker |

---

## 4. User Flows

### F01: Search & Play (Happy Path: 3 taps)
```text
S01 Home -> S02 Search (tap tab) -> Type "Starboy" -> Tap 1st result
-> S03 Mini-player triggers instantly -> Audio starts via Media3 (ExoPlayer)
Happy path: 3 taps
Edge cases: Airplane mode (show cached offline tracks), Network stall (automatic retry with alternate client profile)
```

### F02: Immersive Ambient Mode & Lyrics (Happy Path: 2 taps)
```text
S03 Mini-Player -> Tap to expand S04 Ambient Player -> Dynamic color extractor tints canvas
-> Tap Lyrics icon -> S05 Lyrics Sheet slides in with synchronized glowing text auto-scrolling
Happy path: 2 taps
Edge cases: Obscure song with no LrcLib match (cascades to KuGou / YouLyPlus fallback)
```

### F03: Create & Organize Local Playlist (Happy Path: 4 taps)
```text
S06 Library -> Tap '+' (Create Playlist) -> Enter name "Night Vibes" -> Tap Create
-> On any song (S01/S02/S04), tap 3-dots -> "Add to Playlist" -> Select "Night Vibes"
Happy path: 4 taps
Edge cases: Duplicate track added (warn or append), Reordering tracks via drag-and-drop
```

### F04: Full Library Backup & Restore (Happy Path: 3 taps)
```text
S01/S06 -> Tap Settings (S08) -> Tap "Export Library (JSON)" -> Android SAF File Picker -> Save
-> To restore: Tap "Import Library" -> Select .json file -> Room DB bulk inserts in <100ms
Happy path: 3 taps
Edge cases: Corrupt JSON file (show friendly parse error, preserve existing database)
```

---

## 5. Components

| Component | Description & Variants | States | Used On |
|---|---|---|---|
| **AppBottomBar** | 3-tab navigation (Home, Search, Library) | Selected, Unselected, Hidden in Fullscreen | S01, S02, S06 |
| **TrackItemRow** | Thumbnail, Title, Artist, Duration, 3-dots menu | Default, Playing (equalizer animation), Pressed | S01, S02, S06, S07 |
| **MiniPlayer** | Floating bottom bar with thumbnail and play/pause | Playing, Paused, Buffering | All except S04/S05 |
| **AmbientCanvas** | Dual-layer radial blur gradient computed from album art | Animated, Static (reduced motion), Dimmed | S04, S05 |
| **PlayerControls** | Shuffle, Previous, Play/Pause large FAB, Next, Repeat | Default, Active/Toggled, Disabled | S04 |
| **LyricsLineItem** | Syllable/line lyric text with font size scaling | Past, Active (bright glow + scale), Future (dimmed) | S05 |
| **FilterChip** | Material 3 tonal chip for search category | Selected, Unselected | S02 |
| **PlaylistCard** | 2x2 album cover collage or icon with title | Default, Pressed | S01, S06 |
| **CreatePlaylistDialog** | Text input dialog for new playlist creation | Idle, Typing, Submitting, Error | S06 |

---

## 6. Inferred Data Model (Room SQLite Database: `utaverse_wave.db`)

```text
TrackEntity
    id (String, PK - e.g. "youtube:dQw4w9WgXcQ")
    videoId (String, indexed)
    title (String)
    artist (String)
    album (String?)
    artworkUrl (String)
    durationSeconds (Int)
    isSyncedLyrics (Boolean)
    cachedAt (Long)
    evidence: Echo Music track database, YouTube Music playback info
    confidence: High

PlaylistEntity
    id (String, PK - UUID)
    name (String)
    description (String?)
    createdAt (Long)
    updatedAt (Long)
    evidence: Echo Music Room database schema
    confidence: High

PlaylistTrackCrossRef
    playlistId (String, FK -> PlaylistEntity.id ON DELETE CASCADE)
    trackId (String, FK -> TrackEntity.id ON DELETE CASCADE)
    position (Int)
    addedAt (Long)
    PK: (playlistId, trackId)
    evidence: Echo Music playlist track join table
    confidence: High

FavoriteTrackEntity
    trackId (String, PK, FK -> TrackEntity.id ON DELETE CASCADE)
    likedAt (Long)
    evidence: Echo Music Liked songs tab
    confidence: High

ListeningHistoryEntity
    trackId (String, PK, FK -> TrackEntity.id ON DELETE CASCADE)
    lastPlayedAt (Long)
    playCount (Int)
    totalSecondsListened (Long)
    evidence: Echo Music quick picks & stats
    confidence: High
```

---

## 7. Feature Matrix Summary

- **Must-haves:** 10 (Direct InnerTube Search, Instant CDN Playback via Media3, Background Audio Service, Lockscreen Media Controls, Mini-Player, Fullscreen Ambient Player, Synced Lyrics via LrcLib, Room DB Playlists, Favorites, Local JSON Export/Import).
- **Should-haves:** 5 (Autocomplete Suggestions, Multi-Provider Lyrics Fallback, Reorderable Playlists, Audio Quality Switcher, Pure Black AMOLED Mode).
- **Could-haves:** 3 (Sleep Timer, Playback Speed Control, Volume Normalization).
- **Out of Scope (Skip):** Paid subscriptions, Cloud server database, Google Cloud YouTube API Key usage.

---

## 8. Sizing

- **Screens:** 8 screens & sheets
- **User Flows:** 4 primary flows
- **Database Entities:** 5 Room tables
- **Hard Parts:**
  1. Background Android MediaSessionService with notification and audio focus handling.
  2. InnerTube stream URL extraction with client fallback tiering (`VISIONOS` / `ANDROID` / `WEB_REMIX`).
  3. Dynamic real-time ambient blur shader from album art in Jetpack Compose.
- **Project Sizing:** **Medium (M)** — Straightforward clean-room Android build using modern Jetpack Compose + Media3.
