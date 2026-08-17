package com.middleeastcontainer.di

import com.middleeastcontainer.core.common.Clock
import com.middleeastcontainer.core.common.SystemClock
import com.middleeastcontainer.data.ocr.MlKitContainerOcrEngine
import com.middleeastcontainer.data.repository.ContainerRepositoryImpl
import com.middleeastcontainer.data.repository.InventoryRepositoryImpl
import com.middleeastcontainer.data.repository.ExtraImageRepositoryImpl
import com.middleeastcontainer.data.repository.SideCaptureRepositoryImpl
import com.middleeastcontainer.data.repository.UploadRepositoryImpl
import com.middleeastcontainer.data.session.SessionRepositoryImpl
import com.middleeastcontainer.domain.repository.ContainerRepository
import com.middleeastcontainer.domain.repository.ExtraImageRepository
import com.middleeastcontainer.domain.repository.SessionRepository
import com.middleeastcontainer.domain.repository.SideCaptureRepository
import com.middleeastcontainer.domain.repository.InventoryRepository
import com.middleeastcontainer.domain.repository.UploadRepository
import com.middleeastcontainer.domain.ocr.ContainerOcrEngine
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds @Singleton
    abstract fun bindClock(impl: SystemClock): Clock

    @Binds @Singleton
    abstract fun bindSessionRepository(impl: SessionRepositoryImpl): SessionRepository

    @Binds @Singleton
    abstract fun bindContainerRepository(impl: ContainerRepositoryImpl): ContainerRepository

    @Binds @Singleton
    abstract fun bindSideCaptureRepository(impl: SideCaptureRepositoryImpl): SideCaptureRepository

    @Binds @Singleton
    abstract fun bindExtraImageRepository(impl: ExtraImageRepositoryImpl): ExtraImageRepository

    @Binds @Singleton
    abstract fun bindUploadRepository(impl: UploadRepositoryImpl): UploadRepository

    @Binds @Singleton
    abstract fun bindInventoryRepository(impl: InventoryRepositoryImpl): InventoryRepository

    @Binds @Singleton
    abstract fun bindOcrEngine(impl: MlKitContainerOcrEngine): ContainerOcrEngine
}
