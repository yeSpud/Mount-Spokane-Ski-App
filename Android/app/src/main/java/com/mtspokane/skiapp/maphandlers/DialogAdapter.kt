package com.mtspokane.skiapp.maphandlers

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import com.mtspokane.skiapp.R

class DialogAdapter(context: Context, private val count: Int): BaseAdapter() {

    private val layoutInflater: LayoutInflater = LayoutInflater.from(context)

    override fun getCount(): Int {
        return this.count
    }

    override fun getItem(position: Int): Any {
        return position
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {

        return convertView ?: layoutInflater.inflate(R.layout.custom_menu_v2, parent, false)
    }
}