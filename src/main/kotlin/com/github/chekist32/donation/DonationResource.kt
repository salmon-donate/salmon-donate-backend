package com.github.chekist32.donation

import com.github.chekist32.parseUserIdOrThrowBadRequest
import com.github.chekist32.payment.InvoiceToPayDTO
import com.github.chekist32.user.UserService
import com.github.chekist32.withTransactionScope
import io.quarkus.runtime.annotations.RegisterForReflection
import io.quarkus.security.Authenticated
import jakarta.inject.Named
import jakarta.validation.Valid
import jakarta.ws.rs.*
import jakarta.ws.rs.core.Context
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.sse.SseEventSink
import org.eclipse.microprofile.jwt.JsonWebToken
import org.eclipse.microprofile.openapi.annotations.Operation
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType
import org.eclipse.microprofile.openapi.annotations.media.Content
import org.eclipse.microprofile.openapi.annotations.media.Schema
import org.eclipse.microprofile.openapi.annotations.media.SchemaProperty
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses
import org.jboss.resteasy.reactive.RestStreamElementType
import org.jooq.DSLContext
import java.util.*

@Path("/api/v1/donation")
@RegisterForReflection
class DonationResource(
    private val donationService: DonationService,
    private val donationNotificationService: DonationNotificationService,
    private val userService: UserService,
    @Named("dsl-sd")
    private val dslSD: DSLContext
) {

    @Authenticated
    @GET
    @Path("/donations")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Retrieve paginated donations for the authenticated user",
        description = "Fetches a list of donations with pagination for the authenticated user.",
    )
    @APIResponses(
        value = [
            APIResponse(responseCode = "200", description = "Donations retrieved successfully",
                content = [Content(schema = Schema(implementation = DonationDTOForReceiverResponse::class))]),
            APIResponse(responseCode = "404", description = "User not found"),
            APIResponse(responseCode = "500", description = "Server error")
        ]
    )
    suspend fun getDonationsPageable(@QueryParam("page") page: Int = 0, @QueryParam("limit") limit: Int = 20, @Context token: JsonWebToken): Response {
        val userId = parseUserIdOrThrowBadRequest(token.subject)

        when {
            page <= 0 -> throw BadRequestException("Page must be greater than 0.")
            limit !in 1..99 -> throw BadRequestException("Limit must be between 1 and 99.")
        }

        return Response.ok().entity(donationService.getDonationsPageableByUserId(
            userId = userId,
            page = page-1,
            limit = limit
        )).build()
    }

    @GET
    @Path("/donate/{username}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Get donation data by username", description = "Fetches the donation data for a specific user.")
    @APIResponses(
        value = [
            APIResponse(responseCode = "200", description = "Donation data retrieved successfully",
                content = [Content(schema = Schema(implementation = DonationData::class))]),
            APIResponse(responseCode = "404", description = "User not found"),
            APIResponse(responseCode = "500", description = "Server error")
        ]
    )
    suspend fun donateGet(@PathParam("username") username: String): Response {
        val donationData = userService.getDonationDataByUsername(username)

        return Response.status(Response.Status.OK).entity(donationData).build()
    }

    @POST
    @Path("/donate/{username}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "Make a donation", description = "Processes a donation for the specified user.")
    @APIResponses(
        value = [
            APIResponse(responseCode = "201", description = "Donation successfully processed",
                content = [Content(schema = Schema(implementation = InvoiceToPayDTO::class))]),
            APIResponse(responseCode = "400", description = "Invalid donation request"),
            APIResponse(responseCode = "500", description = "Internal server error")
        ]
    )
    suspend fun donatePost(@PathParam("username") username: String, @Valid @RequestBody req: DonationRequest): Response = withTransactionScope(dslSD) {
        val invoice = donationService.handleDonation(username, req)

        return@withTransactionScope Response.status(Response.Status.CREATED).entity(invoice).build()
    }

    @GET
    @Path("/notification")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    @RestStreamElementType(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Subscribe to donation notifications via Server-Sent Events (SSE)",
        description = "This endpoint provides real-time donation notifications for the authenticated user via SSE."
    )
    @APIResponses(
        value = [
            APIResponse(responseCode = "200", description = "SSE subscription established, real-time notifications will be streamed.",
                content = [Content(
                    mediaType = MediaType.SERVER_SENT_EVENTS,
                    schema = Schema(
                        type = SchemaType.OBJECT,
                        properties = [
                            SchemaProperty(name = "id", type = SchemaType.STRING, description = "Unique event identifier"),
                            SchemaProperty(name = "name", type = SchemaType.STRING, description = "Name of the event (e.g., 'keepalive', 'new-donation')", enumeration = ["keepalive", "new-donation"]),
                            SchemaProperty(name = "comment", type = SchemaType.STRING, description = "Optional comment for the event"),
                            SchemaProperty(name = "data", type = SchemaType.STRING, description = "Actual data of the event", oneOf = arrayOf(DonationDTO::class, Unit::class))
                        ]
                    )
                )]
            ),
            APIResponse(responseCode = "401", description = "Authentication failed"),
            APIResponse(responseCode = "500", description = "Server error while establishing SSE connection")
        ]
    )
    suspend fun donateSseGet(@Context sink: SseEventSink, @QueryParam("token") notificationToken: UUID): Response {
        donationNotificationService.registerDonationSse(userService.getUserIdByNotificationToken(notificationToken), sink)

        return Response.status(Response.Status.OK).build()
    }

    @Authenticated
    @POST
    @Path("/notification/test")
    @Operation(
        summary = "Send a test donation",
        description = "This endpoint allows sending a test donation."
    )
    @APIResponses(
        value = [
            APIResponse(responseCode = "200", description = "Test donation sent successfully."),
            APIResponse(responseCode = "400", description = "Invalid user ID."),
            APIResponse(responseCode = "401", description = "Unauthorized access.")
        ]
    )
    suspend fun sendTestDonation(@Context token: JsonWebToken): Response {
        val userId = parseUserIdOrThrowBadRequest(token.subject)
        donationNotificationService.sendTestDonation(userId)
        return Response.status(Response.Status.OK).build()
    }
}