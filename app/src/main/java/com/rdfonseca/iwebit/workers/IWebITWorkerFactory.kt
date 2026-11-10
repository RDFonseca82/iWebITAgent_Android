package com.rdfonseca.iwebit.workers

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import com.rdfonseca.iwebit.background.BackgroundWallpaperManager
import com.rdfonseca.iwebit.data.remote.IWebITApi
import com.rdfonseca.iwebit.data.repository.OfflineSyncRepository
import com.rdfonseca.iwebit.data.repository.UserPrefsRepositoryImpl
import com.rdfonseca.iwebit.data.util.Converters
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IWebITWorkerFactory @Inject constructor(
    private val userPrefsRepository: UserPrefsRepositoryImpl,
    private val api: IWebITApi,
    private val offlineSyncRepository: OfflineSyncRepository,
    private val converters: Converters,
    private val backgroundWallpaperManager: BackgroundWallpaperManager
) : WorkerFactory() {

    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters
    ): ListenableWorker? = when (workerClassName) {
        FullSyncWorker::class.java.name -> FullSyncWorker(
            appContext,
            workerParameters,
            userPrefsRepository,
            offlineSyncRepository,
            converters
        )

        LocationSyncWorker::class.java.name -> LocationSyncWorker(
            appContext,
            workerParameters,
            userPrefsRepository,
            offlineSyncRepository,
            converters
        )

        MinSyncWorker::class.java.name -> MinSyncWorker(
            appContext,
            workerParameters,
            userPrefsRepository,
            api,
            offlineSyncRepository,
            converters,
            backgroundWallpaperManager
        )

        else -> null
    }
}
