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
import com.mtspokane.skiapp.mapItem.MtSpokaneUIMapItems
import android.app.NotificationManager
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.annotation.DrawableRes
import android.graphics.drawable.BitmapDrawable

import android.graphics.drawable.Drawable
import androidx.appcompat.content.res.AppCompatResources


class SkierLocationService: Service(), LocationListener { // FIXME Leaks memory?

	override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
		Log.v("SkierLocationService", "onStartCommand called!")
		super.onStartCommand(intent, flags, startId)

		val notification: Notification = createNotification("", null)

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
			this.startForeground(SERVICE_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
		} else {
			this.startForeground(SERVICE_ID, notification)
		}

		Log.d("SkierLocationService", "Started foreground service")
		return START_NOT_STICKY
	}

	@SuppressLint("MissingPermission")
	override fun onCreate() {
		Log.v("SkierLocationService", "onCreate called!")
		super.onCreate()

		this.createNotificationChannels()

		val locationManager = this.getSystemService(Context.LOCATION_SERVICE) as LocationManager
		if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
			locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000,
				2F, this)
		}
	}

	private fun createNotificationChannels() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

			val trackingNotificationChannel = NotificationChannel(TRACKING_CHANNEL_ID, this.getString(R.string.tracking_notification_channel_name),
				NotificationManager.IMPORTANCE_DEFAULT)

			val progressNotificationChannel = NotificationChannel(PROGRESS_CHANNEL_ID, this.getString(R.string.progress_notification_channel_name),
				NotificationManager.IMPORTANCE_DEFAULT)

			val notificationManager: NotificationManager = this.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
			notificationManager.createNotificationChannels(listOf(trackingNotificationChannel, progressNotificationChannel))
			Log.v("createNotificatnChnnls", "Created new notification channel")
		} else {
			// TODO Pre Android Oreo
		}
	}

	override fun onDestroy() {
		Log.v("SkierLocationService", "onDestroy called!")
		super.onDestroy()

		// TODO Add code here to notify user.
	}

	override fun onLocationChanged(location: Location) {

		// If we are not on the mountain stop the tracking.
		if (MtSpokaneUIMapItems.lightSKiAreaBounds == null) {
			this.stopSelf()
		} else if (!MtSpokaneUIMapItems.lightSKiAreaBounds!!.hasPoints) {
			this.stopSelf()
		} else if (MtSpokaneUIMapItems.lightSKiAreaBounds!!.locationInsidePoints(location)) {
			this.stopSelf()
		}

		/*
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
		 */
	}

	private fun updateNotification(title: String, @DrawableRes icon: Int?) {

		val bitmap: Bitmap? = if (icon != null) {
			drawableToBitmap(AppCompatResources.getDrawable(this, icon)!!)
		} else {
			null
		}

		val notification: Notification = createNotification(title, bitmap)

		val notificationManager: NotificationManager = this.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
		notificationManager.notify(SERVICE_ID, notification)
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
			val builder = Notification.Builder(this, NotificationChannel(TRACKING_CHANNEL_ID, "Location", NotificationManager.IMPORTANCE_DEFAULT).id)
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

		const val SERVICE_ID = 29500

		const val TRACKING_CHANNEL_ID = "skiAppTracker"

		const val PROGRESS_CHANNEL_ID = "skiAppProgress"

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