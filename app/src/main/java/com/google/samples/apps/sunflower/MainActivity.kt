package com.google.samples.apps.sunflower

import android.content.Context
import android.net.ConnectivityManager
import android.net.ConnectivityManager.NetworkCallback
import android.net.Network
import android.net.NetworkCapabilities.TRANSPORT_WIFI
import android.net.NetworkRequest
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.observe
import androidx.navigation.NavController
import androidx.navigation.ui.setupActionBarWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.snackbar.Snackbar
import com.google.samples.apps.sunflower.databinding.ActivityMainBinding
import com.google.samples.apps.sunflower.utilities.setupWithNavController
import com.google.samples.apps.sunflower.viewmodels.ConnectionState
import com.google.samples.apps.sunflower.viewmodels.MainActivityViewModel
import dagger.android.support.DaggerAppCompatActivity
import javax.inject.Inject

/**
 * An activity that inflates a layout that has a [BottomNavigationView].
 */
class MainActivity : DaggerAppCompatActivity() {

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private val viewModel: MainActivityViewModel by viewModels { viewModelFactory }

    private var currentNavController: LiveData<NavController>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        observeNetworkState(binding.root)
        registerNetworkCallback()

        if (savedInstanceState == null) {
            setupBottomNavigationBar()
        } // Else, need to wait for onRestoreInstanceState
    }

    private fun observeNetworkState(root: View) {
        viewModel.connectionState.observe(this) {
            when (it) {
                ConnectionState.LOST -> {
                    Snackbar.make(root, R.string.network_unavailable, Snackbar.LENGTH_LONG)
                            .setBackgroundTint(resources.getColor(android.R.color.holo_red_light))
                            .show()
                }
            }
        }
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle?) {
        super.onRestoreInstanceState(savedInstanceState)
        // Now that BottomNavigationBar has restored its instance state
        // and its selectedItemId, we can proceed with setting up the
        // BottomNavigationBar with Navigation
        setupBottomNavigationBar()
    }

    private fun registerNetworkCallback() {
        val connMgr = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        connMgr.registerNetworkCallback(
                NetworkRequest.Builder().addTransportType(TRANSPORT_WIFI).build(),
                object : NetworkCallback() {
                    override fun onAvailable(network: Network?) {
                        viewModel.setConnectionState(ConnectionState.AVAILABLE)
                    }

                    override fun onLost(network: Network?) {
                        viewModel.setConnectionState(ConnectionState.LOST)
                    }
                }
        )
    }

    /**
     * Called on first creation and when restoring state.
     */
    private fun setupBottomNavigationBar() {
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_nav)

        val navGraphIds = listOf(R.navigation.nav_garden, R.navigation.list, R.navigation.user)

        // Setup the bottom navigation view with a list of navigation graphs
        val controller = bottomNavigationView.setupWithNavController(
                navGraphIds = navGraphIds,
                fragmentManager = supportFragmentManager,
                containerId = R.id.nav_host_container,
                intent = intent
        )

        // Whenever the selected controller changes, setup the action bar.
        controller.observe(this, Observer { navController ->
            setupActionBarWithNavController(navController)
        })
        currentNavController = controller
    }

    override fun onSupportNavigateUp(): Boolean {
        return currentNavController?.value?.navigateUp() ?: false
    }

}
