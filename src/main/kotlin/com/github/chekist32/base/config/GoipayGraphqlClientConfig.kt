package com.github.chekist32.base.config

import io.smallrye.config.ConfigMapping

@ConfigMapping(prefix = "quarkus.smallrye-graphql-client.GoipayGraphQLClient")
interface GoipayGraphqlClientConfig {
    fun url(): String
}