package com.middleeastcontainer.core.common

/** Centralized constants — no magic strings/values scattered in code. */
object Constants {
    const val DB_NAME = "MiddleEast_Container.db"
    const val PREFS_STORE = "mecrc_secure_prefs"

    // SharedPreferences-equivalent keys, kept for continuity with legacy semantics.
    const val KEY_LOGGED_IN = "login"
    const val KEY_USERNAME = "username"
    const val KEY_DEVICE_ID = "imeinumber" // now a generated UUID (Q4)

    /** Top-level photo folder, visible when the phone is browsed or plugged in. */
    const val IMAGE_ROOT_DIR = "OCR2"

    /** Inspections and yard counts are kept apart under it. */
    const val INSPECTION_DIR = "Inspection"
    const val INVENTORY_DIR = "Inventory"

    // Upload lifecycle status values (must match legacy strings).
    const val STATUS_PENDING = "Upload"
    const val STATUS_DONE = "Done"

    // Housekeeping (Q7): purge uploaded inspections older than this many days.
    const val RETENTION_DAYS = 7L
}
