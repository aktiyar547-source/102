package com.middleeastcontainer.domain.repository

/**
 * Executes the actual network uploads. Implementations reproduce the frozen legacy
 * payloads. Each method returns success/failure so workers can retry.
 */
interface UploadRepository {
    /** Upload one container's inspection data (POST /container/test). */
    suspend fun uploadContainer(containerName: String): Boolean

    /** Upload all pending extra images for a container (POST /container/extra_images). */
    suspend fun uploadExtraImages(containerName: String): Boolean

    /** Mark a container fully uploaded (Status/Status1 = Done). */
    suspend fun markContainerDone(containerName: String)
}
