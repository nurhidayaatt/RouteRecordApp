package com.nurhidayaatt.routerecordapp.presentation.routes_data.routes

import android.annotation.SuppressLint
import android.graphics.drawable.InsetDrawable
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.view.menu.MenuBuilder
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.maps.*
import com.nurhidayaatt.routerecordapp.R
import com.nurhidayaatt.routerecordapp.databinding.FragmentRoutesBinding
import com.nurhidayaatt.routerecordapp.domain.model.Routes
import com.nurhidayaatt.routerecordapp.presentation.SharedViewModel
import com.nurhidayaatt.routerecordapp.presentation.main.MainActivity
import com.nurhidayaatt.routerecordapp.presentation.routes_data.FilterActivity
import com.nurhidayaatt.routerecordapp.presentation.routes_data.FilterTime
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class RoutesFragment : Fragment(R.layout.fragment_routes) {

    private val sharedViewModel by activityViewModels<SharedViewModel>()
    private var binding: FragmentRoutesBinding? = null

    private lateinit var listRoutesAdapter: ListRoutesAdapter

    @SuppressLint("MissingPermission")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentRoutesBinding.bind(view)

        binding?.let { binding ->
            binding.chipSort.setOnClickListener { showPopUpMenu(it) }
            binding.chipDateRange.setOnClickListener { showPopUpMenu(it) }
            binding.chipActivity.setOnClickListener { showPopUpMenu(it) }

            listRoutesAdapter = ListRoutesAdapter(requireContext())
            binding.rvRoute.apply {
                layoutManager = LinearLayoutManager(requireContext())
                setHasFixedSize(false)
                adapter = listRoutesAdapter
            }

            listRoutesAdapter.setOnMenuClickListener { routes, v -> showItemMenu(v, routes) }

            lifecycleScope.launch {
                viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.CREATED) {
                    launch {
                        sharedViewModel.routePref.collectLatest {
                            binding.chipSort.text = when (it.sortType) {
                                SortType.TIMESTAMP_DESC -> resources.getString(R.string.sort_latest)
                                SortType.TIMESTAMP_ASC -> resources.getString(R.string.sort_earliest)
                                SortType.DISTANCE_DESC -> resources.getString(R.string.sort_longest)
                                SortType.DISTANCE_ASC -> resources.getString(R.string.sort_shortest)
                                SortType.DURATION_DESC -> resources.getString(R.string.sort_longest_time)
                                SortType.DURATION_ASC -> resources.getString(R.string.sort_shortest_time)
                            }
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
                        }
                    }
                    launch {
                        sharedViewModel.routesData.collect {
                            if (it.isNotEmpty()) {
                                binding.rvRoute.visibility = View.VISIBLE
                                binding.layoutEmptyData.emptyData.visibility = View.GONE

                                listRoutesAdapter.setListRoutes(it)

                                listRoutesAdapter.setOnCardClickListener { position ->
                                    /*Toast.makeText(this@RoutesFragment.activity, position.toString(), Toast.LENGTH_SHORT)
                                        .show()*/
                                    sharedViewModel.setRoute(routes = it, position = position)
                                }
                            } else {
                                binding.rvRoute.visibility = View.GONE
                                binding.layoutEmptyData.emptyData.visibility = View.VISIBLE
                            }
                        }
                    }
                }
            }
        }
    }

    private fun showPopUpMenu(v: View) {
        when (v.id) {
            R.id.chip_sort -> {
                val menuSort = PopupMenu(requireContext(), v)
                menuSort.menuInflater.inflate(R.menu.sort_menu, menuSort.menu)

                menuSort.setOnMenuItemClickListener { menuItem: MenuItem ->
                    return@setOnMenuItemClickListener when (menuItem.itemId) {
                        R.id.option_latest -> {
                            sharedViewModel.updateSortTypeRoute(sortType = SortType.TIMESTAMP_DESC)
                            true
                        }
                        R.id.option_earliest -> {
                            sharedViewModel.updateSortTypeRoute(sortType = SortType.TIMESTAMP_ASC)
                            true
                        }
                        R.id.option_longest -> {
                            sharedViewModel.updateSortTypeRoute(sortType = SortType.DISTANCE_DESC)
                            true
                        }
                        R.id.option_shortest -> {
                            sharedViewModel.updateSortTypeRoute(sortType = SortType.DISTANCE_ASC)
                            true
                        }
                        R.id.option_longest_time -> {
                            sharedViewModel.updateSortTypeRoute(sortType = SortType.DURATION_DESC)
                            true
                        }
                        R.id.option_shortest_time -> {
                            sharedViewModel.updateSortTypeRoute(sortType = SortType.DURATION_ASC)
                            true
                        }
                        else -> super.onContextItemSelected(menuItem)
                    }
                }
                menuSort.show()
            }
            R.id.chip_date_range -> {
                val menuDateRange = PopupMenu(requireContext(), v)
                menuDateRange.menuInflater.inflate(R.menu.time_range_menu, menuDateRange.menu)

                menuDateRange.setOnMenuItemClickListener { menuItem: MenuItem ->
                    return@setOnMenuItemClickListener when (menuItem.itemId) {
                        R.id.option_all_time -> {
                            sharedViewModel.updateFilterTimeRoute(filterTime = FilterTime.ALL_TIME)
                            true
                        }
                        R.id.option_last_week -> {
                            sharedViewModel.updateFilterTimeRoute(filterTime = FilterTime.LAST_WEEK)
                            true
                        }
                        R.id.option_last_month -> {
                            sharedViewModel.updateFilterTimeRoute(filterTime = FilterTime.LAST_MONTH)
                            true
                        }
                        R.id.option_last_year -> {
                            sharedViewModel.updateFilterTimeRoute(filterTime = FilterTime.LAST_YEAR)
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
                            sharedViewModel.updateFilterActivity(filterActivity = FilterActivity.ALL_ACTIVITY)
                            true
                        }
                        R.id.option_running -> {
                            sharedViewModel.updateFilterActivity(filterActivity = FilterActivity.RUNNING)
                            true
                        }
                        R.id.option_cycling -> {
                            sharedViewModel.updateFilterActivity(filterActivity = FilterActivity.CYCLING)
                            true
                        }
                        else -> super.onContextItemSelected(menuItem)
                    }
                }
                menuActivity.show()
            }
        }
    }

    @SuppressLint("RestrictedApi")
    private fun showItemMenu(v: View, routes: Routes) {
        val popup = PopupMenu(requireContext(), v)
        popup.menuInflater.inflate(R.menu.item_list_menu, popup.menu)
        popup.gravity = Gravity.END

        if (popup.menu is MenuBuilder) {
            val menuBuilder = popup.menu as MenuBuilder
            menuBuilder.setOptionalIconsVisible(true)
            for (item in menuBuilder.visibleItems) {
                val iconMarginPx = TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP, 12.toFloat(), resources.displayMetrics
                    ).toInt()
                if (item.icon != null) {
                    item.icon = object : InsetDrawable(
                        item.icon,
                        iconMarginPx,
                        0,
                        iconMarginPx,
                        0) {
                            override fun getIntrinsicWidth(): Int {
                                return intrinsicHeight + iconMarginPx + iconMarginPx
                            }
                        }
                }
            }
        }

        popup.setOnMenuItemClickListener { menuItem: MenuItem ->
            return@setOnMenuItemClickListener when (menuItem.itemId) {
                R.id.option_delete -> {
                    sharedViewModel.deleteData(routesId = routes.id!!)
                    true
                }
                R.id.option_refresh_image -> {
                    viewLifecycleOwner.lifecycleScope.launch {
                        (activity as MainActivity).updateImageRoute(data = routes)
                    }
                    true
                }
                else -> super.onContextItemSelected(menuItem)
            }
        }

        popup.show()
    }

    override fun onDestroy() {
        binding = null
        super.onDestroy()
    }
}