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

package com.google.samples.apps.sunflower.homescreen

import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.app.ShareCompat
import androidx.core.widget.NestedScrollView
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.exoplayer2.ExoPlayerFactory
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.google.samples.apps.sunflower.R
import com.google.samples.apps.sunflower.data.Movie
import com.google.samples.apps.sunflower.databinding.FragmentPlantDetailBinding
import com.google.samples.apps.sunflower.homescreen.viewmodels.MovieDetailViewModel
import com.google.samples.apps.sunflower.utilities.getHostUrl
import com.google.samples.apps.sunflower.utilities.restoreSystemUI
import dagger.android.support.DaggerFragment
import java.net.URLEncoder
import javax.inject.Inject

/**
 * A fragment representing a single Plant detail screen.
 */
class MovieDetailFragment : DaggerFragment() {
    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private var player: SimpleExoPlayer? = null
    private lateinit var playerView: PlayerView
    private val args: MovieDetailFragmentArgs by navArgs()

    private val movieDetailViewModel: MovieDetailViewModel by viewModels { viewModelFactory }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        val binding = DataBindingUtil.inflate<FragmentPlantDetailBinding>(
                inflater, R.layout.fragment_plant_detail, container, false
        ).apply {
            viewModel = movieDetailViewModel.apply { plantId.value = args.plantId }
            lifecycleOwner = viewLifecycleOwner
            callback = object : Callback {
                override fun add(plant: Movie?) {
                    plant?.let {
                        fab?.let { it1 -> hideAppBarFab(it1) }
                        movieDetailViewModel.addPlantToGarden()
                        Snackbar.make(root, R.string.added_plant_to_garden, Snackbar.LENGTH_LONG)
                                .show()
                    }
                }
            }

            var isToolbarShown = false

            // scroll change listener begins at Y = 0 when image is fully collapsed
            plantDetailScrollview?.setOnScrollChangeListener(
                    NestedScrollView.OnScrollChangeListener { _, _, scrollY, _, _ ->

                        // User scrolled past image to height of toolbar and the title text is
                        // underneath the toolbar, so the toolbar should be shown.
                        val shouldShowToolbar = scrollY > toolbar.height

                        // The new state of the toolbar differs from the previous state; update
                        // appbar and toolbar attributes.
                        if (isToolbarShown != shouldShowToolbar) {
                            isToolbarShown = shouldShowToolbar

                            // Use shadow animator to add elevation if toolbar is shown
                            appbar.isActivated = shouldShowToolbar

                            // Show the plant name if toolbar is shown
                            toolbarLayout.isTitleEnabled = shouldShowToolbar
                        }
                    }
            )

            toolbar.setNavigationOnClickListener { view ->
                view.findNavController().navigateUp()
            }

            toolbar.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.action_share -> {
                        createShareIntent()
                        true
                    }
                    else -> false
                }
            }
        }
        setHasOptionsMenu(true)

        // hide unnecessary view
        (requireActivity() as AppCompatActivity).apply {
            if (resources.configuration.orientation.equals(Configuration.ORIENTATION_LANDSCAPE)) {
                requireActivity().findViewById<BottomNavigationView>(R.id.bottom_nav).visibility = View.GONE

                // Hide the status bar.
                window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN
                // Remember that you should never show the action bar if the
                // status bar is hidden, so hide that too if necessary.
                binding.toolbar.visibility = View.INVISIBLE
            }
            onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
                restoreSystemUI()
                findNavController().popBackStack()
            }
        }

        movieDetailViewModel.hostUrl = PreferenceManager.getDefaultSharedPreferences(context).getString("host", "http://10.0.2.2:8080/")

        playerView = binding.detailImage
        return binding.root
    }

    override fun onStart() {
        super.onStart()
        initializePlayer()
        playerView.onResume()
    }

    override fun onStop() {
        super.onStop()
        playerView.onPause()
        player!!.release()
        player = null
    }

    // Internal methods
    private fun initializePlayer() {
        movieDetailViewModel.plant.observe(this, Observer {
            player = ExoPlayerFactory.newSimpleInstance(context)?.apply {
                // Produces DataSource instances through which media data is loaded.
                var dataSourceFactory = DefaultDataSourceFactory(context, Util.getUserAgent(context, "yourApplicationName"));
                // This is the MediaSource representing the media to be played.
                var videoSource = ProgressiveMediaSource.Factory(dataSourceFactory)
                        .createMediaSource(Uri.parse("${getHostUrl(context!!)}${URLEncoder.encode(it.videoUrl, "utf-8")}"));
                // Prepare the player with the source.
                prepare(videoSource);
            }?.also { playerView.player = it }
        })
    }

    // Helper function for calling a share functionality.
    // Should be used when user presses a share button/menu item.
    @Suppress("DEPRECATION")
    private fun createShareIntent() {
        val shareText = movieDetailViewModel.plant.value.let { plant ->
            if (plant == null) {
                ""
            } else {
                getString(R.string.share_text_plant, plant.name)
            }
        }
        val shareIntent = ShareCompat.IntentBuilder.from(activity)
                .setText(shareText)
                .setType("text/plain")
                .createChooserIntent()
                .addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
        startActivity(shareIntent)
    }

    // FloatingActionButtons anchored to AppBarLayouts have their visibility controlled by the scroll position.
    // We want to turn this behavior off to hide the FAB when it is clicked.
    //
    // This is adapted from Chris Banes' Stack Overflow answer: https://stackoverflow.com/a/41442923
    private fun hideAppBarFab(fab: FloatingActionButton) {
        val params = fab.layoutParams as CoordinatorLayout.LayoutParams
        val behavior = params.behavior as FloatingActionButton.Behavior
        behavior.isAutoHideEnabled = false
        fab.hide()
    }

    interface Callback {
        fun add(plant: Movie?)
    }
}
