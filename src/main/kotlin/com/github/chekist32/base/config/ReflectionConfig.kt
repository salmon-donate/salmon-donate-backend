package com.github.chekist32.base.config

import io.quarkus.runtime.annotations.RegisterForReflection

@RegisterForReflection(
    targets = [
        com.github.chekist32.jooq.sd.tables.records.DonationsRecord::class,
        com.github.chekist32.jooq.sd.tables.records.UsersRecord::class,
        com.github.chekist32.jooq.sd.tables.records.TimeZonesRecord::class,
        com.github.chekist32.jooq.sd.tables.records.DonationProfileDataRecord::class,
        com.github.chekist32.jooq.goipay.tables.records.UsersRecord::class,
        com.github.chekist32.jooq.goipay.tables.records.CryptoAddressesRecord::class,
        com.github.chekist32.jooq.goipay.tables.records.CryptoDataRecord::class,
        com.github.chekist32.jooq.goipay.tables.records.InvoicesRecord::class,
        com.github.chekist32.jooq.goipay.tables.records.XmrCryptoDataRecord::class,
    ]
)
class JooqReflectionConfig


@RegisterForReflection(
    targets = [
        jakarta.ws.rs.sse.SseEventSink::class,
        org.jboss.resteasy.reactive.server.jaxrs.SseEventSinkImpl::class,
        jakarta.ws.rs.sse.Sse::class,
        org.jboss.resteasy.reactive.server.jaxrs.SseImpl::class,
        jakarta.ws.rs.sse.SseBroadcaster::class,
        org.jboss.resteasy.reactive.server.jaxrs.SseBroadcasterImpl::class,
    ]
)
class SseReflectionConfig