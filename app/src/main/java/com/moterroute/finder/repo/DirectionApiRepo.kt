package com.moterroute.finder.repo

import com.google.maps.model.DirectionsResult
import com.moterroute.finder.dataSource.GoogleDirectionApiRemoteResource
import com.moterroute.finder.dataSource.ResponseResult
import com.moterroute.finder.model.GoogleDirectionApiRequest
import javax.inject.Inject

class DirectionApiRepo @Inject constructor(private val googleDirectionApiRemoteResource: GoogleDirectionApiRemoteResource) {

    suspend fun getDirection(googleDirectionApiRequest: GoogleDirectionApiRequest): ResponseResult<DirectionsResult> {
       return googleDirectionApiRemoteResource.getDirection(googleDirectionApiRequest)
    }
}