package com.moterroute.finder


import android.app.Activity
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.location.Location
import android.os.Build
import android.os.Parcelable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import androidx.annotation.DrawableRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getSystemService
import androidx.core.graphics.drawable.toBitmap
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.gson.Gson


fun Activity.ShowLog(tag:String){
    Log.d(this.javaClass.simpleName+"TAG ",tag)
}

fun <T : Parcelable?> Intent.getParseable(key: String, m_class: Class<T>): T? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
        this.getParcelableExtra(key, m_class)
    else
        this.getParcelableExtra(key)
}

fun Service.ShowLogs(tag: String){
    Log.d(this.javaClass.simpleName+"TAG",tag)
}

fun Location.locationToJson(): String {
    return Gson().toJson(this)

}

var bitmap:Bitmap?=null
var oldIcon:Int?=null
 fun MapsActivity.getMarkerBitmapFromView(@DrawableRes resId: Int): Bitmap? {
     if (bitmap==null || resId!= oldIcon ){
         oldIcon = resId
         bitmap=  AppCompatResources.getDrawable(this, resId)!!.toBitmap()
     }
     return bitmap
}
 fun MapsActivity.bitmapDescriptorFromVector( vectorResId: Int): BitmapDescriptor? {
    val vectorDrawable = ContextCompat.getDrawable(this, vectorResId)
    vectorDrawable!!.setBounds(0, 0, vectorDrawable.intrinsicWidth, vectorDrawable.intrinsicHeight)
    val bitmap = Bitmap.createBitmap(
        vectorDrawable.intrinsicWidth,
        vectorDrawable.intrinsicHeight,
        Bitmap.Config.ARGB_8888
    )
    val canvas = Canvas(bitmap)
    vectorDrawable.draw(canvas)
    return BitmapDescriptorFactory.fromBitmap(bitmap)
}