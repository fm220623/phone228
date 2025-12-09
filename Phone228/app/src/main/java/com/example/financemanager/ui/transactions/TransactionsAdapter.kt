package com.example.financemanager.ui.transactions

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.financemanager.R
import com.example.financemanager.data.model.Transaction
import java.text.SimpleDateFormat
import java.util.*

class TransactionsAdapter(
    private val transactions: List<Transaction>,
    private val onDeleteClick: (Transaction) -> Unit
) : RecyclerView.Adapter<TransactionsAdapter.ViewHolder>() {

    class ViewHolder(view: View, onDeleteClick: (Transaction) -> Unit) :
        RecyclerView.ViewHolder(view) {

        private lateinit var currentTransaction: Transaction
        val description: TextView = view.findViewById(R.id.text_description)
        val amount: TextView = view.findViewById(R.id.text_amount)
        val date: TextView = view.findViewById(R.id.text_date)
        val btnDelete: Button = view.findViewById(R.id.btn_delete)

        init {
            btnDelete.setOnClickListener {
                onDeleteClick(currentTransaction)
            }
        }

        fun bind(transaction: Transaction) {
            currentTransaction = transaction
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_transaction, parent, false)
        return ViewHolder(view, onDeleteClick)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val transaction = transactions[position]
        holder.bind(transaction)

        holder.description.text = transaction.description
        holder.amount.text = "${transaction.amount} ₽"

        // Форматируем дату
        val date = Date(transaction.date)
        val formatter = SimpleDateFormat("dd.MM.yy", Locale.getDefault())
        holder.date.text = formatter.format(date)

        // Цвет суммы
        val color = if (transaction.type.name == "EXPENSE") {
            android.graphics.Color.RED
        } else {
            android.graphics.Color.GREEN
        }
        holder.amount.setTextColor(color)
    }

    override fun getItemCount(): Int = transactions.size

    fun updateData(newTransactions: List<Transaction>) {
        // Простой способ — пересоздаём адаптер
        // В реальном приложении используй DiffUtil
    }
}