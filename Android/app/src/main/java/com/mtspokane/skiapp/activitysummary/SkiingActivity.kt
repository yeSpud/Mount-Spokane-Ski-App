package com.mtspokane.skiapp.activitysummary

import android.app.Activity
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.location.Location
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.annotation.DrawableRes
import org.json.JSONArray
import org.json.JSONObject
import java.io.FileInputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class SkiingActivity {

	val name: String

	@DrawableRes
	val icon: Int?

	val accuracy: Float

	val altitude: Double

	val altitudeAccuracy: Float?

	val latitude: Double

	val longitude: Double

	val speed: Float

	val speedAccuracy: Float?

	val time: Long

	constructor(name: String, location: Location, @DrawableRes icon: Int?) {
		this.name = name
		this.icon = icon

		this.accuracy = location.accuracy
		this.altitude = location.altitude

		this.altitudeAccuracy = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			location.verticalAccuracyMeters
		} else {
			null
		}

		this.latitude = location.latitude
		this.longitude = location.longitude
		this.speed = location.speed

		this.speedAccuracy = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			location.speedAccuracyMetersPerSecond
		} else {
			null
		}

		this.time = location.time
	}

	constructor(name: String, @DrawableRes icon: Int?, accuracy: Float, altitude: Double,
	            altitudeAccuracy: Float?, latitude: Double, longitude: Double, speed: Float,
	            speedAccuracy: Float?, time: Long) {

		this.name = name
		this.icon = icon
		this.accuracy = accuracy
		this.altitude = altitude
		this.altitudeAccuracy = altitudeAccuracy
		this.latitude = latitude
		this.longitude = longitude
		this.speed = speed
		this.speedAccuracy = speedAccuracy
		this.time = time
	}

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

		fun writeActivitiesToFile(context: Context): String {

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

			val jsonObject = JSONObject()
			val date: String = getDate()
			jsonObject.put(date, jsonArray)

			val filename = "$date.json"
			context.openFileOutput(filename, Context.MODE_PRIVATE).use {
				it.write(jsonObject.toString().toByteArray())
			}

			return filename
		}

		fun writeToExportFile(contentResolver: ContentResolver, uri: Uri, outText: String) {

			val outputStream: OutputStream = contentResolver.openOutputStream(uri)!!
			outputStream.use {
				it.write(outText.toByteArray())
			}
		}

		fun readJsonFromFile(context: Context, filename: String) : JSONObject {

			var json = JSONObject()
			val fileInputStream: FileInputStream = context.openFileInput(filename)
			fileInputStream.bufferedReader().useLines {

				val string = it.fold("") { _, inText -> inText }

				json = JSONObject(string)
			}
			fileInputStream.close()

			return json
		}

		fun readSkiingActivitiesFromFile(context: Context, filename: String): Array<SkiingActivity> {

			val arrayList = ArrayList<SkiingActivity>(0)

			val json = readJsonFromFile(context, filename)

			val jsonArray: JSONArray = json.getJSONArray(json.keys().next())

			val count = jsonArray.length()
			for (i in 0 until count) {
				val jsonObject: JSONObject = jsonArray.getJSONObject(i)

				val name: String = jsonObject.getString(NAME)

				val iconLocation: String? = jsonObject.opt(ICON) as String?
				val icon: Int? = if (iconLocation != null) {
					context.resources.getIdentifier(iconLocation, "drawable", context.packageName)
				} else {
					null
				}

				val accuracy: Float = parseFloat(jsonObject, ACCURACY)!!
				val altitude: Double = jsonObject.optDouble(ALTITUDE)
				val altitudeAccuracy: Float? = parseFloat(jsonObject, ALTITUDE_ACCURACY)
				val latitude: Double = jsonObject.optDouble(LATITUDE)
				val longitude: Double = jsonObject.optDouble(LONGITUDE)
				val speed: Float = parseFloat(jsonObject, SPEED)!!
				val speedAccuracy: Float? = parseFloat(jsonObject, SPEED_ACCURACY)
				val time: Long = jsonObject.optLong(TIME)

				val activity = SkiingActivity(name, icon, accuracy, altitude, altitudeAccuracy,
					latitude, longitude, speed, speedAccuracy, time)
				arrayList.add(activity)
			}

			return arrayList.toTypedArray()
		}

		private fun parseFloat(jsonObject: JSONObject, key: String): Float? {
			return when (val accuracyAny = jsonObject.opt(key)) {
				is Float -> accuracyAny
				is Int -> accuracyAny.toFloat()
				is Double -> accuracyAny.toFloat()
				else -> {
					Log.w("parseFloat", "Unable to load accuracy")
					null
				}
			}
		}

		fun populateActivitiesArray(context: Context) {

			val date: String = getDate()
			val filename = "$date.json"

			val array: Array<SkiingActivity> = readSkiingActivitiesFromFile(context, filename)

			Activities.addAll(array)
		}

		fun getDate(): String {
			val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
			return dateFormat.format(Date())
		}

		fun shareJsonFile(context: Context, filename: String) {
			// TODO
		}

		fun shareGeoJsonFile(context: Context, filename: String) {
			// TODO
		}

		fun shareKmlFile(context: Context, filename: String) {
			// TODO
		}

		fun createNewFileSAF(activity: Activity, filename: String, mimeType: String, fileCode: Int) {

			val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
			intent.type = mimeType
			intent.putExtra(Intent.EXTRA_TITLE, filename)

			activity.startActivityForResult(intent, fileCode, null)
		}
	}
}