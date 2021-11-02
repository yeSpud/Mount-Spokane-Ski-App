package com.mtspokane.skiapp

import android.content.res.Resources
import android.location.Location
import android.location.LocationListener
import android.os.Bundle
import com.mapbox.geojson.Point
import com.mapbox.maps.MapView
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.plugin.annotation.AnnotationPlugin
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager

class SkierLocation(private val map: MapView, private val resources: Resources) : LocationListener {

	// Create an instance of the Annotation API and get the PointAnnotationManager.
	private val pointAnnotationManager = this.map.annotations.createPointAnnotationManager(this.map)

	override fun onLocationChanged(location: Location) {

		val pointAnnotationOptions: PointAnnotationOptions = PointAnnotationOptions()
			.withPoint(Point.fromLngLat(location.latitude, location.longitude))
			.withIconImage(null) // FIXME

		// Add the resulting pointAnnotation to the map.
		this.pointAnnotationManager.create(pointAnnotationOptions)

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

	override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
		// ignore
	}
}