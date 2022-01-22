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
import com.google.maps.android.ktx.addMarker
import com.mtspokane.skiapp.R
import com.mtspokane.skiapp.activities.DebugActivity
import com.mtspokane.skiapp.mapItem.MtSpokaneMapItems
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
			MtSpokaneMapItems.initializeOtherAsync(this@MainMap.activity::class, this@MainMap),

			// Load the chairlift terminals.
			MtSpokaneMapItems.initializeChairliftTerminalsAsync(this@MainMap.activity::class, this@MainMap),

			// Load the chairlift polygons file.
			MtSpokaneMapItems.addChairliftPolygonsAsync(this@MainMap.activity::class, this@MainMap),

			// Load the easy polygons file.
			MtSpokaneMapItems.addEasyPolygonsAsync(this@MainMap.activity::class, this@MainMap),

			// Load the moderate polygons file.
			MtSpokaneMapItems.addModeratePolygonsAsync(this@MainMap.activity::class, this@MainMap),

			// Load the difficult polygons file.
			MtSpokaneMapItems.addDifficultPolygonsAsync(this@MainMap.activity::class, this@MainMap)
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

					// Load in the chairlift kml file, and iterate though each placemark.
					MtSpokaneMapItems.initializeChairliftsAsync(this@MainMap.activity::class, this@MainMap),

					// Load in the easy runs kml file, and iterate though each placemark.
					MtSpokaneMapItems.initializeEasyRunsAsync(this@MainMap.activity::class, this@MainMap),

					// Load in the moderate runs kml file, and iterate though each placemark.
					MtSpokaneMapItems.initializeModerateRunsAsync(this@MainMap.activity::class, this@MainMap),

					// Load in the difficult runs kml file, and iterate though each placemark.
					MtSpokaneMapItems.initializeDifficultRunsAsync(this@MainMap.activity::class, this@MainMap)
				)

				// Wait for all the polylines to load before checking permissions.
				polylineLoads.awaitAll()
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