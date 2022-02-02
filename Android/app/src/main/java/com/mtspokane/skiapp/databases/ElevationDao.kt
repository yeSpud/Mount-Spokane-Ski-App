package com.mtspokane.skiapp.databases

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface ElevationDao {

	@Insert
	fun addAll(vararg elevationsEntries: Elevation)

	@Query("SELECT * FROM Elevation")
	fun getAll(): List<Elevation>

	@Query("SELECT elevation FROM Elevation WHERE latitude = :latitude AND longitude = :longitude")
	fun getElevationAtLocation(latitude: Double, longitude: Double): Double?

}