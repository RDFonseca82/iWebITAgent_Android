package com.rdfonseca.iwebit

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.rdfonseca.iwebit.data.repository.UserPrefsRepositoryImpl
import com.rdfonseca.iwebit.common.logAndWrite
import com.rdfonseca.iwebit.common.enqueueInitialSync
import com.rdfonseca.iwebit.common.setupFullSyncWorker
import com.rdfonseca.iwebit.common.setupMinSyncWorker
import com.rdfonseca.iwebit.update.scheduleAppUpdateChecks
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@HiltAndroidApp
class IWebITApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var userPrefsRepository: UserPrefsRepositoryImpl

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        logAndWrite(this, "Agente iniciado; versão ${BuildConfig.VERSION_NAME}", "IWebITApplication", "onCreate")
        applicationScope.launch {
            userPrefsRepository.ensurePermanentIdentity()
            setupFullSyncWorker(this@IWebITApplication)
            setupMinSyncWorker(this@IWebITApplication)
            enqueueInitialSync(this@IWebITApplication)
        }
        if (BuildConfig.SELF_UPDATE_ENABLED) {
            scheduleAppUpdateChecks(this)
        }
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}