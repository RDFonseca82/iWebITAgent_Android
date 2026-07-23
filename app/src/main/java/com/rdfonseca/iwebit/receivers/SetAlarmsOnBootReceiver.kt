package com.rdfonseca.iwebit.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.rdfonseca.iwebit.common.logAndWrite
import com.rdfonseca.iwebit.common.setupFullSyncWorker
import com.rdfonseca.iwebit.common.setupMinSyncWorker


class SetAlarmsOnBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (Intent.ACTION_BOOT_COMPLETED == intent?.action) {
            logAndWrite(context, "SETTING ALARMS ON BOOT", "SetAlarmsOnBootReceiver", "onReceive")
            setupFullSyncWorker(context)
            setupMinSyncWorker(context)
        }
    }
}