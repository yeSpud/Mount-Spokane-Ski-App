package com.mtspokane.skiapp.mapItem

import android.graphics.Color
import android.util.Log
import androidx.annotation.DrawableRes
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.mtspokane.skiapp.R
import com.mtspokane.skiapp.activities.activitysummary.ActivitySummaryLocations
import com.mtspokane.skiapp.databases.SkiingActivity

data class MapMarker(val name: String, val skiingActivity: SkiingActivity, @DrawableRes val icon: Int,
                     val markerColor: BitmapDescriptor, val circleColor: Int) {

	// TODO Add equals operator

	companion object {

		const val UNKNOWN_LOCATION = "Unknown Location"

		/**
		 * TODO Documentation
		 */
		fun loadFromSkiingActivityArray(array: Array<SkiingActivity>): Array<MapMarker> {

			return Array(array.size) {
				getMapMarker(array[it])
			}
		}

		/**
		 * TODO Documentation
		 */
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
					Color.MAGENTA)
		}
	}
}