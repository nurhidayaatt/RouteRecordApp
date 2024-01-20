package com.nurhidayaatt.routerecordapp.data.source.local.room

import android.graphics.Bitmap
import androidx.room.*
import androidx.sqlite.db.SupportSQLiteQuery
import com.nurhidayaatt.routerecordapp.data.source.local.entity.DataEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MainDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoute(data: DataEntity)

    @Query("DELETE FROM data WHERE id = :id")
    suspend fun deleteRoute(id: Long)

    @RawQuery(observedEntities = [DataEntity::class])
    fun getAllDataForRoutes(query: SupportSQLiteQuery): Flow<List<DataEntity>>

    @Query("UPDATE data set img = :img WHERE id = :id")
    suspend fun updateImage(id: Long, img: Bitmap)
}