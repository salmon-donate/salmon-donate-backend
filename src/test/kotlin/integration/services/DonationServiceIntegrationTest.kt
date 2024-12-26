package integration.services

import com.github.chekist32.donation.DonationDTOForReceiver
import com.github.chekist32.donation.DonationDTOForReceiverResponse
import com.github.chekist32.donation.DonationRequest
import com.github.chekist32.donation.DonationService
import com.github.chekist32.jooq.goipay.enums.CoinType
import com.github.chekist32.jooq.goipay.enums.InvoiceStatusType
import com.github.chekist32.jooq.goipay.tables.records.InvoicesRecord
import com.github.chekist32.jooq.goipay.tables.references.INVOICES
import com.github.chekist32.jooq.sd.enums.CryptoType
import com.github.chekist32.jooq.sd.tables.records.DonationsRecord
import com.github.chekist32.jooq.sd.tables.references.DONATIONS
import com.github.chekist32.jooq.sd.tables.references.TIME_ZONES
import com.github.chekist32.jooq.sd.tables.references.USERS
import com.github.chekist32.user.CryptoKeysData
import com.github.chekist32.user.XmrKeysData
import integration.BasicIntegrationTest
import integration.TestUser
import io.grpc.StatusRuntimeException
import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import jakarta.ws.rs.InternalServerErrorException
import jakarta.ws.rs.NotFoundException
import org.jooq.impl.DSL
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import user.v1.User
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.*
import kotlin.math.absoluteValue

@QuarkusTest
class DonationServiceIntegrationTest: BasicIntegrationTest() {
    @field:Inject
    private lateinit var donationService: DonationService

    @Test
    fun handleDonation_ShouldProperlyHandleDonationRequest() {
        // Given
        val username = UUID.randomUUID().toString()
        registerUserFull(TestUser.TestUserBuilder().also {
            it.username = username
            it.cryptoKeysData = CryptoKeysData(
                xmr = XmrKeysData(
                    keys = testXmrKeys,
                    enabled = true
                )
            )
        }.build())
        val donationRequest = DonationRequest(
            from = UUID.randomUUID().toString(),
            message = UUID.randomUUID().toString(),
            coin = CryptoType.XMR
        )


        // When
        val payment = donationService.handleDonation(username, donationRequest)

        // Assert
        val donationRec = dslSD.select(DONATIONS).from(DONATIONS).where(DONATIONS.PAYMENT_ID.eq(UUID.fromString(payment.paymentId))).fetchOne()
        Assertions.assertNotNull(donationRec)

        val (donation) = donationRec!!
        Assertions.assertEquals(donationRequest.from, donation.from)
        Assertions.assertEquals(donationRequest.message, donation.message)
        Assertions.assertEquals(payment.paymentId, donation.paymentId.toString())

        Assertions.assertTrue(dslGoipay.fetchExists(DSL.selectOne().from(INVOICES).where(INVOICES.ID.eq(UUID.fromString(payment.paymentId)))))
    }

    @Test
    fun handleDonation_ShouldThrowNotFoundException() {
        // Given
        val username = UUID.randomUUID().toString()
        registerUserFull(TestUser.TestUserBuilder().also {
            it.username = username
            it.cryptoKeysData = CryptoKeysData(
                xmr = XmrKeysData(
                    keys = testXmrKeys,
                    enabled = true
                )
            )
        }.build())
        val donationRequest = DonationRequest(
            from = UUID.randomUUID().toString(),
            message = UUID.randomUUID().toString(),
            coin = CryptoType.XMR
        )

        // When/Assert
        Assertions.assertThrows(NotFoundException::class.java) { donationService.handleDonation(UUID.randomUUID().toString(), donationRequest) }
    }

    @Test
    fun handleDonation_ShouldThrowStatusRuntimeException_NoUser() {
        // Given
        val username = UUID.randomUUID().toString()
        val userId = registerKeycloakUser(TestUser.TestUserBuilder().also {
            it.username = username
        }.build())
        registerUserSd(userId)
        val donationRequest = DonationRequest(
            from = UUID.randomUUID().toString(),
            message = UUID.randomUUID().toString(),
            coin = CryptoType.XMR
        )

        // When/Assert
        Assertions.assertThrows(StatusRuntimeException::class.java) { donationService.handleDonation(username, donationRequest) }
    }
    @Test
    fun handleDonation_ShouldThrowStatusRuntimeException_NoKeys() {
        // Given
        val username = UUID.randomUUID().toString()
        val userId = registerKeycloakUser(TestUser.TestUserBuilder().also {
            it.username = username
        }.build())
        userGoipayGrpcClient.registerUser(User.RegisterUserRequest.newBuilder().setUserId(userId.toString()).build())
        registerUserSd(userId)

        val donationRequest = DonationRequest(
            from = UUID.randomUUID().toString(),
            message = UUID.randomUUID().toString(),
            coin = CryptoType.XMR
        )

        // When/Assert
        Assertions.assertThrows(StatusRuntimeException::class.java) { donationService.handleDonation(username, donationRequest) }
    }

    @RepeatedTest(value = 3)
    fun getDonationsPageableByUserId_ShouldThrowStatusRuntimeException_NoKeys() {
        // Given
        val userId = registerUserFull(TestUser.TestUserBuilder().build())

        val (offset) = dslSD.select(TIME_ZONES.UTC_OFFSET)
            .from(USERS)
            .join(TIME_ZONES)
            .on(TIME_ZONES.NAME.eq(USERS.TIME_ZONE))
            .where(USERS.ID.eq(userId))
            .fetchOne() ?: throw InternalServerErrorException()
        if (offset == null) throw InternalServerErrorException()

        val pairsCount = 1 + rand.nextInt().absoluteValue % 1000
        val paymentDonationList = ArrayList<Pair<InvoicesRecord, DonationsRecord>>(pairsCount)

        repeat(pairsCount) {
            val invoiceChain = dslGoipay.insertInto(INVOICES)
                .set(INVOICES.CRYPTO_ADDRESS, UUID.randomUUID().toString())
                .set(INVOICES.REQUIRED_AMOUNT, rand.nextDouble())
                .set(INVOICES.USER_ID, userId)
                .set(INVOICES.COIN, CoinType.XMR)
                .set(INVOICES.CONFIRMATIONS_REQUIRED, rand.nextInt().absoluteValue.toShort())
                .set(INVOICES.STATUS, InvoiceStatusType.PENDING)
                .set(INVOICES.CREATED_AT, OffsetDateTime.now())
                .set(INVOICES.EXPIRES_AT, OffsetDateTime.now().plusSeconds(rand.nextInt().absoluteValue.toLong()))

            val pendingMempoolApply = {
                invoiceChain.set(INVOICES.STATUS, InvoiceStatusType.PENDING_MEMPOOL)
                invoiceChain.set(INVOICES.ACTUAL_AMOUNT, rand.nextDouble())
                invoiceChain.set(INVOICES.TX_ID, UUID.randomUUID().toString())
            }
            when (rand.nextInt().absoluteValue % 4) {
                // PENDING_MEMPOOL
                1 -> pendingMempoolApply()
                // CONFIRMED
                2 -> {
                    pendingMempoolApply()
                    invoiceChain.set(INVOICES.CONFIRMED_AT, OffsetDateTime.now())
                    invoiceChain.set(INVOICES.STATUS, InvoiceStatusType.CONFIRMED)
                }
                // EXPIRED
                3 -> invoiceChain.set(INVOICES.STATUS, InvoiceStatusType.EXPIRED)
            }
            val payment = invoiceChain.returning().fetchOne()

            val donationChain = dslSD.insertInto(DONATIONS)
                .set(DONATIONS.USER_ID, userId)
                .set(DONATIONS.PAYMENT_ID, payment!!.id)
                .set(DONATIONS.FROM, UUID.randomUUID().toString())
                .set(DONATIONS.MESSAGE, UUID.randomUUID().toString())
            if (payment.status == InvoiceStatusType.CONFIRMED) donationChain.set(DONATIONS.SHOWN_AT, OffsetDateTime.now())
            val donation = donationChain.returning().fetchOne()

            paymentDonationList.add(Pair(payment, donation!!))
        }

        val confirmedPairs = paymentDonationList.filter { it.first.status == InvoiceStatusType.CONFIRMED }.sortedByDescending { it.second.shownAt }
        val expectedLimit = 1 + rand.nextInt().absoluteValue % confirmedPairs.size
        val expectedPage = rand.nextInt().absoluteValue % confirmedPairs.size / expectedLimit
        val expectedResponse = DonationDTOForReceiverResponse(
            page = expectedPage,
            limit = expectedLimit,
            totalCount = confirmedPairs.size,
            data = confirmedPairs.drop(expectedPage*expectedLimit).take(expectedLimit).map { (p, d) -> DonationDTOForReceiver(
                from = d.from!!,
                message = d.message,
                amount = p.actualAmount!!,
                coin = p.coin!!,
                createdAt = d.shownAt!!.toZonedDateTime()
                    .withZoneSameInstant(ZoneOffset.UTC)
                    .plusMinutes(offset.totalMinutes.toLong())
            )}
        )

        // When
        val response = donationService.getDonationsPageableByUserId(userId, expectedPage, expectedLimit)

        // Assert
        Assertions.assertEquals(expectedResponse, response)
    }
}