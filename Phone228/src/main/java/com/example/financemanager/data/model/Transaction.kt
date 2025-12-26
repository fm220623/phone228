package com.example.financemanager.data.model

data class Transaction(
    val id: Long = 0,
    val amount: Double,
    val category: String,  // НОВОЕ ПОЛЕ для хранения категории
    val categoryId: Long,
    val type: TransactionType,
    val description: String,
    val date: Long = System.currentTimeMillis(),
    val photoUri: String? = null
)

enum class TransactionType {
    INCOME, EXPENSE
}