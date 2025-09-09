package com.example.greenery.ui.myplant

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.greenery.R

class PlantAdapter(
    private var plants: List<Plant>,
    private val onPlantClick: (Plant) -> Unit,
    private val onPlantDelete: (Plant) -> Unit
) : RecyclerView.Adapter<PlantAdapter.PlantViewHolder>() {

    class PlantViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val plantImage: ImageView = itemView.findViewById(R.id.plantImage)
        val plantNameTextView: TextView = itemView.findViewById(R.id.plantName)
        val speciesTextView: TextView = itemView.findViewById(R.id.plantSpecies)
        val wateringIntervalTextView: TextView = itemView.findViewById(R.id.waterInterval)
        val editButton: Button = itemView.findViewById(R.id.editButton)
        val deleteButton: Button = itemView.findViewById(R.id.deleteButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlantViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_plant, parent, false)
        return PlantViewHolder(view)
    }

    override fun onBindViewHolder(holder: PlantViewHolder, position: Int) {
        val plant = plants[position]
        holder.plantNameTextView.text = plant.name
        holder.speciesTextView.text = plant.species
        holder.wateringIntervalTextView.text = plant.waterInterval

        // 이미지 로드
        if (plant.image.isNotEmpty()) {
            Glide.with(holder.itemView.context)
                .load(plant.image)
                .placeholder(R.drawable.ic_myplant_default)
                .into(holder.plantImage)
        } else {
            holder.plantImage.setImageResource(R.drawable.ic_myplant_default)
        }

        // 수정 버튼 클릭
        holder.editButton.setOnClickListener {
            onPlantClick(plant)
        }

        // 삭제 버튼 클릭
        holder.deleteButton.setOnClickListener {
            Log.d("PlantAdapter", "삭제 버튼 클릭됨")
            Toast.makeText(holder.itemView.context, "삭제되었습니다", Toast.LENGTH_SHORT).show()
            onPlantDelete(plant) // 삭제 작업 처리
        }
    }

    override fun getItemCount(): Int = plants.size

    // 식물 목록 업데이트
    fun updatePlantList(newPlants: List<Plant>) {
        plants = newPlants
        notifyDataSetChanged()
    }
}
