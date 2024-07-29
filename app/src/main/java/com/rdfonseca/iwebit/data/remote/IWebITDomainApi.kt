package com.rdfonseca.iwebit.data.remote

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

interface IWebITDomainApi {

    @Multipart
    @POST(value = "scripts/script_printscreen.php")
    suspend fun uploadScreenshot(
        @Part image: MultipartBody.Part,
        @Part("uniqueId") uniqueId: RequestBody
    ): Response<String>


}