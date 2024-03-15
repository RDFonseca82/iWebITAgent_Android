package com.rdfonseca.iwebit.domain.use_case

import kotlinx.coroutines.flow.Flow
import com.rdfonseca.iwebit.common.Resource
import com.rdfonseca.iwebit.domain.repository.SupportMsgRepository
import java.io.File

class UploadScreenshot (
    private val repository: SupportMsgRepository,
) {
    operator fun invoke(file: File, uniqueId: String): Flow<Resource<Unit>> =
        repository.uploadScreenshot(file, uniqueId)
}
