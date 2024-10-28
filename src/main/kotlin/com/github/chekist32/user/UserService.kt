package com.github.chekist32.user

import com.github.chekist32.TransactionalContext
import com.github.chekist32.VT
import com.github.chekist32.base.config.MinioConfig
import com.github.chekist32.base.config.baseUrl
import com.github.chekist32.donation.DonationData
import com.github.chekist32.donation.toDonationData
import com.github.chekist32.jooq.goipay.tables.references.CRYPTO_DATA
import com.github.chekist32.jooq.goipay.tables.references.XMR_CRYPTO_DATA
import com.github.chekist32.jooq.sd.enums.CryptoType
import com.github.chekist32.jooq.sd.tables.references.DONATION_PROFILE_DATA
import com.github.chekist32.jooq.sd.tables.references.TIME_ZONES
import com.github.chekist32.jooq.sd.tables.references.USERS
import com.github.chekist32.toCoinTypes
import com.github.chekist32.toCryptoKeysData
import crypto.v1.Crypto
import io.minio.MinioAsyncClient
import io.minio.UploadObjectArgs
import io.quarkus.grpc.GrpcClient
import io.smallrye.mutiny.coroutines.awaitSuspending
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Named
import jakarta.ws.rs.BadRequestException
import jakarta.ws.rs.InternalServerErrorException
import jakarta.ws.rs.NotFoundException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.await
import kotlinx.coroutines.withContext
import org.jboss.resteasy.reactive.multipart.FileUpload
import org.jooq.DSLContext
import org.jooq.impl.DSL
import user.v1.MutinyUserServiceGrpc
import user.v1.User.UpdateCryptoKeysRequest
import java.util.*

@ApplicationScoped
class UserService(
    @Named("dsl-sd")
    private val dslSD: DSLContext,
    @Named("dsl-goipay")
    private val dslGoipay: DSLContext,
    private val keycloakUserService: KeycloakUserService,
    private val minioClient: MinioAsyncClient,
    private val minioConfig: MinioConfig,
    @GrpcClient("goipay")
    private val userGoipayGrpcClient: MutinyUserServiceGrpc.MutinyUserServiceStub,
) {
    private suspend fun getCryptoKeys(userId: UUID): CryptoKeys = withContext(Dispatchers.VT) {
        val keys = dslGoipay.select(XMR_CRYPTO_DATA.PRIV_VIEW_KEY, XMR_CRYPTO_DATA.PUB_SPEND_KEY)
            .from(CRYPTO_DATA)
            .join(XMR_CRYPTO_DATA)
            .on(XMR_CRYPTO_DATA.ID.eq(CRYPTO_DATA.XMR_ID))
            .where(CRYPTO_DATA.USER_ID.eq(userId))
            .fetchOne()
        
        return@withContext CryptoKeys(
            xmr = XmrKeys(
                priv = keys?.value1() ?: "",
                pub = keys?.value2() ?: ""
            )
        )
    } 
    
    suspend fun registerUser(userId: UUID) = withContext(Dispatchers.VT) {
        val dslSD = coroutineContext[TransactionalContext]?.transactionalDslContext ?: dslSD

        dslSD.insertInto(USERS)
            .set(USERS.ID, userId)
            .execute()

        dslSD.insertInto(DONATION_PROFILE_DATA)
            .set(DONATION_PROFILE_DATA.USER_ID, userId)
            .execute()
    }

    suspend fun updateDonationData(userId: UUID, newData: UpdateDonationProfileDataRequest): Unit = withContext(Dispatchers.VT) {
        val dslSD = coroutineContext[TransactionalContext]?.transactionalDslContext ?: dslSD

        dslSD.update(DONATION_PROFILE_DATA)
            .set(DONATION_PROFILE_DATA.MIN_AMOUNT, newData.minAmount)
            .set(DONATION_PROFILE_DATA.CONFIRMATION_TYPE, newData.confirmationType)
            .set(DONATION_PROFILE_DATA.TIMEOUT, newData.timeout)
            .where(DONATION_PROFILE_DATA.USER_ID.eq(userId))
            .execute()
    }

    private suspend fun isCryptoEnabled(userId: UUID, cryptoType: CryptoType) = withContext(Dispatchers.VT) {
        val dslSD = coroutineContext[TransactionalContext]?.transactionalDslContext ?: dslSD

        val (res) = dslSD.selectCount()
            .from(DONATION_PROFILE_DATA)
            .where(
                DONATION_PROFILE_DATA.USER_ID.eq(userId)
                .and(DONATION_PROFILE_DATA.ENABLED_CRYPTO.contains(arrayOf(cryptoType)))
            )
            .fetchOne() ?: throw InternalServerErrorException()

        return@withContext res > 0
    }

    suspend fun updateXMRData(userId: UUID, newData: UpdateXMRDataRequest): Unit = withContext(Dispatchers.VT) {
        val dslSD = coroutineContext[TransactionalContext]?.transactionalDslContext ?: dslSD

        if (!newData.enabled) {
            dslSD.update(DONATION_PROFILE_DATA)
                .set(DONATION_PROFILE_DATA.ENABLED_CRYPTO,
                    DSL.arrayRemove(DONATION_PROFILE_DATA.ENABLED_CRYPTO, CryptoType.XMR))
                .where(DONATION_PROFILE_DATA.USER_ID.eq(userId))
                .execute()

            return@withContext
        }

        if (!isCryptoEnabled(userId, CryptoType.XMR)) {
            dslSD.update(DONATION_PROFILE_DATA)
                .set(DONATION_PROFILE_DATA.ENABLED_CRYPTO,
                    DSL.arrayAppend(DONATION_PROFILE_DATA.ENABLED_CRYPTO, CryptoType.XMR))
                .where(DONATION_PROFILE_DATA.USER_ID.eq(userId))
                .execute()
        }

        userGoipayGrpcClient.updateCryptoKeys(
            UpdateCryptoKeysRequest.newBuilder()
                .setUserId(userId.toString())
                .setXmrReq(
                    Crypto.XmrKeysUpdateRequest.newBuilder()
                        .setPrivViewKey(newData.keys.priv)
                        .setPubSpendKey(newData.keys.pub)
                        .build()
                )
                .build()
        ).awaitSuspending()
    }

    suspend fun getDonationData(userId: UUID): DonationProfileDataResponse = withContext(Dispatchers.VT) {
        val dslSD = coroutineContext[TransactionalContext]?.transactionalDslContext ?: dslSD

        val (minAmount, minAmountCurrency, timeout, confirmationType, enabledCrypto) = dslSD.select(
            DONATION_PROFILE_DATA.MIN_AMOUNT,
            USERS.CURRENCY,
            DONATION_PROFILE_DATA.TIMEOUT,
            DONATION_PROFILE_DATA.CONFIRMATION_TYPE,
            DONATION_PROFILE_DATA.ENABLED_CRYPTO
        )
            .from(DONATION_PROFILE_DATA)
            .join(USERS)
            .on(USERS.ID.eq(DONATION_PROFILE_DATA.USER_ID))
            .where(DONATION_PROFILE_DATA.USER_ID.eq(userId))
            .fetchOne() ?: throw InternalServerErrorException()
        if (minAmount == null || minAmountCurrency == null || timeout == null || confirmationType == null || enabledCrypto == null)
            throw InternalServerErrorException()

        val keys = getCryptoKeys(userId)

        return@withContext DonationProfileDataResponse(
            minAmount = minAmount,
            minAmountCurrency = minAmountCurrency,
            timeout = timeout,
            confirmationType = confirmationType,
            cryptoKeysData = keys.toCryptoKeysData(enabledCrypto.filterNotNull().toSet())
        )
    }

    suspend fun getDonationDataByUsername(username: String): DonationData {
        val user = try {
            keycloakUserService.getUserByUsername(username)
        } catch (e: NotFoundException) {
            throw e
        } catch (e: Exception) {
            throw InternalServerErrorException()
        }

        val userId = UUID.fromString(user.id)

        val (bio, avatarUrl, acceptedCrypto) = withContext(Dispatchers.VT) {
            val dslSD = coroutineContext[TransactionalContext]?.transactionalDslContext ?: dslSD

            return@withContext dslSD.select(USERS.BIO, USERS.AVATAR_URL, DONATION_PROFILE_DATA.ENABLED_CRYPTO)
                .from(USERS)
                .join(DONATION_PROFILE_DATA)
                .on(USERS.ID.eq(DONATION_PROFILE_DATA.USER_ID))
                .where(USERS.ID.eq(userId))
                .fetchOne() ?: throw InternalServerErrorException()
        }
        if (acceptedCrypto == null) throw InternalServerErrorException()

        val keys = getCryptoKeys(userId)

        return user.toDonationData(
            avatarUrl = avatarUrl,
            bio = bio,
            acceptedCrypto = keys.toCoinTypes(acceptedCrypto.filterNotNull().toSet())
        )
    }

    suspend fun getNotificationToken(userId: UUID): UUID = withContext(Dispatchers.VT) {
        val dslSD = coroutineContext[TransactionalContext]?.transactionalDslContext ?: dslSD

        val (token) = dslSD.select(DONATION_PROFILE_DATA.NOTIFICATION_TOKEN)
            .from(DONATION_PROFILE_DATA)
            .where(DONATION_PROFILE_DATA.USER_ID.eq(userId))
            .fetchOne() ?: throw InternalServerErrorException()
        if (token == null) throw InternalServerErrorException()

        return@withContext token
    }

    suspend fun regenerateNotificationToken(userId: UUID): UUID = withContext(Dispatchers.VT) {
        val dslSD = coroutineContext[TransactionalContext]?.transactionalDslContext ?: dslSD

        val newToken = UUID.randomUUID()

        dslSD.update(DONATION_PROFILE_DATA)
            .set(DONATION_PROFILE_DATA.NOTIFICATION_TOKEN, newToken)
            .where(DONATION_PROFILE_DATA.USER_ID.eq(userId))
            .execute()

        return@withContext newToken
    }

    suspend fun getProfileData(userId: UUID): ProfileDataResponse = withContext(Dispatchers.VT) {
        val dslSD = coroutineContext[TransactionalContext]?.transactionalDslContext ?: dslSD

        val (avatarUrl, bio) = dslSD.select(
            USERS.AVATAR_URL,
            USERS.BIO
        )
            .from(USERS)
            .where(USERS.ID.eq(userId))
            .fetchOne() ?: throw NotFoundException("There is no user with such userId")

        return@withContext ProfileDataResponse(
            avatarUrl = avatarUrl,
            bio = bio
        )
    }

    suspend fun updateProfileData(userId: UUID, newData: ProfileDataUpdateRequest) {
        val user = keycloakUserService.getUserById(userId.toString())
        if (user != null) {
            user.firstName = newData.firstName
            user.lastName = newData.lastName
            keycloakUserService.updateUser(user)
        }

        withContext(Dispatchers.VT) {
            val dslSD = coroutineContext[TransactionalContext]?.transactionalDslContext ?: dslSD
            val dslReq = dslSD.update(USERS)

            dslReq.set(USERS.BIO, newData.bio)
                .where(USERS.ID.eq(userId))
                .execute()
        }
    }

    suspend fun updateUserAvatar(userId: UUID, avatarFile: FileUpload) = withContext(Dispatchers.VT) {
        val contentType = avatarFile.contentType()
        val uri = "public/avatars/${UUID.randomUUID()}.${contentType.split('/').last()}"

        minioClient.uploadObject(
            UploadObjectArgs.builder()
                .bucket("salmon-donate")
                .`object`(uri)
                .filename(avatarFile.uploadedFile().toAbsolutePath().toString())
                .contentType(contentType)
                .build()
        ).await()

        val dslSD = coroutineContext[TransactionalContext]?.transactionalDslContext ?: dslSD

        dslSD.update(USERS)
            .set(USERS.AVATAR_URL, "${minioConfig.baseUrl()}/salmon-donate/$uri")
            .where(USERS.ID.eq(userId))
            .execute()
    }

    suspend fun getUserIdByNotificationToken(token: UUID) = withContext(Dispatchers.VT) {
        val dslSD = coroutineContext[TransactionalContext]?.transactionalDslContext ?: dslSD

        val (userId) = dslSD.select(DONATION_PROFILE_DATA.USER_ID)
            .from(DONATION_PROFILE_DATA)
            .where(DONATION_PROFILE_DATA.NOTIFICATION_TOKEN.eq(token))
            .fetchOne() ?: throw BadRequestException("Invalid notification token")
        if (userId == null) throw InternalServerErrorException()

        return@withContext userId
    }

    suspend fun getRegionalSettings(userId: UUID): RegionalProfileDataResponse = withContext(Dispatchers.VT) {
        val dslSD = coroutineContext[TransactionalContext]?.transactionalDslContext ?: dslSD

        val (timeZoneName, offset, currency) = dslSD.select(USERS.TIME_ZONE, TIME_ZONES.UTC_OFFSET, USERS.CURRENCY)
            .from(USERS)
            .join(TIME_ZONES)
            .on(TIME_ZONES.NAME.eq(USERS.TIME_ZONE))
            .where(USERS.ID.eq(userId))
            .fetchOne() ?: throw InternalServerErrorException()
        if (timeZoneName == null || offset == null || currency == null) throw InternalServerErrorException()

        val availableTimeZones = dslSD.select(TIME_ZONES.NAME, TIME_ZONES.UTC_OFFSET)
            .from(TIME_ZONES)
            .orderBy(TIME_ZONES.UTC_OFFSET.asc())
            .fetch()
            .filter { it.value2() != null && it.value1() != null }
            .map { TimeZone(offset = it.value2()!!.totalMinutes.toInt(), name = it.value1()!!) }

        return@withContext RegionalProfileDataResponse(
            timeZone = TimeZone(
                name = timeZoneName,
                offset = offset.totalMinutes.toInt()
            ),
            availableTimeZones = availableTimeZones,
            currency = currency
        )
    }

    suspend fun updateRegionalSettings(userId: UUID, newData: RegionalProfileDataRequest) = withContext(Dispatchers.VT) {
        val dslSD = coroutineContext[TransactionalContext]?.transactionalDslContext ?: dslSD

        val timeZoneExists = dslSD.fetchExists(
            DSL.selectOne()
                .from(TIME_ZONES)
                .where(TIME_ZONES.NAME.eq(newData.timeZoneName))
        )
        if (!timeZoneExists) throw BadRequestException("Invalid time zone name")

        dslSD.update(USERS)
            .set(USERS.TIME_ZONE, newData.timeZoneName)
            .set(USERS.CURRENCY, newData.currency)
            .where(USERS.ID.eq(userId))
            .execute()
    }
}