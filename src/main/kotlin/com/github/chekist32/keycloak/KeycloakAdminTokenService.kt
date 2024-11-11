package com.github.chekist32.keycloak

import com.github.chekist32.base.config.KeycloakAdminConfig
import io.quarkus.scheduler.Scheduled
import jakarta.enterprise.context.ApplicationScoped
import jakarta.json.JsonObject
import jakarta.ws.rs.client.Client
import jakarta.ws.rs.client.ClientBuilder
import jakarta.ws.rs.client.Entity
import jakarta.ws.rs.core.Form
import jakarta.ws.rs.core.MediaType
import java.time.Instant
import java.util.*
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

@ApplicationScoped
class KeycloakAdminTokenService(
    private val keycloakAdminConfig: KeycloakAdminConfig
) {
    private val lock: ReentrantReadWriteLock = ReentrantReadWriteLock()

    private val client: Client = ClientBuilder.newClient()

    private var token: String = ""
    private var expiryTime: Instant = Instant.now()

    init {
        lock.write {
            val (token, expiryTime) =  fetchToken()
            this.token = token
            this.expiryTime = expiryTime
        }
    }

    @Scheduled(every = "10s")
    protected fun updateToken() {
        if (expiryTime.isAfter(Instant.now().minusSeconds(10))) return

        lock.write {
            val (token, expiryTime) =  fetchToken()
            this.token = token
            this.expiryTime = expiryTime
        }
    }

    private fun fetchToken(): Pair<String, Instant> {
        val form = Form()
            .param("grant_type", keycloakAdminConfig.grantType())
            .param("client_id", keycloakAdminConfig.clientId())
            .param("client_secret", keycloakAdminConfig.clientSecret())
        if (keycloakAdminConfig.grantType().lowercase(Locale.getDefault()) == "password") {
            form
                .param("username", keycloakAdminConfig.username())
                .param("password", keycloakAdminConfig.password())
        }

        val res = client.target("${keycloakAdminConfig.serverUrl()}/realms/${keycloakAdminConfig.realm()}/protocol/openid-connect/token")
                .request(MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.form(form))

        return res.use {
            if (res.status != 200) {
                throw RuntimeException("Failed to obtain access token from Keycloak: ${res.statusInfo.reasonPhrase}")
            }

            val json = res.readEntity(JsonObject::class.java)
            val token = json.getString("access_token")
            val expiresIn = json.getInt("expires_in")
            Pair(token, Instant.now().plusSeconds(expiresIn.toLong()))
        }
    }

    fun getToken(): String {
        return lock.read {
            return@read token
        }
    }
}