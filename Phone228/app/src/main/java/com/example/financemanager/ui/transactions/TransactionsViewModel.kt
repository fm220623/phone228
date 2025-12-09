package com.example.financemanager.ui.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.financemanager.data.model.Transaction
import com.example.financemanager.data.repository.SimpleFinanceRepository
import kotlinx.coroutines.launch

class TransactionsViewModel(private val repository: SimpleFinanceRepository) : ViewModel() {

    val transactions = repository.transactions
    val categories = repository.categories

    val totalIncome get() = repository.getTotalIncome()
    val totalExpense get() = repository.getTotalExpense()
    val balance get() = totalIncome - totalExpense

    fun addTransaction(transaction: Transaction) {
        viewModelScope.launch {
            repository.addTransaction(transaction)
        }
    }

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
        }
    }
}