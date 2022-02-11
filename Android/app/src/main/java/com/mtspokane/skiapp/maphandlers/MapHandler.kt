package com.mtspokane.skiapp.maphandlers

import android.os.Build
import android.util.Log
import androidx.annotation.AnyThread
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.RawRes
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
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.RoundCap
import com.google.maps.android.data.kml.KmlLineString
import com.google.maps.android.data.kml.KmlPlacemark
import com.google.maps.android.data.kml.KmlPolygon
import com.google.maps.android.ktx.addPolygon
import com.google.maps.android.ktx.addPolyline
import com.google.maps.android.ktx.utils.kml.kmlLayer
import com.mtspokane.skiapp.BuildConfig
import com.mtspokane.skiapp.R
import com.mtspokane.skiapp.mapItem.MtSpokaneMapItems
import com.mtspokane.skiapp.mapItem.PolylineMapItem
import java.util.Locale
import kotlin.Throws
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

open class MapHandler(internal val activity: FragmentActivity, private val initialCameraPosition: CameraPosition) : OnMapReadyCallback {

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

		MtSpokaneMapItems.destroyUIItems(this.activity::class)
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

		val tag = "onMapReady"

		Log.v(tag, "Setting up map for the first time...")

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

		// Checkout MtSpokaneMapItems.
		MtSpokaneMapItems.checkoutObject(this.activity::class)

		// Load the various polylines onto the map.
		this.activity.lifecycleScope.launch(Dispatchers.IO, CoroutineStart.LAZY) {

			Log.d(tag, "Setting up map polylines...")

			// Start with the chairlift polylines.
			if (MtSpokaneMapItems.chairlifts == null) {
				MtSpokaneMapItems.initializeChairliftsAsync(this@MapHandler.activity::class,
					this@MapHandler).await()
			} else {
				this@MapHandler.loadPolylinesHeadlessAsync("Loading headless chairlift polylines",
					R.raw.lifts, R.color.chairlift, 4.0F, R.drawable.ic_chairlift).await()
			}

			// Move onto the easy runs.
			if (MtSpokaneMapItems.easyRuns == null) {
				MtSpokaneMapItems.initializeEasyRunsAsync(this@MapHandler.activity::class,
					this@MapHandler).await()
			} else {
				this@MapHandler.loadPolylinesHeadlessAsync("Loading headless easy polylines",
					R.raw.easy, R.color.easy, 3.0F, R.drawable.ic_easy).await()
			}

			// Then go to moderate runs.
			if (MtSpokaneMapItems.moderateRuns == null) {
				MtSpokaneMapItems.initializeModerateRunsAsync(this@MapHandler.activity::class,
				this@MapHandler).await()
			} else {
				this@MapHandler.loadPolylinesHeadlessAsync("Loading headless moderate polylines",
					R.raw.moderate, R.color.moderate, 2.0F, R.drawable.ic_moderate).await()
			}

			// Finish with advanced runs.
			if (MtSpokaneMapItems.difficultRuns == null) {
				MtSpokaneMapItems.initializeDifficultRunsAsync(this@MapHandler.activity::class,
				this@MapHandler).await()
			} else {
				this@MapHandler.loadPolylinesHeadlessAsync("Loading headless difficult polylines",
					R.raw.difficult, R.color.difficult, 1.0F, R.drawable.ic_difficult).await()
			}

			Log.d(tag, "Finished setting up map polylines")

		}.start()

		this.map = googleMap

		if (this.additionalCallback != null) {
			Log.v("onMapReady", "Running additional setup steps...")
			this.additionalCallback!!.onMapReady(this.map!!)
		}

		Log.v("onMapReady", "Finished setting up map.")
	}

	@Throws(NullPointerException::class)
	private fun parseKmlFile(@RawRes file: Int): Iterable<KmlPlacemark> {

		if (this.map == null) {
			throw NullPointerException("Map has not been setup yet!")
		}

		val kml = kmlLayer(this.map!!, file, this.activity)
		return kml.placemarks
	}

	@AnyThread
	@Throws(NullPointerException::class)
	suspend fun loadPolylines(@RawRes fileRes: Int, @ColorRes color: Int, zIndex: Float,
	                          @DrawableRes icon: Int? = null): List<PolylineMapItem> = coroutineScope {

		if (this@MapHandler.map == null) {
			throw NullPointerException("Map has not been setup yet!")
		}

		val hashMap: HashMap<String, PolylineMapItem> = HashMap()

		// Load the polyline from the file, and iterate though each placemark.
		this@MapHandler.parseKmlFile(fileRes).forEach {

			// Get the name of the polyline.
			val name: String = getPlacemarkName(it)

			// Get the LatLng coordinates of the placemark.
			val lineString: KmlLineString = it.geometry as KmlLineString
			val coordinates: ArrayList<LatLng> = lineString.geometryObject

			// Get the color of the polyline.
			val argb = this@MapHandler.getARGB(color)

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
				val mapItem = PolylineMapItem(name, MutableList(1) { polyline }, isNightRun = night, icon = icon)

				// Add the map item to the hashmap.
				hashMap[name] = mapItem

			} else {

				// Add the polyline to the map item.
				hashMap[name]!!.polylines.add(polyline)
			}
		}

		return@coroutineScope hashMap.values.toList()
	}

	@AnyThread
	@Throws(NullPointerException::class)
	suspend fun loadPolygons(@RawRes fileRes: Int, @ColorRes color: Int, visible: Boolean = BuildConfig.DEBUG):
			HashMap<String, List<Polygon>> = coroutineScope {

		if (this@MapHandler.map == null) {
			throw NullPointerException("Map has not been setup yet!")
		}

		val hashMap: HashMap<String, List<Polygon>> = HashMap()

		// Load the polygons file.
		this@MapHandler.parseKmlFile(fileRes).forEach { placemark ->

			val kmlPolygon: KmlPolygon = placemark.geometry as KmlPolygon

			val argb = this@MapHandler.getARGB(color)

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
				hashMap[name] = List(1) { polygon }
			} else {

				val list: MutableList<Polygon> = hashMap[name]!!.toMutableList()
				list.add(polygon)
				hashMap[name] = list.toList()
			}
		}

		return@coroutineScope hashMap
	}

	private fun loadPolylinesHeadlessAsync(jobDescription: String, @RawRes polylineResource: Int,
	                               @ColorRes color: Int, zIndex: Float, @DrawableRes icon: Int): Deferred<Int> {
		return this.activity.lifecycleScope.async(Dispatchers.IO, CoroutineStart.LAZY) {
			val tag = "loadPolylinesHeadless"
			Log.v(tag, "Starting ${jobDescription.lowercase(Locale.getDefault())}")
			try {
				this@MapHandler.loadPolylines(polylineResource, color, zIndex, icon)
				Log.v(tag, "Finished ${jobDescription.lowercase(Locale.getDefault())}")
			} catch (npe: NullPointerException) {
				Log.e(tag, "Cannot add polyline to map: Map not yet ready", npe)
			}
		}
	}

	fun loadPolygonsHeadlessAsync(jobDescription: String, @RawRes polygonResource: Int,
	                              @ColorRes color: Int, visible: Boolean = BuildConfig.DEBUG): Deferred<Int> {
		return this.activity.lifecycleScope.async(Dispatchers.IO, CoroutineStart.LAZY) {
			val tag = "loadPolygonsHeadless"
			Log.v(tag, "Starting ${jobDescription.lowercase(Locale.getDefault())}")
			this@MapHandler.loadPolygons(polygonResource, color, visible)
			Log.v(tag, "Finished ${jobDescription.lowercase(Locale.getDefault())}")
		}
	}

	fun getARGB(@ColorRes color: Int): Int {
		return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			this.activity.getColor(color)
		} else {
			ResourcesCompat.getColor(this.activity.resources, color, null)
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