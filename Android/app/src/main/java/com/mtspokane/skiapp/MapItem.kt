package com.mtspokane.skiapp

import android.location.Location
import androidx.annotation.MainThread
import com.google.android.gms.maps.model.Polygon
import com.google.android.gms.maps.model.Polyline
import com.google.maps.android.PolyUtil

class MapItem(val name: String, private val isNightRun: Boolean = false) {

	private var polyline: Array<Polyline> = emptyArray()

	private var polygon: Array<Polygon> = emptyArray()

	var defaultVisibility = true
		private set

	fun addPolyLine(polyline: Polyline) {

		val array: Array<Polyline> = Array(this.polyline.size + 1) {
			if (it == this.polyline.size) {
				polyline
			} else {
				this.polyline[it]
			}
		}

		this.polyline = array
	}

	fun togglePolyLineVisibility(visible: Boolean, nightRunsOnly: Boolean = false) {
		this.polyline.forEach {
			if (nightRunsOnly) {
				if (this.isNightRun) {
					it.isVisible = visible
				} else {
					it.isVisible = false
				}
			} else {
				this.defaultVisibility = visible
				it.isVisible = this.defaultVisibility
			}
		}
	}

	fun addPolygon(polygon: Polygon) {

		val array: Array<Polygon> = Array(this.polygon.size + 1) {
			if (it == this.polygon.size) {
				polygon
			} else {
				this.polygon[it]
			}
		}

		this.polygon = array
	}

	@MainThread
	fun pointInsidePolygon(point: Location): Boolean {
		this.polygon.forEach {
			if (PolyUtil.containsLocation(point.latitude, point.longitude, it.points, true)) {
				return true
			}
		}
		return false
	}
}