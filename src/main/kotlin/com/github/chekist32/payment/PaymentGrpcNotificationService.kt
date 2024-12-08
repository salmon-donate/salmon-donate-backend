package com.github.chekist32.payment

import com.github.chekist32.VT
import invoice.v1.InvoiceOuterClass
import invoice.v1.InvoiceOuterClass.Invoice
import invoice.v1.InvoiceServiceGrpc
import io.quarkus.grpc.GrpcClient
import io.quarkus.runtime.Startup
import jakarta.enterprise.context.ApplicationScoped
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

@Startup
@ApplicationScoped
class PaymentGrpcNotificationService(
    @GrpcClient("goipay")
    private val paymentGoipayGrpcClient: InvoiceServiceGrpc.InvoiceServiceBlockingStub
) {
    private val mutableSharedFlow = MutableSharedFlow<InvoiceOuterClass.InvoiceStatusStreamResponse>()
    private val emitterCoroutineScope = CoroutineScope(Dispatchers.VT)
    private val subscriberCoroutineScope = CoroutineScope(Dispatchers.VT)


    init {
        val stream = paymentGoipayGrpcClient.invoiceStatusStream(InvoiceOuterClass.InvoiceStatusStreamRequest.getDefaultInstance()).asFlow()

        emitterCoroutineScope.launch {
            stream.collect { invoice -> mutableSharedFlow.emit(invoice) }
        }
    }

    fun subscribeToInvoice(onInvoice: suspend (Invoice) -> Unit) {
        subscriberCoroutineScope.launch {
            mutableSharedFlow.collect { res -> onInvoice(res.invoice) }
        }
    }

    fun subscribeToInvoiceByStatus(onInvoice: suspend (Invoice) -> Unit, status: InvoiceOuterClass.InvoiceStatusType) {
        subscriberCoroutineScope.launch {
            mutableSharedFlow
                .filter { res -> res.invoice.status == status }
                .collect { res -> onInvoice(res.invoice) }
        }
    }
}