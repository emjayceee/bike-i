package com.moterroute.finder.algoImplement

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import com.google.android.gms.maps.model.LatLng
import com.google.maps.DistanceMatrixApi
import com.google.maps.DistanceMatrixApiRequest
import com.google.maps.GeoApiContext
import com.google.maps.model.TravelMode
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import java.io.File

open class AStarAlgorithm(private val context: Context, private val apiKey: String) {

    private val geoApiContext: GeoApiContext = GeoApiContext.Builder()
        .apiKey(apiKey)
        .build()

    @RequiresApi(Build.VERSION_CODES.N)
    fun findOptimalPath(start: LatLng, end: LatLng): List<LatLng> {
        val openSet = mutableSetOf<LatLng>()
        val closedSet = mutableSetOf<LatLng>()
        val cameFrom = mutableMapOf<LatLng, LatLng>()
        val gScore = mutableMapOf<LatLng, Double>()
        val fScore = mutableMapOf<LatLng, Double>()

        gScore[start] = 0.0
        fScore[start] = calculateHeuristic(start, end)

        openSet.add(start)

        while (openSet.isNotEmpty()) {
            val current = openSet.minByOrNull { fScore.getOrDefault(it, Double.MAX_VALUE) }
                ?: throw IllegalStateException("No valid path found")

            if (current == end) {
                return reconstructPath(cameFrom, current)
            }

            openSet.remove(current)
            closedSet.add(current)

            val neighbors = getNeighbors(current)
            for (neighbor in neighbors) {
                if (closedSet.contains(neighbor)) {
                    continue
                }

                val tentativeGScore = gScore.getOrDefault(current, Double.MAX_VALUE) + calculateDistance(current, neighbor)

                if (!openSet.contains(neighbor)) {
                    openSet.add(neighbor)
                } else if (tentativeGScore >= gScore.getOrDefault(neighbor, Double.MAX_VALUE)) {
                    continue
                }

                cameFrom[neighbor] = current
                gScore[neighbor] = tentativeGScore
                fScore[neighbor] = tentativeGScore + calculateHeuristic(neighbor, end)
            }
        }

        return emptyList()
    }

    private fun calculateHeuristic(start: LatLng, end: LatLng): Double {
        val earthRadius = 6371.0 // Earth's radius in kilometers
        val startLatRad = Math.toRadians(start.latitude)
        val endLatRad = Math.toRadians(end.latitude)
        val latDiffRad = Math.toRadians(end.latitude - start.latitude)
        val lngDiffRad = Math.toRadians(end.longitude - start.longitude)

        val a = sin(latDiffRad / 2) * sin(latDiffRad / 2) +
                cos(startLatRad) * cos(endLatRad) *
                sin(lngDiffRad / 2) * sin(lngDiffRad / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return earthRadius * c
    }

    private fun calculateDistance(source: LatLng, destination: LatLng): Double {
        val apiKey = "AIzaSyBhEVkpM82FqGfCBXgb5yiyOfMn_yHt23I" // Google Maps API key
        val geoApiContext = GeoApiContext.Builder()
            .apiKey(apiKey)
            .build()

        try {
            val result = DistanceMatrixApi.newRequest(geoApiContext)
                .origins(source)
                .destinations(destination)
                .mode(TravelMode.DRIVING)
                .await()

            val distanceInMeters = result.rows[0].elements[0].distance.inMeters

            return distanceInMeters / 1000.0
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return 0.0
    }

    private fun getNeighbors(node: LatLng, coordinatesFilePath: String): List<LatLng> {
        val neighbors = mutableListOf<LatLng>()
        val coordinates = loadCoordinatesFromFile(coordinatesFilePath)

        for (i in 1..100) {
            val latitudeOffset = 0.1 * (i / 5) // latitude offset
            val longitudeOffset = 0.1 * (i % 5) // longitude offset

            val neighbor = LatLng(node.latitude + latitudeOffset, node.longitude + longitudeOffset)
            neighbors.add(neighbor)

            if (neighbors.size >= 100)
                break
        }

        return neighbors
    }

    private fun loadCoordinatesFromFile(coordinatesFilePath: String): List<LatLng> {
        val coordinates = mutableListOf<LatLng>()
        val fileLines = File(coordinatesFilePath).readLines()

        for (line in fileLines) {
            val parts = line.split(",")
            if (parts.size == 2) {
                val latitude = parts[0].toDouble()
                val longitude = parts[1].toDouble()
                val coordinate = LatLng(latitude, longitude)
                coordinates.add(coordinate)
            }
        }

        return coordinates
    }


    private fun reconstructPath(cameFrom: Map<LatLng, LatLng>, current: LatLng): List<LatLng> {
        val path = mutableListOf(current)
        var node: LatLng = current

        while (cameFrom.containsKey(node)) {
            node = cameFrom[node]!!
            path.add(node)
        }

        path.reverse()
        return path
    }

    private fun getDistance(source: LatLng, destination: LatLng): Double {
        try {
            val result = origins(source)
                .destinations(destination)
                .mode(TravelMode.BICYCLING)
                .await()

            return result.rows[0].elements[0].distance.inMeters / 1000.0 // Convert meters to kilometers
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return 0.0
    }
}

private fun DistanceMatrixApiRequest.origins(source: LatLng) {

}

data class LatLng(val latitude: Double, val longitude: Double)

private fun readCoordinatesFromFile(fileName: String): List<LatLng> {
    val coordinates = mutableListOf<LatLng>()
    File(fileName).forEachLine { line ->
        val (latitude, longitude) = line.split(",")
        val coordinate = LatLng(latitude.toDouble(), longitude.toDouble())
        coordinates.add(coordinate)
    }
    return coordinates
}

private fun origins(source: LatLng) {
    val coordinates = readCoordinatesFromFile("coordinates.txt")
    // Use the loaded coordinates as needed
    for ((index, coordinate) in coordinates.withIndex()) {
        println("Origin ${index + 1}: (${coordinate.latitude}, ${coordinate.longitude})")
    }
}


