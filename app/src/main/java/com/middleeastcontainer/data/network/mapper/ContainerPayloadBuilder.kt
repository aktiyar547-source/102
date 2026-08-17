package com.middleeastcontainer.data.network.mapper

import com.middleeastcontainer.domain.model.Side

/**
 * Builds the ordered field map for POST /container/test, reproducing the legacy
 * Sync payload EXACTLY — including the non-obvious field order recovered from the
 * decompiled source (Back_Top before Back_Bottom; Right before Left; Inside_ftb
 * before Inside_btf). This class is pure Kotlin (no Android types) so it is unit
 * tested directly by the golden-payload compatibility suite.
 *
 * The image-encoding step is injected as a lambda so tests can assert field NAMES
 * and ORDER without needing Android's Bitmap/Base64.
 */
object ContainerPayloadBuilder {

    /**
     * Legacy per-side emission order for the /container/test payload.
     * Under_Floor is intentionally absent (Q8/L5) and appended only when enabled.
     */
    private val TEST_PAYLOAD_SIDE_ORDER: List<Side> = listOf(
        Side.FRONT,
        Side.FRONT_BOTTOM,
        Side.FRONT_TOP,
        Side.BACK,
        Side.BACK_TOP,      // legacy emits Back_Top BEFORE Back_Bottom
        Side.BACK_BOTTOM,
        Side.RIGHT,         // legacy emits Right BEFORE Left
        Side.LEFT,
        Side.INSIDE_FTB,    // legacy emits Inside_ftb BEFORE Inside_btf
        Side.INSIDE_BTF,
    )

    data class Input(
        val deviceId: String,          // wire: IMEInum
        val containerName: String,     // wire: container_name
        val userName: String,          // wire: user_name
        val containerType: String,     // wire: container_type
        val sideImagePaths: Map<Side, String?>,
        val sideRemarks: Map<Side, String?>,
        val includeUnderFloor: Boolean = false,
    )

    /**
     * @param encodeImage maps a stored image path to its Base64 JPEG string. For a missing/null path, an empty string is sent,
     *        matching legacy behaviour when a side was never captured.
     * @return a LinkedHashMap preserving insertion (== wire) order.
     */
    fun build(input: Input, encodeImage: (String) -> String): LinkedHashMap<String, String> {
        val fields = LinkedHashMap<String, String>()
        fields["IMEInum"] = input.deviceId
        fields["container_name"] = input.containerName
        fields["user_name"] = input.userName
        fields["container_type"] = input.containerType

        val order = TEST_PAYLOAD_SIDE_ORDER.toMutableList()
        if (input.includeUnderFloor) order += Side.UNDER_FLOOR

        for (side in order) {
            val path = input.sideImagePaths[side]
            val encoded = if (path.isNullOrEmpty()) "" else encodeImage(path)
            fields[side.dbName] = encoded
            fields[remarkFieldName(side)] = input.sideRemarks[side].orEmpty()
        }
        return fields
    }

    /** Remark field name for a side, e.g. Front -> Front_Remarks. */
    private fun remarkFieldName(side: Side): String = "${side.dbName}_Remarks"

    /** Exposed for the compatibility test to assert the exact expected key order. */
    fun expectedFieldOrder(includeUnderFloor: Boolean): List<String> {
        val keys = mutableListOf("IMEInum", "container_name", "user_name", "container_type")
        val order = TEST_PAYLOAD_SIDE_ORDER.toMutableList()
        if (includeUnderFloor) order += Side.UNDER_FLOOR
        for (side in order) {
            keys += side.dbName
            keys += "${side.dbName}_Remarks"
        }
        return keys
    }
}
