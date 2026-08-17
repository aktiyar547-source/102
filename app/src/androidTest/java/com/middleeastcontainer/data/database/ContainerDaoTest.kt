package com.middleeastcontainer.data.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.middleeastcontainer.data.database.entity.ContainerEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented Room test (runs on device/emulator). Verifies the upload lifecycle
 * and the retention purge query behave as designed: markDone flips Status1, and
 * purge removes only uploaded rows strictly older than the cutoff.
 */
@RunWith(AndroidJUnit4::class)
class ContainerDaoTest {

    private lateinit var db: MecrcDatabase
    private lateinit var dao: com.middleeastcontainer.data.database.dao.ContainerDao

    @Before fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(), MecrcDatabase::class.java
        ).build()
        dao = db.containerDao()
    }

    @After fun tearDown() = db.close()

    private fun entity(name: String, status1: String, created: String) = ContainerEntity(
        Name = name, Type = "Standard 20", Date = "25-July-2026", Status = status1,
        Username = "insp", IMEInum = "dev1", Status1 = status1, CreatedDate = created,
    )

    @Test fun markDone_flips_upload_status() = runTest {
        dao.insert(entity("CSQU3054383", "Upload", "2026-07-25"))
        dao.markDone("CSQU3054383")
        assertEquals("Done", dao.findByName("CSQU3054383")!!.Status1)
    }

    @Test fun purge_removes_only_uploaded_rows_before_cutoff() = runTest {
        dao.insert(entity("AAAU0000000", "Done", "2026-07-10"))   // old + uploaded -> purged
        dao.insert(entity("BBBU0000000", "Upload", "2026-07-10")) // old but pending -> kept
        dao.insert(entity("CCCU0000000", "Done", "2026-07-24"))   // recent + uploaded -> kept

        dao.purgeUploadedBefore("2026-07-18")

        assertNull(dao.findByName("AAAU0000000"))
        assertEquals("Upload", dao.findByName("BBBU0000000")?.Status1)
        assertEquals("Done", dao.findByName("CCCU0000000")?.Status1)
    }
}
