package com.mtspokane.skiapp.activities.activitysummary

import android.graphics.Color
import android.util.Log
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.mtspokane.skiapp.R
import com.mtspokane.skiapp.mapItem.MtSpokaneMapBounds
import com.mtspokane.skiapp.activities.Locations
import com.mtspokane.skiapp.databases.SkiingActivity
import com.mtspokane.skiapp.mapItem.MapMarker

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

	override fun checkIfOnOther(): MapMarker? {

		if (this.currentLocation == null) {
			Log.w("checkIfOnOther", "Other map items have not been set up")
			return null
		}

		MtSpokaneMapBounds.other.forEach {
			if (it.locationInsidePoints(this.currentLocation!!)) {

				return when (it.icon) {
					R.drawable.ic_parking -> MapMarker(it.name, this.currentLocation!!, it.icon,
							BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA),
							Color.GRAY)
					R.drawable.ic_ski_patrol_icon -> MapMarker(it.name, this.currentLocation!!, it.icon,
							BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA),
							Color.WHITE)
					else -> MapMarker(it.name, this.currentLocation!!, it.icon ?: R.drawable.ic_missing,
							BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA),
							Color.MAGENTA)
				}
			}
		}

		return null
	}

	override fun checkIfIOnChairlift(): MapMarker? {

		if (this.currentLocation == null) {
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

		MtSpokaneMapBounds.chairliftsBounds.forEach {
			if (it.locationInsidePoints(this.currentLocation!!)) {
				return MapMarker(it.name, this.currentLocation!!, R.drawable.ic_chairlift,
						BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED), Color.RED)
			}
		}

		return null
	}

	override fun checkIfAtChairliftTerminals(): MapMarker? {

		if (this.currentLocation == null) {
			Log.w("checkChairliftTerminals", "Chairlift terminals have not been set up")
			return null
		}

		MtSpokaneMapBounds.chairliftTerminals.forEach {
			if (it.locationInsidePoints(this.currentLocation!!)) {
				return MapMarker(it.name, this.currentLocation!!, R.drawable.ic_chairlift,
						BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED), Color.RED)
			}
		}

		return null
	}

	override fun checkIfOnRun(): MapMarker? {

		if (this.currentLocation == null) {
			Log.w("checkIfOnRun", "Ski runs have not been set up")
			return null
		}

		MtSpokaneMapBounds.easyRunsBounds.forEach {
			if (it.locationInsidePoints(this.currentLocation!!)) {
				return MapMarker(it.name, this.currentLocation!!, R.drawable.ic_easy,
						BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN),
						Color.GREEN)
			}
		}

		MtSpokaneMapBounds.moderateRunsBounds.forEach {
			if (it.locationInsidePoints(this.currentLocation!!)) {
				return MapMarker(it.name, this.currentLocation!!, R.drawable.ic_moderate,
				BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE), Color.BLUE)
			}
		}

		MtSpokaneMapBounds.difficultRunsBounds.forEach {
			if (it.locationInsidePoints(this.currentLocation!!)) {
				return MapMarker(it.name, this.currentLocation!!, R.drawable.ic_difficult,
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

		if (this.currentLocation!!.speedAccuracy != null && this.previousLocation!!.speedAccuracy != null) {
			if (this.currentLocation!!.speedAccuracy!! > 0.0F && this.previousLocation!!.speedAccuracy!! > 0.0F) {
				if (this.currentLocation!!.speed + this.currentLocation!!.speedAccuracy!! <= maxChairliftSpeed) {
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
}