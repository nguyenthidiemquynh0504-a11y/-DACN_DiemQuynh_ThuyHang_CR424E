package com.hienpc.bmiapp.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.hienpc.bmiapp.R
import com.hienpc.bmiapp.data.repository.UserRepository
import com.hienpc.bmiapp.data.repository.MeasurementRepository
import com.hienpc.bmiapp.databinding.FragmentDashboardBinding
import com.hienpc.bmiapp.databinding.DialogUpdateMeasurementBinding
import com.hienpc.bmiapp.utils.*
import com.hienpc.bmiapp.viewmodel.DashboardViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.hienpc.bmiapp.data.model.*

/**
 * DashboardFragment - hiển thị weight, BMI, calories hôm nay.
 */
class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DashboardViewModel by viewModels {
        DashboardViewModel.Factory(
            UserRepository(),
            MeasurementRepository()
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.swipeRefresh.setOnRefreshListener {
            viewModel.loadDashboard()
        }

        binding.buttonUpdateMeasurement.setOnClickListener {
            showMeasurementDialog()
        }

        observeViewModel()
        viewModel.loadDashboard()
        viewModel.loadWeeklySummary()
        viewModel.loadTrendAnalysis()
        viewModel.loadWeightPrediction(7) // Load 7-day prediction
    }

    private fun observeViewModel() {
        viewModel.dashboardState.observe(viewLifecycleOwner, Observer { state ->
            // Clear all previous states first
            binding.swipeRefresh.isRefreshing = false
            binding.progressBar.visibility = View.GONE
            binding.textError.visibility = View.GONE
            
            // Clear custom states from container
            (binding.root as? ViewGroup)?.clearAllStates()
            
            when (state) {
                is UiState.Idle -> {
                    // Do nothing, just cleared states above
                }
                
                is UiState.Loading -> {
                    if (!binding.swipeRefresh.isRefreshing) {
                        binding.progressBar.visibility = View.VISIBLE
                    }
                }
                
                is UiState.Success -> {
                    // Show data
                    val data = state.data
                    val weightText = data.currentWeight?.let {
                        getString(R.string.dashboard_weight_format, it)
                    } ?: getString(R.string.dashboard_weight_placeholder)
                    binding.textCurrentWeight.text = weightText

                    val bmiText = data.bmi?.let {
                        getString(R.string.dashboard_bmi_format, it)
                    } ?: getString(R.string.dashboard_bmi_placeholder)
                    binding.textBmi.text = bmiText

                    binding.textBmiStatus.setText(
                        if (data.bmi != null) R.string.bmi_latest_measurement else R.string.bmi_no_measurement
                    )
                    val calorieGoal = data.dailyCalorieGoal?.takeIf { it > 0 }
                    binding.textCalories.text = if (calorieGoal == null) {
                        getString(R.string.dashboard_calories_format, data.totalCaloriesToday)
                    } else {
                        getString(R.string.bmi_energy_amount, data.totalCaloriesToday)
                    }
                    binding.textCaloriesGoal.text = calorieGoal?.let {
                        getString(R.string.bmi_energy_goal_inline, it)
                    }.orEmpty()
                    val percent = calorieGoal?.let { (data.totalCaloriesToday * 100.0 / it).toInt() }
                    val progressVisibility = if (percent == null) View.GONE else View.VISIBLE
                    binding.progressCalories.visibility = progressVisibility
                    binding.textCaloriesPercent.visibility = progressVisibility
                    binding.textNoCalorieGoal.visibility = if (percent == null) View.VISIBLE else View.GONE
                    binding.progressCalories.progress = (percent ?: 0).coerceIn(0, 100)
                    binding.textCaloriesPercent.text = percent?.let {
                        getString(R.string.bmi_energy_percent, it)
                    }.orEmpty()
                }
                
                is UiState.Empty -> {
                    // Show empty state
                    (binding.root as? ViewGroup)?.showEmptyState(
                        title = getString(R.string.empty_dashboard),
                        message = state.message,
                        iconRes = R.drawable.ic_dashboard,
                        actionText = "Log bữa ăn",
                        onActionClick = {
                            // Navigate to log screen
                            // TODO: implement navigation
                        }
                    )
                }
                
                is UiState.Error -> {
                    // Show error state with retry
                    (binding.root as? ViewGroup)?.showErrorState(
                        message = state.message,
                        onRetryClick = {
                            viewModel.loadDashboard()
                        }
                    )
                }
            }
        })

        viewModel.measurementState.observe(viewLifecycleOwner, Observer { state ->
            when (state) {
                is UiState.Loading -> {
                    binding.buttonUpdateMeasurement.isEnabled = false
                }
                is UiState.Success -> {
                    binding.buttonUpdateMeasurement.isEnabled = true
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.measurement_update_success),
                        Toast.LENGTH_SHORT
                    ).show()
                    viewModel.resetMeasurementState()
                }
                is UiState.Error -> {
                    binding.buttonUpdateMeasurement.isEnabled = true
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                    viewModel.resetMeasurementState()
                }
                is UiState.Empty -> {
                    // Should not happen for measurement
                }
                else -> Unit
            }
        })
        
        // Observe weekly summary for charts
        viewModel.weeklySummaryState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is UiState.Success -> {
                    val data = state.data
                    // Setup charts
                    ChartHelper.setupWeightChart(binding.chartWeight, data.dailySummaries)
                    ChartHelper.setupCaloriesChart(binding.chartCalories, data.dailySummaries)
                    
                    // Show chart cards
                    binding.cardWeightChart.show()
                    binding.cardCaloriesChart.show()
                }
                is UiState.Error -> {
                    // Hide charts on error
                    binding.cardWeightChart.hide()
                    binding.cardCaloriesChart.hide()
                }
                is UiState.Empty -> {
                    // Hide charts when empty
                    binding.cardWeightChart.hide()
                    binding.cardCaloriesChart.hide()
                }
                else -> {
                    // Loading or Idle - keep current state
                }
            }
        }
        
        // Observe trend analysis for insights
        viewModel.trendAnalysisState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is UiState.Success -> {
                    val data = state.data
                    binding.textInsight.text = buildString {
                        append("${getTrendEmoji(data.weightTrend)} ${data.insight}\n\n")
                        
                        if (data.weightChangeRate != 0.0) {
                            val changeText = if (data.weightChangeRate > 0) {
                                "Tăng ${String.format("%.1f", data.weightChangeRate)} kg/tuần"
                            } else {
                                "Giảm ${String.format("%.1f", Math.abs(data.weightChangeRate))} kg/tuần"
                            }
                            append("📊 $changeText\n")
                        }
                        
                        append("🍽️ Trung bình: ${data.avgDailyCalories.toInt()} kcal/ngày\n")
                        append("💪 Trung bình: ${String.format("%.1f", data.avgWeeklyExercises)} lần tập/tuần\n")
                        
                        if (data.daysToGoal != null) {
                            append("\n🎯 Dự kiến đạt mục tiêu sau ${data.daysToGoal} ngày")
                        }
                    }
                    binding.cardTrendInsight.show()
                }
                is UiState.Error -> {
                    binding.cardTrendInsight.hide()
                }
                is UiState.Empty -> {
                    binding.cardTrendInsight.hide()
                }
                else -> {}
            }
        }
        
        // Observe weight prediction (AI)
        viewModel.weightPredictionState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is UiState.Success -> {
                    val data = state.data
                    
                    // Setup prediction chart
                    ChartHelper.setupWeightPredictionChart(
                        binding.chartWeightPrediction,
                        data.historicalData,
                        data.predictions
                    )
                    
                    // Display AI insights
                    binding.textPredictionInsights.text = data.insights
                    
                    // Show trend indicator
                    val trendText = when (data.metrics.trend) {
                        "INCREASING" -> "📈 Tăng"
                        "DECREASING" -> "📉 Giảm"
                        else -> "➡️ Ổn định"
                    }
                    binding.textPredictionTrend.text = trendText
                    binding.textPredictionTrend.setTextColor(
                        when (data.metrics.trend) {
                            "INCREASING" -> android.graphics.Color.parseColor("#E91E63")
                            "DECREASING" -> android.graphics.Color.parseColor("#4CAF50")
                            else -> android.graphics.Color.parseColor("#9E9E9E")
                        }
                    )
                    
                    // Show card
                    binding.cardWeightPrediction.show()
                }
                is UiState.Error -> {
                    // Hide prediction card on error
                    binding.cardWeightPrediction.hide()
                    Toast.makeText(requireContext(), "Không thể tải dự đoán: ${state.message}", Toast.LENGTH_SHORT).show()
                }
                is UiState.Empty -> {
                    // Hide prediction card when empty
                    binding.cardWeightPrediction.hide()
                }
                is UiState.Loading -> {
                    // Show loading on card
                    binding.textPredictionInsights.text = "⏳ Đang phân tích và dự đoán..."
                }
                else -> {}
            }
        }
    }
    
    private fun getTrendEmoji(trend: String): String {
        return when (trend) {
            "LOSING" -> "📉"
            "GAINING" -> "📈"
            else -> "➡️"
        }
    }

    private fun showMeasurementDialog() {
        val dialogBinding = DialogUpdateMeasurementBinding.inflate(layoutInflater, null, false)

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.measurement_dialog_title))
            .setView(dialogBinding.root)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.measurement_save_button, null)
            .create()

        dialog.setOnShowListener {
            val button = dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)
            button.setOnClickListener {
                val weightInput = dialogBinding.editTextWeight.text?.toString()?.replace(',', '.')
                val weight = weightInput?.toDoubleOrNull()
                if (weight == null || weight <= 0) {
                    dialogBinding.inputWeight.error = getString(R.string.measurement_weight_error)
                    return@setOnClickListener
                } else {
                    dialogBinding.inputWeight.error = null
                }

                val heightInput = dialogBinding.editTextHeight.text?.toString()?.replace(',', '.')
                val height = heightInput?.toDoubleOrNull()

                viewModel.addMeasurement(weight, height)
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
