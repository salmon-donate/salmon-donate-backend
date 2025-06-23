package com.github.chekist32.converter

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.QueryParam
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient
import java.time.OffsetDateTime


data class GetSimplePriceResponse(
    @field:JsonProperty("average_price")
    val averagePrice: Double,
    @field:JsonProperty("simple_price_exchanges")
    val simplePriceExchanges: List<SimplePriceExchanges>,
    @field:JsonProperty("last_updated")
    val lastUpdated: OffsetDateTime
)

data class SimplePriceExchanges(
    @field:JsonProperty("exchange")
    val exchange: String,
    @field:JsonProperty("price")
    val price: Double
)

@RegisterRestClient(baseUri = "https://rates.salmondonate.com/api/v1")
interface CustomApiClient {

    @GET
    @Path("/simple_price")
    fun getSimplePrice(
        @QueryParam("symbol") symbol: String? = null,
    ) : GetSimplePriceResponse
}