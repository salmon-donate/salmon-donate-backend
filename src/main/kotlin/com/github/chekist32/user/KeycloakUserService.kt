package com.github.chekist32.user

import com.github.chekist32.base.config.KeycloakAdminConfig
import jakarta.enterprise.context.ApplicationScoped
import jakarta.ws.rs.NotFoundException
import org.keycloak.admin.client.Keycloak
import org.keycloak.representations.idm.UserRepresentation


@ApplicationScoped
class KeycloakUserService(
    private val keycloakAdminConfig: KeycloakAdminConfig,
    private val keycloakAdminClient: Keycloak
) {
    fun getUserByUsername(username: String): UserRepresentation {
        val userList = keycloakAdminClient.realm(keycloakAdminConfig.realm()).users().searchByUsername(username, true)
        return userList.firstOrNull() ?: throw NotFoundException("There is no user with username: $username")
    }

    fun getUserById(userId: String): UserRepresentation? {
       return keycloakAdminClient.realm(keycloakAdminConfig.realm()).users().get(userId).toRepresentation()
    }

    fun updateUser(userRepresentation: UserRepresentation) {
        keycloakAdminClient.realm(keycloakAdminConfig.realm()).users().get(userRepresentation.id).update(userRepresentation)
    }
}