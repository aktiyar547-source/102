package com.middleeastcontainer.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.middleeastcontainer.data.database.dao.ContainerDao
import com.middleeastcontainer.data.database.dao.ExtraImageDao
import com.middleeastcontainer.data.database.dao.ImagesDao
import com.middleeastcontainer.data.database.dao.InventoryDao
import com.middleeastcontainer.data.database.dao.SideTablesDao
import com.middleeastcontainer.data.database.entity.CImagesEntity
import com.middleeastcontainer.data.database.entity.ContainerEntity
import com.middleeastcontainer.data.database.entity.EImagesEntity
import com.middleeastcontainer.data.database.entity.ImagesEntity
import com.middleeastcontainer.data.database.entity.SightingEntity
import com.middleeastcontainer.data.database.entity.SweepEntity
import com.middleeastcontainer.data.database.entity.UnreadEntity
import com.middleeastcontainer.data.database.entity.RemarksEntity
import com.middleeastcontainer.data.database.entity.TagEntity

/**
 * Fresh v1 database (Q5). Version starts at 1 for the new schema line; no legacy
 * import (legacy self-purges every 2 days and never durably persisted uploads).
 * Real migrations are added going forward — we never drop-and-recreate like legacy.
 */
@Database(
    entities = [
        ContainerEntity::class,
        CImagesEntity::class,
        RemarksEntity::class,
        TagEntity::class,
        EImagesEntity::class,
        ImagesEntity::class,
        SweepEntity::class,
        SightingEntity::class,
        UnreadEntity::class,
    ],
    version = 3,
    exportSchema = true,
)
abstract class MecrcDatabase : RoomDatabase() {
    abstract fun containerDao(): ContainerDao
    abstract fun sideTablesDao(): SideTablesDao
    abstract fun extraImageDao(): ExtraImageDao
    abstract fun imagesDao(): ImagesDao

    abstract fun inventoryDao(): InventoryDao
}
