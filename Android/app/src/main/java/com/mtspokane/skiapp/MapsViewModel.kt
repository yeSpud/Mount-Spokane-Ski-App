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

	private lateinit var easyRuns: Array<Polyline>

	private lateinit var moderateRuns: Array<Polyline>

	private lateinit var difficultRuns: Array<Polyline>

	private val nightRuns: Array<Polyline?> = arrayOfNulls(0) // TODO Determine size and content

	fun createChairLifts(map: GoogleMap, context: Context) {

		// Load in the chairlift kml file.
		val kml = KmlLayer(map, R.raw.lifts, context)

		// Iterate though all the chairlift placemarks and populate the chairlifts array.
		val placemarks: MutableIterator<KmlPlacemark> = kml.placemarks.iterator()
		this.chairlifts = Array(6) {

			// Get the name of the chairlift and its start and end coordinates as a pair.
			val nameAndCordPair: Pair<String, Array<LatLng>> = getCoordinates(placemarks.next())

			// TODO Comments
			createPolyline(*nameAndCordPair.second, color = Color.RED, zIndex = 4,
				name = nameAndCordPair.first, map = map)
		}
	}

	fun createEasyRuns(map: GoogleMap, context: Context) {

		// Load in the easy runs kml file.
		val kml = KmlLayer(map, R.raw.easy, context)

		// Iterate though all the moderate run placemarks and populate the moderate run array.
		val placemarks: MutableIterator<KmlPlacemark> = kml.placemarks.iterator()
		this.easyRuns = Array(23) {

			// Get the name of the run and its coordinates as a pair.
			val nameAndCordPair: Pair<String, Array<LatLng>> = getCoordinates(placemarks.next())

			// Add the moderate run polyline to the map.
			createPolyline(*nameAndCordPair.second, color = Color.GREEN, zIndex = 3,
				name = nameAndCordPair.first, map = map)
		}
	}

	fun createModerateRuns(map: GoogleMap, context: Context) {

		/*
		// Load in the moderate runs kml file.
		val kml = KmlLayer(map, R.raw.moderate, context) // TODO File

		// Iterate though all the moderate run placemarks and populate the moderate run array.
		val placemarks: MutableIterator<KmlPlacemark> = kml.placemarks.iterator()
		this.moderateRuns = Array(0) {  // TODO Determine actual size

			// Get the name of the run and its coordinates as a pair.
			val nameAndCordPair: Pair<String, Array<LatLng>> = getCoordinates(placemarks.next())

			// Add the moderate run polyline to the map.
			createPolyline(*nameAndCordPair.second, color = Color.BLUE, zIndex = 2,
				name = nameAndCordPair.first, map = map)
		} */
	}

	fun createDifficultRuns(map: GoogleMap, context: Context) {

		/*
		// Load in the difficult runs kml file.
		val kml = KmlLayer(map, R.raw.difficult, context) // TODO File

		// Iterate though all the difficult run placemarks and populate the difficult run array.
		val placemarks: MutableIterator<KmlPlacemark> = kml.placemarks.iterator()
		this.difficultRuns = Array(0) {  // TODO Determine actual size

			// Get the name of the run and its coordinates as a pair.
			val nameAndCordPair: Pair<String, Array<LatLng>> = getCoordinates(placemarks.next())

			// Add the difficult run polyline to the map.
			createPolyline(*nameAndCordPair.second, color = Color.BLACK, zIndex = 1,
				name = nameAndCordPair.first, map = map)
		} */
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

	private fun getCoordinates(placemark: KmlPlacemark): Pair<String, Array<LatLng>> {

		// Get the name of the run.
		@Suppress("UNCHECKED_CAST")
		val nameValuePair: Map.Entry<String, String> = placemark.properties.iterator().next()
				as Map.Entry<String, String>
		val name = nameValuePair.value

		// Get what will be the run polyline.
		val line: KmlLineString = placemark.geometry as KmlLineString

		// Create a new array of latlng coords for the run location.
		val coordinates: Array<LatLng> = Array(line.geometryObject.size) { line.geometryObject[it] }

		return Pair(name, coordinates)
	}
}