package com.mtspokane.skiapp.skiingactivity

import android.location.Location
import android.os.Build
import android.os.Parcel
import android.os.Parcelable

class SkiingActivity : Parcelable {

	val accuracy: Float

	val altitude: Double

	val altitudeAccuracy: Float?

	val latitude: Double

	val longitude: Double

	val speed: Float

	val speedAccuracy: Float?

	val time: Long

	constructor(parcel: Parcel): this(parcel.readFloat(), parcel.readDouble(),
		parcel.readValue(Float::class.java.classLoader) as Float?, parcel.readDouble(), parcel.readDouble(),
		parcel.readFloat(), parcel.readValue(Float::class.java.classLoader) as Float?, parcel.readLong())

	constructor(location: Location) {

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

	constructor(accuracy: Float, altitude: Double, altitudeAccuracy: Float?, latitude: Double,
	            longitude: Double, speed: Float, speedAccuracy: Float?, time: Long) {

		this.accuracy = accuracy
		this.altitude = altitude
		this.altitudeAccuracy = altitudeAccuracy
		this.latitude = latitude
		this.longitude = longitude
		this.speed = speed
		this.speedAccuracy = speedAccuracy
		this.time = time
	}

	override fun writeToParcel(parcel: Parcel, flags: Int) {
		parcel.writeFloat(this.accuracy)
		parcel.writeDouble(this.altitude)
		parcel.writeValue(this.altitudeAccuracy)
		parcel.writeDouble(this.latitude)
		parcel.writeDouble(this.longitude)
		parcel.writeFloat(this.speed)
		parcel.writeValue(this.speedAccuracy)
		parcel.writeLong(this.time)
	}

	override fun describeContents(): Int {
		return this.hashCode()
	}

	companion object CREATOR : Parcelable.Creator<SkiingActivity> {

		override fun createFromParcel(parcel: Parcel): SkiingActivity {
			return SkiingActivity(parcel)
		}

		override fun newArray(size: Int): Array<SkiingActivity?> {
			return arrayOfNulls(size)
		}
	}
}