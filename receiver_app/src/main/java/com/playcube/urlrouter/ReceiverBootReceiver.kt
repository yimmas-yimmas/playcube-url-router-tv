package com.playcube.urlrouter

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Restarts the user-enabled local Wi-Fi receiver after a TV reboot or app update. */
class ReceiverBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED ||
            intent?.action == Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            LinkReceiverService.startIfEnabled(context)
        }
    }
}
