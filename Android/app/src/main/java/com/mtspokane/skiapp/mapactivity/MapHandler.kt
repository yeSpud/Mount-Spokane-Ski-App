package com.mtspokane.skiapp.mapactivity

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.location.Location
import android.os.Build
import android.util.Log
import androidx.annotation.AnyThread
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.MainThread
import androidx.annotation.RawRes
import androidx.core.app.ActivityCompat
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.Polygon
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.RoundCap
import com.google.maps.android.data.kml.KmlLineString
import com.google.maps.android.data.kml.KmlPlacemark
import com.google.maps.android.data.kml.KmlPolygon
import com.google.maps.android.ktx.addMarker
import com.google.maps.android.ktx.addPolygon
import com.google.maps.android.ktx.addPolyline
import com.google.maps.android.ktx.utils.kml.kmlLayer
import com.mtspokane.skiapp.BuildConfig
import com.mtspokane.skiapp.R
import com.mtspokane.skiapp.debugview.DebugActivity
import com.mtspokane.skiapp.mapItem.MtSpokaneMapItems
import com.mtspokane.skiapp.mapItem.UIMapItem
import com.mtspokane.skiapp.mapItem.VisibleUIMapItem
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

class MapHandler(private var activity: MapsActivity?) : OnMapReadyCallback {

	private var map: GoogleMap? = null

	private var locationMarker: Marker? = null

	fun destroy() {
		Log.v("MapHandler", "destroy has been called!")
		if (this.locationMarker != null) {
			this.locationMarker!!.remove()
			this.locationMarker = null
		}
		this.map = null
		this.activity = null
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

		this.map = googleMap

		val tag = "onMapReady"

		if (BuildConfig.DEBUG) {
			this.map!!.setOnCameraIdleListener {
				val cameraPosition: CameraPosition = this.map!!.cameraPosition

				Log.v("OnCameraIdle", "Bearing: ${cameraPosition.bearing}")
				Log.v("OnCameraIdle", "Target: ${cameraPosition.target}")
				Log.v("OnCameraIdle", "Tilt: ${cameraPosition.tilt}")
				Log.v("OnCameraIdle", "Zoom: ${cameraPosition.zoom}")
			}
		}

		// Move the camera.
		val cameraPosition = CameraPosition.Builder()
			.target(LatLng(47.92517834073426, -117.10480503737926))
			.tilt(45F)
			.bearing(317.50552F)
			.zoom(14.414046F)
			.build()
		this.map!!.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
		this.map!!.setLatLngBoundsForCameraTarget(LatLngBounds(LatLng(47.912728,
			-117.133402), LatLng(47.943674, -117.092470)))
		this.map!!.setMaxZoomPreference(20F)
		this.map!!.setMinZoomPreference(13F)


		if (this.activity!!.locationEnabled) {
			this.map!!.setOnMapLongClickListener {
				Log.d("onMapLongClick", "Launching debug view")
				val intent = Intent(this.activity!!, DebugActivity::class.java)
				this.activity!!.startActivity(intent)
			}
		}

		// Set the map to use satellite view.
		this.map!!.mapType = GoogleMap.MAP_TYPE_SATELLITE

		this.activity!!.lifecycleScope.async(Dispatchers.IO, CoroutineStart.LAZY) {

			val polylineLoads = listOf(

				// Add the chairlifts to the map.
				// Load in the chairlift kml file, and iterate though each placemark.
				async(Dispatchers.IO) {
					Log.v(tag, "Started loading chairlift polylines")
					MtSpokaneMapItems.chairlifts = loadPolylines(this@MapHandler.map!!, R.raw.lifts,
						this@MapHandler.activity!!, R.color.chairlift, 4f, R.drawable.ic_chairlift)
					Log.v(tag, "Finished loading chairlift polylines")
				},

				// Load in the easy runs kml file, and iterate though each placemark.
				async(Dispatchers.IO) {
					Log.v(tag, "Started loading easy polylines")
					MtSpokaneMapItems.easyRuns = loadPolylines(this@MapHandler.map!!, R.raw.easy,
						this@MapHandler.activity!!, R.color.easy, 3f, R.drawable.ic_easy)
					Log.v(tag, "Finished loading easy run polylines")
				},

				// Load in the moderate runs kml file, and iterate though each placemark.
				async(Dispatchers.IO) {
					Log.v(tag, "Started loading moderate polylines")
					MtSpokaneMapItems.moderateRuns = loadPolylines(this@MapHandler.map!!, R.raw.moderate,
						this@MapHandler.activity!!, R.color.moderate, 2f, R.drawable.ic_moderate)
					Log.v(tag, "Finished loading moderate run polylines")
				},

				// Load in the difficult runs kml file, and iterate though each placemark.
				async(Dispatchers.IO) {
					Log.v(tag, "Started loading difficult polylines")
					MtSpokaneMapItems.difficultRuns = loadPolylines(this@MapHandler.map!!, R.raw.difficult,
						this@MapHandler.activity!!, R.color.difficult, 1f, R.drawable.ic_difficult)
					Log.v(tag, "Finished loading difficult polylines")
				}
			)

			// Wait for all the polylines to load before checking permissions.
			polylineLoads.awaitAll()
			MtSpokaneMapItems.isSetup = true

			// Request location permission, so that we can get the location of the device.
			// The result of the permission request is handled by a callback, onRequestPermissionsResult.
			// If this permission isn't granted then that's fine too.
			withContext(Dispatchers.Main) {
				Log.v("onMapReady", "Checking location permissions...")
				if (this@MapHandler.activity!!.locationEnabled) {
					this@MapHandler.setupLocation()
				} else {

					// Setup the location popup dialog.
					val alertDialogBuilder: AlertDialog.Builder =
						AlertDialog.Builder(this@MapHandler.activity!!)
					alertDialogBuilder.setTitle(R.string.alert_title)
					alertDialogBuilder.setMessage(R.string.alert_message)
					alertDialogBuilder.setPositiveButton(R.string.alert_ok) { _, _ ->
						ActivityCompat.requestPermissions(this@MapHandler.activity!!,
							arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), MapsActivity.permissionValue)
					}

					// Show the info popup about location.
					val locationDialog = alertDialogBuilder.create()
					locationDialog.show()
				}
			}
		}.start()
	}

	suspend fun setupLocation() = coroutineScope {

		val tag = "setupLocation"

		val polygonLoads = listOf(

			// Other polygons
			// (lodges, parking lots, vista house, tubing area, yurt, ski patrol building, and ski area bounds...)
			async(Dispatchers.IO) {
				Log.v(tag, "Started loading other polygons")

				var otherIndex = 0

				// Load the other polygons file.
				parseKmlFile(this@MapHandler.map!!, R.raw.other, this@MapHandler.activity!!).forEach {

					// Get the name of the other polygon.
					val name: String = getPlacemarkName(it)

					// Get the polygon from the file.
					val kmlPolygon: KmlPolygon = it.geometry as KmlPolygon

					// If the polygon is the ski area bounds then add it to the map as its own object.
					withContext(Dispatchers.Main) {
						if (name == "Ski Area Bounds") {
							Log.d(tag, "Adding bounds to map")

							val skiAreaBoundsPolygon: Polygon = addPolygonToMap(this@MapHandler.map!!,
								kmlPolygon.outerBoundaryCoordinates, 0.0F, Color.TRANSPARENT,
								Color.MAGENTA, 1.0F, BuildConfig.DEBUG)

							val skiAreaBoundsMapItem = UIMapItem("Ski Area Bounds", skiAreaBoundsPolygon)

							MtSpokaneMapItems.skiAreaBounds = skiAreaBoundsMapItem

						} else {

							// Load the other polygons as normal.

							val polygon: Polygon = addPolygonToMap(this@MapHandler.map!!, kmlPolygon
								.outerBoundaryCoordinates, 0.5F, R.color.other_polygon_fill,
								Color.MAGENTA, 8F, BuildConfig.DEBUG)

							val item = UIMapItem(name, polygon)

							val icon: Int? = when (name) {
								"Lodge 1" -> R.drawable.ic_missing // TODO Lodge icon
								"Lodge 2" -> R.drawable.ic_missing // TODO Lodge icon
								"Yurt" -> R.drawable.ic_yurt
								"Vista House" -> R.drawable.ic_missing // TODO Vista house icon
								"Ski Patrol" -> R.drawable.ic_ski_patrol_icon
								"Lodge 1 Parking Lot" -> R.drawable.ic_parking
								"Lodge 2 Parking Lot" -> R.drawable.ic_parking
								"Tubing Area" -> R.drawable.ic_missing // TODO Tubing area icon
								"Ski School" -> R.drawable.ic_missing // TODO Ski school icon
								else -> {
									Log.w(tag, "$name does not have an icon")
									null
								}
							}

							if (icon != null) {
								item.setIcon(icon)
							}

							MtSpokaneMapItems.other[otherIndex] = item
							otherIndex++

							Log.i(tag, "Added item: $name")
						}
					}
				}
				Log.v(tag, "Finished loading other polygons")
			},

			// Load the chairlift polygons file.
			async(Dispatchers.IO) {
				Log.v(tag, "Started loading chairlift polygons")
				loadPolygons(this@MapHandler.map!!, R.raw.lift_polygons, this@MapHandler.activity!!,
					R.color.chairlift_polygon, MtSpokaneMapItems.chairlifts)
				Log.v(tag, "Finished loading chairlift polygons")
			},

			// Load the easy polygons file.
			async(Dispatchers.IO) {
				Log.v(tag, "Started loading easy polygons")
				loadPolygons(this@MapHandler.map!!, R.raw.easy_polygons, this@MapHandler.activity!!,
					R.color.easy_polygon, MtSpokaneMapItems.easyRuns)
				Log.v(tag, "Finished loading easy polygons")
			},

			// Load the  moderate polygons file.
			async(Dispatchers.IO) {
				Log.v(tag, "Started loading moderate polygons")
				loadPolygons(this@MapHandler.map!!, R.raw.moderate_polygons, this@MapHandler.activity!!,
					R.color.moderate_polygon, MtSpokaneMapItems.moderateRuns)
				Log.v(tag, "Finished loading moderate polygons")
			},

			// Load the difficult polygons file.
			async(Dispatchers.IO) {
				Log.v(tag, "Started loading difficult polygons")
				loadPolygons(this@MapHandler.map!!, R.raw.difficult_polygons, this@MapHandler.activity!!,
					R.color.difficult_polygon, MtSpokaneMapItems.difficultRuns)
				Log.v(tag, "Finished loading difficult polygons")
			}
		)

		polygonLoads.awaitAll() // Wait for all loads to have finished...

		Log.v(tag, "Setting up location service...")
		this@MapHandler.activity!!.setupLocationService()
	}

	fun updateMarkerLocation(location: Location) {

		// If the marker hasn't been added to the map create a new one.
		if (this.locationMarker == null) {
			this.locationMarker = this.map!!.addMarker {
				position(LatLng(location.latitude, location.longitude))
				title(this@MapHandler.activity!!.resources.getString(R.string.your_location))
			}
		} else {

			// Otherwise just update the LatLng location.
			this.locationMarker!!.position = LatLng(location.latitude, location.longitude)
		}

	}

	companion object {

		fun parseKmlFile(map: GoogleMap, @RawRes file: Int, activity: FragmentActivity):
				Iterable<KmlPlacemark> {
			val kml = kmlLayer(map, file, activity)
			return kml.placemarks
		}

		@AnyThread
		suspend fun loadPolylines(map: GoogleMap, @RawRes fileRes: Int, activity: FragmentActivity,
			@ColorRes color: Int, zIndex: Float, @DrawableRes icon: Int? = null):
				Array<VisibleUIMapItem> = coroutineScope {

			val hashMap: HashMap<String, VisibleUIMapItem> = HashMap()

			// Load the polyline from the file, and iterate though each placemark.
			parseKmlFile(map, fileRes, activity).forEach {

				// Get the name of the polyline.
				val name: String = getPlacemarkName(it)

				// Get the LatLng coordinates of the placemark.
				val lineString: KmlLineString = it.geometry as KmlLineString
				val coordinates: ArrayList<LatLng> = lineString.geometryObject

				// Get the color of the polyline.
				val argb = getARGB(activity, color)

				// Create the polyline using the coordinates and other options.
				var polyline: Polyline
				withContext(Dispatchers.Main) {
					polyline = map.addPolyline {
						addAll(coordinates)
						color(argb)
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

					// Check if this is a night item. Its a night item if the property contains a description.
					val night = it.hasProperty("description")

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

		private fun getARGB(activity: FragmentActivity, @ColorRes color: Int): Int {
			return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
				activity.getColor(color)
			} else {
				ResourcesCompat.getColor(activity.resources, color, null)
			}
		}

		fun getPlacemarkName(placemark: KmlPlacemark): String {

			return if (placemark.hasProperty("name")) {
				placemark.getProperty("name")
			} else {

				// If the name wasn't found in the properties return an empty string.
				Log.w("getPlacemarkName", "Placemark is missing name!")
				""
			}
		}

		@AnyThread
		suspend fun loadPolygons(map: GoogleMap, @RawRes fileRes: Int, activity: FragmentActivity,
			@ColorRes color: Int, visibleUIMapItems: Array<VisibleUIMapItem>, visible: Boolean = BuildConfig.DEBUG) = coroutineScope { // TODO Optimize this

			val hashMap: HashMap<String, ArrayList<Polygon>> = HashMap()

			// Load the polygons file.
			parseKmlFile(map, fileRes, activity).forEach {

				val kmlPolygon: KmlPolygon = it.geometry as KmlPolygon

				val argb = getARGB(activity, color)

				val polygon: Polygon
				withContext(Dispatchers.Main) {
					polygon = addPolygonToMap(map, kmlPolygon.outerBoundaryCoordinates, 0.5F,
						argb, argb, 8F, visible)
				}

				val name: String = getPlacemarkName(it)

				if (hashMap[name] == null) {
					val arrayList: ArrayList<Polygon> = ArrayList(1)
					arrayList.add(polygon)
					hashMap[name] = arrayList
				} else {
					hashMap[name]!!.add(polygon)
				}
			}

			visibleUIMapItems.forEach { visibleMapItem ->
				if (hashMap[visibleMapItem.name] != null) {
					withContext(Dispatchers.Main) {
						hashMap[visibleMapItem.name]!!.forEach { polygon ->
							visibleMapItem.addAdditionalPolygon(polygon)
						}
					}
				} else {
					Log.w("loadPolygons", "Visible map item ${visibleMapItem.name} is not in hashmap!")
				}
			}
		}

		@MainThread
		suspend fun addPolygonToMap(map: GoogleMap, points: Iterable<LatLng>, zIndex: Float,
			fillColor: Int, strokeColor: Int, strokeWidth: Float, visible: Boolean): Polygon = coroutineScope {
			return@coroutineScope map.addPolygon {
				addAll(points)
				clickable(false)
				geodesic(true)
				zIndex(zIndex)
				fillColor(fillColor)
				strokeColor(strokeColor)
				strokeWidth(strokeWidth)
				visible(visible)
			}
		}
	}
}