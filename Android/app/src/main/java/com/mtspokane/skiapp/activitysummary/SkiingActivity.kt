package com.mtspokane.skiapp.activitysummary

import android.content.Context
import android.location.Location
import android.os.Build
import androidx.annotation.DrawableRes
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class SkiingActivity(val name: String, location: Location, @DrawableRes val icon: Int?) {

	val accuracy: Float = location.accuracy

	val altitude: Double = location.altitude

	val altitudeAccuracy: Float? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
		location.verticalAccuracyMeters
	} else {
		null
	}

	val latitude: Double = location.latitude

	val longitude: Double = location.longitude

	val speed: Float = location.speed

	val speedAccuracy: Float? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
		location.speedAccuracyMetersPerSecond
	} else {
		null
	}

	val time: Long = location.time

	companion object  {

		val Activities: ArrayList<SkiingActivity> = ArrayList(0)

		private const val NAME = "name"
		private const val ICON = "icon"
		private const val ACCURACY = "acc"
		private const val ALTITUDE = "alt"
		private const val ALTITUDE_ACCURACY = "altacc"
		private const val LATITUDE = "lat"
		private const val LONGITUDE = "lng"
		private const val SPEED = "speed"
		private const val SPEED_ACCURACY = "speedacc"
		private const val TIME = "time"

		fun writeActivitiesToFile(context: Context) {

			val jsonArray = JSONArray()
			Activities.forEach {
				val jsonEntry = JSONObject()
				jsonEntry.put(NAME, it.name)
				if (it.icon != null) {
					jsonEntry.put(ICON, context.resources.getResourceName(it.icon))
				} else {
					jsonEntry.put(ICON, null)
				}
				jsonEntry.put(ACCURACY, it.accuracy)
				jsonEntry.put(ALTITUDE, it.altitude)
				jsonEntry.put(ALTITUDE_ACCURACY, it.altitudeAccuracy)
				jsonEntry.put(LATITUDE, it.latitude)
				jsonEntry.put(LONGITUDE, it.longitude)
				jsonEntry.put(SPEED, it.speed)
				jsonEntry.put(SPEED_ACCURACY, it.speedAccuracy)
				jsonEntry.put(TIME, it.time)
				jsonArray.put(jsonEntry)
			}

			val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
			val date: String = dateFormat.format(Date())

			val jsonObject = JSONObject()
			jsonObject.put(date, jsonArray)

			context.openFileOutput("$date.json", Context.MODE_PRIVATE).use {
				it.write(jsonObject.toString().toByteArray())
			}
		}

		fun readFromFile(context: Context, filename: String): Array<SkiingActivity> {
			// TODO

			return arrayOf(SkiingActivity("foo", Location("gps"), null))
		}
	}
}