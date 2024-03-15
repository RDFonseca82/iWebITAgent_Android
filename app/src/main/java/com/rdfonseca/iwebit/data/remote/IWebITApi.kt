package com.rdfonseca.iwebit.data.remote

import com.rdfonseca.iwebit.data.remote.dto.CompanyInfoDto
import com.rdfonseca.iwebit.data.remote.dto.DeviceInfoDto
import retrofit2.Response
import retrofit2.http.*

interface IWebITApi {

    @GET("/scripts/script_api.php")
    suspend fun getSupportMessages(
        @Query("Support") support: String = "1",
        @Query("UniqueID") uniqueId: String,
    ): Response<String>

    @GET("/scripts/script_api.php")
    suspend fun getCompanyInfo(
        @Query("IdSync") idSync: String,
    ): CompanyInfoDto

    @GET("/scripts/script_api.php")
    suspend fun getDeviceInfo(
        @Query("UniqueID") uniqueId: String,
    ): DeviceInfoDto

    @GET("scripts/script_api.php?Uninstall=1")
    suspend fun getAppsToUninstall(
        @Query("Uninstall") uninstall: String = "1",
        @Query("UniqueID") uniqueId: String
    ): Response<String>

    @FormUrlEncoded
    @POST("/scripts/script_api_support.php")
    suspend fun postMessageSupport(
        @Query("UniqueID") uniqueId: String,
        @Field("Nome") nome: String,
        @Field("DeviceSupport") msg: String,
        @Field("DeviceSupportDate") data: String,
    ): Response<Unit>

//    @POST(value = "scripts/script_android.php")
//    suspend fun postNewDevice(
//        @Field("UniqueId") uniqueId: String?,        //
//        @Field("idsync") idSync: String?,        //
//        @Field("TypeSync") typeSync: Int?,        //
//        @Field("version") version: String?,        //
//        @Field("currentTime") currentTime: String,        //
//        @Field("hostname") hostName: String?,        //
//        @Field("board") board: String?,        //
//        @Field("###1") bootLoader: String?, //
//        @Field("brand") brand: String?,        //
//        @Field("device") device: String?,        //
//        @Field("display") display: String?,        //
//        @Field("###2") hardware: String?, //
//        @Field("idAndroid") idAndroid: String?,        //
//        @Field("manufacturer") manufacturer: String?,        //
//        @Field("model") model: String?,        //
//        @Field("###3") odmSku: String?,   //
//        @Field("###4") product: String?,   //
//        @Field("###5") sku: String?,   //
//        @Field("###6") socManufacturer: String?,   //
//        @Field("###7") socModel: String?,   //
//        @Field("###8") time: Long?,   //
//        @Field("###9") type: String?,   //
//        @Field("###10") user: String?,   //
//        @Field("baseOS") baseOS: String?,        //
//        @Field("codename") codename: String?,        //
//        @Field("###11") incremental: String?,        //
//        @Field("###12") mediaPerformanceClass: Int?,        //
//        @Field("###13") previewSdkInt: Int?,        //
//        @Field("release") release: String?,        //
//        @Field("###14") releaseOrCodename: String?,        //
//        @Field("###15") sdk: Int?,        //
//        @Field("###16") sdkInt: Int?,        //
//        @Field("securityPatch") securityPatch: String?,        //
//        @Field("###17") freeBytes: Long?,        //
//        @Field("###18") totalBytes: Long?,        //
//        @Field("###19") availMem: Long?,        //
//        @Field("###20") lowMemory: Boolean?,        //
//        @Field("###21") threshold: Long?,        //
//        @Field("###22") totalMem: Long?,        //
//        @Field("batteryCapacity") batteryCapacity: String?,        //
//        @Field("macAddress") macAddress: String?,        //
//        @Field("ipAddress") ipAddress: String?,        //
//        @Field("latitude") latitude: String?,
//        @Field("longitude") longitude: String?,
//        @Field("appName") appName: String?,
//        @Field("appInstallDate") appInstallDate: String?,
//    ): Response<String>

    @FormUrlEncoded
    @POST(value = "scripts/script_android.php")
    suspend fun postSync(
        @Field("json") jsonString: String
    ): Response<String>


}