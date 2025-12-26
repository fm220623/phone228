package com.example.financemanager.data.model

// @Entity(tableName = "budgets") ← ЗАКОММЕНТИРОВАТЬ!
data class Budget(
    // @PrimaryKey(autoGenerate = true) ← ЗАКОММЕНТИРОВАТЬ!
    val id: Long = 0,
    val categoryId: Long,
    val limit: Double,
    val period: BudgetPeriod = BudgetPeriod.MONTHLY
)

enum class BudgetPeriod {
    WEEKLY, MONTHLY, YEARLY
}