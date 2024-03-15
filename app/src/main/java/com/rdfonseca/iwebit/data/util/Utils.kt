package com.rdfonseca.iwebit.data.util

import com.rdfonseca.iwebit.data.remote.dto.CompanyInfoDto
import java.security.MessageDigest
import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.util.*


fun CompanyInfoDto.generateUniqueId(uuid: String): String {
    return MessageDigest
        .getInstance("SHA-256")
        .digest((IdCompany + uuid).toByteArray())
        .fold("") { str, it -> str + "%02x".format(it) }
}


fun getDateHour(): String = Timestamp(System.currentTimeMillis()).toString().slice(0..18)


fun  formatFromStringDateHour(string: String): Date {
    // 2022-08-11 19:11:59
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    return try {
        sdf.parse(string.slice(0..18)) ?: Date(1900, 1,1, 1,1,1)
    } catch (e: Exception) {
        Date(1900, 1,1, 1,1,1)
    }
}


