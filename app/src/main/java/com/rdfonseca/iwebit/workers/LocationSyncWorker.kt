package com.rdfonseca.iwebit.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import com.rdfonseca.iwebit.common.Constants.LOCATION_DATA_EXTRA
import com.rdfonseca.iwebit.common.createSysTrayNotification
import com.rdfonseca.iwebit.common.logAndWrite
import com.rdfonseca.iwebit.data.remote.IWebITApi
import com.rdfonseca.iwebit.data.repository.UserPrefsRepositoryImpl
import com.rdfonseca.iwebit.data.util.Converters
import com.rdfonseca.iwebit.data.util.getDateHour
import com.rdfonseca.iwebit.domain.model.BodyInJsonLocation

@HiltWorker
class LocationSyncWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted params: WorkerParameters,
    private val userPrefsRepo: UserPrefsRepositoryImpl,
    private val api: IWebITApi,
    private val converters: Converters
): CoroutineWorker(context,params) {
    override suspend fun doWork(): Result {
        val userData = userPrefsRepo.userPreferencesFlow.first()
        val (lat, long) = inputData.getString(LOCATION_DATA_EXTRA)
            ?.split("|") ?: listOf("0.0", "0.0")
        logAndWrite(context, "SENDING NEW LOCATION: $lat, $long", "LocationSyncWorker", "doWork")

        try {
            api.postSync(converters.toBodyJsonLocation(
                BodyInJsonLocation(
                    userData.uniqueId,
                    userData.idSync,
                    getDateHour(),
                    lat,
                    long
                )
            ))
        } catch(e: CancellationException) {
            return Result.success()
        } catch (e: Exception) {
            logAndWrite(context, "ERROR: ${e.localizedMessage}", "LocationSyncWorker", "doWork")
            createSysTrayNotification(context, true)
        }

        return Result.success()
    }
}
