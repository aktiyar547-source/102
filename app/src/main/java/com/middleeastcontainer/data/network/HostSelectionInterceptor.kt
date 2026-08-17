package com.middleeastcontainer.data.network

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.Response
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Rewrites every request onto the base URL currently configured in Settings.
 *
 * Retrofit fixes its baseUrl when the client is built, so this interceptor is what
 * makes the server address changeable at runtime. Endpoint paths ("container/test")
 * are appended to whatever base the user has set.
 */
@Singleton
class HostSelectionInterceptor @Inject constructor(
    private val serverConfig: ServerConfigRepository,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val base = serverConfig.baseUrl.toHttpUrlOrNull()
            ?: return chain.proceed(request)

        // The endpoint path as declared on MecrcApi, e.g. "/container/test".
        val endpointPath = request.url.encodedPath.trimStart('/')

        val newUrl = base.newBuilder()
            .addPathSegments(endpointPath)
            .build()

        Timber.d("POST %s", newUrl)
        return chain.proceed(request.newBuilder().url(newUrl).build())
    }
}
