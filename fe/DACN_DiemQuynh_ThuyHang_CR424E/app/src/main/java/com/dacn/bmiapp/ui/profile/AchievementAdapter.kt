package com.hienpc.bmiapp.ui.profile

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.hienpc.bmiapp.R
import com.hienpc.bmiapp.data.model.Achievement
import com.hienpc.bmiapp.databinding.ItemAchievementBinding
import java.text.SimpleDateFormat
import java.util.*

class AchievementAdapter : ListAdapter<Achievement, AchievementAdapter.ViewHolder>(DiffCallback) {

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<Achievement>() {
            override fun areItemsTheSame(oldItem: Achievement, newItem: Achievement): Boolean {
                return oldItem.achievementType == newItem.achievementType
            }

            override fun areContentsTheSame(oldItem: Achievement, newItem: Achievement): Boolean {
                return oldItem == newItem
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAchievementBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(
        private val binding: ItemAchievementBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(achievement: Achievement) {
            binding.textAchievementTitle.text = achievement.title.trimStart { !it.isLetterOrDigit() }
            binding.textAchievementDescription.text = achievement.description

            // Set icon (using emoji from title for now, can be replaced with actual icons)
            val iconResId = getIconResource(achievement.iconName)
            binding.imageAchievementIcon.setImageResource(iconResId)

            if (achievement.isUnlocked) {
                // Unlocked state
                binding.viewLockedOverlay.visibility = View.GONE
                binding.imageLockIcon.visibility = View.GONE
                binding.textAchievedDate.visibility = View.VISIBLE
                
                // Format achieved date
                achievement.achievedAt?.let { dateStr ->
                    try {
                        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                        val outputFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                        val date = inputFormat.parse(dateStr)
                        binding.textAchievedDate.text = "Đạt được: ${date?.let { outputFormat.format(it) }}"
                    } catch (e: Exception) {
                        binding.textAchievedDate.text = "Đạt được: $dateStr"
                    }
                }
            } else {
                // Locked state
                binding.viewLockedOverlay.visibility = View.VISIBLE
                binding.imageLockIcon.visibility = View.VISIBLE
                binding.textAchievedDate.visibility = View.GONE
            }
        }

        private fun getIconResource(iconName: String): Int {
            // Map icon names to drawable resources
            // For now, using default Android icons
            return when (iconName) {
                "ic_achievement_first" -> R.drawable.ic_star_outline
                "ic_achievement_exercise" -> R.drawable.ic_bars_outline
                "ic_achievement_7day" -> R.drawable.ic_calendar_check
                "ic_achievement_30day" -> R.drawable.ic_calendar_check
                "ic_achievement_100day" -> R.drawable.ic_star_outline
                "ic_achievement_goal" -> R.drawable.ic_bars_outline
                "ic_achievement_50logs" -> R.drawable.ic_edit_outline
                "ic_achievement_100logs" -> R.drawable.ic_log
                "ic_achievement_early" -> R.drawable.ic_clock_outline
                "ic_achievement_night" -> R.drawable.ic_clock_outline
                else -> R.drawable.ic_star_outline
            }
        }
    }
}
