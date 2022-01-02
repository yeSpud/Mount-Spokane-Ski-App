package com.mtspokane.skiapp.skierlocation

import android.annotation.SuppressLint
import android.location.Location
import android.os.Build
import android.util.Log
import com.mtspokane.skiapp.mapItem.MapItem
import com.mtspokane.skiapp.mapItem.MtSpokaneMapItems

object Locations {

	private var previousLocation: Location? = null

	var visibleLocationUpdates: ArrayList<VisibleLocationUpdate> = ArrayList(0)

	private var vDirection: VerticalDirection = VerticalDirection.UNKNOWN

	val canUseAccuracy = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O

	private fun updateLocationVDirection(currentLocation: Location) {

		if (previousLocation != null) {
			if (currentLocation.altitude != 0.0 && previousLocation!!.altitude != 0.0) {
				if (canUseAccuracy) {
					vDirection = when {
						(currentLocation.altitude - currentLocation.verticalAccuracyMeters) > (previousLocation!!.altitude + previousLocation!!.verticalAccuracyMeters) -> VerticalDirection.UP_CERTAIN
						(currentLocation.altitude + currentLocation.verticalAccuracyMeters) < (previousLocation!!.altitude - previousLocation!!.verticalAccuracyMeters) -> VerticalDirection.DOWN_CERTAIN
						currentLocation.altitude > previousLocation!!.altitude -> VerticalDirection.UP
						currentLocation.altitude < previousLocation!!.altitude -> VerticalDirection.DOWN
						else -> VerticalDirection.FLAT
					}
				} else {
					Log.w("updateLocationDirection", "Device does not support altitude accuracy!")
					vDirection = when {
						currentLocation.altitude > previousLocation!!.altitude -> VerticalDirection.UP
						currentLocation.altitude < previousLocation!!.altitude -> VerticalDirection.DOWN
						else -> VerticalDirection.FLAT
					}
				}
			} else {
				vDirection = VerticalDirection.UNKNOWN
			}
		} else {
			vDirection = VerticalDirection.UNKNOWN
		}

		previousLocation = currentLocation
	}

	fun checkIfOnOther(location: Location): MapItem? {

		MtSpokaneMapItems.other.forEach {
			if (it != null && it.locationInsidePoints(location)) {
				return it
			}
		}

		return null
	}

	fun checkIfOnChairlift(location: Location): MapItem? {

		val numberOfChecks = 6
		val minimumConfidenceValue: Double = 4.0 / numberOfChecks
		var currentConfidence = 0

		updateLocationVDirection(location)

		// Check altitude.
		currentConfidence += getAltitudeConfidence()

		// Check speed.
		currentConfidence += getSpeedConfidence(location)

		if (!MtSpokaneMapItems.isSetup) {
			return null
		}

		var potentialChairlift: MapItem? = null
		MtSpokaneMapItems.chairlifts.forEach {
			if (it.locationInsidePoints(location)) {
				currentConfidence += 1
				potentialChairlift = it
			}
		}

		return if (currentConfidence / numberOfChecks >= minimumConfidenceValue) {
			potentialChairlift
		} else {
			null
		}
	}

	private fun getAltitudeConfidence(): Int {
		return when (vDirection) {
			VerticalDirection.UP_CERTAIN -> 3
			VerticalDirection.UP -> 2
			VerticalDirection.FLAT -> 1
			else -> 0
		}
	}

	private fun getSpeedConfidence(location: Location): Int {

		if (location.speed != 0.0F && previousLocation != null) {

			// 500 feet per minute to meters per second.
			val maxChairliftSpeed = 500.0F * 0.00508F

			if (canUseAccuracy) {
				if (location.speedAccuracyMetersPerSecond != 0.0F && previousLocation!!.speedAccuracyMetersPerSecond != 0.0F) {
					return when {
						location.speed + location.speedAccuracyMetersPerSecond <= maxChairliftSpeed -> 2
						location.speed <= maxChairliftSpeed -> 1
						else -> 0
					}
				}
			} else {
				Log.w("checkIfOnChairlift", "Device does not support speed accuracy!")
				return if (location.speed <= maxChairliftSpeed) {
					1
				} else {
					0
				}
			}
		}

		return 0
	}

	fun checkIfOnRun(location: Location): MapItem? {

		arrayOf(MtSpokaneMapItems.easyRuns, MtSpokaneMapItems.moderateRuns, MtSpokaneMapItems
			.difficultRuns).forEach { runDifficulty ->
			runDifficulty.forEach {
				if (it.locationInsidePoints(location)) {
					return it
				}
			}
		}

		return null
	}

	private enum class VerticalDirection {
		UP_CERTAIN, UP, FLAT, DOWN, DOWN_CERTAIN, UNKNOWN
	}
}

interface VisibleLocationUpdate {
	fun updateLocation(location: Location, locationString: String)
}