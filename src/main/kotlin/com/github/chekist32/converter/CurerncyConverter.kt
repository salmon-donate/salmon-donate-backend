package com.github.chekist32.converter

import com.github.chekist32.jooq.goipay.enums.CoinType
import crypto.v1.Crypto
import io.quarkus.scheduler.Scheduled
import jakarta.enterprise.context.ApplicationScoped
import org.eclipse.microprofile.rest.client.inject.RestClient
import java.util.*
import java.util.concurrent.ConcurrentHashMap

@ApplicationScoped
class CryptoCurrencyConverter(
    @RestClient
    private val coincapClient: CoincapApiClient
) {
    private val usdRates = ConcurrentHashMap<String, Double>()

    @Scheduled(every = "30s")
    protected fun refreshRates() {
        try {
            val res = coincapClient.getAssets(ids = "monero")
            if (res.data == null) return

            for (coin in res.data) usdRates[coin.symbol.uppercase(Locale.ENGLISH)] = coin.priceUsd
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    fun convertUsdToCrypto(amount: Double, coinType: Crypto.CoinType): Double {
        val rate = usdRates[coinType.name] ?: throw IllegalStateException("Empty USD currency rate value for ${coinType.name}")

        return amount / rate
    }

    fun convertCryptoToUsd(amount: Double, coinType: CoinType): Double {
        val rate = usdRates[coinType.name] ?: throw IllegalStateException("Empty USD currency rate value for ${coinType.name}")

        return amount * rate
    }
}