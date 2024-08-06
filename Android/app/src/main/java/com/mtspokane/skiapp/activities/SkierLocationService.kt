package com.mtspokane.skiapp.activities

import android.Manifest
import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Binder
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.annotation.RequiresApi
import androidx.annotation.StringRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.fragment.app.FragmentActivity
import com.mtspokane.skiapp.R
import com.mtspokane.skiapp.activities.activitysummary.ActivitySummary
import com.mtspokane.skiapp.databases.ActivityDatabase
import com.mtspokane.skiapp.mapItem.SkiingActivity
import com.mtspokane.skiapp.databases.SkiingActivityManager
import com.mtspokane.skiapp.databases.TimeManager
import com.mtspokane.skiapp.mapItem.Locations
import com.mtspokane.skiapp.mapItem.MapMarker
import kotlin.reflect.KClass


class SkierLocationService : Service(), LocationListener {

	// FIXME Binder leaks memory
	private var binder: IBinder? = LocalBinder()

	private var serviceCallbacks: ServiceCallbacks? = null

	inner class LocalBinder: Binder() {
		fun getService(): SkierLocationService = this@SkierLocationService
	}

	private lateinit var locationManager: LocationManager

	private lateinit var notificationManager: NotificationManager

	override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
		Log.v("SkierLocationService", "onStartCommand called!")
		super.onStartCommand(intent, flags, startId)

		val notification: Notification = createPersistentNotification("", null)

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
			startForeground(TRACKING_SERVICE_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
		} else {
			startForeground(TRACKING_SERVICE_ID, notification)
		}

		Log.d("SkierLocationService", "Started foreground service")
		return START_NOT_STICKY
	}

	override fun onCreate() {
		Log.v("SkierLocationService", "onCreate called!")
		super.onCreate()

		// If we don't have permission to track user location somehow at this spot just return early.
		if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
			!= PackageManager.PERMISSION_GRANTED) {
			Log.w("SkierLocationService", "Service started before permissions granted!")
			return
		}

		val manager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
		notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

		SkiingActivityManager.resumeActivityTracking(this)

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			createNotificationChannels()
		}

		if (manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
			manager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 3000, 1F, this)
		}

		locationManager = manager

		Toast.makeText(this, R.string.starting_tracking, Toast.LENGTH_SHORT).show()
	}

	fun setCallbacks(callbacks: ServiceCallbacks?) {
		serviceCallbacks = callbacks
	}

	@RequiresApi(Build.VERSION_CODES.O)
	private fun createNotificationChannels() {

		val trackingNotificationChannel = NotificationChannel(TRACKING_SERVICE_CHANNEL_ID,
			getString(R.string.tracking_notification_channel_name), NotificationManager.IMPORTANCE_LOW)

		val progressNotificationChannel = NotificationChannel(ACTIVITY_SUMMARY_CHANNEL_ID,
			getString(R.string.activity_summary_notification_channel_name), NotificationManager.IMPORTANCE_DEFAULT)

		val notificationManager: NotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
		notificationManager.createNotificationChannels(listOf(trackingNotificationChannel, progressNotificationChannel))
		Log.v("createNotificatnChnnls", "Created new notification channel")
	}

	override fun onDestroy() {
		Log.v("SkierLocationService", "onDestroy has been called!")
		super.onDestroy()

		locationManager.removeUpdates(this)
		notificationManager.cancel(TRACKING_SERVICE_ID)

		if (SkiingActivityManager.InProgressActivities.isNotEmpty()) {

			val database = ActivityDatabase(this)
			ActivityDatabase.writeSkiingActivitiesToDatabase(SkiingActivityManager.InProgressActivities
				.toTypedArray(), database.writableDatabase)
			database.close()
			SkiingActivityManager.InProgressActivities.clear()

			val pendingIntent: PendingIntent = this.createPendingIntent(
				ActivitySummary::class,
				TimeManager.getTodaysDate())

			val builder: NotificationCompat.Builder = this.getNotificationBuilder(ACTIVITY_SUMMARY_CHANNEL_ID,
				true, R.string.activity_notification_text, pendingIntent)

			val notification: Notification = builder.build()

			notificationManager.notify(ACTIVITY_SUMMARY_ID, notification)
		}

		binder = null
	}

	override fun onLocationChanged(location: Location) {
		Log.v("SkierLocationService", "Location updated")
		Locations.updateLocations(SkiingActivity(location))

		if (serviceCallbacks != null) {

			// If we are not on the mountain stop the tracking.
			if (!serviceCallbacks!!.isInBounds(location)) {
				Toast.makeText(this, R.string.out_of_bounds, Toast.LENGTH_LONG).show()
				Log.d("SkierLocationService", "Stopping location tracking service")
				stopSelf()
				return
			}

			var mapMarker = serviceCallbacks!!.getOnLocation(location)
			if (mapMarker != null) {
				appendSkiingActivity(R.string.current_chairlift, mapMarker, location)
				return
			}

			mapMarker = serviceCallbacks!!.getInLocation(location)
			if (mapMarker != null) {
				appendSkiingActivity(R.string.current_other, mapMarker, location)
				return
			}
		}

		updateTrackingNotification(this.getString(R.string.tracking_notice), null)
	}

	private fun appendSkiingActivity(@StringRes textResource: Int, mapMarker: MapMarker, location: Location) {
		val text: String = getString(textResource, mapMarker.name)
		serviceCallbacks!!.updateMapMarker(text)
		updateTrackingNotification(text, mapMarker.icon)

		SkiingActivityManager.InProgressActivities.add(SkiingActivity(location))
	}

	private fun updateTrackingNotification(title: String, @DrawableRes icon: Int?) {

		val bitmap: Bitmap? = if (icon != null) {
			drawableToBitmap(AppCompatResources.getDrawable(this, icon)!!)
		} else {
			null
		}

		val notification: Notification = createPersistentNotification(title, bitmap)
		notificationManager.notify(TRACKING_SERVICE_ID, notification)
	}

	@SuppressLint("UnspecifiedImmutableFlag")
	private fun createPendingIntent(activityToLaunch: KClass<out FragmentActivity>, date: String?): PendingIntent {

		val notificationIntent = Intent(this, activityToLaunch.java)
		if (date != null && TimeManager.isValidDateFormat(date)) {
			notificationIntent.putExtra(ACTIVITY_SUMMARY_LAUNCH_DATE, date)
		}

		return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE)
		} else {
			PendingIntent.getActivity(this, 0, notificationIntent, 0)
		}
	}

	private fun createPersistentNotification(title: String, iconBitmap: Bitmap?): Notification {

		val pendingIntent: PendingIntent = createPendingIntent(MapsActivity::class, null)

		val builder: NotificationCompat.Builder = getNotificationBuilder(TRACKING_SERVICE_CHANNEL_ID,
			false, R.string.tracking_notice, pendingIntent)
		builder.setContentText(title)

		if (iconBitmap != null) {
			builder.setLargeIcon(iconBitmap)
		}

		return builder.build()
	}

	private fun getNotificationBuilder(channelId: String, showTime: Boolean, @StringRes titleText: Int,
		pendingIntent: PendingIntent): NotificationCompat.Builder {

		return NotificationCompat.Builder(this, channelId)
			.setSmallIcon(R.drawable.icon_fg)
			.setShowWhen(showTime)
			.setContentTitle(this.getString(titleText))
			.setContentIntent(pendingIntent)
	}

	override fun onBind(intent: Intent?): IBinder? {
		return binder
	}

	override fun onProviderEnabled(provider: String) {}

	override fun onProviderDisabled(provider: String) {}

	@Deprecated("This callback will never be invoked on Android Q and above.")
	override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}

	companion object {

		const val TRACKING_SERVICE_ID = 69

		const val ACTIVITY_SUMMARY_ID = 420

		const val TRACKING_SERVICE_CHANNEL_ID = "skiAppTracker"

		const val ACTIVITY_SUMMARY_CHANNEL_ID = "skiAppProgress"

		const val ACTIVITY_SUMMARY_LAUNCH_DATE = "activitySummaryLaunchDate"

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
			drawable.setBounds(0, 0, canvas.width, canvas.height)
			drawable.draw(canvas)

			return bitmap
		}
	}

	interface ServiceCallbacks {
		fun isInBounds(location: Location): Boolean

		fun getOnLocation(location: Location): MapMarker?

		fun getInLocation(location: Location): MapMarker?

		fun updateMapMarker(locationString: String)
	}
}