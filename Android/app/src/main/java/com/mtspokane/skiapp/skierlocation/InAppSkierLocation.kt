package com.mtspokane.skiapp.skierlocation

import android.content.Intent
import android.location.Location
import android.location.LocationListener
import android.os.Build
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.maps.android.ktx.addMarker
import com.mtspokane.skiapp.Locations
import com.mtspokane.skiapp.R
import com.mtspokane.skiapp.mapItem.MtSpokaneMapItems
import com.mtspokane.skiapp.mapactivity.MapHandler
import com.mtspokane.skiapp.mapactivity.MapsActivity
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async

class InAppSkierLocation(private var mapHandler: MapHandler?, private var activity: MapsActivity?) :
	LocationListener {

	private var locationMarker: Marker? = null

	fun destroy() {
		this.locationMarker = null
		this.mapHandler = null
		this.activity = null
	}

	override fun onLocationChanged(location: Location) {

		// If we are not on the mountain return early.
		if (MtSpokaneMapItems.skiAreaBounds == null) {
			return
		} else if (!MtSpokaneMapItems.skiAreaBounds!!.locationInsidePolygons(location)) {
			return
		}

		// If the marker hasn't been added to the map create a new one.
		if (this.locationMarker == null) {
			this.locationMarker = this.mapHandler!!.map!!.addMarker {
				position(LatLng(location.latitude, location.longitude))
				title(this@InAppSkierLocation.activity!!.resources.getString(R.string.your_location))
			}
		} else {

			// Otherwise just update the LatLng location.
			this.locationMarker!!.position = LatLng(location.latitude, location.longitude)
		}

		// Check if the location service has already been started.
		if (!SkierLocationService.checkIfRunning(this.activity!!)) {

			val serviceIntent = Intent(this.activity!!, SkierLocationService::class.java)

			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
				this.activity!!.startForegroundService(serviceIntent)
			} else {
				this.activity!!.startService(serviceIntent)
			}
		}

		// Check if our skier is on a run, chairlift, or other.
		this.activity!!.lifecycleScope.async(Dispatchers.IO, CoroutineStart.LAZY) {

			val other = Locations.checkIfOnOtherAsync(location)
			if (other != null) {
				this@InAppSkierLocation.activity!!.actionBar!!.title = this@InAppSkierLocation.activity!!.getString(R.string.current_other, other.name)
				return@async
			}

			val chairlift = Locations.checkIfOnChairliftAsync(location)
			if (chairlift != null) {
				this@InAppSkierLocation.activity!!.actionBar!!.title = this@InAppSkierLocation.activity!!.getString(R.string.current_chairlift, chairlift.name)
				return@async
			}

			val run = Locations.checkIfOnRunAsync(location)
			if (run != null) {
				this@InAppSkierLocation.activity!!.actionBar!!.title = this@InAppSkierLocation.activity!!.getString(R.string.current_run, run.name)
				return@async
			}

			this@InAppSkierLocation.activity!!.actionBar!!.title = this@InAppSkierLocation.activity!!.getString(R.string.app_name)
		}.start()
	}

	override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
		// ignore
	}
}