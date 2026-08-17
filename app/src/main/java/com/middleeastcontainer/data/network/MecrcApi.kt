package com.middleeastcontainer.data.network

import com.middleeastcontainer.data.network.dto.SweepPayload
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Field
import retrofit2.http.FieldMap
import retrofit2.http.FormUrlEncoded
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Frozen legacy wire contract. Both endpoints are application/x-www-form-urlencoded
 * POSTs; responses are opaque (legacy ignored them), so we return raw ResponseBody
 * and treat HTTP 2xx as success.
 */
interface MecrcApi {

    /**
     * Uploads a finished yard sweep.
     *
     * JSON rather than the legacy form encoding, because a sweep is a list of
     * unknown length and nothing legacy consumes this endpoint — there is no
     * contract here to preserve.
     */
    @POST("inventory/sweep")
    suspend fun uploadSweep(@Body sweep: SweepPayload): Response<ResponseBody>

    /**
     * Main inspection upload (legacy Sync -> /container/test, revived per Q2/L1).
     * Field set, names and ORDER are supplied by [ContainerPayloadBuilder] to
     * guarantee byte-parity with legacy. Images are Base64 JPEG (DEFAULT flags).
     */
    @FormUrlEncoded
    @POST("container/test")
    suspend fun uploadContainer(@FieldMap fields: Map<String, String>): Response<ResponseBody>

    /**
     * Extra-image upload (/container/extra_images). Host corrected off the legacy
     * LAN IP (Q2/L2). ExtraImage is a downsampled Base64 JPEG (DEFAULT flags).
     */
    @FormUrlEncoded
    @POST("container/extra_images")
    suspend fun uploadExtraImage(
        @Field("IMEInum") imeiNum: String,
        @Field("container_no") containerNo: String,
        @Field("user_name") userName: String,
        @Field("picture_time") pictureTime: String,
        @Field("ERemarks") eRemarks: String,
        @Field("type") type: String,
        @Field("ExtraImage") extraImageBase64: String,
    ): Response<ResponseBody>
}
