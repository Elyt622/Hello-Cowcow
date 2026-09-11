package com.example.hellocowcow.data.retrofit.mvxGateway.response

import com.google.gson.annotations.SerializedName

data class ProcessStatusResponse(
  @SerializedName("data") val data: ProcessStatusData? = null,
  @SerializedName("error") val error: String = "",
  @SerializedName("code") val code: String = ""
)

data class ProcessStatusData(
  @SerializedName("status") val status: String = "unknown",
  @SerializedName("reason") val reason: String = ""
)
