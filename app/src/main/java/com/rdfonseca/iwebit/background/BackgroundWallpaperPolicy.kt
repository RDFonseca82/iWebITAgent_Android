package com.rdfonseca.iwebit.background

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

internal object BackgroundWallpaperPolicy {
    fun resolveImageUrl(rawValue: String?, baseUrl: String): HttpUrl? {
        val value = rawValue?.trim().orEmpty()
        if (value.isBlank()) return null

        val absolute = value.toHttpUrlOrNull()
        val resolved = absolute ?: baseUrl.toHttpUrlOrNull()?.resolve(value)
        return resolved?.takeIf { it.isHttps }
    }

    fun shouldApply(
        setBackground: Int,
        resolvedImageUrl: String?,
        lastAppliedUrl: String?,
        currentWallpaperId: Int,
        lastAppliedWallpaperId: Int
    ): Boolean {
        if (setBackground != 1 || resolvedImageUrl.isNullOrBlank()) return false
        return resolvedImageUrl != lastAppliedUrl ||
            currentWallpaperId <= 0 ||
            currentWallpaperId != lastAppliedWallpaperId
    }

    fun calculateInSampleSize(
        sourceWidth: Int,
        sourceHeight: Int,
        targetWidth: Int,
        targetHeight: Int
    ): Int {
        if (sourceWidth <= 0 || sourceHeight <= 0 || targetWidth <= 0 || targetHeight <= 0) {
            return 1
        }

        var sampleSize = 1
        while (
            sourceWidth / (sampleSize * 2) >= targetWidth &&
            sourceHeight / (sampleSize * 2) >= targetHeight
        ) {
            sampleSize *= 2
        }
        return sampleSize
    }
}