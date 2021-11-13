package com.mtspokane.skiapp

import android.location.Location
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope


object Locations {

	var currentRun: String = ""
	private set

	fun isOnMountain(): Boolean {
		// TODO
		return true
	}

	suspend fun checkIfOnOther(): Boolean = coroutineScope {
		// TODO
		return@coroutineScope false
	}

	suspend fun checkIfOnChairlift(): Boolean = coroutineScope {
		// TODO
		return@coroutineScope false
	}

	suspend fun checkIfOnRun(location: Location, mapHandler: MapHandler): Boolean = coroutineScope {

		var easyRunName: String? = null
		var moderateRunName: String? = null
		var difficultRunName: String? = null

		val nameJobs = listOf(
			async(Dispatchers.Main) {
				easyRunName = getRunNameFromPoint(location, mapHandler.easyRuns.values)
				Log.d("checkIfOnRun", "Finished checking names for easy runs") },
			async(Dispatchers.Main) {
				moderateRunName = getRunNameFromPoint(location, mapHandler.moderateRuns.values)
				Log.d("checkIfOnRun", "Finished checking names for moderate runs") },
			async(Dispatchers.Main) {
				difficultRunName = getRunNameFromPoint(location, mapHandler.difficultRuns.values)
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
			if (runName == this@Locations.currentRun) {
				Log.d("checkIfOnRun", "Still on $runName")
			} else {
				Log.i("checkIfOnRun", "On run: $runName")
				this@Locations.currentRun = runName

			}
			return@coroutineScope true
		} else {
			Log.i("checkIfOnRun", "Unable to determine run")
			return@coroutineScope false
		}
	}

	private fun getRunNameFromPoint(location: Location, items: Collection<MapItem>): String? {
		for (mapItem: MapItem in items) {
			if (mapItem.pointInsidePolygon(location)) {
				return mapItem.name
			}
		}
		return null
	}

}