package com.mtspokane.skiapp.skierlocation

import com.mtspokane.skiapp.mapItem.MapMarker

abstract class Locations<T> {

	var previousLocation: T? = null
		internal set

	var currentLocation: T? = null
		internal set

	var altitudeConfidence: UShort = 0u
		internal set

	var speedConfidence: UShort = 0u
		internal set

	abstract fun updateLocations(newVariable: T)

	abstract fun getVerticalDirection(): VerticalDirection

	abstract fun checkIfOnOther(): MapMarker?

	abstract fun checkIfIOnChairlift(): MapMarker?

	abstract fun checkIfAtChairliftTerminals(): MapMarker?

	abstract fun checkIfOnRun(): MapMarker?

	abstract fun getSpeedConfidenceValue(): UShort

	enum class VerticalDirection {
		UP_CERTAIN, UP, FLAT, DOWN, DOWN_CERTAIN, UNKNOWN
	}
}