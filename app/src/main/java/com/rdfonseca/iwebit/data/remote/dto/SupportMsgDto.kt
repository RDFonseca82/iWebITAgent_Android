package com.rdfonseca.iwebit.data.remote.dto

import androidx.annotation.Keep
import com.rdfonseca.iwebit.domain.model.SupportMsg

@Keep
data class SupportMsgDto(
    val DeviceSupport: String,
    val DeviceSupportClose: Int,
    val DeviceSupportCloseDate: String,
    val DeviceSupportCloseUser: Any,
    val DeviceSupportDate: String,
    val DeviceSupportDateWork: Any,
    val DeviceSupportTimeWork: Any,
    val IdCompany: String,
    val IdDevice: String,
    val IdDeviceSupport: String,
    val IdUser: Any,
    val IdUserSupport: Any,
    val Nome: String,
    val ReportTec: Any,
    val RespNote: Any,
    val SupportAddOn: Int,
    val SupportCanceled: Int,
    val SupportCanceledDate: Any,
    val SupportInternal: Int,
    val UniqueID: Any
) {
    fun toSupportMsg(): SupportMsg {
        return SupportMsg(
            header = Nome,
            message = DeviceSupport,
            date = DeviceSupportDate
        )
    }
}