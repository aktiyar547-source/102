package com.middleeastcontainer.domain.model

/**
 * The 11 canonical inspection sides, reproduced exactly from the legacy app.
 *
 * - [dbName]  : the legacy database column / wire field name (do NOT change).
 * - [label]   : the on-screen "new" label shown to inspectors.
 * - [inTestPayload] : whether this side is sent in the /container/test payload.
 *   All 11 sides are now sent; the MECRC server accepts them all.
 */
enum class Side(val dbName: String, val label: String, val inTestPayload: Boolean) {
    FRONT("Front", "Front_Panel", true),
    FRONT_BOTTOM("Front_Bottom", "Front_Bottom_Sill", true),
    FRONT_TOP("Front_Top", "Front_Top_Rail", true),
    BACK("Back", "Rear", true),
    BACK_BOTTOM("Back_Bottom", "Rear_Bottom_Sill", true),
    BACK_TOP("Back_Top", "Rear_Header", true),
    LEFT("Left", "Left_Side_Panel", true),
    RIGHT("Right", "Right_Side_Panel", true),
    INSIDE_BTF("Inside_btf", "Interior_Complete", true),
    INSIDE_FTB("Inside_ftb", "Interior_Floor", true),
    UNDER_FLOOR("Under_Floor", "Under_Floor", true);

    companion object {
        /** Grid display order (matches the legacy side grid). */
        val gridOrder: List<Side> = entries.toList()
    }
}
