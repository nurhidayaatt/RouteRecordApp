package com.nurhidayaatt.routerecordapp.data

import com.google.android.gms.common.api.ResolvableApiException

sealed class LocationResponse<T>(val data: T? = null, val exception: ResolvableApiException? = null) {
    class Success<T>(data: T) : LocationResponse<T>(data = data)
    class Error<T>(exception: ResolvableApiException) : LocationResponse<T>(exception = exception)
}
