package com.moterroute.finder.reportAccednt

import com.moterroute.finder.reportAccednt.dataSorce.AccidentRemoteDataSource
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class AccidentRepo @Inject constructor(private val accidentDatabase: AccidentDatabase, private val accidentReportDataSource: AccidentRemoteDataSource) {

    suspend fun reportAccent(location: AccidentReportEntity) {
       return accidentReportDataSource.saveReport(location)
    }

    suspend fun loadReportsByDate(date: String): List<AccidentReportEntity> {
       return accidentReportDataSource.getAccidentReportByDate(date)
    }

    suspend fun removeReportById(date:String,documentId:String){
       return accidentReportDataSource.removeReportById(date,documentId)
    }

}