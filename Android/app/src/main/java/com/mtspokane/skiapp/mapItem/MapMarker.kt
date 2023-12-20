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
                     val markerColor: BitmapDescriptor, val circleColor: Int, val debugAltitude: UShort,
                     val debugSpeed: UShort, val debugVertical: Locations.VerticalDirection,
					 val debugChairlift: Boolean) {

	// TODO Add equals operator

	companion object {

		const val UNKNOWN_LOCATION = "Unknown Location"

		fun loadFromSkiingActivityArray(array: Array<SkiingActivity>): Array<MapMarker> {

			return Array(array.size) {
				getMapMarker(array[it])
			}
		}

		private fun getMapMarker(skiingActivity: SkiingActivity): MapMarker {

			ActivitySummaryLocations.updateLocations(skiingActivity)

			var marker: MapMarker? = ActivitySummaryLocations.checkIfIOnChairlift()
			if (marker != null) {
				return marker
			}

			marker = ActivitySummaryLocations.checkIfAtChairliftTerminals()
			if (marker != null) {
				return marker
			}

			marker = ActivitySummaryLocations.checkIfOnOther()
			if (marker != null) {
				return marker
			}

			marker = ActivitySummaryLocations.checkIfOnRun()
			if (marker != null) {
				return marker
			}

			Log.w("getMapMarker", "Unable to determine location")
			return MapMarker(UNKNOWN_LOCATION, ActivitySummaryLocations.currentLocation!!, R.drawable.ic_missing,
					BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA),
					Color.MAGENTA, 0u, 0u, Locations.VerticalDirection.UNKNOWN,
				false)
		}
	}
}