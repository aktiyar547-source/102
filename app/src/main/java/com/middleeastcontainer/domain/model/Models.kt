package com.middleeastcontainer.domain.model

/** Domain models (UI/data agnostic). */

data class Container(
    val name: String,          // ISO 6346 container number (unique key)
    val type: String,          // ContainerType.wire
    val date: String,          // dd-MMMM-yyyy (legacy display format)
    val status: String,        // "Upload" | "Done"
    val username: String?,
    val deviceId: String?,     // generated install UUID (wire field: IMEInum)
    val uploadStatus: String,  // Status1: "Upload" | "Done"
    val createdDate: String,   // yyyy-MM-dd
    val extraImageCount: Int = 0,
)

data class SideCapture(
    val side: Side,
    val imagePath: String?,    // relative path in app-scoped storage (null = not captured)
    val remark: String?,
    val captured: Boolean,
)

data class ExtraImage(
    val id: Long,
    val containerName: String,
    val imagePath: String?,
    val remark: String?,
    val time: String?,
    val status: String,
    val category: String?,
)

data class Session(
    val loggedIn: Boolean,
    val username: String?,
    val deviceId: String?,
)
