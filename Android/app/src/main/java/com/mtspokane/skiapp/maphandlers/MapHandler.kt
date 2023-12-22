package com.mtspokane.skiapp.maphandlers

import android.os.Build
import android.util.Log
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.RawRes
import androidx.annotation.UiThread
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.Dash
import com.google.android.gms.maps.model.Gap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Polygon
import com.google.android.gms.maps.model.RoundCap
import com.google.maps.android.data.kml.KmlLineString
import com.google.maps.android.data.kml.KmlPlacemark
import com.google.maps.android.data.kml.KmlPolygon
import com.google.maps.android.ktx.addPolygon
import com.google.maps.android.ktx.addPolyline
import com.google.maps.android.ktx.utils.kml.kmlLayer
import com.mtspokane.skiapp.BuildConfig
import com.mtspokane.skiapp.R
import com.mtspokane.skiapp.mapItem.MapItem
import com.mtspokane.skiapp.mapItem.PolylineMapItem
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch

abstract class MapHandler(internal val activity: FragmentActivity) : OnMapReadyCallback {

	internal lateinit var googleMap: GoogleMap

	var isNightOnly = false

	abstract val additionalCallback: OnMapReadyCallback

	lateinit var chairliftPolylines: List<PolylineMapItem>
	lateinit var easyRunsPolylines: List<PolylineMapItem>
	lateinit var moderateRunsPolylines: List<PolylineMapItem>
	lateinit var difficultRunsPolylines: List<PolylineMapItem>

	lateinit var skiAreaBounds: MapItem
	lateinit var otherBounds: List<MapItem> // Should be 9
	lateinit var startingChairliftTerminals: List<MapItem> // Should be size 6
	lateinit var endingChairliftTerminals: List<MapItem> // Should be size 6

	lateinit var easyRunsBounds: List<MapItem> // Should be size 25
	lateinit var moderateRunsBounds: List<MapItem> // Should be size 26
	lateinit var difficultRunsBounds: List<MapItem> // Should be size 33

	open fun destroy() {

		for (chairliftPolyline in chairliftPolylines) {
			chairliftPolyline.destroyUIItems()
		}

		for (easyRunPolyline in easyRunsPolylines) {
			easyRunPolyline.destroyUIItems()
		}

		for (moderateRunPolyline in moderateRunsPolylines) {
			moderateRunPolyline.destroyUIItems()
		}

		for (difficultRunsPolyline in difficultRunsPolylines) {
			difficultRunsPolyline.destroyUIItems()
		}

		// Clear the map if its not null.
		Log.v("MapHandler", "Clearing map.")
		googleMap.clear()

		System.gc()
	}

	/**
	 * Manipulates the map once available.
	 * This callback is triggered when the map is ready to be used.
	 * This is where we can add markers or lines, add listeners or move the camera. In this case,
	 * we just add a marker near Sydney, Australia.
	 * If Google Play services is not installed on the device, the user will be prompted to install
	 * it inside the SupportMapFragment. This method will only be triggered once the user has
	 * installed Google Play services and returned to the app.
	 */
	override fun onMapReady(map: GoogleMap) {

		val tag = "onMapReady"

		Log.v(tag, "Setting up map for the first time...")
		googleMap = map

		// Setup camera view logging.
		if (BuildConfig.DEBUG) {
			googleMap.setOnCameraIdleListener {
				val cameraPosition: CameraPosition = googleMap.cameraPosition

				val cameraTag = "OnCameraIdle"
				Log.d(cameraTag, "Bearing: ${cameraPosition.bearing}")
				Log.d(cameraTag, "Target: ${cameraPosition.target}")
				Log.d(cameraTag, "Tilt: ${cameraPosition.tilt}")
				Log.d(cameraTag, "Zoom: ${cameraPosition.zoom}")
			}
		}

		// Move the map camera view and set the view restrictions.
		googleMap.moveCamera(CameraUpdateFactory.newCameraPosition(CameraPosition.Builder()
				.target(LatLng(47.92517834073426, -117.10480503737926)).tilt(45F)
				.bearing(317.50552F).zoom(14.414046F).build()))
		googleMap.setLatLngBoundsForCameraTarget(CAMERA_BOUNDS)
		googleMap.setMinZoomPreference(MINIMUM_ZOOM)
		googleMap.setMaxZoomPreference(MAXIMUM_ZOOM)

		// Set the map view type to satellite.
		googleMap.mapType = GoogleMap.MAP_TYPE_SATELLITE

		// Load the various polylines onto the map.
		activity.lifecycleScope.launch {

			val polylinesJob = async(context=Dispatchers.Main, start=CoroutineStart.LAZY) {
				Log.d(tag, "Loading polylines...")
				chairliftPolylines = loadPolylines(R.raw.lifts, R.color.chairlift, 4f,
					R.drawable.ic_chairlift)
				easyRunsPolylines = loadPolylines(R.raw.easy, R.color.easy, 3f, R.drawable.ic_easy)
				moderateRunsPolylines = loadPolylines(R.raw.moderate, R.color.moderate, 2f,
					R.drawable.ic_moderate)
				difficultRunsPolylines = loadPolylines(R.raw.difficult, R.color.difficult, 1f,
					R.drawable.ic_difficult)

				System.gc()
				Log.d(tag, "Finished loading polylines")
			}

			val polygonsJob = async(context=Dispatchers.Main, start=CoroutineStart.LAZY) {
				Log.d(tag, "Adding other bounds...")
				val other = loadPolygons(R.raw.other, R.color.other_polygon_fill)
				val bounds = mutableListOf<MapItem>()
				for (name in other.keys) {
					val values = other[name]!!
					val polygonPoints: MutableList<List<LatLng>> = mutableListOf()
					for (value in values) {
						polygonPoints.add(value.points)
					}

					if (name == "Ski Area Bounds") {
						values[0].remove()
						skiAreaBounds = MapItem(name, polygonPoints)
						continue
					}

					Log.d(tag, "Getting icon for $name")
					val icon: Int? = when (name) {
						"Lodge 1" -> R.drawable.ic_lodge
						"Lodge 2" -> R.drawable.ic_lodge
						"Yurt" -> R.drawable.ic_yurt
						"Vista House" -> R.drawable.ic_vista_house
						"Ski Patrol Building" -> R.drawable.ic_ski_patrol_icon
						"Lodge 1 Parking Lot" -> R.drawable.ic_parking
						"Lodge 2 Parking Lot" -> R.drawable.ic_parking
						"Tubing Area" -> R.drawable.ic_missing // todo Tubing area icon
						"Ski School" -> R.drawable.ic_missing // todo Ski school icon
						else -> {
							Log.w(tag, "$name does not have an icon")
							null
						}
					}
					bounds.add(MapItem(name, polygonPoints, icon))
				}
				otherBounds = bounds

				System.gc()
				Log.d(tag, "Finished adding other bounds")

				Log.d(tag, "Adding starting chairlift terminals...")
				startingChairliftTerminals = loadMapItems(R.raw.starting_lift_polygons,
					R.color.chairlift_polygon, R.drawable.ic_chairlift)

				System.gc()
				Log.d(tag, "Finished adding ending chairlift terminals")

				Log.d(tag, "Adding ending chairlift terminals...")
				endingChairliftTerminals = loadMapItems(R.raw.ending_lift_polygons,
					R.color.chairlift_polygon, R.drawable.ic_chairlift)

				System.gc()
				Log.d(tag, "Finished adding ending chairlift terminals")

				Log.d(tag, "Adding easy bounds...")
				easyRunsBounds = loadMapItems(R.raw.easy_polygons, R.color.easy_polygon, R.drawable.ic_easy)

				System.gc()
				Log.d(tag, "Finished adding easy bounds")

				Log.d(tag, "Adding moderate bounds...")
				moderateRunsBounds = loadMapItems(R.raw.moderate_polygons, R.color.moderate_polygon,
					R.drawable.ic_moderate)

				System.gc()
				Log.d(tag, "Finished adding moderate bounds")

				Log.d(tag, "Adding difficult bounds...")
				difficultRunsBounds = loadMapItems(R.raw.difficult_polygons, R.color.difficult_polygon,
					R.drawable.ic_difficult)

				System.gc()
				Log.d(tag, "Finished adding difficult bounds")
			}

			val callbackJob = async(context=Dispatchers.Main, start=CoroutineStart.LAZY) {
				Log.d("onMapReady", "Running additional setup steps...")
				additionalCallback.onMapReady(googleMap)
				Log.d("onMapReady", "Finished setting up map.")
			}

			awaitAll(polylinesJob, polygonsJob, callbackJob)
		}
	}

	private fun parseKmlFile(@RawRes file: Int): Iterable<KmlPlacemark> {
		val kml = kmlLayer(googleMap, file, activity)
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && kml.placemarks.spliterator().estimateSize() == 0L) {
			Log.w("parseKmlFile", "No placemarks in kml file!")
		}
		return kml.placemarks
	}

	@UiThread
	private fun loadPolylines(@RawRes fileRes: Int, @ColorRes color: Int, zIndex: Float,
	                          @DrawableRes icon: Int? = null): List<PolylineMapItem> {

		val hashMap: HashMap<String, PolylineMapItem> = HashMap()

		// Load the polyline from the file, and iterate though each placemark.
		for (placemark in parseKmlFile(fileRes)) {

			// Get the name of the polyline.
			val name: String = getPlacemarkName(placemark)

			// Get the LatLng coordinates of the placemark.
			val lineString: KmlLineString = placemark.geometry as KmlLineString
			val coordinates: ArrayList<LatLng> = lineString.geometryObject

			// Get the color of the polyline.
			val argb = getARGB(color)

			// Get the properties of the polyline.
			val polylineProperties: List<String>? = if (placemark.hasProperty(PROPERTY_KEY)) {
				placemark.getProperty(PROPERTY_KEY).split('\n')
			} else {
				null
			}

			// Check if the polyline is an easy way down polyline.
			val easiestWayDown = polylineHasProperty(polylineProperties, "easiest way down")

			// Create the polyline using the coordinates and other options.
			val polyline = googleMap.addPolyline {
				addAll(coordinates)
				color(argb)
				if (easiestWayDown) {
					pattern(listOf(Gap(2.0F), Dash(8.0F)))
				}
				geodesic(true)
				startCap(RoundCap())
				endCap(RoundCap())
				clickable(false)
				width(8.0F)
				zIndex(zIndex)
				visible(true)
			}

			// Check if the map item is already in the hashmap.
			if (hashMap[name] == null) {

				// Check if this is a night item.
				val night = polylineHasProperty(polylineProperties, "night run")

				// Create a new map item for the polyline (since its not in the hashmap).
				val mapItem = PolylineMapItem(name, MutableList(1) { polyline }, isNightRun = night, icon = icon)

				// Add the map item to the hashmap.
				hashMap[name] = mapItem

			} else {

				// Add the polyline to the map item.
				hashMap[name]!!.polylines.add(polyline)
			}
		}

		return hashMap.values.toList()
	}

	@UiThread
	private fun loadPolygons(@RawRes fileRes: Int, @ColorRes color: Int):
			HashMap<String, List<Polygon>> {

		val hashMap: HashMap<String, List<Polygon>> = HashMap() // todo Consider making this a set or regular map..

		// Load the polygons file.
		for (placemark in parseKmlFile(fileRes)) {

			val kmlPolygon: KmlPolygon = placemark.geometry as KmlPolygon

			val argb = getARGB(color)

			val polygon = googleMap.addPolygon {
				addAll(kmlPolygon.outerBoundaryCoordinates)
				clickable(false)
				geodesic(true)
				zIndex(0.5F)
				fillColor(argb)
				strokeColor(argb)
				strokeWidth(8.0F)
				visible(BuildConfig.DEBUG)
			}

			val name: String = getPlacemarkName(placemark)
			Log.d("loadPolygons", "Loading polygon for $name")

			if (hashMap[name] == null) {
				hashMap[name] = List(1) { polygon }
			} else {
				val list: MutableList<Polygon> = hashMap[name]!!.toMutableList()
				list.add(polygon)
				hashMap[name] = list.toList()
			}
		}

		return hashMap
	}

	private fun loadMapItems(@RawRes fileRes: Int, @ColorRes color: Int,
							 @DrawableRes drawableRes: Int? = null): List<MapItem> {
		val mapItems = mutableListOf<MapItem>()
		val polygons = loadPolygons(fileRes, color)
		for (name in polygons.keys) {
			val values = polygons[name]!!
			val polygonPoints: MutableList<List<LatLng>> = mutableListOf()
			for (value in values) {
				polygonPoints.add(value.points)
			}
			mapItems.add(MapItem(name, polygonPoints, drawableRes))
		}
		return mapItems
	}

	fun getARGB(@ColorRes color: Int): Int {
		return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			activity.getColor(color)
		} else {
			ResourcesCompat.getColor(activity.resources, color, null)
		}
	}

	companion object {

		private val CAMERA_BOUNDS = LatLngBounds(LatLng(47.912728, -117.133402),
			LatLng(47.943674, -117.092470))

		private const val MINIMUM_ZOOM = 13.0F

		private const val MAXIMUM_ZOOM = 20.0F

		private const val PROPERTY_KEY = "description"

		private fun getPlacemarkName(placemark: KmlPlacemark): String {

			return if (placemark.hasProperty("name")) {
				placemark.getProperty("name")
			} else {

				// If the name wasn't found in the properties return an empty string.
				Log.w("getPlacemarkName", "Placemark is missing name!")
				""
			}
		}

		private fun polylineHasProperty(polylineProperties: List<String>?, propertyKey: String): Boolean {

			if (polylineProperties == null) {
				return false
			}

			for (property in polylineProperties) {
				Log.v("polylineHasProperty", "Checking if property \"$property\" matches property key \"$propertyKey\"")
				if (property.contains(propertyKey)) {
					return true
				}
			}

			return false
		}
	}
}