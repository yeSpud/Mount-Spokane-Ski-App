package com.mtspokane.skiapp.skiingactivity

import android.location.Location
import android.os.Build

class SkiingActivity {

	val accuracy: Float

	val altitude: Double

	val altitudeAccuracy: Float?

	val latitude: Double

	val longitude: Double

	val speed: Float

	val speedAccuracy: Float?

	val time: Long

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
}