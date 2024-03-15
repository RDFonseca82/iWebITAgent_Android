package com.rdfonseca.iwebit.data.util

import com.google.gson.reflect.TypeToken
import com.rdfonseca.iwebit.data.remote.dto.SupportMsgDto
import com.rdfonseca.iwebit.data.remote.dto.UninstallAppsDtoItem
import com.rdfonseca.iwebit.domain.model.BodyInJsonFull
import com.rdfonseca.iwebit.domain.model.BodyInJsonLocation
import com.rdfonseca.iwebit.domain.model.BodyInJsonMin

class Converters (
    private val jsonParser: JsonParser
) {
    fun fromSupportMessagesJson(json: String): List<SupportMsgDto> {
        return jsonParser.fromJson<ArrayList<SupportMsgDto>>(
            json,
            object : TypeToken<ArrayList<SupportMsgDto>>(){}.type
        ) ?: emptyList()
    }

    fun fromUninstallAppsJson(json: String): List<UninstallAppsDtoItem> {
        return jsonParser.fromJson<ArrayList<UninstallAppsDtoItem>>(
            json,
            object : TypeToken<ArrayList<UninstallAppsDtoItem>>(){}.type
        ) ?: emptyList()
    }

    fun toBodyJsonFull(bodyInJsonFull: BodyInJsonFull): String {
        return jsonParser.toJson(
            bodyInJsonFull,
            object : TypeToken<BodyInJsonFull>(){}.type
        ) ?: "{}"
    }

    fun toBodyJsonMin(bodyInJsonMin: BodyInJsonMin): String {
        return jsonParser.toJson(
            bodyInJsonMin,
            object : TypeToken<BodyInJsonMin>(){}.type
        ) ?: "{}"
    }

    fun toBodyJsonLocation(bodyInJsonLocation: BodyInJsonLocation): String {
        return jsonParser.toJson(
            bodyInJsonLocation,
            object : TypeToken<BodyInJsonLocation>(){}.type
        ) ?: "{}"
    }
}