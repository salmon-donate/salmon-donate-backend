package com.github.chekist32.webhook

import com.github.chekist32.getBodyWithoutModifying
import jakarta.ws.rs.container.ContainerRequestContext
import jakarta.ws.rs.core.Response
import org.eclipse.microprofile.config.inject.ConfigProperty
import org.jboss.resteasy.reactive.server.ServerRequestFilter
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec


class KeycloakWebhookFilter(
    @ConfigProperty(name = "keycloak-webhook.secret")
    private val apiKey: String?
) {
    private val uriMatcher = "/api/v1/webhook/keycloak_event_webhook"

    @OptIn(ExperimentalStdlibApi::class)
    private fun hmacSha256(secret: String, msg: ByteArray): String {
        val alg = "HmacSHA256"
        val mac = Mac.getInstance(alg)
        mac.init(SecretKeySpec(secret.toByteArray(), alg))
        return mac.doFinal(msg).toHexString()
    }

    @ServerRequestFilter
    suspend fun checkApiKey(ctx: ContainerRequestContext): Response?  {
        if (apiKey == null || ctx.uriInfo.path != uriMatcher) return null

        val reqApiKey = ctx.getHeaderString("X-Keycloak-Signature") ?: return Response.status(Response.Status.UNAUTHORIZED).build()
        val reqBody = ctx.getBodyWithoutModifying()

        if (reqApiKey != hmacSha256(apiKey, reqBody)) return Response.status(Response.Status.UNAUTHORIZED).build()

        return null
    }
}