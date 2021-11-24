package com.mtspokane.skiapp

import android.location.Location
import android.location.LocationListener
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.maps.android.PolyUtil
import com.google.maps.android.ktx.addMarker
import kotlinx.coroutines.*

class InAppSkierLocation(private val mapHandler: MapHandler) : LocationListener {

	private var locationMarker: Marker? = null

	override fun onLocationChanged(location: Location) {

		// If we are not on the mountain return early.
		if (!PolyUtil.containsLocation(location.latitude, location.longitude, this.mapHandler
				.skiAreaBounds.points, true)) {
			return
		}

		// If the marker hasn't been added to the map create a new one.
		if (this.locationMarker == null) {
			this.locationMarker = this.mapHandler.map.addMarker {
				position(LatLng(location.latitude, location.longitude))
				title(this@InAppSkierLocation.mapHandler.activity.resources.getString(R.string.your_location))
			}
		} else {

			// Otherwise just update the LatLng location.
			this.locationMarker!!.position = LatLng(location.latitude, location.longitude)
		}

		// Check if our skier is on a run, chairlift, or other.
		this.mapHandler.activity.lifecycleScope.launch(Dispatchers.Main, CoroutineStart.LAZY) {

			when {
				@Suppress("UNCHECKED_CAST")
				Locations.checkIfOnOther(location, this@InAppSkierLocation.mapHandler.other
						as Array<MapItem>) -> this@InAppSkierLocation.mapHandler.activity.actionBar!!
					.title = mapHandler.activity.getString(R.string.current_other, Locations.otherName)
				Locations.checkIfOnChairlift(location, this@InAppSkierLocation.mapHandler.chairlifts
					.values.toTypedArray()) -> this@InAppSkierLocation.mapHandler.activity
					.actionBar!!.title = this@InAppSkierLocation.mapHandler.activity
					.getString(R.string.current_chairlift, Locations.chairliftName)
				Locations.checkIfOnRun(location, this@InAppSkierLocation.mapHandler) -> this@InAppSkierLocation
					.mapHandler.activity.actionBar!!.title = this@InAppSkierLocation.mapHandler
					.activity.getString(R.string.current_run, Locations.currentRun)
				else -> this@InAppSkierLocation.mapHandler.activity.actionBar!!.title = this@InAppSkierLocation
					.mapHandler.activity.getString(R.string.app_name)
			}
		}.start()
	}

	override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
		// ignore
	}
}