package com.middleeastcontainer.domain.repository

import com.middleeastcontainer.domain.model.Side
import com.middleeastcontainer.domain.model.SideCapture

interface SideCaptureRepository {
    suspend fun sidesFor(containerName: String): List<SideCapture>

    /** Persist a captured side image path + remark (legacy updateimages, parameterized). */
    suspend fun saveSide(containerName: String, side: Side, imageRelativePath: String, remark: String)

    /**
     * Updates only the remark. Deliberately leaves the image path and the
     * captured flag alone, so a note can be attached to a side that has no photo
     * without the side appearing photographed.
     */
    suspend fun saveRemark(containerName: String, side: Side, remark: String)
}
