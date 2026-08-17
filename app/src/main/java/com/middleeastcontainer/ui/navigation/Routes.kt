package com.middleeastcontainer.ui.navigation

/** Central route table. Args are URL-encoded container names / side db-names / types. */
object Routes {
    /** Shown before anything else until the device is activated. */
    const val ACTIVATION = "activation"

    const val LOGIN = "login"
    const val MENU = "menu"
    const val OCR = "ocr"
    const val SETTINGS = "settings"
    const val INVENTORY = "inventory"

    /** A yard sweep in progress. */
    const val SWEEP = "sweep/{sweepId}"
    fun sweep(id: Long) = "sweep/$id"
    const val PREVIEW = "preview"
    const val DELETE = "delete"
    const val UPLOAD = "upload"

    const val DIMENSION = "dimension/{container}/{type}"
    fun dimension(container: String, type: String) = "dimension/$container/$type"

    const val SINGLE_SIDE = "single/{container}/{type}/{side}"
    fun singleSide(container: String, type: String, side: String) = "single/$container/$type/$side"

    /**
     * Straight to the camera. [target] is a side's dbName, or CaptureViewModel.EXTRA
     * for a loose extra image. Tapping a row opens this directly — no form first.
     */
    const val CAMERA = "camera/{container}/{target}"
    fun camera(container: String, target: String) = "camera/$container/$target"

    const val VIEW_EXTRA = "extra_view/{container}"
    fun viewExtra(container: String) = "extra_view/$container"
}
