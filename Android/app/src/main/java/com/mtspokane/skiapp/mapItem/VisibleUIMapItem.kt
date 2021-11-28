package com.mtspokane.skiapp.mapItem

import androidx.annotation.DrawableRes
import com.google.android.gms.maps.model.Polyline

class VisibleUIMapItem(name: String, private var polyline: Array<Polyline>, @DrawableRes icon: Int? = null,
                       private val isNightRun: Boolean = false): UIMapItem(name, icon = icon) {

	var defaultVisibility = true
		private set

	var nightOnlyVisibility = false
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

	/**
	 * Default visibility | Nights Only | Night Run | Output
	 *        0	                 0	         0	        0
	 *        0	                 0	         1	        0
	 *        0	                 1	         0	        0
	 *        0	                 1	         1	        0
	 *        1	                 0	         0	        1
	 *        1	                 0	         1	        1
	 *        1	                 1	         0	        0
	 *        1	                 1	         1	        1
	 */
	fun togglePolyLineVisibility(visible: Boolean, nightRunsOnly: Boolean) {
		this.defaultVisibility = visible
		this.nightOnlyVisibility = nightRunsOnly

		this.updateVisibility()
	}

	private fun updateVisibility() {
		this.polyline.forEach {
			it.isVisible = this.defaultVisibility && (this.isNightRun >= this.nightOnlyVisibility)
		}
	}
}