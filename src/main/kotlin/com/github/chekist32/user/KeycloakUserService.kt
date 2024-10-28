package com.github.chekist32.user

import com.github.chekist32.base.config.KeycloakAdminConfig
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import jakarta.enterprise.context.ApplicationScoped
import jakarta.ws.rs.NotFoundException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.keycloak.admin.client.Keycloak
import org.keycloak.admin.client.KeycloakBuilder
import org.keycloak.representations.idm.UserRepresentation


@ApplicationScoped
class KeycloakUserService(
    private val keycloakAdminConfig: KeycloakAdminConfig
) {
    private lateinit var keycloak: Keycloak

    @PostConstruct
    protected fun initKeycloak() {
        keycloak = KeycloakBuilder.builder()
            .serverUrl(keycloakAdminConfig.serverUrl())
            .realm(keycloakAdminConfig.realm())
            .clientId(keycloakAdminConfig.clientId())
            .clientSecret(keycloakAdminConfig.clientSecret())
            .grantType(keycloakAdminConfig.grantType())
            .username(keycloakAdminConfig.username())
            .password(keycloakAdminConfig.password())
            .build()
    }

    @PreDestroy
    protected fun closeKeycloak() {
        keycloak.close()
    }

    suspend fun getUserByUsername(username: String): UserRepresentation = withContext(Dispatchers.IO) {
        val userResource = keycloak.realm(keycloakAdminConfig.realm()).users()

        val userList = userResource.search(username,true)
        return@withContext userList.firstOrNull() ?: throw NotFoundException("There is no user with username: $username")
    }

    suspend fun getUserById(userId: String): UserRepresentation? = withContext(Dispatchers.IO) {
        val userResource = keycloak.realm(keycloakAdminConfig.realm()).users()

        return@withContext userResource.get(userId).toRepresentation()
    }

    suspend fun updateUser(userRepresentation: UserRepresentation) = withContext(Dispatchers.IO) {
        val userResource = keycloak.realm(keycloakAdminConfig.realm()).users()

        userResource.get(userRepresentation.id).update(userRepresentation)
    }
}