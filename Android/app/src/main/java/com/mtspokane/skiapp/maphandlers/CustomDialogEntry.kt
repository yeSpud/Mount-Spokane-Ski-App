package com.mtspokane.skiapp.maphandlers

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import com.mtspokane.skiapp.R
import com.mtspokane.skiapp.mapItem.PolylineMapItem

class CustomDialogEntry: LinearLayout {

	val menuEntryIcon: ImageView

	val menuEntryText: TextView

	private var itemEnabled: Boolean

	constructor(context: Context): this(context, null)

	constructor(context: Context, attributeSet: AttributeSet?): this(context, attributeSet, 0)

	constructor(context: Context, attributeSet: AttributeSet?, defStyleAttr: Int) : super(context, attributeSet, defStyleAttr) {

		inflate(context, R.layout.menu_dialog_entry, this)

		this.menuEntryIcon = findViewById(R.id.menu_entry_icon)
		this.menuEntryText = findViewById(R.id.menu_entry_text)

		context.theme.obtainStyledAttributes(attributeSet, R.styleable.CustomDialogEntry, 0, 0).apply {

			try {
				this@CustomDialogEntry.menuEntryIcon.setImageDrawable(getDrawable(R.styleable.CustomDialogEntry_menu_icon))
				this@CustomDialogEntry.menuEntryText.text = getText(R.styleable.CustomDialogEntry_menu_title)
				this@CustomDialogEntry.itemEnabled = getBoolean(R.styleable.CustomDialogEntry_menu_enable_by_default, true)
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
		itemEnabled = !this.itemEnabled
	}
}

class CustomDialogOnClickListener(private val polylineMapItems: List<PolylineMapItem>, private val map: MapHandler): View.OnClickListener {

	override fun onClick(v: View?) {

		if (v == null || v !is CustomDialogEntry) {
			return
		}

		for (polylineMapItem in polylineMapItems) {
			polylineMapItem.togglePolyLineVisibility(!polylineMapItem.defaultVisibility, map.isNightOnly)
		}

		v.setGlowing(polylineMapItems[0].polylines[0].isVisible)
	}
}