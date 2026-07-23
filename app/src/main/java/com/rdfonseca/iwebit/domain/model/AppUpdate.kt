package com.rdfonseca.iwebit.domain.model

data class AppUpdate(
    val schemaVersion: Int = 1,
    val versionCode: Long = 0,
    val versionName: String = "",
    val apkUrl: String = "",
    val sha256: String = "",
    val fileSize: Long = 0,
    val releaseNotes: String = "",
    val publishedAt: String = ""
)