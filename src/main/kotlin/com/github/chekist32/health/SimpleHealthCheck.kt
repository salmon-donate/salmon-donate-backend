package com.github.chekist32.health

import io.smallrye.health.api.AsyncHealthCheck
import io.smallrye.mutiny.Uni
import jakarta.enterprise.context.ApplicationScoped
import org.eclipse.microprofile.health.HealthCheckResponse
import org.eclipse.microprofile.health.Liveness

@Liveness
@ApplicationScoped
class SimpleHealthCheck : AsyncHealthCheck {
    override fun call(): Uni<HealthCheckResponse> {
        return Uni.createFrom().item(HealthCheckResponse.up("CHECKED"))
    }
}