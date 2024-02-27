package com.moterroute.finder.model

import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Polyline
import com.google.maps.model.DirectionsLeg

class DirectionsUi( val legs:Array<DirectionsLeg>,val summary:String,val polyline: List<LatLng>,val estimatedDistanceInKm: String,  val estimatedTimeInMints: String) {
}