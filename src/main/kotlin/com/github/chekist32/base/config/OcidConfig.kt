package com.github.chekist32.base.config

import io.smallrye.config.ConfigMapping

@ConfigMapping(prefix = "quarkus.oidc")
interface OidcConfig {
    fun authServerUrl(): String
    fun clientId(): String
    fun credentials(): Credentials

    interface Credentials {
        fun secret(): String
    }
}

fun OidcConfig.realm(): String {
    return this.authServerUrl().split("/realms/").getOrNull(1)?.split("?")?.get(0) ?: ""
}