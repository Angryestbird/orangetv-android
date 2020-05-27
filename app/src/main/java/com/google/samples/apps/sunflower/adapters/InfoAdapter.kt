/*
 * Copyright 2018 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.samples.apps.sunflower.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.findNavController
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.google.samples.apps.sunflower.homescreen.MovieListFragment
import com.google.samples.apps.sunflower.data.Movie
import com.google.samples.apps.sunflower.databinding.ListItemPlantBinding
import com.google.samples.apps.sunflower.newsscreen.InfoOverviewFragmentDirections

/**
 * Adapter for the [RecyclerView] in [MovieListFragment].
 */
class InfoAdapter : PagedListAdapter<Movie, RecyclerView.ViewHolder>(InfoDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return PlantViewHolder(ListItemPlantBinding.inflate(
                LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val plant = getItem(position)
        (holder as PlantViewHolder).bind(plant)
    }

    class PlantViewHolder(
            private val binding: ListItemPlantBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.setClickListener {
                binding.plant?.let { plant ->
                    navigateToPlant(plant, it)
                }
            }
        }

        private fun navigateToPlant(
                plant: Movie,
                it: View
        ) {
            val direction =
                    InfoOverviewFragmentDirections.actionNewsOverviewToNewsDetail(plant.id)
            it.findNavController().navigate(direction)
        }

        fun bind(item: Movie?) {
            binding.apply {
                plant = item
                executePendingBindings()
            }
        }
    }
}

private class InfoDiffCallback : DiffUtil.ItemCallback<Movie>() {

    override fun areItemsTheSame(oldItem: Movie, newItem: Movie): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Movie, newItem: Movie): Boolean {
        return oldItem == newItem
    }
}