package com.github.chekist32.donation

import com.github.chekist32.jooq.goipay.tables.references.INVOICES
import com.github.chekist32.jooq.sd.tables.references.DONATIONS
import com.github.chekist32.jooq.sd.tables.references.DONATION_PROFILE_DATA
import com.github.chekist32.jooq.sd.tables.references.TIME_ZONES
import com.github.chekist32.jooq.sd.tables.references.USERS
import com.github.chekist32.payment.InvoiceToPayDTO
import com.github.chekist32.payment.NewCryptoPaymentRequest
import com.github.chekist32.payment.PaymentService
import com.github.chekist32.toCoinType
import com.github.chekist32.user.KeycloakUserService
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Named
import jakarta.ws.rs.InternalServerErrorException
import jakarta.ws.rs.NotFoundException
import org.jooq.DSLContext
import java.time.ZoneOffset
import java.util.*

@ApplicationScoped
class DonationService(
    private val keycloakUserService: KeycloakUserService,
    private val paymentService: PaymentService,
    @Named("dsl-sd")
    private val dslSD: DSLContext,
    @Named("dsl-goipay")
    private val dslGoipay: DSLContext
) {
    fun getDonationsPageableByUserId(userId: UUID, page: Int, limit: Int): DonationDTOForReceiverResponse {
        val (totalCount) = dslSD.selectCount()
            .from(DONATIONS)
            .where(DONATIONS.USER_ID.eq(userId).and(DONATIONS.SHOWN_AT.isNotNull))
            .fetchOne() ?: throw InternalServerErrorException()

        val donations = dslSD.select(
            DONATIONS.FROM,
            DONATIONS.MESSAGE,
            DONATIONS.SHOWN_AT,
            DONATIONS.PAYMENT_ID
        )
            .from(DONATIONS)
            .join(USERS)
            .on(USERS.ID.eq(DONATIONS.USER_ID))
            .where(USERS.ID.eq(userId).and(DONATIONS.SHOWN_AT.isNotNull))
            .orderBy(DONATIONS.SHOWN_AT.desc())
            .limit(limit)
            .offset(page*limit)
            .fetch()

        val (offset) = dslSD.select(TIME_ZONES.UTC_OFFSET)
            .from(USERS)
            .join(TIME_ZONES)
            .on(TIME_ZONES.NAME.eq(USERS.TIME_ZONE))
            .where(USERS.ID.eq(userId))
            .fetchOne() ?: throw InternalServerErrorException()
        if (offset == null) throw InternalServerErrorException()

        val payments = dslGoipay.select(INVOICES.ID, INVOICES.ACTUAL_AMOUNT, INVOICES.COIN)
            .from(INVOICES)
            .where(INVOICES.ID.`in`(donations.mapNotNull { it.value4() }))
            .orderBy(INVOICES.CONFIRMED_AT.desc())
            .fetch()

        return DonationDTOForReceiverResponse(
            page = page,
            limit = limit,
            totalCount = totalCount,
            data = donations.mapNotNull { d ->
                payments.find { p -> p.value1() == d.value4() }?.let { p ->
                    val (from, message, shownAt) = d
                    val (_, actualAmount, coin) = p
                    if (shownAt == null || actualAmount == null || coin == null) throw InternalServerErrorException()

                    DonationDTOForReceiver(
                        from = from ?: "Anon",
                        message = message,
                        amount = actualAmount,
                        coin = coin,
                        createdAt = shownAt.toZonedDateTime()
                            .withZoneSameInstant(ZoneOffset.UTC)
                            .plusMinutes(offset.totalMinutes.toLong())
                    )
                }
            }
        )
    }

    fun handleDonation(username: String, req: DonationRequest): InvoiceToPayDTO {
        val userId = try {
            val user = keycloakUserService.getUserByUsername(username)
            UUID.fromString(user.id)
        } catch (e: NotFoundException) {
            throw e
        } catch (e: Exception) {
            throw InternalServerErrorException()
        }

        val (minAmount, confirmation, minAmountCurrency, timeout) =
            dslSD.select(
                DONATION_PROFILE_DATA.MIN_AMOUNT,
                DONATION_PROFILE_DATA.CONFIRMATION_TYPE,
                USERS.CURRENCY,
                DONATION_PROFILE_DATA.TIMEOUT
            )
                .from(DONATION_PROFILE_DATA)
                .join(USERS)
                .on(USERS.ID.eq(DONATION_PROFILE_DATA.USER_ID))
                .where(DONATION_PROFILE_DATA.USER_ID.eq(userId))
                .fetchOne() ?: throw InternalServerErrorException()
        if (minAmount == null || confirmation == null || minAmountCurrency == null || timeout == null) throw InternalServerErrorException()

        val invoice = paymentService.createCryptoPayment(
            NewCryptoPaymentRequest(
                userId = userId,
                amount = minAmount,
                currency = minAmountCurrency,
                confirmation = confirmation,
                timeout = timeout.toInt(),
                coin = req.coin.toCoinType()
            )
        )

        dslSD.insertInto(DONATIONS)
            .set(DONATIONS.FROM, req.from)
            .set(DONATIONS.MESSAGE, req.message)
            .set(DONATIONS.USER_ID, userId)
            .set(DONATIONS.PAYMENT_ID, UUID.fromString(invoice.paymentId))
            .execute()

        return invoice
    }
}