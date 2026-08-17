package com.middleeastcontainer.data.network

import com.middleeastcontainer.data.network.mapper.ContainerPayloadBuilder
import com.middleeastcontainer.domain.model.Side
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit

/**
 * Proves the ACTUAL HTTP request the backend receives is byte-compatible with the
 * legacy client — method, path, form-encoding, and (critically) field order. This is
 * the compatibility gate made executable; it runs on the JVM without Android.
 */
class MecrcApiWireCompatibilityTest {

    private lateinit var server: MockWebServer
    private lateinit var api: MecrcApi

    @Before
    fun setUp() {
        server = MockWebServer().also { it.start() }
        api = Retrofit.Builder()
            .baseUrl(server.url("/container_web/"))
            .build()
            .create(MecrcApi::class.java)
    }

    @After fun tearDown() { server.shutdown() }

    @Test
    fun `uploadContainer posts form body with exact legacy field order`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200))

        val fields = ContainerPayloadBuilder.build(
            ContainerPayloadBuilder.Input(
                deviceId = "dev1",
                containerName = "CSQU3054383",
                userName = "insp",
                containerType = "Standard 20",
                sideImagePaths = Side.entries.associateWith { "p_${it.dbName}" },
                sideRemarks = Side.entries.associateWith { "r_${it.dbName}" },
                includeUnderFloor = false,
            ),
            encodeImage = { "ENC_$it" },
        )

        api.uploadContainer(fields)
        val req: RecordedRequest = server.takeRequest()

        assertEquals("POST", req.method)
        assertTrue(req.path!!.endsWith("/container_web/container/test"))
        assertEquals(
            "application/x-www-form-urlencoded",
            req.getHeader("Content-Type")?.substringBefore(";"),
        )

        // Field ORDER on the wire must match the frozen legacy order exactly.
        val wireKeys = req.body.readUtf8().split("&").map { it.substringBefore("=") }
        assertEquals(ContainerPayloadBuilder.expectedFieldOrder(false), wireKeys)
    }

    @Test
    fun `uploadExtraImage posts the seven legacy fields to the extra endpoint`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200))

        api.uploadExtraImage(
            imeiNum = "dev1", containerNo = "CSQU3054383", userName = "insp",
            pictureTime = "2026-07-25 10:00:00", eRemarks = "front", type = "Front Side",
            extraImageBase64 = "BASE64DATA",
        )
        val req = server.takeRequest()

        assertEquals("POST", req.method)
        assertTrue(req.path!!.endsWith("/container_web/container/extra_images"))

        val wireKeys = req.body.readUtf8().split("&").map { it.substringBefore("=") }
        assertEquals(
            listOf("IMEInum", "container_no", "user_name", "picture_time", "ERemarks", "type", "ExtraImage"),
            wireKeys,
        )
    }
}
