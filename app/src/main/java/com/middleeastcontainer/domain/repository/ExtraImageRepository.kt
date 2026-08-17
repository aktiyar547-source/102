package com.middleeastcontainer.domain.repository

import com.middleeastcontainer.domain.model.ExtraImage

interface ExtraImageRepository {
    suspend fun forContainer(containerName: String): List<ExtraImage>
    suspend fun add(containerName: String, imageRelativePath: String, remark: String, category: String)
    suspend fun pendingCount(containerName: String): Int
}
