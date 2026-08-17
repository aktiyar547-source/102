package com.middleeastcontainer.data.database

import com.middleeastcontainer.data.database.entity.CImagesEntity
import com.middleeastcontainer.data.database.entity.RemarksEntity
import com.middleeastcontainer.data.database.entity.TagEntity
import com.middleeastcontainer.domain.model.Side

/**
 * Legacy code built column names into raw SQL (`UPDATE CImages SET '<side>'=...`),
 * which is both injectable and buggy. Room cannot parameterize a column name, so we
 * instead copy the immutable entity with the one target field changed, selected by a
 * `when(side)` — safe, exhaustive, and unit-testable. Values are still bound as
 * parameters by Room's generated @Update.
 */
object SideColumnMapper {

    fun withImage(e: CImagesEntity, side: Side, value: String): CImagesEntity = when (side) {
        Side.FRONT -> e.copy(Front = value)
        Side.FRONT_BOTTOM -> e.copy(Front_Bottom = value)
        Side.FRONT_TOP -> e.copy(Front_Top = value)
        Side.BACK -> e.copy(Back = value)
        Side.BACK_BOTTOM -> e.copy(Back_Bottom = value)
        Side.BACK_TOP -> e.copy(Back_Top = value)
        Side.LEFT -> e.copy(Left = value)
        Side.RIGHT -> e.copy(Right = value)
        Side.INSIDE_BTF -> e.copy(Inside_btf = value)
        Side.INSIDE_FTB -> e.copy(Inside_ftb = value)
        Side.UNDER_FLOOR -> e.copy(Under_Floor = value)
    }

    fun withRemark(e: RemarksEntity, side: Side, value: String): RemarksEntity = when (side) {
        Side.FRONT -> e.copy(Front = value)
        Side.FRONT_BOTTOM -> e.copy(Front_Bottom = value)
        Side.FRONT_TOP -> e.copy(Front_Top = value)
        Side.BACK -> e.copy(Back = value)
        Side.BACK_BOTTOM -> e.copy(Back_Bottom = value)
        Side.BACK_TOP -> e.copy(Back_Top = value)
        Side.LEFT -> e.copy(Left = value)
        Side.RIGHT -> e.copy(Right = value)
        Side.INSIDE_BTF -> e.copy(Inside_btf = value)
        Side.INSIDE_FTB -> e.copy(Inside_ftb = value)
        Side.UNDER_FLOOR -> e.copy(Under_Floor = value)
    }

    fun withTag(e: TagEntity, side: Side, value: String): TagEntity = when (side) {
        Side.FRONT -> e.copy(Front = value)
        Side.FRONT_BOTTOM -> e.copy(Front_Bottom = value)
        Side.FRONT_TOP -> e.copy(Front_Top = value)
        Side.BACK -> e.copy(Back = value)
        Side.BACK_BOTTOM -> e.copy(Back_Bottom = value)
        Side.BACK_TOP -> e.copy(Back_Top = value)
        Side.LEFT -> e.copy(Left = value)
        Side.RIGHT -> e.copy(Right = value)
        Side.INSIDE_BTF -> e.copy(Inside_btf = value)
        Side.INSIDE_FTB -> e.copy(Inside_ftb = value)
        Side.UNDER_FLOOR -> e.copy(Under_Floor = value)
    }

    /** Reads a side's stored image path from a CImages row. */
    fun imageOf(e: CImagesEntity?, side: Side): String? = when (side) {
        Side.FRONT -> e?.Front
        Side.FRONT_BOTTOM -> e?.Front_Bottom
        Side.FRONT_TOP -> e?.Front_Top
        Side.BACK -> e?.Back
        Side.BACK_BOTTOM -> e?.Back_Bottom
        Side.BACK_TOP -> e?.Back_Top
        Side.LEFT -> e?.Left
        Side.RIGHT -> e?.Right
        Side.INSIDE_BTF -> e?.Inside_btf
        Side.INSIDE_FTB -> e?.Inside_ftb
        Side.UNDER_FLOOR -> e?.Under_Floor
    }

    fun remarkOf(e: RemarksEntity?, side: Side): String? = when (side) {
        Side.FRONT -> e?.Front
        Side.FRONT_BOTTOM -> e?.Front_Bottom
        Side.FRONT_TOP -> e?.Front_Top
        Side.BACK -> e?.Back
        Side.BACK_BOTTOM -> e?.Back_Bottom
        Side.BACK_TOP -> e?.Back_Top
        Side.LEFT -> e?.Left
        Side.RIGHT -> e?.Right
        Side.INSIDE_BTF -> e?.Inside_btf
        Side.INSIDE_FTB -> e?.Inside_ftb
        Side.UNDER_FLOOR -> e?.Under_Floor
    }

    fun tagOf(e: TagEntity?, side: Side): String? = when (side) {
        Side.FRONT -> e?.Front
        Side.FRONT_BOTTOM -> e?.Front_Bottom
        Side.FRONT_TOP -> e?.Front_Top
        Side.BACK -> e?.Back
        Side.BACK_BOTTOM -> e?.Back_Bottom
        Side.BACK_TOP -> e?.Back_Top
        Side.LEFT -> e?.Left
        Side.RIGHT -> e?.Right
        Side.INSIDE_BTF -> e?.Inside_btf
        Side.INSIDE_FTB -> e?.Inside_ftb
        Side.UNDER_FLOOR -> e?.Under_Floor
    }
}
