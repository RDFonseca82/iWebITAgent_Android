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

        // O agendamento não pode depender da recuperação da identidade. Se a leitura
        // do armazenamento falhar, os trabalhos periódicos continuam ativos e tentam
        // novamente quando o Android permitir a execução em background.
        runCatching {
            setupFullSyncWorker(this)
            setupMinSyncWorker(this)
        }.onFailure {
            logAndWrite(
                this,
                "Não foi possível agendar as sincronizações: ${it.localizedMessage}",
                "IWebITApplication",
                "onCreate",
                "ERROR"
            )
        }

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
            runCatching { enqueueInitialSync(this@IWebITApplication) }
                .onFailure {
                    logAndWrite(
                        this@IWebITApplication,
                        "Não foi possível iniciar a sincronização: ${it.localizedMessage}",
                        "IWebITApplication",
                        "onCreate",
                        "ERROR"
                    )
                }
        }
        if (BuildConfig.SELF_UPDATE_ENABLED) {
            runCatching { scheduleAppUpdateChecks(this) }
                .onFailure {
                    logAndWrite(
                        this,
                        "Não foi possível agendar o auto-update: ${it.localizedMessage}",
                        "IWebITApplication",
                        "onCreate",
                        "ERROR"
                    )
                }
        }
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
