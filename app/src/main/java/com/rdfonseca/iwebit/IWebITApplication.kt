package com.rdfonseca.iwebit

import android.app.Application
import androidx.work.Configuration
import com.rdfonseca.iwebit.data.repository.UserPrefsRepositoryImpl
import com.rdfonseca.iwebit.common.logAndWrite
import com.rdfonseca.iwebit.common.enqueueInitialSync
import com.rdfonseca.iwebit.common.setupFullSyncWorker
import com.rdfonseca.iwebit.common.setupMinSyncWorker
import com.rdfonseca.iwebit.update.scheduleAppUpdateChecks
import com.rdfonseca.iwebit.workers.IWebITWorkerFactory
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@HiltAndroidApp
class IWebITApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: IWebITWorkerFactory

    @Inject
    lateinit var userPrefsRepository: UserPrefsRepositoryImpl

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        logAndWrite(this, "Agente iniciado; versão ${BuildConfig.VERSION_NAME}", "IWebITApplication", "onCreate")

        // O agendamento não pode depender da recuperação da identidade. Se a leitura
        // do armazenamento falhar, os trabalhos periódicos continuam ativos e tentam
        // novamente quando o Android permitir a execução em background.
        setupFullSyncWorker(this)
        setupMinSyncWorker(this)

        applicationScope.launch {
            runCatching { userPrefsRepository.ensurePermanentIdentity() }
                .onFailure {
                    logAndWrite(
                        this@IWebITApplication,
                        "Não foi possível validar a identidade permanente: ${it.localizedMessage}",
                        "IWebITApplication",
                        "onCreate",
                        "ERROR"
                    )
                }
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
