package com.mtspokane.skiapp.activities

import android.graphics.Color
import android.location.Location
import android.util.Log
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.mtspokane.skiapp.R
import com.mtspokane.skiapp.databases.SkiingActivity
import com.mtspokane.skiapp.mapItem.MapItem
import com.mtspokane.skiapp.mapItem.MapMarker

abstract class Locations<T> {

	var previousLocation: T? = null
		internal set

	var currentLocation: T? = null
		internal set

	internal var isOnChairlift: String? = null

	// TODO Add helper function to convert from resource color to actual color

	abstract fun updateLocations(newVariable: T)

	abstract fun checkIfOnOther(otherBounds: List<MapItem>): MapMarker?

	abstract fun isInStartingTerminal(startingChairliftBounds: List<MapItem>): String?

	abstract fun isInEndingTerminal(endingChairliftBounds: List<MapItem>): String?

	abstract fun checkIfIOnChairlift(startingChairliftBounds: List<MapItem>,
									 endingChairliftBounds: List<MapItem>): MapMarker?

	abstract fun checkIfOnRun(easyRunsBounds: List<MapItem>, moderateRunsBounds: List<MapItem>,
							  difficultRunsBounds: List<MapItem>): MapMarker?
}

object ActivitySummaryLocations: Locations<SkiingActivity>() {

	override fun updateLocations(newVariable: SkiingActivity) {
		previousLocation = currentLocation
		currentLocation = newVariable
	}

	override fun checkIfOnOther(otherBounds: List<MapItem>): MapMarker? {

		if (currentLocation == null) {
			Log.w("checkIfOnOther", "Other map items have not been set up")
			return null
		}

		for (other in otherBounds) {
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

	override fun isInStartingTerminal(startingChairliftBounds: List<MapItem>): String? {
		for (startingChairlift in startingChairliftBounds) {
			if (startingChairlift.locationInsidePoints(currentLocation!!)) {
				return startingChairlift.name
			}
		}

		return null
	}

	override fun isInEndingTerminal(endingChairliftBounds: List<MapItem>): String? {
		for (endingChairlift in endingChairliftBounds) {
			if (endingChairlift.locationInsidePoints(currentLocation!!)) {
				return endingChairlift.name
			}
		}

		return null
	}

	override fun checkIfIOnChairlift(startingChairliftBounds: List<MapItem>,
									 endingChairliftBounds: List<MapItem>): MapMarker? {

		if (currentLocation == null) {
			Log.w("checkIfIOnChairlift", "Chairlifts have not been set up")
			return null
		}

		val startingTerminal = isInStartingTerminal(startingChairliftBounds)
		if (startingTerminal != null) {
			isOnChairlift = startingTerminal
			return MapMarker(startingTerminal, currentLocation!!, R.drawable.ic_chairlift,
				BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED), Color.RED)
		}

		val endingTerminal = isInEndingTerminal(endingChairliftBounds)
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

	override fun checkIfOnRun(easyRunsBounds: List<MapItem>, moderateRunsBounds: List<MapItem>,
		difficultRunsBounds: List<MapItem>): MapMarker? {

		if (currentLocation == null) {
			Log.w("checkIfOnRun", "Ski runs have not been set up")
			return null
		}

		for (easyRunBounds in easyRunsBounds) {
			if (easyRunBounds.locationInsidePoints(currentLocation!!)) {
				return MapMarker(easyRunBounds.name, currentLocation!!, R.drawable.ic_easy,
						BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN),
						Color.GREEN)
			}
		}

		for (moderateRunBounds in moderateRunsBounds) {
			if (moderateRunBounds.locationInsidePoints(currentLocation!!)) {
				return MapMarker(moderateRunBounds.name, currentLocation!!, R.drawable.ic_moderate,
						BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE),
					Color.BLUE)
			}
		}

		for (difficultRunBounds in difficultRunsBounds) {
			if (difficultRunBounds.locationInsidePoints(currentLocation!!)) {
				return MapMarker(difficultRunBounds.name, currentLocation!!, R.drawable.ic_difficult,
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

	override fun checkIfOnOther(otherBounds: List<MapItem>): MapMarker? {

		if (currentLocation == null) {
			Log.w("checkIfOnOther", "Other map item has not been set up")
			return null
		}

		for (otherBound in otherBounds) {
			if (otherBound.locationInsidePoints(currentLocation!!)) {
				return when (otherBound.icon) {
					R.drawable.ic_parking -> MapMarker(otherBound.name, SkiingActivity(currentLocation!!),
							otherBound.icon, BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA),
							Color.GRAY)
					R.drawable.ic_ski_patrol_icon -> MapMarker(otherBound.name, SkiingActivity(currentLocation!!),
							otherBound.icon, BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA),
							Color.WHITE)
					else -> MapMarker(otherBound.name, SkiingActivity(currentLocation!!),
							otherBound.icon ?: R.drawable.ic_missing, BitmapDescriptorFactory
							.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA), Color.MAGENTA)
				}
			}
		}

		return null
	}

	override fun isInStartingTerminal(startingChairliftBounds: List<MapItem>): String? {
		for (startingChairlift in startingChairliftBounds) {
			if (startingChairlift.locationInsidePoints(currentLocation!!)) {
				return startingChairlift.name
			}
		}

		return null
	}

	override fun isInEndingTerminal(endingChairliftBounds: List<MapItem>): String? {
		for (endingChairlift in endingChairliftBounds) {
			if (endingChairlift.locationInsidePoints(currentLocation!!)) {
				return endingChairlift.name
			}
		}

		return null
	}

	override fun checkIfIOnChairlift(startingChairliftBounds: List<MapItem>,
		endingChairliftBounds: List<MapItem>): MapMarker? {

		if (currentLocation == null) {
			Log.w("checkIfIOnChairlift", "Chairlifts have not been set up")
			return null
		}

		val startingTerminal = isInStartingTerminal(startingChairliftBounds)
		if (startingTerminal != null) {
			isOnChairlift = startingTerminal
			return MapMarker(startingTerminal, SkiingActivity(currentLocation!!), R.drawable.ic_chairlift,
				BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED), Color.RED)
		}

		val endingTerminal = isInEndingTerminal(endingChairliftBounds)
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

	override fun checkIfOnRun(easyRunsBounds: List<MapItem>, moderateRunsBounds: List<MapItem>,
		difficultRunsBounds: List<MapItem>): MapMarker? {

		if (currentLocation == null) {
			Log.w("checkIfOnRun", "Ski runs have not been set up")
			return null
		}

		for (endingRunBounds in easyRunsBounds) {
			if (endingRunBounds.locationInsidePoints(currentLocation!!)) {
				return MapMarker(endingRunBounds.name, SkiingActivity(currentLocation!!), R.drawable.ic_easy,
						BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN),
						Color.GREEN)
			}
		}

		for (moderateRunBounds in moderateRunsBounds) {
			if (moderateRunBounds.locationInsidePoints(currentLocation!!)) {
				return MapMarker(moderateRunBounds.name, SkiingActivity(currentLocation!!), R.drawable.ic_moderate,
						BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE),
					Color.BLUE)
			}
		}

		for (difficultRunBounds in difficultRunsBounds) {
			if (difficultRunBounds.locationInsidePoints(currentLocation!!)) {
				return MapMarker(difficultRunBounds.name, SkiingActivity(currentLocation!!), R.drawable.ic_difficult,
						BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE),
						Color.BLACK)
			}
		}

		return null
	}

	@Deprecated("Will be removed(?)")
	// todo Remove me?
	interface VisibleLocationUpdate {
		fun updateLocation(locationString: String)
	}
}