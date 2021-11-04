package com.mtspokane.skiapp

class MapItem(val name: String, private val isNightRun: Boolean = false) {

	//private var polyline: Array<Polyline> = emptyArray()

	var defaultVisibility = true
		private set

	/*
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
	} */
}