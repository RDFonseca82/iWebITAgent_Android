package com.rdfonseca.iwebit.data.repository

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import com.rdfonseca.iwebit.common.Constants.HTTP_EXCEPTION_MESSAGE
import com.rdfonseca.iwebit.common.Constants.IO_EXCEPTION_MESSAGE
import com.rdfonseca.iwebit.common.Constants.NULL_POINTER_EXCEPTION_MESSAGE
import com.rdfonseca.iwebit.common.Constants.SOCKET_TIMEOUT_EXCEPTION_MESSAGE
import com.rdfonseca.iwebit.common.Resource
import com.rdfonseca.iwebit.common.logAndWrite
import com.rdfonseca.iwebit.data.remote.IWebITApi
import com.rdfonseca.iwebit.data.remote.IWebITDomainApi
import com.rdfonseca.iwebit.data.util.Converters
import com.rdfonseca.iwebit.data.util.generateUniqueId
import com.rdfonseca.iwebit.domain.model.CompanyDeviceInfo
import com.rdfonseca.iwebit.domain.model.SupportMsg
import com.rdfonseca.iwebit.domain.model.getBodyInJsonFull
import com.rdfonseca.iwebit.domain.repository.SupportMsgRepository
import retrofit2.HttpException
import java.io.File
import java.io.IOException
import java.net.SocketTimeoutException
import java.text.SimpleDateFormat
import java.util.*

class SupportMsgRepositoryImpl(
    private val api: IWebITApi,
    private val apiDomain: IWebITDomainApi,
    private val converters: Converters,
    private val context: Context
) : SupportMsgRepository {

    private val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    override fun getSupportMsgs(uniqueId: String): Flow<Resource<List<SupportMsg>>> = flow {
        emit(Resource.Loading())

        try {
            val result = api.getSupportMessages(uniqueId = uniqueId)
            val json = "[" + (result.body()?.replace("}{", "}, {") ?: "") + "]"
            val messages = converters.fromSupportMessagesJson(json)
                .map { it.toSupportMsg() }
                .sortedByDescending { sdf.parse(it.date) ?: Date() }

            emit(Resource.Success(messages))
        } catch (e: HttpException) {
            emit(Resource.Error(message = HTTP_EXCEPTION_MESSAGE))
        } catch (e: SocketTimeoutException) {
            emit(Resource.Error(message = SOCKET_TIMEOUT_EXCEPTION_MESSAGE))
        } catch (e: IOException) {
            emit(Resource.Error(message = IO_EXCEPTION_MESSAGE))
        }
    }

    override fun postSupportMsgs(nome: String, suporte: String, uniqueId: String): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading())

        try {
            api.postMessageSupport(
                uniqueId = uniqueId,
                nome = nome,
                msg = suporte,
                data = sdf.format(Date())
            )
            emit(Resource.Success(Unit))
        } catch (e: HttpException) {
            emit(Resource.Error(message = HTTP_EXCEPTION_MESSAGE))
        } catch (e: SocketTimeoutException) {
            emit(Resource.Error(message = SOCKET_TIMEOUT_EXCEPTION_MESSAGE))
        } catch (e: IOException) {
            emit(Resource.Error(message = IO_EXCEPTION_MESSAGE))
        }
    }

    override fun logIn(uuid: String, idSync: String): Flow<Resource<CompanyDeviceInfo>> = flow {
        emit(Resource.Loading(CompanyDeviceInfo()))

        try {
            val companyInfoDto = api.getCompanyInfo(idSync)

            val uniqueId = companyInfoDto.generateUniqueId(uuid)
            val bodyInJsonFull = getBodyInJsonFull(
                context,
                uniqueId = uniqueId,
                idsync = idSync,
                getHostName = true
            )
            api.postSync(converters.toBodyJsonFull(bodyInJsonFull))
            try {
                emit(Resource.Success(CompanyDeviceInfo(
                    uniqueId = uniqueId,
                    idSync = bodyInJsonFull.idsync,
                    idCompany = companyInfoDto.IdCompany,
                    companyName = companyInfoDto.Company,
                    timeSync = companyInfoDto.TimeSync,
                    timeAlive = companyInfoDto.TimeAlive
                )))
            } catch (e: Exception) {
                throw Exception("Deu barraca aqui, ${companyInfoDto.IdCompany} ${idSync} ")
            }

        } catch (e: KotlinNullPointerException) { // Throws KotlinNullPointerException when api returns null, because idSync was not found
            emit(Resource.Error(message = NULL_POINTER_EXCEPTION_MESSAGE))
        } catch (e: HttpException) {
            emit(Resource.Error(message = HTTP_EXCEPTION_MESSAGE))
        } catch (e: SocketTimeoutException) {
            emit(Resource.Error(message = SOCKET_TIMEOUT_EXCEPTION_MESSAGE))
        } catch (e: IOException) {
            emit(Resource.Error(message = IO_EXCEPTION_MESSAGE))
        } catch (e: Exception) {
            emit(Resource.Error(message = e.localizedMessage ?: "Oops"))
        }
    }

    override fun uploadScreenshot(file: File, uniqueId: String): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading())
        try {
            val body = MultipartBody.Part
                .createFormData(
                    "File",
                    file.name,
                    file.asRequestBody()
                )

            val uniqueIdBody =
                uniqueId.toRequestBody()

            val result = apiDomain.uploadScreenshot(
                image = body,
                uniqueId = uniqueIdBody
            )

            logAndWrite(context, result.body() ?: "", "SupportMsgRepositoryImpl", "uploadScreenshot")

            emit(Resource.Success(Unit))
        } catch (e: HttpException) {
            emit(Resource.Error(message = HTTP_EXCEPTION_MESSAGE))
        } catch (e: SocketTimeoutException) {
            emit(Resource.Error(message = SOCKET_TIMEOUT_EXCEPTION_MESSAGE))
        } catch (e: IOException) {
            emit(Resource.Error(message = IO_EXCEPTION_MESSAGE))
        }
    }
}

