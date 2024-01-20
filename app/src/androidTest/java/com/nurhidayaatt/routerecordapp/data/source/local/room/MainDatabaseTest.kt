package com.nurhidayaatt.routerecordapp.data.source.local.room

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.nurhidayaatt.routerecordapp.utils.DataDummy
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith


@RunWith(AndroidJUnit4::class)
@SmallTest
class MainDatabaseTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var database: MainDatabase
    private lateinit var dao: MainDao
    // just make 1 data dummy if not need much
    private val sampleRoute = DataDummy.generateDummyRouteEntity()[0]

    @Before
    fun initDb() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            MainDatabase::class.java
        ).build()
        dao = database.mainDao()
    }

    @After
    fun closeDb() = database.close()

    @Test
    fun saveRoute_Success() = runTest {
        dao.insertRoute(sampleRoute)
        val actualRoute = dao.getAllDataForRoutes(SimpleSQLiteQuery("SELECT * FROM data")).first()
        assertEquals(sampleRoute.timestamp, actualRoute[0].timestamp)
    }

    @Test
    fun deleteRoute_Success() = runTest {
        dao.insertRoute(sampleRoute)
        dao.deleteRoute(sampleRoute.id ?: 1)
        val actualRoute = dao.getAllDataForRoutes(SimpleSQLiteQuery("SELECT * FROM data")).first()
        assertTrue(actualRoute.isEmpty())
    }
}