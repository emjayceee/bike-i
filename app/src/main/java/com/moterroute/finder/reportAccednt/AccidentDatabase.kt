package com.moterroute.finder.reportAccednt

import androidx.room.Database
import androidx.room.RoomDatabase
import com.moterroute.finder.reportAccednt.dataSorce.ReportAccidentDao

@Database(entities = [AccidentReportEntity::class], version = 1)
abstract class AccidentDatabase : RoomDatabase() {
    abstract fun reportAccidentDao(): ReportAccidentDao
}
