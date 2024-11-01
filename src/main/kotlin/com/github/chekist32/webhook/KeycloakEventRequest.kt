package com.github.chekist32.webhook

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import java.util.*

enum class KeycloakEventType {
    AUTHREQID_TO_TOKEN,
    AUTHREQID_TO_TOKEN_ERROR,
    CLIENT_DELETE,
    CLIENT_DELETE_ERROR,
    CLIENT_INFO,
    CLIENT_INFO_ERROR,
    CLIENT_INITIATED_ACCOUNT_LINKING,
    CLIENT_INITIATED_ACCOUNT_LINKING_ERROR,
    CLIENT_LOGIN,
    CLIENT_LOGIN_ERROR,
    CLIENT_REGISTER,
    CLIENT_REGISTER_ERROR,
    CLIENT_UPDATE,
    CLIENT_UPDATE_ERROR,
    CODE_TO_TOKEN,
    CODE_TO_TOKEN_ERROR,
    CUSTOM_REQUIRED_ACTION,
    CUSTOM_REQUIRED_ACTION_ERROR,
    DELETE_ACCOUNT,
    DELETE_ACCOUNT_ERROR,
    EXECUTE_ACTION_TOKEN,
    EXECUTE_ACTION_TOKEN_ERROR,
    EXECUTE_ACTIONS,
    EXECUTE_ACTIONS_ERROR,
    FEDERATED_IDENTITY_LINK,
    FEDERATED_IDENTITY_LINK_ERROR,
    GRANT_CONSENT,
    GRANT_CONSENT_ERROR,
    IDENTITY_PROVIDER_FIRST_LOGIN,
    IDENTITY_PROVIDER_FIRST_LOGIN_ERROR,
    IDENTITY_PROVIDER_LINK_ACCOUNT,
    IDENTITY_PROVIDER_LINK_ACCOUNT_ERROR,
    IDENTITY_PROVIDER_LOGIN,
    IDENTITY_PROVIDER_LOGIN_ERROR,
    IDENTITY_PROVIDER_POST_LOGIN,
    IDENTITY_PROVIDER_POST_LOGIN_ERROR,
    IDENTITY_PROVIDER_RESPONSE,
    IDENTITY_PROVIDER_RESPONSE_ERROR,
    IDENTITY_PROVIDER_RETRIEVE_TOKEN,
    IDENTITY_PROVIDER_RETRIEVE_TOKEN_ERROR,
    IMPERSONATE,
    IMPERSONATE_ERROR,
    INTROSPECT_TOKEN,
    INTROSPECT_TOKEN_ERROR,
    INVALID_SIGNATURE,
    INVALID_SIGNATURE_ERROR,
    LOGIN,
    LOGIN_ERROR,
    LOGOUT,
    LOGOUT_ERROR,
    OAUTH2_DEVICE_AUTH,
    OAUTH2_DEVICE_AUTH_ERROR,
    OAUTH2_DEVICE_CODE_TO_TOKEN,
    OAUTH2_DEVICE_CODE_TO_TOKEN_ERROR,
    OAUTH2_DEVICE_VERIFY_USER_CODE,
    OAUTH2_DEVICE_VERIFY_USER_CODE_ERROR,
    PERMISSION_TOKEN,
    PERMISSION_TOKEN_ERROR,
    PUSHED_AUTHORIZATION_REQUEST,
    PUSHED_AUTHORIZATION_REQUEST_ERROR,
    REFRESH_TOKEN,
    REFRESH_TOKEN_ERROR,
    REGISTER,
    REGISTER_ERROR,
    REGISTER_NODE,
    REGISTER_NODE_ERROR,
    REMOVE_FEDERATED_IDENTITY,
    REMOVE_FEDERATED_IDENTITY_ERROR,
    REMOVE_TOTP,
    REMOVE_TOTP_ERROR,
    RESET_PASSWORD,
    RESET_PASSWORD_ERROR,
    RESTART_AUTHENTICATION,
    RESTART_AUTHENTICATION_ERROR,
    REVOKE_GRANT,
    REVOKE_GRANT_ERROR,
    SEND_IDENTITY_PROVIDER_LINK,
    SEND_IDENTITY_PROVIDER_LINK_ERROR,
    SEND_RESET_PASSWORD,
    SEND_RESET_PASSWORD_ERROR,
    SEND_VERIFY_EMAIL,
    SEND_VERIFY_EMAIL_ERROR,
    TOKEN_EXCHANGE,
    TOKEN_EXCHANGE_ERROR,
    UNREGISTER_NODE,
    UNREGISTER_NODE_ERROR,
    UPDATE_CONSENT,
    UPDATE_CONSENT_ERROR,
    UPDATE_EMAIL,
    UPDATE_EMAIL_ERROR,
    UPDATE_PASSWORD,
    UPDATE_PASSWORD_ERROR,
    UPDATE_PROFILE,
    UPDATE_PROFILE_ERROR,
    UPDATE_TOTP,
    UPDATE_TOTP_ERROR,
    USER_INFO_REQUEST,
    USER_INFO_REQUEST_ERROR,
    VALIDATE_ACCESS_TOKEN,
    VALIDATE_ACCESS_TOKEN_ERROR,
    VERIFY_EMAIL,
    VERIFY_EMAIL_ERROR,
    VERIFY_PROFILE,
    VERIFY_PROFILE_ERROR;
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class KeycloakEventRequest(
    @field:JsonProperty(value = "time")
    val time: Long?,

    @field:JsonProperty(value = "type")
    val type: KeycloakEventType?,

    @field:JsonProperty(value = "realmId")
    val realmId: UUID?,

    @field:JsonProperty(value = "clientId")
    val clientId: String?,

    @field:JsonProperty(value = "userId")
    val userId: UUID?,

    @field:JsonProperty(value = "ipAddress")
    val ipAddress: String?,

    @field:JsonProperty(value = "error")
    val error: String?,

    @field:JsonProperty(value = "details")
    val details: HashMap<String, String>?
)
