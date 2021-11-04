package com.mtspokane.skiapp

import com.google.android.gms.maps.model.Polyline

class MapItem(val name: String, val night: Boolean = false) {

	private var polyline: Array<Polyline> = emptyArray()

	fun addPolyLine(polyline: Polyline) {

		val array: Array<Polyline> = Array(this.polyline.size+1) {
			if (it == this.polyline.size) {
				polyline
			} else {
				this.polyline[it]
			}
		}

		this.polyline = array
	}

	fun togglePolyLineVisibility(visible: Boolean) {
		this.polyline.forEach { it.isVisible = visible }
	}
}