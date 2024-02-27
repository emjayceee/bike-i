package com.moterroute.finder.dataSource

sealed class ResponseResult<out T> {
    data class Loading(val message: String = "") : ResponseResult<Nothing>()
    data class Success<out T>(val data: T) : ResponseResult<T>()
    data class Failure(val error: String, val throwable: Throwable?) : ResponseResult<Nothing>()
}
