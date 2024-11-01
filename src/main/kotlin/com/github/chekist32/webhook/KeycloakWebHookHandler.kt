package com.github.chekist32.webhook

import com.github.chekist32.user.UserService
import io.quarkus.grpc.GrpcClient
import io.smallrye.mutiny.coroutines.awaitSuspending
import jakarta.enterprise.context.ApplicationScoped
import jakarta.ws.rs.BadRequestException
import user.v1.MutinyUserServiceGrpc
import user.v1.User

@ApplicationScoped
class KeycloakWebHookHandler(
    private val userService: UserService,
    @GrpcClient("goipay")
    private val userGoipayGrpcClient: MutinyUserServiceGrpc.MutinyUserServiceStub
) {
    suspend fun handleKeycloakWebHookEvent(eventRequest: KeycloakEventRequest) {
        when (eventRequest.type) {
            KeycloakEventType.REGISTER -> handleUserRegistrationEvent(eventRequest)
            else -> { }
        }
    }

    private suspend fun handleUserRegistrationEvent(eventRequest: KeycloakEventRequest) {
        val userId = eventRequest.userId ?: throw BadRequestException("user_id can't be null")
        userGoipayGrpcClient.registerUser(User.RegisterUserRequest.newBuilder().setUserId(userId.toString()).build()).awaitSuspending()
        userService.registerUser(userId)
    }
}