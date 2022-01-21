package com.mtspokane.skiapp.mapItem

import android.content.ContextWrapper
import android.util.Log
import com.google.android.gms.maps.model.Polygon
import com.mtspokane.skiapp.R
import com.mtspokane.skiapp.maphandlers.MapHandler
import kotlin.reflect.KClass
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

object MtSpokaneMapItems {

	private val classesUsingObject: ArrayList<KClass<out ContextWrapper>> = ArrayList(0)

	var skiAreaBounds: UIMapItem? = null
	private set

	var other: Array<UIMapItem>? = null // Should be 9
	private set

	var chairliftTerminals: Array<UIMapItem>? = null // Should be size 6
	private set

	var chairlifts: Array<VisibleUIMapItem>? = null // Should be size 6
	private set

	var easyRuns: Array<VisibleUIMapItem>? = null // Should be size 22
	private set

	var moderateRuns: Array<VisibleUIMapItem>? = null // Should be size 19
	private set

	var difficultRuns: Array<VisibleUIMapItem>? = null // Should be size 25
	private set

	fun checkoutObject(classUsingObject: KClass<out ContextWrapper>) {
		if (!this.classesUsingObject.contains(classUsingObject)) {
			this.classesUsingObject.add(classUsingObject)
		}
	}

	/**
	 * lodges, parking lots, vista house, tubing area, yurt, ski patrol building, and ski area bounds...
	 */
	suspend fun initializeOtherAsync(classUsingObject: KClass<out ContextWrapper>, mapHandler: MapHandler):
			Deferred<Int> = coroutineScope {

		this@MtSpokaneMapItems.checkoutObject(classUsingObject)
		val tag = "initializeOtherAsync"

		return@coroutineScope async(Dispatchers.IO) {

			Log.v(tag, "Started loading other polygons")

			val hashmap: HashMap<String, Array<Polygon>> = mapHandler.loadPolygons(R.raw.other,
				R.color.other_polygon_fill, false)

			val skiAreaBoundsKeyName = "Ski Area Bounds"
			val bounds: Array<Polygon>? = hashmap[skiAreaBoundsKeyName]

			withContext(Dispatchers.Main) {
				if (bounds != null) {

					this@MtSpokaneMapItems.skiAreaBounds = UIMapItem(skiAreaBoundsKeyName, bounds[0])
					hashmap.remove(skiAreaBoundsKeyName)
				}

				val names: Array<String> = hashmap.keys.toTypedArray()
				this@MtSpokaneMapItems.other = Array(9) {

					val uiMapItemPolygons: Array<Polygon>? = hashmap[names[it]]

					val icon: Int? = when (names[it]) {
						"Lodge 1" -> R.drawable.ic_missing // TODO Lodge icon
						"Lodge 2" -> R.drawable.ic_missing // TODO Lodge icon
						"Yurt" -> R.drawable.ic_yurt
						"Vista House" -> R.drawable.ic_missing // TODO Vista house icon
						"Ski Patrol Building" -> R.drawable.ic_ski_patrol_icon
						"Lodge 1 Parking Lot" -> R.drawable.ic_parking
						"Lodge 2 Parking Lot" -> R.drawable.ic_parking
						"Tubing Area" -> R.drawable.ic_missing // TODO Tubing area icon
						"Ski School" -> R.drawable.ic_missing // TODO Ski school icon
						else -> {
							Log.w(tag, "${names[it]} does not have an icon")
							null
						}
					}

					if (uiMapItemPolygons != null) {
						UIMapItem(names[it], uiMapItemPolygons[0], icon)
					} else {
						Log.w(tag, "No polygon for ${names[it]}")
						UIMapItem(names[it])
					}
				}
			}

			Log.v(tag, "Finished loading other polygons")
		}
	}

	suspend fun initializeChairliftTerminalsAsync(classUsingObject: KClass<out ContextWrapper>, mapHandler: MapHandler):
			Deferred<Int> = coroutineScope {

		this@MtSpokaneMapItems.checkoutObject(classUsingObject)
		val tag = "ChairliftTerminalsAsync"

		return@coroutineScope async(Dispatchers.IO) {

			Log.v(tag, "Started loading chairlift terminal polylines")

			val hashmap: HashMap<String, Array<Polygon>> = mapHandler.loadPolygons(R.raw.lift_terminal_polygons,
				R.color.chairlift_polygon)

			val names: Array<String> = hashmap.keys.toTypedArray()
			withContext(Dispatchers.Main) {
				this@MtSpokaneMapItems.chairliftTerminals = Array(6) {

					val polygonArray: Array<Polygon> = hashmap[names[it]]!!

					val uiMapItem = UIMapItem(names[it], polygonArray[0], R.drawable.ic_chairlift)
					uiMapItem.addAdditionalPolygon(polygonArray[1])
					uiMapItem
				}
			}

			Log.v(tag, "Finished loading chairlift terminal polylines")
		}
	}

	suspend fun initializeChairliftsAsync(classUsingObject: KClass<out ContextWrapper>, mapHandler: MapHandler):
			Deferred<Int> = coroutineScope {

		this@MtSpokaneMapItems.checkoutObject(classUsingObject)
		val tag = "initializeChairlifts"

		return@coroutineScope async(Dispatchers.IO) {
			Log.v(tag, "Started loading chairlift polylines")
			this@MtSpokaneMapItems.chairlifts = mapHandler.loadPolylines(R.raw.lifts, R.color.chairlift,
				4f, R.drawable.ic_chairlift)
			Log.v(tag, "Finished loading chairlift polylines")
		}
	}

	suspend fun addChairliftPolygonsAsync(classUsingObject: KClass<out ContextWrapper>, mapHandler: MapHandler):
			Deferred<Int> = coroutineScope {

		this@MtSpokaneMapItems.checkoutObject(classUsingObject)
		val tag = "addChairliftPolygons"

		return@coroutineScope async(Dispatchers.IO) {

			Log.v(tag, "Started loading chairlift polygons")
			val hashmap: HashMap<String, Array<Polygon>> = mapHandler.loadPolygons(R.raw.lift_polygons,
				R.color.chairlift_polygon)

			if (this@MtSpokaneMapItems.chairlifts != null) {
				withContext(Dispatchers.Main) {
					this@MtSpokaneMapItems.chairlifts!!.forEach { visibleMapItem: VisibleUIMapItem ->

						val polygons: Array<Polygon>? = hashmap[visibleMapItem.name]
						polygons?.forEach {
							visibleMapItem.addAdditionalPolygon(it)
						}
					}
				}

				Log.v(tag, "Finished loading chairlift polygons")
			} else {

				Log.w(tag, "Unable to load chairlift polygons")
			}
		}
	}

	suspend fun initializeEasyRunsAsync(classUsingObject: KClass<out ContextWrapper>, mapHandler: MapHandler):
			Deferred<Int> = coroutineScope {

		this@MtSpokaneMapItems.checkoutObject(classUsingObject)
		val tag = "initializeEasyRunsAsync"

		return@coroutineScope async(Dispatchers.IO) {
			Log.v(tag, "Started loading easy polylines")
			this@MtSpokaneMapItems.easyRuns = mapHandler.loadPolylines(R.raw.easy, R.color.easy,
				3f, R.drawable.ic_easy)
			Log.v(tag, "Finished loading easy run polylines")
		}
	}

	suspend fun addEasyPolygonsAsync(classUsingObject: KClass<out ContextWrapper>, mapHandler: MapHandler):
			Deferred<Int> = coroutineScope {

		this@MtSpokaneMapItems.checkoutObject(classUsingObject)
		val tag = "addEasyPolygonsAsync"

		return@coroutineScope async(Dispatchers.IO) {
			Log.v(tag, "Started loading easy polygons")

			val hashmap: HashMap<String, Array<Polygon>> = mapHandler.loadPolygons(R.raw.easy_polygons,
				R.color.easy_polygon)

			if (this@MtSpokaneMapItems.easyRuns != null) {
				withContext(Dispatchers.Main) {
					this@MtSpokaneMapItems.easyRuns!!.forEach { visibleUIMapItem: VisibleUIMapItem ->

						val polygons: Array<Polygon>? = hashmap[visibleUIMapItem.name]
						polygons?.forEach {
							visibleUIMapItem.addAdditionalPolygon(it)
						}
					}
				}

				Log.v(tag, "Finished loading easy polygons")
			} else {

				Log.w(tag, "Unable to load easy polygons")
			}
		}
	}

	suspend fun initializeModerateRunsAsync(classUsingObject: KClass<out ContextWrapper>, mapHandler: MapHandler):
			Deferred<Int> = coroutineScope {

		this@MtSpokaneMapItems.checkoutObject(classUsingObject)
		val tag = "initializeModerateRuns"

		return@coroutineScope async(Dispatchers.IO) {
			Log.v(tag, "Started loading moderate polylines")
			this@MtSpokaneMapItems.moderateRuns = mapHandler.loadPolylines(R.raw.moderate, R.color.moderate,
				2f, R.drawable.ic_moderate)
			Log.v(tag, "Finished loading moderate run polylines")
		}
	}

	suspend fun addModeratePolygonsAsync(classUsingObject: KClass<out ContextWrapper>, mapHandler: MapHandler):
			Deferred<Int> = coroutineScope {

		this@MtSpokaneMapItems.checkoutObject(classUsingObject)
		val tag = "addModeratePolygons"

		return@coroutineScope async(Dispatchers.IO) {
			Log.v(tag, "Started loading moderate polygons")
			val hashmap: HashMap<String, Array<Polygon>> = mapHandler.loadPolygons(R.raw.moderate_polygons,
				R.color.moderate_polygon)

			if (this@MtSpokaneMapItems.moderateRuns != null) {
				withContext(Dispatchers.Main) {
					this@MtSpokaneMapItems.moderateRuns!!.forEach { visibleUIMapItem: VisibleUIMapItem ->

						val polygons: Array<Polygon>? = hashmap[visibleUIMapItem.name]
						polygons?.forEach {
							visibleUIMapItem.addAdditionalPolygon(it)
						}
					}
				}

				Log.v(tag, "Finished loading moderate polygons")
			} else {

				Log.w(tag, "Unable to load moderate polygons")
			}
		}
	}

	suspend fun initializeDifficultRunsAsync(classUsingObject: KClass<out ContextWrapper>, mapHandler: MapHandler):
			Deferred<Int> = coroutineScope {

		this@MtSpokaneMapItems.checkoutObject(classUsingObject)
		val tag = "initializeDifficultRuns"

		return@coroutineScope async(Dispatchers.IO) {
			Log.v(tag, "Started loading difficult polylines")
			this@MtSpokaneMapItems.difficultRuns = mapHandler.loadPolylines(R.raw.difficult, R.color.difficult,
				1f, R.drawable.ic_difficult)
			Log.v(tag, "Finished loading difficult polylines")
		}
	}

	suspend fun addDifficultPolygonsAsync(classUsingObject: KClass<out ContextWrapper>, mapHandler: MapHandler):
			Deferred<Int> = coroutineScope {

		this@MtSpokaneMapItems.checkoutObject(classUsingObject)
		val tag = "addDifficultPolygons"

		return@coroutineScope async(Dispatchers.IO) {
			Log.v(tag, "Started loading difficult polygons")
			val hashmap: HashMap<String, Array<Polygon>> = mapHandler.loadPolygons(R.raw.difficult_polygons,
				R.color.difficult_polygon)

			if (this@MtSpokaneMapItems.difficultRuns != null) {
				withContext(Dispatchers.Main) {
					this@MtSpokaneMapItems.difficultRuns!!.forEach { visibleUIMapItem: VisibleUIMapItem ->

						val polygons: Array<Polygon>? = hashmap[visibleUIMapItem.name]
						polygons?.forEach {
							visibleUIMapItem.addAdditionalPolygon(it)
						}
					}
				}

				Log.v(tag, "Finished loading difficult polygons")
			} else {

				Log.w(tag, "Unable to load difficult polygons")
			}
		}
	}

	fun destroyUIItems(classUsingObject: KClass<out ContextWrapper>) {

		this.classesUsingObject.remove(classUsingObject)
		if (this.classesUsingObject.isNotEmpty()) {
			return
		}

		this.skiAreaBounds?.destroyUIItems()

		val mapItems: Array<Array<UIMapItem>?> = arrayOf(this.other, this.chairliftTerminals,
			this.chairlifts as Array<UIMapItem>?, this.easyRuns as Array<UIMapItem>?,
			this.moderateRuns as Array<UIMapItem>?, this.difficultRuns as Array<UIMapItem>?)
		mapItems.forEach { array ->
			array?.forEach {
				it.destroyUIItems()
			}
		}

		this.other = null
		this.chairliftTerminals = null
		this.chairlifts = null
		this.easyRuns = null
		this.moderateRuns = null
		this.difficultRuns = null

		Log.d("destroyUIItems", "Finished clearing UI Items")
	}
}