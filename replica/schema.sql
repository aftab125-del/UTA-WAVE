-- =============================================================================
-- UTA-wave Local Room SQLite Schema
-- Target Database: utaverse_wave.db (Android Room)
-- Zero Cloud Dependencies, Zero Storage Quotas, 100% Offline Resilience
-- =============================================================================

CREATE TABLE IF NOT EXISTS tracks (
    id TEXT PRIMARY KEY NOT NULL,              -- e.g. "youtube:dQw4w9WgXcQ"
    video_id TEXT NOT NULL,                    -- e.g. "dQw4w9WgXcQ"
    title TEXT NOT NULL,
    artist TEXT NOT NULL,
    album TEXT,
    artwork_url TEXT NOT NULL,
    duration_seconds INTEGER NOT NULL DEFAULT 0,
    is_synced_lyrics INTEGER NOT NULL DEFAULT 0,
    cached_at INTEGER NOT NULL DEFAULT (strftime('%s', 'now'))
);

CREATE INDEX IF NOT EXISTS idx_tracks_video_id ON tracks (video_id);
CREATE INDEX IF NOT EXISTS idx_tracks_artist ON tracks (artist);

CREATE TABLE IF NOT EXISTS playlists (
    id TEXT PRIMARY KEY NOT NULL,              -- UUID
    name TEXT NOT NULL,
    description TEXT,
    cover_url TEXT,
    created_at INTEGER NOT NULL DEFAULT (strftime('%s', 'now')),
    updated_at INTEGER NOT NULL DEFAULT (strftime('%s', 'now'))
);

CREATE TABLE IF NOT EXISTS playlist_tracks (
    playlist_id TEXT NOT NULL,
    track_id TEXT NOT NULL,
    position INTEGER NOT NULL DEFAULT 0,
    added_at INTEGER NOT NULL DEFAULT (strftime('%s', 'now')),
    PRIMARY KEY (playlist_id, track_id),
    FOREIGN KEY (playlist_id) REFERENCES playlists (id) ON DELETE CASCADE,
    FOREIGN KEY (track_id) REFERENCES tracks (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_playlist_tracks_pos ON playlist_tracks (playlist_id, position);

CREATE TABLE IF NOT EXISTS favorites (
    track_id TEXT PRIMARY KEY NOT NULL,
    liked_at INTEGER NOT NULL DEFAULT (strftime('%s', 'now')),
    FOREIGN KEY (track_id) REFERENCES tracks (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_favorites_liked_at ON favorites (liked_at DESC);

CREATE TABLE IF NOT EXISTS history (
    track_id TEXT PRIMARY KEY NOT NULL,
    last_played_at INTEGER NOT NULL DEFAULT (strftime('%s', 'now')),
    play_count INTEGER NOT NULL DEFAULT 1,
    total_seconds_listened INTEGER NOT NULL DEFAULT 0,
    FOREIGN KEY (track_id) REFERENCES tracks (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_history_last_played ON history (last_played_at DESC);

CREATE TABLE IF NOT EXISTS settings (
    key TEXT PRIMARY KEY NOT NULL,
    value TEXT NOT NULL
);
