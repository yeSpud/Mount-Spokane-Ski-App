package com.mtspokane.skiapp

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.IBinder
import com.mtspokane.skiapp.mapItem.MapItem

class SkierLocationService: Service(), LocationListener {

	private lateinit var skiAreaBounds: MapItem
	private lateinit var otherMapItems: Array<MapItem>
	private lateinit var chairliftMapItems: Array<MapItem>
	private lateinit var runs: Array<Array<MapItem>>

	@SuppressLint("MissingPermission")
	override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
		super.onStartCommand(intent, flags, startId)

		// TODO Load lateinits from intent.

		val locationManager = this.getSystemService(Context.LOCATION_SERVICE) as LocationManager
		if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
			locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000,
				2F, this)
		}

		return START_NOT_STICKY
	}

	override fun onLocationChanged(location: Location) {

		// If we are not on the mountain return early.
		if (!this.skiAreaBounds.locationInsidePoints(location)) {
			return
		}

		// Check if our skier is on a run, chairlift, or other.
		//this.lifecycleScope.async(Dispatchers.IO, CoroutineStart.LAZY) {

		when {
			@Suppress("UNCHECKED_CAST")
			Locations.checkIfOnOther(location, this.otherMapItems) -> this.recreateNotification(this.getString(R.string.current_other, Locations.otherName), null /* TODO Other icon*/)
			Locations.checkIfOnChairlift(location, this.chairliftMapItems) -> this.recreateNotification(this.getString(R.string.current_chairlift, Locations.chairliftName), null /* TODO Chairlift icon*/ )
			Locations.checkIfOnRun(location, this.runs) -> this.recreateNotification(this.getString(R.string.current_run, Locations.currentRun), /* TODO Run icon */ null)
			else -> this.recreateNotification(this.getString(R.string.app_name), null)
		}
		//}.start()
	}

	private fun recreateNotification(title: String, icon: Int?) {
		// TODO
	}

	override fun onBind(intent: Intent?): IBinder? {

		// We don't provide binding, so return null
		return null
	}

	companion object {

		const val foregroundId = 29500

		fun checkIfRunning(activity: MapsActivity): Boolean {

			val activityManager: ActivityManager = activity.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
			activityManager.getRunningServices(Int.MAX_VALUE).forEach {
				if (it.service.className == SkierLocationService::class.java.name) {
					return true
				}
			}

			return false
		}
	}
}