package com.mtspokane.skiapp.maphandlers

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.IdRes
import androidx.annotation.LayoutRes
import androidx.appcompat.content.res.AppCompatResources
import com.mtspokane.skiapp.R
import com.mtspokane.skiapp.mapItem.PolylineMapItem

class MapOptionItem: LinearLayout {

	private val menuEntryIcon: ImageView

	private val menuEntryText: TextView

	private var itemEnabled: Boolean

	constructor(context: Context): this(context, null)

	constructor(context: Context, attributeSet: AttributeSet?): this(context, attributeSet, 0)

	constructor(context: Context, attributeSet: AttributeSet?, defStyleAttr: Int) : super(context, attributeSet, defStyleAttr) {

		inflate(context, R.layout.menu_dialog_entry, this)

		menuEntryIcon = findViewById(R.id.menu_entry_icon)
		menuEntryText = findViewById(R.id.menu_entry_text)

		context.theme.obtainStyledAttributes(attributeSet, R.styleable.MapOptions, 0, 0).apply {

			try {
				this@MapOptionItem.menuEntryIcon.setImageDrawable(getDrawable(R.styleable.MapOptions_menu_icon))
				this@MapOptionItem.menuEntryText.text = getText(R.styleable.MapOptions_menu_title)
				this@MapOptionItem.itemEnabled = getBoolean(R.styleable.MapOptions_menu_enable_by_default, true)
			} finally {
				recycle()
			}
		}

	}

	fun setGlowing(glow: Boolean) {
		if (glow) {
			menuEntryIcon.background = AppCompatResources.getDrawable(context, R.drawable.glowing)
		} else {
			menuEntryIcon.background = null
		}
	}

	override fun setOnClickListener(l: OnClickListener?) {
		super.setOnClickListener(l)
		itemEnabled = !itemEnabled
	}
}

class OnMapItemClicked(private val polylineMapItems: List<PolylineMapItem>, private val map: MapHandler): View.OnClickListener {

	override fun onClick(v: View?) {

		if (v == null || v !is MapOptionItem) {
			return
		}

		for (polylineMapItem in polylineMapItems) {
			polylineMapItem.togglePolyLineVisibility(!polylineMapItem.defaultVisibility, map.isNightOnly)
		}

		v.setGlowing(polylineMapItems[0].polylines[0].isVisible)
	}
}

open class MapOptionsDialog(private val layoutInflater: LayoutInflater, @LayoutRes private val menu: Int,
							private val map: MapHandler) : BaseAdapter() {

	private var showChairliftImage: MapOptionItem? = null

	private var showEasyRunsImage: MapOptionItem? = null

	private var showModerateRunsImage: MapOptionItem? = null

	private var showDifficultRunsImage: MapOptionItem? = null

	private var showNightRunsImage: MapOptionItem? = null

	override fun getCount(): Int {
		return 1
	}

	override fun getItem(position: Int): Any {
		return position // Todo properly implement me?
	}

	override fun getItemId(position: Int): Long {
		return position.toLong() // Todo properly implement me?
	}

	override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {

		val view: View = convertView ?: layoutInflater.inflate(menu, parent, false)

		if (showChairliftImage == null) {
			showChairliftImage = getRunOption(view, R.id.show_chairlift, map.chairliftPolylines, map)
		}

		if (showEasyRunsImage == null) {
			showEasyRunsImage = getRunOption(view, R.id.show_easy_runs, map.easyRunsPolylines, map)

		}

		if (showModerateRunsImage == null) {
			showModerateRunsImage = getRunOption(view, R.id.show_moderate_runs, map.moderateRunsPolylines,
				map)
		}

		if (showDifficultRunsImage == null) {
			showDifficultRunsImage = getRunOption(view, R.id.show_difficult_runs, map.difficultRunsPolylines,
				map)
		}

		if (showNightRunsImage == null) {
			val nightRunImage: MapOptionItem? = view.findViewById(R.id.show_night_runs)
			if (nightRunImage == null) {
				Log.w("getView", "Unable to find night run option")
				return view
			}

			nightRunImage.setOnClickListener {
				if (it == null || it !is MapOptionItem) {
					return@setOnClickListener
				}

				with(map) {

					isNightOnly = !isNightOnly

					for (chairliftPolyline in chairliftPolylines) {
						chairliftPolyline.togglePolyLineVisibility(chairliftPolyline.defaultVisibility,
							isNightOnly)
					}

					for (easyRunPolyline in easyRunsPolylines) {
						easyRunPolyline.togglePolyLineVisibility(easyRunPolyline.defaultVisibility,
							isNightOnly)
					}

					for (moderateRunPolyline in moderateRunsPolylines) {
						moderateRunPolyline.togglePolyLineVisibility(moderateRunPolyline.defaultVisibility,
							isNightOnly)
					}

					for (difficultRunPolyline in difficultRunsPolylines) {
						difficultRunPolyline.togglePolyLineVisibility(difficultRunPolyline.defaultVisibility,
							isNightOnly)
					}

					it.setGlowing(isNightOnly)
				}
			}
			nightRunImage.setGlowing(map.isNightOnly)
			showNightRunsImage = nightRunImage
		}

		return view
	}

	companion object {
		private fun getRunOption(view: View, @IdRes resId: Int, runs: List<PolylineMapItem>,
								 map: MapHandler): MapOptionItem? {
			val optionsView: MapOptionItem? = view.findViewById(resId)
			if (optionsView == null) {
				Log.w("getRunObject", "Unable to find that option!")
				return null
			}

			optionsView.setOnClickListener(OnMapItemClicked(runs, map))
			optionsView.setGlowing(runs[0].polylines[0].isVisible)
			return optionsView
		}
	}
}