package com.example.moneymate.ui.settings

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import com.example.moneymate.R
import com.example.moneymate.data.Currency

class CurrencyAdapter(context: Context) :
    ArrayAdapter<Currency>(context, R.layout.item_currency, Currency.values()) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View =
        createView(position, convertView, parent)

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View =
        createView(position, convertView, parent)

    private fun createView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.item_currency, parent, false)

        val currency = getItem(position)
        val currencyName = view.findViewById<TextView>(R.id.currencyName)
        val currencyCode = view.findViewById<TextView>(R.id.currencyCode)

        currencyName.text = "${currency?.symbol} • ${currency?.displayName}"
        currencyCode.text = currency?.code

        return view
    }
}
