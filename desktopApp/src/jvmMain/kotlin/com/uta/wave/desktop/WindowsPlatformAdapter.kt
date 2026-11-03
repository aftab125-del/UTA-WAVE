package com.uta.wave.desktop

import com.uta.wave.audio.UnifiedPlaybackState

/**
 * Windows-specific system integrations interface.
 * Implements native Windows 10/11 platform hooks via Win32 / WinRT APIs.
 */
interface WindowsPlatformAdapter {

    /**
     * Initializes the Windows System Media Transport Controls (SMTC).
     * Connects to WinRT API to display track metadata on the Windows volume overlay,
     * lockscreen, and taskbar thumbnail preview.
     */
    fun initSystemMediaTransportControls(
        onPlayPauseClicked: () -> Unit,
        onNextClicked: () -> Unit,
        onPreviousClicked: () -> Unit,
        onStopClicked: () -> Unit
    )

    /**
     * Updates track metadata and playback status on the Windows volume overlay.
     */
    fun updateSmtcPlaybackState(state: UnifiedPlaybackState)

    /**
     * Creates and mounts the Windows System Tray icon with context menu.
     */
    fun setupSystemTray(
        onRestoreWindow: () -> Unit,
        onPlayPause: () -> Unit,
        onNext: () -> Unit,
        onExitApp: () -> Unit
    )

    /**
     * Applies Windows 11 Mica or Acrylic translucent backdrop effect to the window.
     */
    fun applyWindowMicaBackdrop(windowHandle: Long, isDarkTheme: Boolean)

    /**
     * Registers global Windows keyboard media keys (VK_MEDIA_PLAY_PAUSE, etc.).
     */
    fun registerGlobalMediaKeys(
        onPlayPause: () -> Unit,
        onNext: () -> Unit,
        onPrevious: () -> Unit
    )
}
