package com.example.finalyearproject

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class PatientAdapter(
    private var patientList: List<Patient>,
    private val onItemClick: (Patient) -> Unit // callback for item click
) : RecyclerView.Adapter<PatientAdapter.PatientViewHolder>() {

    class PatientViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // view holder class for each patient item
        val nameTextView: TextView = itemView.findViewById(R.id.textName)
        val dobTextView: TextView = itemView.findViewById(R.id.textDOB)
        val nhsIdTextView: TextView = itemView.findViewById(R.id.textNHSID)
        val infoTextView: TextView = itemView.findViewById(R.id.textInfo)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PatientViewHolder {
        // create a new view holder for each patient item
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_patient, parent, false) // inflate the layout for the item
        return PatientViewHolder(view)
    }

    override fun onBindViewHolder(holder: PatientViewHolder, position: Int) {
        // bind the data to the view holder
        val patient = patientList[position]
        holder.nameTextView.text = "Name: ${patient.firstName} ${patient.lastName}"
        holder.nhsIdTextView.text = "NHS ID: ${patient.id}"
        holder.dobTextView.text = "DOB: ${patient.dateOfBirth}"
        holder.infoTextView.text = "Conditions: ${patient.medicalConditions.joinToString(", ")}"

        holder.itemView.setOnClickListener {
            // set an onClickListener for the item view
            Log.d(
                "DEBUG_SELECTION",
                "Selected patient: ${patient.firstName} ${patient.lastName} (ID: ${patient.id})"
            )
            onItemClick(patient)
        }

    }

    override fun getItemCount(): Int {
        // return the size of the list
        return patientList.size
    }

    fun updateList(newList: List<Patient>) {
        // update the list of patients
        patientList = newList
        notifyDataSetChanged() // notify the adapter that the data has changed
    }
}
