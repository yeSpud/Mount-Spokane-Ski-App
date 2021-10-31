package com.mtspokane.skiapp

import android.content.Context
import android.graphics.Color
import androidx.lifecycle.ViewModel
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.gms.maps.model.RoundCap
import com.google.maps.android.data.kml.KmlLayer
import com.google.maps.android.data.kml.KmlLineString
import com.google.maps.android.data.kml.KmlPlacemark

class MapsViewModel: ViewModel() {

	private lateinit var chairlifts: Array<Polyline>

	private val easyRuns: Array<Polyline?> = arrayOfNulls(12) // TODO Determine actual size

	private val moderateRuns: Array<Polyline?> = arrayOfNulls(0) // TODO Determine size

	private val difficultRuns: Array<Polyline?> = arrayOfNulls(0) // TODO Determine size

	fun createChairLifts(map: GoogleMap, context: Context) {

		// Load in the chairlift kml file
		val kml = KmlLayer(map, R.raw.lifts, context)

		// Iterate though all the chairlift placemarks and populate the chairlifts array.
		val placemarks: MutableIterator<KmlPlacemark> = kml.placemarks.iterator()
		this.chairlifts = Array(6) {
			val placemark: KmlPlacemark = placemarks.next()

			// Get the name if the chairlift.
			val nameValuePair: Map.Entry<String, String> = placemark.properties.iterator().next()
					as Map.Entry<String, String>
			val name = nameValuePair.value

			// Get what will be the chairlift polyline.
			val line: KmlLineString = placemark.geometry as KmlLineString

			// Create a new chairlift object using the geometry objects (the start and end LatLng objects).
			addChairLift(line.geometryObject[0], line.geometryObject[1], name, map)
		}
	}

	fun createEasyRuns(map: GoogleMap) {
		// TODO
	}

	fun createModerateRuns(map: GoogleMap) {
		// TODO
	}

	fun createDifficultRuns(map: GoogleMap) {
		// TODO
	}

	private fun addChairLift(startLocation: LatLng, endLocation: LatLng, name: String, map: GoogleMap): Polyline {
		return createPolyline(startLocation, endLocation, color = Color.RED, zIndex = 4, name = name, map = map)
	}

	private fun addEasyRun(vararg coordinates: LatLng, name: String, map: GoogleMap): Polyline {
		return createPolyline(*coordinates, color = Color.GREEN, zIndex = 3, name = name, map = map)
	}

	private fun addModerateRun(vararg coordinates: LatLng, name: String, map: GoogleMap): Polyline {
		return createPolyline(*coordinates, color = Color.BLUE, zIndex = 2, name = name, map = map)
	}

	private fun addDifficultRun(vararg coordinates: LatLng, name: String, map: GoogleMap): Polyline {
		return createPolyline(*coordinates, color = Color.BLACK, zIndex = 1, name = name, map = map)
	}

	private fun createPolyline(vararg coordinates: LatLng, color: Int, zIndex: Short, name: String,
	                           map: GoogleMap): Polyline {
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