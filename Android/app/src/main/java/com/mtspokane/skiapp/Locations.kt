package com.mtspokane.skiapp

import android.location.Location
import android.os.Build
import android.util.Log
import androidx.annotation.AnyThread
import androidx.annotation.MainThread
import com.mtspokane.skiapp.mapItem.MapItem
import com.mtspokane.skiapp.mapItem.UIMapItem
import kotlinx.coroutines.*


object Locations {

	var currentRun: String = ""
	private set

	var chairliftName: String = ""
	private set

	var otherName: String = ""
	private set

	private var previousLocation: Location? = null

	private var vDirection: VerticalDirection = VerticalDirection.UNKNOWN

	private val canUseAccuracy = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O

	private fun updateLocationVDirection(currentLocation: Location) {

		if (this.previousLocation != null) {
			if (currentLocation.altitude != 0.0 && this.previousLocation!!.altitude != 0.0) {
				if (this.canUseAccuracy) {
					when {
						(currentLocation.altitude - currentLocation.verticalAccuracyMeters) > (this.previousLocation!!.altitude + this.previousLocation!!.verticalAccuracyMeters) -> this.vDirection = VerticalDirection.UP_CERTAIN
						(currentLocation.altitude + currentLocation.verticalAccuracyMeters) < (this.previousLocation!!.altitude - this.previousLocation!!.verticalAccuracyMeters) -> this.vDirection = VerticalDirection.DOWN_CERTAIN
						currentLocation.altitude > this.previousLocation!!.altitude -> this.vDirection = VerticalDirection.UP
						currentLocation.altitude < this.previousLocation!!.altitude -> this.vDirection = VerticalDirection.DOWN
						else -> this.vDirection = VerticalDirection.FLAT
					}
				} else {
					Log.w("updateLocationDirection", "Device does not support altitude accuracy!")
					when {
						currentLocation.altitude > this.previousLocation!!.altitude -> this.vDirection = VerticalDirection.UP
						currentLocation.altitude < this.previousLocation!!.altitude -> this.vDirection = VerticalDirection.DOWN
						else -> this.vDirection = VerticalDirection.FLAT
					}
				}
			} else {
				this.vDirection = VerticalDirection.UNKNOWN
			}
		} else {
			this.vDirection = VerticalDirection.UNKNOWN
		}

		this.previousLocation = currentLocation
	}

	@AnyThread
	suspend fun checkIfOnOtherAsync(location: Location, otherItem: Array<UIMapItem>): Boolean = coroutineScope {

		withContext(Dispatchers.Main) {
			otherItem.forEach {
				if (it.locationInsidePolygons(location)) {
					this@Locations.otherName = it.name
					return@withContext true
				}
			}

			return@withContext false
		}
	}

	fun checkIfOnOther(location: Location, otherItem: Array<MapItem>): Boolean {

		otherItem.forEach {
			if (it.locationInsidePoints(location)) {
				this.otherName = it.name
				return true
			}
		}

		return false
	}

	@AnyThread
	suspend fun checkIfOnChairliftAsync(location: Location, chairlifts: Array<UIMapItem>): Boolean = coroutineScope {

		val numberOfChecks = 6
		val minimumConfidenceValue: Double = 4.0/numberOfChecks
		var currentConfidence = 0

		this@Locations.updateLocationVDirection(location)

		currentConfidence += when (this@Locations.vDirection) {
			VerticalDirection.UP_CERTAIN -> 3
			VerticalDirection.UP -> 2
			VerticalDirection.FLAT -> 1
			else -> 0
		}

		// Check speed.
		if (location.speed != 0.0F && this@Locations.previousLocation != null) {

			// 500 feet per minute to meters per second.
			val maxChairliftSpeed = 500.0F * 0.00508F

			if (this@Locations.canUseAccuracy) {
				if (location.speedAccuracyMetersPerSecond != 0.0F && this@Locations.previousLocation!!.speedAccuracyMetersPerSecond != 0.0F) {
					currentConfidence += when {
						location.speed + location.speedAccuracyMetersPerSecond <= maxChairliftSpeed -> 2
						location.speed <= maxChairliftSpeed -> 1
						else -> 0
					}
				}
			} else {
				Log.w("checkIfOnChairlift", "Device does not support speed accuracy!")
				currentConfidence += if (location.speed <= maxChairliftSpeed) {
					1
				} else {
					0
				}
			}
		}

		withContext(Dispatchers.Main) {
			chairlifts.forEach {
				if (it.locationInsidePolygons(location)) {
					this@Locations.chairliftName = it.name
					currentConfidence += 1
				}
			}
		}

		return@coroutineScope currentConfidence / numberOfChecks >= minimumConfidenceValue
	}

	fun checkIfOnChairlift(location: Location, chairlifts: Array<MapItem>): Boolean {
		// TODO
		return false
	}

	@AnyThread
	suspend fun checkIfOnRunAsync(location: Location, mapHandler: MapHandler): Boolean = coroutineScope {

		val runArrays: Array<Collection<UIMapItem>> = arrayOf(mapHandler.easyRuns.values,
			mapHandler.moderateRuns.values,
			mapHandler.difficultRuns.values)

		runArrays.forEach { runs -> // TODO Does running this asynchronously actually work? Or is it better just to run it inline...
			val job: Deferred<Boolean> = async(Dispatchers.Main, CoroutineStart.LAZY) {
				Log.v("checkIfOnRun", "Checking run array...")
				runs.forEach {
					if (checkIfOnSpecificRun(it, location)) {
						Log.v("checkIfOnRun", "Finished checking run array (returning true)")
						return@async true
					}
				}
				Log.v("checkIfOnRun", "Finished checking run array (returning false)")
				return@async false
			}
			job.start()
			if (job.await()) {
				return@coroutineScope true
			}
		}
		return@coroutineScope false
	}

	fun checkIfOnRun(location: Location, runs: Array<Array<MapItem>>): Boolean {
		runs.forEach { runDifficulty ->
			runDifficulty.forEach {
				if (it.locationInsidePoints(location)) {
					return true
				}
			}
		}

		return false
	}

	@MainThread
	private fun checkIfOnSpecificRun(mapItem: UIMapItem, location: Location): Boolean {
		if (mapItem.locationInsidePolygons(location)) {
			if (mapItem.name != "") {
				if (mapItem.name == this@Locations.currentRun) {
					Log.d("checkIfOnSpecificRun", "Still on ${mapItem.name}")
				} else {
					Log.d("checkIfOnSpecificRun", "On run: ${mapItem.name}")
					this@Locations.currentRun = mapItem.name
				}
				return true
			}
		}
		return false
	}

	private enum class VerticalDirection {
		UP_CERTAIN, UP, FLAT, DOWN, DOWN_CERTAIN, UNKNOWN
	}
}