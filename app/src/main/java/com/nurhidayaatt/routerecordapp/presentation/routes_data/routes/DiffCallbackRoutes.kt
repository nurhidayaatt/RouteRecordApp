package com.nurhidayaatt.routerecordapp.presentation.routes_data.routes

import androidx.recyclerview.widget.DiffUtil
import com.nurhidayaatt.routerecordapp.domain.model.Routes

class DiffCallbackRoutes(
    private val oldSuggestionList: List<Routes>,
    private val newSuggestionList: List<Routes>
): DiffUtil.Callback() {
    override fun getOldListSize(): Int {
        return oldSuggestionList.size
    }

    override fun getNewListSize(): Int {
        return newSuggestionList.size
    }

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldSuggestionList[oldItemPosition] == newSuggestionList[newItemPosition]
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val oldSuggestion = oldSuggestionList[oldItemPosition]
        val newSuggestion = newSuggestionList[newItemPosition]
        return oldSuggestion.id == newSuggestion.id
    }
}