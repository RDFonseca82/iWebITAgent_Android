package com.rdfonseca.iwebit.data.util

import com.rdfonseca.iwebit.data.remote.dto.CompanyInfoDto
import java.security.MessageDigest
import java.sql.Timestamp
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter


fun CompanyInfoDto.generateUniqueId(uuid: String): String {
    return MessageDigest
        .getInstance("SHA-256")
        .digest((IdCompany + uuid).toByteArray())
        .fold("") { str, it -> str + "%02x".format(it) }
}


fun getDateHour(): String = Timestamp(System.currentTimeMillis()).toString().slice(0..18)


fun formatFromStringDateHour(string: String): LocalDateTime {
    // 2022-08-11 19:11:59
    val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    
    return try {
        LocalDateTime.parse(string, formatter) ?: LocalDateTime.of(1900, 1, 1, 1, 1, 1)
    } catch (e: Exception) {
        LocalDateTime.of(1900, 1, 1, 1, 1, 1)
    }
}


