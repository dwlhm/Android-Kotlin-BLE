package com.dwlhm.cpacp_basic

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.res.TypedArrayUtils.getText
import androidx.recyclerview.widget.RecyclerView

internal class DeviceAdapter(private var deviceList: List<ModelDevice>):
    RecyclerView.Adapter<DeviceAdapter.MyViewHolder>() {

    var listener: RecyclerViewClickListener? = null

    internal inner class MyViewHolder(view: View) :
        RecyclerView.ViewHolder(view) {
            var mac: TextView = view.findViewById(R.id.device_mac)
            var date: TextView = view.findViewById(R.id.device_date)
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int)
        : MyViewHolder {
            val itemView = LayoutInflater.from(parent.context)
                .inflate(R.layout.device_list, parent, false)
            return MyViewHolder(itemView)
        }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val device = deviceList[position]
        holder.mac.text = device.getMac()
        holder.date.text = device.getDate()

        // onClick Event
        holder.itemView.setOnClickListener {
            listener?.onItemClicked(it, deviceList[position])
        }
    }

    override fun getItemCount(): Int {
        return deviceList.size
    }

}

