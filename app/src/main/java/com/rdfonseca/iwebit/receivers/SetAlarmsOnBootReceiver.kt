package com.rdfonseca.iwebit.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.rdfonseca.iwebit.common.enqueueInitialSync
import com.rdfonseca.iwebit.common.logAndWrite
import com.rdfonseca.iwebit.common.setupFullSyncWorker
import com.rdfonseca.iwebit.common.setupMinSyncWorker

class SetAlarmsOnBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED ||
            intent?.action == Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            logAndWrite(
                context,
                "A preparar sincronização após ${intent.action}",
                "SetAlarmsOnBootReceiver",
                "onReceive"
            )
            setupFullSyncWorker(context)
            setupMinSyncWorker(context)
            enqueueInitialSync(context)
        }
    }
}
