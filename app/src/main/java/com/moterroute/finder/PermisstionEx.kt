package com.moterroute.finder

import android.Manifest
import android.content.Context.SENSOR_SERVICE
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.tasks.Task


fun AppCompatActivity.getLocationPermissionResult(accessFineLocationGranted : () ->Unit): ActivityResultLauncher<Array<String>> {
    val locationPermissionRequest = this.registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted: Boolean? =
            permissions.get(Manifest.permission.ACCESS_FINE_LOCATION)
        val coarseLocationGranted: Boolean? =
            permissions.get(Manifest.permission.ACCESS_COARSE_LOCATION)

        if (fineLocationGranted == true) {
            showLogs("fine location permission granted")
            accessFineLocationGranted()
        } else if (coarseLocationGranted == true) {
            showLogs("only coarse location permission granted")
            showRequestPermissionRationale("App need precise location permission to work",MapsActivity.REQUEST_RATIONAL_PERMISSTION)
        } else {

        }
    }
    return locationPermissionRequest
}

@RequiresApi(Build.VERSION_CODES.M)
fun AppCompatActivity.checkLocationPermission(locationPermissionRequest: ActivityResultLauncher<Array<String>>, showRequestPermissionRationale:() ->Unit, permissionGranted: ()->Unit) {
    if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    ) {
       permissionGranted()
    } else if (shouldShowRequestPermissionRationale(
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    ) {
       showRequestPermissionRationale()
    } else {
              locationPermissionRequest.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )

    }
}

 fun MapsActivity.checkIfGpsEnable(onSuccess: (locationSettingResponse: LocationSettingsResponse) -> Unit) {
    val locationRequest = LocationRequest.create().apply {
        interval = 10000
        fastestInterval = 5000
        priority = Priority.PRIORITY_HIGH_ACCURACY
    }

    val builder = LocationSettingsRequest.Builder()
        .addLocationRequest(locationRequest!!)

    val client: SettingsClient = LocationServices.getSettingsClient(this)
    val task: Task<LocationSettingsResponse> = client.checkLocationSettings(builder.build())
    task.addOnSuccessListener { locationSettingsResponse ->
        onSuccess(locationSettingsResponse)
    }
    task.addOnFailureListener { exception ->

        if (exception is ResolvableApiException) {
           exception.startResolutionForResult(this,MapsActivity.REQUEST_RATIONAL_PERMISSTION)
        } else {
            Toast.makeText(this,exception.message.toString(),Toast.LENGTH_LONG).show()
        }
    }

}

fun MapsActivity.sensorDegree(success: ( degree:Float)->Unit){
    mSensorManager =this.getSystemService(SENSOR_SERVICE) as SensorManager?
    accelerometer = mSensorManager!!.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    magnetometer = mSensorManager!!.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
    sensorEventListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) mGravity = event.values
            if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) mGeomagnetic = event.values
            if (mGravity != null && mGeomagnetic != null) {
                val R = FloatArray(9)
                val I = FloatArray(9)
                val success = SensorManager.getRotationMatrix(R, I, mGravity, mGeomagnetic)
                if (success) {
                    val orientation = FloatArray(3)
                    SensorManager.getOrientation(R, orientation)
                    azimut = orientation[0] // orientation contains: azimut, pitch and roll
                    val degrees = Math.toDegrees(azimut!!.toDouble()).toFloat()
                    success(degrees)
                }
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }
}

fun MapsActivity.ShowResolucationErrorDiloage(setPositiveButton:()->Unit){
    AlertDialog.Builder(this)
        .setMessage("Please enable gps, so we can get your current location")
        .setPositiveButton("Okay",DialogInterface.OnClickListener { dialog, which ->
            setPositiveButton()
            dialog.dismiss()
        })
        .create()
        .show()
}

fun AppCompatActivity.showRequestPermissionRationale(
    message: String,requestCode:Int
) {
    AlertDialog.Builder(this)
        .setCancelable(false)
        .setMessage(message)
        .setPositiveButton("Okay") { dialogInterface, a ->
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", applicationContext.packageName, null)
                flags =  Intent.FLAG_ACTIVITY_NEW_TASK
            }

            startActivityForResult(intent,requestCode)
            dialogInterface.dismiss()
        }
        .setNegativeButton("No") { dialogInterface, i ->
            dialogInterface.dismiss()
        }
        .show()
}

fun com.google.maps.model.LatLng.toGsmLocationLatLng():LatLng{
    return LatLng(this.lat,this.lng)
}
fun Location.toGsmLocationLatLng(): LatLng {
    return LatLng(this.latitude,this.longitude)
}

fun com.google.maps.model.LatLng.toMapLatLng():com.google.android.gms.maps.model.LatLng{
    return com.google.android.gms.maps.model.LatLng(this.lat,this.lng)
}

fun AppCompatActivity.showLogs(message:String){
    Log.d(this.javaClass.simpleName+"TAG: ",message)
}

fun AppCompatActivity.showToast(message: String){
    Toast.makeText(this,message,Toast.LENGTH_LONG).show()
}

