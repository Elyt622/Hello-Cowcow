package com.example.hellocowcow.datas.response.xoxnoApi

import com.google.gson.annotations.SerializedName


data class NftValueResponse (

    @SerializedName("floorValue"   ) var floorValue   : Double? = null,
    @SerializedName("avgValue"     ) var avgValue     : Double? = null,
    @SerializedName("maxValue"     ) var maxValue     : Int?    = null,
    @SerializedName("collectionFp" ) var collectionFp : Double? = null

)