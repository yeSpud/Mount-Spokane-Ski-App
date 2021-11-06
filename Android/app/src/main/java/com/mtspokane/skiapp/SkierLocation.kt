package com.mtspokane.skiapp

import android.location.Location
import android.location.LocationListener
import android.os.Bundle
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions

class SkierLocation(private val mapHandler: MapHandler) : LocationListener {

	private var locationMarker: Marker? = null

	override fun onLocationChanged(location: Location) {

		// If the marker hasn't been added to the map create a new one.
		if (this.locationMarker == null) {
			this.locationMarker = this.mapHandler.map.addMarker(MarkerOptions()
				.position(LatLng(location.latitude, location.longitude))
				.title(this.mapHandler.activity.resources.getString(R.string.your_location)))
				//.snippet(this.resources.getString(R.string.more_info)))
		} else {

			// Otherwise just update the LatLng location.
			this.locationMarker!!.position = LatLng(location.latitude, location.longitude)
		}

		// Set the marker tag to the location object.
		this.locationMarker!!.tag = location
	}

	override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
		// ignore
	}
}