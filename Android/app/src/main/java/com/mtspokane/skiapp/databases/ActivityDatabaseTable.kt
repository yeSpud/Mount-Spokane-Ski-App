package com.mtspokane.skiapp.databases

class ActivityDatabaseTable(private val tableName: String) {

	val SQL_CREATE_ENTRIES: String = "CREATE TABLE ${this.tableName} (" +
			"$ACCURACY_COLUMN REAL NOT NULL," +
			"$ALTITUDE_COLUMN REAL NOT NULL," +
			"$ALTITUDE_ACCURACY_COLUMN REAL," +
			"$LATITUDE_COLUMN REAL NOT NULL," +
			"$LONGITUDE_COLUMN REAL NOT NULL," +
			"$SPEED_COLUMN REAL NOT NULL," +
			"$SPEED_ACCURACY_COLUMN REAL," +
			"$TIME_COLUMN INTEGER PRIMARY KEY NOT NULL" +
			")"

	companion object {

		const val ACCURACY_COLUMN = "accuracy"

		const val ALTITUDE_COLUMN = "altitude"

		const val ALTITUDE_ACCURACY_COLUMN = "altitude_accuracy"

		const val LATITUDE_COLUMN = "latitude"

		const val LONGITUDE_COLUMN = "longitude"

		const val SPEED_COLUMN = "speed"

		const val SPEED_ACCURACY_COLUMN = "speed_accuracy"

		// This should be unique.
		const val TIME_COLUMN = "time"
	}

	init {
		// TODO Make sure table name does not contain SQLite escape characters.
	}
}