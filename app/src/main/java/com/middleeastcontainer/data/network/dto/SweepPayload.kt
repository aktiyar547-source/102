package com.middleeastcontainer.data.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A finished sweep on the wire.
 *
 * Photos are sent once and referenced by key rather than embedded per unit. One
 * frame commonly carries eight containers, and inlining the image against each
 * would send the same megabyte eight times — five to seven times the payload for
 * a normal sweep.
 */
@Serializable
data class SweepPayload(
    @SerialName("IMEInum") val deviceId: String,
    val zone: String,
    @SerialName("user_name") val userName: String,
    @SerialName("started_at") val startedAt: String,
    @SerialName("finished_at") val finishedAt: String?,
    /** Key -> Base64 JPEG. Each distinct frame appears exactly once. */
    val photos: Map<String, String>,
    val units: List<SweepUnit>,
)

@Serializable
data class SweepUnit(
    @SerialName("container_no") val containerNo: String,
    @SerialName("seen_at") val seenAt: String,
    @SerialName("from_ocr") val fromOcr: Boolean,
    /** Key into [SweepPayload.photos]; null when the unit was typed by hand. */
    @SerialName("photo_ref") val photoRef: String? = null,
)
