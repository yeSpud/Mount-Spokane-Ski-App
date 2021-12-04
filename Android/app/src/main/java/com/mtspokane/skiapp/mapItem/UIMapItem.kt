package com.mtspokane.skiapp.mapItem

import android.location.Location
import androidx.annotation.DrawableRes
import androidx.annotation.MainThread
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Polygon
import com.google.maps.android.ktx.utils.contains

open class UIMapItem(name: String, initialPolygon: Polygon? = null, @DrawableRes icon: Int? = null) : MapItem(name, icon) {

	private var polygons: Array<Polygon> = emptyArray()

	@MainThread
	fun addAdditionalPolygon(polygon: Polygon) {

		val array: Array<Polygon> = Array(this.polygons.size + 1) {
			if (it == this.polygons.size) {
				polygon
			} else {
				this.polygons[it]
			}
		}

		this.polygons = array
		this.points = Array(this.polygons.size) { numberOfPolygons ->

			// This needs to be run on the main thread.
			val polygonPoints = this.polygons[numberOfPolygons].points

			Array(polygonPoints.size) { numberOfPoints ->
				val tempLatLng = polygonPoints[numberOfPoints]
				Pair(tempLatLng.latitude, tempLatLng.longitude)
			}
		}
	}

	@MainThread
	fun locationInsidePolygons(location: Location): Boolean {
		this.polygons.forEach {
			if (it.contains(LatLng(location.latitude, location.longitude))) {
				return true
			}
		}
		return false
	}

	init {
		if (initialPolygon != null) {
			this.addAdditionalPolygon(initialPolygon)
		}
	}
}