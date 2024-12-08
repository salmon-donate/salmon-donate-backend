package com.github.chekist32.payment

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.chekist32.VT
import com.github.chekist32.jooq.goipay.enums.InvoiceStatusType
import invoice.v1.InvoiceOuterClass
import invoice.v1.InvoiceOuterClass.Invoice
import io.quarkus.scheduler.Scheduled
import jakarta.annotation.PostConstruct
import jakarta.enterprise.context.ApplicationScoped
import jakarta.ws.rs.sse.Sse
import jakarta.ws.rs.sse.SseBroadcaster
import jakarta.ws.rs.sse.SseEventSink
import kotlinx.coroutines.*
import kotlinx.coroutines.future.await
import org.jboss.resteasy.reactive.server.jaxrs.SseBroadcasterImpl
import java.util.*
import java.util.concurrent.ConcurrentHashMap

@ApplicationScoped
class PaymentNotificationService(
    private val paymentService: PaymentService,
    private val paymentGrpcNotificationService: PaymentGrpcNotificationService,
    private val sse: Sse,
    private val objectMapper: ObjectMapper
) {
    private val sseConnections = ConcurrentHashMap<UUID, SseBroadcaster>()

    @OptIn(DelicateCoroutinesApi::class)
    @PostConstruct
    protected fun init() {
        GlobalScope.launch {
            paymentGrpcNotificationService.subscribeToInvoice(::onNewInvoice)
        }
    }

    private suspend fun onNewInvoice(invoice: Invoice) {
        val paymentId = UUID.fromString(invoice.id)

        val connections = sseConnections[paymentId] ?: return
        try {
            connections.broadcast(
                sse.newEvent(
                    PaymentNotificationName.PAYMENT_CHANGED_STATUS.sseName,
                    objectMapper.writeValueAsString(invoice.toInvoiceDTO())
                )
            ).await()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (invoice.status == InvoiceOuterClass.InvoiceStatusType.CONFIRMED || invoice.status == InvoiceOuterClass.InvoiceStatusType.EXPIRED)
            sseConnections.remove(paymentId)?.close(true)
    }

    @Scheduled(every = "30s")
    protected fun pingConnections() = runBlocking {
        sseConnections.map { (_, connections) ->
            launch(Dispatchers.VT) {
                try {
                    connections.broadcast(
                        sse.newEvent(
                            PaymentNotificationName.KEEPALIVE.sseName,
                            PaymentNotificationName.KEEPALIVE.sseName
                        )
                    ).await()
                } catch (e: Exception) {
                    // TODO: add id to it
                    e.printStackTrace()
                }
            }
        }.joinAll()
    }

    fun addNewPaymentStatusSee(paymentId: UUID, sink: SseEventSink) {
        val invoice = try {
            paymentService.getPaymentByPaymentId(paymentId)
        } catch (e: Exception) {
            if (!sink.isClosed) sink.close()
            throw e
        }

        if (invoice.status == InvoiceStatusType.CONFIRMED || invoice.status == InvoiceStatusType.EXPIRED) {
            runBlocking {
                sink.use {
                    try {
                        it.send(
                            sse.newEvent(
                                PaymentNotificationName.PAYMENT_CHANGED_STATUS.sseName,
                                objectMapper.writeValueAsString(invoice.toInvoiceDTO())
                            )
                        ).await()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
            return
        }

        sseConnections.getOrPut(paymentId, { SseBroadcasterImpl() }).register(sink)
    }

    enum class PaymentNotificationName(val sseName: String) {
        KEEPALIVE("keepalive"),
        PAYMENT_CHANGED_STATUS("payment-changed-status")
    }
}