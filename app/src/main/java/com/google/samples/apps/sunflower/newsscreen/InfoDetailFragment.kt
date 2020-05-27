package com.google.samples.apps.sunflower.newsscreen

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ShareCompat
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.snackbar.Snackbar
import com.google.samples.apps.sunflower.R
import com.google.samples.apps.sunflower.data.Movie
import com.google.samples.apps.sunflower.databinding.FragmentInfoDetailBinding
import com.google.samples.apps.sunflower.newsscreen.viewmodels.InfoDetailViewModel
import dagger.android.support.DaggerFragment
import javax.inject.Inject

/**
 * A simple [Fragment] subclass.
 * Use the [InfoDetailFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class InfoDetailFragment : DaggerFragment() {

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private val infoDetailViewModel: InfoDetailViewModel by viewModels { viewModelFactory }

    private val args: InfoDetailFragmentArgs by navArgs()
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val binding = FragmentInfoDetailBinding.inflate(inflater, container, false)
                .apply {
                    viewModel = infoDetailViewModel.apply { plantId.value = args.newsId }
                    lifecycleOwner = viewLifecycleOwner
                    callback = object : Callback {
                        override fun add(plant: Movie?) {
                            plant?.let {
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
        return binding.root
    }

    // Helper function for calling a share functionality.
    // Should be used when user presses a share button/menu item.
    @Suppress("DEPRECATION")
    private fun createShareIntent() {
        val shareText = infoDetailViewModel.plant.value.let { plant ->
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

    interface Callback {
        fun add(plant: Movie?)
    }
}
