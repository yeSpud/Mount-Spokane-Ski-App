package com.mtspokane.skiapp.maphandlers

import androidx.annotation.UiThread
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.Circle
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.maps.android.ktx.addCircle
import com.google.maps.android.ktx.addMarker
import com.mtspokane.skiapp.mapItem.MapMarker

@UiThread
class ActivitySummaryLocationMarkers(map: GoogleMap, mapMarker: MapMarker) {

	var marker: Marker? = null
		private set

	var circle: Circle? = null
		private set

	fun destroy() {

		if (marker != null) {
			marker!!.remove()
			marker = null
		}

		if (circle != null) {
			circle!!.remove()
			circle = null
		}
	}

	init {

		val location = LatLng(mapMarker.skiingActivity.latitude, mapMarker.skiingActivity.longitude)

		circle = map.addCircle {
			center(location)
			strokeColor(mapMarker.circleColor)
			fillColor(mapMarker.circleColor)
			clickable(true)
			radius(3.0)
			zIndex(50.0F)
			visible(true)
		}

		marker = map.addMarker {
			position(location)
			icon(mapMarker.markerColor)
			title(mapMarker.name)
			zIndex(99.0F)
			visible(false)
		}

		marker!!.tag = mapMarker

		circle!!.tag = this
	}
}