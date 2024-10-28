package com.github.chekist32.converter

import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.QueryParam
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient


data class CoinCapResponse<T>(
    val data: List<T>?,
    val timestamp: Long
)

data class CryptoData(
    val id: String,
    val rank: Int,
    val symbol: String,
    val name: String,
    val supply: Double,
    val maxSupply: Double,
    val marketCapUsd: Double,
    val volumeUsd24Hr: Double,
    val priceUsd: Double,
    val changePercent24Hr: Double,
    val vwap24Hr: Double,
    val explorer: String
)

@RegisterRestClient(baseUri = "https://api.coincap.io/v2")
interface CoincapApiClient {

    @GET
    @Path("/assets")
    fun getAssets(
        @QueryParam("search") search: String? = null,
        @QueryParam("ids") ids: String? = null,
        @QueryParam("limit") limit: Int? = 5,
        @QueryParam("offset") offset: Int? = 0
    ): CoinCapResponse<CryptoData>
}