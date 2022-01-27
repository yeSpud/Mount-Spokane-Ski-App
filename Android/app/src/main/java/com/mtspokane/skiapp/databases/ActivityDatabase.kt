package com.mtspokane.skiapp.databases

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import androidx.core.database.getFloatOrNull
import java.io.File
import java.io.FileInputStream
import org.json.JSONObject

class ActivityDatabase(val context: Context): SQLiteOpenHelper(context, this.DATABASE_NAME, null,
	this.DATABASE_VERSION) {

	override fun onCreate(db: SQLiteDatabase) {

		// First check for legacy files (json files).
		if (this.hasLegacyFiles()) {

			// Import legacy files to database.
			this.context.filesDir.listFiles()?.forEach {
				if (it.name.contains(Regex("\\d\\d\\d\\d-\\d\\d-\\d\\d"))) {
					val legacyJson: JSONObject = this.readJsonFromFile(it.name)
					importJsonToDatabase(legacyJson, db)
					it.delete()
				}
			}
		} else {

			// Create an entry with today's date.
			val initialTable = ActivityDatabaseTable(TimeManager.getTodaysDate())
			db.execSQL(initialTable.SQL_CREATE_ENTRIES)
		}
	}

	override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
		Log.w("ActivityDatabase", "Database needs to be upgraded from $oldVersion to $newVersion")
		// TODO
	}

	private fun hasLegacyFiles(): Boolean {

		val files: Array<out File> = this.context.filesDir.listFiles() ?: return false

		files.forEach {
			if (it.isFile && it.name.endsWith(".json")) {
				return true
			}
		}

		return false
	}

	private fun readJsonFromFile(filename: String): JSONObject {

		var json = JSONObject()
		val fileInputStream: FileInputStream = this.context.openFileInput(filename)
		fileInputStream.bufferedReader().useLines {

			val string = it.fold("") { _, inText -> inText }

			json = JSONObject(string)
		}
		fileInputStream.close()

		return json
	}

	companion object {

		// If you change the database schema, you must increment the database version.
		const val DATABASE_VERSION = 1
		const val DATABASE_NAME = "Activities.db"

		private const val NAME_COLUMN = "NAME"

		fun importJsonToDatabase(jsonObject: JSONObject, database: SQLiteDatabase) {

			val date: String = jsonObject.keys().next()

			// Test if the date variable is in the proper date format before continuing.
			if (TimeManager.isValidDateFormat(date)) {
				val skiingActivities: Array<SkiingActivity> = SkiingActivityManager
					.jsonArrayToSkiingActivities(jsonObject.getJSONArray(date))

				this.writeSkiingActivitiesToDatabase(skiingActivities, database, date)
			} else {
				Log.w("importJsonToDatabase", "Invalid date: $date")
			}
		}

		fun writeSkiingActivitiesToDatabase(skiingActivities: Array<SkiingActivity>, database: SQLiteDatabase,
		                                    date: String = TimeManager.getTodaysDate()) {

			// Create table if it doest exist.
			if (!this.getTables(database).contains(date)) {
				val initialTable = ActivityDatabaseTable(date)
				database.execSQL(initialTable.SQL_CREATE_ENTRIES)
			}

			skiingActivities.forEach { activity: SkiingActivity ->

				val values = ContentValues().apply {

					this.put(ActivityDatabaseTable.ACCURACY_COLUMN, activity.accuracy)
					this.put(ActivityDatabaseTable.ALTITUDE_COLUMN, activity.altitude)
					if (activity.altitudeAccuracy != null) {
						this.put(ActivityDatabaseTable.ALTITUDE_ACCURACY_COLUMN, activity.altitudeAccuracy)
					}
					this.put(ActivityDatabaseTable.LATITUDE_COLUMN, activity.latitude)
					this.put(ActivityDatabaseTable.LONGITUDE_COLUMN, activity.longitude)
					this.put(ActivityDatabaseTable.SPEED_COLUMN, activity.speed)
					if (activity.speedAccuracy != null) {
						this.put(ActivityDatabaseTable.SPEED_ACCURACY_COLUMN, activity.speedAccuracy)
					}
					Log.d("writeToDatabase", "Time: ${activity.time}")
					this.put(ActivityDatabaseTable.TIME_COLUMN, activity.time)
				}

				database.insertWithOnConflict("'$date'", null, values, SQLiteDatabase.CONFLICT_IGNORE)
			}
		}

		fun getTables(database: SQLiteDatabase): Array<String> {

			val dates = mutableListOf<String>()

			val result = database.query("sqlite_master", arrayOf(this.NAME_COLUMN), "type=?",
				arrayOf("table"), null, null, "${this.NAME_COLUMN} ASC")

			with(result) {

				val nameColumn: Int = this.getColumnIndex("name")

				while (this.moveToNext()) {
					val date = this.getString(nameColumn)

					if (TimeManager.isValidDateFormat(date)) {
						dates.add(date)
					}
				}

			}
			result.close()

			return dates.toTypedArray()
		}

		fun readSkiingActivesFromDatabase(date: String, database: SQLiteDatabase): Array<SkiingActivity> {

			val tag = "readActivesFromDatabase"

			if (!TimeManager.isValidDateFormat(date)) {
				Log.w(tag, "Invalid date: $date")
				return emptyArray()
			}

			if (!this.getTables(database).contains(date)) {
				Log.i(tag, "No entry for $date")
				return emptyArray()
			}

			val allColumns: Array<String> = arrayOf(ActivityDatabaseTable.ACCURACY_COLUMN,
				ActivityDatabaseTable.ALTITUDE_COLUMN, ActivityDatabaseTable.ALTITUDE_ACCURACY_COLUMN,
				ActivityDatabaseTable.LATITUDE_COLUMN, ActivityDatabaseTable.LONGITUDE_COLUMN,
				ActivityDatabaseTable.SPEED_COLUMN, ActivityDatabaseTable.SPEED_ACCURACY_COLUMN,
				ActivityDatabaseTable.TIME_COLUMN)

			val result: Cursor = database.query("'$date'", allColumns, null,
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
	}
}