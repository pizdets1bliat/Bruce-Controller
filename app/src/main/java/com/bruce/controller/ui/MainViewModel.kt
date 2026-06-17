package com.bruce.controller.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bruce.controller.ble.BruceBleManager
import com.bruce.controller.cli.BruceCliHandler
import kotlinx.coroutines.launch

class MainViewModel(
    val bleManager: BruceBleManager,
    val cliHandler: BruceCliHandler
) : ViewModel() {

    init {
        cliHandler.startCollecting()
    }

    override fun onCleared() {
        super.onCleared()
        cliHandler.stopCollecting()
        bleManager.disconnect()
    }
}
