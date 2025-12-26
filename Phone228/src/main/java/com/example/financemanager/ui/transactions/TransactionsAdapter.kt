package com.example.financemanager.ui.transactions

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.financemanager.R
import com.example.financemanager.data.model.Transaction
import com.example.financemanager.data.model.TransactionType
import com.example.financemanager.utils.ImageUtils
import java.text.SimpleDateFormat
import java.util.*

class TransactionsAdapter(
    private var transactions: List<Transaction>,
    private val onDeleteClick: (Transaction) -> Unit,
    private val onItemClick: (Transaction) -> Unit
) : RecyclerView.Adapter<TransactionsAdapter.ViewHolder>() {

    // Добавляем переменную для хранения последней позиции
    private var lastPosition = -1

    class ViewHolder(view: View, onDeleteClick: (Transaction) -> Unit, onItemClick: (Transaction) -> Unit) :
        RecyclerView.ViewHolder(view) {

        private lateinit var currentTransaction: Transaction
        val description: TextView = view.findViewById(R.id.text_description)
        val amount: TextView = view.findViewById(R.id.text_amount)
        val date: TextView = view.findViewById(R.id.text_date)
        val btnDelete: Button = view.findViewById(R.id.btn_delete)
        val photoIndicator: TextView = view.findViewById(R.id.text_photo_indicator)

        init {
            btnDelete.setOnClickListener {
                onDeleteClick(currentTransaction)
            }

            itemView.setOnClickListener {
                onItemClick(currentTransaction)
            }
        }

        fun bind(transaction: Transaction) {
            currentTransaction = transaction
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_transaction, parent, false)
        return ViewHolder(view, onDeleteClick, onItemClick)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val transaction = transactions[position]
        holder.bind(transaction)

        // Описание
        holder.description.text = transaction.description

        // Сумма с цветом
        holder.amount.text = "${transaction.amount} ₽"
        if (transaction.type == TransactionType.EXPENSE) {
            holder.amount.setTextColor(android.graphics.Color.RED)
        } else {
            holder.amount.setTextColor(android.graphics.Color.GREEN)
        }

        // Дата
        val date = Date(transaction.date)
        val formatter = SimpleDateFormat("dd.MM.yy HH:mm", Locale.getDefault())
        holder.date.text = formatter.format(date)

        // Иконка фото
        val hasPhoto = transaction.photoUri != null && transaction.photoUri!!.isNotEmpty()
        holder.photoIndicator.text = if (hasPhoto) "📷" else ""
        holder.photoIndicator.visibility = if (hasPhoto) View.VISIBLE else View.GONE

        // ВОТ АНИМАЦИЯ: применяем только для новых элементов (position > lastPosition)
        if (position > lastPosition) {
            val animation = AnimationUtils.loadAnimation(holder.itemView.context, R.anim.transaction_add)
            holder.itemView.startAnimation(animation)
            lastPosition = position
        }
    }

    override fun getItemCount(): Int = transactions.size

    fun updateData(newTransactions: List<Transaction>) {
        // Сохраняем старый размер для определения новых элементов
        val oldSize = this.transactions.size

        // Обновляем список
        this.transactions = newTransactions

        // Определяем, сколько новых элементов добавилось
        val addedCount = newTransactions.size - oldSize

        if (addedCount > 0) {
            // Если добавились новые элементы - анимируем их появление
            notifyItemRangeInserted(oldSize, addedCount)
            // Сбрасываем lastPosition, чтобы анимация сработала для новых элементов
            lastPosition = oldSize - 1
        } else {
            // Если изменений нет или удаление - просто обновляем
            notifyDataSetChanged()
            lastPosition = -1
        }
    }
}