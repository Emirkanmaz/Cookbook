package com.emirkanmaz.cookbook.adapter

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.navigation.Navigation
import androidx.recyclerview.widget.RecyclerView
import com.emirkanmaz.cookbook.databinding.RecyclerRowBinding
import com.emirkanmaz.cookbook.model.Recipe
import com.emirkanmaz.cookbook.view.ListFragmentDirections

class RecipeAdapter(val recipesList:List<Recipe>): RecyclerView.Adapter<RecipeAdapter.RecipeViewHolder>() {

    class RecipeViewHolder(val binding: RecyclerRowBinding): RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecipeViewHolder {
        val binding = RecyclerRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return RecipeViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return recipesList.size
    }

    override fun onBindViewHolder(holder: RecipeViewHolder, position: Int) {
        holder.binding.recyclerTextView.text = recipesList[position].name

        holder.itemView.setOnClickListener {
            val action = ListFragmentDirections.actionListFragmentToRecipeFragment(new = false, id = recipesList[position].id)
            Navigation.findNavController(it).navigate(action)
        }
    }

}