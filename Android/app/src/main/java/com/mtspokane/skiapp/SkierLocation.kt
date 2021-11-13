package com.mtspokane.skiapp

import android.location.Location
import android.location.LocationListener
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.coroutines.*

class SkierLocation(private val mapHandler: MapHandler) : LocationListener {

	private var locationMarker: Marker? = null

	private var lastKnownRun: String = ""

	override fun onLocationChanged(location: Location) {

		// If the marker hasn't been added to the map create a new one.
		if (this.locationMarker == null) {
			this.locationMarker = this.mapHandler.map.addMarker(MarkerOptions()
				.position(LatLng(location.latitude, location.longitude))
				.title(this.mapHandler.activity.resources.getString(R.string.your_location)))
		} else {

			// Otherwise just update the LatLng location.
			this.locationMarker!!.position = LatLng(location.latitude, location.longitude)
		}

		// Check if our skier is on a run.
		this.mapHandler.activity.lifecycleScope.launch(Dispatchers.Main, CoroutineStart.LAZY) {
			checkIfOnRun(location)
		}.start()
	}

	private suspend fun checkIfOnRun(location: Location): Boolean = coroutineScope {

		var easyRunName: String? = null
		var moderateRunName: String? = null
		var difficultRunName: String? = null

		val nameJobs = listOf(
			async(Dispatchers.Main) {
				easyRunName = getRunNameFromPoint(location, this@SkierLocation.mapHandler.easyRuns.values)
				Log.d("checkIfOnRun", "Finished checking names for easy runs") },
			async(Dispatchers.Main) { moderateRunName = getRunNameFromPoint(location, this@SkierLocation.mapHandler.moderateRuns.values)
				Log.d("checkIfOnRun", "Finished checking names for moderate runs") },
			async(Dispatchers.Main) { difficultRunName = getRunNameFromPoint(location, this@SkierLocation.mapHandler.difficultRuns.values)
				Log.d("checkIfOnRun", "Finished checking names for difficult runs") }
		)

		nameJobs.awaitAll()

		var runName = ""
		when {
			easyRunName != null -> runName = easyRunName!!
			moderateRunName != null -> runName = moderateRunName!!
			difficultRunName != null -> runName = difficultRunName!!
		}

		// TODO
		if (runName != "") {
			if (runName == this@SkierLocation.lastKnownRun) {
				Log.d("checkIfOnRun", "Still on $runName")
			} else {
				Log.i("checkIfOnRun", "On run: $runName")
				this@SkierLocation.lastKnownRun = runName
				this@SkierLocation.mapHandler.activity.actionBar!!.title = this@SkierLocation.
				mapHandler.activity.getString(R.string.current_run, runName)
			}
			return@coroutineScope true
		} else {
			Log.i("checkIfOnRun", "Unable to determine run")
			this@SkierLocation.mapHandler.activity.actionBar!!.title = this@SkierLocation.
			mapHandler.activity.getString(R.string.app_name)
			return@coroutineScope false
		}
	}

	override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
		// ignore
	}

	companion object {

		fun getRunNameFromPoint(location: Location, items: Collection<MapItem>): String? {
			for (mapItem: MapItem in items) {
				if (mapItem.pointInsidePolygon(location)) {
					return mapItem.name
				}
			}
			return null
		}
	}
}