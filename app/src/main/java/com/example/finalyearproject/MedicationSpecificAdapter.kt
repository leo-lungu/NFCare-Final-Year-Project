package com.example.finalyearproject

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class MedicationSpecificAdapter(
    // adapter class for displaying a list of medication boxes
    private val boxes: List<MedicationSpecific>
) : RecyclerView.Adapter<MedicationSpecificAdapter.MedicationSpecificViewHolder>() {

    class MedicationSpecificViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // view holder class for each medication box item
        val boxNameTextView: TextView = itemView.findViewById(R.id.boxNameTextView)
        val expirationDateTextView: TextView = itemView.findViewById(R.id.expirationDateTextView)
        val quantityTextView: TextView = itemView.findViewById(R.id.quantityTextView)
        val batchNumberTextView: TextView = itemView.findViewById(R.id.batchNumberTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MedicationSpecificViewHolder {
        // create a new view holder for each medication box item
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_medication_specific, parent, false) // inflate the layout for the item
        return MedicationSpecificViewHolder(view)
    }

    override fun onBindViewHolder(holder: MedicationSpecificViewHolder, position: Int) {
        // bind the data to the view holder
        val box = boxes[position]
        holder.boxNameTextView.text = box.boxName
        holder.expirationDateTextView.text = "Exp: ${box.expirationDate}"
        holder.quantityTextView.text = "Qty: ${box.quantity}"
        holder.batchNumberTextView.text = "Batch: ${box.batchNumber}"
    }

    override fun getItemCount(): Int = boxes.size // return the size of the list
}