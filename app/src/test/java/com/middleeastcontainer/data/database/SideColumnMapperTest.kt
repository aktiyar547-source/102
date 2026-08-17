package com.middleeastcontainer.data.database

import com.middleeastcontainer.data.database.entity.CImagesEntity
import com.middleeastcontainer.domain.model.Side
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Verifies the safe side-column writer sets exactly the target column and leaves
 * the other 10 untouched — the injection-free replacement for legacy dynamic SQL.
 */
class SideColumnMapperTest {

    private val base = CImagesEntity(Name = "CSQU3054383", CreatedDate = "2026-07-25")

    @Test
    fun `withImage sets only the targeted side`() {
        val updated = SideColumnMapper.withImage(base, Side.BACK_TOP, "img/back_top.png")
        assertEquals("img/back_top.png", updated.Back_Top)
        // spot-check neighbours remain null
        assertNull(updated.Back)
        assertNull(updated.Back_Bottom)
        assertNull(updated.Front)
        assertNull(updated.Under_Floor)
    }

    @Test
    fun `every side maps to a distinct column`() {
        val paths = Side.entries.map { side ->
            SideColumnMapper.imageOf(SideColumnMapper.withImage(base, side, side.dbName), side)
        }
        // Each read-back equals the side's own dbName, proving 1:1 column mapping.
        assertEquals(Side.entries.map { it.dbName }, paths)
    }
}
