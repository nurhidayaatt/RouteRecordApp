package com.nurhidayaatt.routerecordapp.presentation.routes_data.routes

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.RoundedCornersTransformation
import com.nurhidayaatt.routerecordapp.R
import com.nurhidayaatt.routerecordapp.databinding.ItemRoutesBinding
import com.nurhidayaatt.routerecordapp.domain.model.Routes
import com.nurhidayaatt.routerecordapp.presentation.main.ActivityType
import com.nurhidayaatt.routerecordapp.util.dp
import com.nurhidayaatt.routerecordapp.util.formatDistance
import com.nurhidayaatt.routerecordapp.util.formatPace
import com.nurhidayaatt.routerecordapp.util.toDurationString
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

// TODO: check if this diff util implementation correct or not
class ListRoutesAdapter(private val context: Context): RecyclerView.Adapter<ListRoutesAdapter.RoutesViewHolder>() {

    private val listRoutes = mutableListOf<Routes>()

    fun setListRoutes(listRoutes: List<Routes>) {
        val diffCallback = DiffCallbackRoutes(this.listRoutes, listRoutes)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        this.listRoutes.clear()
        this.listRoutes.addAll(listRoutes)
        diffResult.dispatchUpdatesTo(this)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RoutesViewHolder {
        val binding = ItemRoutesBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return RoutesViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return listRoutes.size
    }

    override fun onBindViewHolder(holder: RoutesViewHolder, position: Int) {
        holder.bind(listRoutes[position], position)
    }

    inner class RoutesViewHolder(private val binding: ItemRoutesBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(routes: Routes, position: Int) {
            with(binding) {
                if(routes.typeActivity == ActivityType.RUNNING.name) {
                    iconButton.icon = ContextCompat.getDrawable(context, R.drawable.ic_directions_run)
                    tvTitle.text = "Running"
                } else if (routes.typeActivity == ActivityType.CYCLING.name) {
                    iconButton.icon = ContextCompat.getDrawable(context, R.drawable.ic_directions_bike)
                    tvTitle.text = "Cycling"
                }

                val timestamp = routes.timestamp.atZone(ZoneId.systemDefault())
                val date = timestamp?.toLocalDate()?.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL))
                val time = timestamp?.toLocalTime()?.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))
                tvTimestamp.text = String.format(context.getString(R.string.format_date_time), date, time)

                ivRoute.load(routes.img) {
                    transformations(RoundedCornersTransformation(radius = 12.dp(context)))
                }

                tvDistance.text = routes.distancePerRoute.formatDistance()
                tvDuration.text = routes.elapsedTime.toDurationString(context)
                tvPace.text = routes.pacePerKm.formatPace()
                btnMore.setOnClickListener {
                    onMenuClickListener(routes, it)
                }
                cardView.setOnClickListener {
                    onCardClickListener(position)
                }
            }
        }
    }

    private lateinit var onMenuClickListener: ((Routes, View) -> Unit)
    fun setOnMenuClickListener(listener: (Routes, View) -> Unit) {
        onMenuClickListener = listener
    }

    private lateinit var onCardClickListener: ((Int) -> Unit)
    fun setOnCardClickListener(listener: (Int) -> Unit) {
        onCardClickListener = listener
    }
}