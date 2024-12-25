import com.github.chekist32.converter.CryptoCurrencyConverter
import com.github.chekist32.jooq.goipay.enums.InvoiceStatusType
import com.github.chekist32.jooq.goipay.tables.references.INVOICES
import com.github.chekist32.jooq.sd.enums.ConfirmationType
import com.github.chekist32.jooq.sd.enums.CurrencyType
import com.github.chekist32.payment.NewCryptoPaymentRequest
import com.github.chekist32.payment.PaymentService
import com.github.chekist32.toCoinTypeJooq
import com.github.chekist32.user.CryptoKeysData
import com.github.chekist32.user.XmrKeysData
import crypto.v1.Crypto
import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import jakarta.ws.rs.NotFoundException
import org.jooq.impl.DSL
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.util.*
import kotlin.math.absoluteValue

@QuarkusTest
class PaymentServiceIntegrationTest: BasicIntegrationTest() {
    @field:Inject
    private lateinit var paymentService: PaymentService
    @field:Inject
    private lateinit var currencyConverter: CryptoCurrencyConverter

    @Test
    fun createCryptoPayment_ShouldReturnCreatedPayment() {
        // Given
        val userId = registerUserFull(TestUser.TestUserBuilder().apply {
            cryptoKeysData = CryptoKeysData(
                xmr = XmrKeysData(
                    keys = testXmrKeys,
                    enabled = true
                )
            ) 
        }.build())
        val request = NewCryptoPaymentRequest(
            userId = userId,
            amount = rand.nextDouble(0.999_999_999_999).absoluteValue,
            currency = CurrencyType.values().random(),
            confirmation = ConfirmationType.values().random(),
            timeout = rand.nextInt(999_999).absoluteValue,
            coin = Crypto.CoinType.XMR
        )
        val confirmations = PaymentService::class.java.declaredMethods.find { it.name.contains("confirmations") } ?: throw IllegalStateException()
        confirmations.isAccessible = true

        // When
        val payment = paymentService.createCryptoPayment(request)

        // Assert
        Assertions.assertEquals(request.coin.toCoinTypeJooq(), payment.coin)
        Assertions.assertEquals(currencyConverter.convertUsdToCrypto(request.amount, request.coin), payment.requiredAmount)
        Assertions.assertEquals(request.timeout.toLong(), payment.timeout)
        Assertions.assertEquals((confirmations.invoke(paymentService, request.coin, request.confirmation) as Short).toInt(), payment.confirmationsRequired)
        Assertions.assertEquals("74xhb5sXRsnDZv8RKFEv7LAMfUq5AmGEEB77SVvsUJf8bLvFMSEfc8YYyJHF6xNNnjAZQmgqZp76AjT8bD6qKkLZLeR42oi", payment.cryptoAddress)
        Assertions.assertTrue(dslGoipay.fetchExists(DSL.selectOne().from(INVOICES).where(INVOICES.ID.eq(UUID.fromString(payment.paymentId)))))
    }

    @Test
    fun getPaymentByPaymentId_ShouldReturnCreatedPayment() {
        // Given
        val userId = registerUserFull(TestUser.TestUserBuilder().apply {
            cryptoKeysData = CryptoKeysData(
                xmr = XmrKeysData(
                    keys = testXmrKeys,
                    enabled = true
                )
            )
        }.build())
        val expectedPayment = paymentService.createCryptoPayment(NewCryptoPaymentRequest(
            userId = userId,
            amount = rand.nextDouble(0.999_999_999_999).absoluteValue,
            currency = CurrencyType.values().random(),
            confirmation = ConfirmationType.values().random(),
            timeout = rand.nextInt(999_999).absoluteValue,
            coin = Crypto.CoinType.XMR
        ))

        // When
        val payment = paymentService.getPaymentByPaymentId(UUID.fromString(expectedPayment.paymentId))

        // Assert
        Assertions.assertEquals(expectedPayment.paymentId, payment.id.toString())
        Assertions.assertEquals(expectedPayment.requiredAmount, payment.requiredAmount)
        Assertions.assertEquals(expectedPayment.cryptoAddress, payment.cryptoAddress)
        Assertions.assertEquals(expectedPayment.coin, payment.coin)
        Assertions.assertEquals(expectedPayment.confirmationsRequired, payment.confirmationsRequired!!.toInt())
        Assertions.assertEquals(userId, payment.userId)
        Assertions.assertEquals(InvoiceStatusType.PENDING, payment.status)
        Assertions.assertNull(payment.confirmedAt)
        Assertions.assertNull(payment.txId)
    }

    @Test
    fun getPaymentByPaymentId_ShouldThrowException() {
        // Given
        val userId = registerUserFull(TestUser.TestUserBuilder().apply {
            cryptoKeysData = CryptoKeysData(
                xmr = XmrKeysData(
                    keys = testXmrKeys,
                    enabled = true
                )
            )
        }.build())
        paymentService.createCryptoPayment(NewCryptoPaymentRequest(
            userId = userId,
            amount = rand.nextDouble(0.999_999_999_999).absoluteValue,
            currency = CurrencyType.values().random(),
            confirmation = ConfirmationType.values().random(),
            timeout = rand.nextInt(999_999).absoluteValue,
            coin = Crypto.CoinType.XMR
        ))

        // When/Assert
        Assertions.assertThrows(NotFoundException::class.java) { paymentService.getPaymentByPaymentId(UUID.randomUUID()) }
    }
}