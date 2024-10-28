package com.github.chekist32.base.config

import io.smallrye.config.ConfigMapping

@ConfigMapping(prefix = "quarkus.keycloak.admin-client")
interface KeycloakAdminConfig {
    fun serverUrl(): String
    fun realm(): String
    fun clientId(): String
    fun clientSecret(): String
    fun username(): String
    fun password(): String
    fun grantType(): String
}