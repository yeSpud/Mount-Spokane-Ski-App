package com.mtspokane.skiapp

import android.location.Location
import android.location.LocationListener
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.coroutines.*

class InAppSkierLocation(private val mapHandler: MapHandler) : LocationListener {

	private var locationMarker: Marker? = null

	override fun onLocationChanged(location: Location) {

		// If we are not on the mountain return early.
		if (!Locations.isOnMountain(location, this.mapHandler.skiAreaBounds)) {
			return
		}

		// If the marker hasn't been added to the map create a new one.
		if (this.locationMarker == null) {
			this.locationMarker = this.mapHandler.map.addMarker(MarkerOptions()
				.position(LatLng(location.latitude, location.longitude))
				.title(this.mapHandler.activity.resources.getString(R.string.your_location)))
		} else {

			// Otherwise just update the LatLng location.
			this.locationMarker!!.position = LatLng(location.latitude, location.longitude)
		}

		// Check if our skier is on a run, chairlift, or other.
		this.mapHandler.activity.lifecycleScope.launch(Dispatchers.Main, CoroutineStart.LAZY) {

			when {
				Locations.checkIfOnOther(location) -> {
					// TODO On other
				}
				Locations.checkIfOnChairlift(location) -> {
					// TODO On chairlift
				}
				Locations.checkIfOnRun(location, this@InAppSkierLocation.mapHandler) -> {
					this@InAppSkierLocation.mapHandler.activity.actionBar!!.title = mapHandler.activity.
					getString(R.string.current_run, Locations.currentRun)
				}
				else -> {
					this@InAppSkierLocation.mapHandler.activity.actionBar!!.title = this@InAppSkierLocation.
					mapHandler.activity.getString(R.string.app_name)
				}
			}
		}.start()
	}

	override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
		// ignore
	}
}