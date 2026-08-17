package com.middleeastcontainer.ui.menu

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * The legacy Menu triggered a data purge on open; in v1 that is a scheduled
 * WorkManager job (see MecrcApp/UploadScheduler), so this ViewModel is intentionally
 * thin. Kept for future menu-level state (e.g. pending-upload badges).
 */
@HiltViewModel
class MenuViewModel @Inject constructor() : ViewModel()
