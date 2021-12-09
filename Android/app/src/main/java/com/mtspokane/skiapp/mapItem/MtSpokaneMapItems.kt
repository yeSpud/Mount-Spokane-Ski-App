package com.mtspokane.skiapp.mapItem

object MtSpokaneMapItems {

	var skiAreaBounds: UIMapItem? = null

	val other: Array<UIMapItem?> = arrayOfNulls(9)

	var isSetup = false

	lateinit var chairlifts: Array<VisibleUIMapItem> // Should be size 6

	lateinit var easyRuns: Array<VisibleUIMapItem> // Should be size 22

	lateinit var moderateRuns: Array<VisibleUIMapItem> // Should be size 19

	lateinit var difficultRuns: Array<VisibleUIMapItem> // Should be size 25

	fun destroyUIItems() {

		this.skiAreaBounds?.destroyUIItems()
		this.other.forEach {
			it?.destroyUIItems()
		}
		val visibleArrays: Array<Array<VisibleUIMapItem>> = arrayOf(this.chairlifts, this.easyRuns,
			this.moderateRuns, this.difficultRuns)
		visibleArrays.forEach { array ->
			array.forEach {
				it.destroyUIItems()
			}
		}

		this.isSetup = false
	}

	fun reset() {

		this.destroyUIItems()

		for (i in this.other.indices) {
			this.other[i] = null
		}

		this.skiAreaBounds = null
	}

}