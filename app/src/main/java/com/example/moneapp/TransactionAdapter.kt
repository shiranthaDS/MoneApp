package com.example.moneapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Locale

class TransactionAdapter(
    private var transactions: MutableList<Transaction>,
    private val onEditClick: (Transaction) -> Unit,
    private val onDeleteClick: (Transaction) -> Unit
) : RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder>() {

    inner class TransactionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
        val tvAmount: TextView = itemView.findViewById(R.id.tvAmount)
        val tvCategory: TextView = itemView.findViewById(R.id.tvCategory)
        val tvDate: TextView = itemView.findViewById(R.id.tvDate)
        val btnEdit: Button = itemView.findViewById(R.id.btnEdit)
        val btnDelete: Button = itemView.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_transaction, parent, false)
        return TransactionViewHolder(view)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        val transaction = transactions[position]
        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

        holder.tvTitle.text = transaction.title
        holder.tvAmount.text = String.format(Locale.getDefault(), "$%.2f", transaction.amount)
        holder.tvCategory.text = transaction.category
        holder.tvDate.text = dateFormat.format(transaction.date)

        holder.tvAmount.setTextColor(
            when (transaction.type) {
                "Income" -> holder.itemView.context.getColor(android.R.color.holo_green_dark)
                else -> holder.itemView.context.getColor(android.R.color.holo_red_dark)
            }
        )

        holder.btnEdit.setOnClickListener { onEditClick(transaction) }
        holder.btnDelete.setOnClickListener { onDeleteClick(transaction) }
    }

    override fun getItemCount() = transactions.size

    fun updateTransactions(newTransactions: MutableList<Transaction>) {
        transactions = newTransactions
        notifyDataSetChanged()
    }

    fun removeTransaction(transaction: Transaction) {
        val position = transactions.indexOf(transaction)
        if (position != -1) {
            transactions.removeAt(position)
            notifyItemRemoved(position)
        }
    }
}