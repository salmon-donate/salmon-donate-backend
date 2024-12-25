package com.github.chekist32.payment

import com.github.chekist32.converter.CryptoCurrencyConverter
import com.github.chekist32.jooq.goipay.tables.records.InvoicesRecord
import com.github.chekist32.jooq.goipay.tables.references.INVOICES
import com.github.chekist32.jooq.sd.enums.ConfirmationType
import com.github.chekist32.toCoinTypeJooq
import crypto.v1.Crypto
import invoice.v1.InvoiceOuterClass
import invoice.v1.InvoiceServiceGrpc
import io.quarkus.grpc.GrpcClient
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Named
import jakarta.ws.rs.NotFoundException
import org.jooq.DSLContext
import java.util.*

@ApplicationScoped
class PaymentService(
    @GrpcClient("goipay")
    private val paymentGoipayGrpcClient: InvoiceServiceGrpc.InvoiceServiceBlockingStub,
    private val converter: CryptoCurrencyConverter,
    @Named("dsl-goipay")
    private val dslGoipay: DSLContext
) {
    private fun confirmations(coin: Crypto.CoinType, confirmationType: ConfirmationType): Short {
        val invalidCoinEx = IllegalArgumentException("Invalid coin type")

        return when(confirmationType) {
            ConfirmationType.UNCONFIRMED -> 0
            ConfirmationType.PARTIALLY_CONFIRMED -> when(coin) {
                Crypto.CoinType.XMR -> 1
                Crypto.CoinType.BTC -> 1
                Crypto.CoinType.LTC -> 1
                Crypto.CoinType.ETH -> 1
                Crypto.CoinType.TON -> 1
                Crypto.CoinType.UNRECOGNIZED -> throw invalidCoinEx
            }
            ConfirmationType.CONFIRMED -> when(coin) {
                Crypto.CoinType.XMR -> 10
                Crypto.CoinType.BTC -> 3
                Crypto.CoinType.LTC -> 6
                Crypto.CoinType.ETH -> 10
                Crypto.CoinType.TON -> 10
                Crypto.CoinType.UNRECOGNIZED -> throw invalidCoinEx
            }
        }
    }

    fun createCryptoPayment(request: NewCryptoPaymentRequest): InvoiceToPayDTO {
        val invReq = InvoiceOuterClass.CreateInvoiceRequest
            .newBuilder()
            .setUserId(request.userId.toString())
            .setCoin(request.coin)
            .setAmount(converter.convertUsdToCrypto(request.amount, request.coin))
            .setConfirmations(confirmations(request.coin, request.confirmation).toInt())
            .setTimeout(request.timeout.toLong())
            .build()

        val res = paymentGoipayGrpcClient.createInvoice(invReq)

       return InvoiceToPayDTO(
           paymentId = res.paymentId,
           cryptoAddress = res.address,
           coin = invReq.coin.toCoinTypeJooq(),
           requiredAmount = invReq.amount,
           timeout = invReq.timeout,
           confirmationsRequired = invReq.confirmations
       )
    }

    fun getPaymentByPaymentId(paymentId: UUID): InvoicesRecord {
        val record = dslGoipay.select()
            .from(INVOICES)
            .where(INVOICES.ID.eq(paymentId))
            .fetchOne() ?: throw NotFoundException("No payment with such paymentId exists.")
        val paymentRecord = record.into(InvoicesRecord::class.java)

        return paymentRecord
    }
}