package com.mtspokane.skiapp.mapItem

import android.graphics.Color
import android.util.Log
import androidx.annotation.DrawableRes
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.mtspokane.skiapp.R
import com.mtspokane.skiapp.activities.ActivitySummaryLocations
import com.mtspokane.skiapp.activities.Locations
import com.mtspokane.skiapp.databases.SkiingActivity

data class MapMarker(val name: String, val skiingActivity: SkiingActivity, @DrawableRes val icon: Int,
                     val markerColor: BitmapDescriptor, val circleColor: Int) {

	// TODO Add equals operator

	companion object {

		const val UNKNOWN_LOCATION = "Unknown Location"

		fun loadFromSkiingActivityArray(array: Array<SkiingActivity>, startingChairliftTerminal: List<MapItem>,
										endingChairliftBounds: List<MapItem>, easyRunsBounds: List<MapItem>,
										moderateRunsBounds: List<MapItem>, difficultRunsBounds: List<MapItem>,
										otherBounds: List<MapItem>): Array<MapMarker> {

			return Array(array.size) {
				getMapMarker(array[it], startingChairliftTerminal, endingChairliftBounds, easyRunsBounds,
					moderateRunsBounds, difficultRunsBounds, otherBounds)
			}
		}

		private fun getMapMarker(skiingActivity: SkiingActivity, startingChairliftTerminal: List<MapItem>,
								 endingChairliftBounds: List<MapItem>, easyRunsBounds: List<MapItem>,
								 moderateRunsBounds: List<MapItem>, difficultRunsBounds: List<MapItem>,
								 otherBounds: List<MapItem>): MapMarker {

			ActivitySummaryLocations.updateLocations(skiingActivity)

			var marker: MapMarker? = ActivitySummaryLocations.checkIfIOnChairlift(startingChairliftTerminal, endingChairliftBounds)
			if (marker != null) {
				return marker
			}

			marker = ActivitySummaryLocations.checkIfOnOther(otherBounds)
			if (marker != null) {
				return marker
			}

			marker = ActivitySummaryLocations.checkIfOnRun(easyRunsBounds, moderateRunsBounds, difficultRunsBounds)
			if (marker != null) {
				return marker
			}

			Log.w("getMapMarker", "Unable to determine location")
			return MapMarker(UNKNOWN_LOCATION, ActivitySummaryLocations.currentLocation!!, R.drawable.ic_missing,
					BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA),
					Color.MAGENTA)
		}
	}
}