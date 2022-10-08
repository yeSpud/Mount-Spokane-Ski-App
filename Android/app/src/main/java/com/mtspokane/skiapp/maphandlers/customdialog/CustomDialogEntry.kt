package com.mtspokane.skiapp.maphandlers.customdialog

import android.content.Context
import android.util.AttributeSet
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.mtspokane.skiapp.R

class CustomDialogEntry: LinearLayout {

	val menuEntryIcon: ImageView

	val menuEntryText: TextView

	private var itemEnabled: Boolean

	constructor(context: Context): this(context, null)

	constructor(context: Context, attributeSet: AttributeSet?): this(context, attributeSet, 0)

	constructor(context: Context, attributeSet: AttributeSet?, defStyleAttr: Int) : super(context, attributeSet, defStyleAttr) {

		inflate(context, R.layout.menu_dialog_entry, this)

		this.menuEntryIcon = this.findViewById(R.id.menu_entry_icon)
		this.menuEntryText = this.findViewById(R.id.menu_entry_text)

		context.theme.obtainStyledAttributes(attributeSet, R.styleable.CustomDialogEntry, 0, 0).apply {

			try {
				this@CustomDialogEntry.menuEntryIcon.setImageDrawable(this.getDrawable(R.styleable.CustomDialogEntry_menu_icon))
				this@CustomDialogEntry.menuEntryText.text = this.getText(R.styleable.CustomDialogEntry_menu_title)
				this@CustomDialogEntry.itemEnabled = this.getBoolean(R.styleable.CustomDialogEntry_menu_enable_by_default, true)
			} finally {
				this.recycle()
			}
		}

	}

	override fun setOnClickListener(l: OnClickListener?) {
		super.setOnClickListener(l)
		this.itemEnabled = !this.itemEnabled
	}
}