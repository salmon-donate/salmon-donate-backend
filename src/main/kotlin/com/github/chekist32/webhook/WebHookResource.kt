package com.github.chekist32.webhook

import jakarta.transaction.Transactional
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody

@Path("/api/v1/webhook")
class WebHookResource(
    private val webHookHandler: KeycloakWebHookHandler
) {
    @Transactional
    @POST
    @Path("/keycloak_event_webhook")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    fun handleKeycloakEvent(@RequestBody req: KeycloakEventRequest): Response {
        webHookHandler.handleKeycloakWebHookEvent(req)
        return Response.ok().build()
    }
}