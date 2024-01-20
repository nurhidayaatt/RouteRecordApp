package com.nurhidayaatt.routerecordapp.util

import android.content.Context
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.nurhidayaatt.routerecordapp.R
import java.text.NumberFormat
import java.util.Locale
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

fun Int.dp(context: Context): Float = (this * context.resources.displayMetrics.density)

inline fun showAlertDialog(
    context: Context,
    icon: Int = R.drawable.ic_location_off,
    title: String,
    message: String,
    positiveButtonText: String = context.resources.getString(R.string.dialog_positive_button_default),
    crossinline onPositiveClick: () -> Unit? = {},
    crossinline onDismiss: () -> Unit? = {},
    crossinline onCancel: () -> Unit? = {},
) {
    MaterialAlertDialogBuilder(context)
        .setIcon(icon)
        .setTitle(title)
        .setMessage(message)
        .setPositiveButton(positiveButtonText) { dialog, _ ->
            dialog.dismiss()
            onPositiveClick()
        }
        .setNegativeButton("Cancel") { dialog, _ ->
            dialog.cancel()
        }
        .setOnCancelListener {
            onCancel()
        }
        .setOnDismissListener {
            onDismiss()
        }.create().show()
}

fun Duration.durationToTimerFormat(): String {
    this.toComponents { hours, minutes, seconds, _ ->
        return "${hours.toInt().pad()}:${minutes.pad()}:${seconds.pad()}"
    }
}

fun List<Duration>.durationToTimerFormat(): String {
    this.sumOf {
        it.inWholeSeconds
    }.toDuration(DurationUnit.SECONDS).toComponents { hours, minutes, seconds, _ ->
        return "${hours.toInt().pad()}:${minutes.pad()}:${seconds.pad()}"
    }
}

fun Duration.durationToString(context: Context): String {
    this.toComponents { hours, minutes, seconds, _ ->
        return when {
            hours > 0 -> {
                String.format(context.getString(R.string.format_time_hour), hours, minutes)
            }

            minutes > 0 -> {
                String.format(context.getString(R.string.format_time_minute), minutes, seconds)
            }

            else -> {
                String.format(context.getString(R.string.format_time_second), seconds)
            }
        }
    }
}

fun Long.toDurationString(context: Context): String {
    return this.toDuration(DurationUnit.SECONDS).durationToString(context)
}

fun List<Double>.formatDistance(): String {
    val numberFormat = NumberFormat.getInstance(Locale.getDefault())
    val distance = "%.2f".format(this.sum())
    val distanceInKm =
        numberFormat.parse("%.2f".format((numberFormat.parse(distance)!!.toDouble() / 1000)))!!
            .toDouble()
    return when {
        distance.length > 8 -> "${distanceInKm.toInt()} km"
        distance.length > 7 -> "${"%.1f".format(distanceInKm)} km"
        distance.length > 6 -> "$distanceInKm km"
        else -> "${numberFormat.parse(distance)!!.toInt()} m"
    }
}

fun Double.formatDistance(): String {
    val numberFormat = NumberFormat.getInstance(Locale.getDefault())
    val distance = "%.2f".format(this)
    val distanceInKm =
        numberFormat.parse("%.2f".format((numberFormat.parse(distance)!!.toDouble() / 1000)))!!
            .toDouble()
    return when {
        distance.length > 8 -> "${distanceInKm.toInt()} km"
        distance.length > 7 -> "${"%.1f".format(distanceInKm)} km"
        distance.length > 6 -> "$distanceInKm km"
        else -> "${numberFormat.parse(distance)!!.toInt()} m"
    }
}

fun Duration.formatPace(): String {
    return if (this != Duration.ZERO) {
        this.toComponents { hours, minutes, seconds, _ ->
            "%.2f".format((hours * 60) + minutes + (seconds / 60f)) + "min/km"
        }
    } else {
        "0min/km"
    }
}

fun List<Long>.formatPace(): String {
    val pace = (this.sum()/this.size).toDuration(DurationUnit.SECONDS)
    return if (pace != Duration.ZERO) {
        pace.toComponents { hours, minutes, seconds, _ ->
            "%.2f".format((hours * 60) + minutes + (seconds / 60f)) + "min/km"
        }
    } else {
        "0min/km"
    }
}

fun Int.pad(): String = this.toString().padStart(2, '0')