package com.example.moneapp

data class Budget(
    val category: String,
    var amount: Double,
    val month: Int,
    val year: Int,
    var spent: Double = 0.0
) {
    val percentSpent: Int
        get() = if (amount > 0) ((spent / amount) * 100).toInt() else 0

    val isExceeded: Boolean
        get() = spent > amount

    val isNearLimit: Boolean
        get() = percentSpent >= 80 && percentSpent < 100
}