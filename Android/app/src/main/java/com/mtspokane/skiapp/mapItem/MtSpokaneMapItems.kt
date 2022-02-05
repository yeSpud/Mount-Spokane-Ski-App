package com.mtspokane.skiapp.mapItem

import android.content.ContextWrapper
import android.util.Log
import com.google.android.gms.maps.model.LatLng
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

	private val classesUsingObject: MutableList<KClass<out ContextWrapper>> = mutableListOf()

	var skiAreaBounds: PolygonMapItem? = null
	private set

	var other: List<PolygonMapItem>? = null // Should be 9
	private set

	var chairliftTerminals: List<PolygonMapItem>? = null // Should be size 6
	private set

	var chairlifts: List<PolylineMapItem>? = null // Should be size 6
	private set

	var easyRuns: List<PolylineMapItem>? = null // Should be size 22
	private set

	var moderateRuns: List<PolylineMapItem>? = null // Should be size 19
	private set

	var difficultRuns: List<PolylineMapItem>? = null // Should be size 25
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

			val hashmap: HashMap<String, List<Polygon>> = mapHandler.loadPolygons(R.raw.other,
				R.color.other_polygon_fill, false)

			val skiAreaBoundsKeyName = "Ski Area Bounds"
			val bounds: List<Polygon>? = hashmap[skiAreaBoundsKeyName]

			withContext(Dispatchers.Main) {
				if (bounds != null) {

					val boundsPoints: MutableList<List<LatLng>> = mutableListOf()
					bounds.forEach {
						boundsPoints.add(it.points)
					}

					this@MtSpokaneMapItems.skiAreaBounds = PolygonMapItem(skiAreaBoundsKeyName,
						bounds.toMutableList(), boundsPoints)
					hashmap.remove(skiAreaBoundsKeyName)
				}
			}

			val othersList: MutableList<PolygonMapItem> = mutableListOf()

			hashmap.keys.forEach {

				val uiMapItemPolygons: List<Polygon>? = hashmap[it]

				val icon: Int? = when (it) {
					"Lodge 1" -> R.drawable.ic_lodge
					"Lodge 2" -> R.drawable.ic_lodge
					"Yurt" -> R.drawable.ic_yurt
					"Vista House" -> R.drawable.ic_vista_house
					"Ski Patrol Building" -> R.drawable.ic_ski_patrol_icon
					"Lodge 1 Parking Lot" -> R.drawable.ic_parking
					"Lodge 2 Parking Lot" -> R.drawable.ic_parking
					"Tubing Area" -> R.drawable.ic_missing // TODO Tubing area icon
					"Ski School" -> R.drawable.ic_missing // TODO Ski school icon
					else -> {
						Log.w(tag, "$it does not have an icon")
						null
					}
				}

				withContext(Dispatchers.Main) {
					val polygonMapItem: PolygonMapItem = if (uiMapItemPolygons != null) {

						val uiMapItemPoints: MutableList<List<LatLng>> = mutableListOf()
						uiMapItemPolygons.forEach { p ->
							uiMapItemPoints.add(p.points)
						}

						PolygonMapItem(it, uiMapItemPolygons.toMutableList(), uiMapItemPoints, icon)
					} else {
						Log.w(tag, "No polygon for $it")
						PolygonMapItem(it, mutableListOf(), mutableListOf(), icon)
					}

					othersList.add(polygonMapItem)
				}
			}

			this@MtSpokaneMapItems.other = othersList

			Log.v(tag, "Finished loading other polygons")
		}
	}

	suspend fun initializeChairliftTerminalsAsync(classUsingObject: KClass<out ContextWrapper>, mapHandler: MapHandler):
			Deferred<Int> = coroutineScope {

		this@MtSpokaneMapItems.checkoutObject(classUsingObject)
		val tag = "ChairliftTerminalsAsync"

		return@coroutineScope async(Dispatchers.IO) {

			Log.v(tag, "Started loading chairlift terminal polygons")

			val hashmap: HashMap<String, List<Polygon>> = mapHandler.loadPolygons(R.raw.lift_terminal_polygons,
				R.color.chairlift_polygon)

			val chairliftTerminalList: MutableList<PolygonMapItem> = mutableListOf()

			hashmap.keys.forEach {

				val polygonList: List<Polygon>? = hashmap[it]

				withContext(Dispatchers.Main) {

					val polygonMapItem: PolygonMapItem = if (polygonList != null) {

						val polygonPoints: MutableList<List<LatLng>> = mutableListOf()
						polygonList.forEach { p ->
							polygonPoints.add(p.points)
						}

						PolygonMapItem(it, polygonList.toMutableList(), polygonPoints, R.drawable.ic_chairlift)
					} else {

						Log.w(tag, "No polygon for $it")
						PolygonMapItem(it, mutableListOf(), mutableListOf(), R.drawable.ic_chairlift)
					}

					chairliftTerminalList.add(polygonMapItem)
				}
			}

			this@MtSpokaneMapItems.chairliftTerminals = chairliftTerminalList

			Log.v(tag, "Finished loading chairlift terminal polygons")
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
			val hashmap: HashMap<String, List<Polygon>> = mapHandler.loadPolygons(R.raw.lift_polygons,
				R.color.chairlift_polygon)

			if (this@MtSpokaneMapItems.chairlifts != null) {

				this@MtSpokaneMapItems.chairlifts!!.forEach { visibleMapItem: PolylineMapItem ->

					val polygons: List<Polygon>? = hashmap[visibleMapItem.name]
					if (polygons != null) {

						val polygonPoints: MutableList<List<LatLng>> = mutableListOf()

						withContext(Dispatchers.Main) {
							polygons.forEach {
								polygonPoints.add(it.points)
							}
						}

						visibleMapItem.polygons.addAll(polygons)
						visibleMapItem.points.addAll(polygonPoints)
					}
				}

				Log.v(tag, "Finished loading chairlift polygons")
			} else {

				Log.w(tag, "Unable to load chairlift polygons due to missing polylines")
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

			val hashmap: HashMap<String, List<Polygon>> = mapHandler.loadPolygons(R.raw.easy_polygons,
				R.color.easy_polygon)

			if (this@MtSpokaneMapItems.easyRuns != null) {

				this@MtSpokaneMapItems.easyRuns!!.forEach { polylineMapItem: PolylineMapItem ->

					val polygons: List<Polygon>? = hashmap[polylineMapItem.name]
					if (polygons != null) {

						val polygonPoints: MutableList<List<LatLng>> = mutableListOf()

						withContext(Dispatchers.Main) {
							polygons.forEach {
								polygonPoints.add(it.points)
							}
						}

						polylineMapItem.polygons.addAll(polygons)
						polylineMapItem.points.addAll(polygonPoints)
					}
				}

				Log.v(tag, "Finished loading easy polygons")
			} else {

				Log.w(tag, "Unable to load easy polygons due to missing polylines")
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

			val hashmap: HashMap<String, List<Polygon>> = mapHandler.loadPolygons(R.raw.moderate_polygons,
				R.color.moderate_polygon)

			if (this@MtSpokaneMapItems.moderateRuns != null) {

				this@MtSpokaneMapItems.moderateRuns!!.forEach { polylineMapItem: PolylineMapItem ->

					val polygons: List<Polygon>? = hashmap[polylineMapItem.name]
					if (polygons != null) {

						val polygonPoints: MutableList<List<LatLng>> = mutableListOf()

						withContext(Dispatchers.Main) {
							polygons.forEach {
								polygonPoints.add(it.points)
							}
						}

						polylineMapItem.polygons.addAll(polygons)
						polylineMapItem.points.addAll(polygonPoints)
					}
				}

				Log.v(tag, "Finished loading moderate polygons")
			} else {

				Log.w(tag, "Unable to load moderate polygons due to missing polylines")
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
			val hashmap: HashMap<String, List<Polygon>> = mapHandler.loadPolygons(R.raw.difficult_polygons,
				R.color.difficult_polygon)

			if (this@MtSpokaneMapItems.difficultRuns != null) {

				this@MtSpokaneMapItems.difficultRuns!!.forEach { polylineMapItem: PolylineMapItem ->

					val polygons: List<Polygon>? = hashmap[polylineMapItem.name]
					if (polygons != null) {

						val polygonPoints: MutableList<List<LatLng>> = mutableListOf()

						withContext(Dispatchers.Main) {
							polygons.forEach {
								polygonPoints.add(it.points)
							}
						}

						polylineMapItem.polygons.addAll(polygons)
						polylineMapItem.points.addAll(polygonPoints)
					}
				}

				Log.v(tag, "Finished loading difficult polygons")
			} else {

				Log.w(tag, "Unable to load difficult polygons due to missing polylines")
			}
		}
	}

	fun destroyUIItems(classUsingObject: KClass<out ContextWrapper>) {

		this.classesUsingObject.remove(classUsingObject)

		if (this.classesUsingObject.isNotEmpty()) {
			return
		}

		this.skiAreaBounds?.destroyUIItems()

		val mapItems: Array<List<PolygonMapItem>?> = arrayOf(this.other, this.chairliftTerminals,
			this.chairlifts, this.easyRuns, this.moderateRuns, this.difficultRuns)
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