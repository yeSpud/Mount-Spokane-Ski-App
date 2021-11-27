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
import com.mtspokane.skiapp.mapItem.MtSpokaneMapItems
import kotlinx.coroutines.CoroutineStart

class SkierLocationService: Service(), LocationListener {

	@SuppressLint("MissingPermission")
	override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
		super.onStartCommand(intent, flags, startId)

		// TODO Load lateinits from intent.

		/*
		val locationManager = this.getSystemService(Context.LOCATION_SERVICE) as LocationManager
		if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
			locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000,
				2F, this)
		}
		 */

		return START_NOT_STICKY
	}

	override fun onLocationChanged(location: Location) {

		// If we are not on the mountain return early.
		if (MtSpokaneMapItems.skiAreaBounds == null) {
			return
		} else if (!MtSpokaneMapItems.skiAreaBounds!!.hasPoints) {
			return
		} else if (MtSpokaneMapItems.skiAreaBounds!!.locationInsidePoints(location)) {
			return
		}

		// Check if our skier is on a run, chairlift, or other.
		//this.lifecycleScope.async(Dispatchers.IO, CoroutineStart.LAZY) {

		val other = Locations.checkIfOnOther(location)
		if (other != null) {
			this.recreateNotification(this.getString(R.string.current_other, other.name), other.getIcon())
			return
		}

		val chairlift = Locations.checkIfOnChairlift(location)
		if (chairlift != null) {
			this.recreateNotification(this.getString(R.string.current_chairlift, chairlift.name), chairlift.getIcon())
			return
		}

		val run = Locations.checkIfOnRun(location)
		if (run != null) {
			this.recreateNotification(this.getString(R.string.current_run, run.name), run.getIcon())
			return
		}

		this.recreateNotification(this.getString(R.string.app_name), null)
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