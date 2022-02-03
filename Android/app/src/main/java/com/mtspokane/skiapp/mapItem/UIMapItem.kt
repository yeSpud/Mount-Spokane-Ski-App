package com.mtspokane.skiapp.mapItem

import android.location.Location
import androidx.annotation.DrawableRes
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Polygon
import com.google.maps.android.PolyUtil
import com.mtspokane.skiapp.databases.SkiingActivity

open class UIMapItem(name: String, val polygons: MutableList<Polygon>, val points: MutableList<List<LatLng>>,
                     @DrawableRes icon: Int? = null) : MapItem(name, icon) {

	open fun destroyUIItems() {
		this.polygons.clear()
	}

	fun locationInsidePoints(skiingActivity: SkiingActivity): Boolean {

		this.points.forEach {

			if (PolyUtil.containsLocation(skiingActivity.latitude, skiingActivity.longitude, it, true)) {
				return true
			}
		}

		return false

	}

	fun locationInsidePoints(point: Location): Boolean {

		this.points.forEach {

			if (PolyUtil.containsLocation(point.latitude, point.longitude, it, true)) {
				return true
			}
		}

		return false
	}
}