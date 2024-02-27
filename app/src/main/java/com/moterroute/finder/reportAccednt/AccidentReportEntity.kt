package com.moterroute.finder.reportAccednt

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.firestore.Exclude
import java.text.SimpleDateFormat
import java.util.*

@Entity
class AccidentReportEntity(
    var address:String?="",
    val locationLat:Double=0.0,
    val locationLng:Double=0.0,
    val date : String="",
    @Exclude
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    var documentId:String="",
    val localTimeStamp:Long  = System.currentTimeMillis()
) {

    fun timeStampToHumanReadAbleTime():String{
        val dateFormat = SimpleDateFormat("hh:mm:ss a", Locale.getDefault())
        return dateFormat.format(Date(localTimeStamp))
    }

}