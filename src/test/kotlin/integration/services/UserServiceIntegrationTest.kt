package integration.services

import com.github.chekist32.jooq.goipay.tables.references.CRYPTO_DATA
import com.github.chekist32.jooq.goipay.tables.references.XMR_CRYPTO_DATA
import com.github.chekist32.jooq.sd.enums.ConfirmationType
import com.github.chekist32.jooq.sd.enums.CryptoType
import com.github.chekist32.jooq.sd.enums.CurrencyType
import com.github.chekist32.jooq.sd.tables.references.DONATION_PROFILE_DATA
import com.github.chekist32.jooq.sd.tables.references.USERS
import com.github.chekist32.user.*
import integration.BasicIntegrationTest
import integration.TestUser
import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import jakarta.ws.rs.client.ClientBuilder
import org.jboss.resteasy.reactive.common.util.CaseInsensitiveMap
import org.jboss.resteasy.reactive.server.core.multipart.DefaultFileUpload
import org.jboss.resteasy.reactive.server.core.multipart.FormData
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.io.InputStream
import java.nio.file.Paths
import java.util.*

@QuarkusTest
class UserServiceIntegrationTest: BasicIntegrationTest() {
    @field:Inject
    protected lateinit var userService: UserService

    @Test
    fun registerUser_ShouldCreateUserAndDonationProfile() {
        // Given
        val userId = UUID.randomUUID()

        // When
        userService.registerUser(userId)

        // Assert
        val user = dslSD.selectFrom(USERS).where(USERS.ID.eq(userId)).fetchOne()
        Assertions.assertNotNull(user)
        Assertions.assertEquals(CurrencyType.USD, user!!.currency)
        Assertions.assertNotNull(user.timeZone)

        val donationData = dslSD.selectFrom(DONATION_PROFILE_DATA).where(DONATION_PROFILE_DATA.USER_ID.eq(userId)).fetchOne()
        Assertions.assertNotNull(donationData)
        Assertions.assertEquals(0.1, donationData!!.minAmount)
        Assertions.assertEquals((40*60).toShort(), donationData.timeout)
        Assertions.assertEquals(ConfirmationType.UNCONFIRMED, donationData.confirmationType)
        Assertions.assertNotNull(donationData.notificationToken)
    }

    @Test
    fun updateDonationData_ShouldProperlyUpdateDonationProfileData() {
        // Given
        val userId = UUID.randomUUID()
        registerUserSd(userId)

        val newData = UpdateDonationProfileDataRequest(
            minAmount = rand.nextDouble(),
            timeout = rand.nextInt().toShort(),
            confirmationType = ConfirmationType.values().random(),
        )

        // When
        userService.updateDonationData(userId, newData)

        // Assert
        val donationData = dslSD.selectFrom(DONATION_PROFILE_DATA).where(DONATION_PROFILE_DATA.USER_ID.eq(userId)).fetchOne()
        Assertions.assertNotNull(donationData)
        Assertions.assertEquals(newData.minAmount, donationData!!.minAmount)
        Assertions.assertEquals(newData.timeout, donationData.timeout)
        Assertions.assertEquals(newData.confirmationType, donationData.confirmationType)
    }

    @Test
    fun getNotificationToken_ShouldReturnNotificationToken() {
        // Given
        val userId = UUID.randomUUID()
        registerUserSd(userId)

        val (expectedToken) = dslSD.select(DONATION_PROFILE_DATA.NOTIFICATION_TOKEN)
            .from(DONATION_PROFILE_DATA)
            .where(DONATION_PROFILE_DATA.USER_ID.eq(userId))
            .fetchOne() ?: throw IllegalStateException()

        // When
        val token = userService.getNotificationToken(userId)

        // Assert
        Assertions.assertEquals(expectedToken, token)
    }

    @Test
    fun regenerateNotificationToken_ShouldProperlyRegenerateNotificationToken() {
        // Given
        val userId = UUID.randomUUID()
        registerUserSd(userId)

        val (prevToken) = dslSD.select(DONATION_PROFILE_DATA.NOTIFICATION_TOKEN)
            .from(DONATION_PROFILE_DATA)
            .where(DONATION_PROFILE_DATA.USER_ID.eq(userId))
            .fetchOne() ?: throw IllegalStateException()

        // When
        val newToken = userService.regenerateNotificationToken(userId)

        // Assert
        val (newTokenDb) = dslSD.select(DONATION_PROFILE_DATA.NOTIFICATION_TOKEN)
            .from(DONATION_PROFILE_DATA)
            .where(DONATION_PROFILE_DATA.USER_ID.eq(userId))
            .fetchOne() ?: throw IllegalStateException()
        Assertions.assertEquals(newTokenDb, newToken)
        Assertions.assertNotEquals(prevToken, newToken)
    }

    @Test
    fun getUserIdByNotificationToken_ShouldReturnRightUserId() {
        // Given
        val expectedUserId = UUID.randomUUID()
        registerUserSd(expectedUserId)

        val (token) = dslSD.select(DONATION_PROFILE_DATA.NOTIFICATION_TOKEN)
            .from(DONATION_PROFILE_DATA)
            .where(DONATION_PROFILE_DATA.USER_ID.eq(expectedUserId))
            .fetchOne() ?: throw IllegalStateException()
        if (token == null) throw IllegalStateException()

        // When
        val userId = userService.getUserIdByNotificationToken(token)

        // Assert
        Assertions.assertEquals(expectedUserId, userId)
    }

    @Test
    fun getProfileData_ShouldReturnProfileData() {
        // Given
        val userId = UUID.randomUUID()
        registerUserSd(userId)

        // When
        val profileData = userService.getProfileData(userId)

        // Assert
        Assertions.assertNull(profileData.bio)
        Assertions.assertNull(profileData.avatarUrl)
    }

    @Test
    fun updateProfileData_ShouldProperlyUpdateProfileData() {
        // Given
        val userId = registerUserFull(TestUser.TestUserBuilder().build())
        val profileDataUpdateRequest = ProfileDataUpdateRequest(
            bio = UUID.randomUUID().toString(),
            firstName = UUID.randomUUID().toString(),
            lastName = UUID.randomUUID().toString()
        )

        // When
        userService.updateProfileData(userId, profileDataUpdateRequest)

        // Assert
        val (bio) = dslSD.select(USERS.BIO).from(USERS).where(USERS.ID.eq(userId)).fetchOne() ?: throw IllegalStateException()
        Assertions.assertEquals(profileDataUpdateRequest.bio, bio)

        val user = keycloakAdminClient.realm(realmName).users().get(userId.toString()).toRepresentation()
        Assertions.assertEquals(profileDataUpdateRequest.firstName, user.firstName)
        Assertions.assertEquals(profileDataUpdateRequest.lastName, user.lastName)
    }


    @Test
    fun getRegionalSettings_ShouldReturnRegionalSettings() {
        // Given
        val userId = UUID.randomUUID()
        registerUserSd(userId)

        // When
        val regionalSettings = userService.getRegionalSettings(userId)

        // Assert
        Assertions.assertEquals("America/New_York", regionalSettings.timeZone.name)
        Assertions.assertEquals(-5*60, regionalSettings.timeZone.offset)
        Assertions.assertEquals(CurrencyType.USD, regionalSettings.currency)
        Assertions.assertEquals(76, regionalSettings.availableTimeZones.size)
    }

    @Test
    fun updateRegionalSettings_ShouldProperlyUpdateRegionalSettings() {
        // Given
        val userId = UUID.randomUUID()
        registerUserSd(userId)

        val newData = RegionalProfileDataRequest(
            timeZoneName = "Asia/Tokyo",
            currency = CurrencyType.USD
        )

        // When
        userService.updateRegionalSettings(userId, newData)

        // Assert
        val regionalSettings = userService.getRegionalSettings(userId)
        Assertions.assertEquals(newData.timeZoneName, regionalSettings.timeZone.name)
        Assertions.assertEquals(9*60, regionalSettings.timeZone.offset)
        Assertions.assertEquals(CurrencyType.USD, regionalSettings.currency)
    }

    @Test
    fun updateXMRData_ShouldProperlyUpdateXMRData() {
        // Given
        val userId = registerUserFull(TestUser.TestUserBuilder().build())

        val newData = UpdateXMRDataRequest(
            enabled = true,
            keys = testXmrKeys
        )

        // When
        userService.updateXMRData(userId, newData)

        // Assert
        val (priv, pub) = dslGoipay.select(XMR_CRYPTO_DATA.PRIV_VIEW_KEY, XMR_CRYPTO_DATA.PUB_SPEND_KEY)
            .from(XMR_CRYPTO_DATA)
            .join(CRYPTO_DATA).on(CRYPTO_DATA.XMR_ID.eq(XMR_CRYPTO_DATA.ID))
            .where(CRYPTO_DATA.USER_ID.eq(userId))
            .fetchOne() ?: throw IllegalStateException()

        val (enabledCrypto) = dslSD.select(DONATION_PROFILE_DATA.ENABLED_CRYPTO)
            .from(DONATION_PROFILE_DATA)
            .where(DONATION_PROFILE_DATA.USER_ID.eq(userId))
            .fetchOne() ?: throw IllegalStateException()

        Assertions.assertEquals(newData.keys.priv, priv)
        Assertions.assertEquals(newData.keys.pub, pub)
        Assertions.assertNotNull(enabledCrypto)
        Assertions.assertTrue(enabledCrypto!!.contains(CryptoType.XMR))
    }

    @Test
    fun getDonationData_ShouldReturnDonationData() {
        // Given
        val expectedCryptoKeysData = CryptoKeysData(
            xmr = XmrKeysData(
                keys = testXmrKeys,
                enabled = false
            )
        )
        val userId = registerUserFull(TestUser.TestUserBuilder().apply { cryptoKeysData = expectedCryptoKeysData }.build())

        // When
        val donationData = userService.getDonationData(userId)

        // Assert
        Assertions.assertEquals(0.1, donationData.minAmount)
        Assertions.assertEquals((40*60).toShort(), donationData.timeout)
        Assertions.assertEquals(ConfirmationType.UNCONFIRMED, donationData.confirmationType)
        Assertions.assertEquals(CurrencyType.USD, donationData.minAmountCurrency)
        Assertions.assertEquals(expectedCryptoKeysData, donationData.cryptoKeysData)
    }

    @Test
    fun getDonationDataByUsername_ShouldReturnDonationDataByUsername() {
        // Given
        val expectedCryptoKeysData = CryptoKeysData(
            xmr = XmrKeysData(
                keys = testXmrKeys,
                enabled = true
            )
        )
        val expectedUsername = UUID.randomUUID()
        registerUserFull(TestUser.TestUserBuilder().apply {
            username = expectedUsername.toString()
            email = "$expectedUsername@test.com"
            firstName = "$expectedUsername-1"
            lastName = "$expectedUsername-2"
            cryptoKeysData = expectedCryptoKeysData
        }.build())

        // When
        val donationData = userService.getDonationDataByUsername(expectedUsername.toString())

        // Assert
        Assertions.assertEquals("$expectedUsername-1", donationData.firstName)
        Assertions.assertEquals("$expectedUsername-2", donationData.lastName)
        Assertions.assertEquals(setOf(CryptoType.XMR), donationData.acceptedCrypto)
        Assertions.assertNull(donationData.bio)
        Assertions.assertNull(donationData.avatarUrl)
    }

    @Test
    fun updateUserAvatar_ShouldProperlySaveAvatarToMinioBucket() {
        // Given
        val userId = UUID.randomUUID()
        registerUserSd(userId)

        // When
        userService.updateUserAvatar(userId, DefaultFileUpload(
            UUID.randomUUID().toString(),
            FormData(1).apply {
                val headers = CaseInsensitiveMap<String>()
                headers.add("Content-Type", "image/jpeg")
                add("avatar", Paths.get("src/test/resources/avatar.jpeg"), "avatar.jpeg", headers)
        }.getLast("avatar")))

        // Assert
        val (avatarUrl) = dslSD.select(USERS.AVATAR_URL).from(USERS).where(USERS.ID.eq(userId)).fetchOne() ?: throw IllegalStateException()
        Assertions.assertNotNull(avatarUrl)

        val getAvatarResponse = ClientBuilder.newClient().target(avatarUrl).request().get()
        Assertions.assertEquals(200, getAvatarResponse.status)

        val avatar = getAvatarResponse.readEntity(InputStream::class.java).readAllBytes()
        Assertions.assertArrayEquals(expectedAvatar, avatar)
    }
}