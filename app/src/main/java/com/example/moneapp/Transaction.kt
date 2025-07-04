package com.example.moneapp

import java.util.Date

data class Transaction(
    var id: Int,
    var title: String,
    var amount: Double,
    var category: String,
    var type: String, // "Income" or "Expense"
    var date: Date = Date()
)