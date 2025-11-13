package com.example.flow.ui.finance

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.flow.R
import com.example.flow.data.model.Transaction
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class TransactionListAdapter(
    private val onTransactionClicked: (Transaction) -> Unit
) : ListAdapter<Transaction, TransactionListAdapter.TransactionViewHolder>(TransactionDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_item_transaction, parent, false)
        return TransactionViewHolder(view)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        val transaction = getItem(position)
        holder.bind(transaction)
    }

    inner class TransactionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val descriptionTextView: TextView = itemView.findViewById(R.id.text_transaction_description)
        private val dateTextView: TextView = itemView.findViewById(R.id.text_transaction_date)
        private val amountTextView: TextView = itemView.findViewById(R.id.text_transaction_amount)
        private val context = itemView.context

        fun bind(transaction: Transaction) {
            descriptionTextView.text = transaction.description
            dateTextView.text = formatDate(transaction.date)

            val format = NumberFormat.getCurrencyInstance()
            val amountString = format.format(transaction.amount)
            if (transaction.isIncome) {
                amountTextView.text = "+$amountString"
                amountTextView.setTextColor(ContextCompat.getColor(context, android.R.color.holo_green_dark))
            } else {
                amountTextView.text = "-$amountString"
                amountTextView.setTextColor(ContextCompat.getColor(context, android.R.color.holo_red_dark))
            }

            itemView.setOnClickListener {
                onTransactionClicked(transaction)
            }
        }

        private fun formatDate(timestamp: Long): String {
            val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            return sdf.format(Date(timestamp))
        }
    }

    private class TransactionDiffCallback : DiffUtil.ItemCallback<Transaction>() {
        override fun areItemsTheSame(oldItem: Transaction, newItem: Transaction): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Transaction, newItem: Transaction): Boolean {
            return oldItem == newItem
        }
    }
}