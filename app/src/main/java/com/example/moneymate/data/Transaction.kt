package com.example.moneymate.data

import java.util.Date

data class Transaction(
    val id: Long = 0,
    val amount: Double,
    val description: String,
    val category: TransactionCategory,
    val type: TransactionType,
    val date: Date = Date()
)


