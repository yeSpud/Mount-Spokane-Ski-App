package com.mtspokane.skiapp.activities.activitysummary

import android.util.Log
import com.mtspokane.skiapp.mapItem.MapItem
import com.mtspokane.skiapp.mapItem.MtSpokaneMapItems

object ActivitySummaryLocations {

	var previousSkiingActivity: SkiingActivity? = null
		private set

	var currentSkiingActivity: SkiingActivity? = null
		private set

	var altitudeConfidence: UShort = 0u
		private set

	var speedConfidence: UShort = 0u
		private set

	var mostLikelyChairlift: MapItem? = null
		private set

	fun updateLocations(newSkiingActivity: SkiingActivity) {
		this.previousSkiingActivity = this.currentSkiingActivity

		this.altitudeConfidence = when (this.getVerticalDirection()) {
			VerticalDirection.UP_CERTAIN -> 3u
			VerticalDirection.UP -> 2u
			VerticalDirection.FLAT -> 1u
			else -> 0u
		}

		this.speedConfidence = this.getSpeedConfidenceValue()

		MtSpokaneMapItems.chairlifts.forEach {
			if (it.locationInsidePoints(newSkiingActivity.latitude, newSkiingActivity.longitude)) {
				this.mostLikelyChairlift = it
			}
		}

		this.currentSkiingActivity = newSkiingActivity
	}

	private fun getVerticalDirection(): VerticalDirection {

		if (this.previousSkiingActivity == null || this.currentSkiingActivity == null) {
			return VerticalDirection.UNKNOWN
		}

		if (this.currentSkiingActivity!!.altitude == 0.0 || this.previousSkiingActivity!!.altitude == 0.0) {
			return VerticalDirection.UNKNOWN
		}


		if (this.currentSkiingActivity!!.altitudeAccuracy != null && this.previousSkiingActivity!!
				.altitudeAccuracy != null) {

			if ((this.currentSkiingActivity!!.altitude - this.currentSkiingActivity!!.altitudeAccuracy!!)
				> (this.previousSkiingActivity!!.altitude + this.previousSkiingActivity!!.altitudeAccuracy!!)) {
				return VerticalDirection.UP_CERTAIN
			} else if ((this.currentSkiingActivity!!.altitude + this.currentSkiingActivity!!.altitudeAccuracy!!)
				< (this.previousSkiingActivity!!.altitude - this.previousSkiingActivity!!.altitudeAccuracy!!)) {
				return VerticalDirection.DOWN_CERTAIN
			}
		}

		return when {
			this.currentSkiingActivity!!.altitude > previousSkiingActivity!!.altitude -> VerticalDirection.UP
			this.currentSkiingActivity!!.altitude < previousSkiingActivity!!.altitude -> VerticalDirection.DOWN
			else -> VerticalDirection.FLAT
		}
	}

	fun checkIfOnOther(): MapItem? {

		if (!MtSpokaneMapItems.isSetup || this.currentSkiingActivity == null) {
			Log.w("checkIfOnOther", "Map items are not set up")
			return null
		}

		MtSpokaneMapItems.other.forEach {
			if (it.locationInsidePoints(this.currentSkiingActivity!!.latitude,
					this.currentSkiingActivity!!.longitude)) {
				return it
			}
		}

		return null
	}

	fun checkIfAtChairliftTerminals(): MapItem? {

		if (!MtSpokaneMapItems.isSetup || this.currentSkiingActivity == null) {
			Log.w("checkChairliftTerminals", "Map items are not set up")
			return null
		}

		MtSpokaneMapItems.chairliftTerminals.forEach {
			if (it.locationInsidePoints(this.currentSkiingActivity!!.latitude,
				this.currentSkiingActivity!!.longitude)) {
				return it
			}
		}

		return null
	}

	private fun getSpeedConfidenceValue(): UShort {

		if (this.currentSkiingActivity == null || this.previousSkiingActivity == null) {
			return 0u
		}

		if (this.currentSkiingActivity!!.speed == 0.0F || this.previousSkiingActivity!!.speed == 0.0F) {
			return 0u
		}


		// 550 feet per minute to meters per second.
		val maxChairliftSpeed = 550.0F * 0.00508F

		// 150 feet per minute to meters per second.
		val minChairliftSpeed = 150.0F * 0.00508F

		if (this.currentSkiingActivity!!.speedAccuracy != null && this.previousSkiingActivity!!.speedAccuracy != null) {
			if (this.currentSkiingActivity!!.speedAccuracy!! > 0.0F && this.previousSkiingActivity!!.speedAccuracy!! > 0.0F) {
				if ((this.currentSkiingActivity!!.speed - this.currentSkiingActivity!!.speedAccuracy!!
							>= minChairliftSpeed) && (this.currentSkiingActivity!!.speed +
							this.currentSkiingActivity!!.speedAccuracy!! <= maxChairliftSpeed)) {
					return 2u
				}
			}
		}

		return if (this.currentSkiingActivity!!.speed in minChairliftSpeed..maxChairliftSpeed) {
			1u
		} else {
			0u
		}

	}

	fun checkIfOnRun(): MapItem? {

		if (!MtSpokaneMapItems.isSetup || this.currentSkiingActivity == null) {
			Log.w("checkIfOnRun", "Map items are not set up")
			return null
		}

		arrayOf(
			MtSpokaneMapItems.easyRuns, MtSpokaneMapItems.moderateRuns,
			MtSpokaneMapItems.difficultRuns).forEach { runDifficulty ->
			runDifficulty.forEach {
				if (it.locationInsidePoints(this.currentSkiingActivity!!.latitude,
						this.currentSkiingActivity!!.longitude)) {
					return it
				}
			}
		}

		return null
	}

	enum class VerticalDirection {
		UP_CERTAIN, UP, FLAT, DOWN, DOWN_CERTAIN, UNKNOWN
	}
}