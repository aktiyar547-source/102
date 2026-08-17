package com.middleeastcontainer.core.common

import kotlinx.coroutines.CoroutineDispatcher

/** Injectable dispatchers so use cases and repositories stay testable. */
interface DispatcherProvider {
    val io: CoroutineDispatcher
    val default: CoroutineDispatcher
    val main: CoroutineDispatcher
}
