package com.github.chekist32.payment

import com.fasterxml.jackson.annotation.JsonProperty
import com.github.chekist32.jooq.goipay.enums.CoinType
import com.github.chekist32.jooq.goipay.enums.InvoiceStatusType
import com.github.chekist32.jooq.sd.enums.ConfirmationType
import com.github.chekist32.jooq.sd.enums.CurrencyType
import com.github.chekist32.toCoinTypeJooq
import com.github.chekist32.toInvoiceStatusTypeJooq
import com.github.chekist32.toUTCLocalDateTime
import com.google.protobuf.Timestamp
import crypto.v1.Crypto
import invoice.v1.InvoiceOuterClass.Invoice
import io.quarkus.runtime.annotations.RegisterForReflection
import jakarta.validation.constraints.Positive
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*

data class InvoiceToPayDTO(
    @field:JsonProperty("paymentId")
    val paymentId: String,
    @field:JsonProperty("cryptoAddress")
    val cryptoAddress: String,
    @field:JsonProperty("coin")
    val coin: CoinType,
    @field:JsonProperty("requiredAmount")
    val requiredAmount: Double,
    @field:JsonProperty("timeout")
    val timeout: Long,
    @field:JsonProperty("confirmationsRequired")
    val confirmationsRequired: Int
)

@RegisterForReflection
data class InvoiceDTO(
    @field:JsonProperty("id")
    val id: String,
    @field:JsonProperty("cryptoAddress")
    val cryptoAddress: String,
    @field:JsonProperty("coin")
    val coin: CoinType,
    @field:JsonProperty("requiredAmount")
    val requiredAmount: Double,
    @field:JsonProperty("actualAmount")
    val actualAmount: Double?,
    @field:JsonProperty("confirmationsRequired")
    val confirmationsRequired: Int,
    @field:JsonProperty("createdAt")
    val createdAt: LocalDateTime,
    @field:JsonProperty("confirmedAt")
    val confirmedAt: LocalDateTime?,
    @field:JsonProperty("status")
    val status: InvoiceStatusType,
    @field:JsonProperty("expiresAt")
    val expiresAt: LocalDateTime,
    @field:JsonProperty("txId")
    val txId: String?
)

fun Invoice.toInvoiceDTO(): InvoiceDTO {
    return InvoiceDTO(
        id = this.id,
        cryptoAddress = this.cryptoAddress,
        coin = this.coin.toCoinTypeJooq(),
        requiredAmount = this.requiredAmount,
        actualAmount = if (this.actualAmount == 0.0) null else this.actualAmount,
        confirmationsRequired = this.confirmationsRequired,
        createdAt = this.createdAt.toUTCLocalDateTime(),
        confirmedAt = if (this.confirmedAt.equals(Timestamp.getDefaultInstance())) null else this.confirmedAt.toUTCLocalDateTime(),
        status = this.status.toInvoiceStatusTypeJooq(),
        expiresAt = this.expiresAt.toUTCLocalDateTime(),
        txId = this.txId
    )
}


fun Invoice.toInvoiceToPayDTO(): InvoiceToPayDTO {
    return InvoiceToPayDTO(
        paymentId = this.id,
        cryptoAddress = this.cryptoAddress,
        coin = this.coin.toCoinTypeJooq(),
        requiredAmount = this.requiredAmount,
        timeout = Duration.between(LocalDateTime.now(ZoneOffset.UTC), this.expiresAt.toUTCLocalDateTime()).toSeconds(),
        confirmationsRequired = this.confirmationsRequired
    )
}

data class NewCryptoPaymentRequest(
    val goipayUserId: UUID,
    @Positive
    val amount: Double,
    val currency: CurrencyType,
    val confirmation: ConfirmationType,
    @Positive
    val timeout: Int,
    val coin: Crypto.CoinType
)