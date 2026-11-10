package com.rdfonseca.iwebit.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.rdfonseca.iwebit.common.Constants.LOCATION_DATA_EXTRA
import com.rdfonseca.iwebit.common.createSysTrayNotification
import com.rdfonseca.iwebit.common.logAndWrite
import com.rdfonseca.iwebit.data.repository.OfflineSyncRepository
import com.rdfonseca.iwebit.data.repository.UserPrefsRepositoryImpl
import com.rdfonseca.iwebit.data.util.Converters
import com.rdfonseca.iwebit.data.util.getDateHour
import com.rdfonseca.iwebit.domain.model.BodyInJsonLocation
import com.rdfonseca.iwebit.domain.model.SyncFlushResult
import com.rdfonseca.iwebit.domain.model.SyncType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first

class LocationSyncWorker(
    private val context: Context,
    params: WorkerParameters,
    private val userPrefsRepo: UserPrefsRepositoryImpl,
    private val offlineSyncRepository: OfflineSyncRepository,
    private val converters: Converters
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val userData = userPrefsRepo.userPreferencesFlow.first()
        if (userData.uniqueId.isBlank() || userData.idSync.isBlank()) {
            logAndWrite(context, "Sincronização ignorada: identidade ou IDSync em falta", "LocationSyncWorker", "doWork", "WARN")
            return Result.success()
        }

        val locationParts = inputData.getString(LOCATION_DATA_EXTRA)?.split("|")
        if (locationParts == null || locationParts.size != 2) return Result.failure()
        val (latitude, longitude) = locationParts
        logAndWrite(
            context,
            "SENDING NEW LOCATION: $latitude, $longitude",
            "LocationSyncWorker",
            "doWork"
        )

        return try {
            val payload = converters.toBodyJsonLocation(
                BodyInJsonLocation(
                    userData.uniqueId,
                    userData.idSync,
                    getDateHour(),
                    latitude,
                    longitude
                )
            )
            offlineSyncRepository.enqueue(SyncType.LOCATION, payload)
            when (offlineSyncRepository.flush()) {
                SyncFlushResult.SUCCESS -> Result.success()
                SyncFlushResult.RETRY -> Result.retry()
                SyncFlushResult.FAILURE -> Result.failure()
            }
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            logAndWrite(context, "ERROR: ${exception.localizedMessage}", "LocationSyncWorker", "doWork")
            createSysTrayNotification(context, true)
            Result.retry()
        }
    }
}
