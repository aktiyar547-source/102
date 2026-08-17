package com.middleeastcontainer.di

import com.middleeastcontainer.BuildConfig
import com.middleeastcontainer.core.common.AppConfig
import com.middleeastcontainer.data.network.HostSelectionInterceptor
import com.middleeastcontainer.data.network.MecrcApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides @Singleton
    fun provideAppConfig(): AppConfig = AppConfig(
        mainBaseUrl = BuildConfig.MAIN_BASE_URL,
        extraBaseUrl = BuildConfig.EXTRA_BASE_URL,
        includeUnderFloorInTestPayload = BuildConfig.INCLUDE_UNDER_FLOOR_IN_TEST_PAYLOAD,
        uploadImageMaxEdge = BuildConfig.UPLOAD_IMAGE_MAX_EDGE,
    )

    @Provides @Singleton
    fun provideOkHttp(hostSelection: HostSelectionInterceptor): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.HEADERS
            else HttpLoggingInterceptor.Level.NONE
            redactHeader("ExtraImage")
        }
        return OkHttpClient.Builder()
            // Applies the server address configured in Settings to every request.
            .addInterceptor(hostSelection)
            .addInterceptor(logging)
            // Uploads carry multi-megabyte payloads over mobile networks.
            .connectTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(5, TimeUnit.MINUTES)
            .readTimeout(5, TimeUnit.MINUTES)
            .callTimeout(10, TimeUnit.MINUTES)
            .build()
    }

    @Provides @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    @Provides @Singleton
    fun provideApi(client: OkHttpClient, json: Json, config: AppConfig): MecrcApi =
        Retrofit.Builder()
            // Placeholder only - HostSelectionInterceptor rewrites this per request.
            .baseUrl(config.mainBaseUrl)
            .client(client)
            // The legacy endpoints are form-encoded and need no converter, but the
            // sweep endpoint posts JSON. Without this @Body fails at runtime, not
            // at compile time — the kind of gap a build cannot catch.
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(MecrcApi::class.java)
}
