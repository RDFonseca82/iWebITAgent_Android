package com.rdfonseca.iwebit.domain.use_case

import kotlinx.coroutines.flow.Flow
import com.rdfonseca.iwebit.common.Resource
import com.rdfonseca.iwebit.domain.model.SupportMsg
import com.rdfonseca.iwebit.domain.repository.SupportMsgRepository

class GetSupportMessages(
    private val repository: SupportMsgRepository
) {
    operator fun invoke(uniqueId: String): Flow<Resource<List<SupportMsg>>> = repository.getSupportMsgs(uniqueId)
}