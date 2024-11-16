package com.github.chekist32.stats

import com.github.chekist32.parseUserIdOrThrowBadRequest
import io.quarkus.security.Authenticated
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.Context
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import org.eclipse.microprofile.jwt.JsonWebToken
import org.eclipse.microprofile.openapi.annotations.Operation
import org.eclipse.microprofile.openapi.annotations.media.Content
import org.eclipse.microprofile.openapi.annotations.media.Schema
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses

@Path("/api/v1/stats")
class StatsResource(
    private val statsService: StatsService
) {
    @Authenticated
    @GET
    @Path("/donation")
    @Operation(
        summary = "Retrieve donation statistics by period",
        description = "Fetches donation statistics for the authenticated user based on the specified period."
    )
    @APIResponses(
        value = [
            APIResponse(
                responseCode = "200",
                description = "Notification token retrieved successfully",
                content = arrayOf(
                    Content(
                        mediaType = MediaType.APPLICATION_JSON,
                        schema = Schema(implementation = DonationStatsPeriodResponse::class)
                    )
                )),
            APIResponse(responseCode = "401", description = "Unauthorized access"),
            APIResponse(responseCode = "500", description = "Internal server error")
        ]
    )
    fun getDonationStatsByPeriod(@QueryParam("period") period: StatsPeriod = StatsPeriod.MONTH, @Context token: JsonWebToken): Response {
        val userId = parseUserIdOrThrowBadRequest(token.subject)

        return Response.ok().entity(statsService.getDonationStatsByUserIdAndPeriod(userId, period)).build()
    }
}