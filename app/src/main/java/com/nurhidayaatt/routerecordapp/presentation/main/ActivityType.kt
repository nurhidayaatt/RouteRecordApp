package com.nurhidayaatt.routerecordapp.presentation.main

import androidx.datastore.preferences.core.stringPreferencesKey

enum class ActivityType { RUNNING, CYCLING }

val ACTIVITY_TYPE = stringPreferencesKey("activity_type")