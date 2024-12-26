package integration.services

import com.github.chekist32.user.CryptoKeysData
import com.github.chekist32.user.XmrKeysData
import crypto.v1.Crypto
import integration.BasicIntegrationTest
import integration.TestUser
import invoice.v1.InvoiceOuterClass
import io.quarkus.test.junit.QuarkusTest
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.TimeUnit
import kotlin.math.absoluteValue

@QuarkusTest
class PaymentGrpcNotificationServiceIntegrationTest: BasicIntegrationTest() {
    @Test
    fun subscribeToInvoice_subscribeToInvoiceByStatus_ShouldSendInvoice() {
        // Given
        val blockingQueue = ArrayBlockingQueue<InvoiceOuterClass.Invoice>(2)
        val userId = registerUserFull(TestUser.TestUserBuilder().apply {
            cryptoKeysData = CryptoKeysData(
                xmr = XmrKeysData(
                    keys = testXmrKeys,
                    enabled = true
                )
            )
        }.build())

        // When
        paymentGrpcNotificationService.subscribeToInvoice { blockingQueue.add(it) }
        paymentGrpcNotificationService.subscribeToInvoice({ it.status == InvoiceOuterClass.InvoiceStatusType.PENDING }) { blockingQueue.add(it) }

        val paymentId = paymentGoipayGrpcClient.createInvoice(InvoiceOuterClass.CreateInvoiceRequest
            .newBuilder()
            .setUserId(userId.toString())
            .setCoin(Crypto.CoinType.XMR)
            .setAmount(rand.nextDouble())
            .setConfirmations(rand.nextInt().absoluteValue)
            .setTimeout(rand.nextInt().absoluteValue.toLong())
            .build()
        ).paymentId


        // Assert
        val payment1 = blockingQueue.poll(20, TimeUnit.SECONDS)
        val payment2 = blockingQueue.poll(20, TimeUnit.SECONDS)
        Assertions.assertEquals(payment1, payment2)
        Assertions.assertNotNull(payment1)
        Assertions.assertEquals(paymentId, payment1!!.id)
    }

}