package com.nurhidayaatt.routerecordapp.presentation.routes_data

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.nurhidayaatt.routerecordapp.presentation.routes_data.routes.RoutesFragment
import com.nurhidayaatt.routerecordapp.presentation.routes_data.statistics.StatisticFragment
import com.nurhidayaatt.routerecordapp.util.Constants.NUM_TABS

class TabPagerAdapter(fragmentManager: FragmentManager, lifecycle: Lifecycle): FragmentStateAdapter(fragmentManager, lifecycle) {

    override fun getItemCount(): Int = NUM_TABS

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> RoutesFragment()
            else -> StatisticFragment()
        }
    }


}