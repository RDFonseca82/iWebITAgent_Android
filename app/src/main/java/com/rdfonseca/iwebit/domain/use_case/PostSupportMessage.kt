package com.rdfonseca.iwebit.domain.use_case

import kotlinx.coroutines.flow.Flow
import com.rdfonseca.iwebit.common.Resource
import com.rdfonseca.iwebit.domain.repository.SupportMsgRepository

class PostSupportMessage(
    private val repository: SupportMsgRepository,
) {
    operator fun invoke(nome: String, suporte: String, uniqueId: String): Flow<Resource<Unit>> =
        repository.postSupportMsgs(nome, suporte, uniqueId)
}