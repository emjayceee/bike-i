package com.moterroute.finder.locationRepositry

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.text.TextUtils
import javax.inject.Inject

class DecodeGpsRepositry @Inject constructor() {
    private var geocoder: Geocoder? = null
     suspend fun decodeGpsLocation(mContext:Context,latitude:Double?,longitude:Double?): String? {

        if (geocoder == null && Geocoder.isPresent()) {
            geocoder = Geocoder(mContext)
        }
        return if (latitude != null && longitude != null) {
            val addresses: List<Address>? = geocoder?.getFromLocation(
                latitude,
               longitude,
                1
            )
            val match = addresses?.firstOrNull()
            if (match!=null){
                val addressFragments = with(match) {
                    (0..maxAddressLineIndex).map { getAddressLine(it) }
                }
                val streatAdress= TextUtils.join(System.getProperty("line.separator").toString(), addressFragments)
                var city=""
                if (match.locality!=null) {
                    city=match.locality.toString()

                }
                return streatAdress + city
            }
            return "address null"
        } else {
            null
        }

    }

}