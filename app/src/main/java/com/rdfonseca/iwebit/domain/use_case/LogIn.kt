package com.rdfonseca.iwebit.domain.use_case

import kotlinx.coroutines.flow.Flow
import com.rdfonseca.iwebit.common.Resource
import com.rdfonseca.iwebit.domain.model.BodyInJsonFull
import com.rdfonseca.iwebit.domain.model.CompanyDeviceInfo
import com.rdfonseca.iwebit.domain.repository.SupportMsgRepository

class LogIn (
    private val repository: SupportMsgRepository
) {
    operator fun invoke(uuid: String, idSync: String): Flow<Resource<CompanyDeviceInfo>> =
        repository.logIn(uuid, idSync)
}