package com.hienpc.bmiapp.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.material.tabs.TabLayoutMediator
import com.hienpc.bmiapp.R
import com.hienpc.bmiapp.databinding.FragmentLogContainerBinding

/**
 * LogContainerFragment - chứa 2 tab: Food Log & Exercise Log.
 */
class LogContainerFragment : Fragment() {

    private var _binding: FragmentLogContainerBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLogContainerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = LogPagerAdapter(this)
        binding.viewPager.adapter = adapter

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            if (position == 0) {
                tab.setText(R.string.bmi_log_food_tab).setIcon(R.drawable.ic_meal_outline)
            } else {
                tab.setText(R.string.bmi_log_exercise_tab).setIcon(R.drawable.ic_exercise_outline)
            }
        }.attach()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
