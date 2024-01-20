package com.nurhidayaatt.routerecordapp.presentation.routes_data.statistics

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.android.material.chip.Chip
import com.nurhidayaatt.routerecordapp.R
import com.nurhidayaatt.routerecordapp.databinding.FragmentStatisticBinding
import com.nurhidayaatt.routerecordapp.presentation.SharedViewModel
import com.nurhidayaatt.routerecordapp.presentation.routes_data.FilterActivity
import com.nurhidayaatt.routerecordapp.presentation.routes_data.FilterTime
import com.nurhidayaatt.routerecordapp.presentation.routes_data.routes.SortType
import com.nurhidayaatt.routerecordapp.util.colorOnSurface
import com.nurhidayaatt.routerecordapp.util.colorPrimary
import com.nurhidayaatt.routerecordapp.util.colorPrimaryContainer
import com.nurhidayaatt.routerecordapp.util.formatDistance
import com.nurhidayaatt.routerecordapp.util.toDurationString
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@AndroidEntryPoint
class StatisticFragment : Fragment(R.layout.fragment_statistic) {

    private val sharedViewModel by activityViewModels<SharedViewModel>()
    private var binding: FragmentStatisticBinding? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentStatisticBinding.bind(view)

        binding?.let { binding ->
            binding.chipDateRange.setOnClickListener { showPopUpMenu(it) }
            binding.chipActivity.setOnClickListener { showPopUpMenu(it) }
            binding.chipDataset.setOnClickListener { showPopUpMenu(it) }

            binding.cvDistance.setOnClickListener {
                sharedViewModel.updateDataTypeStatistic(dataType = DataType.DISTANCE)
            }

            binding.cvDuration.setOnClickListener {
                sharedViewModel.updateDataTypeStatistic(dataType = DataType.DURATION)
            }

            binding.chart.setTouchEnabled(true)
            binding.chart.xAxis.position = XAxis.XAxisPosition.BOTTOM
            binding.chart.xAxis.textColor = requireContext().colorOnSurface
            binding.chart.xAxis.granularity = 1f
            binding.chart.xAxis.setDrawGridLines(false)
            binding.chart.axisLeft.setDrawGridLines(false)
            binding.chart.axisLeft.textColor = requireContext().colorOnSurface
            binding.chart.axisLeft.axisMinimum = 0f
            binding.chart.axisRight.isEnabled = false
            binding.chart.legend.textColor = requireContext().colorOnSurface
            binding.chart.legend.isEnabled = false
            val description = Description()
            description.text = ""
            binding.chart.description = description

            viewLifecycleOwner.lifecycleScope.launch {
                viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                    launch {
                        sharedViewModel.statisticPrefs.collectLatest {
                            binding.chipDateRange.text = when (it.filterTime) {
                                FilterTime.ALL_TIME -> resources.getString(R.string.all_time)
                                FilterTime.LAST_WEEK -> resources.getString(R.string.last_week)
                                FilterTime.LAST_MONTH -> resources.getString(R.string.last_month)
                                FilterTime.LAST_YEAR -> resources.getString(R.string.last_year)
                            }

                            binding.chipActivity.text = when (it.filterActivity) {
                                FilterActivity.ALL_ACTIVITY -> resources.getString(R.string.all_activity)
                                FilterActivity.RUNNING -> resources.getString(R.string.running)
                                FilterActivity.CYCLING -> resources.getString(R.string.cycling)
                            }

                            binding.chipDataset.text = when (it.dataset) {
                                Dataset.AVERAGE -> resources.getString(R.string.average)
                                Dataset.TOTAL -> resources.getString(R.string.total)
                            }

                            when (it.dataType) {
                                DataType.DISTANCE -> {
                                    binding.cvDistance.isChecked = true
                                    binding.cvDuration.isChecked = false
                                }
                                DataType.DURATION -> {
                                    binding.cvDuration.isChecked = true
                                    binding.cvDistance.isChecked = false
                                }
                            }
                        }
                    }
                    launch {
                        sharedViewModel.statisticData.collectLatest { allData ->
                            binding.tvDistance.text = allData.distance?.formatDistance() ?: "-"
                            binding.tvDuration.text =
                                allData.duration?.toDurationString(requireContext()) ?: "-"

                            binding.chart.fitScreen()
                            binding.chart.data = BarData(listOf())

                            if (allData.statisticsData.isNotEmpty()) {
                                binding.chart.visibility = View.VISIBLE
                                binding.layoutEmptyData.emptyData.visibility = View.GONE

                                val valueSet: MutableList<BarEntry> = mutableListOf()
                                (0 until allData.statisticsData.size).toList().forEach {
                                    valueSet.add(BarEntry(it.toFloat(), allData.statisticsData[it]))
                                }
                                val barDataSet = BarDataSet(valueSet, "Brand 1")
                                barDataSet.apply {
                                    valueTextSize = 12f
                                    valueTextColor = requireContext().colorOnSurface
                                    color = requireContext().colorPrimaryContainer
                                    isHighlightEnabled = true
                                    highLightColor = requireContext().colorPrimary
                                    highLightAlpha = 255
                                }

                                binding.chart.xAxis.labelCount = allData.label.size
                                binding.chart.xAxis.valueFormatter = xAxisFormatter(allData.label)
                                binding.chart.axisLeft.valueFormatter = yAxisFormatter(allData.type)
                                val barData = BarData(listOf(barDataSet))
                                barData.setValueFormatter(yAxisFormatter(allData.type))
                                binding.chart.data = barData
                                if (allData.label.size > 5) {
                                    binding.chart.setVisibleXRange(0f, 5.5f)
                                }
                                binding.chart.invalidate()
                                binding.chart.animateY(500)
                            } else {
                                binding.chart.visibility = View.GONE
                                binding.layoutEmptyData.emptyData.visibility = View.VISIBLE
                            }
                        }
                    }
                }
            }
        }
    }

    private fun xAxisFormatter(label: List<ZonedDateTime>) = object : ValueFormatter() {
        override fun getAxisLabel(value: Float, axis: AxisBase?): String {
            return label[value.toInt()].toLocalDate().format(
                DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
            ).toString()
        }
    }

    private fun yAxisFormatter(dataType: DataType) = object : ValueFormatter() {
        override fun getBarLabel(barEntry: BarEntry): String {
            return when (dataType) {
                DataType.DISTANCE -> barEntry.y.toDouble().formatDistance()
                DataType.DURATION -> barEntry.y.toLong().toDurationString(requireContext())
            }
        }

        override fun getAxisLabel(value: Float, axis: AxisBase?): String {
            return when (dataType) {
                DataType.DISTANCE -> value.toDouble().formatDistance()
                DataType.DURATION -> value.toLong().toDurationString(requireContext())
            }
        }
    }

    private fun showPopUpMenu(v: View) {
        when (v.id) {
            R.id.chip_date_range -> {
                val menuDateRange = PopupMenu(requireContext(), v)
                menuDateRange.menuInflater.inflate(R.menu.time_range_menu, menuDateRange.menu)

                menuDateRange.setOnMenuItemClickListener { menuItem: MenuItem ->
                    return@setOnMenuItemClickListener when (menuItem.itemId) {
                        R.id.option_all_time -> {
                            sharedViewModel.updateFilterTimeStatistic(filterTime = FilterTime.ALL_TIME)
                            true
                        }
                        R.id.option_last_week -> {
                            sharedViewModel.updateFilterTimeStatistic(filterTime = FilterTime.LAST_WEEK)
                            true
                        }
                        R.id.option_last_month -> {
                            sharedViewModel.updateFilterTimeStatistic(filterTime = FilterTime.LAST_MONTH)
                            true
                        }
                        R.id.option_last_year -> {
                            sharedViewModel.updateFilterTimeStatistic(filterTime = FilterTime.LAST_YEAR)
                            true
                        }
                        else -> super.onContextItemSelected(menuItem)
                    }
                }
                menuDateRange.show()
            }
            R.id.chip_activity -> {
                val menuActivity = PopupMenu(requireContext(), v)
                menuActivity.menuInflater.inflate(R.menu.activity_menu, menuActivity.menu)

                menuActivity.setOnMenuItemClickListener { menuItem: MenuItem ->
                    return@setOnMenuItemClickListener when (menuItem.itemId) {
                        R.id.option_all_activity -> {
                            sharedViewModel.updateFilterActivityStatistic(filterActivity = FilterActivity.ALL_ACTIVITY)
                            true
                        }
                        R.id.option_running -> {
                            sharedViewModel.updateFilterActivityStatistic(filterActivity = FilterActivity.RUNNING)
                            true
                        }
                        R.id.option_cycling -> {
                            sharedViewModel.updateFilterActivityStatistic(filterActivity = FilterActivity.CYCLING)
                            true
                        }
                        else -> super.onContextItemSelected(menuItem)
                    }
                }
                menuActivity.show()
            }
            R.id.chip_dataset -> {
                val menuDataset = PopupMenu(requireContext(), v)
                menuDataset.menuInflater.inflate(R.menu.dataset_menu, menuDataset.menu)

                menuDataset.setOnMenuItemClickListener { menuItem: MenuItem ->
                    return@setOnMenuItemClickListener when (menuItem.itemId) {
                        R.id.option_average -> {
                            sharedViewModel.updateDatasetStatistic(dataset = Dataset.AVERAGE)
                            true
                        }
                        R.id.option_total -> {
                            sharedViewModel.updateDatasetStatistic(dataset = Dataset.TOTAL)
                            true
                        }
                        else -> super.onContextItemSelected(menuItem)
                    }
                }
                menuDataset.show()
            }
        }
    }

    override fun onDestroy() {
        binding = null
        super.onDestroy()
    }
}