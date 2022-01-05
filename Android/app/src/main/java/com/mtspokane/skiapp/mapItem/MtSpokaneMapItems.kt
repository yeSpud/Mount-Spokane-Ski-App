package com.mtspokane.skiapp.mapItem

object MtSpokaneMapItems {

	var isSetup = false

	lateinit var skiAreaBounds: UIMapItem

	lateinit var other: Array<UIMapItem> // Should be 9

	lateinit var chairliftTerminals: Array<UIMapItem> // Should be size 6

	lateinit var chairlifts: Array<VisibleUIMapItem> // Should be size 6

	lateinit var easyRuns: Array<VisibleUIMapItem> // Should be size 22

	lateinit var moderateRuns: Array<VisibleUIMapItem> // Should be size 19

	lateinit var difficultRuns: Array<VisibleUIMapItem> // Should be size 25

	fun destroyUIItems() {

		this.skiAreaBounds.destroyUIItems()
		this.other.forEach {
			it.destroyUIItems()
		}
		val visibleArrays: Array<Array<UIMapItem>> = arrayOf(this.chairliftTerminals,
			this.chairlifts as Array<UIMapItem>, this.easyRuns as Array<UIMapItem>,
			this.moderateRuns as Array<UIMapItem>, this.difficultRuns as Array<UIMapItem>)
		visibleArrays.forEach { array ->
			array.forEach {
				it.destroyUIItems()
			}
		}

		this.isSetup = false
	}
}