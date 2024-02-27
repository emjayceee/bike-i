package com.moterroute.finder.algoImplement

import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.moterroute.finder.R

class NewActivity(context: Context, apiKey: String) : AStarAlgorithm(context, apiKey), OnMapReadyCallback {

    private var mMap: GoogleMap? = null
    private var aStarAlgorithm: AStarAlgorithm? = null

    fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // Initialize the AStarAlgorithm with the Google Maps API key
        val apiKey = "AIzaSyBhEVkpM82FqGfCBXgb5yiyOfMn_yHt23I"
        aStarAlgorithm = AStarAlgorithm(this, apiKey)
    }

    private fun setContentView(activityMain: Any) {

    }

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        val startPoint = LatLng(START_LATITUDE, START_LONGITUDE)
        val endPoint = LatLng(END_LATITUDE, END_LONGITUDE)

        val optimalPath = aStarAlgorithm.findOptimalPath(startPoint, endPoint)

        val polylineOptions = PolylineOptions()
            .addAll(optimalPath)
            .width(5f)
            .color(resources.getColor(R.color.colorPrimary))
        mMap!!.addPolyline(polylineOptions)

        mMap!!.addMarker(MarkerOptions().position(startPoint).title("Start"))
        mMap!!.addMarker(MarkerOptions().position(endPoint).title("End"))

        val builder = LatLngBounds.builder()
        builder.include(startPoint)
        builder.include(endPoint)
        val bounds = builder.build()
        mMap!!.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 50))
    }
}