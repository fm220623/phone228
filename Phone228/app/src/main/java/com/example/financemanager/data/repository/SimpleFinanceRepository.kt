package com.example.financemanager.data.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.MutableLiveData
import com.example.financemanager.R
import com.example.financemanager.data.model.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class SimpleFinanceRepository(private val context: Context) { // УБРАТЬ 'private constructor'

    companion object {
        @Volatile
        private var INSTANCE: SimpleFinanceRepository? = null

        fun getInstance(context: Context): SimpleFinanceRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SimpleFinanceRepository(context).also { // ПРОСТО вызываем конструктор
                    INSTANCE = it
                }
            }
        }
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences("finance_data", Context.MODE_PRIVATE)

    private val gson = Gson()
    private val transactionKey = "transactions"

    // LiveData для наблюдения за транзакциями
    private val _transactions = MutableLiveData<List<Transaction>>()
    val transactions: MutableLiveData<List<Transaction>> = _transactions

    // Получаем категории из ресурсов
    val categories: List<String>
        get() {
            val expenseCategories = context.resources.getStringArray(R.array.categories_expense).toList()
            val incomeCategories = context.resources.getStringArray(R.array.categories_income).toList()
            return expenseCategories + incomeCategories
        }

    val expenseCategories: List<String>
        get() = try {
            context.resources.getStringArray(R.array.categories_expense).toList()
        } catch (e: Exception) {
            // Возвращаем список по умолчанию при ошибке
            listOf("🍔 Еда", "🚗 Транспорт", "⚡ Прочее")
        }

    val incomeCategories: List<String>
        get() = try {
            context.resources.getStringArray(R.array.categories_income).toList()
        } catch (e: Exception) {
            listOf("💰 Зарплата", "💼 Фриланс", "💡 Прочее")
        }

    init {
        loadTransactions()
    }

    fun addTransaction(transaction: Transaction) {
        val newTransaction = transaction.copy(
            id = System.currentTimeMillis(),
            category = extractCategoryFromDescription(transaction.description)
        )

        val currentList = _transactions.value.orEmpty().toMutableList()
        currentList.add(newTransaction)
        _transactions.value = currentList

        saveTransactions(currentList)
    }

    fun deleteTransaction(transaction: Transaction) {
        val currentList = _transactions.value.orEmpty().toMutableList()
        currentList.removeAll { it.id == transaction.id }
        _transactions.value = currentList
        saveTransactions(currentList)
    }

    fun getTotalIncome(): Double {
        return _transactions.value.orEmpty()
            .filter { it.type == TransactionType.INCOME }
            .sumOf { it.amount }
    }

    fun getTotalExpense(): Double {
        return _transactions.value.orEmpty()
            .filter { it.type == TransactionType.EXPENSE }
            .sumOf { it.amount }
    }

    private fun extractCategoryFromDescription(description: String): String {
        return description.substringBefore(":").trim()
    }

    private fun saveTransactions(transactions: List<Transaction>) {
        val json = gson.toJson(transactions)
        prefs.edit().putString(transactionKey, json).apply()
    }

    private fun loadTransactions() {
        val json = prefs.getString(transactionKey, "[]") ?: "[]"
        val type = object : TypeToken<List<Transaction>>() {}.type
        val loadedList = gson.fromJson<List<Transaction>>(json, type)
        _transactions.value = loadedList ?: emptyList()
    }
}