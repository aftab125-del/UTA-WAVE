-- ==============================================================================
-- UTA-WAVE Cloud Sync & Cross-Device Account Schema (Supabase PostgreSQL)
-- Target Platforms: Mobile (Android/iOS) & Desktop (Windows 10/11)
-- Enables: Realtime Playlists, Liked Songs, History, Stats & UTA Connect
-- ==============================================================================

-- 1. User Profiles & Cloud Preferences
CREATE TABLE IF NOT EXISTS public.user_profiles (
    id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    display_name TEXT NOT NULL DEFAULT 'UTA Wave User',
    avatar_url TEXT,
    email TEXT,
    preferences JSONB DEFAULT '{
        "audio_quality": "HIGH",
        "crossfade_seconds": 0,
        "loudness_normalization": true,
        "theme": "DARK",
        "liquid_glass_enabled": true
    }'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 2. Cloud Playlists (Bi-directional sync with soft delete / tombstones)
CREATE TABLE IF NOT EXISTS public.cloud_playlists (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    title TEXT NOT NULL,
    description TEXT DEFAULT '',
    artwork_url TEXT,
    is_favorite BOOLEAN DEFAULT FALSE,
    sync_version BIGINT NOT NULL DEFAULT 1,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 3. Cloud Playlist Tracks
CREATE TABLE IF NOT EXISTS public.cloud_playlist_tracks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    playlist_id UUID NOT NULL REFERENCES public.cloud_playlists(id) ON DELETE CASCADE,
    video_id TEXT NOT NULL,
    title TEXT NOT NULL,
    artist TEXT NOT NULL,
    artwork_url TEXT,
    duration INT NOT NULL DEFAULT 0,
    position INT NOT NULL DEFAULT 0,
    added_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT unique_playlist_track_pos UNIQUE (playlist_id, position) DEFERRABLE INITIALLY DEFERRED
);

-- 4. User Liked Songs (Favorites)
CREATE TABLE IF NOT EXISTS public.user_favorites (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    video_id TEXT NOT NULL,
    title TEXT NOT NULL,
    artist TEXT NOT NULL,
    artwork_url TEXT,
    duration INT NOT NULL DEFAULT 0,
    liked_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT user_favorite_track_unique UNIQUE (user_id, video_id)
);

-- 5. Listening History & Cross-Device Stats
CREATE TABLE IF NOT EXISTS public.listening_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    video_id TEXT NOT NULL,
    title TEXT NOT NULL,
    artist TEXT NOT NULL,
    artwork_url TEXT,
    duration INT NOT NULL DEFAULT 0,
    seconds_played INT NOT NULL DEFAULT 30,
    device_type TEXT NOT NULL, -- 'mobile_android' | 'desktop_windows' | 'mobile_ios'
    played_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 6. UTA Connect: Real-Time Active Playback Sessions (Spotify Connect style)
CREATE TABLE IF NOT EXISTS public.active_playback_sessions (
    device_id TEXT PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    device_name TEXT NOT NULL, -- e.g. "Windows Gaming PC", "Pixel 9 Pro"
    device_type TEXT NOT NULL, -- 'windows' | 'android' | 'ios'
    is_active BOOLEAN NOT NULL DEFAULT FALSE,
    is_playing BOOLEAN NOT NULL DEFAULT FALSE,
    current_track JSONB,
    position_ms BIGINT NOT NULL DEFAULT 0,
    volume FLOAT NOT NULL DEFAULT 1.0,
    last_heartbeat TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ==============================================================================
-- Indexes for Sub-Millisecond Query Response
-- ==============================================================================
CREATE INDEX IF NOT EXISTS idx_cloud_playlists_user_id ON public.cloud_playlists(user_id) WHERE NOT is_deleted;
CREATE INDEX IF NOT EXISTS idx_cloud_playlist_tracks_playlist ON public.cloud_playlist_tracks(playlist_id, position);
CREATE INDEX IF NOT EXISTS idx_user_favorites_user ON public.user_favorites(user_id, liked_at DESC);
CREATE INDEX IF NOT EXISTS idx_listening_history_user_time ON public.listening_history(user_id, played_at DESC);
CREATE INDEX IF NOT EXISTS idx_active_sessions_user ON public.active_playback_sessions(user_id, last_heartbeat DESC);

-- ==============================================================================
-- Row Level Security (RLS) Policies
-- ==============================================================================
ALTER TABLE public.user_profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.cloud_playlists ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.cloud_playlist_tracks ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.user_favorites ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.listening_history ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.active_playback_sessions ENABLE ROW LEVEL SECURITY;

-- 1. Profiles Policy
CREATE POLICY "Users can manage their own profile"
    ON public.user_profiles FOR ALL
    USING (auth.uid() = id);

-- 2. Playlists Policy
CREATE POLICY "Users can manage their own playlists"
    ON public.cloud_playlists FOR ALL
    USING (auth.uid() = user_id);

-- 3. Playlist Tracks Policy
CREATE POLICY "Users can manage tracks in their playlists"
    ON public.cloud_playlist_tracks FOR ALL
    USING (
        EXISTS (
            SELECT 1 FROM public.cloud_playlists
            WHERE id = public.cloud_playlist_tracks.playlist_id
            AND user_id = auth.uid()
        )
    );

-- 4. Favorites Policy
CREATE POLICY "Users can manage their own favorites"
    ON public.user_favorites FOR ALL
    USING (auth.uid() = user_id);

-- 5. History Policy
CREATE POLICY "Users can manage their own listening history"
    ON public.listening_history FOR ALL
    USING (auth.uid() = user_id);

-- 6. Sessions Policy
CREATE POLICY "Users can manage their active devices"
    ON public.active_playback_sessions FOR ALL
    USING (auth.uid() = user_id);

-- ==============================================================================
-- Triggers: Auto Profile Creation on Auth Sign-Up
-- ==============================================================================
CREATE OR REPLACE FUNCTION public.handle_new_user_profile()
RETURNS TRIGGER AS $$
BEGIN
    INSERT INTO public.user_profiles (id, email, display_name, avatar_url)
    VALUES (
        NEW.id,
        NEW.email,
        COALESCE(NEW.raw_user_meta_data->>'full_name', NEW.raw_user_meta_data->>'name', 'UTA Wave User'),
        NEW.raw_user_meta_data->>'avatar_url'
    )
    ON CONFLICT (id) DO UPDATE SET
        email = EXCLUDED.email,
        updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

DROP TRIGGER IF EXISTS on_auth_user_created ON auth.users;
CREATE TRIGGER on_auth_user_created
    AFTER INSERT OR UPDATE ON auth.users
    FOR EACH ROW EXECUTE FUNCTION public.handle_new_user_profile();

-- Trigger: Auto-bump Playlist Version on Changes
CREATE OR REPLACE FUNCTION public.bump_playlist_version()
RETURNS TRIGGER AS $$
BEGIN
    UPDATE public.cloud_playlists
    SET sync_version = sync_version + 1, updated_at = NOW()
    WHERE id = COALESCE(NEW.playlist_id, OLD.playlist_id);
    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

DROP TRIGGER IF EXISTS on_playlist_track_mutated ON public.cloud_playlist_tracks;
CREATE TRIGGER on_playlist_track_mutated
    AFTER INSERT OR UPDATE OR DELETE ON public.cloud_playlist_tracks
    FOR EACH ROW EXECUTE FUNCTION public.bump_playlist_version();

-- ==============================================================================
-- Supabase Realtime Publications for Instant Multi-Device Sync
-- ==============================================================================
ALTER PUBLICATION supabase_realtime ADD TABLE public.cloud_playlists;
ALTER PUBLICATION supabase_realtime ADD TABLE public.cloud_playlist_tracks;
ALTER PUBLICATION supabase_realtime ADD TABLE public.user_favorites;
ALTER PUBLICATION supabase_realtime ADD TABLE public.active_playback_sessions;
