package unit

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.chekist32.donation.DonationRequest
import com.github.chekist32.jooq.sd.enums.ConfirmationType
import com.github.chekist32.jooq.sd.enums.CryptoType
import com.github.chekist32.jooq.sd.enums.CurrencyType
import com.github.chekist32.user.*
import genRandomHexString
import genRandomString
import io.quarkus.test.junit.QuarkusTest
import io.quarkus.test.security.TestSecurity
import io.restassured.RestAssured.given
import io.restassured.http.ContentType
import jakarta.ws.rs.core.Response
import org.junit.jupiter.api.Test
import kotlin.random.Random


@QuarkusTest
@TestSecurity(authorizationEnabled = false)
class ResourcesUnitTest {
    private var objectMapper = ObjectMapper()

    /** Donation Resource **/

    @Test
    fun donationResource_donatePost_ShouldReturnBadRequest_FromMaxSize() {
        // Given
        val req = DonationRequest(
            from = genRandomString(51),
            message = "",
            coin = CryptoType.XMR
        )

        // When/Assert
        given()
            .request()
            .contentType(ContentType.JSON)
            .body(req)
            .post("/api/v1/donation/donate/{username}", genRandomString())
            .then()
            .statusCode(Response.Status.BAD_REQUEST.statusCode)
    }

    @Test
    fun donationResource_donatePost_ShouldReturnBadRequest_FromMinSize() {
        // Given
        val req = DonationRequest(
            from = "",
            message = "",
            coin = CryptoType.XMR
        )

        // When/Assert
        given()
            .request()
            .contentType(ContentType.JSON)
            .body(req)
            .post("/api/v1/donation/donate/{username}", genRandomString())
            .then()
            .statusCode(Response.Status.BAD_REQUEST.statusCode)
    }

    @Test
    fun donationResource_donatePost_ShouldReturnBadRequest_MessageMaxSize() {
        // Given
        val req = DonationRequest(
            from = genRandomString(10),
            message = genRandomString(301),
            coin = CryptoType.XMR
        )

        // When/Assert
        val response = given()
            .request()
            .contentType(ContentType.JSON)
            .body(req)
            .post("/api/v1/donation/donate/{username}", genRandomString())
            .then()
            .statusCode(Response.Status.BAD_REQUEST.statusCode)
    }

    @Test
    fun donationResource_donatePost_ShouldReturnBadRequest_InvalidCoin() {
        // Given
        val req = DonationRequest(
            from = genRandomString(10),
            message = "",
            coin = CryptoType.XMR
        )

        // When/Assert
        given()
            .request()
            .contentType(ContentType.JSON)
            .body(objectMapper.writeValueAsString(req).replace("XMR", genRandomString(3)))
            .post("/api/v1/donation/donate/{username}", genRandomString())
            .then()
            .statusCode(Response.Status.BAD_REQUEST.statusCode)
    }

    /** User Resource **/

    @Test
    fun userResource_putProfileData_ShouldReturnBadRequest_BioMaxSize() {
        // Given
         val req = ProfileDataUpdateRequest(
             bio = genRandomString(301),
             firstName = "",
             lastName = ""
         )

        // When/Assert
        given()
            .request()
            .contentType(ContentType.JSON)
            .body(req)
            .put("/api/v1/user/profile")
            .then()
            .statusCode(Response.Status.BAD_REQUEST.statusCode)
    }

    @Test
    fun userResource_putProfileData_ShouldReturnBadRequest_FirstNameMaxSize() {
        // Given
        val req = ProfileDataUpdateRequest(
            bio = null,
            firstName = genRandomString(51),
            lastName = ""
        )

        // When/Assert
        given()
            .request()
            .contentType(ContentType.JSON)
            .body(req)
            .put("/api/v1/user/profile")
            .then()
            .statusCode(Response.Status.BAD_REQUEST.statusCode)
    }

    @Test
    fun userResource_putProfileData_ShouldReturnBadRequest_LastNameMaxSize() {
        // Given
        val req = ProfileDataUpdateRequest(
            bio = null,
            firstName = "",
            lastName = genRandomString(51),
        )

        // When/Assert
        given()
            .request()
            .contentType(ContentType.JSON)
            .body(req)
            .put("/api/v1/user/profile")
            .then()
            .statusCode(Response.Status.BAD_REQUEST.statusCode)
    }

    @Test
    fun userResource_updateDonationData_ShouldReturnBadRequest_TimeoutMinValue() {
        // Given
        val req = UpdateDonationProfileDataRequest(
            minAmount = Random.nextDouble(),
            timeout = 59,
            confirmationType = ConfirmationType.values().random()
        )

        // When/Assert
        given()
            .request()
            .contentType(ContentType.JSON)
            .body(req)
            .put("/api/v1/user/profile/donation_data")
            .then()
            .statusCode(Response.Status.BAD_REQUEST.statusCode)
    }

    @Test
    fun userResource_updateDonationData_ShouldReturnBadRequest_TimeoutMaxValue() {
        // Given
        val req = UpdateDonationProfileDataRequest(
            minAmount = Random.nextDouble(),
            timeout = 60,
            confirmationType = ConfirmationType.values().random()
        )

        // When/Assert
        given()
            .request()
            .contentType(ContentType.JSON)
            .body(objectMapper.writeValueAsString(req).replace("60", "86000"))
            .put("/api/v1/user/profile/donation_data")
            .then()
            .statusCode(Response.Status.BAD_REQUEST.statusCode)
    }

    @Test
    fun userResource_updateDonationData_ShouldReturnBadRequest_MinAmountNegative() {
        // Given
        val req = UpdateDonationProfileDataRequest(
            minAmount = -Random.nextDouble(from = Double.MIN_VALUE, until = Double.MAX_VALUE),
            timeout = 60,
            confirmationType = ConfirmationType.values().random()
        )

        // When/Assert
        given()
            .request()
            .contentType(ContentType.JSON)
            .body(req)
            .put("/api/v1/user/profile/donation_data")
            .then()
            .statusCode(Response.Status.BAD_REQUEST.statusCode)
    }

    @Test
    fun userResource_updateXMRData_ShouldReturnBadRequest_PrivMinSize() {
        // Given
        val req = UpdateXMRDataRequest(
            enabled = true,
            keys = XmrKeys(
                priv = genRandomHexString(63),
                pub = genRandomHexString(64)
            )
        )

        // When/Assert
        given()
            .request()
            .contentType(ContentType.JSON)
            .body(req)
            .put("/api/v1/user/profile/donation_data/xmr")
            .then()
            .statusCode(Response.Status.BAD_REQUEST.statusCode)
    }

    @Test
    fun userResource_updateXMRData_ShouldReturnBadRequest_PrivMaxSize() {
        // Given
        val req = UpdateXMRDataRequest(
            enabled = true,
            keys = XmrKeys(
                priv = genRandomHexString(65),
                pub = genRandomHexString(64)
            )
        )

        // When/Assert
        given()
            .request()
            .contentType(ContentType.JSON)
            .body(req)
            .put("/api/v1/user/profile/donation_data/xmr")
            .then()
            .statusCode(Response.Status.BAD_REQUEST.statusCode)
    }

    @Test
    fun userResource_updateXMRData_ShouldReturnBadRequest_PrivPattern() {
        // Given
        val req = UpdateXMRDataRequest(
            enabled = true,
            keys = XmrKeys(
                priv = genRandomString(64),
                pub = genRandomHexString(64)
            )
        )

        // When/Assert
        given()
            .request()
            .contentType(ContentType.JSON)
            .body(req)
            .put("/api/v1/user/profile/donation_data/xmr")
            .then()
            .statusCode(Response.Status.BAD_REQUEST.statusCode)
    }

    @Test
    fun userResource_updateXMRData_ShouldReturnBadRequest_PubMinSize() {
        // Given
        val req = UpdateXMRDataRequest(
            enabled = true,
            keys = XmrKeys(
                priv = genRandomHexString(64),
                pub = genRandomHexString(63)
            )
        )

        // When/Assert
        given()
            .request()
            .contentType(ContentType.JSON)
            .body(req)
            .put("/api/v1/user/profile/donation_data/xmr")
            .then()
            .statusCode(Response.Status.BAD_REQUEST.statusCode)
    }

    @Test
    fun userResource_updateXMRData_ShouldReturnBadRequest_PubMaxSize() {
        // Given
        val req = UpdateXMRDataRequest(
            enabled = true,
            keys = XmrKeys(
                priv = genRandomHexString(64),
                pub = genRandomHexString(65)
            )
        )

        // When/Assert
        given()
            .request()
            .contentType(ContentType.JSON)
            .body(req)
            .put("/api/v1/user/profile/donation_data/xmr")
            .then()
            .statusCode(Response.Status.BAD_REQUEST.statusCode)
    }

    @Test
    fun userResource_updateXMRData_ShouldReturnBadRequest_PubPattern() {
        // Given
        val req = UpdateXMRDataRequest(
            enabled = true,
            keys = XmrKeys(
                priv = genRandomHexString(64),
                pub = genRandomString(64)
            )
        )

        // When/Assert
        given()
            .request()
            .contentType(ContentType.JSON)
            .body(req)
            .put("/api/v1/user/profile/donation_data/xmr")
            .then()
            .statusCode(Response.Status.BAD_REQUEST.statusCode)
    }

    @Test
    fun userResource_putRegionalSettings_ShouldReturnBadRequest_TimeZoneNameBlank() {
        // Given
        val req = RegionalProfileDataRequest(
            timeZoneName = "",
            currency = CurrencyType.USD
        )

        // When/Assert
        given()
            .request()
            .contentType(ContentType.JSON)
            .body(req)
            .put("/api/v1/user/profile/regional")
            .then()
            .statusCode(Response.Status.BAD_REQUEST.statusCode)
    }
}