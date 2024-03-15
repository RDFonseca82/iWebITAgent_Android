package com.rdfonseca.iwebit.presentation

import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import com.rdfonseca.iwebit.common.Constants
import com.rdfonseca.iwebit.common.Constants.ACTION_ASK_AND_CAPTURE_SCREEN
import com.rdfonseca.iwebit.common.Constants.ACTION_CAPTURED_REQUESTED_SCREEN
import com.rdfonseca.iwebit.common.Constants.ACTION_FORCE_FULL_SYNC
import com.rdfonseca.iwebit.common.Constants.ACTION_REQUESTED_ASK_AND_CAPTURE_SCREEN
import com.rdfonseca.iwebit.common.Constants.ACTION_TRY_AGAIN_LOCATION
import com.rdfonseca.iwebit.common.Constants.GERENCIADOR_REQUEST_EXTRA
import com.rdfonseca.iwebit.common.Constants.SCREENSHOT_REQUESTED_DONE_NOTIFICATION_ID
import com.rdfonseca.iwebit.common.Resource
import com.rdfonseca.iwebit.common.createNotification
import com.rdfonseca.iwebit.data.repository.UserPrefsRepositoryImpl
import com.rdfonseca.iwebit.domain.repository.SupportMsgRepository
import com.rdfonseca.iwebit.services.ScreenCaptureService
import com.rdfonseca.iwebit.services.SincLocationService
import com.rdfonseca.iwebit.workers.FullSyncWorker
import java.io.File
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class MockActivityPermission @Inject constructor() : ComponentActivity() {

    @Inject
    lateinit var userPrefsRepositoryImpl: UserPrefsRepositoryImpl

    @Inject
    lateinit var repository: SupportMsgRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        when (intent.action) {
            ACTION_ASK_AND_CAPTURE_SCREEN -> {
                askAndCaptureScreen(::finishAndRemoveTask)
            }
            ACTION_REQUESTED_ASK_AND_CAPTURE_SCREEN -> {
                askAndCaptureScreen(::finishAndRemoveTask, true)
            }
            ACTION_CAPTURED_REQUESTED_SCREEN -> {
                val externalFilesDir = getExternalFilesDir(null)
                if (externalFilesDir != null) {
                    val path = externalFilesDir.absolutePath + "/screenshots/" + "ScreenShot.png"
                    val file = File(path)
                    if (!file.exists()) {
                        finish()
                    }
                    moveTaskToBack(true)

                    CoroutineScope(Dispatchers.IO).launch {
                        repository.uploadScreenshot(
                            file,
                            userPrefsRepositoryImpl.userPreferencesFlow.first().uniqueId
                        ).onEach { result ->
                            when (result) {
                                is Resource.Success  -> {
                                    createNotification(
                                        this@MockActivityPermission,
                                        title = "IWebIT",
                                        message = "Captura de ecrã enviada com sucesso.",
                                        notificationId = SCREENSHOT_REQUESTED_DONE_NOTIFICATION_ID
                                    )
                                    finishAndRemoveTask()
                                }
                                is Resource.Error  -> {
                                    finishAndRemoveTask()
                                }
                                else -> {}
                            }
                        }.launchIn(this)
                    }
                } else {
                    finishAndRemoveTask()
                }
            }
            ACTION_FORCE_FULL_SYNC -> {
                CoroutineScope(Dispatchers.IO).launch {
                    val userData = userPrefsRepositoryImpl.userPreferencesFlow.first()
                    if (Date().time - userData.lastForceFullSync < 15000) {
                        Toast.makeText(this@MockActivityPermission, "Ação limitada a cada 15 segundos.", Toast.LENGTH_LONG).show()
                        finishAndRemoveTask()
                        return@launch
                    }
                    val workerData = Data.Builder().apply {
                        putBoolean(Constants.FORCE_FULL_EXTRA, true)
                    }

                    val oneTimeWorkRequest = OneTimeWorkRequestBuilder<FullSyncWorker>()
                        .setInputData(workerData.build())
                        .build()

                    WorkManager.getInstance(this@MockActivityPermission).enqueueUniqueWork(
                        Constants.WORKER_FORCE_SYNC_TAG,
                        ExistingWorkPolicy.REPLACE,
                        oneTimeWorkRequest
                    )

                    finishAndRemoveTask()
                }
            }
            ACTION_TRY_AGAIN_LOCATION -> {
                val tryAgainIntent = Intent(this, SincLocationService::class.java).apply {
                    action = ACTION_TRY_AGAIN_LOCATION
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(tryAgainIntent)
                } else {
                    startService(tryAgainIntent)
                }
                finishAndRemoveTask()
            }
        }
    }

    private fun askAndCaptureScreen(
        onComplete: () -> Unit = {},
        requestedScreenshot: Boolean = false
    ) {
        val mProjectionManager = getSystemService(MEDIA_PROJECTION_SERVICE)
                as MediaProjectionManager

        registerForActivityResult(ActivityResultContracts.StartActivityForResult()){ result ->
            if (result.resultCode == RESULT_OK) {
                val screenshotIntent = ScreenCaptureService.getStartIntent(
                    this,
                    result.resultCode,
                    result.data
                )

                if (requestedScreenshot) {
                    screenshotIntent.apply {
                        putExtra(GERENCIADOR_REQUEST_EXTRA, true)
                    }
                }

                startService(
                    screenshotIntent
                )
            }
            onComplete()
        }.launch(mProjectionManager.createScreenCaptureIntent())
    }
}