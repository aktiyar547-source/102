package com.middleeastcontainer.data.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Legacy `CImages` table: 11 flat side columns keyed by container Name. */
@Entity(tableName = "CImages", indices = [Index(value = ["Name"], unique = true)])
data class CImagesEntity(
    @PrimaryKey(autoGenerate = true) val Id: Long = 0,
    val Remarks: String? = null,
    val Front: String? = null,
    val Front_Bottom: String? = null,
    val Front_Top: String? = null,
    val Back: String? = null,
    val Back_Bottom: String? = null,
    val Back_Top: String? = null,
    val Left: String? = null,
    val Right: String? = null,
    val Inside_btf: String? = null,
    val Inside_ftb: String? = null,
    val Under_Floor: String? = null,
    val Name: String,
    val CreatedDate: String,
)
