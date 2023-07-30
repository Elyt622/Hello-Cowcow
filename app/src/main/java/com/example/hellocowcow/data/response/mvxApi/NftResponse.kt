package com.example.hellocowcow.data.response.mvxApi


import com.example.hellocowcow.domain.DomainModelConvertible
import com.example.hellocowcow.domain.models.DomainNft
import com.google.gson.annotations.SerializedName


data class NftResponse (

    @SerializedName("identifier"           ) var identifier           : String?           = null,
    @SerializedName("collection"           ) var collection           : String?           = null,
    @SerializedName("attributes"           ) var attributes           : String?           = null,
    @SerializedName("nonce"                ) var nonce                : Int?              = null,
    @SerializedName("type"                 ) var type                 : String?           = null,
    @SerializedName("name"                 ) var name                 : String?           = null,
    @SerializedName("creator"              ) var creator              : String?           = null,
    @SerializedName("royalties"            ) var royalties            : Double?              = null,
    @SerializedName("uris"                 ) var uris                 : ArrayList<String> = arrayListOf(),
    @SerializedName("url"                  ) var url                  : String?           = null,
    @SerializedName("media"                ) var media                : ArrayList<MediaResponse>  = arrayListOf(),
    @SerializedName("isWhitelistedStorage" ) var isWhitelistedStorage : Boolean?          = null,
    @SerializedName("metadata"             ) var metadata             : MetadataResponse?         = MetadataResponse(),
    @SerializedName("ticker"               ) var ticker               : String?           = null,
    @SerializedName("score"                ) var score                : Double?           = null,
    @SerializedName("rank"                 ) var rank                 : Int?              = null,
    @SerializedName("isNsfw"               ) var isNsfw               : Boolean?          = null,
    @SerializedName("assets"               ) var assets               : AssetsResponse?   = AssetsResponse()

) : DomainModelConvertible<DomainNft> {
    override fun toDomain()
    : DomainNft =
        DomainNft(
            identifier = identifier,
            collection = collection,
            attributes = attributes,
            name = name,
            creator = creator,
            royalties = royalties,
            uris = uris,
            url = url,
            ticker = ticker
        )

}