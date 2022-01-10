package com.mtspokane.skiapp.skierlocation

import com.mtspokane.skiapp.activitysummary.SkiingActivity
import com.mtspokane.skiapp.mapItem.MapItem
import com.mtspokane.skiapp.mapItem.MtSpokaneMapItems

object Locations {

	var previousLocation: SkiingActivity? = null
	private set

	var currentLocation: SkiingActivity? = null
	private set

	var altitudeConfidence: UShort = 0u
	private set

	var speedConfidence: UShort = 0u
	private set

	var mostLikelyChairlift: MapItem? = null
	private set

	var visibleLocationUpdates: ArrayList<VisibleLocationUpdate> = ArrayList(0)

	fun updateLocations(skiingActivity: SkiingActivity) {
		this.previousLocation = this.currentLocation
		this.currentLocation = skiingActivity

		this.altitudeConfidence = when (this.getVerticalDirection()) {
			VerticalDirection.UP_CERTAIN -> 3u
			VerticalDirection.UP -> 2u
			VerticalDirection.FLAT -> 1u
			else -> 0u
		}

		this.speedConfidence = this.getSpeedConfidenceValue()

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

		if (this.currentLocation!!.altitudeAccuracy != null && this.previousLocation!!.altitudeAccuracy != null) {
			if ((this.currentLocation!!.altitude - this.currentLocation!!.altitudeAccuracy!!) >
				(this.previousLocation!!.altitude + this.previousLocation!!.altitudeAccuracy!!)) {
				return VerticalDirection.UP_CERTAIN
			} else if ((this.currentLocation!!.altitude + this.currentLocation!!.altitudeAccuracy!!) <
				(this.previousLocation!!.altitude - this.previousLocation!!.altitudeAccuracy!!)) {
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

	private fun getSpeedConfidenceValue(): UShort {

		if (this.currentLocation == null || this.previousLocation == null) {
			return 0u
		}

		if (this.currentLocation!!.speed == 0.0F || this.previousLocation!!.speed == 0.0F) {
			return 0u
		}


		// 550 feet per minute to meters per second.
		val maxChairliftSpeed = 550.0F * 0.00508F

		// 150 feet per minute to meters per second.
		val minChairliftSpeed = 150.0F * 0.00508F

		if (this.currentLocation!!.speedAccuracy != null && this.previousLocation!!.speedAccuracy != null) {
			if (this.currentLocation!!.speedAccuracy!! > 0.0F && this.previousLocation!!.speedAccuracy!! > 0.0F) {
				 if ((this.currentLocation!!.speed - this.currentLocation!!.speedAccuracy!! >= minChairliftSpeed)
					 && (this.currentLocation!!.speed + this.currentLocation!!.speedAccuracy!! <= maxChairliftSpeed)) {
					 return 2u
				}
			}
		}

		return if (this.currentLocation!!.speed in minChairliftSpeed..maxChairliftSpeed) {
			1u
		} else {
			0u
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