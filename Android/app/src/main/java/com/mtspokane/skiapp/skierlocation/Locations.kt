package com.mtspokane.skiapp.skierlocation

import android.location.Location
import android.os.Build
import com.mtspokane.skiapp.mapItem.MapItem
import com.mtspokane.skiapp.mapItem.MtSpokaneMapItems

object Locations {

	var previousLocation: Location? = null
	private set

	var currentLocation: Location? = null
	private set

	var chairliftConfidence = 0
	private set

	const val numberOfChairliftChecks = 5.0F

	var mostLikelyChairlift: MapItem? = null
	private set

	var visibleLocationUpdates: ArrayList<VisibleLocationUpdate> = ArrayList(0)

	fun updateLocations(newLocation: Location) {
		this.previousLocation = this.currentLocation
		this.currentLocation = newLocation
		this.chairliftConfidence = 0

		MtSpokaneMapItems.chairlifts.forEach {
			if (it.locationInsidePoints(this.currentLocation!!)) {
				this.mostLikelyChairlift = it
			}
		}
	}

	fun getVerticalDirection(): VerticalDirection {

		if (this.previousLocation == null || this.currentLocation == null) {
			return VerticalDirection.UNKNOWN
		}

		if (this.currentLocation!!.altitude == 0.0 || this.previousLocation!!.altitude == 0.0) {
			return VerticalDirection.UNKNOWN
		}


		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

			if ((this.currentLocation!!.altitude - this.currentLocation!!.verticalAccuracyMeters) > (this.previousLocation!!.altitude + this.previousLocation!!.verticalAccuracyMeters)) {
				return VerticalDirection.UP_CERTAIN
			} else if ((this.currentLocation!!.altitude + this.currentLocation!!.verticalAccuracyMeters) < (this.previousLocation!!.altitude - this.previousLocation!!.verticalAccuracyMeters)) {
				return VerticalDirection.DOWN_CERTAIN
			}
		}

		return when {
			this.currentLocation!!.altitude > previousLocation!!.altitude -> VerticalDirection.UP
			this.currentLocation!!.altitude < previousLocation!!.altitude -> VerticalDirection.DOWN
			else -> VerticalDirection.FLAT
		}
	}

	fun checkIfOnOther(): MapItem? {

		if (!MtSpokaneMapItems.isSetup || this.currentLocation == null) {
			return null
		}

		MtSpokaneMapItems.other.forEach {
			if (it.locationInsidePoints(this.currentLocation!!)) {
				return it
			}
		}

		return null
	}

	fun checkIfAtChairliftTerminals(): MapItem? {

		if (!MtSpokaneMapItems.isSetup || this.currentLocation == null) {
			return null
		}

		MtSpokaneMapItems.chairliftTerminals.forEach {
			if (it.locationInsidePoints(this.currentLocation!!)) {
				return it
			}
		}

		return null
	}

	fun getChairliftConfidencePercentage(): Float {

		if (!MtSpokaneMapItems.isSetup || this.currentLocation == null) {
			return 0.0F
		}

		// Check altitude.
		this.chairliftConfidence += getAltitudeConfidence()

		// Check speed.
		this.chairliftConfidence += getSpeedConfidence()

		return (this.chairliftConfidence / this.numberOfChairliftChecks)
	}

	private fun getAltitudeConfidence(): Int {
		return when (this.getVerticalDirection()) {
			VerticalDirection.UP_CERTAIN -> 3
			VerticalDirection.UP -> 2
			VerticalDirection.FLAT -> 1
			else -> 0
		}
	}

	private fun getSpeedConfidence(): Int {

		if (this.currentLocation == null || this.previousLocation == null) {
			return 0
		}

		if (this.currentLocation!!.speed == 0.0F || this.previousLocation!!.speed == 0.0F) {
			return 0
		}


		// 550 feet per minute to meters per second.
		val maxChairliftSpeed = 550.0F * 0.00508F

		// 150 feet per minute to meters per second.
		val minChairliftSpeed = 150.0F * 0.00508F

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			if (this.currentLocation!!.speedAccuracyMetersPerSecond > 0.0F && this.previousLocation!!.speedAccuracyMetersPerSecond > 0.0F) {
				 if ((this.currentLocation!!.speed - this.currentLocation!!.speedAccuracyMetersPerSecond >= minChairliftSpeed) && (this.currentLocation!!.speed + this.currentLocation!!.speedAccuracyMetersPerSecond <= maxChairliftSpeed)) {
					 return 2
				}
			}
		}

		return if (this.currentLocation!!.speed in minChairliftSpeed..maxChairliftSpeed) {
			1
		} else {
			0
		}

	}

	fun checkIfOnRun(): MapItem? {

		if (!MtSpokaneMapItems.isSetup || this.currentLocation == null) {
			return null
		}

		arrayOf(MtSpokaneMapItems.easyRuns, MtSpokaneMapItems.moderateRuns,
			MtSpokaneMapItems.difficultRuns).forEach { runDifficulty ->
			runDifficulty.forEach {
				if (it.locationInsidePoints(this.currentLocation!!)) {
					return it
				}
			}
		}

		return null
	}

	enum class VerticalDirection {
		UP_CERTAIN, UP, FLAT, DOWN, DOWN_CERTAIN, UNKNOWN
	}

	interface VisibleLocationUpdate {
		fun updateLocation(locationString: String)
	}
}