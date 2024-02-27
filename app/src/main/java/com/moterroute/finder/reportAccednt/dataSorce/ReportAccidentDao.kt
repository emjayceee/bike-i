package com.moterroute.finder.reportAccednt.dataSorce

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.moterroute.finder.reportAccednt.AccidentReportEntity
import kotlinx.coroutines.flow.Flow


@Dao
interface ReportAccidentDao {
    @Insert
    fun insertAll( users: AccidentReportEntity)

    @Query("SELECT * FROM accidentreportentity")
    fun loadAllReportsByDate(): Flow<List<AccidentReportEntity>>

    @Query("DELETE FROM accidentreportentity WHERE id = :id")
    fun removeById(id: Int)
}