import com.github.chekist32.user.KeycloakUserService
import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import jakarta.ws.rs.NotFoundException
import jakarta.ws.rs.WebApplicationException
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.keycloak.representations.idm.UserRepresentation
import java.util.*

@QuarkusTest
class KeycloakUserServiceIntegrationTest: BasicIntegrationTest() {
    @field:Inject
    private lateinit var keycloakUserService: KeycloakUserService

    @Test
    fun getUserByUsername_ShouldReturnRightUser() {
        // Given
        val expectedUser = TestUser.TestUserBuilder().build()
        val expectedUserId = registerKeycloakUser(expectedUser)

        // When
        val user = keycloakUserService.getUserByUsername(expectedUser.username)

        // Assert
        Assertions.assertEquals(expectedUserId.toString(), user.id)
        Assertions.assertEquals(expectedUser.username, user.username)
        Assertions.assertEquals(expectedUser.lastName, user.lastName)
        Assertions.assertEquals(expectedUser.firstName, user.firstName)
        Assertions.assertEquals(expectedUser.email, user.email)
    }

    @Test
    fun getUserByUsername_ShouldThrowException() {
        // Given
        registerKeycloakUser(TestUser.TestUserBuilder().build())

        // When/Assert
        Assertions.assertThrows(NotFoundException::class.java) { keycloakUserService.getUserByUsername(UUID.randomUUID().toString()) }
    }

    @Test
    fun getUserById_ShouldReturnRightUser() {
        // Given
        val expectedUser = TestUser.TestUserBuilder().build()
        val expectedUserId = registerKeycloakUser(expectedUser)

        // When
        val user = keycloakUserService.getUserById(expectedUserId.toString())

        // Assert
        Assertions.assertEquals(expectedUserId.toString(), user.id)
        Assertions.assertEquals(expectedUser.username, user.username)
        Assertions.assertEquals(expectedUser.lastName, user.lastName)
        Assertions.assertEquals(expectedUser.firstName, user.firstName)
        Assertions.assertEquals(expectedUser.email, user.email)
    }

    @Test
    fun getUserById_ShouldThrowException() {
        // Given
        registerKeycloakUser(TestUser.TestUserBuilder().build())

        // When/Assert
        Assertions.assertThrows(WebApplicationException::class.java) { keycloakUserService.getUserById(UUID.randomUUID().toString()) }
    }


    @Test
    fun updateUser_ShouldProperlyUpdateUser() {
        // Given
        val expectedUserId = registerKeycloakUser(TestUser.TestUserBuilder().build())
        val expectedUpdatedKeycloakUser = keycloakAdminClient.realm(realmName).users().get(expectedUserId.toString()).toRepresentation().apply {
            lastName = UUID.randomUUID().toString()
            firstName = UUID.randomUUID().toString()
        }

        // When
        keycloakUserService.updateUser(expectedUpdatedKeycloakUser)

        // Assert
        val updatedKeycloakUser = keycloakAdminClient.realm(realmName).users().get(expectedUserId.toString()).toRepresentation()
        Assertions.assertEquals(expectedUpdatedKeycloakUser.id, updatedKeycloakUser.id)
        Assertions.assertEquals(expectedUpdatedKeycloakUser.username, updatedKeycloakUser.username)
        Assertions.assertEquals(expectedUpdatedKeycloakUser.lastName, updatedKeycloakUser.lastName)
        Assertions.assertEquals(expectedUpdatedKeycloakUser.firstName, updatedKeycloakUser.firstName)
        Assertions.assertEquals(expectedUpdatedKeycloakUser.email, updatedKeycloakUser.email)
    }

    @Test
    fun updateUser_ShouldThrowException() {
        // Given
        registerKeycloakUser(TestUser.TestUserBuilder().build())

        // When/Assert
        Assertions.assertThrows(WebApplicationException::class.java) { keycloakUserService.updateUser(UserRepresentation().apply { id = UUID.randomUUID().toString() }) }
    }
}