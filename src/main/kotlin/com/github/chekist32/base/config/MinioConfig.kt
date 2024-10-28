package com.github.chekist32.base.config

import io.smallrye.config.ConfigMapping

@ConfigMapping(prefix = "quarkus.minio")
interface MinioConfig {
    fun url(): String
    fun port(): Int
}

fun MinioConfig.baseUrl(): String {
    return "${this.url()}:${port()}"
}