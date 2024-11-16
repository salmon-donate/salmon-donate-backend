package com.github.chekist32.payment

import com.github.chekist32.converter.CryptoCurrencyConverter
import com.github.chekist32.jooq.sd.enums.ConfirmationType
import com.github.chekist32.toCoinTypeJooq
import crypto.v1.Crypto
import invoice.v1.InvoiceOuterClass
import invoice.v1.InvoiceServiceGrpc
import invoice.v1.MutinyInvoiceServiceGrpc
import io.quarkus.grpc.GrpcClient
import io.smallrye.mutiny.coroutines.awaitSuspending
import jakarta.enterprise.context.ApplicationScoped
import jakarta.ws.rs.NotFoundException
import java.util.*

@ApplicationScoped
class PaymentService(
    @GrpcClient("goipay")
    private val paymentGoipayGrpcClient: InvoiceServiceGrpc.InvoiceServiceBlockingStub,
    private val converter: CryptoCurrencyConverter
) {
    private fun confirmations(coin: Crypto.CoinType, confirmationType: ConfirmationType): UShort {
        val invalidCoinEx = IllegalArgumentException("Invalid coin type")

        return when(confirmationType) {
            ConfirmationType.UNCONFIRMED -> 0u
            ConfirmationType.PARTIALLY_CONFIRMED -> when(coin) {
                Crypto.CoinType.XMR -> 1u
                Crypto.CoinType.BTC -> 1u
                Crypto.CoinType.LTC -> 1u
                Crypto.CoinType.ETH -> 1u
                Crypto.CoinType.TON -> 1u
                Crypto.CoinType.UNRECOGNIZED -> throw invalidCoinEx
            }
            ConfirmationType.CONFIRMED -> when(coin) {
                Crypto.CoinType.XMR -> 10u
                Crypto.CoinType.BTC -> 3u
                Crypto.CoinType.LTC -> 6u
                Crypto.CoinType.ETH -> 10u
                Crypto.CoinType.TON -> 10u
                Crypto.CoinType.UNRECOGNIZED -> throw invalidCoinEx
            }
        }
    }

    fun createCryptoPayment(request: NewCryptoPaymentRequest): InvoiceToPayDTO {
        val invReq = InvoiceOuterClass.CreateInvoiceRequest
            .newBuilder()
            .setUserId(request.goipayUserId.toString())
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

    fun getPaymentByPaymentId(paymentId: UUID): InvoiceOuterClass.Invoice {
        val res = paymentGoipayGrpcClient.getInvoices(InvoiceOuterClass.GetInvoicesRequest.newBuilder().addPaymentIds(paymentId.toString()).build())
        if (res.invoicesList.isEmpty()) throw NotFoundException("No payment with such paymentId exists.")

        return res.invoicesList.first()
    }
}