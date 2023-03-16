package com.mtspokane.skiapp.mapItem

import android.location.Location
import androidx.annotation.AnyThread
import androidx.annotation.DrawableRes
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.PolyUtil
import com.mtspokane.skiapp.databases.SkiingActivity
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
	fun locationInsidePoints(point: Location): Boolean {

		for (p in points) {
			if (PolyUtil.containsLocation(point.latitude, point.longitude, p, true)) {
				return true
			}
		}

		return false
	}
}