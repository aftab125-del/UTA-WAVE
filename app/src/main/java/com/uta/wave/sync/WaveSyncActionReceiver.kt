package com.uta.wave.sync

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat

class WaveSyncActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val client = WaveSyncClient.getInstance(context)
        val notifId = intent.getIntExtra(WaveSyncClient.EXTRA_NOTIFICATION_ID, 0)
        if (notifId != 0) {
            NotificationManagerCompat.from(context).cancel(notifId)
        }

        when (intent.action) {
            WaveSyncClient.ACTION_APPROVE_JOIN -> {
                val userId = intent.getStringExtra(WaveSyncClient.EXTRA_USER_ID) ?: return
                client.approveJoin(userId)
            }
            WaveSyncClient.ACTION_REJECT_JOIN -> {
                val userId = intent.getStringExtra(WaveSyncClient.EXTRA_USER_ID) ?: return
                client.rejectJoin(userId, null)
            }
            WaveSyncClient.ACTION_APPROVE_SUGGESTION -> {
                val suggestionId = intent.getStringExtra(WaveSyncClient.EXTRA_SUGGESTION_ID) ?: return
                client.approveSuggestion(suggestionId)
            }
            WaveSyncClient.ACTION_REJECT_SUGGESTION -> {
                val suggestionId = intent.getStringExtra(WaveSyncClient.EXTRA_SUGGESTION_ID) ?: return
                client.rejectSuggestion(suggestionId, null)
            }
        }
    }
}
