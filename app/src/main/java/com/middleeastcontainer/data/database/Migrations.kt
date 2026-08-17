package com.middleeastcontainer.data.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Schema migrations.
 *
 * Additive rather than destructive: an inspector may already have captured
 * containers that have not been uploaded, and dropping the database to add
 * inventory tables would destroy work that exists nowhere else.
 */
object Migrations {

    /** v1 -> v2: adds the inventory sweep tables. Nothing existing is touched. */
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS Sweep (
                    Id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    Zone TEXT NOT NULL,
                    StartedBy TEXT NOT NULL,
                    StartedAt TEXT NOT NULL,
                    FinishedAt TEXT,
                    Status TEXT NOT NULL DEFAULT 'Upload'
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS Sighting (
                    Id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    SweepId INTEGER NOT NULL,
                    ContainerNumber TEXT NOT NULL,
                    PhotoPath TEXT,
                    SeenAt TEXT NOT NULL,
                    FromOcr INTEGER NOT NULL DEFAULT 1,
                    Status TEXT NOT NULL DEFAULT 'Upload',
                    FOREIGN KEY(SweepId) REFERENCES Sweep(Id) ON DELETE CASCADE
                )
                """.trimIndent()
            )
            // Unique index is the deduplication: re-seeing a unit inserts once.
            db.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS index_Sighting_SweepId_ContainerNumber" +
                    " ON Sighting (SweepId, ContainerNumber)"
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS index_Sighting_SweepId ON Sighting (SweepId)"
            )
        }
    }

    /**
     * v2 -> v3: records containers seen in a frame but not readable.
     *
     * Must mirror UnreadEntity exactly. Room compares the migrated schema against
     * the entities on the first open after an upgrade and throws if they differ,
     * so a table that merely looks right is not good enough.
     */
    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS Unread (
                    Id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    SweepId INTEGER NOT NULL,
                    Tag TEXT NOT NULL,
                    Partial TEXT NOT NULL,
                    PhotoPath TEXT,
                    BoxLeft REAL NOT NULL,
                    BoxTop REAL NOT NULL,
                    BoxRight REAL NOT NULL,
                    BoxBottom REAL NOT NULL,
                    SeenAt TEXT NOT NULL,
                    ResolvedBy TEXT,
                    FOREIGN KEY(SweepId) REFERENCES Sweep(Id) ON DELETE CASCADE
                )
                """.trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS index_Unread_SweepId ON Unread (SweepId)")
        }
    }

    val ALL = arrayOf(MIGRATION_1_2, MIGRATION_2_3)
}
