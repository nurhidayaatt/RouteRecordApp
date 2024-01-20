package com.nurhidayaatt.routerecordapp.data.source.local.room

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.room.TypeConverter
import com.google.gson.Gson
import com.nurhidayaatt.routerecordapp.data.source.local.entity.DistanceEntity
import com.nurhidayaatt.routerecordapp.data.source.local.entity.DurationEntity
import com.nurhidayaatt.routerecordapp.data.source.local.entity.RoutesEntity
import java.io.ByteArrayOutputStream
import java.time.LocalDateTime

class Converter {

    @TypeConverter
    fun fromTimestampToString(timestamp: LocalDateTime): String {
        return timestamp.toString()
    }

    @TypeConverter
    fun fromStringToTimestamp(timestamp: String): LocalDateTime {
        return LocalDateTime.parse(timestamp)
    }

    @TypeConverter
    fun fromBitmapToByteArray(bytes: ByteArray): Bitmap {
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }

    @TypeConverter
    fun fromByteArrayToBitmap(bmp: Bitmap): ByteArray {
        val outputStream = ByteArrayOutputStream()
        bmp.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        return outputStream.toByteArray()
    }

    @TypeConverter
    fun fromDurationsToJSON(durations: DurationEntity): String {
        return Gson().toJson(durations)
    }

    @TypeConverter
    fun fromJSONToDurations(json: String): DurationEntity {
        return Gson().fromJson(json, DurationEntity::class.java)
    }

    @TypeConverter
    fun fromRoutesToJSON(routes: RoutesEntity): String {
        return Gson().toJson(routes)
    }

    @TypeConverter
    fun fromJSONToRoutes(json: String): RoutesEntity {
        return Gson().fromJson(json, RoutesEntity::class.java)
    }

    @TypeConverter
    fun fromDistancesToJSON(distances: DistanceEntity): String {
        return Gson().toJson(distances)
    }

    @TypeConverter
    fun fromJSONToDistances(json: String): DistanceEntity {
        return Gson().fromJson(json, DistanceEntity::class.java)
    }
}