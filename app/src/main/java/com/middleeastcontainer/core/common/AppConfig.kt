package com.middleeastcontainer.core.common

/**
 * Runtime-resolvable backend configuration. Defaults come from BuildConfig (the
 * frozen legacy contract); an in-app settings override can replace host/scheme
 * once the production endpoint is confirmed (Q2).
 */
data class AppConfig(
    val mainBaseUrl: String,
    val extraBaseUrl: String,
    val includeUnderFloorInTestPayload: Boolean,
    /**
     * Longest edge (px) for uploaded photos. The legacy backend only ever received
     * ~324px images; keeping this modest is what keeps a 10-side POST under PHP's
     * default 8 MB post_max_size. Raise it only after testing the real server.
     */
    val uploadImageMaxEdge: Int,
)
