package com.middleeastcontainer.data.network

import com.middleeastcontainer.data.network.mapper.ContainerPayloadBuilder
import com.middleeastcontainer.domain.model.Side
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Golden-payload compatibility test. Locks the exact /container/test field set and
 * ORDER recovered from the legacy Sync source, so a future refactor cannot silently
 * change what the backend receives.
 */
class ContainerPayloadBuilderTest {

    private fun sampleInput(includeUnderFloor: Boolean) = ContainerPayloadBuilder.Input(
        deviceId = "device-123",
        containerName = "CSQU3054383",
        userName = "inspector1",
        containerType = "Standard 20",
        sideImagePaths = Side.entries.associateWith { "path/${it.dbName}.png" },
        sideRemarks = Side.entries.associateWith { "remark-${it.dbName}" },
        includeUnderFloor = includeUnderFloor,
    )

    // Encoder stub: returns a deterministic token so we assert names/order, not bytes.
    private val fakeEncoder: (String) -> String = { path -> "ENC($path)" }

    @Test
    fun `default payload matches legacy field order and omits Under_Floor`() {
        val fields = ContainerPayloadBuilder.build(sampleInput(false), fakeEncoder)

        val expectedOrder = listOf(
            "IMEInum", "container_name", "user_name", "container_type",
            "Front", "Front_Remarks",
            "Front_Bottom", "Front_Bottom_Remarks",
            "Front_Top", "Front_Top_Remarks",
            "Back", "Back_Remarks",
            "Back_Top", "Back_Top_Remarks",       // Back_Top BEFORE Back_Bottom
            "Back_Bottom", "Back_Bottom_Remarks",
            "Right", "Right_Remarks",              // Right BEFORE Left
            "Left", "Left_Remarks",
            "Inside_ftb", "Inside_ftb_Remarks",    // Inside_ftb BEFORE Inside_btf
            "Inside_btf", "Inside_btf_Remarks",
        )
        assertEquals(expectedOrder, fields.keys.toList())
        assertEquals(expectedOrder, ContainerPayloadBuilder.expectedFieldOrder(false))
    }

    @Test
    fun `header fields carry through unchanged`() {
        val fields = ContainerPayloadBuilder.build(sampleInput(false), fakeEncoder)
        assertEquals("device-123", fields["IMEInum"])
        assertEquals("CSQU3054383", fields["container_name"])
        assertEquals("inspector1", fields["user_name"])
        assertEquals("Standard 20", fields["container_type"])
    }

    @Test
    fun `images are encoded and remarks passed through`() {
        val fields = ContainerPayloadBuilder.build(sampleInput(false), fakeEncoder)
        assertEquals("ENC(path/Front.png)", fields["Front"])
        assertEquals("remark-Front", fields["Front_Remarks"])
    }

    @Test
    fun `missing side image yields empty string not an encode call`() {
        val input = sampleInput(false).copy(
            sideImagePaths = mapOf(Side.FRONT to null) // others absent -> null
        )
        val fields = ContainerPayloadBuilder.build(input) { "SHOULD_NOT_BE_CALLED" }
        assertEquals("", fields["Front"])
        assertEquals("", fields["Back"])
    }

    @Test
    fun `Under_Floor appended only when flag enabled (Q8)`() {
        val fields = ContainerPayloadBuilder.build(sampleInput(true), fakeEncoder)
        val keys = fields.keys.toList()
        assertEquals("Under_Floor", keys[keys.size - 2])
        assertEquals("Under_Floor_Remarks", keys.last())
    }
}
