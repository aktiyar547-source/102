package com.middleeastcontainer.data.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Master inspection record. Column names mirror legacy `Container` exactly. */
@Entity(tableName = "Container", indices = [Index(value = ["Name"], unique = true)])
data class ContainerEntity(
    @PrimaryKey(autoGenerate = true) val Id: Long = 0,
    val Name: String,
    val Type: String,
    val Date: String,
    val Status: String,
    val Username: String?,
    val IMEInum: String?,   // now the generated install UUID (wire-compatible field name)
    val Status1: String,    // upload lifecycle: "Upload" -> "Done"
    val CreatedDate: String,
)
