package com.mtspokane.skiapp

import android.graphics.Color
import androidx.lifecycle.ViewModel
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.gms.maps.model.RoundCap

class MapsViewModel: ViewModel() {

	private val chairlifts: Array<Polyline?> = arrayOfNulls(6)

	private val easyRuns: Array<Polyline?> = arrayOfNulls(12)

	fun createChairLifts(map: GoogleMap) {

		this.chairlifts[0] = addChairLift(47.91606715553383, -117.099266845541,
			47.92294353366535, -117.1126129810919, "Chair 1", map)

		this.chairlifts[1] = addChairLift(47.92221929989261, -117.098637384573,
			47.9250541084338, -117.1119355485026, "Chair 2", map)

		this.chairlifts[2] = addChairLift(47.92301666633388, -117.0966530617209,
			47.93080242968863, -117.1039234488206, "Chair 3", map)

		this.chairlifts[3] = addChairLift(47.94163035979481, -117.1005550502552,
			47.9323389155571, -117.1067590655054, "Chair 4", map)

		this.chairlifts[4] = addChairLift(47.92175256734555, -117.0954266523773,
			47.92292940522836, -117.0989319661659, "Chair 5", map)

		this.chairlifts[5] = addChairLift(47.92891149682423, -117.1299404320796,
			47.92339173840757, -117.112973282171, "Chair 6", map)

	}

	fun createEasyRuns(map: GoogleMap) {

		// TODO

	}

	private fun addChairLift(startLatitude: Double, startLongitude: Double, endLatitude: Double,
	                         endLongitude: Double, name: String, map: GoogleMap): Polyline {
		return createPolyline(LatLng(startLatitude, startLongitude), LatLng(endLatitude, endLongitude),
			color = Color.RED, zIndex = 4, name = name, map = map)
	}

	private fun addEasyRun(vararg coordinates: LatLng, name: String, map: GoogleMap): Polyline {
		return createPolyline(*coordinates, color = Color.GREEN, zIndex = 3, name = name, map = map)
	}

	private fun createPolyline(vararg coordinates: LatLng, color: Int, zIndex: Short, name: String, map: GoogleMap): Polyline {
		val polyline = map.addPolyline(PolylineOptions()
			.add(*coordinates)
			.color(color)
			.geodesic(true)
			.startCap(RoundCap())
			.endCap(RoundCap())
			.clickable(false)
			.width(8F)
			.zIndex(zIndex.toFloat())
			.visible(true))
		polyline.tag = name
		return polyline
	}
}