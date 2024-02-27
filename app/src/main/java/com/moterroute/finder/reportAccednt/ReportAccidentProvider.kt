package com.moterroute.finder.reportAccednt

import android.content.Context
import androidx.room.Room
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object ReportAccidentProvider {
    @Provides
    fun provideReportAccidentDataBase(@ApplicationContext appContext: Context): AccidentDatabase {
        return Room.databaseBuilder(
            appContext,
            AccidentDatabase::class.java, "accident-database"
        ).build()

    }
    @Provides
    fun providerReportFirebase():FirebaseFirestore{
        return Firebase.firestore
    }
}