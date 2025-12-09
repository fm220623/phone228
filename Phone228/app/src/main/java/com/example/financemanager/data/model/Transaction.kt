package com.example.financemanager.data.model

data class Transaction(
    val id: Long = 0,
    val amount: Double,
    val categoryId: Long,
    val type: TransactionType,
    val description: String,
    val date: Long = System.currentTimeMillis()
)

enum class TransactionType {
    INCOME, EXPENSE
}