package com.m.series.scanner.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.m.series.scanner.ble.KeiserBikeData
import com.m.series.scanner.databinding.ItemBikeBinding

class BikeListAdapter(
    private val onBikeSelected: (KeiserBikeData) -> Unit,
) : ListAdapter<KeiserBikeData, BikeListAdapter.ViewHolder>(DiffCallback) {

    inner class ViewHolder(private val binding: ItemBikeBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(data: KeiserBikeData) {
            binding.tvBikeId.text = "Bike #%d".format(data.equipmentId)
            binding.tvBikeFirmware.text = "Firmware v${data.firmwareVersion}"
            binding.tvBikeCadence.text = "%.1f RPM".format(data.cadenceRpm)
            binding.tvBikePower.text = "${data.powerWatts} W"
            binding.root.setOnClickListener { onBikeSelected(data) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(ItemBikeBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) =
        holder.bind(getItem(position))

    private object DiffCallback : DiffUtil.ItemCallback<KeiserBikeData>() {
        override fun areItemsTheSame(a: KeiserBikeData, b: KeiserBikeData) =
            a.equipmentId == b.equipmentId
        override fun areContentsTheSame(a: KeiserBikeData, b: KeiserBikeData) =
            a == b
    }
}
