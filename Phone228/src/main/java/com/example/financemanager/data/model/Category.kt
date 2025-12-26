package com.example.financemanager.data.model

// @Entity(tableName = "categories") ← ЗАКОММЕНТИРОВАТЬ!
data class Category(
    // @PrimaryKey(autoGenerate = true) ← ЗАКОММЕНТИРОВАТЬ!
    val id: Long = 0,
    val name: String,
    val type: TransactionType,
    val color: String = "#2196F3"
)