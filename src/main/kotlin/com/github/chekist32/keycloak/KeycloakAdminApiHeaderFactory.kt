package com.github.chekist32.keycloak

import io.quarkus.rest.client.reactive.ReactiveClientHeadersFactory
import io.smallrye.mutiny.Uni
import jakarta.enterprise.context.ApplicationScoped
import jakarta.ws.rs.core.MultivaluedMap

@ApplicationScoped
class KeycloakAdminApiHeaderFactory(
    private val keycloakAdminTokenService: KeycloakAdminTokenService
) : ReactiveClientHeadersFactory() {

    override fun getHeaders(
        incomingHeaders: MultivaluedMap<String, String>,
        clientOutgoingHeaders: MultivaluedMap<String, String>
    ): Uni<MultivaluedMap<String, String>> {
        return Uni.createFrom().item {
            clientOutgoingHeaders.add("Authorization", "Bearer ${keycloakAdminTokenService.getToken()}")
            return@item clientOutgoingHeaders
        }
    }
}