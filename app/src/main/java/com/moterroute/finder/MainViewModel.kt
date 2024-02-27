package com.moterroute.finder

import android.content.Context
import android.location.Location
import android.os.Build
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.core.text.HtmlCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.maps.android.SphericalUtil
import com.moterroute.finder.dataSource.ResponseResult
import com.moterroute.finder.locationRepositry.DecodeGpsRepositry
import com.moterroute.finder.model.DirectionsUi
import com.moterroute.finder.model.GoogleDirectionApiRequest
import com.moterroute.finder.repo.DirectionApiRepo
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val directionApiRepo: DirectionApiRepo,
    @ApplicationContext private val mContext: Context,
    private val decodeGpsRepositry: DecodeGpsRepositry
) :
    ViewModel() {


    private val TAG = "MainViewModelTAG"

    private val _mutableLiveDataDirection: MutableLiveData<ResponseResult<List<DirectionsUi>>> =
        MutableLiveData()
    val liveDataDirectionsResult: LiveData<ResponseResult<List<DirectionsUi>>> =
        _mutableLiveDataDirection

    private val _mutableExtractMakerLocationFromSteps: MutableLiveData<List<LatLng>> =
        MutableLiveData()
    val extractMakerLocationFromStepsLiveDate: LiveData<List<LatLng>> =
        _mutableExtractMakerLocationFromSteps

    private var userCurrentLocation: Location = Location("unKnow")
    var destination: LatLng? = null

    private var muteTts = false
    val list = ArrayList<DirectionsUi>()
    private var currentUserRouts: DirectionsUi? = null
    private val alreadySpake = java.util.HashSet<String>()
    private var tts: TextToSpeech? = null
    private val firebaseFireStore = Firebase.firestore


    private fun initTTS() {
        tts = TextToSpeech(mContext, TextToSpeech.OnInitListener { status ->
            if (status != TextToSpeech.ERROR) {
                tts?.language = Locale.US
            }
        })
    }

    init {
        initTTS()
    }

    fun findNextPointToSpeak() {
        viewModelScope.launch(Dispatchers.Default) {
            currentUserRouts?.let { allRoutes ->
                allRoutes.legs.forEach { directionsLeg ->
                    directionsLeg.steps.forEach { step ->
                        val distance = SphericalUtil.computeDistanceBetween(
                            userCurrentLocation.toGsmLocationLatLng(),
                            step.endLocation.toMapLatLng()
                        )
                        val textToSpeake = HtmlCompat.fromHtml(
                            step.htmlInstructions,
                            HtmlCompat.FROM_HTML_MODE_LEGACY
                        ).toString()
                        Log.d(TAG, "distance " + distance)
                        if (distance < 100 && !alreadySpake.contains(textToSpeake)) {
                            Log.d(TAG, "speaker this ${textToSpeake}")
                            alreadySpake.add(textToSpeake)
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                tts?.speak(textToSpeake, TextToSpeech.QUEUE_FLUSH, null, null);
                            } else {
                                tts?.speak(textToSpeake, TextToSpeech.QUEUE_FLUSH, null);
                            }
                        }
                    }
                }
            }
        }
    }

    fun getBikeIcons() {
        viewModelScope.launch(Dispatchers.Default) {
            val bikeLocationList = ArrayList<LatLng>()
            LocationOfBikeIcons.getLocations().forEach {
                try {
                    val lat = it.split(",").get(0).trim().toDouble()
                    val lng = it.split(",").get(1).trim().toDouble()
                    bikeLocationList.add(LatLng(lat, lng))
                } catch (e: Exception) {
                    Log.d(TAG, "bike location not valid ${it}")
                }
            }
            _mutableExtractMakerLocationFromSteps.postValue(bikeLocationList)
        }
    }

    fun getCurrentLocation(): Location {
        return this.userCurrentLocation
    }

    fun setCurrentLocation(location: Location) {
        this.userCurrentLocation = location
        if (muteTts) {
            this.findNextPointToSpeak()
        }

        saveCurrentLocation()
    }


    private fun saveCurrentLocation() {
        viewModelScope.launch(Dispatchers.IO) {
            firebaseFireStore.collection("currentLocation")
                .document("currentUser1")
                .set(
                    mapOf(
                        "latLng" to getCurrentLocation(),
                        "serverTime" to FieldValue.serverTimestamp(),
                        "localTime" to SimpleDateFormat("dd/MM/yyyy hh:mm:ss a", Locale.getDefault()).format(Date()),
                        "address" to  decodeGpsRepositry.decodeGpsLocation(mContext,getCurrentLocation().latitude,getCurrentLocation().longitude)
                    )
                )
        }
    }

    fun setCurrentRoute(currentRoute: DirectionsUi) {
        this.currentUserRouts = currentRoute
    }

    fun getDirection(googleDirectionApiRequest: GoogleDirectionApiRequest) {
        this.destination = googleDirectionApiRequest.destination
        if (googleDirectionApiRequest.origin == null) {
            _mutableLiveDataDirection.postValue(
                ResponseResult.Failure(
                    throwable = null,
                    error = "Current location null"
                )
            )
        } else if (googleDirectionApiRequest.destination == null) {
            _mutableLiveDataDirection.postValue(
                ResponseResult.Failure(
                    throwable = null,
                    error = "destination location null"
                )
            )

        } else {
            viewModelScope.launch(Dispatchers.IO) {
                _mutableLiveDataDirection.postValue(ResponseResult.Loading())
                val result = directionApiRepo.getDirection(
                    googleDirectionApiRequest
                )
                when (result) {
                    is ResponseResult.Success -> {
                        list.clear()
                        alreadySpake.clear()
                        result.data.routes.forEach {

                            val poliylineLatlngList = it.overviewPolyline.decodePath()
                                .map { latLng ->
                                    com.google.android.gms.maps.model.LatLng(
                                        latLng.lat,
                                        latLng.lng
                                    )
                                }
                            var distanceInMeters = 0L
                            var etaTime = 0L
                            it.legs.forEach { directionsLeg ->
                                distanceInMeters += directionsLeg.distance.inMeters
                                etaTime += directionsLeg.duration.inSeconds
                            }

                            list.add(
                                DirectionsUi(
                                    it.legs,
                                    it.summary,
                                    poliylineLatlngList,
                                    (distanceInMeters / 1000).toString() + " km",
                                    TimeUnit.SECONDS.toMinutes(etaTime).toString() + " m"
                                )
                            )

                        }
                        _mutableLiveDataDirection.postValue(ResponseResult.Success(list))
                    }
                    is ResponseResult.Failure -> {
                        _mutableLiveDataDirection.postValue(
                            ResponseResult.Failure(
                                result.error,
                                result.throwable
                            )
                        )
                    }
                    is ResponseResult.Loading -> {
                        _mutableLiveDataDirection.postValue(ResponseResult.Loading())

                    }
                }
            }
        }
    }

    override fun onCleared() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        super.onCleared()
    }

    fun muteTTS() {
        this.muteTts = true
    }

    fun unMuteTTS() {
        this.muteTts = false
    }

    fun getTTSMuteStatus(): Boolean {
        return this.muteTts
    }

    fun findRouteByPolylineTag(it: String) {
        list.forEach { directionsUi ->
            val tag =
                directionsUi.summary + ": " + directionsUi.estimatedDistanceInKm + ":" + directionsUi.estimatedTimeInMints
            if (it.equals(tag)) {
                setCurrentRoute(directionsUi)
            }
        }
    }
}