package com.mtspokane.skiapp.maphandlers

import android.Manifest
import android.app.AlertDialog
import android.location.Location
import android.util.Log
import android.widget.BaseAdapter
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.maps.android.ktx.addMarker
import com.mtspokane.skiapp.R
import com.mtspokane.skiapp.mapItem.MtSpokaneMapItems
import com.mtspokane.skiapp.activities.mainactivity.MapsActivity
import com.orhanobut.dialogplus.OnItemClickListener
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch

class MainMap(activity: MapsActivity) : MapHandler(activity, CameraPosition.Builder()
		.target(LatLng(47.92517834073426, -117.10480503737926)).tilt(45F)
		.bearing(317.50552F).zoom(14.414046F).build()) {

	private var locationMarker: Marker? = null

	override val additionalCallback: OnMapReadyCallback = OnMapReadyCallback {

		// Request location permission, so that we can get the location of the device.
		// The result of the permission request is handled by a callback, onRequestPermissionsResult.
		// If this permission isn't granted then that's fine too.
		Log.v("onMapReady", "Checking location permissions...")
		if ((this@MainMap.activity as MapsActivity).locationEnabled) {
			Log.v("onMapReady", "Location tracking enabled")
			this.setupLocation()
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

	override val mapOptionItemClickListener: OnItemClickListener = OnItemClickListener { dialog, item, view, position -> {
			// TODO
		}
	}

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

	fun setupLocation() {

		val tag = "setupLocation"

		this.activity.lifecycleScope.launch(Dispatchers.IO, CoroutineStart.LAZY) {

			val polygonLoads = listOf(

					// Other polygons
					MtSpokaneMapItems.initializeOtherPolygonsAsync(this@MainMap.activity::class,
							this@MainMap),

					// Load the chairlift terminals.
					MtSpokaneMapItems.addChairliftTerminalPolygonsAsync(this@MainMap.activity::class,
							this@MainMap),

					// Load the chairlift polygons file.
					MtSpokaneMapItems.addChairliftPolygonsAsync(this@MainMap.activity::class,
							this@MainMap),

					// Load the easy polygons file.
					MtSpokaneMapItems.addEasyPolygonsAsync(this@MainMap.activity::class,
							this@MainMap),

					// Load the moderate polygons file.
					MtSpokaneMapItems.addModeratePolygonsAsync(this@MainMap.activity::class,
							this@MainMap),

					// Load the difficult polygons file.
					MtSpokaneMapItems.addDifficultPolygonsAsync(this@MainMap.activity::class,
							this@MainMap)
			)

			polygonLoads.awaitAll() // Wait for all loads to have finished...

		}.start()

		Log.v(tag, "Setting up location service...")
		(this@MainMap.activity as MapsActivity).setupLocationService()
	}
}