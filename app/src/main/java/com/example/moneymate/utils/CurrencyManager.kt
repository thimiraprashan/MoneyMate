package com.example.moneymate.utils

import android.content.Context
import android.content.SharedPreferences
import com.example.moneymate.data.Currency
import com.example.moneymate.data.Currency.USD

class CurrencyManager private constructor(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private var currentCurrency: Currency = getSavedCurrency()
    private val listeners = mutableListOf<CurrencyChangeListener>()

    fun getCurrentCurrency(): Currency = currentCurrency

    fun setCurrency(currency: Currency) {
        currentCurrency = currency
        prefs.edit().putString(CURRENCY_KEY, currency.code).apply()
        notifyListeners()
    }

    private fun getSavedCurrency(): Currency {
        val savedCode = prefs.getString(CURRENCY_KEY, USD.code)
        return Currency.fromCode(savedCode ?: USD.code) ?: USD
    }

    fun formatAmount(amount: Double): String {
        return "${currentCurrency.symbol}${String.format("%.2f", amount)}"
    }

    fun addListener(listener: CurrencyChangeListener) {
        listeners.add(listener)
    }

    fun removeListener(listener: CurrencyChangeListener) {
        listeners.remove(listener)
    }

    private fun notifyListeners() {
        listeners.forEach { it.onCurrencyChanged(currentCurrency) }
    }

    interface CurrencyChangeListener {
        fun onCurrencyChanged(newCurrency: Currency)
    }

    companion object {
        private const val PREFS_NAME = "currency_prefs"
        private const val CURRENCY_KEY = "selected_currency"
        private var instance: CurrencyManager? = null

        fun getInstance(context: Context): CurrencyManager {
            return instance ?: synchronized(this) {
                instance ?: CurrencyManager(context.applicationContext).also { instance = it }
            }
        }
    }
} 
