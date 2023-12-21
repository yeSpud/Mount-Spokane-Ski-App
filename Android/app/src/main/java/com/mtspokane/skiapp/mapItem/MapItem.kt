package com.mtspokane.skiapp.mapItem

import android.location.Location
import androidx.annotation.AnyThread
import androidx.annotation.DrawableRes
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Polyline
import com.google.maps.android.PolyUtil

open class MapItem(val name: String, val points: List<List<LatLng>>, @DrawableRes val icon: Int? = null) {

	@AnyThread
	fun locationInsidePoints(skiingActivity: SkiingActivity): Boolean {
		for (point in points) {
			if (PolyUtil.containsLocation(skiingActivity.latitude, skiingActivity.longitude, point, true)) {
				return true
			}
		}
		return false
	}

	@AnyThread
	fun locationInsidePoints(location: Location): Boolean {
		for (point in points) {
			if (PolyUtil.containsLocation(location.latitude, location.longitude, point, true)) {
				return true
			}
		}
		return false
	}
}

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