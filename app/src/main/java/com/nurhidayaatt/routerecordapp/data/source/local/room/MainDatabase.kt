package com.nurhidayaatt.routerecordapp.data.source.local.room

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.nurhidayaatt.routerecordapp.data.source.local.entity.DataEntity

@TypeConverters(value = [Converter::class])
@Database(
    entities = [DataEntity::class],
    version = 1,
    exportSchema = false
)
abstract class MainDatabase: RoomDatabase() {
    abstract fun mainDao(): MainDao
}