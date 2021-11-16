package com.mtspokane.skiapp

import android.location.Location
import android.util.Log
import androidx.annotation.MainThread
import com.google.android.gms.maps.model.Polygon
import com.google.maps.android.PolyUtil
import kotlinx.coroutines.*


object Locations {

	var currentRun: String = ""
	private set

	var chairliftName: String = ""
	private set

	var otherName: String = ""
	private set

	fun isOnMountain(location: Location, skiAreaBounds: Polygon): Boolean {
		return PolyUtil.containsLocation(location.latitude, location.longitude, skiAreaBounds.points, true)
	}

	@MainThread
	suspend fun checkIfOnOther(location: Location, otherItem: Array<MapItem>): Boolean = coroutineScope {

		otherItem.forEach {
			if (it.pointInsidePolygon(location)) {
				this@Locations.otherName = it.name
				return@coroutineScope true
			}
		}

		return@coroutineScope false
	}

	suspend fun checkIfOnChairlift(location: Location): Boolean = coroutineScope {
		// TODO
		// Remember to check change in altitude (ascending vs descending)
		return@coroutineScope false
	}

	suspend fun checkIfOnRun(location: Location, mapHandler: MapHandler): Boolean = coroutineScope {

		var easyRunName: String? = null
		var moderateRunName: String? = null
		var difficultRunName: String? = null

		val nameJobs = listOf(
			async(Dispatchers.Main, CoroutineStart.LAZY) {
				easyRunName = getRunNameFromPoint(location, mapHandler.easyRuns.values)
				Log.v("checkIfOnRun", "Finished checking names for easy runs") },
			async(Dispatchers.Main, CoroutineStart.LAZY) {
				moderateRunName = getRunNameFromPoint(location, mapHandler.moderateRuns.values)
				Log.v("checkIfOnRun", "Finished checking names for moderate runs") },
			async(Dispatchers.Main, CoroutineStart.LAZY) {
				difficultRunName = getRunNameFromPoint(location, mapHandler.difficultRuns.values)
				Log.v("checkIfOnRun", "Finished checking names for difficult runs") }
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
			if (runName == this@Locations.currentRun) {
				Log.d("checkIfOnRun", "Still on $runName")
			} else {
				Log.d("checkIfOnRun", "On run: $runName")
				this@Locations.currentRun = runName

			}
			return@coroutineScope true
		} else {
			Log.i("checkIfOnRun", "Unable to determine run")
			return@coroutineScope false
		}
	}

	@MainThread
	private fun getRunNameFromPoint(location: Location, items: Collection<MapItem>): String? {
		for (mapItem: MapItem in items) {
			if (mapItem.pointInsidePolygon(location)) {
				return mapItem.name
			}
		}
		return null
	}

}