package com.mtspokane.skiapp

import android.content.res.Resources
import android.location.Location
import android.location.LocationListener
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions

class SkierLocation(private val map: GoogleMap, private val resources: Resources) : LocationListener {

	private var locationMarker: Marker? = null

	override fun onLocationChanged(location: Location) {

		// If the marker hasn't been added to the map create a new one.
		if (this.locationMarker == null) {
			this.locationMarker = this.map.addMarker(MarkerOptions()
				.position(LatLng(location.latitude, location.longitude))
				.title(this.resources.getString(R.string.your_location))
				.snippet(this.resources.getString(R.string.more_info)))
		} else {

			// Otherwise just update the LatLng location.
			this.locationMarker!!.position = LatLng(location.latitude, location.longitude)
		}

		// Set the marker tag to the location object.
		this.locationMarker!!.tag = location
	}
}