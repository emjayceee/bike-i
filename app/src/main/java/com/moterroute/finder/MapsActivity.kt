package com.moterroute.finder

import android.annotation.SuppressLint
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.gms.common.api.Status
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.AutocompleteActivity
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import com.google.android.material.snackbar.Snackbar
import com.moterroute.finder.dataSource.ResponseResult
import com.moterroute.finder.databinding.ActivityMapsBinding
import com.moterroute.finder.model.DirectionsUi
import com.moterroute.finder.model.GoogleDirectionApiRequest
import com.moterroute.finder.reportAccednt.AccidentReportEntity
import com.moterroute.finder.reportAccednt.ReportAccidentViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.util.*
import kotlin.collections.HashMap


@AndroidEntryPoint
class MapsActivity : AppCompatActivity(), OnMapReadyCallback, GoogleMap.OnPolylineClickListener,
    GoogleMap.OnMarkerClickListener {

    private var screenRotation: Float = 0f
    var mSensorManager: SensorManager? = null
    var sensorEventListener: SensorEventListener? = null
    var accelerometer: Sensor? = null
    var magnetometer: Sensor? = null
    var mGravity: FloatArray? = null
    var mGeomagnetic: FloatArray? = null
    var azimut: Float? = null

    private var reportAccidentBottomPressed = false
    private val viewModel: MainViewModel by viewModels()
    private val accidentViewModel: ReportAccidentViewModel by viewModels()
    private lateinit var mMap: GoogleMap
    private val accidentMakerList = HashMap<String,Marker>()
    private val routeMakerList = HashMap<String?,Marker>()
    private val routePolyline = HashMap<String?,Polyline?>()
    private lateinit var binding: ActivityMapsBinding
    private val realTimeLocationUpdateBroadCast = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                GetLocationService.BROAdCAST_LOCATION_UPDATE -> {
                    val location = intent.getParseable("location", m_class = Location::class.java)
                    if (location == null) {
                        val errorMessage = intent?.getStringExtra("errorMessage")
                        showToast("Error: " + errorMessage.toString())
                    } else {

                        if (!binding.search.isVisible) {
                            binding.btReportAccident.visibility = View.VISIBLE
                            binding.search.visibility = View.VISIBLE
                            binding.progressCircular.visibility = View.INVISIBLE
                        }
                        showLogs("location " + location?.latitude + " ${location?.longitude}")
                        if (location != null) {
                            if (reportAccidentBottomPressed) {
                                accidentViewModel.reportAccident(
                                    AccidentReportEntity(
                                        locationLat = location!!.latitude,
                                        locationLng = location.longitude,
                                        date = accidentViewModel.accidentDate(),
                                    ).apply {
                                        this.documentId = timeStampToHumanReadAbleTime()
                                            .replace(" ",",")
                                            .replace("/",",")
                                            .replace(":",",")

                                    }
                                )
                                reportAccidentBottomPressed = false
                            }
                            viewModel.setCurrentLocation(location)
                            showCurrentLocation()
                        }

                    }
                }
            }
        }

    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkLocationPermission(
                getLocationPermissionResult(accessFineLocationGranted = {
                    checkIfGpsEnable {
                        startLocationUpdatesService()
                    }
                }),
                showRequestPermissionRationale = {
                    showRequestPermissionRationale(
                        "App need precise location permission to work",
                        REQUEST_RATIONAL_PERMISSTION
                    )
                }, permissionGranted = {
                    checkIfGpsEnable() {
                        startLocationUpdatesService()
                    }
                })
        } else {
            checkIfGpsEnable {
                startLocationUpdatesService()
            }
        }

        binding.search.setOnClickListener {
            val fields: List<Place.Field> = Arrays.asList(
                Place.Field.ID,
                Place.Field.ADDRESS,
                Place.Field.LAT_LNG,
                Place.Field.NAME
            )

            val intent: Intent = Autocomplete.IntentBuilder(
                AutocompleteActivityMode.FULLSCREEN, fields
            )
                .build(this)
            startActivityForResult(intent, REQUEST_AUTOCOMPLETE)

        }
        binding.search.visibility = View.INVISIBLE
        binding.btReportAccident.visibility = View.INVISIBLE
        binding.progressReportAccident.visibility = View.INVISIBLE
        binding.btReportAccident.setOnClickListener {
            reportAccidentBottomPressed = true
            accidentViewModel.showProgressBar()
            Intent(this, GetLocationService::class.java).also {
                it.action = GetLocationService.ACTION_GET_ONE_TIME_CURRENT_LOCATION
            }.apply {
                startService(this)
            }
        }


        sensorDegree { degree ->
            this.screenRotation = degree;
        }



        binding.btMute.setOnClickListener {
            viewModel.muteTTS()
            updateMuteButtonState()
        }
        binding.btUnMute.setOnClickListener {
            viewModel.unMuteTTS()
            updateMuteButtonState()
        }

        initObserver()
    }

    private fun updateMuteButtonState(){
        if (viewModel.getTTSMuteStatus()){
            binding.btMute.visibility = View.INVISIBLE
            binding.btUnMute.visibility = View.VISIBLE
        }else{
            binding.btMute.visibility = View.VISIBLE
            binding.btUnMute.visibility = View.INVISIBLE
        }
    }

    override fun onPause() {
        super.onPause()
        mSensorManager?.unregisterListener(sensorEventListener)
    }

    override fun onResume() {
        super.onResume()
        mSensorManager?.registerListener(
            sensorEventListener,
            accelerometer,
            SensorManager.SENSOR_DELAY_UI
        )
        mSensorManager?.registerListener(
            sensorEventListener,
            magnetometer,
            SensorManager.SENSOR_DELAY_UI
        )
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdatesService() {
        mMap?.setMyLocationEnabled(true)
        Intent(this, GetLocationService::class.java).also {
            it.action = GetLocationService.ACTION_GET_REAL_TIME_LOCATION_UPDATE
            startService(it)
        }
    }

    override fun onStart() {
        super.onStart()
        LocalBroadcastManager.getInstance(this).registerReceiver(
            realTimeLocationUpdateBroadCast,
            IntentFilter(GetLocationService.BROAdCAST_LOCATION_UPDATE)
        )

    }

    override fun onStop() {
        super.onStop()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(realTimeLocationUpdateBroadCast)
    }

    private fun initObserver() {
        viewModel.extractMakerLocationFromStepsLiveDate.observe(this){
            it.forEach {latlng->

                mMap?.addMarker(
                    MarkerOptions()
                        .position(latlng)
                        .icon(BitmapDescriptorFactory.fromBitmap(getMarkerBitmapFromView(R.drawable.ic_baseline_directions_bike_24)!!))

                )
            }
        }
        viewModel.liveDataDirectionsResult.observe(this) {
            when (it) {
                is ResponseResult.Success -> {
                    routeMakerList?.forEach { (t, u) ->
                        u.remove()
                    }
                    routePolyline?.forEach { (t, u) ->
                        u?.remove()
                    }
                    var showEndingMaker = true
                    it.data.forEach { directionsUi ->
                        showpolygon(directionsUi)
                        if (showEndingMaker){
                            val tag =  directionsUi.summary + ": " + directionsUi.estimatedDistanceInKm + ":" + directionsUi.estimatedTimeInMints

                            val lastPoint =  directionsUi.polyline.last()
                            showEndingMaker(lastPoint,"Ending point")
                            showEndingMaker = false
                            viewModel.findRouteByPolylineTag(tag)
                        }
                    }
                }
                is ResponseResult.Failure -> {
                    showToast(it.error)
                    it.throwable?.printStackTrace()
                }
                else -> {

                }
            }
        }

        accidentViewModel.liveDataAccidents.observe(this) {
            when (it) {
                is ResponseResult.Success -> {
                    accidentMakerList.forEach { (t, u) ->
                        u.remove()
                    }
                    accidentMakerList.clear()
                    it.data.forEachIndexed { index,  accident ->
                        shpwAccidentMaker(accident.documentId,
                            LatLng(accident.locationLat, accident.locationLng),
                            accident.timeStampToHumanReadAbleTime()
                        )
                    }
                    binding.progressReportAccident.visibility = View.INVISIBLE
                }
                is ResponseResult.Failure -> {

                }
                is ResponseResult.Loading -> {
                    binding.progressReportAccident.visibility = View.VISIBLE
                }
            }
        }
    }

    var firstTimeSetup = true;
    private fun showCurrentLocation() {
        var currentposition: CameraPosition? = mMap?.getCameraPosition()
        if (firstTimeSetup) {
            currentposition =
                CameraPosition.Builder().target(viewModel.getCurrentLocation().toGsmLocationLatLng())
                    .zoom(17f)
                    .bearing(screenRotation).tilt(30f).build()
            firstTimeSetup = false
            mMap?.animateCamera(CameraUpdateFactory.newCameraPosition(currentposition))

        } else {
            if (currentposition != null) {
                mMap?.animateCamera(CameraUpdateFactory.newCameraPosition(currentposition))

            }
        }

    }

    private fun shpwAccidentMaker(id:String,
        latLng: LatLng,
        time: String
    ) {
        showLogs("tag of accident maker "+id)
        mMap?.addMarker(
            MarkerOptions()
                .position(latLng)
                .title(time)
                .snippet("Delete Report")
                .icon(BitmapDescriptorFactory.fromBitmap(getMarkerBitmapFromView(R.drawable.ic_baseline_report_problem_24)!!))

        )?.also {
            it.tag = id
            accidentMakerList.put(id,it)
        }

    }


    private fun showEndingMaker(latLng:LatLng,endingLocationName:String){
       val maker = mMap?.addMarker(
            MarkerOptions()
                .position(latLng)
                .title(endingLocationName)
                .icon(BitmapDescriptorFactory.fromBitmap(getMarkerBitmapFromView(R.drawable.ic_baseline_outlined_flag_24)!!))

        )

      if (maker!=null){
          routeMakerList.put(maker!!.id,maker)
      }

    }

    private fun showpolygon(directionsUi: DirectionsUi) {

        val polyline2 = mMap?.addPolyline(
            PolylineOptions()
                .clickable(true)
                .addAll(directionsUi.polyline)
        )
        polyline2?.tag =
            directionsUi.summary + ": " + directionsUi.estimatedDistanceInKm + ":" + directionsUi.estimatedTimeInMints

        if (polyline2 != null) {
            stylePolyline(polyline2)
        }

        routePolyline?.put(polyline2?.id,polyline2)

    }

    private fun stylePolyline(polyline: Polyline) {
        val rnd = Random();
        val color = Color.argb(255, rnd.nextInt(256), rnd.nextInt(256), rnd.nextInt(256));
        if (polyline.tag?.equals("A") == true) {
            polyline.color = Color.BLUE
            polyline.width = 26f
        } else {
            polyline.color = color
            polyline.width = 26f
        }

    }

    override fun onPolylineClick(polyline: Polyline) {
        // Flip the values of the red, green, and blue components of the polygon's color.
        Snackbar.make(binding.search, polyline.tag.toString(), Snackbar.LENGTH_LONG).show()
    }

    override fun onMarkerClick(p0: Marker): Boolean {
        return false
    }

    override fun onDestroy() {
        Intent(this, GetLocationService::class.java).also {
            it.action = GetLocationService.ACTION_STOP
        }
        super.onDestroy()

    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap?.setOnPolylineClickListener{
            Snackbar.make(binding.search, it.tag.toString(), Snackbar.LENGTH_LONG).show()
            viewModel.findRouteByPolylineTag(it.tag.toString())
        }
        mMap.setOnMarkerClickListener(this)
        mMap.setOnInfoWindowClickListener {
           if (it.tag!=null){
               showLogs(""+it.tag.toString())
               accidentViewModel.removeReportById(it.tag.toString())
           }
        }
        viewModel.getBikeIcons()
        // Add a marker in Sydney and move the camera

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_RATIONAL_PERMISSTION && resultCode == Activity.RESULT_OK) {
            checkIfGpsEnable {
                startLocationUpdatesService()
            }
        } else if (requestCode == REQUEST_RATIONAL_PERMISSTION && resultCode == Activity.RESULT_CANCELED) {
            ShowResolucationErrorDiloage {
                checkIfGpsEnable {
                    startLocationUpdatesService()
                }
            }
        } else if (requestCode == REQUEST_AUTOCOMPLETE) {
            if (resultCode == RESULT_OK) {
                val place = Autocomplete.getPlaceFromIntent(data)
                viewModel.getDirection(
                    GoogleDirectionApiRequest(
                        viewModel.getCurrentLocation().toGsmLocationLatLng(),
                        LatLng(
                            place.latLng.latitude,
                            place.latLng.longitude
                        )
                    )
                )
            } else if (resultCode == AutocompleteActivity.RESULT_ERROR) {
                // TODO: Handle the error.
                val status: Status = Autocomplete.getStatusFromIntent(data)
                showLogs(status.getStatusMessage().toString())
            } else if (resultCode == RESULT_CANCELED) {
                // The user canceled the operation.
            }
            return
        }

    }

    companion object {
        public val REQUEST_RATIONAL_PERMISSTION = 98
        private val REQUEST_AUTOCOMPLETE = 1
    }


}