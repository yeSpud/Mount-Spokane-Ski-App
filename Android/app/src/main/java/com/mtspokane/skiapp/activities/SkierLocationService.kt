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
import androidx.annotation.StringRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.fragment.app.FragmentActivity
import androidx.room.Room
import com.mtspokane.skiapp.R
import com.mtspokane.skiapp.activities.activitysummary.ActivitySummary
import com.mtspokane.skiapp.databases.Database
import com.mtspokane.skiapp.databases.LongAndShortDate
import com.mtspokane.skiapp.databases.SkiingActivity
import com.mtspokane.skiapp.databases.SkiingActivityDao
import com.mtspokane.skiapp.databases.SkiingDate
import com.mtspokane.skiapp.mapItem.Locations
import com.mtspokane.skiapp.mapItem.MapMarker
import java.text.SimpleDateFormat
import java.util.Locale
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

	private lateinit var databaseDao: SkiingActivityDao

	private lateinit var skiingDate: SkiingDate

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

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			val trackingNotificationChannel = NotificationChannel(TRACKING_SERVICE_CHANNEL_ID,
				getString(R.string.tracking_notification_channel_name), NotificationManager.IMPORTANCE_LOW)

			val progressNotificationChannel = NotificationChannel(ACTIVITY_SUMMARY_CHANNEL_ID,
				getString(R.string.activity_summary_notification_channel_name), NotificationManager.IMPORTANCE_DEFAULT)

			val notificationManager: NotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
			notificationManager.createNotificationChannels(listOf(trackingNotificationChannel, progressNotificationChannel))
			Log.v("onCreate", "Created new notification channel")
		}

		if (manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
			manager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 3000, 1F, this)
		}

		locationManager = manager

		val database = Room.databaseBuilder(this, Database::class.java, Database.NAME)
			.allowMainThreadQueries().build()
		databaseDao = database.skiingActivityDao()

		val todaysDate = Database.getTodaysDate()

		var skiingDateAndActivities = databaseDao.getSkiingDateWithActivitiesByShortDate(todaysDate)
		while (skiingDateAndActivities == null) {
			databaseDao.addSkiingDate(LongAndShortDate(
				longDate = SimpleDateFormat("LLLL dd yyyy", Locale.US).format(todaysDate).toString(),
				shortDate = todaysDate
			))

			skiingDateAndActivities = databaseDao.getSkiingDateWithActivitiesByShortDate(todaysDate)
		}
		skiingDate = skiingDateAndActivities.skiingDate

		Toast.makeText(this, R.string.starting_tracking, Toast.LENGTH_SHORT).show()
	}

	fun setCallbacks(callbacks: ServiceCallbacks?) {
		serviceCallbacks = callbacks
	}

	override fun onDestroy() {
		Log.v("SkierLocationService", "onDestroy has been called!")
		super.onDestroy()

		locationManager.removeUpdates(this)
		notificationManager.cancel(TRACKING_SERVICE_ID)

		val skiingActivities = databaseDao.getActivitiesByDateId(skiingDate.id)
		if (skiingActivities.isNotEmpty()) {

			val pendingIntent: PendingIntent = createPendingIntent(ActivitySummary::class, skiingDate.id)
			val builder: NotificationCompat.Builder = getNotificationBuilder(ACTIVITY_SUMMARY_CHANNEL_ID,
				true, R.string.activity_notification_text, pendingIntent)

			val notification: Notification = builder.build()
			notificationManager.notify(ACTIVITY_SUMMARY_ID, notification)
		}

		binder = null
	}

	override fun onLocationChanged(location: Location) {
		Log.v("SkierLocationService", "Location updated")
		val localServiceCallback = serviceCallbacks ?: return

		// If we are not on the mountain stop the tracking.
		if (!localServiceCallback.isInBounds(location)) {
			Toast.makeText(this, R.string.out_of_bounds,
				Toast.LENGTH_LONG).show()
			notificationManager.cancel(TRACKING_SERVICE_ID)
			Log.d("SkierLocationService", "Stopping location tracking service")
			stopSelf()
			return
		}

		val skiingActivity = SkiingActivity(
			location.accuracy,
			location.altitude,
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
				location.mslAltitudeAccuracyMeters
			} else { null },
			location.latitude,
			location.longitude,
			location.speed,
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
				location.speedAccuracyMetersPerSecond
			} else { null },
			location.time,
			skiingDate.id
		)
		databaseDao.addSkiingActivity(skiingActivity)
		Locations.updateLocations(skiingActivity)

		var mapMarker = localServiceCallback.getOnLocation(location)
		if (mapMarker != null) {
			displaySkiingActivity(R.string.current_chairlift, mapMarker)
			return
		}

		mapMarker = localServiceCallback.getInLocation(location)
		if (mapMarker != null) {
			displaySkiingActivity(R.string.current_other, mapMarker)
			return
		}

		updateTrackingNotification(getString(R.string.tracking_notice), null)
	}

	private fun displaySkiingActivity(@StringRes textResource: Int, mapMarker: MapMarker) {
		val localServiceCallback = serviceCallbacks ?: return
		val text: String = getString(textResource, mapMarker.name)
		localServiceCallback.updateMapMarker(text)
		updateTrackingNotification(text, mapMarker.icon)
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
	private fun createPendingIntent(activityToLaunch: KClass<out FragmentActivity>, dateId: Int): PendingIntent {
		val notificationIntent = Intent(this, activityToLaunch.java)
		notificationIntent.putExtra(ACTIVITY_SUMMARY_LAUNCH_DATE, dateId)

		return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE)
		} else {
			PendingIntent.getActivity(this, 0, notificationIntent, 0)
		}
	}

	private fun createPersistentNotification(title: String, iconBitmap: Bitmap?): Notification {
		val pendingIntent: PendingIntent = createPendingIntent(MapsActivity::class, skiingDate.id)
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