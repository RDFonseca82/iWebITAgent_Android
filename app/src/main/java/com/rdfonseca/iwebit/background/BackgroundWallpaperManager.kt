package com.rdfonseca.iwebit.background

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.ceil
import kotlin.math.max

@Singleton
class BackgroundWallpaperManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .callTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    suspend fun synchronize(setBackground: Int, backgroundImage: String?): BackgroundUpdateResult =
        withContext(Dispatchers.IO) {
            if (setBackground != 1) {
                preferences.edit()
                    .remove(KEY_LAST_APPLIED_URL)
                    .remove(KEY_LAST_APPLIED_WALLPAPER_ID)
                    .apply()
                return@withContext BackgroundUpdateResult.NoRequest
            }

            val imageUrl = BackgroundWallpaperPolicy.resolveImageUrl(
                rawValue = backgroundImage,
                baseUrl = BASE_IMAGE_URL
            ) ?: throw IllegalArgumentException(
                "BackgroundImage tem de indicar um URL HTTPS válido"
            )

            val wallpaperManager = WallpaperManager.getInstance(context)
            if (!wallpaperManager.isWallpaperSupported || !wallpaperManager.isSetWallpaperAllowed) {
                throw SecurityException("O dispositivo não permite alterar o background")
            }

            val imageUrlValue = imageUrl.toString()
            if (!BackgroundWallpaperPolicy.shouldApply(
                    setBackground = setBackground,
                    resolvedImageUrl = imageUrlValue,
                    lastAppliedUrl = preferences.getString(KEY_LAST_APPLIED_URL, null),
                    currentWallpaperId = wallpaperManager.getWallpaperId(WallpaperManager.FLAG_SYSTEM),
                    lastAppliedWallpaperId = preferences.getInt(KEY_LAST_APPLIED_WALLPAPER_ID, -1)
                )
            ) {
                return@withContext BackgroundUpdateResult.AlreadyApplied
            }

            val encodedImage = downloadImage(imageUrlValue)
            val targetWidth = context.resources.displayMetrics.widthPixels.coerceAtLeast(1)
            val targetHeight = context.resources.displayMetrics.heightPixels.coerceAtLeast(1)
            val source = decodeSampledBitmap(encodedImage, targetWidth, targetHeight)
            val wallpaper = centerCrop(source, targetWidth, targetHeight)

            try {
                wallpaperManager.setBitmap(
                    wallpaper,
                    Rect(0, 0, wallpaper.width, wallpaper.height),
                    true,
                    WallpaperManager.FLAG_SYSTEM
                )
            } finally {
                if (wallpaper !== source && !wallpaper.isRecycled) wallpaper.recycle()
                if (!source.isRecycled) source.recycle()
            }

            preferences.edit()
                .putString(KEY_LAST_APPLIED_URL, imageUrlValue)
                .putInt(
                    KEY_LAST_APPLIED_WALLPAPER_ID,
                    wallpaperManager.getWallpaperId(WallpaperManager.FLAG_SYSTEM)
                )
                .apply()
            BackgroundUpdateResult.Applied(imageUrl.host)
        }

    private fun downloadImage(url: String): ByteArray {
        val request = Request.Builder()
            .url(url)
            .header("Accept", "image/*")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Download do background devolveu HTTP ${response.code}")
            }
            if (!response.request.url.isHttps) {
                throw IOException("O redirecionamento do background não terminou em HTTPS")
            }

            val body = response.body ?: throw IOException("Resposta do background sem conteúdo")
            val declaredLength = body.contentLength()
            if (declaredLength > MAX_DOWNLOAD_BYTES) {
                throw IOException("Imagem de background excede o limite de ${MAX_DOWNLOAD_BYTES / 1024 / 1024} MB")
            }

            val output = ByteArrayOutputStream(
                declaredLength.takeIf { it in 1..MAX_DOWNLOAD_BYTES }?.toInt() ?: DEFAULT_BUFFER_SIZE
            )
            body.byteStream().use { input ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                var total = 0L
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    total += count
                    if (total > MAX_DOWNLOAD_BYTES) {
                        throw IOException("Imagem de background excede o limite de ${MAX_DOWNLOAD_BYTES / 1024 / 1024} MB")
                    }
                    output.write(buffer, 0, count)
                }
            }
            return output.toByteArray()
        }
    }

    private fun decodeSampledBitmap(
        encodedImage: ByteArray,
        targetWidth: Int,
        targetHeight: Int
    ): Bitmap {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(encodedImage, 0, encodedImage.size, bounds)
        if (
            bounds.outWidth <= 0 ||
            bounds.outHeight <= 0 ||
            bounds.outWidth > MAX_IMAGE_DIMENSION ||
            bounds.outHeight > MAX_IMAGE_DIMENSION
        ) {
            throw IOException("BackgroundImage não contém uma imagem suportada")
        }

        val options = BitmapFactory.Options().apply {
            inSampleSize = BackgroundWallpaperPolicy.calculateInSampleSize(
                sourceWidth = bounds.outWidth,
                sourceHeight = bounds.outHeight,
                targetWidth = targetWidth,
                targetHeight = targetHeight
            )
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }

        return BitmapFactory.decodeByteArray(encodedImage, 0, encodedImage.size, options)
            ?: throw IOException("Não foi possível descodificar BackgroundImage")
    }

    private fun centerCrop(source: Bitmap, targetWidth: Int, targetHeight: Int): Bitmap {
        val scale = max(
            targetWidth.toFloat() / source.width.toFloat(),
            targetHeight.toFloat() / source.height.toFloat()
        )
        val scaledWidth = ceil(source.width * scale).toInt().coerceAtLeast(targetWidth)
        val scaledHeight = ceil(source.height * scale).toInt().coerceAtLeast(targetHeight)
        val scaled = if (source.width == scaledWidth && source.height == scaledHeight) {
            source
        } else {
            Bitmap.createScaledBitmap(source, scaledWidth, scaledHeight, true)
        }

        if (scaled.width == targetWidth && scaled.height == targetHeight) return scaled

        val left = ((scaled.width - targetWidth) / 2).coerceAtLeast(0)
        val top = ((scaled.height - targetHeight) / 2).coerceAtLeast(0)
        val result = Bitmap.createBitmap(scaled, left, top, targetWidth, targetHeight)
        if (scaled !== source && scaled !== result && !scaled.isRecycled) scaled.recycle()
        return result
    }

    companion object {
        private const val BASE_IMAGE_URL = "https://agent.iwebit.app/"
        private const val PREFERENCES_NAME = "iwebit_background"
        private const val KEY_LAST_APPLIED_URL = "last_applied_url"
        private const val KEY_LAST_APPLIED_WALLPAPER_ID = "last_applied_wallpaper_id"
        private const val MAX_DOWNLOAD_BYTES = 10L * 1024L * 1024L
        private const val MAX_IMAGE_DIMENSION = 16_384
    }
}

sealed interface BackgroundUpdateResult {
    data object NoRequest : BackgroundUpdateResult
    data object AlreadyApplied : BackgroundUpdateResult
    data class Applied(val sourceHost: String) : BackgroundUpdateResult
}