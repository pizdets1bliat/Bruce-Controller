package com.bruce.controller.di

import com.bruce.controller.ble.BruceBleManager
import com.bruce.controller.cli.BruceCliHandler
import com.bruce.controller.ui.MainViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single { BruceBleManager(androidContext()) }
    single { BruceCliHandler(get()) }
    viewModel { MainViewModel(get(), get()) }
}
