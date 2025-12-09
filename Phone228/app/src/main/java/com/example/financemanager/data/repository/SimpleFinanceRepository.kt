package com.example.financemanager.data.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.MutableLiveData
import com.example.financemanager.data.model.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class SimpleFinanceRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("finance_data", Context.MODE_PRIVATE)

    private val gson = Gson()

    // Ключи для SharedPreferences
    private val transactionKey = "transactions"
    private val categoryKey = "categories"

    // LiveData
    private val _transactions = MutableLiveData<List<Transaction>>()
    val transactions: MutableLiveData<List<Transaction>> = _transactions

    private val _categories = MutableLiveData<List<Category>>()
    val categories: MutableLiveData<List<Category>> = _categories

    init {
        loadTransactions()
        loadCategories()

        // Если категорий нет — создаём начальные
        if (_categories.value.isNullOrEmpty()) {
            createDefaultCategories()
        }
    }

    // === ТРАНЗАКЦИИ ===
    fun addTransaction(transaction: Transaction) {
        val newTransaction = transaction.copy(id = System.currentTimeMillis())
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

    // === КАТЕГОРИИ ===
    private fun createDefaultCategories() {
        val defaultCategories = listOf(
            Category(id = 1, name = "Продукты", type = TransactionType.EXPENSE),
            Category(id = 2, name = "Транспорт", type = TransactionType.EXPENSE),
            Category(id = 3, name = "Развлечения", type = TransactionType.EXPENSE),
            Category(id = 4, name = "Зарплата", type = TransactionType.INCOME),
            Category(id = 5, name = "Фриланс", type = TransactionType.INCOME),
            Category(id = 6, name = "Подарки", type = TransactionType.INCOME)
        )
        _categories.value = defaultCategories
        saveCategories(defaultCategories)
    }

    // === СОХРАНЕНИЕ/ЗАГРУЗКА ===
    private fun saveTransactions(list: List<Transaction>) {
        val json = gson.toJson(list)
        prefs.edit().putString(transactionKey, json).apply()
    }

    private fun loadTransactions() {
        val json = prefs.getString(transactionKey, "[]") ?: "[]"
        val type = object : TypeToken<List<Transaction>>() {}.type
        val list = gson.fromJson<List<Transaction>>(json, type)
        _transactions.value = list ?: emptyList()
    }

    private fun saveCategories(list: List<Category>) {
        val json = gson.toJson(list)
        prefs.edit().putString(categoryKey, json).apply()
    }

    private fun loadCategories() {
        val json = prefs.getString(categoryKey, "[]") ?: "[]"
        val type = object : TypeToken<List<Category>>() {}.type
        val list = gson.fromJson<List<Category>>(json, type)
        _categories.value = list ?: emptyList()
    }
}