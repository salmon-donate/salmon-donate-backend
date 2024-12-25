package com.github.chekist32.donation

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.chekist32.VT
import com.github.chekist32.jooq.goipay.enums.CoinType
import com.github.chekist32.jooq.sd.tables.references.DONATIONS
import com.github.chekist32.payment.PaymentGrpcNotificationService
import com.github.chekist32.toCoinTypeJooq
import invoice.v1.InvoiceOuterClass
import io.quarkus.scheduler.Scheduled
import jakarta.annotation.PostConstruct
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Named
import jakarta.ws.rs.sse.Sse
import jakarta.ws.rs.sse.SseBroadcaster
import jakarta.ws.rs.sse.SseEventSink
import kotlinx.coroutines.*
import kotlinx.coroutines.future.await
import org.jboss.resteasy.reactive.server.jaxrs.SseBroadcasterImpl
import org.jooq.DSLContext
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.*
import java.util.concurrent.ConcurrentHashMap

@ApplicationScoped
class DonationNotificationService(
    private val paymentNotificationService: PaymentGrpcNotificationService,
    @Named("dsl-sd")
    private val dslSD: DSLContext,
    private val sse: Sse,
    private val objectMapper: ObjectMapper
) {
    private val sseConnections = ConcurrentHashMap<UUID, SseBroadcaster>()

    @PostConstruct
    protected fun init() {
        paymentNotificationService.subscribeToInvoice({ it.status == InvoiceOuterClass.InvoiceStatusType.CONFIRMED }, ::onNewConfirmedInvoice)
    }

    private fun onNewConfirmedInvoice(invoice: InvoiceOuterClass.Invoice) {
        val invoiceId = UUID.fromString(invoice.id)

        val donationRec = dslSD
            .update(DONATIONS)
            .set(DONATIONS.SHOWN_AT, OffsetDateTime.now(ZoneOffset.UTC))
            .where(DONATIONS.PAYMENT_ID.eq(invoiceId))
            .returning(DONATIONS.FROM, DONATIONS.MESSAGE, DONATIONS.USER_ID)
            .fetchOne()
        val (from, message, userId) = donationRec?.let {
            Triple(it[DONATIONS.FROM], it[DONATIONS.MESSAGE], it[DONATIONS.USER_ID])
        } ?: return
        if (from == null || userId == null) return

        broadcastDonation(userId, DonationDTO(from, message, invoice.actualAmount, invoice.coin.toCoinTypeJooq()))
    }

    private fun broadcastDonation(userId: UUID, donation: DonationDTO) {
        val connections = sseConnections[userId] ?: return

        try {
            connections.broadcast(
                sse.newEvent(
                    DonationNotificationName.NEW_DONATION.sseName,
                    objectMapper.writeValueAsString(donation)
                )
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @Scheduled(every = "30s")
    protected fun pingConnections() = runBlocking {
        sseConnections.map { (id, connections) ->
            launch(Dispatchers.VT) {
                try {
                    connections.broadcast(
                        sse.newEvent(
                            DonationNotificationName.KEEPALIVE.sseName,
                            DonationNotificationName.KEEPALIVE.sseName
                        )
                    ).await()
                } catch (e: Exception) {
                    // TODO: add id to it
                    e.printStackTrace()
                }
            }
        }.joinAll()
    }

    fun sendTestDonation(userId: UUID) {
        broadcastDonation(userId, DonationDTO(
            from = "username",
            message = "Lorem ipsum dolor sit amet. Rem minima consequuntur 33 optio quia qui alias aliquid qui nemo provident? Qui voluptas fugit qui cumque similique non iure voluptas ad voluptates quam. Et rerum debitis id blanditiis quia in explicabo vero aut sint consequatur in maiores saepe sed vitae iure.",
            amount = 1.256,
            coin = CoinType.XMR
        ))
    }

    fun registerDonationSse(userId: UUID, sink: SseEventSink) {
        sseConnections.getOrPut(userId, {SseBroadcasterImpl()}).register(sink)
    }

    enum class DonationNotificationName(val sseName: String) {
        KEEPALIVE("keepalive"),
        NEW_DONATION("new-donation")
    }
}