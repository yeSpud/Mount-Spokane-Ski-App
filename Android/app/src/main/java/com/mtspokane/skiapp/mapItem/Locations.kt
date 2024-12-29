package com.mtspokane.skiapp.mapItem

import android.graphics.Color
import android.util.Log
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.mtspokane.skiapp.R
import com.mtspokane.skiapp.SkiingActivity

object Locations {

	var previousLocation: SkiingActivity? = null
		private set
	var currentLocation: SkiingActivity? = null
		private set

	private var isOnChairlift: String? = null

	// TODO Add helper function to convert from resource color to actual color

	fun updateLocations(newLocation: SkiingActivity) {
		previousLocation = currentLocation
		currentLocation = newLocation
	}

	fun checkIfOnOther(otherBounds: List<MapItem>): MapMarker? {
		val location = currentLocation
		if (location == null) {
			Log.w("checkIfOnOther", "Other map items have not been set up")
			return null
		}

		for (other in otherBounds) {
			if (other.locationInsidePoints(location)) {

				return when (other.icon) {
					R.drawable.ic_parking -> MapMarker(other.name, location, other.icon,
						BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA),
						Color.GRAY)
					R.drawable.ic_ski_patrol_icon -> MapMarker(other.name, location, other.icon,
						BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA),
						Color.WHITE)
					R.drawable.ic_ski_school -> MapMarker(other.name, location, other.icon,
						BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA),
						Color.GRAY)
					else -> MapMarker(other.name, location, other.icon ?: R.drawable.ic_missing,
						BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA),
						Color.MAGENTA)
				}
			}
		}

		return null
	}

	private fun isInStartingTerminal(startingChairliftBounds: List<MapItem>): String? {
		for (startingChairlift in startingChairliftBounds) {
			if (startingChairlift.locationInsidePoints(currentLocation!!)) {
				return startingChairlift.name
			}
		}

		return null
	}

	private fun isInEndingTerminal(endingChairliftBounds: List<MapItem>): String? {
		for (endingChairlift in endingChairliftBounds) {
			if (endingChairlift.locationInsidePoints(currentLocation!!)) {
				return endingChairlift.name
			}
		}

		return null
	}

	fun checkIfIOnChairlift(startingChairliftBounds: List<MapItem>,
							endingChairliftBounds: List<MapItem>): MapMarker? {
		val location = currentLocation
		if (location == null) {
			Log.w("checkIfIOnChairlift", "Chairlifts have not been set up")
			return null
		}

		val startingTerminal = isInStartingTerminal(startingChairliftBounds)
		if (startingTerminal != null) {
			isOnChairlift = startingTerminal
			return MapMarker(startingTerminal, location, R.drawable.ic_chairlift,
				BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED), Color.RED)
		}

		val endingTerminal = isInEndingTerminal(endingChairliftBounds)
		if (endingTerminal != null) {
			isOnChairlift = null
			return MapMarker(endingTerminal, location, R.drawable.ic_chairlift,
				BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED), Color.RED)
		}

		if (isOnChairlift != null) {
			return MapMarker(isOnChairlift!!, location, R.drawable.ic_chairlift,
				BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED), Color.RED)
		}

		return null
	}

	fun checkIfOnRun(easyRunsBounds: List<MapItem>, moderateRunsBounds: List<MapItem>,
							  difficultRunsBounds: List<MapItem>): MapMarker? {
		val location = currentLocation
		if (location == null) {
			Log.w("checkIfOnRun", "Ski runs have not been set up")
			return null
		}

		for (easyRunBounds in easyRunsBounds) {
			if (easyRunBounds.locationInsidePoints(location)) {
				return MapMarker(easyRunBounds.name, location, R.drawable.ic_easy,
					BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN),
					Color.GREEN)
			}
		}

		for (moderateRunBounds in moderateRunsBounds) {
			if (moderateRunBounds.locationInsidePoints(location)) {
				return MapMarker(moderateRunBounds.name, location, R.drawable.ic_moderate,
					BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE),
					Color.BLUE)
			}
		}

		for (difficultRunBounds in difficultRunsBounds) {
			if (difficultRunBounds.locationInsidePoints(location)) {
				return MapMarker(difficultRunBounds.name, location, R.drawable.ic_difficult,
					BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE),
					Color.BLACK)
			}
		}

		return null
	}
}