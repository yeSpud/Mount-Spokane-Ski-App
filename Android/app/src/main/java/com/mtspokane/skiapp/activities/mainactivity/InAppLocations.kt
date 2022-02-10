package com.mtspokane.skiapp.activities.mainactivity

import android.graphics.Color
import android.location.Location
import android.os.Build
import android.util.Log
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.mtspokane.skiapp.R
import com.mtspokane.skiapp.databases.SkiingActivity
import com.mtspokane.skiapp.mapItem.MapMarker
import com.mtspokane.skiapp.mapItem.MtSpokaneMapItems
import com.mtspokane.skiapp.activities.Locations

object InAppLocations: Locations<Location>() {

	var visibleLocationUpdates: MutableList<VisibleLocationUpdate> = mutableListOf()

	override fun updateLocations(newVariable: Location) {
		this.previousLocation = this.currentLocation

		this.altitudeConfidence = when (this.getVerticalDirection()) {
			VerticalDirection.UP_CERTAIN -> 3u
			VerticalDirection.UP -> 2u
			VerticalDirection.FLAT -> 1u
			else -> 0u
		}

		this.speedConfidence = getSpeedConfidenceValue()

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

	override fun checkIfOnOther(): MapMarker? {

		if (MtSpokaneMapItems.other == null || this.currentLocation == null) {
			Log.w("checkIfOnOther", "Other map item has not been set up")
			return null
		}

		MtSpokaneMapItems.other!!.forEach {
			if (it.locationInsidePoints(this.currentLocation!!)) {

				return when (it.icon) {
					R.drawable.ic_parking -> MapMarker(it.name, SkiingActivity(this.currentLocation!!),
							it.icon, BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA),
							Color.GRAY)
					R.drawable.ic_ski_patrol_icon -> MapMarker(it.name, SkiingActivity(this.currentLocation!!),
							it.icon, BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA),
							Color.WHITE)
					else -> MapMarker(it.name, SkiingActivity(this.currentLocation!!),
							it.icon ?: R.drawable.ic_missing, BitmapDescriptorFactory
							.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA), Color.MAGENTA)
				}
			}
		}

		return null
	}

	override fun checkIfIOnChairlift(): MapMarker? {

		if (MtSpokaneMapItems.chairlifts == null || this.currentLocation == null) {
			Log.w("checkIfIOnChairlift", "Chairlifts have not been set up")
			return null
		}

		val vDirection: VerticalDirection = this.getVerticalDirection()
		if (vDirection == VerticalDirection.DOWN || vDirection == VerticalDirection.DOWN_CERTAIN) {

			return null
		}

		if (this.altitudeConfidence < 1u || this.speedConfidence < 1u) {

			return null
		}

		MtSpokaneMapItems.chairlifts!!.forEach {
			if (it.locationInsidePoints(this.currentLocation!!)) {
				return MapMarker(it.name, SkiingActivity(this.currentLocation!!), R.drawable.ic_chairlift,
						BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED), Color.RED)

			}
		}

		return null
	}

	override fun checkIfAtChairliftTerminals(): MapMarker? {

		if (MtSpokaneMapItems.chairliftTerminals == null || this.currentLocation == null) {
			Log.w("checkChairliftTerminals", "Chairlift terminals have not been set up")
			return null
		}

		MtSpokaneMapItems.chairliftTerminals!!.forEach {
			if (it.locationInsidePoints(this.currentLocation!!)) {

				return MapMarker(it.name, SkiingActivity(this.currentLocation!!), R.drawable.ic_chairlift,
						BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED), Color.RED)
			}
		}

		return null
	}

	override fun checkIfOnRun(): MapMarker? {

		if (MtSpokaneMapItems.easyRuns == null || MtSpokaneMapItems.moderateRuns == null ||
			MtSpokaneMapItems.difficultRuns == null || this.currentLocation == null) {
			Log.w("checkIfOnRun", "Ski runs have not been set up")
			return null
		}

		MtSpokaneMapItems.easyRuns!!.forEach {
			if (it.locationInsidePoints(this.currentLocation!!)) {
				return MapMarker(it.name, SkiingActivity(this.currentLocation!!), R.drawable.ic_easy,
						BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN),
						Color.GREEN)
			}
		}

		MtSpokaneMapItems.moderateRuns!!.forEach {
			if (it.locationInsidePoints(this.currentLocation!!)) {
				return MapMarker(it.name, SkiingActivity(this.currentLocation!!), R.drawable.ic_moderate,
						BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE), Color.BLUE)
			}
		}

		MtSpokaneMapItems.difficultRuns!!.forEach {
			if (it.locationInsidePoints(this.currentLocation!!)) {
				return MapMarker(it.name, SkiingActivity(this.currentLocation!!), R.drawable.ic_difficult,
						/*this.bitmapDescriptorFromVector(context, R.drawable.ic_black_marker)*/
						BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE),
						Color.BLACK)
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
		//val minChairliftSpeed = 150.0F * 0.00508F

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			if (this.currentLocation!!.speedAccuracyMetersPerSecond > 0.0F &&
				this.previousLocation!!.speedAccuracyMetersPerSecond > 0.0F) {
				if (this.currentLocation!!.speed + this.currentLocation!!.speedAccuracyMetersPerSecond <= maxChairliftSpeed) {
					return 2u
				}
			}
		}

		return if (this.currentLocation!!.speed <= maxChairliftSpeed) {
			1u
		} else {
			0u
		}

	}

	interface VisibleLocationUpdate {
		fun updateLocation(locationString: String)
	}
}