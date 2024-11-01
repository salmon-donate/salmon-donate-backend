package com.github.chekist32.webhook

import com.github.chekist32.withTransactionScope
import jakarta.inject.Named
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody
import org.jooq.DSLContext

@Path("/api/v1/webhook")
class WebHookResource(
    private val webHookHandler: KeycloakWebHookHandler,
    @Named("dsl-sd")
    private val dslSD: DSLContext
) {
    @POST
    @Path("/keycloak_event_webhook")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    suspend fun handleKeycloakEvent(@RequestBody req: KeycloakEventRequest): Response = withTransactionScope(dslSD) {
        webHookHandler.handleKeycloakWebHookEvent(req)
        return@withTransactionScope Response.ok().build()
    }
}