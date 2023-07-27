package com.example.hellocowcow.data.response.xoxnoApi

import com.google.gson.annotations.SerializedName


data class CollectionResponse (

    @SerializedName("nftsWorth"           ) var nftsWorth           : Double?                        = null,
    @SerializedName("groupedByCollection" ) var groupedByCollection : ArrayList<GroupedByCollectionResponse> = arrayListOf()

)