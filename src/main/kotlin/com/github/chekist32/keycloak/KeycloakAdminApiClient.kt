package com.github.chekist32.keycloak

import jakarta.ws.rs.*
import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient
import org.keycloak.representations.idm.UserRepresentation


@RegisterRestClient(configKey = "keycloak-admin-api")
@RegisterClientHeaders(KeycloakAdminApiHeaderFactory::class)
interface KeycloakAdminApiClient {
    @GET
    @Path("/admin/realms/{realm}/users")
    suspend fun getUsersByUsername(
        @PathParam("realm") realm: String,
        @QueryParam("username") username: String,
        @QueryParam("exact") exact: Boolean
    ): List<UserRepresentation>

    @GET
    @Path("/admin/realms/{realm}/users/{id}")
    suspend fun getUserById(
        @PathParam("realm") realm: String,
        @PathParam("id") userId: String
    ): UserRepresentation

    @PUT
    @Path("/admin/realms/{realm}/users/{id}")
    suspend fun updateUser(
        @PathParam("realm") realm: String,
        @PathParam("id") userId: String,
        userRepresentation: UserRepresentation
    )
}