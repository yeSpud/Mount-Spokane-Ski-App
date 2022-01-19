package com.mtspokane.skiapp.activities.activitysummary

import android.util.Log
import com.mtspokane.skiapp.mapItem.MapItem
import com.mtspokane.skiapp.mapItem.MtSpokaneMapItems
import com.mtspokane.skiapp.skierlocation.Locations

object ActivitySummaryLocations: Locations<SkiingActivity>() {

	override fun updateLocations(newVariable: SkiingActivity) {
		this.previousLocation = this.currentLocation

		this.altitudeConfidence = when (this.getVerticalDirection()) {
			VerticalDirection.UP_CERTAIN -> 3u
			VerticalDirection.UP -> 2u
			VerticalDirection.FLAT -> 1u
			else -> 0u
		}

		this.speedConfidence = this.getSpeedConfidenceValue()

		MtSpokaneMapItems.chairlifts.forEach {
			if (it.locationInsidePoints(newVariable.latitude, newVariable.longitude)) {
				this.mostLikelyChairlift = it
			}
		}

		this.currentLocation = newVariable
	}

	override fun getVerticalDirection(): VerticalDirection {

		if (this.previousLocation == null || this.currentLocation == null) {
			return VerticalDirection.UNKNOWN
		}

		if (this.currentLocation!!.altitude == 0.0 || this.previousLocation!!.altitude == 0.0) {
			return VerticalDirection.UNKNOWN
		}


		if (this.currentLocation!!.altitudeAccuracy != null && this.previousLocation!!
				.altitudeAccuracy != null) {

			if ((this.currentLocation!!.altitude - this.currentLocation!!.altitudeAccuracy!!)
				> (this.previousLocation!!.altitude + this.previousLocation!!.altitudeAccuracy!!)) {
				return VerticalDirection.UP_CERTAIN
			} else if ((this.currentLocation!!.altitude + this.currentLocation!!.altitudeAccuracy!!)
				< (this.previousLocation!!.altitude - this.previousLocation!!.altitudeAccuracy!!)) {
				return VerticalDirection.DOWN_CERTAIN
			}
		}

		return when {
			this.currentLocation!!.altitude > this.previousLocation!!.altitude -> VerticalDirection.UP
			this.currentLocation!!.altitude < this.previousLocation!!.altitude -> VerticalDirection.DOWN
			else -> VerticalDirection.FLAT
		}
	}

	override fun checkIfOnOther(): MapItem? {

		if (!MtSpokaneMapItems.isSetup || this.currentLocation == null) {
			Log.w("checkIfOnOther", "Map items are not set up")
			return null
		}

		MtSpokaneMapItems.other.forEach {
			if (it.locationInsidePoints(this.currentLocation!!.latitude, this.currentLocation!!.longitude)) {
				return it
			}
		}

		return null
	}

	override fun checkIfAtChairliftTerminals(): MapItem? {

		if (!MtSpokaneMapItems.isSetup || this.currentLocation == null) {
			Log.w("checkChairliftTerminals", "Map items are not set up")
			return null
		}

		MtSpokaneMapItems.chairliftTerminals.forEach {
			if (it.locationInsidePoints(this.currentLocation!!.latitude, this.currentLocation!!.longitude)) {
				return it
			}
		}

		return null
	}

	override fun checkIfOnRun(): MapItem? {

		if (!MtSpokaneMapItems.isSetup || this.currentLocation == null) {
			Log.w("checkIfOnRun", "Map items are not set up")
			return null
		}

		arrayOf(MtSpokaneMapItems.easyRuns, MtSpokaneMapItems.moderateRuns,
			MtSpokaneMapItems.difficultRuns).forEach { runDifficulty ->
			runDifficulty.forEach {
				if (it.locationInsidePoints(this.currentLocation!!.latitude, this.currentLocation!!.longitude)) {
					return it
				}
			}
		}

		return null
	}

	override fun getSpeedConfidenceValue(): UShort {

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
				if ((this.currentLocation!!.speed - this.currentLocation!!.speedAccuracy!!
							>= minChairliftSpeed) && (this.currentLocation!!.speed +
							this.currentLocation!!.speedAccuracy!! <= maxChairliftSpeed)) {
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
}