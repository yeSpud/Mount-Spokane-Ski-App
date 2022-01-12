package com.mtspokane.skiapp.maphandlers

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.AnyThread
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.RawRes
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.FragmentActivity
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.Dash
import com.google.android.gms.maps.model.Gap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Polygon
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.RoundCap
import com.google.maps.android.data.kml.KmlLineString
import com.google.maps.android.data.kml.KmlPlacemark
import com.google.maps.android.data.kml.KmlPolygon
import com.google.maps.android.ktx.addPolygon
import com.google.maps.android.ktx.addPolyline
import com.google.maps.android.ktx.utils.kml.kmlLayer
import com.mtspokane.skiapp.BuildConfig
import com.mtspokane.skiapp.mapItem.VisibleUIMapItem
import kotlin.Throws
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

open class MapHandler(private val initialCameraPosition: CameraPosition) : OnMapReadyCallback {

	internal var map: GoogleMap? = null

	private var additionalCallback: OnMapReadyCallback? = null

	open fun destroy() {

		// Clear the map if its not null.
		if (this.map != null) {
			Log.v("MapHandler", "Clearing map.")
			this.map!!.clear()
			this.map = null
		}

		if (this.additionalCallback != null) {
			Log.v("MapHandler", "Clearing additional callback.")
			this.additionalCallback = null
		}
	}

	internal fun setAdditionalCallback(additionalCallback: OnMapReadyCallback) {
		this.additionalCallback = additionalCallback
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
	override fun onMapReady(googleMap: GoogleMap) {

		Log.v("onMapReady", "Setting up map for the first time...")

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
		googleMap.moveCamera(CameraUpdateFactory.newCameraPosition(this.initialCameraPosition))
		googleMap.setLatLngBoundsForCameraTarget(CAMERA_BOUNDS)
		googleMap.setMinZoomPreference(MINIMUM_ZOOM)
		googleMap.setMaxZoomPreference(MAXIMUM_ZOOM)

		// Set the map view type to satellite.
		googleMap.mapType = GoogleMap.MAP_TYPE_SATELLITE

		this.map = googleMap

		if (this.additionalCallback != null) {
			Log.v("onMapReady", "Running additional setup steps...")
			this.additionalCallback!!.onMapReady(this.map!!)
		}

		Log.v("onMapReady", "Finished setting up map.")
	}

	@Throws(NullPointerException::class)
	private fun parseKmlFile(@RawRes file: Int, context: Context): Iterable<KmlPlacemark> {

		if (this.map == null) {
			throw NullPointerException("Map has not been setup yet!")
		}

		val kml = kmlLayer(this.map!!, file, context)
		return kml.placemarks
	}

	@AnyThread
	@Throws(NullPointerException::class)
	suspend fun loadPolylines(@RawRes fileRes: Int, activity: FragmentActivity,
	                          @ColorRes color: Int, zIndex: Float, @DrawableRes icon: Int? = null):
			Array<VisibleUIMapItem> = coroutineScope {

		if (this@MapHandler.map == null) {
			throw NullPointerException("Map has not been setup yet!")
		}

		val hashMap: HashMap<String, VisibleUIMapItem> = HashMap()

		// Load the polyline from the file, and iterate though each placemark.
		this@MapHandler.parseKmlFile(fileRes, activity).forEach {

			// Get the name of the polyline.
			val name: String = getPlacemarkName(it)

			// Get the LatLng coordinates of the placemark.
			val lineString: KmlLineString = it.geometry as KmlLineString
			val coordinates: ArrayList<LatLng> = lineString.geometryObject

			// Get the color of the polyline.
			val argb = getARGB(activity, color)

			// Get the properties of the polyline.
			val polylineProperties: List<String>? = if (it.hasProperty(PROPERTY_KEY)) {
				it.getProperty(PROPERTY_KEY).split('\n')
			} else {
				null
			}

			// Check if the polyline is an easy way down polyline.
			val easiestWayDown = polylineHasProperty(polylineProperties, "easiest way down")

			// Create the polyline using the coordinates and other options.
			var polyline: Polyline
			withContext(Dispatchers.Main) {
				polyline = this@MapHandler.map!!.addPolyline {
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
			}

			// Check if the map item is already in the hashmap.
			if (hashMap[name] == null) {

				// Check if this is a night item.
				val night = polylineHasProperty(polylineProperties, "night run")

				// Create a new map item for the polyline (since its not in the hashmap).
				val mapItem = VisibleUIMapItem(name, arrayOf(polyline), isNightRun = night, icon = icon)

				// Add the map item to the hashmap.
				hashMap[name] = mapItem

			} else {

				// Add the polyline to the map item.
				hashMap[name]!!.addAdditionalPolyLine(polyline)
			}
		}

		return@coroutineScope hashMap.values.toTypedArray()
	}

	@AnyThread
	@Throws(NullPointerException::class)
	suspend fun loadPolygons(@RawRes fileRes: Int, context: Context, @ColorRes color: Int,
	                         visible: Boolean = BuildConfig.DEBUG): HashMap<String, Array<Polygon>> = coroutineScope {

		if (this@MapHandler.map == null) {
			throw NullPointerException("Map has not been setup yet!")
		}

		val hashMap: HashMap<String, Array<Polygon>> = HashMap()

		// Load the polygons file.
		this@MapHandler.parseKmlFile(fileRes, context).forEach { placemark ->

			val kmlPolygon: KmlPolygon = placemark.geometry as KmlPolygon

			val argb = getARGB(context, color)

			val polygon: Polygon
			withContext(Dispatchers.Main) {
				polygon = this@MapHandler.map!!.addPolygon {
					addAll(kmlPolygon.outerBoundaryCoordinates)
					clickable(false)
					geodesic(true)
					zIndex(0.5F)
					fillColor(argb)
					strokeColor(argb)
					strokeWidth(8.0F)
					visible(visible)
				}
			}

			val name: String = getPlacemarkName(placemark)
			Log.d("loadPolygons", "Loading polygon for $name")

			if (hashMap[name] == null) {
				val array: Array<Polygon> = Array(1) { polygon }
				hashMap[name] = array
			} else {
				val oldArray: Array<Polygon>? = hashMap[name]
				if (oldArray != null) {
					val largerArray: Array<Polygon> = Array(oldArray.size + 1) {
						if (it == oldArray.size) {
							polygon
						} else {
							oldArray[it]
						}
					}
					hashMap[name] = largerArray
				}
			}
		}

		return@coroutineScope hashMap
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

		private fun getARGB(context: Context, @ColorRes color: Int): Int {
			return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
				context.getColor(color)
			} else {
				ResourcesCompat.getColor(context.resources, color, null)
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