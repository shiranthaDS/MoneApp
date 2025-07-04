package com.example.moneapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.util.Locale

class CategorySummaryAdapter(
    private var categories: List<CategorySummary>,
    private val isIncome: Boolean
) : RecyclerView.Adapter<CategorySummaryAdapter.CategoryViewHolder>() {

    inner class CategoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvCategoryName: TextView = itemView.findViewById(R.id.tvCategoryName)
        val tvCategoryAmount: TextView = itemView.findViewById(R.id.tvCategoryAmount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category_summary, parent, false)
        return CategoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        val category = categories[position]
        holder.tvCategoryName.text = category.category
        holder.tvCategoryAmount.text = String.format(Locale.getDefault(), "$%.2f", category.amount)

        // Set text color based on whether it's income or expense
        holder.tvCategoryAmount.setTextColor(
            if (isIncome)
                holder.itemView.context.getColor(android.R.color.holo_green_dark)
            else
                holder.itemView.context.getColor(android.R.color.holo_red_dark)
        )
    }

    override fun getItemCount() = categories.size

    fun updateCategories(newCategories: List<CategorySummary>) {
        categories = newCategories
        notifyDataSetChanged()
    }
}