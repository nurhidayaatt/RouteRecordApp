package com.nurhidayaatt.routerecordapp.util

import android.content.Context
import android.graphics.Color
import android.util.TypedValue
import androidx.core.content.ContextCompat

val Context.colorPrimary get() = TypedValue().let {
    theme.resolveAttribute(com.google.android.material.R.attr.colorPrimary, it, true)
    ContextCompat.getColor(this, it.resourceId)
}

val Context.colorPrimaryContainer get() = TypedValue().let {
    theme.resolveAttribute(com.google.android.material.R.attr.colorPrimaryContainer, it, true)
    ContextCompat.getColor(this, it.resourceId)
}

val Context.colorOnSurface get() = TypedValue().let {
    theme.resolveAttribute(com.google.android.material.R.attr.colorOnSurface, it, true)
    ContextCompat.getColor(this, it.resourceId)
}

val colorPolyLine = Color.parseColor("#73b9ff")


