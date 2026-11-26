package com.example.moneymate.ui.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.moneymate.data.TransactionRepository
import com.example.moneymate.data.TransactionCategory
import java.util.Calendar

class DashboardViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = TransactionRepository(application)
    private val calendar = Calendar.getInstance()
    private val currentMonth = calendar.get(Calendar.MONTH)
    private val currentYear = calendar.get(Calendar.YEAR)

    private val _totalIncome = MutableLiveData<Double>()
    val totalIncome: LiveData<Double> = _totalIncome

    private val _totalExpenses = MutableLiveData<Double>()
    val totalExpenses: LiveData<Double> = _totalExpenses

    private val _categoryExpenses = MutableLiveData<Map<TransactionCategory, Double>>()
    val categoryExpenses: LiveData<Map<TransactionCategory, Double>> = _categoryExpenses

    private val _monthlyBudget = MutableLiveData<Double>()
    val monthlyBudget: LiveData<Double> = _monthlyBudget

    init {
        loadDashboardData()
    }

    private fun loadDashboardData() {
        _totalIncome.value = repository.getTotalIncome(currentMonth, currentYear)
        _totalExpenses.value = repository.getTotalExpenses(currentMonth, currentYear)
        _categoryExpenses.value = repository.getCategoryExpenses(currentMonth, currentYear)
        _monthlyBudget.value = repository.getMonthlyBudget()
    }

    fun getBalance(): Double {
        return (_totalIncome.value ?: 0.0) - (_totalExpenses.value ?: 0.0)
    }

    fun getBudgetProgress(): Double {
        val budget = _monthlyBudget.value ?: 0.0
        val expenses = _totalExpenses.value ?: 0.0
        return if (budget > 0) (expenses / budget) * 100 else 0.0
    }

    fun refreshData() {
        loadDashboardData()
    }
} 
