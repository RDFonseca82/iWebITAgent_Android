package com.rdfonseca.iwebit.presentation.utils

import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*


fun formatDateToHumanReadable(dateToParse: String, sdf: SimpleDateFormat): String {
    val today = Date()
    val dateParsed: Date = try {
        sdf.parse(dateToParse) ?: today
    } catch (e: ParseException) {
        today
    }

    val timeDelta = (today.time - dateParsed.time)/1000

    val days = timeDelta/86400

    if (days > 365) {
        val years = days / 365
        return "há $years ${if (years > 1) "anos" else "ano"}"
    }
    else if (days > 30) {
        val months = days/30
        return "há $months ${if (months > 1) "meses" else "mês"}"
    }
    else if (days > 14) {
        val weaks = days/7
        return "há $weaks ${if (weaks > 1) "semanas" else "semana"}"
    }
    else if (days > 0)
        return "há $days ${if (days > 1) "dias" else "dia"}"

    val hours = timeDelta%86400/3600
    if (hours > 0)
        return "há $hours ${if (hours > 1) "horas" else "hora"}"

    val minutes = timeDelta%86400%3600/60
    if (minutes > 0)
        return "há $minutes ${if (minutes > 1) "minutos" else "minuto"}"

    val seconds = timeDelta%86400%3600%60
    return "há $seconds ${if (seconds > 1) "segundos" else "segundo"}"


}
