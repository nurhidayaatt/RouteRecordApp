package com.nurhidayaatt.routerecordapp.util

import com.google.maps.android.PolyUtil
import com.nurhidayaatt.routerecordapp.data.source.local.entity.DataEntity
import com.nurhidayaatt.routerecordapp.data.source.local.entity.DistanceEntity
import com.nurhidayaatt.routerecordapp.data.source.local.entity.DurationEntity
import com.nurhidayaatt.routerecordapp.data.source.local.entity.RoutesEntity
import com.nurhidayaatt.routerecordapp.domain.model.Routes
import com.nurhidayaatt.routerecordapp.presentation.main.ActivityType

object DataMapper {
    fun mapFromRouteToDataEntity(routes: Routes): DataEntity {
        return DataEntity(
            typeActivity = routes.typeActivity!!,
            timestamp = routes.timestamp,
            img = routes.img!!,
            elapsedTime = routes.elapsedTimePerKm.sum(),
            elapsedTimePerKm = DurationEntity(duration = routes.elapsedTimePerKm),
            elapsedTimePerRoute = DurationEntity(duration = routes.elapsedTimePerRoute),
            movingTime = routes.movingTime,
            pacePerKm = DurationEntity(duration = routes.pacePerKm),
            routes = RoutesEntity(routes = routes.routes.map { PolyUtil.encode(it) }),
            distance = routes.distance,
            distancePerRoute = DistanceEntity(distances = routes.distancePerRoute)
        )
    }

    fun mapFromDataEntityToRoutes(dataEntity: List<DataEntity>): List<Routes> {
        return dataEntity.map { entity ->
            Routes(
                id = entity.id,
                typeActivity = entity.typeActivity,
                timestamp = entity.timestamp,
                img = entity.img,
                elapsedTime = entity.elapsedTime,
                elapsedTimePerKm = entity.elapsedTimePerKm.duration,
                elapsedTimePerRoute = entity.elapsedTimePerRoute.duration,
                movingTime = entity.movingTime,
                pacePerKm = entity.pacePerKm.duration,
                routes = entity.routes.routes.map { PolyUtil.decode(it) },
                distance = entity.distance,
                distancePerRoute = entity.distancePerRoute.distances
            )
        }
    }
}