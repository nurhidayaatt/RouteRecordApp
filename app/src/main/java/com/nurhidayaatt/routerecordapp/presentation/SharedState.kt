package com.nurhidayaatt.routerecordapp.presentation

import com.nurhidayaatt.routerecordapp.domain.model.Routes

data class SharedState(
    val routes: List<Routes>? = emptyList(),
    val position: Int? = null,
    val needDelay: Boolean = false
)
