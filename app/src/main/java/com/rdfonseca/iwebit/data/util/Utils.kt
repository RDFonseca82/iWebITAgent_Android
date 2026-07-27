package com.rdfonseca.iwebit.data.util

import com.rdfonseca.iwebit.data.identity.DeviceIdentity
import com.rdfonseca.iwebit.data.remote.dto.CompanyInfoDto
import java.sql.Timestamp
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

fun CompanyInfoDto.generateUniqueId(uuid: String): String {
    return DeviceIdentity.permanentUniqueId(uuid)
}

fun getDateHour(): String = Timestamp(System.currentTimeMillis()).toString().slice(0..18)

fun formatFromStringDateHour(string: String): LocalDateTime {
    val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    return try {
        LocalDateTime.parse(string, formatter)
    } catch (e: Exception) {
        LocalDateTime.of(1900, 1, 1, 1, 1, 1)
    }
}