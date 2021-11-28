package com.mtspokane.skiapp.mapItem

object MtSpokaneUIMapItems {

	var skiAreaBounds: UIMapItem? = null // FIXME Leaks memory!

	val other: Array<UIMapItem?> = arrayOfNulls(6) // TODO Account for parking lots and tubing area...

	var isSetup = false

	lateinit var chairlifts: Array<VisibleUIMapItem> // Should be size 6

	lateinit var easyRuns: Array<VisibleUIMapItem> // Should be size 22

	lateinit var moderateRuns: Array<VisibleUIMapItem> // Should be size 19

	lateinit var difficultRuns: Array<VisibleUIMapItem> // Should be size 25
}