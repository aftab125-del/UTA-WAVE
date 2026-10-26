package com.uta.wave.player

import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.Player

/**
 * Custom ForwardingPlayer for AndroidX Media3 MediaSession.
 *
 * Exposes seek-to-next and seek-to-previous capability to SystemUI notification player,
 * lockscreen player, Bluetooth headsets, and Android Auto, delegating navigation
 * directly to PlaybackManager's multi-track queue and radio pipeline.
 */
class WaveSessionPlayer(
    player: Player,
    private val playbackManager: PlaybackManager
) : ForwardingPlayer(player) {

    override fun getAvailableCommands(): Player.Commands {
        val commands = super.getAvailableCommands().buildUpon()
            .add(COMMAND_SEEK_TO_NEXT)
            .add(COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
            .add(COMMAND_SEEK_TO_PREVIOUS)
            .add(COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
        return commands.build()
    }

    override fun isCommandAvailable(command: Int): Boolean {
        return when (command) {
            COMMAND_SEEK_TO_NEXT,
            COMMAND_SEEK_TO_NEXT_MEDIA_ITEM -> playbackManager.hasNextTrack()
            COMMAND_SEEK_TO_PREVIOUS,
            COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM -> playbackManager.hasPreviousTrack()
            else -> super.isCommandAvailable(command)
        }
    }

    override fun hasNextMediaItem(): Boolean = playbackManager.hasNextTrack()

    override fun hasPreviousMediaItem(): Boolean = playbackManager.hasPreviousTrack()

    override fun seekToNext() {
        playbackManager.next()
    }

    override fun seekToNextMediaItem() {
        playbackManager.next()
    }

    override fun seekToPrevious() {
        playbackManager.previous()
    }

    override fun seekToPreviousMediaItem() {
        playbackManager.previous()
    }
}
