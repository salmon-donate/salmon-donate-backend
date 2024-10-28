package com.github.chekist32.converter

import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.QueryParam
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient


data class GetTickerPriceResponse(
    val symbol: String,
    val price: Double
)

@RegisterRestClient(baseUri = "https://api.coingecko.com/api/v3")
interface BinanceApiClient {

    @GET
    @Path("/simple/price")
    fun getTickerPrice(
        @QueryParam("symbol") symbol: String? = null,
        @QueryParam("symbols") symbols: String? = null  // ["BTCUSDT","BNBUSDT"]
    ) : List<GetTickerPriceResponse>
}