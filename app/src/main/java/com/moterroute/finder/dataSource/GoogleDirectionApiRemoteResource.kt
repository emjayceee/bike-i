package com.moterroute.finder.dataSource

import android.util.Log
import com.google.maps.DirectionsApi
import com.google.maps.GeoApiContext
import com.google.maps.errors.ZeroResultsException
import com.google.maps.model.DirectionsResult
import com.google.maps.model.LatLng

import com.google.maps.model.TravelMode
import com.moterroute.finder.model.GoogleDirectionApiRequest
import com.moterroute.finder.providerHilt.GeoApiContextProvider
import javax.inject.Inject

class GoogleDirectionApiRemoteResource @Inject constructor() {

    suspend fun getDirection(googleDirectionApiRequest: GoogleDirectionApiRequest): ResponseResult<DirectionsResult> {
        Log.d("GoogleDirection","${googleDirectionApiRequest.origin} ${googleDirectionApiRequest.destination}")
     val request=  DirectionsApi.newRequest(GeoApiContextProvider.provideGeoApiContext())
           .origin(LatLng(googleDirectionApiRequest.origin?.latitude?:0.0,googleDirectionApiRequest.origin?.longitude?:0.0))
           .destination(LatLng(googleDirectionApiRequest.destination?.latitude?:0.0,googleDirectionApiRequest.destination?.longitude?:0.0))
         .alternatives(true);

       return try{
          val data=   request.await()
           ResponseResult.Success(data =data )
        }catch (e:Exception){
            if (e is ZeroResultsException){
                ResponseResult.Failure("No route found ",e)

            }else{
                ResponseResult.Failure(e.message.toString(),e)

            }
        }

    }


}