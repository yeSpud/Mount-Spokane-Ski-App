package com.mtspokane.skiapp.databases

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import org.json.JSONObject

class ActivityDatabase(context: Context): SQLiteOpenHelper(context, this.DATABASE_NAME, null,
	this.DATABASE_VERSION) {

	override fun onCreate(db: SQLiteDatabase) {

		// Create an entry with today's date.
		val initialTable = ActivityDatabaseTable(SkiingActivityManager.getTodaysDate())

		db.execSQL(initialTable.SQL_CREATE_ENTRIES)
	}

	override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
		Log.w("ActivityDatabase", "Database needs to be upgraded from $oldVersion to $newVersion")
		// TODO
	}

	fun addJsonFileToDB(jsonObject: JSONObject) {

		val date: String = jsonObject.keys().next()

		// TODO Add regex for testing if the date variable is in the proper date format before continuing.
		val skiingActivities: Array<SkiingActivity> = SkiingActivityManager
			.jsonArrayToSkiingActivities(jsonObject.getJSONArray(date))

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

	companion object {

		// If you change the database schema, you must increment the database version.
		const val DATABASE_VERSION = 1
		const val DATABASE_NAME = "Activities.db"
	}
}