package com.mtspokane.skiapp

import android.location.Location
import android.os.Build
import android.util.Log
import androidx.annotation.AnyThread
import androidx.annotation.MainThread
import kotlinx.coroutines.*


object Locations {

	var currentRun: String = ""
	private set

	var chairliftName: String = ""
	private set

	var otherName: String = ""
	private set

	private var previousLocation: Location? = null

	private var vDirection: VerticalDirection = VerticalDirection.UNKNOWN

	private val canUseAccuracy = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O

	private fun updateLocationVDirection(currentLocation: Location) {

		if (this.previousLocation != null) {
			if (currentLocation.altitude != 0.0 && this.previousLocation!!.altitude != 0.0) {
				if (this.canUseAccuracy) {
					when {
						(currentLocation.altitude - currentLocation.verticalAccuracyMeters) > (this.previousLocation!!.altitude + this.previousLocation!!.verticalAccuracyMeters) -> this.vDirection = VerticalDirection.UP_CERTAIN
						(currentLocation.altitude + currentLocation.verticalAccuracyMeters) < (this.previousLocation!!.altitude - this.previousLocation!!.verticalAccuracyMeters) -> this.vDirection = VerticalDirection.DOWN_CERTAIN
						currentLocation.altitude > this.previousLocation!!.altitude -> this.vDirection = VerticalDirection.UP
						currentLocation.altitude < this.previousLocation!!.altitude -> this.vDirection = VerticalDirection.DOWN
						else -> this.vDirection = VerticalDirection.FLAT
					}
				} else {
					Log.w("updateLocationDirection", "Device does not support altitude accuracy!")
					when {
						currentLocation.altitude > this.previousLocation!!.altitude -> this.vDirection = VerticalDirection.UP
						currentLocation.altitude < this.previousLocation!!.altitude -> this.vDirection = VerticalDirection.DOWN
						else -> this.vDirection = VerticalDirection.FLAT
					}
				}
			} else {
				this.vDirection = VerticalDirection.UNKNOWN
			}
		} else {
			this.vDirection = VerticalDirection.UNKNOWN
		}

		this.previousLocation = currentLocation
	}

	@MainThread
	suspend fun checkIfOnOther(location: Location, otherItem: Array<MapItem>): Boolean = coroutineScope {

		otherItem.forEach {
			if (it.pointInsidePolygon(location)) {
				this@Locations.otherName = it.name
				return@coroutineScope true
			}
		}

		return@coroutineScope false
	}

	@MainThread
	suspend fun checkIfOnChairlift(location: Location, chairlifts: Array<MapItem>): Boolean = coroutineScope {

		val numberOfChecks = 6
		val minimumConfidenceValue = 4/numberOfChecks
		var currentConfidence = 0

		this@Locations.updateLocationVDirection(location)

		currentConfidence += when (this@Locations.vDirection) {
			VerticalDirection.UP_CERTAIN -> 3
			VerticalDirection.UP -> 2
			VerticalDirection.FLAT -> 1
			else -> 0
		}

		// Check speed.
		if (location.speed != 0.0F && this@Locations.previousLocation != null) {

			// 500 feet per minute to meters per second.
			val maxChairliftSpeed = 500.0F * 0.00508F

			if (this@Locations.canUseAccuracy) {
				if (location.speedAccuracyMetersPerSecond != 0.0F && this@Locations.previousLocation!!.speedAccuracyMetersPerSecond != 0.0F) {
					currentConfidence += when {
						location.speed + location.speedAccuracyMetersPerSecond <= maxChairliftSpeed -> 2
						location.speed <= maxChairliftSpeed -> 1
						else -> 0
					}
				}
			} else {
				Log.w("checkIfOnChairlift", "Device does not support speed accuracy!")
				currentConfidence += if (location.speed <= maxChairliftSpeed) {
					1
				} else {
					0
				}
			}
		}

		chairlifts.forEach {
			if (it.pointInsidePolygon(location)) {
				this@Locations.chairliftName = it.name
				currentConfidence += 1
			}
		}

		return@coroutineScope currentConfidence / numberOfChecks >= minimumConfidenceValue
	}

	suspend fun checkIfOnRun(location: Location, mapHandler: MapHandler): Boolean = coroutineScope {

		var easyRunName: String? = null
		var moderateRunName: String? = null
		var difficultRunName: String? = null

		val nameJobs = listOf(
			async(Dispatchers.IO) {
				easyRunName = getRunNameFromPoint(location, mapHandler.easyRuns.values)
				Log.v("checkIfOnRun", "Finished checking names for easy runs") },
			async(Dispatchers.IO) {
				moderateRunName = getRunNameFromPoint(location, mapHandler.moderateRuns.values)
				Log.v("checkIfOnRun", "Finished checking names for moderate runs") },
			async(Dispatchers.IO) {
				difficultRunName = getRunNameFromPoint(location, mapHandler.difficultRuns.values)
				Log.v("checkIfOnRun", "Finished checking names for difficult runs") }
		)

		nameJobs.awaitAll()

		var runName = ""
		when {
			easyRunName != null -> runName = easyRunName!!
			moderateRunName != null -> runName = moderateRunName!!
			difficultRunName != null -> runName = difficultRunName!!
		}

		if (runName != "") {
			if (runName == this@Locations.currentRun) {
				Log.d("checkIfOnRun", "Still on $runName")
			} else {
				Log.d("checkIfOnRun", "On run: $runName")
				this@Locations.currentRun = runName

			}
			return@coroutineScope true
		} else {
			Log.i("checkIfOnRun", "Unable to determine run")
			return@coroutineScope false
		}
	}

	@AnyThread
	private suspend fun getRunNameFromPoint(location: Location, items: Collection<MapItem>): String? = coroutineScope {
		withContext(Dispatchers.Main) {
			for (mapItem: MapItem in items) {
				if (mapItem.pointInsidePolygon(location)) {
					return@withContext mapItem.name
				}
			}
		}
		return@coroutineScope null
	}

	private enum class VerticalDirection {
		UP_CERTAIN, UP, FLAT, DOWN, DOWN_CERTAIN, UNKNOWN
	}
}