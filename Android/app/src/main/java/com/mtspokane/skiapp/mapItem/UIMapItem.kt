package com.mtspokane.skiapp.mapItem

import androidx.annotation.DrawableRes
import androidx.annotation.MainThread
import com.google.android.gms.maps.model.Polygon

open class UIMapItem(name: String, initialPolygon: Polygon? = null, @DrawableRes icon: Int? = null) :
	MapItem(name, icon) {

	private var polygons: Array<Polygon> = emptyArray()

	open fun destroyUIItems() {
		this.polygons = emptyArray()
	}

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

	init {
		if (initialPolygon != null) {
			this.addAdditionalPolygon(initialPolygon)
		}
	}
}