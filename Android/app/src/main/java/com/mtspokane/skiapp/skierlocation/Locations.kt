package com.mtspokane.skiapp.skierlocation

import com.mtspokane.skiapp.mapItem.MapItem

abstract class Locations<T> {

	var previousLocation: T? = null
		internal set

	var currentLocation: T? = null
		internal set

	var altitudeConfidence: UShort = 0u
		internal set

	var speedConfidence: UShort = 0u
		internal set

	var mostLikelyChairlift: MapItem? = null
		internal set

	abstract fun updateLocations(newVariable: T)

	abstract fun getVerticalDirection(): VerticalDirection

	abstract fun checkIfOnOther(): MapItem?

	abstract fun checkIfAtChairliftTerminals(): MapItem?

	abstract fun checkIfOnRun(): MapItem?

	abstract fun getSpeedConfidenceValue(): UShort

	enum class VerticalDirection {
		UP_CERTAIN, UP, FLAT, DOWN, DOWN_CERTAIN, UNKNOWN
	}
}