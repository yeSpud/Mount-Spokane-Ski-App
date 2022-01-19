package com.mtspokane.skiapp.maphandlers

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.location.Location
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.Polygon
import com.google.maps.android.ktx.addMarker
import com.mtspokane.skiapp.R
import com.mtspokane.skiapp.activities.DebugActivity
import com.mtspokane.skiapp.mapItem.MtSpokaneMapItems
import com.mtspokane.skiapp.mapItem.UIMapItem
import com.mtspokane.skiapp.mapItem.VisibleUIMapItem
import com.mtspokane.skiapp.activities.MapsActivity
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

class MainMap(activity: MapsActivity) : MapHandler(activity, CameraPosition.Builder()
	.target(LatLng(47.92517834073426, -117.10480503737926)).tilt(45F)
	.bearing(317.50552F).zoom(14.414046F).build()) {

	private var locationMarker: Marker? = null

	override fun destroy() {

		if (this.locationMarker != null) {
			Log.v("MainMap", "Removing location marker")
			this.locationMarker!!.remove()
			this.locationMarker = null
		}

		super.destroy()
	}

	fun updateMarkerLocation(location: Location) {

		if (this.map != null) {

			// If the marker hasn't been added to the map create a new one.
			if (this.locationMarker == null) {
				this.locationMarker = this.map!!.addMarker {
					position(LatLng(location.latitude, location.longitude))
					title(this@MainMap.activity.resources.getString(R.string.your_location))
				}
			} else {

				// Otherwise just update the LatLng location.
				this.locationMarker!!.position = LatLng(location.latitude, location.longitude)
			}
		}
	}

	suspend fun setupLocation() = coroutineScope {

		val tag = "setupLocation"

		val polygonLoads = listOf(

			// Other polygons
			// (lodges, parking lots, vista house, tubing area, yurt, ski patrol building, and ski area bounds...)
			async(Dispatchers.IO) {
				Log.v(tag, "Started loading other polygons")

				val hashmap: HashMap<String, Array<Polygon>> = loadPolygons(R.raw.other,
					R.color.other_polygon_fill, false)

				val skiAreaBoundsKeyName = "Ski Area Bounds"
				val skiAreaBounds = hashmap[skiAreaBoundsKeyName]

				withContext(Dispatchers.Main) {
					if (skiAreaBounds != null) {

						MtSpokaneMapItems.skiAreaBounds = UIMapItem(skiAreaBoundsKeyName, skiAreaBounds[0])
						hashmap.remove(skiAreaBoundsKeyName)
					}

					val names: Array<String> = hashmap.keys.toTypedArray()
					MtSpokaneMapItems.other = Array(9) {

						val uiMapItemPolygons: Array<Polygon>? = hashmap[names[it]]

						val icon: Int? = when (names[it]) {
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
								Log.w(tag, "${names[it]} does not have an icon")
								null
							}
						}

						if (uiMapItemPolygons != null) {
							UIMapItem(names[it], uiMapItemPolygons[0], icon)
						} else {
							Log.w(tag, "No polygon for ${names[it]}")
							UIMapItem(names[it])
						}
					}
				}

				Log.v(tag, "Finished loading other polygons")
			},

			// Load the chairlift terminals.
			async(Dispatchers.IO) {
				Log.v(tag, "Started loading chairlift terminal polylines")
				val hashmap: HashMap<String, Array<Polygon>> = loadPolygons(R.raw.lift_terminal_polygons,
					R.color.chairlift_polygon)

				val names: Array<String> = hashmap.keys.toTypedArray()
				withContext(Dispatchers.Main) {
					MtSpokaneMapItems.chairliftTerminals = Array(6) {

						val polygonArray: Array<Polygon> = hashmap[names[it]]!!

						val uiMapItem = UIMapItem(names[it], polygonArray[0], R.drawable.ic_chairlift)
						uiMapItem.addAdditionalPolygon(polygonArray[1])
						uiMapItem
					}
				}

				Log.v(tag, "Finished loading chairlift terminal polylines")
			},

			// Load the chairlift polygons file.
			async(Dispatchers.IO) {
				Log.v(tag, "Started loading chairlift polygons")
				val hashmap: HashMap<String, Array<Polygon>> = loadPolygons(R.raw.lift_polygons,
					R.color.chairlift_polygon)

				withContext(Dispatchers.Main) {
					MtSpokaneMapItems.chairlifts.forEach { visibleMapItem: VisibleUIMapItem ->

						val polygons: Array<Polygon>? = hashmap[visibleMapItem.name]
						polygons?.forEach {
							visibleMapItem.addAdditionalPolygon(it)
						}
					}
				}

				Log.v(tag, "Finished loading chairlift polygons")
			},

			// Load the easy polygons file.
			async(Dispatchers.IO) {
				Log.v(tag, "Started loading easy polygons")

				val hashmap: HashMap<String, Array<Polygon>> = loadPolygons(R.raw.easy_polygons,
					R.color.easy_polygon)

				withContext(Dispatchers.Main) {
					MtSpokaneMapItems.easyRuns.forEach { visibleUIMapItem: VisibleUIMapItem ->

						val polygons: Array<Polygon>? = hashmap[visibleUIMapItem.name]
						polygons?.forEach {
							visibleUIMapItem.addAdditionalPolygon(it)
						}
					}
				}

				Log.v(tag, "Finished loading easy polygons")
			},

			// Load the moderate polygons file.
			async(Dispatchers.IO) {
				Log.v(tag, "Started loading moderate polygons")
				val hashmap: HashMap<String, Array<Polygon>> = loadPolygons(R.raw.moderate_polygons,
					R.color.moderate_polygon)

				withContext(Dispatchers.Main) {
					MtSpokaneMapItems.moderateRuns.forEach { visibleUIMapItem: VisibleUIMapItem ->

						val polygons: Array<Polygon>? = hashmap[visibleUIMapItem.name]
						polygons?.forEach {
							visibleUIMapItem.addAdditionalPolygon(it)
						}
					}
				}

				Log.v(tag, "Finished loading moderate polygons")
			},

			// Load the difficult polygons file.
			async(Dispatchers.IO) {
				Log.v(tag, "Started loading difficult polygons")
				val hashmap: HashMap<String, Array<Polygon>> = loadPolygons(R.raw.difficult_polygons,
					R.color.difficult_polygon)

				withContext(Dispatchers.Main) {
					MtSpokaneMapItems.difficultRuns.forEach { visibleUIMapItem: VisibleUIMapItem ->

						val polygons: Array<Polygon>? = hashmap[visibleUIMapItem.name]
						polygons?.forEach {
							visibleUIMapItem.addAdditionalPolygon(it)
						}
					}
				}

				Log.v(tag, "Finished loading difficult polygons")
			}
		)

		polygonLoads.awaitAll() // Wait for all loads to have finished...

		Log.v(tag, "Setting up location service...")
		(this@MainMap.activity as MapsActivity).setupLocationService()
	}

	init {

		this.setAdditionalCallback {

			val tag = "MainMap"

			if ((this@MainMap.activity as MapsActivity).locationEnabled) {
				it.setOnMapLongClickListener {
					Log.d(tag, "Launching debug view")
					val intent = Intent(this.activity, DebugActivity::class.java)
					this.activity.startActivity(intent)
				}
			}

			this.activity.lifecycleScope.async(Dispatchers.IO, CoroutineStart.LAZY) {

				val polylineLoads = listOf(

					// Add the chairlifts to the map.
					// Load in the chairlift kml file, and iterate though each placemark.
					async(Dispatchers.IO) {
						Log.v(tag, "Started loading chairlift polylines")
						MtSpokaneMapItems.chairlifts = loadPolylines(R.raw.lifts, R.color.chairlift,
							4f, R.drawable.ic_chairlift)
						Log.v(tag, "Finished loading chairlift polylines")
					},

					// Load in the easy runs kml file, and iterate though each placemark.
					async(Dispatchers.IO) {
						Log.v(tag, "Started loading easy polylines")
						MtSpokaneMapItems.easyRuns = loadPolylines(R.raw.easy, R.color.easy,
							3f, R.drawable.ic_easy)
						Log.v(tag, "Finished loading easy run polylines")
					},

					// Load in the moderate runs kml file, and iterate though each placemark.
					async(Dispatchers.IO) {
						Log.v(tag, "Started loading moderate polylines")
						MtSpokaneMapItems.moderateRuns = loadPolylines(R.raw.moderate, R.color.moderate,
							2f, R.drawable.ic_moderate)
						Log.v(tag, "Finished loading moderate run polylines")
					},

					// Load in the difficult runs kml file, and iterate though each placemark.
					async(Dispatchers.IO) {
						Log.v(tag, "Started loading difficult polylines")
						MtSpokaneMapItems.difficultRuns = loadPolylines(R.raw.difficult, R.color.difficult,
							1f, R.drawable.ic_difficult)
						Log.v(tag, "Finished loading difficult polylines")
					}
				)

				// Wait for all the polylines to load before checking permissions.
				polylineLoads.awaitAll()
				MtSpokaneMapItems.isSetup = true
				Log.i(tag, "Finished setting up map items")

				// Request location permission, so that we can get the location of the device.
				// The result of the permission request is handled by a callback, onRequestPermissionsResult.
				// If this permission isn't granted then that's fine too.
				withContext(Dispatchers.Main) {
					Log.v("onMapReady", "Checking location permissions...")
					if (this@MainMap.activity.locationEnabled) {
						this@MainMap.setupLocation()
					} else {

						// Setup the location popup dialog.
						val alertDialogBuilder: AlertDialog.Builder = AlertDialog.Builder(this@MainMap.activity)
						alertDialogBuilder.setTitle(R.string.alert_title)
						alertDialogBuilder.setMessage(R.string.alert_message)
						alertDialogBuilder.setPositiveButton(R.string.alert_ok) { _, _ ->
							ActivityCompat.requestPermissions(this@MainMap.activity,
								arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), MapsActivity.permissionValue)
						}

						// Show the info popup about location.
						val locationDialog = alertDialogBuilder.create()
						locationDialog.show()
					}
				}
			}.start()
		}
	}
}