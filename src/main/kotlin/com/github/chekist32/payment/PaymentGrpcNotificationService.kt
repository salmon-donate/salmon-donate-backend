package com.github.chekist32.payment

import com.github.chekist32.VT
import invoice.v1.InvoiceOuterClass
import invoice.v1.InvoiceOuterClass.Invoice
import invoice.v1.MutinyInvoiceServiceGrpc
import io.quarkus.grpc.GrpcClient
import io.quarkus.runtime.Startup
import io.smallrye.mutiny.coroutines.asFlow
import jakarta.enterprise.context.ApplicationScoped
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

@Startup
@ApplicationScoped
class PaymentGrpcNotificationService(
    @GrpcClient("goipay")
    private val paymentGoipayGrpcClient: MutinyInvoiceServiceGrpc.MutinyInvoiceServiceStub
) {
    private val mutableSharedFlow = MutableSharedFlow<InvoiceOuterClass.InvoiceStatusStreamResponse>()
    private val defaultCoroutineScope = CoroutineScope(Dispatchers.VT)

    init {
        val stream = paymentGoipayGrpcClient.invoiceStatusStream(InvoiceOuterClass.InvoiceStatusStreamRequest.getDefaultInstance()).asFlow()

        defaultCoroutineScope.launch {
            stream.collect { invoice -> mutableSharedFlow.emit(invoice) }
        }
    }

    suspend fun subscribeToInvoice(onInvoice: suspend (Invoice) -> Unit) {
        mutableSharedFlow.collect { res -> onInvoice(res.invoice) }
    }

    suspend fun subscribeToInvoiceByStatus(onInvoice: suspend (Invoice) -> Unit, status: InvoiceOuterClass.InvoiceStatusType) {
        mutableSharedFlow
            .filter { res -> res.invoice.status == status }
            .collect { res -> onInvoice(res.invoice) }
    }
}