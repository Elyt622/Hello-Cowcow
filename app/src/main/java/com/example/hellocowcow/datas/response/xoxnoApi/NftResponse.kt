package com.example.hellocowcow.datas.response.xoxnoApi

import com.google.gson.annotations.SerializedName


data class NftResponse (

    @SerializedName("identifier"           ) var identifier           : String?           = null,
    @SerializedName("collection"           ) var collection           : String?           = null,
    @SerializedName("timestamp"            ) var timestamp            : Int?              = null,
    @SerializedName("attributes"           ) var attributes           : String?           = null,
    @SerializedName("nonce"                ) var nonce                : Int?              = null,
    @SerializedName("type"                 ) var type                 : String?           = null,
    @SerializedName("name"                 ) var name                 : String?           = null,
    @SerializedName("creator"              ) var creator              : String?           = null,
    @SerializedName("royalties"            ) var royalties            : Int?              = null,
    @SerializedName("uris"                 ) var uris                 : ArrayList<String> = arrayListOf(),
    @SerializedName("url"                  ) var url                  : String?           = null,
    @SerializedName("isWhitelistedStorage" ) var isWhitelistedStorage : String?           = null,
    @SerializedName("thumbnailUrl"         ) var thumbnailUrl         : String?           = null,
    @SerializedName("tags"                 ) var tags                 : ArrayList<String> = arrayListOf(),
    @SerializedName("avifUrl"              ) var avifUrl              : String?           = null,
    @SerializedName("webpUrl"              ) var webpUrl              : String?           = null,
    @SerializedName("wasProcessed"         ) var wasProcessed         : Boolean?          = null,
    @SerializedName("id"                   ) var id                   : String?           = null,
    @SerializedName("rid"                  ) var rid                  : String?           = null,
    @SerializedName("onSale"               ) var onSale               : String?           = null,
    @SerializedName("lastPrice"            ) var lastPrice            : String?           = null,
    @SerializedName("lastToken"            ) var lastToken            : String?           = null,
    @SerializedName("hasOffers"            ) var hasOffers            : String?           = null,
    @SerializedName("retries"              ) var retries              : Int?              = null,
    @SerializedName("hasSecondNFT"         ) var hasSecondNFT         : Boolean?          = null,
    @SerializedName("metadata"             ) var metadata             : MetadataResponse? = MetadataResponse(),
    @SerializedName("originalMedia"        ) var originalMedia        : OriginalMediaResponse? = OriginalMediaResponse(),
    @SerializedName("saleInfoNft"          ) var saleInfoNft          : String?           = null,
    @SerializedName("offersInfo"           ) var offersInfo           : ArrayList<String> = arrayListOf(),
    @SerializedName("gameData"             ) var gameData             : ArrayList<String> = arrayListOf(),
    @SerializedName("owner"                ) var owner                : String?           = null,
    @SerializedName("ownerUsername"        ) var ownerUsername        : String?           = null,
    @SerializedName("isVerified"           ) var isVerified           : Boolean?          = null,
    @SerializedName("collectionName"       ) var collectionName       : String?           = null,
    @SerializedName("isVisible"            ) var isVisible            : Boolean?          = null,
    @SerializedName("nftValue"             ) var nftValue             : NftValueResponse? = NftValueResponse()

)