package com.github.chekist32.stats

import com.github.chekist32.VT
import com.github.chekist32.base.dto.Quadruple
import com.github.chekist32.converter.CryptoCurrencyConverter
import com.github.chekist32.donation.DonationDTOForReceiver
import com.github.chekist32.jooq.goipay.tables.references.INVOICES
import com.github.chekist32.jooq.sd.enums.CurrencyType
import com.github.chekist32.jooq.sd.tables.references.DONATIONS
import com.github.chekist32.jooq.sd.tables.references.TIME_ZONES
import com.github.chekist32.jooq.sd.tables.references.USERS
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Named
import jakarta.ws.rs.InternalServerErrorException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jooq.DSLContext
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.temporal.TemporalAdjusters
import java.util.*

@ApplicationScoped
class StatsService(
    @Named("dsl-sd")
    private val dslSD: DSLContext,
    @Named("dsl-goipay")
    private val dslGoipay: DSLContext,
    private val currencyConverter: CryptoCurrencyConverter
) {
    private fun getTimeRangeByPeriod(period: StatsPeriod): Pair<OffsetDateTime, OffsetDateTime> {
        return when (period) {
            StatsPeriod.MONTH -> {
                Pair(
                    OffsetDateTime.now(ZoneOffset.UTC).withDayOfMonth(1).with(LocalTime.MIN),
                    OffsetDateTime.now(ZoneOffset.UTC).with(TemporalAdjusters.lastDayOfMonth()).with(LocalTime.MAX)
                )
            }
            StatsPeriod.ALL_TIME -> {
                Pair(
                    OffsetDateTime.of(1, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC),
                    OffsetDateTime.now(ZoneOffset.UTC)
                )
            }
        }
    }

    suspend fun getDonationStatsByUserIdAndPeriod(userId: UUID, period: StatsPeriod) = withContext(Dispatchers.VT) {
        val (start, end) = getTimeRangeByPeriod(period)
        val paymentIdsForPeriod = dslSD.select(DONATIONS.PAYMENT_ID)
            .from(DONATIONS)
            .where(DONATIONS.USER_ID.eq(userId).and(DONATIONS.SHOWN_AT.between(start, end)))
            .fetch()
            .mapNotNull { pi -> pi.value1() }

        val invoices = dslGoipay.select(INVOICES.ID, INVOICES.ACTUAL_AMOUNT, INVOICES.COIN)
            .from(INVOICES)
            .where(INVOICES.ID.`in`(paymentIdsForPeriod))
            .fetch()
            .map { r -> Triple(r.value1(), r.value2(), r.value3()) }
        val invoicesWithUsdPrice = invoices.map { (id, actualAmount, coin) ->
            if (actualAmount == null || coin == null) return@map Quadruple(id, actualAmount, coin, 0.0)
            val usdPrice = try {
                currencyConverter.convertCryptoToUsd(actualAmount, coin)
            } catch (e: Exception) { 0.0 }

            Quadruple(id, actualAmount, coin, usdPrice)
        }.sortedByDescending { it.fourth }

        val amount = invoicesWithUsdPrice.sumOf { it.fourth }

        val top3Invoices = invoicesWithUsdPrice.take(3)
        val top3Donations = dslSD.select(DONATIONS.FROM, DONATIONS.MESSAGE, DONATIONS.SHOWN_AT)
            .from(DONATIONS)
            .where(DONATIONS.PAYMENT_ID.`in`(top3Invoices.mapNotNull { i -> i.first }))
            .fetch()

        val (offset, currency) = dslSD.select(TIME_ZONES.UTC_OFFSET, USERS.CURRENCY)
            .from(USERS)
            .join(TIME_ZONES)
            .on(TIME_ZONES.NAME.eq(USERS.TIME_ZONE))
            .where(USERS.ID.eq(userId))
            .fetchOne() ?: throw InternalServerErrorException()
        if (offset == null || currency == null) throw InternalServerErrorException()

        val top3DonationsByAmount = top3Donations.zip(top3Invoices).map { (donation, invoice) ->
            val (from, message, shownAt) = donation
            val (_, actualAmount, coin) = invoice
            if (from == null || shownAt == null || actualAmount == null || coin == null) throw InternalServerErrorException()

            DonationDTOForReceiver(
                from = from,
                message = message,
                amount = actualAmount,
                coin = coin,
                createdAt = shownAt.atZoneSameInstant(ZoneOffset.UTC).plusMinutes(offset.totalMinutes.toLong())
            )
        }

        return@withContext DonationStatsPeriodResponse(
            donationCount = paymentIdsForPeriod.size,
            amount = amount,
            avgAmount = if (paymentIdsForPeriod.isNotEmpty()) amount / paymentIdsForPeriod.size else 0.0,
            top3DonationsByAmount = top3DonationsByAmount,
            period = period,
            amountCurrency = currency
        )
    }
}