package com.moterroute.finder.api

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import retrofit2.Retrofit

@Module
@InstallIn(ActivityComponent::class)
object AnalyticsModule {

    @Provides
    fun provideService(
        // Potential dependencies of this type
    ): DirectionsService {
        return Retrofit.Builder()
            .baseUrl("https://example.com")
            .build()
            .create(DirectionsService::class.java)
    }
}
