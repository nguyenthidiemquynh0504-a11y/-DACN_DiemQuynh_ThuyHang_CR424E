package com.hienpc.bmiapp.ui.main.log

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.hienpc.bmiapp.R
import com.hienpc.bmiapp.data.model.RecommendationItem
import com.hienpc.bmiapp.databinding.ItemRecommendationBinding

/**
 * Adapter for displaying AI recommendations
 */
class RecommendationAdapter(
    private var items: List<RecommendationItem>,
    private val onItemClick: (RecommendationItem) -> Unit
) : RecyclerView.Adapter<RecommendationAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemRecommendationBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    fun updateItems(newItems: List<RecommendationItem>) {
        items = newItems
        notifyDataSetChanged()
    }
    
    fun getItems(): List<RecommendationItem> = items

    inner class ViewHolder(
        private val binding: ItemRecommendationBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: RecommendationItem) {
            binding.textItemName.text = item.name
            binding.textReason.text = item.reason
            val context = binding.root.context
            if (item.unit == "Exercise") {
                binding.imageIcon.setImageResource(R.drawable.ic_exercise_outline)
                binding.textCalories.text = context.getString(R.string.recommendation_exercise_energy, item.calories)
            } else {
                binding.imageIcon.setImageResource(R.drawable.ic_bowl_outline)
                binding.textCalories.text = context.getString(R.string.recommendation_food_energy, item.calories, item.unit)
            }

            binding.root.setOnClickListener {
                onItemClick(item)
            }
        }
    }
}

