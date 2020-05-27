package com.google.samples.apps.sunflower.newsscreen

import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.observe
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.samples.apps.sunflower.R
import com.google.samples.apps.sunflower.adapters.InfoAdapter
import com.google.samples.apps.sunflower.data.NetworkState
import com.google.samples.apps.sunflower.databinding.FragmentInfoOverviewBinding
import com.google.samples.apps.sunflower.newsscreen.viewmodels.InfoOverviewViewModel
import dagger.android.support.DaggerFragment
import javax.inject.Inject

/**
 * A simple [Fragment] subclass.
 */
class InfoOverviewFragment : DaggerFragment(), SearchView.OnQueryTextListener {

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private val viewModel: InfoOverviewViewModel by viewModels { viewModelFactory }

    private lateinit var swipeRefresh: SwipeRefreshLayout

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val binding = FragmentInfoOverviewBinding.inflate(inflater, container, false)
        context ?: return binding.root

        (activity as AppCompatActivity).setSupportActionBar(binding.toolbar)

        val adapter = InfoAdapter()
        binding.plantList.adapter = adapter
        subscribeUi(adapter)

        swipeRefresh = binding.swipeRefresh
        initSwipeToRefresh()

        setHasOptionsMenu(true)
        return binding.root
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_plant_list, menu)
        // Get the MenuItem for the action item
        (menu?.findItem(R.id.search).actionView as SearchView).apply {
            setIconifiedByDefault(false)
            setOnQueryTextListener(this@InfoOverviewFragment)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.filter_zone -> {
                updateData()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun subscribeUi(adapter: InfoAdapter) {
        viewModel.plants.observe(viewLifecycleOwner) { plants ->
            adapter.submitList(plants)
        }
    }

    private fun initSwipeToRefresh() {
        viewModel.refreshState.observe(this, Observer {
            swipeRefresh.isRefreshing = it == NetworkState.LOADING
        })
        swipeRefresh.setOnRefreshListener {
            viewModel.refresh()
        }
    }

    private fun updateData() {
        with(viewModel) {
            if (isFiltered()) {
                clearGrowZoneNumber()
            } else {
                setGrowZoneNumber(9)
            }
        }
    }

    override fun onQueryTextSubmit(query: String?): Boolean {
        query?.also {
            viewModel.setSearchString(it)
            return true
        }
        return false
    }

    override fun onQueryTextChange(newText: String?): Boolean {
        newText?.also {
            if (it.isBlank()) {
                viewModel.setSearchString("")
            }
        }
        return false
    }
}
