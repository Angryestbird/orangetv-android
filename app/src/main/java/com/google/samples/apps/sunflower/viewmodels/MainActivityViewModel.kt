package com.google.samples.apps.sunflower.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

class MainActivityViewModel @Inject constructor() : ViewModel() {

    private val _connectionState: MutableLiveData<ConnectionState> = MutableLiveData()
    val connectionState: LiveData<ConnectionState> = _connectionState
    fun setConnectionState(state: ConnectionState) {
        viewModelScope.launch {
            withContext(Dispatchers.Main) {
                _connectionState.value = state
            }
        }
    }
}

enum class ConnectionState {
    AVAILABLE,
    LOST
}
