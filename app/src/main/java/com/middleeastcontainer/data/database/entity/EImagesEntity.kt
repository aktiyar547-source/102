package com.middleeastcontainer.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Extra images. Mirrors legacy `EImages`. */
@Entity(tableName = "EImages")
data class EImagesEntity(
    @PrimaryKey(autoGenerate = true) val Id: Long = 0,
    val Name: String,
    val Image: String?,
    val Remarks: String?,
    val Time: String?,
    val Status: String,
    val Type: String?,
    val CreatedDate: String,
)
