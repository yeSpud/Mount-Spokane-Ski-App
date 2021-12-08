package com.mtspokane.skiapp.mapItem

object MtSpokaneMapItems {

	var skiAreaBounds: UIMapItem? = null

	val other: Array<UIMapItem?> = arrayOfNulls(9)

	var isSetup = false

	lateinit var chairlifts: Array<VisibleUIMapItem> // Should be size 6

	lateinit var easyRuns: Array<VisibleUIMapItem> // Should be size 22

	lateinit var moderateRuns: Array<VisibleUIMapItem> // Should be size 19

	lateinit var difficultRuns: Array<VisibleUIMapItem> // Should be size 25
}