package com.github.chekist32.user

import com.github.chekist32.parseUserIdOrThrowBadRequest
import io.quarkus.security.Authenticated
import jakarta.transaction.Transactional
import jakarta.validation.Valid
import jakarta.ws.rs.*
import jakarta.ws.rs.core.Context
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import org.eclipse.microprofile.jwt.JsonWebToken
import org.eclipse.microprofile.openapi.annotations.Operation
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType
import org.eclipse.microprofile.openapi.annotations.media.Content
import org.eclipse.microprofile.openapi.annotations.media.Schema
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses
import org.jboss.resteasy.reactive.multipart.FileUpload


@Path("/api/v1/user")
class UserResource(
    private val userService: UserService
) {

    @Schema(type = SchemaType.STRING, format = "binary")
    internal class UploadItemSchema

    @Authenticated
    @GET
    @Path("/profile")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Get the profile data for the authenticated user",
        description = "Fetches the profile data for the user identified by the JWT token."
    )
    @APIResponses(
        value = [
            APIResponse(responseCode = "200", description = "Profile data retrieved successfully",
                content = [Content(mediaType = MediaType.APPLICATION_JSON, schema = Schema(implementation = ProfileDataResponse::class))]),
            APIResponse(responseCode = "401", description = "Unauthorized access")
        ]
    )
    fun getProfileData(@Context token: JsonWebToken): Response {
        val userId = parseUserIdOrThrowBadRequest(token.subject)

        return Response.ok().entity(userService.getProfileData(userId)).build()
    }

    @Transactional
    @Authenticated
    @PUT
    @Path("/profile")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Update the profile data for the authenticated user",
        description = "This endpoint allows an authenticated user to update their profile information."
    )
    @APIResponses(
        value = [
            APIResponse(responseCode = "200", description = "Profile data updated successfully"),
            APIResponse(responseCode = "400", description = "Invalid request body"),
            APIResponse(responseCode = "401", description = "Unauthorized access"),
            APIResponse(responseCode = "500", description = "Internal server error")
        ]
    )
    fun putProfileData(@Valid req: ProfileDataUpdateRequest, @Context token: JsonWebToken): Response {
        val userId = parseUserIdOrThrowBadRequest(token.subject)

        userService.updateProfileData(userId, req)
        return Response.ok().build()
    }

    @Transactional
    @Authenticated
    @PUT
    @Path("/profile/avatar")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Operation(
        summary = "Update the profile avatar for the authenticated user",
        description = "This endpoint allows an authenticated user to upload a new profile avatar."
    )
    @APIResponses(
        value = [
            APIResponse(responseCode = "200", description = "Avatar updated successfully"),
            APIResponse(responseCode = "400", description = "Invalid request body"),
            APIResponse(responseCode = "401", description = "Unauthorized access"),
            APIResponse(responseCode = "500", description = "Internal server error")
        ]
    )
    fun putProfileAvatar(@FormParam("avatar") @Schema(implementation = UploadItemSchema::class) avatar: FileUpload, @Context token: JsonWebToken): Response  {
        val userId = parseUserIdOrThrowBadRequest(token.subject)

        if (!avatar.contentType().startsWith("image/")) {
            return Response.status(Response.Status.UNSUPPORTED_MEDIA_TYPE)
                .entity("Uploaded file is not an image.")
                .build()
        }
        else if (avatar.size() > 1_048_576) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity("Max size of avatar is limited to 1 MB.")
                .build()
        }

        userService.updateUserAvatar(userId, avatar)
        return Response.ok().build()
    }

    @Authenticated
    @GET
    @Path("/profile/notification")
    @Produces(MediaType.TEXT_PLAIN)
    @Operation(
        summary = "Get the user's notification token",
        description = "Retrieve the notification token associated with the current user."
    )
    @APIResponses(
        value = [
            APIResponse(
                responseCode = "200",
                description = "Notification token retrieved successfully",
                content = arrayOf(
                Content(
                    mediaType = MediaType.TEXT_PLAIN,
                    schema = Schema(implementation = String::class)
                )
            )),
            APIResponse(responseCode = "401", description = "Unauthorized access"),
            APIResponse(responseCode = "500", description = "Internal server error")
        ]
    )
    fun getNotificationToken(@Context token: JsonWebToken): Response  {
        val userId = parseUserIdOrThrowBadRequest(token.subject)

        return Response.ok().entity(userService.getNotificationToken(userId)).build()
    }

    @Transactional
    @Authenticated
    @PUT
    @Path("/profile/notification")
    @Produces(MediaType.TEXT_PLAIN)
    @Operation(
        summary = "Regenerate the user's notification token",
        description = "Regenerate a new notification token for the current user."
    )
    @APIResponses(
        value = [
            APIResponse(
                responseCode = "200",
                description = "Notification token regenerated successfully",
                content = arrayOf(
                    Content(
                        mediaType = MediaType.TEXT_PLAIN,
                        schema = Schema(implementation = String::class)
                    )
            )),
            APIResponse(responseCode = "401", description = "Unauthorized access"),
            APIResponse(responseCode = "500", description = "Internal server error")
        ]
    )
    fun putNotificationToken(@Context token: JsonWebToken): Response  {
        val userId = parseUserIdOrThrowBadRequest(token.subject)

        return Response.ok().entity(userService.regenerateNotificationToken(userId)).build()
    }

    @Authenticated
    @GET
    @Path("/profile/donation_data")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Get the donation data for the authenticated user",
        description = "Fetches the donation-related data for the user identified by the JWT token."
    )
    @APIResponses(
        value = [
            APIResponse(responseCode = "200", description = "Donation data retrieved successfully",
                content = [Content(mediaType = MediaType.APPLICATION_JSON, schema = Schema(implementation = DonationProfileDataResponse::class))]),
            APIResponse(responseCode = "401", description = "Unauthorized access"),
            APIResponse(responseCode = "404", description = "User not found"),
            APIResponse(responseCode = "500", description = "Internal server error")
        ]
    )
    fun getDonationData(@Context token: JsonWebToken): Response {
        val userId = parseUserIdOrThrowBadRequest(token.subject)

        return Response.ok().entity(userService.getDonationData(userId)).build()
    }

    @Transactional
    @Authenticated
    @PUT
    @Path("/profile/donation_data")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Update the donation data for the authenticated user",
        description = "Updates the donation-related data for the user identified by the JWT token."
    )
    @APIResponses(
        value = [
            APIResponse(responseCode = "200", description = "Donation data updated successfully"),
            APIResponse(responseCode = "400", description = "Invalid request body"),
            APIResponse(responseCode = "401", description = "Unauthorized access"),
            APIResponse(responseCode = "500", description = "Internal server error")
        ]
    )
    fun updateDonationData(@Valid @RequestBody req: UpdateDonationProfileDataRequest, @Context token: JsonWebToken): Response {
        val userId = parseUserIdOrThrowBadRequest(token.subject)

        userService.updateDonationData(userId, req)
        return Response.ok().build()
    }

    @Transactional
    @Authenticated
    @PUT
    @Path("/profile/donation_data/xmr")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Update the XMR donation data for the authenticated user",
        description = "Updates the XMR donation-related data for the user identified by the JWT token."
    )
    @APIResponses(
        value = [
            APIResponse(responseCode = "200", description = "XMR Donation data updated successfully"),
            APIResponse(responseCode = "400", description = "Invalid request body"),
            APIResponse(responseCode = "401", description = "Unauthorized access"),
            APIResponse(responseCode = "500", description = "Internal server error")
        ]
    )
    fun updateXMRData(@Valid @RequestBody req: UpdateXMRDataRequest, @Context token: JsonWebToken): Response {
        val userId = parseUserIdOrThrowBadRequest(token.subject)

        userService.updateXMRData(userId, req)
        return Response.ok().build()
    }

    @Authenticated
    @GET
    @Path("/profile/regional")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Retrieve regional settings for the user",
        description = "Fetches the regional settings data for the authenticated user.",
    )
    @APIResponses(
        value = [
            APIResponse(responseCode = "200", description = "Regional Settings data retrieved successfully",
                content = [Content(mediaType = MediaType.APPLICATION_JSON, schema = Schema(implementation = RegionalProfileDataResponse::class))]),
            APIResponse(responseCode = "400", description = "Invalid request body"),
            APIResponse(responseCode = "401", description = "Unauthorized access"),
            APIResponse(responseCode = "500", description = "Internal server error")
        ]
    )
    fun getRegionalSettings(@Context token: JsonWebToken): Response {
        val userId = parseUserIdOrThrowBadRequest(token.subject)

        return Response.ok().entity(userService.getRegionalSettings(userId)).build()
    }

    @Transactional
    @Authenticated
    @PUT
    @Path("/profile/regional")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Update regional settings for the user",
        description = "Updates the regional settings data for the authenticated user."
    )
    @APIResponses(
        value = [
            APIResponse(responseCode = "200", description = "Regional Settings data updated successfully"),
            APIResponse(responseCode = "400", description = "Invalid request body"),
            APIResponse(responseCode = "401", description = "Unauthorized access"),
            APIResponse(responseCode = "500", description = "Internal server error")
        ]
    )
    fun putRegionalSettings(@Valid @RequestBody req: RegionalProfileDataRequest, @Context token: JsonWebToken): Response {
        val userId = parseUserIdOrThrowBadRequest(token.subject)

        userService.updateRegionalSettings(userId, req)
        return Response.ok().build()
    }
}