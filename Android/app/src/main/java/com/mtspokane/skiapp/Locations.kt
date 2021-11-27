package com.mtspokane.skiapp

import android.location.Location
import android.os.Build
import android.util.Log
import androidx.annotation.AnyThread
import com.mtspokane.skiapp.mapItem.MapItem
import com.mtspokane.skiapp.mapItem.MtSpokaneMapItems
import com.mtspokane.skiapp.mapItem.VisibleUIMapItem
import kotlinx.coroutines.*

object Locations {

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

	@AnyThread
	suspend fun checkIfOnOtherAsync(location: Location): MapItem? = coroutineScope {

		withContext(Dispatchers.Main) {
			MtSpokaneMapItems.other.forEach {
				if (it != null && it.locationInsidePolygons(location)) {
					return@withContext it
				}
			}

			return@withContext null
		}
	}

	fun checkIfOnOther(location: Location): MapItem? {

		MtSpokaneMapItems.other.forEach { if (it != null && it.locationInsidePoints(location)) { return it } }

		return null
	}

	@AnyThread
	suspend fun checkIfOnChairliftAsync(location: Location): MapItem? = coroutineScope {

		val numberOfChecks = 6
		val minimumConfidenceValue: Double = 4.0/numberOfChecks
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

		if (!MtSpokaneMapItems.isSetup) {
			return@coroutineScope null
		}

		var potentialChairlift: MapItem? = null

		withContext(Dispatchers.Main) {
			MtSpokaneMapItems.chairlifts.forEach {
				if (it.locationInsidePolygons(location)) {
					currentConfidence += 1
					potentialChairlift = it
				}
			}
		}

		if (currentConfidence / numberOfChecks >= minimumConfidenceValue) {
			return@coroutineScope potentialChairlift
		} else {
			return@coroutineScope null
		}
	}

	fun checkIfOnChairlift(location: Location): MapItem? {
		// TODO
		return null
	}

	@AnyThread
	suspend fun checkIfOnRunAsync(location: Location): MapItem? = coroutineScope {

		if (!MtSpokaneMapItems.isSetup) { return@coroutineScope null }

		val runArrays: Array<Array<VisibleUIMapItem>> = arrayOf(MtSpokaneMapItems.easyRuns,
			MtSpokaneMapItems.moderateRuns, MtSpokaneMapItems.difficultRuns)

		withContext(Dispatchers.Main) {
			runArrays.forEach { runs -> runs.forEach { if (it.locationInsidePolygons(location)) { return@withContext it } } }
		}

		return@coroutineScope null
	}

	fun checkIfOnRun(location: Location): MapItem? {

		val runs: Array<Array<VisibleUIMapItem>> = arrayOf(MtSpokaneMapItems.easyRuns,
			MtSpokaneMapItems.moderateRuns, MtSpokaneMapItems.difficultRuns)

		runs.forEach { runDifficulty -> runDifficulty.forEach { if (it.locationInsidePoints(location)) { return it } } }

		return null
	}

	private enum class VerticalDirection {
		UP_CERTAIN, UP, FLAT, DOWN, DOWN_CERTAIN, UNKNOWN
	}
}