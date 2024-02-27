package com.moterroute.finder.providerHilt

import com.google.maps.GeoApiContext
import com.moterroute.finder.api.DirectionsService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton


@Module
@InstallIn(ActivityComponent::class)
object GeoApiContextProvider {

    @Provides
    fun provideGeoApiContext(
    ): GeoApiContext {
        return GeoApiContext.Builder()
            .apiKey("AIzaSyBhEVkpM82FqGfCBXgb5yiyOfMn_yHt23I")
            .build();
    }
}
