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

	internal var isOnChairlift: String? = null

	// TODO Add helper function to convert from resource color to actual color

	abstract fun updateLocations(newVariable: T)

	abstract fun checkIfOnOther(): MapMarker?

	abstract fun isInStartingTerminal(): String?

	abstract fun isInEndingTerminal(): String?

	abstract fun checkIfIOnChairlift(): MapMarker?

	abstract fun checkIfOnRun(): MapMarker?
}

object ActivitySummaryLocations: Locations<SkiingActivity>() {

	override fun updateLocations(newVariable: SkiingActivity) {
		previousLocation = currentLocation
		currentLocation = newVariable
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
							Color.GRAY)
					R.drawable.ic_ski_patrol_icon -> MapMarker(other.name, currentLocation!!, other.icon,
							BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA),
							Color.WHITE)
					else -> MapMarker(other.name, currentLocation!!, other.icon ?: R.drawable.ic_missing,
							BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA),
							Color.MAGENTA)
				}
			}
		}

		return null
	}

	override fun isInStartingTerminal(): String? {
		for (it in MtSpokaneMapBounds.startingChairliftTerminals) {
			if (it.locationInsidePoints(currentLocation!!)) {
				return it.name
			}
		}

		return null
	}

	override fun isInEndingTerminal(): String? {
		for (it in MtSpokaneMapBounds.endingChairliftTerminals) {
			if (it.locationInsidePoints(currentLocation!!)) {
				return it.name
			}
		}

		return null
	}

	override fun checkIfIOnChairlift(): MapMarker? {

		if (currentLocation == null) {
			Log.w("checkIfIOnChairlift", "Chairlifts have not been set up")
			return null
		}

		val startingTerminal = isInStartingTerminal()
		if (startingTerminal != null) {
			isOnChairlift = startingTerminal
			return MapMarker(startingTerminal, currentLocation!!, R.drawable.ic_chairlift,
				BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED), Color.RED)
		}

		val endingTerminal = isInEndingTerminal()
		if (endingTerminal != null) {
			isOnChairlift = null
			return MapMarker(endingTerminal, currentLocation!!, R.drawable.ic_chairlift,
				BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED), Color.RED)
		}

		if (isOnChairlift != null) {
			return MapMarker(isOnChairlift!!, currentLocation!!, R.drawable.ic_chairlift,
				BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED), Color.RED)
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
						Color.GREEN)
			}
		}

		for (it in MtSpokaneMapBounds.moderateRunsBounds) {
			if (it.locationInsidePoints(currentLocation!!)) {
				return MapMarker(it.name, currentLocation!!, R.drawable.ic_moderate,
						BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE),
					Color.BLUE)
			}
		}

		for (it in MtSpokaneMapBounds.difficultRunsBounds) {
			if (it.locationInsidePoints(currentLocation!!)) {
				return MapMarker(it.name, currentLocation!!, R.drawable.ic_difficult,
						BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE),
						Color.BLACK)
			}
		}

		return null
	}
}

object InAppLocations: Locations<Location>() {

	var visibleLocationUpdates: MutableList<VisibleLocationUpdate> = mutableListOf()

	override fun updateLocations(newVariable: Location) {
		previousLocation = currentLocation
		currentLocation = newVariable
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
							Color.GRAY)
					R.drawable.ic_ski_patrol_icon -> MapMarker(it.name, SkiingActivity(currentLocation!!),
							it.icon, BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA),
							Color.WHITE)
					else -> MapMarker(it.name, SkiingActivity(currentLocation!!),
							it.icon ?: R.drawable.ic_missing, BitmapDescriptorFactory
							.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA), Color.MAGENTA)
				}
			}
		}

		return null
	}

	override fun isInStartingTerminal(): String? {
		for (it in MtSpokaneMapBounds.startingChairliftTerminals) {
			if (it.locationInsidePoints(currentLocation!!)) {
				return it.name
			}
		}

		return null
	}

	override fun isInEndingTerminal(): String? {
		for (it in MtSpokaneMapBounds.endingChairliftTerminals) {
			if (it.locationInsidePoints(currentLocation!!)) {
				return it.name
			}
		}

		return null
	}

	override fun checkIfIOnChairlift(): MapMarker? {

		if (currentLocation == null) {
			Log.w("checkIfIOnChairlift", "Chairlifts have not been set up")
			return null
		}

		val startingTerminal = isInStartingTerminal()
		if (startingTerminal != null) {
			isOnChairlift = startingTerminal
			return MapMarker(startingTerminal, SkiingActivity(currentLocation!!), R.drawable.ic_chairlift,
				BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED), Color.RED)
		}

		val endingTerminal = isInEndingTerminal()
		if (endingTerminal != null) {
			isOnChairlift = null
			return MapMarker(endingTerminal, SkiingActivity(currentLocation!!), R.drawable.ic_chairlift,
				BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED), Color.RED)
		}

		if (isOnChairlift != null) {
			return MapMarker(isOnChairlift!!, SkiingActivity(currentLocation!!), R.drawable.ic_chairlift,
				BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED), Color.RED)
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
						Color.GREEN)
			}
		}

		for (it in MtSpokaneMapBounds.moderateRunsBounds) {
			if (it.locationInsidePoints(currentLocation!!)) {
				return MapMarker(it.name, SkiingActivity(currentLocation!!), R.drawable.ic_moderate,
						BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE),
					Color.BLUE)
			}
		}

		for (it in MtSpokaneMapBounds.difficultRunsBounds) {
			if (it.locationInsidePoints(currentLocation!!)) {
				return MapMarker(it.name, SkiingActivity(currentLocation!!), R.drawable.ic_difficult,
						BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE),
						Color.BLACK)
			}
		}

		return null
	}

	interface VisibleLocationUpdate {
		fun updateLocation(locationString: String)
	}
}