package com.nurhidayaatt.routerecordapp.presentation.main

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.nurhidayaatt.routerecordapp.R
import com.nurhidayaatt.routerecordapp.databinding.ItemRoutesPagerBinding
import com.nurhidayaatt.routerecordapp.domain.model.Routes
import com.nurhidayaatt.routerecordapp.util.formatDistance
import com.nurhidayaatt.routerecordapp.util.toDurationString
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

class CarouselPagerAdapter(private val context: Context) :
    RecyclerView.Adapter<CarouselPagerAdapter.CarouselItemViewHolder>() {

    private val differCallback = object : DiffUtil.ItemCallback<Routes>(){
        override fun areItemsTheSame(oldItem: Routes, newItem: Routes): Boolean {
            return  oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Routes, newItem: Routes): Boolean {
            return oldItem == newItem
        }
    }

    val differ = AsyncListDiffer(this, differCallback)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CarouselItemViewHolder {
        val binding = ItemRoutesPagerBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CarouselItemViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return differ.currentList.size
    }

    override fun onBindViewHolder(holder: CarouselItemViewHolder, position: Int) {
        holder.bind(differ.currentList[position])
    }

    inner class CarouselItemViewHolder(private val binding: ItemRoutesPagerBinding) :
        RecyclerView.ViewHolder(binding.root) {
            fun bind(routes: Routes) {
                with(binding) {
                    ivRoute.load(routes.img)
                    val timestamp = routes.timestamp.atZone(ZoneId.systemDefault())
                    val date = timestamp?.toLocalDate()?.format(
                        DateTimeFormatter.ofLocalizedDate(
                            FormatStyle.FULL))
                    val time = timestamp?.toLocalTime()?.format(
                        DateTimeFormatter.ofLocalizedTime(
                            FormatStyle.SHORT))
                    tvTimestamp.text = String.format(context.getString(R.string.format_date_time), date, time)
                    tvDistance.text = routes.distance.formatDistance()
                    tvDuration.text = routes.elapsedTime.toDurationString(context)
                }
            }
        }
}