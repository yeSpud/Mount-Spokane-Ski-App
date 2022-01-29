package com.mtspokane.skiapp.databases

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [Elevation::class], version = 1)
abstract class ElevationDatabase: RoomDatabase() {

	abstract fun elevationDao(): ElevationDao

}