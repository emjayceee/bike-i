package com.moterroute.finder

import android.annotation.SuppressLint
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.IBinder
import android.os.Looper
import android.widget.Toast
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.gms.location.*
import com.google.android.gms.tasks.CancellationToken
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.tasks.OnTokenCanceledListener
import java.util.concurrent.TimeUnit

class GetLocationService : Service() {
    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(p0: LocationResult) {
            val locationRequest = p0.locations
            locationRequest.forEach {
                ShowLogs("new location ${it.latitude} ${it.longitude}")
                sendRealTimeLocationBroadCast(it)
            }
        }
    }

    private var fusedLocationClient: FusedLocationProviderClient? = null

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_GET_ONE_TIME_CURRENT_LOCATION -> {
                ShowLogs("getOneTimeCurrentLocation")

            }
            ACTION_GET_REAL_TIME_LOCATION_UPDATE -> {
                ShowLogs("getCurrentLocation")
                getCurrentLocation()
            }
            ACTION_STOP ->{
                stopSelf()
            }
        }
        return super.onStartCommand(intent, flags, startId)

    }

    override fun onDestroy() {
        fusedLocationClient?.removeLocationUpdates(locationCallback)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }


    @SuppressLint("MissingPermission")
    private fun getOneTimeLocation(){
        val currentLocationRequest = CurrentLocationRequest.Builder()
            .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
            .build()

        fusedLocationClient?.getCurrentLocation(currentLocationRequest,object :CancellationToken(){
            override fun onCanceledRequested(p0: OnTokenCanceledListener): CancellationToken {
                return CancellationTokenSource().token
            }

            override fun isCancellationRequested(): Boolean {
            return  false
            }
        })?.addOnSuccessListener {
                if (it==null){
                    Toast.makeText(this, "Cannot get location.", Toast.LENGTH_SHORT).show()
                }else{
                    sendRealTimeLocationBroadCast(it)
                }
            }
    }

    @SuppressLint("MissingPermission")
    private fun getCurrentLocation() {
        val currentLocationRequest = LocationRequest.Builder(TimeUnit.SECONDS.toMillis(2))

            .setPriority(
                Priority.PRIORITY_HIGH_ACCURACY
            ).build()
        fusedLocationClient?.requestLocationUpdates(
            currentLocationRequest,
            locationCallback,
            Looper.getMainLooper()
        )

    }

    private fun sendRealTimeLocationBroadCast(location: Location) {
        Intent(BROAdCAST_LOCATION_UPDATE).also { intent ->
            intent.action = BROAdCAST_LOCATION_UPDATE
            intent.putExtra("location", location)
           LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(intent)
        }

    }


    private fun sendErrorBroadCast(message: String?, errorCode: Int) {
        Intent().also {
            it.action = BROAdCAST_LOCATION_UPDATE
            it.putExtra("errorCode", errorCode)
            it.putExtra("errorMessage", message)
            sendBroadcast(it)
        }
    }

    companion object {
        val ACTION_STOP = "stop location update"
        val ACTION_GET_ONE_TIME_CURRENT_LOCATION = "get current location"
        val ACTION_GET_REAL_TIME_LOCATION_UPDATE = "get real time location update"
        val BROAdCAST_LOCATION_UPDATE = "GeolocationService.LOCATION_UPDATE"
        val ERROR_CODE_ENABLE_GPS = 6
        val ERROR_CODE_UNDEFINDED = -1
    }
}

class LocationBroadCast : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {

    }

}