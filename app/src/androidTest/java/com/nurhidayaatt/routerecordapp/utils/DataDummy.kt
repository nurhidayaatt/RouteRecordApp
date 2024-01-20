package com.nurhidayaatt.routerecordapp.utils

import android.graphics.Bitmap
import com.nurhidayaatt.routerecordapp.data.source.local.entity.DataEntity
import com.nurhidayaatt.routerecordapp.data.source.local.entity.DistanceEntity
import com.nurhidayaatt.routerecordapp.data.source.local.entity.DurationEntity
import com.nurhidayaatt.routerecordapp.data.source.local.entity.RoutesEntity
import java.time.LocalDateTime

object DataDummy {
    fun generateDummyRouteEntity(): List<DataEntity> {
        val items: MutableList<DataEntity> = arrayListOf()
        for (i in 0..10) {
            val story = DataEntity(
                typeActivity = "typeActivity$i",
                timestamp = LocalDateTime.MIN.plusDays(i.toLong()),
                img = Bitmap.createBitmap(400, 400, Bitmap.Config.ARGB_8888),
                elapsedTime = i.toLong(),
                elapsedTimePerKm = DurationEntity(listOf(i.toLong())),
                distancePerRoute = DistanceEntity(listOf(i.toDouble())),
                elapsedTimePerRoute = DurationEntity(listOf(i.toLong())),
                movingTime = i.toLong(),
                pacePerKm = DurationEntity(listOf(i.toLong())),
                routes = RoutesEntity(listOf(i.toString())),
                distance = i.toDouble()
            )
            items.add(story)
        }
        return items
    }
}