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
import com.mtspokane.skiapp.mapItem.MtSpokaneMapItems
import android.app.NotificationManager
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.annotation.DrawableRes
import android.graphics.drawable.BitmapDrawable

import android.graphics.drawable.Drawable
import androidx.appcompat.content.res.AppCompatResources


class SkierLocationService: Service(), LocationListener { // FIXME Leaks memory?

	/*
	 * Runs every time the service is started.
	 */
	override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
		Log.v("SkierLocationService", "onStartCommand called!")
		super.onStartCommand(intent, flags, startId)

		val notification: Notification = createNotification("", null)

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

		val locationManager = this.getSystemService(Context.LOCATION_SERVICE) as LocationManager
		if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
			locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000,
				2F, this)
		}
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
			this.updateNotification(this.getString(R.string.current_other, other.name), other.getIcon())
			return
		}

		val chairlift = Locations.checkIfOnChairlift(location)
		if (chairlift != null) {
			this.updateNotification(this.getString(R.string.current_chairlift, chairlift.name), chairlift.getIcon())
			return
		}

		val run = Locations.checkIfOnRun(location)
		if (run != null) {
			this.updateNotification(this.getString(R.string.current_run, run.name), run.getIcon())
			return
		}

		this.updateNotification(this.getString(R.string.tracking_notice), null)
		//}.start()
	}

	private fun updateNotification(title: String, @DrawableRes icon: Int?) {

		val bitmap: Bitmap? = if (icon != null) {
			drawableToBitmap(AppCompatResources.getDrawable(this, icon)!!)
		} else {
			null
		}

		val notification: Notification = createNotification(title, bitmap)

		val notificationManager: NotificationManager = this.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
		notificationManager.notify(foregroundId, notification)
	}

	private fun createPendingIntent(): PendingIntent {
		val notificationIntent = Intent(this, MapsActivity::class.java)
		return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE)
		} else {
			PendingIntent.getActivity(this, 0, notificationIntent, 0)
		}
	}

	private fun createNotification(title: String, iconBitmap: Bitmap?): Notification {

		val pendingIntent: PendingIntent = this.createPendingIntent()

		return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			val builder = Notification.Builder(this, NotificationChannel(CHANNEL_ID, "Location", NotificationManager.IMPORTANCE_DEFAULT).id)
			builder.setSmallIcon(R.drawable.icon_fg)
			builder.setShowWhen(false)
			builder.setContentTitle(this.getString(R.string.tracking_notice))
			builder.setContentText(title)
			builder.setContentIntent(pendingIntent)
			if (iconBitmap != null) {
				builder.setLargeIcon(iconBitmap)
			}
			builder.build()
		} else {
			Notification() // TODO Notification pre Oreo
		}
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

		/**
		 * @author https://studiofreya.com/2018/08/15/android-notification-large-icon-from-vector-xml/
		 */
		private fun drawableToBitmap(drawable: Drawable): Bitmap? {

			if (drawable is BitmapDrawable) {
				return drawable.bitmap
			}

			val bitmap: Bitmap = Bitmap.createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight,
				Bitmap.Config.ARGB_8888)

			val canvas = Canvas(bitmap)
			drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight())
			drawable.draw(canvas)

			return bitmap
		}
	}
}