package com.github.chekist32.stats

import com.fasterxml.jackson.annotation.JsonProperty
import com.github.chekist32.donation.DonationDTOForReceiver
import com.github.chekist32.jooq.sd.enums.CurrencyType

enum class StatsPeriod {
    MONTH,
    ALL_TIME
}

data class DonationStatsPeriodResponse(
    @field:JsonProperty("donationCount")
    val donationCount: Int,
    @field:JsonProperty("amount")
    val amount: Double,
    @field:JsonProperty("avgAmount")
    val avgAmount: Double,
    @field:JsonProperty("top3DonationsByAmount")
    val top3DonationsByAmount: List<DonationDTOForReceiver>,
    @field:JsonProperty("period")
    val period: StatsPeriod,
    @field:JsonProperty("amountCurrency")
    val amountCurrency: CurrencyType,
)