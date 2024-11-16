package com.github.chekist32.keycloak

import io.quarkus.rest.client.reactive.ReactiveClientHeadersFactory
import io.smallrye.mutiny.Uni
import jakarta.enterprise.context.ApplicationScoped
import jakarta.ws.rs.core.MultivaluedMap
import org.eclipse.microprofile.rest.client.ext.ClientHeadersFactory

@ApplicationScoped
class KeycloakAdminApiHeaderFactory(
    private val keycloakAdminTokenService: KeycloakAdminTokenService
) : ClientHeadersFactory {

    override fun update(
        incomingHeaders: MultivaluedMap<String, String>,
        clientOutgoingHeaders: MultivaluedMap<String, String>
    ): MultivaluedMap<String, String> {
        clientOutgoingHeaders.add("Authorization", "Bearer ${keycloakAdminTokenService.getToken()}")
        return clientOutgoingHeaders
    }
}