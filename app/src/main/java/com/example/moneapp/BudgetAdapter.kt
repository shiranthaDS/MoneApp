package com.example.moneapp

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.util.Locale

class BudgetAdapter(
    private val budgets: List<Budget>,
    private val onEditClick: (Budget) -> Unit,
    private val onDeleteClick: (Budget) -> Unit
) : RecyclerView.Adapter<BudgetAdapter.BudgetViewHolder>() {

    inner class BudgetViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvCategory: TextView = itemView.findViewById(R.id.tvCategory)
        val tvBudgetAmount: TextView = itemView.findViewById(R.id.tvBudgetAmount)
        val tvSpentAmount: TextView = itemView.findViewById(R.id.tvSpentAmount)
        val progressBar: ProgressBar = itemView.findViewById(R.id.progressBar)
        val tvProgress: TextView = itemView.findViewById(R.id.tvProgress)
        val btnEdit: Button = itemView.findViewById(R.id.btnEdit)
        val btnDelete: Button = itemView.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BudgetViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_budget, parent, false)
        return BudgetViewHolder(view)
    }

    override fun onBindViewHolder(holder: BudgetViewHolder, position: Int) {
        val budget = budgets[position]

        holder.tvCategory.text = budget.category
        holder.tvBudgetAmount.text = String.format(Locale.getDefault(), "$%.2f", budget.amount)
        holder.tvSpentAmount.text = String.format(Locale.getDefault(), "$%.2f / ", budget.spent)

        // Update progress bar
        holder.progressBar.progress = budget.percentSpent.coerceAtMost(100)
        holder.tvProgress.text = "${budget.percentSpent}%"

        // Set progress color based on status
        when {
            budget.isExceeded -> {
                holder.progressBar.progressDrawable.setTint(Color.RED)
                holder.tvProgress.setTextColor(Color.RED)
            }
            budget.isNearLimit -> {
                holder.progressBar.progressDrawable.setTint(Color.parseColor("#FFA500")) // Orange
                holder.tvProgress.setTextColor(Color.parseColor("#FFA500"))
            }
            else -> {
                holder.progressBar.progressDrawable.setTint(Color.GREEN)
                holder.tvProgress.setTextColor(Color.GREEN)
            }
        }

        holder.btnEdit.setOnClickListener { onEditClick(budget) }
        holder.btnDelete.setOnClickListener { onDeleteClick(budget) }
    }

    override fun getItemCount() = budgets.size
}