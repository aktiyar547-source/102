package com.middleeastcontainer.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Flat per-image record. Mirrors legacy `Images`. */
@Entity(tableName = "Images")
data class ImagesEntity(
    @PrimaryKey(autoGenerate = true) val Id: Long = 0,
    val Imei_Num: String?,
    val C_Num: String,
    val U_Name: String?,
    val Type: String?,
    val Image: String?,
    val Remarks: String?,
    val Tag: String?,
    val Side: String?,
    val Time: String?,
    val Status: String,
    val CreatedDate: String,
)
