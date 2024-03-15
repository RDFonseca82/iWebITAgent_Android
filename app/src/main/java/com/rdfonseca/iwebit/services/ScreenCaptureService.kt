package com.rdfonseca.iwebit.services

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image.Plane
import android.media.ImageReader
import android.media.ImageReader.OnImageAvailableListener
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Display
import android.view.OrientationEventListener
import android.view.WindowManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.rdfonseca.iwebit.common.Constants.ACTION_CAPTURED_REQUESTED_SCREEN
import com.rdfonseca.iwebit.common.Constants.ACTION_CAPTURED_SCREEN
import com.rdfonseca.iwebit.common.Constants.GERENCIADOR_REQUEST_EXTRA
import com.rdfonseca.iwebit.common.Constants.SCREENSHOT_NOTIFICATION_ID
import com.rdfonseca.iwebit.common.Constants.SCREENSHOT_REQUESTED_NOTIFICATION_ID
import com.rdfonseca.iwebit.common.cancelNotification
import com.rdfonseca.iwebit.common.createNotification
import com.rdfonseca.iwebit.common.createRequestedScreenShotNotification
import com.rdfonseca.iwebit.common.logAndWrite
import com.rdfonseca.iwebit.presentation.MockActivityPermission
import com.rdfonseca.iwebit.presentation.ScreenshotShowCaseActivity
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class ScreenCaptureService : Service() {

    private var mediaProjection: MediaProjection? = null
    private var storeDir: String? = null
    private var imageReader: ImageReader? = null
    private var handler: Handler? = null
    private var deviceDisplay: Display? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var density = 0
    private var width = 0
    private var height = 0
    private var rotation = 0
    private var capturedTheScreenshot = false
    private var canTakeScreenshot = false
    private var orientationChangeCallback: OrientationEventListener? = null
    private var userStillSignedIn = true
    private var started = false
    private var requestedScreenshot = false

    private fun isStartCommand(intent: Intent): Boolean {
        return (intent.hasExtra(RESULT_CODE) &&
                intent.hasExtra(DATA) &&
                intent.hasExtra(ACTION) &&
                intent.getStringExtra(ACTION) == START)
    }

    private fun getVirtualDisplayFlags(): Int {
        return DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY or DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC
    }

    override fun onCreate() {
        super.onCreate()

        val externalFilesDir = getExternalFilesDir(null)
        if (externalFilesDir != null) {
            storeDir = externalFilesDir.absolutePath + "/screenshots/"
            val storeDirectory = File(storeDir!!)
            if (!storeDirectory.exists()) {
                val success = storeDirectory.mkdirs()
                if (!success) {
                    logAndWrite(this, "ERROR: Failed to create screenshots directory.", "ScreenCaptureService", "onCreate")
                    stopSelf()
                }
            }
        } else {
            logAndWrite(this, "ERROR: getExternalFilesDir is null.", "ScreenCaptureService", "onCreate")
            stopSelf()
        }

        object : Thread() {
            override fun run() {
                Looper.prepare()
                handler = Handler()
                Looper.loop()
            }
        }.start()
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {

        if (isStartCommand(intent)) {
            if (started) return super.onStartCommand(intent, flags, startId)
            started = true
            val gerenciadorRequested = intent.getBooleanExtra(GERENCIADOR_REQUEST_EXTRA, false)

            if (!gerenciadorRequested) {
                requestedScreenshot = false
                val notification = createScreenShotNotification(this)
                startForeground(SCREENSHOT_NOTIFICATION_ID, notification)
            } else {
                requestedScreenshot = true
                val notification = createRequestedScreenShotNotification(this,
                    "A capturar ecrã...",
                    waitingForUserConfirmation = false,
                    capturedScreen = false
                )
                startForeground(SCREENSHOT_REQUESTED_NOTIFICATION_ID, notification)
            }

            val resultCode = intent.getIntExtra(RESULT_CODE, Activity.RESULT_CANCELED)
            val data = intent.getParcelableExtra<Intent>(DATA)
            if (data != null) {
                startProjection(resultCode, data)
            }

            if (!gerenciadorRequested) {
                CoroutineScope(Dispatchers.Main).launch {
                    for (i in 3 downTo 1) {
                        if (!userStillSignedIn) {
                            break
                        }
                        createScreenShotNotification(this@ScreenCaptureService, "A capturar ecrã em $i...")
                        delay(1000)
                    }
                    cancelNotification(SCREENSHOT_NOTIFICATION_ID, this@ScreenCaptureService)
                    if (userStillSignedIn) canTakeScreenshot = true
                }
            } else {
                CoroutineScope(Dispatchers.Main).launch {
                    delay(500)
                    if (userStillSignedIn) canTakeScreenshot = true
                }
            }
        } else {
            started = true
            userStillSignedIn = false
            cancelNotification(SCREENSHOT_NOTIFICATION_ID, this)
            stopProjection()
            stopForeground(false)
            stopSelf()
        }

        return super.onStartCommand(intent, flags, startId)
    }

    private val imageAvailableListenerObj = OnImageAvailableListener {
        var fos: FileOutputStream? = null
        var bitmap: Bitmap? = null
        try {
            imageReader?.acquireLatestImage().use { image ->
                if (image != null) {
                    if (!capturedTheScreenshot && canTakeScreenshot) {
                        cancelNotification(SCREENSHOT_NOTIFICATION_ID, this)
                        capturedTheScreenshot = true
                        val planes: Array<Plane> = image.planes
                        val buffer = planes[0].buffer
                        val pixelStride = planes[0].pixelStride
                        val rowStride = planes[0].rowStride
                        val rowPadding: Int = rowStride - pixelStride * width

                        // create bitmap
                        bitmap = Bitmap.createBitmap(
                            width + rowPadding / pixelStride,
                            height,
                            Bitmap.Config.ARGB_8888
                        )
                        bitmap?.copyPixelsFromBuffer(buffer)

                        // write bitmap to a file
                        fos = FileOutputStream("$storeDir/ScreenShot.png")
                        bitmap?.compress(Bitmap.CompressFormat.JPEG, 100, fos)

                        if (requestedScreenshot) {
                            val intent = Intent(
                                this,
                                MockActivityPermission::class.java
                            ).apply {
                                action = ACTION_CAPTURED_REQUESTED_SCREEN
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                            }
                            startActivity(intent)
                        } else {
                            val intent = Intent(
                                this,
                                ScreenshotShowCaseActivity::class.java
                            ).apply {
                                action = ACTION_CAPTURED_SCREEN
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                            }
                            startActivity(intent)
                        }
                        stopProjection()
                        stopSelf()
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            if (fos != null) {
                try {
                    fos!!.close()
                } catch (ioe: IOException) {
                    ioe.printStackTrace()
                }
            }
            if (bitmap != null) {
                bitmap!!.recycle()
            }
        }
    }


    override fun onBind(p0: Intent?): IBinder? = null

    private fun startProjection(resultCode: Int, data: Intent) {
        val mpManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        if (mediaProjection == null) {
            mediaProjection = mpManager.getMediaProjection(resultCode, data)
            if (mediaProjection != null) {
                density = Resources.getSystem().displayMetrics.densityDpi
                val windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
                deviceDisplay = windowManager.defaultDisplay

                createVirtualDisplay()

                orientationChangeCallback = object : OrientationEventListener(this) {

                    override fun onOrientationChanged(orientation: Int) {
                        val dRotation: Int = deviceDisplay!!.rotation
                        if (dRotation != rotation) {
                            rotation = dRotation
                            try {
                                virtualDisplay?.release()
                                imageReader?.setOnImageAvailableListener(
                                    null,
                                    null
                                )

                                createVirtualDisplay()
                            } catch (e: java.lang.Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                }

                if ((orientationChangeCallback as OrientationEventListener).canDetectOrientation()) {
                    (orientationChangeCallback as OrientationEventListener).enable()
                }

                mediaProjection!!.registerCallback(
                    object : MediaProjection.Callback() {
                        override fun onStop() {
                            logAndWrite(this@ScreenCaptureService, "STOPPING PROJECTION", "ScreenCaptureService", "startProjection")
                            handler?.post {
                                virtualDisplay?.release()
                                imageReader?.setOnImageAvailableListener(
                                    null,
                                    null
                                )
                                if (orientationChangeCallback != null) (orientationChangeCallback as OrientationEventListener).disable()
                                mediaProjection!!.unregisterCallback(this)
                            }
                        }
                    },
                    handler
                )
            }
        }
    }

    private fun stopProjection() {
        handler?.post {
            if (mediaProjection != null) {
                mediaProjection!!.stop()
                capturedTheScreenshot = false
                canTakeScreenshot = false
            }
        }
    }

    @SuppressLint("WrongConstant")
    private fun createVirtualDisplay() {
        // get width and height
        width = Resources.getSystem().displayMetrics.widthPixels
        height = Resources.getSystem().displayMetrics.heightPixels

        // start capture reader
        imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
        virtualDisplay = mediaProjection?.createVirtualDisplay(
            SCREENCAP_NAME,
            width,
            height,
            density,
            getVirtualDisplayFlags(),
            imageReader!!.surface,
            null,
            handler
        )
        imageReader!!.setOnImageAvailableListener(imageAvailableListenerObj, handler)
    }

    private fun createScreenShotNotification(context: Context, message: String = "A capturar ecrã em 3..."): Notification = createNotification(
        context = context,
        title = "IWebIT",
        message = message,
        notificationId = SCREENSHOT_NOTIFICATION_ID,
        cancelPrevious = true
    )

    companion object {
        private const val DATA = "DATA"
        private const val START = "START"
        private const val SCREENCAP_NAME = "screencap"
        private const val ACTION = "ACTION"
        private const val RESULT_CODE = "RESULT_CODE"

        fun getStartIntent(context: Context?, resultCode: Int, data: Intent?): Intent {
            val intent = Intent(context, ScreenCaptureService::class.java)
            intent.putExtra(ACTION, START)
            intent.putExtra(RESULT_CODE, resultCode)
            intent.putExtra(DATA, data)
            return intent
        }
    }

}