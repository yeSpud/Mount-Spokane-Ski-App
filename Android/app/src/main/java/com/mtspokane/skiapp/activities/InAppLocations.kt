package com.mtspokane.skiapp.activities

import android.location.Location
import android.os.Build
import android.util.Log
import com.mtspokane.skiapp.mapItem.MapItem
import com.mtspokane.skiapp.mapItem.MtSpokaneMapItems
import com.mtspokane.skiapp.skierlocation.Locations

object InAppLocations: Locations<Location>() {

	var visibleLocationUpdates: ArrayList<VisibleLocationUpdate> = ArrayList(0)

	override fun updateLocations(newVariable: Location) {
		this.previousLocation = this.currentLocation

		this.altitudeConfidence = when (getVerticalDirection()) {
			VerticalDirection.UP_CERTAIN -> 3u
			VerticalDirection.UP -> 2u
			VerticalDirection.FLAT -> 1u
			else -> 0u
		}

		this.speedConfidence = getSpeedConfidenceValue()

		MtSpokaneMapItems.chairlifts.forEach {
			if (it.locationInsidePoints(newVariable)) {
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


		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

			if ((this.currentLocation!!.altitude - this.currentLocation!!.verticalAccuracyMeters) >
				(this.previousLocation!!.altitude + this.previousLocation!!.verticalAccuracyMeters)) {
				return VerticalDirection.UP_CERTAIN
			} else if ((this.currentLocation!!.altitude + this.currentLocation!!.verticalAccuracyMeters) <
				(this.previousLocation!!.altitude - this.previousLocation!!.verticalAccuracyMeters)) {
				return VerticalDirection.DOWN_CERTAIN
			}
		}

		return when {
			this.currentLocation!!.altitude > previousLocation!!.altitude -> VerticalDirection.UP
			this.currentLocation!!.altitude < previousLocation!!.altitude -> VerticalDirection.DOWN
			else -> VerticalDirection.FLAT
		}
	}

	override fun checkIfOnOther(): MapItem? {

		if (!MtSpokaneMapItems.isSetup || this.currentLocation == null) {
			Log.w("checkIfOnOther", "Map items are not set up")
			return null
		}

		MtSpokaneMapItems.other.forEach {
			if (it.locationInsidePoints(this.currentLocation!!)) {
				this.mostLikelyChairlift = null
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
			if (it.locationInsidePoints(this.currentLocation!!)) {
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
				if (it.locationInsidePoints(this.currentLocation!!)) {
					this.mostLikelyChairlift = null
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

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			if (this.currentLocation!!.speedAccuracyMetersPerSecond > 0.0F &&
				this.previousLocation!!.speedAccuracyMetersPerSecond > 0.0F) {
				if ((this.currentLocation!!.speed - this.currentLocation!!.speedAccuracyMetersPerSecond >=
							minChairliftSpeed) && (this.currentLocation!!.speed +
							this.currentLocation!!.speedAccuracyMetersPerSecond <= maxChairliftSpeed)) {
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

	interface VisibleLocationUpdate {
		fun updateLocation(locationString: String)
	}
}