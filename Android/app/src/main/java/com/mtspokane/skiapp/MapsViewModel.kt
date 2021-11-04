package com.mtspokane.skiapp

import android.app.Application
import android.os.Build
import androidx.annotation.ColorRes
import androidx.core.content.res.ResourcesCompat
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

	private val chairlifts: HashMap<String, MapItem> = HashMap(6)

	private val easyRuns: HashMap<String, MapItem> = HashMap(22)

	private val moderateRuns: HashMap<String, MapItem> = HashMap(19)

	private val difficultRuns: HashMap<String, MapItem> = HashMap(25)

	fun createChairLifts(map: GoogleMap) {

		// Load in the chairlift kml file.
		val kml = KmlLayer(map, R.raw.lifts, this.getApplication<Application>().applicationContext)

		// Iterate though each placemark...
		kml.placemarks.forEach {

			// Get the name and polyline.
			val hashPair = getHashmapPair(it, R.color.chairlift, 4, map)

			// Check if the map item is already in the hashmap.
			if (this.chairlifts[hashPair.first] == null) {

				// Create a map item and add it to the hashmap.
				this.chairlifts[hashPair.first] = createMapItem(it.properties, hashPair.first, hashPair.second)

			} else {

				// Add the polyline to the map item.
				this.chairlifts[hashPair.first]!!.addPolyLine(hashPair.second)
			}
		}
	}

	fun createEasyRuns(map: GoogleMap) {

		// Load in the easy runs kml file.
		val kml = KmlLayer(map, R.raw.easy, this.getApplication<Application>().applicationContext)

		// Iterate though each placemark...
		kml.placemarks.forEach {

			// Get the name and polyline.
			val hashPair = getHashmapPair(it, R.color.easy, 3, map)

			// Check if the map item is already in the hashmap.
			if (this.easyRuns[hashPair.first] == null) {

				// Create a map item and add it to the hashmap.
				this.easyRuns[hashPair.first] = createMapItem(it.properties, hashPair.first, hashPair.second)

			} else {

				// Add the polyline to the map item.
				this.easyRuns[hashPair.first]!!.addPolyLine(hashPair.second)
			}
		}
	}

	fun createModerateRuns(map: GoogleMap) {

		// Load in the moderate runs kml file.
		val kml = KmlLayer(map, R.raw.moderate, this.getApplication<Application>().applicationContext)

		// Iterate though each placemark...
		kml.placemarks.forEach {

			// Get the name and polyline.
			val hashPair = getHashmapPair(it, R.color.moderate, 2, map)

			// Check if the map item is already in the hashmap.
			if (this.moderateRuns[hashPair.first] == null) {

				// Create a map item and add it to the hashmap.
				this.moderateRuns[hashPair.first] = createMapItem(it.properties, hashPair.first, hashPair.second)

			} else {

				// Add the polyline to the map item.
				this.moderateRuns[hashPair.first]!!.addPolyLine(hashPair.second)
			}
		}
	}

	fun createDifficultRuns(map: GoogleMap) {

		// Load in the difficult runs kml file.
		val kml = KmlLayer(map, R.raw.difficult, this.getApplication<Application>().applicationContext)

		// Iterate though each placemark...
		kml.placemarks.forEach {

			// Get the name and polyline.
			val hashPair = getHashmapPair(it, R.color.difficult, 1, map)

			// Check if the map item is already in the hashmap.
			if (this.difficultRuns[hashPair.first] == null) {

				// Create a map item and add it to the hashmap.
				this.difficultRuns[hashPair.first] = createMapItem(it.properties, hashPair.first, hashPair.second)

			} else {

				// Add the polyline to the map item.
				this.difficultRuns[hashPair.first]!!.addPolyLine(hashPair.second)
			}
		}
	}

	private fun createPolyline(vararg coordinates: LatLng, @ColorRes color: Int, zIndex: Short,
	                           map: GoogleMap): Polyline {

		val application: Application = this.getApplication<Application>()
		val argb = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			application.getColor(color)
		} else {
			ResourcesCompat.getColor(application.resources, color, null)
		}

		return map.addPolyline(PolylineOptions()
			.add(*coordinates)
			.color(argb)
			.geodesic(true)
			.startCap(RoundCap())
			.endCap(RoundCap())
			.clickable(false)
			.width(8F)
			.zIndex(zIndex.toFloat())
			.visible(true))
	}

	private fun getHashmapPair(placemark: KmlPlacemark, @ColorRes color: Int, zIndex: Short,
	                           map: GoogleMap): Pair<String, Polyline> {

		// Get the name of the placemark.
		@Suppress("UNCHECKED_CAST")
		val name = (placemark.properties.elementAt(0) as Map.Entry<String, String>).value

		// Get the LatLng coordinates of the placemark.
		val lineString: KmlLineString = placemark.geometry as KmlLineString
		val coordinates: Array<LatLng> = lineString.geometryObject.toTypedArray()

		// Create the polyline using the coordinates.
		val polyline = createPolyline(*coordinates, color = color, zIndex = zIndex, map = map)

		// Return the name and polyline as a pair for the hashmap.
		return Pair(name, polyline)
	}

	private fun createMapItem(properties: MutableIterable<Any?>, name: String, polyline: Polyline): MapItem {

		// Check if this is a night item.
		var night = false
		if (properties.count() == 2) {

			// Its a night item if it the 2nd property contains "night run".
			@Suppress("UNCHECKED_CAST")
			night = (properties.elementAt(1) as Map.Entry<String, String>).value.contains("night run")
		}

		// Create a new map item for the polyline (since its not in the hashmap).
		val mapItem = MapItem(name, night)
		mapItem.addPolyLine(polyline)

		return mapItem
	}
}