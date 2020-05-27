/*
 * Copyright 2019, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.samples.apps.sunflower.userscreen

import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.setupActionBarWithNavController
import com.google.samples.apps.sunflower.R
import com.google.samples.apps.sunflower.databinding.FragmentUserProfileBinding


/**
 * Shows a profile screen for a user, taking the name from the arguments.
 */
class UserProfile : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        val binding = FragmentUserProfileBinding.inflate(inflater, container, false)
        (requireActivity() as AppCompatActivity).apply {
            setupActionBarWithNavController(findNavController())
            (parentFragment?.parentFragment as User).toolbar.setNavigationOnClickListener {
                findNavController().navigateUp()
            }
        }

        val name = arguments?.getString("user") ?: "not logged in"
        binding.profileUserName.text = name
        binding.loginButton.setOnClickListener(Navigation.createNavigateOnClickListener(R.id.login))
        setHasOptionsMenu(true)
        return binding.root
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_profile, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_settings -> {
                findNavController().navigate(R.id.setting)
            }
        }
        return super.onOptionsItemSelected(item)
    }
}
