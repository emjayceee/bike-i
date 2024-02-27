package com.moterroute.finder.reportAccednt.dataSorce

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.moterroute.finder.reportAccednt.AccidentReportEntity
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class AccidentRemoteDataSource @Inject constructor(private val firebaseFirestore: FirebaseFirestore) {
    private val TAG ="AccidentRemoteData"
    suspend fun saveReport(accidentReportEntity: AccidentReportEntity) =
        suspendCoroutine<Unit> { continuation ->
           val collectionId= SimpleDateFormat(
                "dd/MM/yyyy",
                Locale.getDefault()
            ).format(Date(System.currentTimeMillis())).replace(
                "/",
                ","
            )
            firebaseFirestore.collection("Report")
                .document("user1")
                .collection(collectionId)
                .document(
                  accidentReportEntity.documentId )
                .set(accidentReportEntity)
                .addOnSuccessListener {
                    continuation.resume(Unit)
                }
                .addOnFailureListener {
                    continuation.resumeWithException(it)
                }
        }

    suspend fun getAccidentReportByDate(date:String)= suspendCoroutine<List<AccidentReportEntity>>{
        coroutine->
        firebaseFirestore.collection("Report")
            .document("user1")
            .collection(date)
            .get()
            .addOnSuccessListener {
                val list = mutableListOf<AccidentReportEntity>()
                it.documents.forEach {documentSnapShort->
                    list.add(documentSnapShort.toObject(AccidentReportEntity::class.java)!!)
                }
                coroutine.resume(list)
            }
            .addOnFailureListener {
                it.printStackTrace()
                coroutine.resumeWithException(it)
            }
    }

    suspend fun removeReportById(date: String, id: String)= suspendCoroutine<Unit> {coroutine->
        firebaseFirestore.collection("Report")
            .document("user1")
            .collection(date)
            .document(id)
            .delete()
            .addOnSuccessListener {
                coroutine.resume(Unit)
            }.addOnFailureListener {
                coroutine.resumeWithException(it)
            }
    }
}