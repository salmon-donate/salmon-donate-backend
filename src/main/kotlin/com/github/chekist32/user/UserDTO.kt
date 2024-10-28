package com.github.chekist32.user

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.github.chekist32.jooq.sd.enums.ConfirmationType
import com.github.chekist32.jooq.sd.enums.CurrencyType
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

data class XmrKeysData @JsonCreator constructor(
    @field:JsonProperty("keys")
    val keys: XmrKeys,
    @field:JsonProperty("enabled")
    val enabled: Boolean,
)

data class CryptoKeys @JsonCreator constructor(
    @field:JsonProperty("xmr")
    val xmr: XmrKeys
)

data class CryptoKeysData @JsonCreator constructor(
    @field:JsonProperty("xmr")
    val xmr: XmrKeysData
)
data class XmrKeys @JsonCreator constructor(
    @field:JsonProperty("priv")
    @Size(min = 64, max = 64)
    @Pattern(regexp = "^[0-9a-fA-F]+$", message = "Private key must be a valid hexadecimal string")
    val priv: String,
    @field:JsonProperty("pub")
    @Size(min = 64, max = 64)
    @Pattern(regexp = "^[0-9a-fA-F]+$", message = "Public key must be a valid hexadecimal string")
    val pub: String
)

data class UpdateXMRDataRequest(
    @field:JsonProperty("enabled")
    val enabled: Boolean,
    @field:JsonProperty("keys")
    val keys: XmrKeys
)

data class UpdateDonationProfileDataRequest(
    @field:JsonProperty("minAmount")
    val minAmount: Double,
    @field:JsonProperty("timeout")
    val timeout: Short,
    @field:JsonProperty("confirmationType")
    val confirmationType: ConfirmationType
)

data class DonationProfileDataResponse(
    @field:JsonProperty("minAmount")
    val minAmount: Double,
    @field:JsonProperty("minAmountCurrency")
    val minAmountCurrency: CurrencyType,
    @field:JsonProperty("timeout")
    val timeout: Short,
    @field:JsonProperty("confirmationType")
    val confirmationType: ConfirmationType,
    @field:JsonProperty("cryptoKeysData")
    val cryptoKeysData: CryptoKeysData
)

data class ProfileDataResponse(
    @field:JsonProperty("avatarUrl")
    val avatarUrl: String?,
    @field:JsonProperty("bio")
    val bio: String?
)

data class ProfileDataUpdateRequest(
    @field:JsonProperty("bio")
    @Size(max = 300, message = "The maximum length for the bio field is 300 characters")
    val bio: String?,
    @field:JsonProperty("firstName")
    @Size(max = 50, message = "The maximum length for the firstName field is 50 characters")
    val firstName: String,
    @field:JsonProperty("lastName")
    @Size(max = 50, message = "The maximum length for the lastName field is 50 characters")
    val lastName: String
)

data class TimeZone(
    @field:JsonProperty("name")
    val name: String,
    @field:JsonProperty("offset")
    val offset: Int
)

data class RegionalProfileDataRequest(
    @field:JsonProperty("timeZoneName")
    @NotBlank
    val timeZoneName: String,
    @field:JsonProperty("currency")
    val currency: CurrencyType
)

data class RegionalProfileDataResponse(
    @field:JsonProperty("timeZone")
    val timeZone: TimeZone,
    @field:JsonProperty("availableTimeZones")
    val availableTimeZones: List<TimeZone>,
    @field:JsonProperty("currency")
    val currency: CurrencyType
)
