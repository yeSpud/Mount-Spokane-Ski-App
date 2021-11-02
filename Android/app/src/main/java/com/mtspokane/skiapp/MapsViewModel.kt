package com.mtspokane.skiapp

import android.app.Application
import androidx.annotation.ColorRes
import androidx.lifecycle.AndroidViewModel
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.gms.maps.model.RoundCap
import com.google.maps.android.data.kml.KmlLayer
import com.google.maps.android.data.kml.KmlLineString
import com.google.maps.android.data.kml.KmlPlacemark

class MapsViewModel(activity: MapsActivity): AndroidViewModel(activity.application) {

	private lateinit var chairlifts: Array<Polyline>

	private lateinit var easyRuns: Array<Polyline>

	private lateinit var moderateRuns: Array<Polyline>

	private lateinit var difficultRuns: Array<Polyline>

	private val nightRuns: Array<Polyline?> = arrayOfNulls(0) // TODO Determine size and content

	fun createChairLifts(map: GoogleMap) {

		// Load in the chairlift kml file.
		val kml = KmlLayer(map, R.raw.lifts, this.getApplication<Application>().applicationContext)

		// Iterate though all the chairlift placemarks and populate the chairlifts array.
		val placemarks: MutableIterator<KmlPlacemark> = kml.placemarks.iterator()
		this.chairlifts = Array(6) {

			// Get the name of the chairlift and its start and end coordinates as a pair.
			val nameAndCordPair: Pair<String, Array<LatLng>> = getCoordinates(placemarks.next())

			// Add the chairlifts to the map.
			createPolyline(*nameAndCordPair.second, color = R.color.chairlift, zIndex = 4,
				name = nameAndCordPair.first, map = map)
		}
	}

	fun createEasyRuns(map: GoogleMap) {

		// Load in the easy runs kml file.
		val kml = KmlLayer(map, R.raw.easy, this.getApplication<Application>().applicationContext)

		// Iterate though all the moderate run placemarks and populate the moderate run array.
		val placemarks: MutableIterator<KmlPlacemark> = kml.placemarks.iterator()
		this.easyRuns = Array(23) {

			// Get the name of the run and its coordinates as a pair.
			val nameAndCordPair: Pair<String, Array<LatLng>> = getCoordinates(placemarks.next())

			// Add the easy run polyline to the map.
			createPolyline(*nameAndCordPair.second, color = R.color.easy, zIndex = 3,
				name = nameAndCordPair.first, map = map)
		}
	}

	fun createModerateRuns(map: GoogleMap) {

		/*
		// Load in the moderate runs kml file.
		val kml = KmlLayer(map, R.raw.moderate, this.getApplication<Application>().applicationContext) // TODO File

		// Iterate though all the moderate run placemarks and populate the moderate run array.
		val placemarks: MutableIterator<KmlPlacemark> = kml.placemarks.iterator()
		this.moderateRuns = Array(33) {  // TODO Determine actual size

			// Get the name of the run and its coordinates as a pair.
			val nameAndCordPair: Pair<String, Array<LatLng>> = getCoordinates(placemarks.next())

			// Add the moderate run polyline to the map.
			createPolyline(*nameAndCordPair.second, color = R.color.moderate, zIndex = 2,
				name = nameAndCordPair.first, map = map)
		} */
	}

	fun createDifficultRuns(map: GoogleMap) {

		/*
		// Load in the difficult runs kml file.
		val kml = KmlLayer(map, R.raw.difficult, this.getApplication<Application>().applicationContext) // TODO File

		// Iterate though all the difficult run placemarks and populate the difficult run array.
		val placemarks: MutableIterator<KmlPlacemark> = kml.placemarks.iterator()
		this.difficultRuns = Array(0) {  // TODO Determine actual size

			// Get the name of the run and its coordinates as a pair.
			val nameAndCordPair: Pair<String, Array<LatLng>> = getCoordinates(placemarks.next())

			// Add the difficult run polyline to the map.
			createPolyline(*nameAndCordPair.second, color = R.color.difficult, zIndex = 1,
				name = nameAndCordPair.first, map = map)
		} */
	}

	private fun createPolyline(vararg coordinates: LatLng, @ColorRes color: Int, zIndex: Short,
	                           name: String, map: GoogleMap): Polyline {
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