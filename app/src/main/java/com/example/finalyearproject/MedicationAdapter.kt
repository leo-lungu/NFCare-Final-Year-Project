package com.example.finalyearproject

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MedicationAdapter(
    // adapter class for displaying a list of medications
    medications: List<Medication>,
    private val boxesMap: Map<String, List<MedicationSpecific>>,
    private val onItemClick: ((Medication) -> Unit)? = null
) : RecyclerView.Adapter<MedicationAdapter.MedicationViewHolder>() {

    private val medicationList = medications.toMutableList()

    class MedicationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // view holder class for each medication item
        val nameTextView: TextView = itemView.findViewById(R.id.medicationNameTextView)
        val dosageTextView: TextView = itemView.findViewById(R.id.medicationDosageTextView)
        val descriptionTextView: TextView =
            itemView.findViewById(R.id.medicationDescriptionTextView)
        val allergensTextView: TextView = itemView.findViewById(R.id.medicationAllergensTextView)
        val boxesRecyclerView: RecyclerView = itemView.findViewById(R.id.boxesRecyclerView)
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MedicationViewHolder {
        // create a new view holder for each medication item
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_medication, parent, false) // inflate the layout for the item
        return MedicationViewHolder(view)
    }

    override fun onBindViewHolder(holder: MedicationViewHolder, position: Int) {
        // bind the data to the view holder
        val medication = medicationList[position] // get the medication at the current position
        holder.nameTextView.text = medication.name
        holder.dosageTextView.text = "Dosage: ${medication.dosage}"

        holder.nameTextView.text = medication.name
        holder.descriptionTextView.text = medication.description
        holder.dosageTextView.text = "Dosage: ${medication.dosage}"

        val allergens = medication.allergens
        val allergensDisplay = if (allergens.isEmpty()) "None" else allergens.joinToString(", ")

        holder.allergensTextView.text = "Allergens: $allergensDisplay" // set the allergens text


        val boxes = boxesMap[medication.id] ?: emptyList() // get the boxes for the medication
        val boxAdapter = MedicationSpecificAdapter(boxes)
        holder.boxesRecyclerView.layoutManager = LinearLayoutManager(holder.itemView.context)
        holder.boxesRecyclerView.adapter = boxAdapter

        holder.itemView.setOnClickListener {
            // set an onClickListener for the item view
            onItemClick?.invoke(medication)
        }
    }


    override fun getItemCount(): Int = medicationList.size // return the size of the medication list

    fun updateList(newList: List<Medication>) {
        // update the medication list with a new list
        medicationList.clear()
        medicationList.addAll(newList)
        notifyDataSetChanged()
    }
}
