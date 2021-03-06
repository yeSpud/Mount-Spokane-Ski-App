package com.mtspokane.skiapp.activities.mainactivity

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.annotation.RequiresApi
import androidx.annotation.StringRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.app.NotificationCompat
import androidx.fragment.app.FragmentActivity
import com.mtspokane.skiapp.R
import com.mtspokane.skiapp.databases.SkiingActivity
import com.mtspokane.skiapp.mapItem.MtSpokaneMapItems
import com.mtspokane.skiapp.activities.activitysummary.ActivitySummary
import com.mtspokane.skiapp.databases.ActivityDatabase
import com.mtspokane.skiapp.databases.SkiingActivityManager
import com.mtspokane.skiapp.databases.TimeManager
import com.mtspokane.skiapp.mapItem.MapMarker
import kotlin.reflect.KClass

class SkierLocationService : Service(), LocationListener {

	private lateinit var locationManager: LocationManager

	private lateinit var notificationManager: NotificationManager

	override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
		Log.v("SkierLocationService", "onStartCommand called!")
		super.onStartCommand(intent, flags, startId)

		val notification: Notification = this.createPersistentNotification("", null)

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
			this.startForeground(TRACKING_SERVICE_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
		} else {
			this.startForeground(TRACKING_SERVICE_ID, notification)
		}

		Log.d("SkierLocationService", "Started foreground service")
		return START_NOT_STICKY
	}

	@SuppressLint("MissingPermission")
	override fun onCreate() {
		Log.v("SkierLocationService", "onCreate called!")
		super.onCreate()

		this.locationManager = this.getSystemService(Context.LOCATION_SERVICE) as LocationManager
		this.notificationManager = this.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

		MtSpokaneMapItems.checkoutObject(this::class)

		SkiingActivityManager.resumeActivityTracking(this)

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			this.createNotificationChannels()
		}

		if (this.locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
			this.locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 3000,
				1F, this)
		}

		Toast.makeText(this, R.string.starting_tracking, Toast.LENGTH_SHORT).show()
	}

	@RequiresApi(Build.VERSION_CODES.O)
	private fun createNotificationChannels() {

		val trackingNotificationChannel = NotificationChannel(TRACKING_SERVICE_CHANNEL_ID,
			this.getString(R.string.tracking_notification_channel_name), NotificationManager.IMPORTANCE_LOW)

		val progressNotificationChannel = NotificationChannel(ACTIVITY_SUMMARY_CHANNEL_ID,
			this.getString(R.string.activity_summary_notification_channel_name), NotificationManager.IMPORTANCE_DEFAULT)

		val notificationManager: NotificationManager = this.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
		notificationManager.createNotificationChannels(listOf(trackingNotificationChannel,
				progressNotificationChannel))
		Log.v("createNotificatnChnnls", "Created new notification channel")
	}

	@SuppressLint("MissingPermission")
	override fun onDestroy() {
		Log.v("SkierLocationService", "onDestroy has been called!")
		super.onDestroy()

		InAppLocations.visibleLocationUpdates.clear()

		this.locationManager.removeUpdates(this)
		this.notificationManager.cancel(TRACKING_SERVICE_ID)

		if (SkiingActivityManager.InProgressActivities.isNotEmpty()) {

			val database = ActivityDatabase(this)
			ActivityDatabase.writeSkiingActivitiesToDatabase(SkiingActivityManager.InProgressActivities
				.toTypedArray(), database.writableDatabase)
			database.close()
			SkiingActivityManager.InProgressActivities.clear()

			val pendingIntent: PendingIntent = this.createPendingIntent(ActivitySummary::class,
				TimeManager.getTodaysDate())

			val builder: NotificationCompat.Builder = this.getNotificationBuilder(ACTIVITY_SUMMARY_CHANNEL_ID,
				true, R.string.activity_notification_text, pendingIntent)

			val notification: Notification = builder.build()

			this.notificationManager.notify(ACTIVITY_SUMMARY_ID, notification)
		}

		MtSpokaneMapItems.destroyUIItems(this::class)
	}

	override fun onLocationChanged(location: Location) {

		if (MtSpokaneMapItems.skiAreaBounds == null) {
			Log.w("SkierLocationService", "Ski bounds have not been set up!")
			return
		}

		InAppLocations.updateLocations(location)

		// If we are not on the mountain stop the tracking.
		if (MtSpokaneMapItems.skiAreaBounds!!.points.isEmpty()) {
			Toast.makeText(this, R.string.bounds_missing,
				Toast.LENGTH_LONG).show()
			this.stopSelf()
			return
		} else if (!MtSpokaneMapItems.skiAreaBounds!!.locationInsidePoints(location)) {
			Toast.makeText(this, R.string.out_of_bounds,
				Toast.LENGTH_LONG).show()
			this.stopSelf()
			return
		}

		var mapMarker: MapMarker? = InAppLocations.checkIfAtChairliftTerminals()
		if (mapMarker != null) {
			this.appendSkiingActivity(R.string.current_chairlift, mapMarker, location)
			return
		}

		mapMarker = InAppLocations.checkIfIOnChairlift()
		if (mapMarker != null) {
			this.appendSkiingActivity(R.string.current_chairlift, mapMarker, location)
			return
		}

		mapMarker = InAppLocations.checkIfOnOther()
		if (mapMarker != null) {
			this.appendSkiingActivity(R.string.current_other, mapMarker, location)
			return
		}

		mapMarker = InAppLocations.checkIfOnRun()
		if (mapMarker != null) {
			this.appendSkiingActivity(R.string.current_run, mapMarker, location)
			return
		}

		InAppLocations.visibleLocationUpdates.forEach { it.updateLocation(this.getString(R.string.app_name)) }
		this.updateTrackingNotification(this.getString(R.string.tracking_notice), null)
	}

	private fun appendSkiingActivity(@StringRes textResource: Int, mapMarker: MapMarker, location: Location) {
		val text: String = this.getString(textResource, mapMarker.name)
		InAppLocations.visibleLocationUpdates.forEach { it.updateLocation(text) }
		this.updateTrackingNotification(text, mapMarker.icon)

		SkiingActivityManager.InProgressActivities.add(SkiingActivity(location))
	}

	private fun updateTrackingNotification(title: String, @DrawableRes icon: Int?) {

		val bitmap: Bitmap? = if (icon != null) {
			drawableToBitmap(AppCompatResources.getDrawable(this, icon)!!)
		} else {
			null
		}

		val notification: Notification = this.createPersistentNotification(title, bitmap)
		this.notificationManager.notify(TRACKING_SERVICE_ID, notification)
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

		val pendingIntent: PendingIntent = this.createPendingIntent(MapsActivity::class, null)

		val builder: NotificationCompat.Builder = this.getNotificationBuilder(TRACKING_SERVICE_CHANNEL_ID,
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

		// We don't provide binding, so return null
		return null
	}

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
}