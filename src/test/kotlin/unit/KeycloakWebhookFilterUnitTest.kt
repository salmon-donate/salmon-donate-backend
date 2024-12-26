package unit

import com.github.chekist32.webhook.KeycloakWebhookFilter
import jakarta.ws.rs.container.ContainerRequestContext
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.core.UriInfo
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import java.io.ByteArrayInputStream
import java.util.*
import java.util.stream.Stream


class KeycloakWebhookFilterUnitTest {
    companion object {
        @JvmStatic
        private fun validTestData() = Stream.of(
            Arguments.of(
                """
                    550YQjJdE/OuI/roadBsFMlDM+qfkLEag8JHPj0siMBzLvA76PZyZ2bG82kIobE3
                    Tkdnvy/PXCGrGLtuPFEuLLHCKtvOsIvaGGpL/znycMME3mc0GqU6P5xSCxmQcmNg
                    Em9vz0+PPRXTw+KupfB3HPzGZHaTHFZvZYbSFGSVRP3jUbcjiyNZyJ+Je+jALHjG
                    n43lZ3WX7yZtA2pWS25UnhpBig47slU+YSMLaYx8cPKco7h1LNGM7pdffKs3YG7R
                    qnN2h6yfGpfnm3jgAH6xXMjKeKtWLlAp0MN6cXQctu2x+SU1XwWttuukH7MFyCN+
                    6YmBkyXAr+jWGp9vMrxh4w==
                """.trimIndent(),
                "hPfeMh/2Y8VlH+62nz7bd5+bwX2ezSHn1jCO2IkcJio=",
                "eb2f1fb0e4e14539da3f3908c02121ac05ad6c70f49b9b5c216d7cf8250c8f37"
            ),
            Arguments.of(
                """
                    tdCuuv+Wdg2QgIan11opnKsgtrwH8u0Ke5U1p/Wxul9m5kC7CdM9xVQHm/Rr1boR
                    PQS85hEQx4YRxFU0iuj0zTkpiCyB
                """.trimIndent(),
                "RLfITCqfGLuYexuVYFtT",
                "1cad749cc21a498f228bef2dc0abfe67f81d8be0decf795f80c494bc5759c438"
            ),
            Arguments.of(
                """
                    TjB50s1UmD5sSwdznpiqkGlaw7Vr9jZ/VZ+r/7Mz+CfAVtsU8BzwIMdmenqjnv+o
                    JYFDdmS2aF6oWq8JamrXD1wMBcvJ/vgv+FalA7tZhleOlqKM6ZwerPg9u8CzujEJ
                    17HE7u/SzIgIFSpHxANr6vi1hstIzY+et4Or7xw59kzybNbHGX/GUuaaSoSa3WoX
                    AkNivXCVkv17NgjAo/GGVDg/vcXygtE07l2oO9BwbccvQUXQwhsS6de++DSrCTiC
                    8jQKHUuMj4r0MVmoTeiCIFQPgVNaRofjGPuP2R9vKswAd95M1t4dj0zFG/oAz2nt
                    jejP3Zi3lbNYRiGQDeFBQX+aMiKlMaOftroL5xzjAULTW0+vhN3Oww+egHCZ3QUq
                    3CQfY7/YvKYpYIV17u/kQwZsbOCmuA/OqlVx/VmYslQ4EqjrGZEJ9yC51RypAj6Q
                    TLCJK70bCXs/J/UyQVEmWfQKGjLV0U0Fp0HUQAlG0VgsqtPMhS2/iQ6pQ9DFrJwV
                    CE+WF5lwHB+AfIPT1fZ2tkGELo4+Ko3kbrmjMGOtFFPdLjeLTyFBGHwGfm1P89Dw
                    /+jyYpYj5kLL5EpnPbud4nJQ71UPtsQGSWXC0Q5pqBfW+jDZAm3xiXk/w+ollvpS
                    k6qgeol9j01vEDkQyq/ehaIkSCkgrISDmikZlxYUWcF4L9aKoaWprtLriKlo0xWs
                    c4Jt7glDuh7u9roqXvQnfF2x804cavpKPIaHg++or3BwfVsj7pYnfIiLw3RrwyQO
                    W7xCSiPPrQz6+LGeSjkL/192lwDuHDC/XCq4rifWL39x48gf4BwYU/pB6GmMDiLg
                    lQa6T+jUvib0/QeOEL4pBPB4yXHSrUkP19MTDtkcyivDxVXV4J2tRsLwtmwHS6ic
                    FiuLC4yyWf6iDwJCyiqVQGVObt7ah+2/EmmnrLLtr/fS+1bkdGgOAiDw007uNobK
                    V1SMOkYIPFrrrIr4UO7M1h0h9dnefR459jXff2UNr62iW2RSrz4lYGqi9zhfvtGA
                    7deuIyL6LvAVzERWtPWavBpeRmctjAw0/WXmveFJI/tS6rq205fa5y1mX9217FJx
                    gdSDzAxZPiXgc73SIf1eBPUVYHPB4TxHSpa792d1I0MLp4QPeD/fBkH+a/ML9vDd
                    R3jsX/Ew5LPGblsUnDTBoHvCkWFlevf0pSU7NliBUS8MOdqIS/Xw1XgM/f2gxD9Q
                    JQr2HCdyRSvb1x6hQJ2D1mqeJKjHnSzFXMQpGluTG6t73kOUKx2MWQvYNlr9aH0d
                    2SG5uppAN/AcRJtRFayfEVRBRHR86dvcVaXJUg807XhQVSzS1v4yWPPjwydFvie9
                    uY5d1cgfHLAkzPzqqKlUWQ==
                """.trimIndent(),
                """
                    kpA1fpMnJq8WXfcjmUHlMEHoZxXYnn26wuNGhRV5ukFH3AdzgclQJDalz5xFz7K7
                    Kl99bfyK1dDxD4B4Xv2FmTfxMcqckR6fffCTDEPG0a7cibh+aUfmJRdetzEP8ifv
                    bL5lKNJq0BRzRXjWw218WmnYiOLprVMdKXwbEb0ryY0=
                """.trimIndent(),
                "772e7d19e227e8cd313c65a9270377d71307adc21b7389e8544009f27f4d9ca9"
            )
        )
        @JvmStatic
        private fun invalidTestData() = Stream.of(
            Arguments.of(
                """
                    550YQjJdE/OuI/roadBsFMlDM+qfkLEag8JHPj0siMBzLvA76PZyZ2bG82kIobE3
                    Tkdnvy/PXCGrGLtuPFEuLLHCKtvOsIvaGGpL/znycMME3mc0GqU6P5xSCxmQcmNg
                    Em9vz0+PPRXTw+KupfB3HPzGZHaTHFZvZYbSFGSVRP3jUbcjiyNZyJ+Je+jALHjG
                    n43lZ3WX7yZtA2pWS25UnhpBig47slU+YSMLaYx8cPKco7h1LNGM7pdffKs3YG7R
                    qnN2h6yfGpfnm3jgAH6xXMjKeKtWLlAp0MN6cXQctu2x+SU1XwWttuukH7MFyCN+
                    6YmBkyXAr+jWGp9vMrxh4w=
                """.trimIndent(),
                "hPfeMh/2Y8VlH+62nz7bd5+bwX2ezSHn1jCO2IkcJio=",
                "eb2f1fb0e4e14539da3f3908c02121ac05ad6c70f49b9b5c216d7cf8250c8f37"
            ),
            Arguments.of(
                """
                    tdCuuv+Wdg2QgIan11opnKsgtrwH8u0Ke5U1p/Wxul9m5kC7CdM9xVQHm/Rr1boR
                    PQS85hEQx4YRxFU0iuj0zTkpiCyB
                """.trimIndent(),
                "hPfeMh/2Y8VlH+62nz7bd5+bwX2ez",
                "1cad749cc21a498f228bef2dc0abfe67f81d8be0decf795f80c494bc5759c438"
            ),
            Arguments.of(
                """
                    TjB50s1UmD5sSwdznpiqkGlaw7Vr9jZ/VZ+r/7Mz+CfAVtsU8BzwIMdmenqjnv+o
                    JYFDdmS2aF6oWq8JamrXD1wMBcvJ/vgv+FalA7tZhleOlqKM6ZwerPg9u8CzujEJ
                    17HE7u/SzIgIFSpHxANr6vi1hstIzY+et4Or7xw59kzybNbHGX/GUuaaSoSa3WoX
                    AkNivXCVkv17NgjAo/GGVDg/vcXygtE07l2oO9BwbccvQUXQwhsS6de++DSrCTiC
                    8jQKHUuMj4r0MVmoTeiCIFQPgVNaRofjGPuP2R9vKswAd95M1t4dj0zFG/oAz2nt
                    jejP3Zi3lbNYRiGQDeFBQX+aMiKlMaOftroL5xzjAULTW0+vhN3Oww+egHCZ3QUq
                    3CQfY7/YvKYpYIV17u/kQwZsbOCmuA/OqlVx/VmYslQ4EqjrGZEJ9yC51RypAj6Q
                    TLCJK70bCXs/J/UyQVEmWfQKGjLV0U0Fp0HUQAlG0VgsqtPMhS2/iQ6pQ9DFrJwV
                    CE+WF5lwHB+AfIPT1fZ2tkGELo4+Ko3kbrmjMGOtFFPdLjeLTyFBGHwGfm1P89Dw
                    /+jyYpYj5kLL5EpnPbud4nJQ71UPtsQGSWXC0Q5pqBfW+jDZAm3xiXk/w+ollvpS
                    k6qgeol9j01vEDkQyq/ehaIkSCkgrISDmikZlxYUWcF4L9aKoaWprtLriKlo0xWs
                    c4Jt7glDuh7u9roqXvQnfF2x804cavpKPIaHg++or3BwfVsj7pYnfIiLw3RrwyQO
                    W7xCSiPPrQz6+LGeSjkL/192lwDuHDC/XCq4rifWL39x48gf4BwYU/pB6GmMDiLg
                    lQa6T+jUvib0/QeOEL4pBPB4yXHSrUkP19MTDtkcyivDxVXV4J2tRsLwtmwHS6ic
                    FiuLC4yyWf6iDwJCyiqVQGVObt7ah+2/EmmnrLLtr/fS+1bkdGgOAiDw007uNobK
                    V1SMOkYIPFrrrIr4UO7M1h0h9dnefR459jXff2UNr62iW2RSrz4lYGqi9zhfvtGA
                    7deuIyL6LvAVzERWtPWavBpeRmctjAw0/WXmveFJI/tS6rq205fa5y1mX9217FJx
                    gdSDzAxZPiXgc73SIf1eBPUVYHPB4TxHSpa792d1I0MLp4QPeD/fBkH+a/ML9vDd
                    R3jsX/Ew5LPGblsUnDTBoHvCkWFlevf0pSU7NliBUS8MOdqIS/Xw1XgM/f2gxD9Q
                    JQr2HCdyRSvb1x6hQJ2D1mqeJKjHnSzFXMQpGluTG6t73kOUKx2MWQvYNlr9aH0d
                    2SG5uppAN/AcRJtRFayfEVRBRHR86dvcVaXJUg807XhQVSzS1v4yWPPjwydFvie9
                    uY5d1cgfHLAkzPzqqKlUWQ==
                """.trimIndent(),
                """
                    kpA1fpMnJq8WXfcjmUHlMEHoZxXYnn26wuNGhRV5ukFH3AdzgclQJDalz5xFz7K7
                    Kl99bfyK1dDxD4B4Xv2FmTfxMcqckR6fffCTDEPG0a7cibh+aUfmJRdetzEP8ifv
                    bL5lKNJq0BRzRXjWw218WmnYiOLprVMdKXwbEb0ryY0=
                """.trimIndent(),
                "eb2f1fb0e4e14539da3f3908c02121ac05ad6c70f49b9b5c216d7cf8250c8f37"
            )
        )
    }

    private fun getKeycloakWebhookFilter(apiKey: String = UUID.randomUUID().toString()): KeycloakWebhookFilter {
        return KeycloakWebhookFilter(apiKey)
    }

    @Test
    fun checkApiKey_ShouldReturnNull_uriDoesntMatch() {
        // Given
        val mockContainerRequestContext = Mockito.mock(ContainerRequestContext::class.java)
        val mockUriInfo = Mockito.mock(UriInfo::class.java)
        `when`(mockUriInfo.path).thenReturn(UUID.randomUUID().toString())
        `when`(mockContainerRequestContext.uriInfo).thenReturn(mockUriInfo)

        // When
        val result = getKeycloakWebhookFilter().checkApiKey(mockContainerRequestContext)

        // Assert
        Assertions.assertNull(result)
    }

    @Test
    fun checkApiKey_ShouldReturnUnauthorizedResponse_NoHeader() {
        // Given
        val mockContainerRequestContext = Mockito.mock(ContainerRequestContext::class.java)
        val mockUriInfo = Mockito.mock(UriInfo::class.java)
        `when`(mockUriInfo.path).thenReturn(KeycloakWebhookFilter.URI_MATCHER)
        `when`(mockContainerRequestContext.uriInfo).thenReturn(mockUriInfo)
        `when`(mockContainerRequestContext.getHeaderString(KeycloakWebhookFilter.SIGNATURE_HEADER_NAME)).thenReturn(null)

        // When
        val result = getKeycloakWebhookFilter().checkApiKey(mockContainerRequestContext)

        // Assert
        Assertions.assertNotNull(result)
        Assertions.assertEquals(Response.Status.UNAUTHORIZED.statusCode, result!!.status)
    }

    @ParameterizedTest
    @MethodSource("validTestData")
    fun checkApiKey_ShouldReturnNull_ValidInput(data: String, apiKey: String, hash: String) {
        // Given
        val mockContainerRequestContext = Mockito.mock(ContainerRequestContext::class.java)
        val mockUriInfo = Mockito.mock(UriInfo::class.java)
        `when`(mockUriInfo.path).thenReturn(KeycloakWebhookFilter.URI_MATCHER)
        `when`(mockContainerRequestContext.uriInfo).thenReturn(mockUriInfo)
        `when`(mockContainerRequestContext.getHeaderString(KeycloakWebhookFilter.SIGNATURE_HEADER_NAME)).thenReturn(hash)
        `when`(mockContainerRequestContext.entityStream).thenReturn(ByteArrayInputStream(data.toByteArray()))

        // When
        val result = getKeycloakWebhookFilter(apiKey).checkApiKey(mockContainerRequestContext)

        // Assert
        Assertions.assertNull(result)
    }

    @ParameterizedTest
    @MethodSource("invalidTestData")
    fun checkApiKey_ShouldReturnUnauthorizedResponse_InvalidInput(data: String, apiKey: String, hash: String) {
        // Given
        val mockContainerRequestContext = Mockito.mock(ContainerRequestContext::class.java)
        val mockUriInfo = Mockito.mock(UriInfo::class.java)
        `when`(mockUriInfo.path).thenReturn(KeycloakWebhookFilter.URI_MATCHER)
        `when`(mockContainerRequestContext.uriInfo).thenReturn(mockUriInfo)
        `when`(mockContainerRequestContext.getHeaderString(KeycloakWebhookFilter.SIGNATURE_HEADER_NAME)).thenReturn(hash)
        `when`(mockContainerRequestContext.entityStream).thenReturn(ByteArrayInputStream(data.toByteArray()))

        // When
        val result = getKeycloakWebhookFilter(apiKey).checkApiKey(mockContainerRequestContext)

        // Assert
        Assertions.assertNotNull(result)
        Assertions.assertEquals(Response.Status.UNAUTHORIZED.statusCode, result!!.status)
    }
}