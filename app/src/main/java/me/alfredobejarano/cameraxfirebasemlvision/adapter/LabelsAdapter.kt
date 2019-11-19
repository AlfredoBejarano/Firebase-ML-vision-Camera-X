package me.alfredobejarano.cameraxfirebasemlvision.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import me.alfredobejarano.cameraxfirebasemlvision.databinding.ItemLabelBinding

/**
 * Created by alfredo on 2019-11-19.
 * Copyright © 2019 GROW. All rights reserved.
 */
class LabelsAdapter(private var labels: Set<String>) :
    RecyclerView.Adapter<LabelsAdapter.LabelViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = LabelViewHolder(
        ItemLabelBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
    )

    override fun getItemCount() = labels.size

    override fun onBindViewHolder(holder: LabelViewHolder, position: Int) =
        holder.bind(labels.elementAt(position))

    fun setLabels(newLabels: Set<String>) = GlobalScope.launch(Dispatchers.IO) {
        val diffCallback = LabelCallback(labels, newLabels)
        val diffs = DiffUtil.calculateDiff(diffCallback, true)
        labels = newLabels
        GlobalScope.launch(Dispatchers.Main) { diffs.dispatchUpdatesTo(this@LabelsAdapter) }
    }

    class LabelCallback(private val oldLabels: Set<String>, private val newLabels: Set<String>) :
        DiffUtil.Callback() {
        override fun getOldListSize() = oldLabels.size

        override fun getNewListSize() = newLabels.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int) =
            oldLabels.elementAt(oldItemPosition) == newLabels.elementAt(newItemPosition)

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int) =
            areItemsTheSame(oldItemPosition, newItemPosition)

    }

    class LabelViewHolder(private val binding: ItemLabelBinding) :
        RecyclerView.ViewHolder(binding.root) {
        /**
         * Binds the label to this item.
         */
        fun bind(label: String) {
            binding.label = label
        }
    }
}