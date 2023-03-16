package com.mtspokane.skiapp.mapItem

import androidx.annotation.DrawableRes
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Polyline

class PolylineMapItem(name: String, val polylines: MutableList<Polyline>, @DrawableRes icon: Int? = null,
                      private val isNightRun: Boolean = false) : MapItem(name, mutableListOf(), icon) {

	var defaultVisibility = true
		private set

	private var nightOnlyVisibility = false

	fun destroyUIItems() {
		for (polyline in polylines) {
			polyline.remove()
		}
		polylines.clear()
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

		for (polyline in polylines) {
			polyline.isVisible = this.defaultVisibility && (this.isNightRun >= this.nightOnlyVisibility)
		}
	}
}