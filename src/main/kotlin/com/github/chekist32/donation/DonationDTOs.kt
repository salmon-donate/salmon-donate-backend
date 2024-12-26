package com.github.chekist32.donation

import com.fasterxml.jackson.annotation.JsonProperty
import com.github.chekist32.base.dto.Pageable
import com.github.chekist32.jooq.goipay.enums.CoinType
import com.github.chekist32.jooq.sd.enums.CryptoType
import io.quarkus.runtime.annotations.RegisterForReflection
import jakarta.validation.constraints.Size
import org.keycloak.representations.idm.UserRepresentation
import java.time.ZonedDateTime


data class DonationData(
    @field:JsonProperty("firstName")
    val firstName: String,
    @field:JsonProperty("lastName")
    val lastName: String,
    @field:JsonProperty("avatarUrl")
    val avatarUrl: String?,
    @field:JsonProperty("bio")
    val bio: String?,
    @field:JsonProperty("acceptedCrypto")
    val acceptedCrypto: Set<CryptoType>
)
fun UserRepresentation.toDonationData(avatarUrl: String?, bio: String?, acceptedCrypto: Set<CryptoType>): DonationData {
    return DonationData(
        firstName = this.firstName,
        lastName = this.lastName,
        acceptedCrypto = acceptedCrypto,
        avatarUrl = avatarUrl,
        bio = bio
    )
}

data class DonationRequest(
    @field:JsonProperty("from")
    @field:Size(min = 1, max = 50)
    val from: String,
    @field:JsonProperty("message")
    @field:Size(max = 300)
    val message: String?,
    @field:JsonProperty("coin")
    val coin: CryptoType
)

@RegisterForReflection
data class DonationDTO(
    @field:JsonProperty("from")
    val from: String,
    @field:JsonProperty("message")
    val message: String?,
    @field:JsonProperty("amount")
    val amount: Double,
    @field:JsonProperty("coin")
    val coin: CoinType
)

data class DonationDTOForReceiver(
    @field:JsonProperty("from")
    val from: String,
    @field:JsonProperty("message")
    val message: String?,
    @field:JsonProperty("amount")
    val amount: Double,
    @field:JsonProperty("coin")
    val coin: CoinType?,
    @field:JsonProperty("createdAt")
    val createdAt: ZonedDateTime
)

data class DonationDTOForReceiverResponse(
    @field:JsonProperty("page")
    override val page: Int,
    @field:JsonProperty("limit")
    override val limit: Int,
    @field:JsonProperty("totalCount")
    override val totalCount: Int,
    @field:JsonProperty("data")
    override val data: Collection<DonationDTOForReceiver>
) : Pageable<DonationDTOForReceiver>
