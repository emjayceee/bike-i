package com.moterroute.finder.reportAccednt

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moterroute.finder.dataSource.ResponseResult
import com.moterroute.finder.locationRepositry.DecodeGpsRepositry
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class ReportAccidentViewModel @Inject constructor(
    private val accidentRepo: AccidentRepo,
private val decodeGpsRepositry: DecodeGpsRepositry,
    @ApplicationContext private val mContext: Context
    ) : ViewModel() {

    private val mutableLiveData: MutableLiveData<ResponseResult<List<AccidentReportEntity>>> =
        MutableLiveData()
    val liveDataAccidents: LiveData<ResponseResult<List<AccidentReportEntity>>> = mutableLiveData

    private val localCacheOfAccident = mutableListOf<AccidentReportEntity>()

    init {
        getReportsByDate()
    }

    fun showProgressBar(){
        mutableLiveData.postValue(ResponseResult.Loading())
    }

    fun reportAccident(location: AccidentReportEntity) {

        mutableLiveData.postValue(ResponseResult.Loading())
        viewModelScope.launch(Dispatchers.IO) {
           try {
               location.apply {
                   address = decodeGpsRepositry.decodeGpsLocation(mContext=mContext, latitude = this.locationLat,longitude = this.locationLng)
               }
               accidentRepo.reportAccent(location)
               localCacheOfAccident.add(location)
               mutableLiveData.postValue(ResponseResult.Success(localCacheOfAccident))
           }catch (e:Exception){
               e.printStackTrace()
               mutableLiveData.postValue(ResponseResult.Failure(e.message.toString(),e))
           }
        }
    }

    private fun getReportsByDate() {
        mutableLiveData.postValue(ResponseResult.Loading())
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val result=  accidentRepo.loadReportsByDate(accidentDate().replace("/",","))
                mutableLiveData.postValue(ResponseResult.Success(result))
            }catch (e:Exception){
                e.printStackTrace()
                mutableLiveData.postValue(ResponseResult.Failure(e.message.toString(),e))

            }
        }
    }

    fun removeReportById(id:String){
       viewModelScope.launch (Dispatchers.IO){
          try {
              mutableLiveData.postValue(ResponseResult.Loading())
            val result=  accidentRepo.removeReportById(accidentDate().replace("/",","),id)
              showLog("Success: Delete report")
             val newList= localCacheOfAccident.filter { it.documentId != id }
              localCacheOfAccident.clear()
              localCacheOfAccident.addAll(newList)
              mutableLiveData.postValue(ResponseResult.Success(localCacheOfAccident))
          }catch (e:Exception){
              e.printStackTrace()
              showLog("Error:Delete report")
          }
       }
    }

    fun accidentDate(): String {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        return dateFormat.format(Date())
    }


    fun showLog(tag: String) {
        Log.d(this.javaClass.simpleName + "TAG", tag)
    }
}