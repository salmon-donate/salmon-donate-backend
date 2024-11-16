package com.github.chekist32.payment

import io.quarkus.runtime.annotations.RegisterForReflection
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.Context
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.sse.SseEventSink
import org.eclipse.microprofile.openapi.annotations.Operation
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType
import org.eclipse.microprofile.openapi.annotations.media.Content
import org.eclipse.microprofile.openapi.annotations.media.Schema
import org.eclipse.microprofile.openapi.annotations.media.SchemaProperty
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses
import org.jboss.resteasy.reactive.RestStreamElementType
import java.util.*


@RegisterForReflection
@Path("/api/v1/payment")
class PaymentResource(
    private val paymentService: PaymentService,
    private val paymentNotificationService: PaymentNotificationService
) {
    @GET
    @Path("/{paymentId}/donation")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Retrieve donation invoice by payment ID",
        description = "Fetches the details of a donation invoice associated with the given payment ID."
    )
    @APIResponses(
        value = [
            APIResponse(
                responseCode = "200",
                description = "Donation invoice retrieved successfully.",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = InvoiceToPayDTO::class)
                    )
                ]
            ),
            APIResponse(responseCode = "400", description = "Invalid payment ID"),
            APIResponse(responseCode = "404", description = "Not found"),
            APIResponse(responseCode = "500", description = "Server error")
        ]
    )
    fun getDonationInvoice(@PathParam("paymentId") paymentId: UUID): Response {
        val payment = paymentService.getPaymentByPaymentId(paymentId)

        return Response.ok().entity(payment.toInvoiceToPayDTO()).build()
    }


    @GET
    @Path("/{paymentId}/status")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    @RestStreamElementType(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Get payment status updates via SSE",
        description = "Subscribe to real-time payment status updates for a specific payment ID using Server-Sent Events (SSE)."
    )
    @APIResponses(
        value = [
            APIResponse(
                responseCode = "200",
                description = "SSE subscription established, real-time payment status updates will be streamed.",
                content = [Content(
                    mediaType = MediaType.SERVER_SENT_EVENTS,
                    schema = Schema(
                        type = SchemaType.OBJECT,
                        properties = [
                            SchemaProperty(name = "id", type = SchemaType.STRING, description = "Unique event identifier"),
                            SchemaProperty(name = "name",
                                type = SchemaType.STRING,
                                description = "Type of sse event",
                                enumeration = ["payment-changed-status", "keepalive"]
                            ),
                            SchemaProperty(name = "data", type = SchemaType.STRING, description = "Details of the payment status update", oneOf = [InvoiceDTO::class, Unit::class])
                        ]
                    )
                )]
            ),
            APIResponse(responseCode = "400", description = "Invalid payment ID"),
            APIResponse(responseCode = "404", description = "Not found"),
            APIResponse(responseCode = "500", description = "Server error while establishing SSE connection")
        ]
    )
    fun getPaymentStatusSse(@PathParam("paymentId") paymentId: UUID, @Context sink: SseEventSink) {
        paymentNotificationService.addNewPaymentStatusSee(paymentId, sink)
    }


}