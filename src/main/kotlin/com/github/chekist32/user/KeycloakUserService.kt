package com.github.chekist32.user

import com.github.chekist32.base.config.KeycloakAdminConfig
import com.github.chekist32.keycloak.KeycloakAdminApiClient
import jakarta.enterprise.context.ApplicationScoped
import jakarta.ws.rs.NotFoundException
import org.eclipse.microprofile.rest.client.inject.RestClient
import org.keycloak.representations.idm.UserRepresentation


@ApplicationScoped
class KeycloakUserService(
    private val keycloakAdminConfig: KeycloakAdminConfig,
    @RestClient
    private val keycloakAdminApiClient: KeycloakAdminApiClient
) {
    suspend fun getUserByUsername(username: String): UserRepresentation {
        val userList = keycloakAdminApiClient.getUsersByUsername(
            realm = keycloakAdminConfig.realm(),
            username = username,
            exact = true
        )
        return userList.firstOrNull() ?: throw NotFoundException("There is no user with username: $username")
    }

    suspend fun getUserById(userId: String): UserRepresentation? {
        return keycloakAdminApiClient.getUserById(
            realm = keycloakAdminConfig.realm(),
            userId = userId
        )
    }

    suspend fun updateUser(userRepresentation: UserRepresentation) {
        keycloakAdminApiClient.updateUser(
            realm = keycloakAdminConfig.realm(),
            userId = userRepresentation.id,
            userRepresentation = userRepresentation
        )
    }
}