package com.mtspokane.skiapp.activities

import android.graphics.Color
import android.location.Location
import android.os.Build
import android.util.Log
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.mtspokane.skiapp.R
import com.mtspokane.skiapp.databases.SkiingActivity
import com.mtspokane.skiapp.mapItem.MapMarker
import com.mtspokane.skiapp.maphandlers.MtSpokaneMapBounds

abstract class Locations<T> {

	var previousLocation: T? = null
		internal set

	var currentLocation: T? = null
		internal set

	var altitudeConfidence: UShort = 0u
		internal set

	var speedConfidence: UShort = 0u
		internal set

	abstract fun updateLocations(newVariable: T)

	abstract fun getVerticalDirection(): VerticalDirection

	abstract fun checkIfOnOther(): MapMarker?

	fun checkIfIOnChairlift(): MapMarker? {
		if (ActivitySummaryLocations.currentLocation == null) {
			Log.w("checkIfIOnChairlift", "Chairlifts have not been set up")
			return null
		}

		val vDirection: VerticalDirection = ActivitySummaryLocations.getVerticalDirection()
		if (vDirection == VerticalDirection.UP || vDirection == VerticalDirection.UP_CERTAIN) {

			if (ActivitySummaryLocations.altitudeConfidence >= 1u && ActivitySummaryLocations.speedConfidence >= 0u) {
				for (it in MtSpokaneMapBounds.chairliftsBounds) {
					if (it.locationInsidePoints(ActivitySummaryLocations.currentLocation!!)) {
						return MapMarker(it.name, ActivitySummaryLocations.currentLocation!!, R.drawable.ic_chairlift,
								BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED), Color.RED,
								ActivitySummaryLocations.altitudeConfidence, ActivitySummaryLocations.speedConfidence, ActivitySummaryLocations.getVerticalDirection())
					}
				}
			}

			if (ActivitySummaryLocations.speedConfidence >= 1u && ActivitySummaryLocations.altitudeConfidence >= 0u) {
				for (it in MtSpokaneMapBounds.chairliftsBounds) {
					if (it.locationInsidePoints(ActivitySummaryLocations.currentLocation!!)) {
						return MapMarker(it.name, ActivitySummaryLocations.currentLocation!!, R.drawable.ic_chairlift,
								BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED), Color.RED,
								ActivitySummaryLocations.altitudeConfidence, ActivitySummaryLocations.speedConfidence, ActivitySummaryLocations.getVerticalDirection())
					}
				}
			}
		}

		/*
		if (ActivitySummaryLocations.altitudeConfidence >= 2u && ActivitySummaryLocations.speedConfidence >= 1u) {
			for (it in MtSpokaneMapBounds.chairliftsBounds) {
				if (it.locationInsidePoints(ActivitySummaryLocations.currentLocation!!)) {
					return MapMarker(it.name, ActivitySummaryLocations.currentLocation!!, R.drawable.ic_chairlift,
							BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED), Color.RED,
							ActivitySummaryLocations.altitudeConfidence, ActivitySummaryLocations.speedConfidence,
							getVerticalDirection())
				}
			}
		}*/

		/*
		if (ActivitySummaryLocations.speedConfidence >= 2u && ActivitySummaryLocations.altitudeConfidence >= 1u) {
			for (it in MtSpokaneMapBounds.chairliftsBounds) {
				if (it.locationInsidePoints(ActivitySummaryLocations.currentLocation!!)) {
					return MapMarker(it.name, ActivitySummaryLocations.currentLocation!!, R.drawable.ic_chairlift,
							BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED), Color.RED,
							ActivitySummaryLocations.altitudeConfidence, ActivitySummaryLocations.speedConfidence,
							ActivitySummaryLocations.getVerticalDirection())
				}
			}
		}*/

		return null
	}

	abstract fun checkIfAtChairliftTerminals(): MapMarker?

	abstract fun checkIfOnRun(): MapMarker?

	abstract fun getSpeedConfidenceValue(): UShort

	enum class VerticalDirection {
		UP_CERTAIN, UP, FLAT, DOWN, DOWN_CERTAIN, UNKNOWN
	}
}

object ActivitySummaryLocations: Locations<SkiingActivity>() {

	override fun updateLocations(newVariable: SkiingActivity) {
		previousLocation = currentLocation

		altitudeConfidence = when (getVerticalDirection()) {
			VerticalDirection.UP_CERTAIN -> 3u
			VerticalDirection.UP -> 2u
			VerticalDirection.FLAT -> 1u
			else -> 0u
		}

		speedConfidence = getSpeedConfidenceValue()

		currentLocation = newVariable
	}

	override fun getVerticalDirection(): VerticalDirection {

		if (previousLocation == null || currentLocation == null) {
			return VerticalDirection.UNKNOWN
		}

		if (currentLocation!!.altitude == 0.0 || previousLocation!!.altitude == 0.0) {
			return VerticalDirection.UNKNOWN
		}


		if (currentLocation!!.altitudeAccuracy != null && previousLocation!!
						.altitudeAccuracy != null) {

			if ((currentLocation!!.altitude - currentLocation!!.altitudeAccuracy!!)
					> (previousLocation!!.altitude + previousLocation!!.altitudeAccuracy!!)) {
				return VerticalDirection.UP_CERTAIN
			} else if ((currentLocation!!.altitude + currentLocation!!.altitudeAccuracy!!)
					< (previousLocation!!.altitude - previousLocation!!.altitudeAccuracy!!)) {
				return VerticalDirection.DOWN_CERTAIN
			}
		}

		return when {
			currentLocation!!.altitude > previousLocation!!.altitude -> VerticalDirection.UP
			currentLocation!!.altitude < previousLocation!!.altitude -> VerticalDirection.DOWN
			else -> VerticalDirection.FLAT
		}
	}

	override fun checkIfOnOther(): MapMarker? {

		if (currentLocation == null) {
			Log.w("checkIfOnOther", "Other map items have not been set up")
			return null
		}

		for (other in MtSpokaneMapBounds.other) {
			if (other.locationInsidePoints(currentLocation!!)) {

				return when (other.icon) {
					R.drawable.ic_parking -> MapMarker(other.name, currentLocation!!, other.icon,
							BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA),
							Color.GRAY, altitudeConfidence, speedConfidence, getVerticalDirection())
					R.drawable.ic_ski_patrol_icon -> MapMarker(other.name, currentLocation!!, other.icon,
							BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA),
							Color.WHITE, altitudeConfidence, speedConfidence, getVerticalDirection())
					else -> MapMarker(other.name, currentLocation!!, other.icon ?: R.drawable.ic_missing,
							BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA),
							Color.MAGENTA, altitudeConfidence, speedConfidence, getVerticalDirection())
				}
			}
		}

		return null
	}

	override fun checkIfAtChairliftTerminals(): MapMarker? {

		if (currentLocation == null) {
			Log.w("checkChairliftTerminals", "Chairlift terminals have not been set up")
			return null
		}

		for (it in MtSpokaneMapBounds.chairliftTerminals) {
			if (it.locationInsidePoints(currentLocation!!)) {
				return MapMarker(it.name, currentLocation!!, R.drawable.ic_chairlift,
						BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED), Color.RED,
						altitudeConfidence, speedConfidence, getVerticalDirection())
			}
		}

		return null
	}

	override fun checkIfOnRun(): MapMarker? {

		if (currentLocation == null) {
			Log.w("checkIfOnRun", "Ski runs have not been set up")
			return null
		}

		for (it in MtSpokaneMapBounds.easyRunsBounds) {
			if (it.locationInsidePoints(currentLocation!!)) {
				return MapMarker(it.name, currentLocation!!, R.drawable.ic_easy,
						BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN),
						Color.GREEN, altitudeConfidence, speedConfidence, getVerticalDirection())
			}
		}

		for (it in MtSpokaneMapBounds.moderateRunsBounds) {
			if (it.locationInsidePoints(currentLocation!!)) {
				return MapMarker(it.name, currentLocation!!, R.drawable.ic_moderate,
						BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE), Color.BLUE,
						altitudeConfidence, speedConfidence, getVerticalDirection())
			}
		}

		for (it in MtSpokaneMapBounds.difficultRunsBounds) {
			if (it.locationInsidePoints(currentLocation!!)) {
				return MapMarker(it.name, currentLocation!!, R.drawable.ic_difficult,
						/*bitmapDescriptorFromVector(context, R.drawable.ic_black_marker)*/
						BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE),
						Color.BLACK, altitudeConfidence, speedConfidence, getVerticalDirection())
			}
		}

		return null
	}

	override fun getSpeedConfidenceValue(): UShort {

		if (currentLocation == null || previousLocation == null) {
			return 0u
		}

		if (currentLocation!!.speed == 0.0F || previousLocation!!.speed == 0.0F) {
			return 0u
		}

		// 550 feet per minute to meters per second.
		val maxChairliftSpeed = 550.0F * 0.00508F

		// 150 feet per minute to meters per second.
		//val minChairliftSpeed = 150.0F * 0.00508F

		if (currentLocation!!.speedAccuracy != null && previousLocation!!.speedAccuracy != null) {
			if (currentLocation!!.speedAccuracy!! > 0.0F && previousLocation!!.speedAccuracy!! > 0.0F) {
				if (currentLocation!!.speed + currentLocation!!.speedAccuracy!! <= maxChairliftSpeed) {
					Log.v("getSpeedConfidenceValue", "High confidence")
					return 2u
				}
			}
		}

		return if (currentLocation!!.speed <= maxChairliftSpeed) {
			1u
		} else {
			0u
		}
	}
}

object InAppLocations: Locations<Location>() {

	var visibleLocationUpdates: MutableList<VisibleLocationUpdate> = mutableListOf()

	override fun updateLocations(newVariable: Location) {
		previousLocation = currentLocation

		altitudeConfidence = when (getVerticalDirection()) {
			VerticalDirection.UP_CERTAIN -> 3u
			VerticalDirection.UP -> 2u
			VerticalDirection.FLAT -> 1u
			else -> 0u
		}

		speedConfidence = getSpeedConfidenceValue()

		currentLocation = newVariable
	}

	override fun getVerticalDirection(): VerticalDirection {

		if (previousLocation == null || currentLocation == null) {
			return VerticalDirection.UNKNOWN
		}

		if (currentLocation!!.altitude == 0.0 || previousLocation!!.altitude == 0.0) {
			return VerticalDirection.UNKNOWN
		}


		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

			if ((currentLocation!!.altitude - currentLocation!!.verticalAccuracyMeters) >
					(previousLocation!!.altitude + previousLocation!!.verticalAccuracyMeters)) {
				return VerticalDirection.UP_CERTAIN
			} else if ((currentLocation!!.altitude + currentLocation!!.verticalAccuracyMeters) <
					(previousLocation!!.altitude - previousLocation!!.verticalAccuracyMeters)) {
				return VerticalDirection.DOWN_CERTAIN
			}
		}

		return when {
			currentLocation!!.altitude > previousLocation!!.altitude -> VerticalDirection.UP
			currentLocation!!.altitude < previousLocation!!.altitude -> VerticalDirection.DOWN
			else -> VerticalDirection.FLAT
		}
	}

	override fun checkIfOnOther(): MapMarker? {

		if (currentLocation == null) {
			Log.w("checkIfOnOther", "Other map item has not been set up")
			return null
		}

		for (it in MtSpokaneMapBounds.other) {
			if (it.locationInsidePoints(currentLocation!!)) {
				return when (it.icon) {
					R.drawable.ic_parking -> MapMarker(it.name, SkiingActivity(currentLocation!!),
							it.icon, BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA),
							Color.GRAY, ActivitySummaryLocations.altitudeConfidence, ActivitySummaryLocations.speedConfidence,
							ActivitySummaryLocations.getVerticalDirection())
					R.drawable.ic_ski_patrol_icon -> MapMarker(it.name, SkiingActivity(currentLocation!!),
							it.icon, BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA),
							Color.WHITE, ActivitySummaryLocations.altitudeConfidence, ActivitySummaryLocations.speedConfidence,
							ActivitySummaryLocations.getVerticalDirection())
					else -> MapMarker(it.name, SkiingActivity(currentLocation!!),
							it.icon ?: R.drawable.ic_missing, BitmapDescriptorFactory
							.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA), Color.MAGENTA,
							ActivitySummaryLocations.altitudeConfidence, ActivitySummaryLocations.speedConfidence,
							ActivitySummaryLocations.getVerticalDirection())
				}
			}
		}

		return null
	}

	override fun checkIfAtChairliftTerminals(): MapMarker? {

		if (currentLocation == null) {
			Log.w("checkChairliftTerminals", "Chairlift terminals have not been set up")
			return null
		}

		for (it in MtSpokaneMapBounds.chairliftTerminals) {
			if (it.locationInsidePoints(currentLocation!!)) {
				return MapMarker(it.name, SkiingActivity(currentLocation!!), R.drawable.ic_chairlift,
						BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED), Color.RED,
						ActivitySummaryLocations.altitudeConfidence, ActivitySummaryLocations.speedConfidence,
						ActivitySummaryLocations.getVerticalDirection())
			}
		}

		return null
	}

	override fun checkIfOnRun(): MapMarker? {

		if (currentLocation == null) {
			Log.w("checkIfOnRun", "Ski runs have not been set up")
			return null
		}

		for (it in MtSpokaneMapBounds.easyRunsBounds) {
			if (it.locationInsidePoints(currentLocation!!)) {
				return MapMarker(it.name, SkiingActivity(currentLocation!!), R.drawable.ic_easy,
						BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN),
						Color.GREEN, ActivitySummaryLocations.altitudeConfidence, ActivitySummaryLocations.speedConfidence,
						ActivitySummaryLocations.getVerticalDirection())
			}
		}

		for (it in MtSpokaneMapBounds.moderateRunsBounds) {
			if (it.locationInsidePoints(currentLocation!!)) {
				return MapMarker(it.name, SkiingActivity(currentLocation!!), R.drawable.ic_moderate,
						BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE), Color.BLUE,
						ActivitySummaryLocations.altitudeConfidence, ActivitySummaryLocations.speedConfidence,
						ActivitySummaryLocations.getVerticalDirection())
			}
		}

		for (it in MtSpokaneMapBounds.difficultRunsBounds) {
			if (it.locationInsidePoints(currentLocation!!)) {
				return MapMarker(it.name, SkiingActivity(currentLocation!!), R.drawable.ic_difficult,
						/*bitmapDescriptorFromVector(context, R.drawable.ic_black_marker)*/
						BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE),
						Color.BLACK, ActivitySummaryLocations.altitudeConfidence, ActivitySummaryLocations.speedConfidence,
						ActivitySummaryLocations.getVerticalDirection())
			}
		}

		return null
	}

	override fun getSpeedConfidenceValue(): UShort {

		if (currentLocation == null || previousLocation == null) {
			return 0u
		}

		if (currentLocation!!.speed == 0.0F || previousLocation!!.speed == 0.0F) {
			return 0u
		}

		// 550 feet per minute to meters per second.
		val maxChairliftSpeed = 550.0F * 0.00508F

		// 150 feet per minute to meters per second.
		//val minChairliftSpeed = 150.0F * 0.00508F

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			if (currentLocation!!.speedAccuracyMetersPerSecond > 0.0F &&
					previousLocation!!.speedAccuracyMetersPerSecond > 0.0F) {
				if (currentLocation!!.speed + currentLocation!!.speedAccuracyMetersPerSecond <= maxChairliftSpeed) {
					return 2u
				}
			}
		}

		return if (currentLocation!!.speed <= maxChairliftSpeed) {
			1u
		} else {
			0u
		}

	}

	interface VisibleLocationUpdate {
		fun updateLocation(locationString: String)
	}
}