package com.middleeastcontainer.ui.components

/**
 * The state contract every screen honours (UI requirement): Loading, Empty,
 * Offline, Error(+retry), Success(content).
 */
sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data object Empty : UiState<Nothing>
    data object Offline : UiState<Nothing>
    data class Error(val message: String) : UiState<Nothing>
    data class Content<T>(val data: T) : UiState<T>
}
