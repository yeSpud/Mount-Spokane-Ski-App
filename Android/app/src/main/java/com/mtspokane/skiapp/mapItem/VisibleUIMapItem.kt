package com.mtspokane.skiapp.mapItem

import androidx.annotation.DrawableRes
import com.google.android.gms.maps.model.Polyline

class VisibleUIMapItem(name: String, private var polyline: Array<Polyline>, @DrawableRes icon: Int? = null,
                       private val isNightRun: Boolean = false): UIMapItem(name, icon = icon) {

	var defaultVisibility = true
		private set

	fun addAdditionalPolyLine(polyline: Polyline) {

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
}