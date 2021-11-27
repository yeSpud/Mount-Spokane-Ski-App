package com.mtspokane.skiapp

import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import com.mtspokane.skiapp.mapItem.MapItem
import com.mtspokane.skiapp.mapItem.MtSpokaneMapItems
import kotlinx.coroutines.CoroutineStart

class SkierLocationService: Service(), LocationListener {

	/*
	 * Runs every time the service is started.
	 */
	override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
		Log.v("SkierLocationService", "onStartCommand called!")
		super.onStartCommand(intent, flags, startId)

		val notificationIntent = Intent(this, MapsActivity::class.java)

		val pendingIntent: PendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE)
		} else {
			PendingIntent.getActivity(this, 0, notificationIntent, 0)
		}

		val notification: Notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			Notification.Builder(this, NotificationChannel(CHANNEL_ID, "Location", NotificationManager.IMPORTANCE_DEFAULT).id)
				.setSmallIcon(R.drawable.icon_fg)
				.setShowWhen(false)
				.setContentTitle(this.getString(R.string.tracking_notice))
				.setContentIntent(pendingIntent)
				.build()
		} else {
			Notification() // TODO Notification pre Oreo
		}

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
			this.startForeground(foregroundId, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
		} else {
			startForeground(foregroundId, notification)
		}

		return START_NOT_STICKY
	}

	/*
	 * Runs only once (supposedly?)
	 */
	@SuppressLint("MissingPermission")
	override fun onCreate() {
		Log.v("SkierLocationService", "onCreate called!")
		super.onCreate()

		/*
		val locationManager = this.getSystemService(Context.LOCATION_SERVICE) as LocationManager
		if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
			locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000,
				2F, this)
		}
		 */
	}

	override fun onDestroy() {
		Log.v("SkierLocationService", "onDestroy called!")
		super.onDestroy()

		// TODO Add code here to notify user.
	}

	override fun onLocationChanged(location: Location) {

		// If we are not on the mountain stop the tracking.
		if (MtSpokaneMapItems.skiAreaBounds == null) {
			this.stopSelf()
		} else if (!MtSpokaneMapItems.skiAreaBounds!!.hasPoints) {
			this.stopSelf()
		} else if (MtSpokaneMapItems.skiAreaBounds!!.locationInsidePoints(location)) {
			this.stopSelf()
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

		this.recreateNotification(this.getString(R.string.tracking_notice), null)
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

		const val CHANNEL_ID = "skiAppTracker"


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