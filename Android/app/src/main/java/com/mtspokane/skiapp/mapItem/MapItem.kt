package com.mtspokane.skiapp.mapItem

import android.location.Location
import androidx.annotation.DrawableRes
import kotlin.math.PI
import kotlin.math.sin
import kotlin.math.tan

open class MapItem(val name: String, @DrawableRes private var icon: Int? = null)/*: Parcelable*/ {

	internal var points: Array<Array<Pair<Double, Double>>> = emptyArray()

	val hasPoints: Boolean = this.points.isNotEmpty()

	fun setIcon(@DrawableRes icon: Int) {
		this.icon = icon
	}

	fun getIcon(): Int? {
		return this.icon
	}

	/**
	 * Copyright 2008, 2013 Google Inc.
	 *
	 * Licensed under the Apache License, Version 2.0 (the "License");
	 * you may not use this file except in compliance with the License.
	 * You may obtain a copy of the License at
	 *
	 *      http://www.apache.org/licenses/LICENSE-2.0
	 *
	 * Unless required by applicable law or agreed to in writing, software
	 * distributed under the License is distributed on an "AS IS" BASIS,
	 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
	 * See the License for the specific language governing permissions and
	 * limitations under the License.
	 */
	fun locationInsidePoints(point: Location): Boolean {

		this.points.forEach {

			// Only continue if the array of points isn't empty...
			if (it.isNotEmpty()) {

				// Get the latitude and longitude of the location point.
				val locationLatitude: Double = Math.toRadians(point.latitude)
				val locationLongitude: Double = Math.toRadians(point.longitude)

				val prevLocation: Pair<Double, Double> = it[it.size - 1]
				var previousLatitude = Math.toRadians(prevLocation.first)
				var previousLongitude = Math.toRadians(prevLocation.second)
				var nIntersect = 0
				it.forEach { latLng ->
					val dLng3 = wrap(locationLongitude - previousLongitude)

					// Special case: point equal to vertex is inside.
					if (locationLatitude == previousLatitude && dLng3 == 0.0) {
						return true
					}
					val lat2 = Math.toRadians(latLng.first)
					val lng2 = Math.toRadians(latLng.second)

					// Offset longitudes by -lng1.
					if (intersects(previousLatitude, lat2, wrap(lng2 - previousLongitude), locationLatitude, dLng3)) {
						++nIntersect
					}
					previousLatitude = lat2
					previousLongitude = lng2
				}

				if ((nIntersect and 1) != 0) {
					return true
				}
			}
		}

		return false
	}

	/*
	 * Parcel stuff below.
	 */

	/*
	constructor(parcel: Parcel) : this(parcel.readString()!!, parcel.readValue(Int::class.java.classLoader) as? Int) {
		val readArray = parcel.readArray(Array::class.java.classLoader)
		try {
			val testArray = readArray as Array<Array<Pair<Double, Double>>>
			this.points = testArray
		} catch (e: ClassCastException) {
			e.printStackTrace()
		}
	}

	override fun writeToParcel(parcel: Parcel, flags: Int) {
		parcel.writeString(this.name)
		parcel.writeValue(this.icon)
		parcel.writeArray(this.points)
	}

	override fun describeContents(): Int {
		return this.hashCode()
	} */

	companion object {

		/*
		@JvmField
		val CREATOR = object: Parcelable.Creator<MapItem> {
			override fun createFromParcel(parcel: Parcel): MapItem {
				return MapItem(parcel)
			}

			override fun newArray(size: Int): Array<MapItem?> {
				return arrayOfNulls(size)
			}
		} */
		/*
		 * End parcel stuff.
		 */

		/**
		 * Copyright 2008, 2013 Google Inc.
		 *
		 * Licensed under the Apache License, Version 2.0 (the "License");
		 * you may not use this file except in compliance with the License.
		 * You may obtain a copy of the License at
		 *
		 *      http://www.apache.org/licenses/LICENSE-2.0
		 *
		 * Unless required by applicable law or agreed to in writing, software
		 * distributed under the License is distributed on an "AS IS" BASIS,
		 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
		 * See the License for the specific language governing permissions and
		 * limitations under the License.
		 */
		private fun wrap(n: Double): Double {

			return if (n >= -PI && n < PI) {
				n
			} else {
				val operand: Double = n + PI
				val modulus: Double = PI + PI
				(((operand % modulus) + modulus) % modulus) - PI
			}
		}

		/**
		 * Copyright 2008, 2013 Google Inc.
		 *
		 * Licensed under the Apache License, Version 2.0 (the "License");
		 * you may not use this file except in compliance with the License.
		 * You may obtain a copy of the License at
		 *
		 *      http://www.apache.org/licenses/LICENSE-2.0
		 *
		 * Unless required by applicable law or agreed to in writing, software
		 * distributed under the License is distributed on an "AS IS" BASIS,
		 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
		 * See the License for the specific language governing permissions and
		 * limitations under the License.
		 */
		private fun intersects(lat1: Double, lat2: Double, lng2: Double, lat3: Double, lng3: Double): Boolean {

			// Both ends on the same side of lng3.
			if ((lng3 >= 0 && lng3 >= lng2) || (lng3 < 0 && lng3 < lng2)) {
				return false
			}

			// Point is South Pole.
			if (lat3 <= -PI / 2) {
				return false
			}

			// Any segment end is a pole.
			if (lat1 <= -PI / 2 || lat2 <= -PI / 2 || lat1 >= PI / 2 || lat2 >= PI / 2) {
				return false
			}
			if (lng2 <= -PI) {
				return false
			}

			val linearLat: Double = (lat1 * (lng2 - lng3) + lat2 * lng3) / lng2

			// Northern hemisphere and point under lat-lng line.
			if (lat1 >= 0 && lat2 >= 0 && lat3 < linearLat) {
				return false
			}

			// Southern hemisphere and point above lat-lng line.
			if (lat1 <= 0 && lat2 <= 0 && lat3 >= linearLat) {
				return true
			}

			// North Pole.
			if (lat3 >= PI / 2) {
				return true
			}

			// Compare lat3 with latitude on the GC/Rhumb segment corresponding to lng3.
			// Compare through a strictly-increasing function (tan() or mercator()) as convenient.
			// http://williams.best.vwh.net/avform.htm
			return tan(lat3) >= (tan(lat1) * sin(lng2 - lng3) + tan(lat2) * sin(lng3)) / sin(lng2)
		}
	}
}