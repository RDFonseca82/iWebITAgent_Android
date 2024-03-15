package com.rdfonseca.iwebit.domain.repository

import kotlinx.coroutines.flow.Flow
import com.rdfonseca.iwebit.common.Resource
import com.rdfonseca.iwebit.domain.model.BodyInJsonFull
import com.rdfonseca.iwebit.domain.model.CompanyDeviceInfo
import com.rdfonseca.iwebit.domain.model.SupportMsg
import java.io.File

interface SupportMsgRepository {

    fun getSupportMsgs(uniqueId: String): Flow<Resource<List<SupportMsg>>>

    fun postSupportMsgs(nome: String, suporte: String, uniqueId: String): Flow<Resource<Unit>>

    fun logIn(uuid: String, idSync: String): Flow<Resource<CompanyDeviceInfo>>

    fun uploadScreenshot(file: File, uniqueId: String): Flow<Resource<Unit>>

}