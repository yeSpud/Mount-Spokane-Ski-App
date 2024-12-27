package com.mtspokane.skiapp

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Relation
import androidx.room.RoomDatabase
import androidx.room.Transaction
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Entity(indices = [Index(value = ["time"], unique = true)])
data class SkiingActivity(
	@ColumnInfo(name = "accuracy") val accuracy: Float,
	@ColumnInfo(name = "altitude") val altitude: Double,
	@ColumnInfo(name = "altitude_accuracy") val altitudeAccuracy: Float?,
	@ColumnInfo(name = "latitude") val latitude: Double,
	@ColumnInfo(name = "longitude") val longitude: Double,
	@ColumnInfo(name = "speed") val speed: Float,
	@ColumnInfo(name = "speed_accuracy") val speedAccuracy: Float?,
	@PrimaryKey @ColumnInfo(name = "time") val time: Long,
	@ColumnInfo(name = "skiing_date_id") val skiingDateId: Int
)

@Entity(indices = [Index(value = ["shortDate"], unique = true)])
data class SkiingDate(@PrimaryKey(autoGenerate = true) val id: Int, val longDate: String, val shortDate: String)

data class LongAndShortDate(val longDate: String, val shortDate: String)

data class SkiingDateWithActivities(
	@Embedded val skiingDate: SkiingDate,
	@Relation(
		parentColumn = "id",
		entityColumn = "skiing_date_id",
		entity = SkiingActivity::class
	) val skiingActivities: List<SkiingActivity>
)

@Dao
interface SkiingActivityDao {

	@Query("SELECT * FROM SkiingActivity ORDER BY time")
	fun getAllSkiingActivities(): List<SkiingActivity>

	@Query("SELECT * FROM SkiingActivity WHERE skiing_date_id == :skiingDateId ORDER BY time")
	fun getActivitiesByDateId(skiingDateId: Int): List<SkiingActivity>

	@Insert(onConflict = OnConflictStrategy.ABORT)
	fun addSkiingActivity(skiingActivity: SkiingActivity)

	@Insert(onConflict = OnConflictStrategy.ABORT, entity = SkiingDate::class)
	fun addSkiingDate(skiingDate: LongAndShortDate)

	@Transaction
	@Query("SELECT * FROM SkiingDate ORDER BY shortDate")
	fun getAllSkiingDatesWithActivities(): List<SkiingDateWithActivities>

	@Transaction
	@Query("SELECT * FROM SkiingDate WHERE shortDate IS :shortDate ORDER BY shortDate")
	fun getSkiingDateWithActivitiesByShortDate(shortDate: String): SkiingDateWithActivities?
}

@Database(version = 1, entities = [SkiingActivity::class, SkiingDate::class])
abstract class Database : RoomDatabase() {
	abstract fun skiingActivityDao(): SkiingActivityDao

	companion object {
		const val NAME = "skiing-activity"

		fun getTodaysDate(): String {
			val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
			return dateFormat.format(Date())
		}

		fun getDateFromLong(date: Long): String {
			val dateFormat = SimpleDateFormat("LLLL dd yyyy", Locale.US)
			return dateFormat.format(date)
		}
	}
}