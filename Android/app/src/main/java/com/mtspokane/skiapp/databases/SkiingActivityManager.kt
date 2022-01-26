package com.mtspokane.skiapp.databases

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import com.mtspokane.skiapp.R
import java.io.File
import java.io.OutputStream
import org.json.JSONArray
import org.json.JSONObject

object SkiingActivityManager {

	var InProgressActivities: Array<SkiingActivity> = emptyArray()

	var FinishedAndLoadedActivities: Array<SkiingActivity>? = null

	private const val ACCURACY = "acc"
	private const val ALTITUDE = "alt"
	private const val ALTITUDE_ACCURACY = "altacc"
	private const val LATITUDE = "lat"
	private const val LONGITUDE = "lng"
	private const val SPEED = "speed"
	private const val SPEED_ACCURACY = "speedacc"
	private const val TIME = "time"

	fun writeToExportFile(contentResolver: ContentResolver, uri: Uri, outText: String) {

		val outputStream: OutputStream? = contentResolver.openOutputStream(uri)
		outputStream?.use { it.write(outText.toByteArray()) }
	}

	fun jsonArrayToSkiingActivities(jsonArray: JSONArray): Array<SkiingActivity> {

		val count = jsonArray.length()

		val array: Array<SkiingActivity> = Array(count) {

			val jsonObject: JSONObject = jsonArray.getJSONObject(it)

			val accuracy: Float = parseFloat(jsonObject, this.ACCURACY)!!
			val altitude: Double = jsonObject.optDouble(this.ALTITUDE)
			val altitudeAccuracy: Float? = parseFloat(jsonObject, this.ALTITUDE_ACCURACY)
			val latitude: Double = jsonObject.optDouble(this.LATITUDE)
			val longitude: Double = jsonObject.optDouble(this.LONGITUDE)
			val speed: Float = parseFloat(jsonObject, this.SPEED)!!
			val speedAccuracy: Float? = parseFloat(jsonObject, this.SPEED_ACCURACY)
			val time: Long = jsonObject.optLong(this.TIME)

			SkiingActivity(accuracy, altitude, altitudeAccuracy, latitude, longitude, speed,
				speedAccuracy, time)
		}

		return array
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

	fun resumeActivityTracking(context: Context) {

		val date: String = TimeManager.getTodaysDate()

		val database = ActivityDatabase(context)

		this.InProgressActivities = database.readSkiingActivesFromDatabase(date)

		database.close()
	}

	fun convertSkiingActivitiesToJson(skiingActivities: Array<SkiingActivity>): JSONObject {

		if (skiingActivities.isEmpty()) {
			val emptyObject = JSONObject()
			val emptyJsonArray = JSONArray()
			emptyObject.put(TimeManager.getTodaysDate(), emptyJsonArray)
			return emptyObject
		}

		val date = TimeManager.getDateFromLong(skiingActivities[0].time)

		val jsonArray = JSONArray()
		skiingActivities.forEach {

			val activityObject = JSONObject()
			activityObject.put(this.ACCURACY, it.accuracy)
			activityObject.put(this.ALTITUDE, it.altitude)
			activityObject.put(this.ALTITUDE_ACCURACY, it.altitudeAccuracy)
			activityObject.put(this.LATITUDE, it.latitude)
			activityObject.put(this.LONGITUDE, it.longitude)
			activityObject.put(this.SPEED, it.speed)
			activityObject.put(this.SPEED_ACCURACY, it.speedAccuracy)
			activityObject.put(this.TIME, it.time)

			jsonArray.put(activityObject)
		}

		val jsonObject = JSONObject()
		jsonObject.put(date, jsonArray)
		return jsonObject
	}

	fun convertJsonToGeoJson(json: JSONObject): JSONObject {

		val geoJson = JSONObject()

		geoJson.put("type", "FeatureCollection")

		val key: String = json.keys().next()
		val jsonArray: JSONArray = json.getJSONArray(key)

		val featureArray = JSONArray()

		val count = jsonArray.length()
		for (i in 0 until count) {

			val featureEntry = JSONObject()
			featureEntry.put("type", "Feature")

			val jsonEntry: JSONObject = jsonArray.getJSONObject(i)

			val geometryJson = JSONObject()
			geometryJson.put("type", "Point")

			val coordinateJson = JSONArray()
			coordinateJson.put(0, jsonEntry.getDouble(this.LONGITUDE))
			coordinateJson.put(1, jsonEntry.getDouble(this.LATITUDE))
			coordinateJson.put(2, jsonEntry.getDouble(this.ALTITUDE))
			geometryJson.put("coordinates", coordinateJson)
			featureEntry.put("geometry", geometryJson)

			val propertiesJson = JSONObject()
			propertiesJson.put(this.ACCURACY, jsonEntry.opt(this.ACCURACY))
			propertiesJson.put(this.ALTITUDE_ACCURACY, jsonEntry.opt(this.ALTITUDE_ACCURACY))
			propertiesJson.put(this.SPEED, jsonEntry.opt(this.SPEED))
			propertiesJson.put(this.SPEED_ACCURACY, jsonEntry.opt(this.SPEED_ACCURACY))
			propertiesJson.put(this.TIME, jsonEntry.opt(this.TIME))
			featureEntry.put("properties", propertiesJson)

			featureArray.put(featureEntry)
		}

		geoJson.put("features", featureArray)

		return geoJson
	}

	fun shareFile(context: Context, file: File, mimeType: String) {

		val providerString = "${context.packageName}.provider"
		Log.v("shareFIle", "Provider string: $providerString")
		val fileUri: Uri = FileProvider.getUriForFile(context, providerString, file)

		val sharingIntent = Intent(Intent.ACTION_SEND)
		sharingIntent.type = mimeType
		sharingIntent.putExtra(Intent.EXTRA_STREAM, fileUri)

		val chooserIntent = Intent.createChooser(sharingIntent, context.getText(R.string.share_description))
		context.startActivity(chooserIntent)
	}
}