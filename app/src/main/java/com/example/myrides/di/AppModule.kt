package com.example.myrides.di

import com.example.myrides.data.repository.ExampleRepository
import com.example.myrides.data.repository.ExampleRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Singleton
    @Provides
    fun provideExampleRepository(): ExampleRepository {
        return ExampleRepositoryImpl()
    }
}
