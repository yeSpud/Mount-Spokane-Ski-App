package com.mtspokane.skiapp.databases

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import androidx.core.database.getFloatOrNull
import org.json.JSONObject

class ActivityDatabase(context: Context): SQLiteOpenHelper(context, this.DATABASE_NAME, null,
	this.DATABASE_VERSION) {

	override fun onCreate(db: SQLiteDatabase) {

		// Create an entry with today's date.
		val initialTable = ActivityDatabaseTable(TimeManager.getTodaysDate())

		db.execSQL(initialTable.SQL_CREATE_ENTRIES)
	}

	override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
		Log.w("ActivityDatabase", "Database needs to be upgraded from $oldVersion to $newVersion")
		// TODO
	}

	fun importJsonToDatabase(jsonObject: JSONObject) {

		val date: String = jsonObject.keys().next()

		// Test if the date variable is in the proper date format before continuing.
		if (TimeManager.isValidDateFormat(date)) {
			val skiingActivities: Array<SkiingActivity> = SkiingActivityManager
				.jsonArrayToSkiingActivities(jsonObject.getJSONArray(date))

			this.writeSkiingActivitiesToDatabase(skiingActivities, date)
		} else {
			Log.w("importJsonToDatabase", "Invalid date: $date")
		}
	}

	fun writeSkiingActivitiesToDatabase(skiingActivities: Array<SkiingActivity>, date: String =
		TimeManager.getTodaysDate()) {

		skiingActivities.forEach { activity ->

			val values = ContentValues().apply {

				put(ActivityDatabaseTable.ACCURACY_COLUMN, activity.accuracy)
				put(ActivityDatabaseTable.ALTITUDE_COLUMN, activity.altitude)
				if (activity.altitudeAccuracy != null) {
					put(ActivityDatabaseTable.ALTITUDE_ACCURACY_COLUMN, activity.altitudeAccuracy)
				}
				put(ActivityDatabaseTable.LATITUDE_COLUMN, activity.latitude)
				put(ActivityDatabaseTable.LONGITUDE_COLUMN, activity.longitude)
				put(ActivityDatabaseTable.SPEED_COLUMN, activity.speed)
				if (activity.speedAccuracy != null) {
					put(ActivityDatabaseTable.SPEED_ACCURACY_COLUMN, activity.speedAccuracy)
				}
				put(ActivityDatabaseTable.TIME_COLUMN, activity.time)

			}

			this.writableDatabase.insertWithOnConflict(date, null, values,
				SQLiteDatabase.CONFLICT_FAIL)

		}
	}

	fun readSkiingActivesFromDatabase(date: String): Array<SkiingActivity> {

		if (TimeManager.isValidDateFormat(date)) {
			Log.w("readActivesFromDatabase", "Invalid date: $date")
			return emptyArray()
		}

		val allColumns: Array<String> = arrayOf(ActivityDatabaseTable.ACCURACY_COLUMN,
			ActivityDatabaseTable.ALTITUDE_COLUMN, ActivityDatabaseTable.ALTITUDE_ACCURACY_COLUMN,
			ActivityDatabaseTable.LATITUDE_COLUMN, ActivityDatabaseTable.LONGITUDE_COLUMN,
			ActivityDatabaseTable.SPEED_COLUMN, ActivityDatabaseTable.SPEED_ACCURACY_COLUMN,
			ActivityDatabaseTable.TIME_COLUMN)

		val result: Cursor = this.readableDatabase.query(date, allColumns, null,
			null, null, null, "${ActivityDatabaseTable.TIME_COLUMN} ASC")

		val skiingActivitiesList = mutableListOf<SkiingActivity>()
		with(result) {

			val accuracyColumn: Int = this.getColumnIndex(ActivityDatabaseTable.ACCURACY_COLUMN)
			val altitudeColumn: Int = this.getColumnIndex(ActivityDatabaseTable.ALTITUDE_COLUMN)
			val altitudeAccuracyColumn: Int = this.getColumnIndex(ActivityDatabaseTable.ALTITUDE_ACCURACY_COLUMN)
			val latitudeColumn: Int = this.getColumnIndex(ActivityDatabaseTable.LATITUDE_COLUMN)
			val longitudeColumn: Int = this.getColumnIndex(ActivityDatabaseTable.LONGITUDE_COLUMN)
			val speedColumn: Int = this.getColumnIndex(ActivityDatabaseTable.SPEED_COLUMN)
			val speedAccuracyColumn: Int = this.getColumnIndex(ActivityDatabaseTable.SPEED_ACCURACY_COLUMN)
			val timeColumn: Int = this.getColumnIndex(ActivityDatabaseTable.TIME_COLUMN)

			while (this.moveToNext()) {

				val accuracy: Float = this.getFloat(accuracyColumn)
				val altitude: Double = this.getDouble(altitudeColumn)
				val altitudeAccuracy: Float? = this.getFloatOrNull(altitudeAccuracyColumn)
				val latitude: Double = this.getDouble(latitudeColumn)
				val longitude: Double = this.getDouble(longitudeColumn)
				val speed: Float = this.getFloat(speedColumn)
				val speedAccuracy: Float? = this.getFloatOrNull(speedAccuracyColumn)
				val time: Long = this.getLong(timeColumn)

				val skiingActivity = SkiingActivity(accuracy, altitude, altitudeAccuracy, latitude,
					longitude, speed, speedAccuracy, time)
				skiingActivitiesList.add(skiingActivity)
			}
		}
		result.close()

		return skiingActivitiesList.toTypedArray()
	}

	companion object {

		// If you change the database schema, you must increment the database version.
		const val DATABASE_VERSION = 1
		const val DATABASE_NAME = "Activities.db"
	}
}