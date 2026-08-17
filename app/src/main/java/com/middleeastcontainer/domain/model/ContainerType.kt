package com.middleeastcontainer.domain.model

/** The 11 container types, exact legacy wire strings and order (spinner parity). */
enum class ContainerType(val wire: String) {
    STANDARD_20("Standard 20"),
    STANDARD_40("Standard 40"),
    OPEN_TOP_20("Open Top 20"),
    OPEN_TOP_40("Open Top 40"),
    HI_CUBE_20("HI Cube 20"),
    HI_CUBE_40("HI Cube 40"),
    HI_CUBE_45("HI Cube 45"),
    REEFER_20("Reefer 20"),
    REEFER_40("Reefer 40"),
    FLAT_RACK_20("Flat rack 20"),
    FLAT_RACK_40("Flat rack 40");

    companion object {
        val wireValues: List<String> = entries.map { it.wire }
        fun fromWire(value: String): ContainerType? = entries.firstOrNull { it.wire == value }
    }
}
