package com.mtspokane.skiapp.mapItem

object MtSpokaneMapBounds {

	var skiAreaBounds: MapItem? = null

	val other: MutableList<MapItem> = mutableListOf() // Should be 9

	val chairliftTerminals: MutableList<MapItem> = mutableListOf() // Should be size 6

	val chairliftsBounds: MutableList<MapItem> = mutableListOf() // Should be size 6

	val easyRunsBounds: MutableList<MapItem> = mutableListOf() // Should be size 22

	val moderateRunsBounds: MutableList<MapItem> = mutableListOf() // Should be size 19

	val difficultRunsBounds: MutableList<MapItem> = mutableListOf() // Should be size 25

	/**
	 * lodges, parking lots, vista house, tubing area, yurt, ski patrol building, and ski area bounds...
	 */
	/*
	@AnyThread
	@Deprecated("To be removed")
	suspend fun initializeOtherPolygonsAsync(classUsingObject: KClass<out ContextWrapper>,
	                                         mapHandler: MapHandler): Deferred<Int> = coroutineScope {

		this@MtSpokaneMapBounds.checkoutObject(classUsingObject)
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

					this@MtSpokaneMapBounds.skiAreaBounds = MapItem(skiAreaBoundsKeyName,
						bounds.toMutableList(), boundsPoints)
					hashmap.remove(skiAreaBoundsKeyName)
				}
			}

			val othersList: MutableList<MapItem> = mutableListOf()

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
					val mapItem: MapItem = if (uiMapItemPolygons != null) {

						val uiMapItemPoints: MutableList<List<LatLng>> = mutableListOf()
						uiMapItemPolygons.forEach { p ->
							uiMapItemPoints.add(p.points)
						}

						MapItem(it, uiMapItemPolygons.toMutableList(), uiMapItemPoints, icon)
					} else {
						Log.w(tag, "No polygon for $it")
						MapItem(it, mutableListOf(), mutableListOf(), icon)
					}

					othersList.add(mapItem)
				}
			}

			this@MtSpokaneMapBounds.other = othersList

			Log.v(tag, "Finished loading other polygons")
		}
	}

	@Deprecated("To be removed")
	suspend fun addChairliftTerminalPolygonsAsync(classUsingObject: KClass<out ContextWrapper>,
	                                              mapHandler: MapHandler): Deferred<Int> = coroutineScope {

		this@MtSpokaneMapBounds.checkoutObject(classUsingObject)
		val tag = "ChairliftTerminalsAsync"

		return@coroutineScope async(Dispatchers.IO) {

			Log.v(tag, "Started loading chairlift terminal polygons")

			val hashmap: HashMap<String, List<Polygon>> = mapHandler.loadPolygons(R.raw.lift_terminal_polygons,
				R.color.chairlift_polygon)

			val chairliftTerminalList: MutableList<MapItem> = mutableListOf()

			hashmap.keys.forEach {

				val polygonList: List<Polygon>? = hashmap[it]

				withContext(Dispatchers.Main) {

					val mapItem: MapItem = if (polygonList != null) {

						val polygonPoints: MutableList<List<LatLng>> = mutableListOf()
						polygonList.forEach { p ->
							polygonPoints.add(p.points)
						}

						MapItem(it, polygonList.toMutableList(), polygonPoints, R.drawable.ic_chairlift)
					} else {

						Log.w(tag, "No polygon for $it")
						MapItem(it, mutableListOf(), mutableListOf(), R.drawable.ic_chairlift)
					}

					chairliftTerminalList.add(mapItem)
				}
			}

			this@MtSpokaneMapBounds.chairliftTerminals = chairliftTerminalList

			Log.v(tag, "Finished loading chairlift terminal polygons")
		}
	}*/

	/*
	@AnyThread
	@Deprecated("To be removed")
	suspend fun initializeChairliftsAsync(classUsingObject: KClass<out ContextWrapper>,
	                                      mapHandler: MapHandler): Deferred<Int> = coroutineScope {

		this@MtSpokaneMapBounds.checkoutObject(classUsingObject)
		val tag = "initializeChairlifts"

		return@coroutineScope async(Dispatchers.IO) {
			Log.v(tag, "Started loading chairlift polylines")

			try {
				this@MtSpokaneMapBounds.chairliftsBounds = mapHandler.loadPolylines(R.raw.lifts, R.color.chairlift,
					4f, R.drawable.ic_chairlift)
				Log.v(tag, "Finished loading chairlift polylines")
			} catch (npe: NullPointerException) {
				Log.e(tag, "Unable to add chairlift polylines to map: map not setup", npe)
			}
		}
	}*/

	/*
	@Deprecated("To be removed")
	suspend fun addChairliftPolygonsAsync(classUsingObject: KClass<out ContextWrapper>,
	                                      mapHandler: MapHandler): Deferred<Int> = coroutineScope {

		this@MtSpokaneMapBounds.checkoutObject(classUsingObject)
		val tag = "addChairliftPolygons"

		return@coroutineScope async(Dispatchers.IO) {

			Log.v(tag, "Started loading chairlift polygons")
			val hashmap: HashMap<String, List<Polygon>> = mapHandler.loadPolygons(R.raw.lift_polygons,
				R.color.chairlift_polygon)

			if (this@MtSpokaneMapBounds.chairliftsBounds != null) {

				this@MtSpokaneMapBounds.chairliftsBounds!!.forEach { visibleMapItem: PolylineMapItem ->

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
	}*/

	/*
	@AnyThread
	@Deprecated("To be removed")
	suspend fun initializeEasyRunsAsync(classUsingObject: KClass<out ContextWrapper>, mapHandler: MapHandler):
			Deferred<Int> = coroutineScope {

		this@MtSpokaneMapBounds.checkoutObject(classUsingObject)
		val tag = "initializeEasyRunsAsync"

		return@coroutineScope async(Dispatchers.IO) {
			Log.v(tag, "Started loading easy polylines")

			try {
				this@MtSpokaneMapBounds.easyRunsBounds = mapHandler.loadPolylines(R.raw.easy, R.color.easy,
					3f, R.drawable.ic_easy)
				Log.v(tag, "Finished loading easy run polylines")
			} catch (npe: NullPointerException) {
				Log.e(tag, "Unable to add easy polylines to map: map not setup", npe)
			}
		}
	}*/

	/*
	@Deprecated("To be removed")
	suspend fun addEasyPolygonsAsync(classUsingObject: KClass<out ContextWrapper>, mapHandler: MapHandler):
			Deferred<Int> = coroutineScope {

		this@MtSpokaneMapBounds.checkoutObject(classUsingObject)
		val tag = "addEasyPolygonsAsync"

		return@coroutineScope async(Dispatchers.IO) {
			Log.v(tag, "Started loading easy polygons")

			val hashmap: HashMap<String, List<Polygon>> = mapHandler.loadPolygons(R.raw.easy_polygons,
				R.color.easy_polygon)

			if (this@MtSpokaneMapBounds.easyRunsBounds != null) {

				this@MtSpokaneMapBounds.easyRunsBounds!!.forEach { polylineMapItem: PolylineMapItem ->

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

	/*
	@AnyThread
	@Deprecated("To be removed")
	suspend fun initializeModerateRunsAsync(classUsingObject: KClass<out ContextWrapper>,
	                                        mapHandler: MapHandler): Deferred<Int> = coroutineScope {

		this@MtSpokaneMapBounds.checkoutObject(classUsingObject)
		val tag = "initializeModerateRuns"

		return@coroutineScope async(Dispatchers.IO) {
			Log.v(tag, "Started loading moderate polylines")

			try {
				this@MtSpokaneMapBounds.moderateRunsBounds = mapHandler.loadPolylines(R.raw.moderate, R.color.moderate,
					2f, R.drawable.ic_moderate)
				Log.v(tag, "Finished loading moderate run polylines")
			} catch (npe: NullPointerException) {
				Log.e(tag, "Unable to add moderate runs to map: map not setup", npe)
			}
		}
	}*/

	@Deprecated("To be removed")
	suspend fun addModeratePolygonsAsync(classUsingObject: KClass<out ContextWrapper>, mapHandler: MapHandler):
			Deferred<Int> = coroutineScope {

		this@MtSpokaneMapBounds.checkoutObject(classUsingObject)
		val tag = "addModeratePolygons"

		return@coroutineScope async(Dispatchers.IO) {
			Log.v(tag, "Started loading moderate polygons")

			val hashmap: HashMap<String, List<Polygon>> = mapHandler.loadPolygons(R.raw.moderate_polygons,
				R.color.moderate_polygon)

			if (this@MtSpokaneMapBounds.moderateRunsBounds != null) {

				this@MtSpokaneMapBounds.moderateRunsBounds!!.forEach { polylineMapItem: PolylineMapItem ->

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

	/*
	@AnyThread
	@Deprecated("To be removed")
	suspend fun initializeDifficultRunsAsync(classUsingObject: KClass<out ContextWrapper>,
	                                         mapHandler: MapHandler): Deferred<Int> = coroutineScope {

		this@MtSpokaneMapBounds.checkoutObject(classUsingObject)
		val tag = "initializeDifficultRuns"

		return@coroutineScope async(Dispatchers.IO) {
			Log.v(tag, "Started loading difficult polylines")

			try {
				this@MtSpokaneMapBounds.difficultRunsBounds = mapHandler.loadPolylines(R.raw.difficult,
					R.color.difficult, 1f, R.drawable.ic_difficult)
				Log.v(tag, "Finished loading difficult polylines")
			} catch (npe: NullPointerException) {
				Log.e(tag, "Unable to add advanced runs to map: map not setup", npe)
			}
		}
	}*/

	@Deprecated("To be removed")
	suspend fun addDifficultPolygonsAsync(classUsingObject: KClass<out ContextWrapper>, mapHandler: MapHandler):
			Deferred<Int> = coroutineScope {

		this@MtSpokaneMapBounds.checkoutObject(classUsingObject)
		val tag = "addDifficultPolygons"

		return@coroutineScope async(Dispatchers.IO) {
			Log.v(tag, "Started loading difficult polygons")
			val hashmap: HashMap<String, List<Polygon>> = mapHandler.loadPolygons(R.raw.difficult_polygons,
				R.color.difficult_polygon)

			if (this@MtSpokaneMapBounds.difficultRunsBounds != null) {

				this@MtSpokaneMapBounds.difficultRunsBounds!!.forEach { polylineMapItem: PolylineMapItem ->

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
	}*/
}