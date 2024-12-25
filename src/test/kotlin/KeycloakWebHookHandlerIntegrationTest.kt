import com.github.chekist32.jooq.sd.tables.references.DONATION_PROFILE_DATA
import com.github.chekist32.jooq.sd.tables.references.USERS
import com.github.chekist32.webhook.KeycloakEventRequest
import com.github.chekist32.webhook.KeycloakEventType
import com.github.chekist32.webhook.KeycloakWebHookHandler
import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import org.jooq.impl.DSL
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

@QuarkusTest
class KeycloakWebHookHandlerIntegrationTest: BasicIntegrationTest() {
    @field:Inject
    private lateinit var keycloakWebHookHandler: KeycloakWebHookHandler

    @Test
    fun handleKeycloakWebHookEvent_ShouldProperlyRegisterUser() {
        // Given
        val userId = registerKeycloakUser(TestUser.TestUserBuilder().build())

        // When
        keycloakWebHookHandler.handleKeycloakWebHookEvent(KeycloakEventRequest(
            userId = userId,
            time = null,
            type = KeycloakEventType.REGISTER,
            realmId = null,
            clientId = null,
            ipAddress = null,
            error = null,
            details = null
        ))

        // Assert
        Assertions.assertTrue(dslSD.fetchExists(DSL.selectOne().from(USERS).where(USERS.ID.eq(userId))))
        Assertions.assertTrue(dslSD.fetchExists(DSL.selectOne().from(DONATION_PROFILE_DATA).where(DONATION_PROFILE_DATA.USER_ID.eq(userId))))
        Assertions.assertTrue(dslGoipay.fetchExists(DSL.selectOne().from(com.github.chekist32.jooq.goipay.tables.references.USERS).where(
            com.github.chekist32.jooq.goipay.tables.references.USERS.ID.eq(userId))))
    }

}