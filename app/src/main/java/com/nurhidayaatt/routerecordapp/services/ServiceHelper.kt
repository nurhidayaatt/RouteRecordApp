package com.nurhidayaatt.routerecordapp.services

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.nurhidayaatt.routerecordapp.presentation.main.MainActivity
import com.nurhidayaatt.routerecordapp.util.Constants.CANCEL_REQUEST_CODE
import com.nurhidayaatt.routerecordapp.util.Constants.CLICK_REQUEST_CODE
import com.nurhidayaatt.routerecordapp.util.Constants.FINISH_REQUEST_CODE
import com.nurhidayaatt.routerecordapp.util.Constants.LOCATION_TRACKING_STATE
import com.nurhidayaatt.routerecordapp.util.Constants.PAUSE_REQUEST_CODE
import com.nurhidayaatt.routerecordapp.util.Constants.RESUME_REQUEST_CODE

object ServiceHelper {

    private const val flag = PendingIntent.FLAG_IMMUTABLE

    fun clickPendingIntent(context: Context): PendingIntent {
        val clickIntent = Intent(context, MainActivity::class.java).apply {
            putExtra(LOCATION_TRACKING_STATE, LocationTrackingState.Started.name)
        }
        return PendingIntent.getActivity(context, CLICK_REQUEST_CODE, clickIntent, flag)
    }

    fun pausePendingIntent(context: Context): PendingIntent {
        val stopIntent = Intent(context, LocationService::class.java).apply {
            putExtra(LOCATION_TRACKING_STATE, LocationTrackingState.Paused.name)
        }
        return PendingIntent.getService(context, PAUSE_REQUEST_CODE, stopIntent, flag)
    }

    fun resumePendingIntent(context: Context): PendingIntent {
        val resumeIntent = Intent(context, LocationService::class.java).apply {
            putExtra(LOCATION_TRACKING_STATE, LocationTrackingState.Started.name)
        }
        return PendingIntent.getService(context, RESUME_REQUEST_CODE, resumeIntent, flag)
    }

    fun finishPendingIntent(context: Context): PendingIntent {
        val finishIntent = Intent(context, MainActivity::class.java).apply {
            putExtra(LOCATION_TRACKING_STATE, LocationTrackingState.Finished.name)
        }
        return PendingIntent.getActivity(context, FINISH_REQUEST_CODE, finishIntent, flag)
    }

    fun cancelPendingIntent(context: Context): PendingIntent {
        val cancelIntent = Intent(context, MainActivity::class.java).apply {
            putExtra(LOCATION_TRACKING_STATE, LocationTrackingState.Canceled.name)
        }
        return PendingIntent.getActivity(context, CANCEL_REQUEST_CODE, cancelIntent, flag)
    }

    fun triggerForegroundService(context: Context, action: String) {
        Intent(context, LocationService::class.java).apply {
            this.action = action
            context.startService(this)
        }
    }
}