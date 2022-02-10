package com.mtspokane.skiapp.maphandlers.activitysummarymap

import android.util.Log
import android.view.View
import android.widget.TextView
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.Marker
import com.mtspokane.skiapp.R
import com.mtspokane.skiapp.activities.activitysummary.ActivitySummary
import com.mtspokane.skiapp.mapItem.MapMarker
import kotlin.math.roundToInt

class CustomInfoWindow(private val activity: ActivitySummary) : GoogleMap.InfoWindowAdapter {

	override fun getInfoContents(marker: Marker): View? {
		Log.v("CustomInfoWindow", "getInfoContents called")

		if (marker.tag is Pair<*, *>) {

			val markerInfo: Pair<MapMarker, String?> = marker.tag as Pair<MapMarker, String?>

			val markerView: View = this.activity.layoutInflater.inflate(R.layout.info_window, null)

			val name: TextView = markerView.findViewById(R.id.marker_name)
			name.text = markerInfo.first.name

			val altitude: TextView = markerView.findViewById(R.id.marker_altitude)
			altitude.text = this.activity.getString(R.string.marker_altitude, markerInfo.first
				.skiingActivity.altitude.roundToInt()) // TODO Convert to feet & catch round NaN

			val speed: TextView = markerView.findViewById(R.id.marker_speed)

			// Convert from meters per second to miles per hour.
			val speedConversion = 0.44704f

			speed.text = this.activity.getString(R.string.marker_speed, (markerInfo.first
				.skiingActivity.speed / speedConversion).roundToInt()) // TODO Catch round NaN

			if (markerInfo.second != null) {

				val debug: TextView = markerView.findViewById(R.id.marker_debug)
				debug.text = markerInfo.second
			}

			return markerView

		} else {

			return null
		}
	}

	override fun getInfoWindow(marker: Marker): View? {
		Log.v("CustomInfoWindow", "getInfoWindow called")

		return null
	}
}