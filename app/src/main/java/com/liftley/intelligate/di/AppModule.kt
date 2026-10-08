package com.liftley.intelligate.di

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.liftley.intelligate.data.FirebaseVerificationRepository
import com.liftley.intelligate.data.PlateImageProcessor
import com.liftley.intelligate.domain.VerificationRepository
import com.liftley.intelligate.ui.scan.ScanViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single { PlateImageProcessor(androidContext()) }
    single<VerificationRepository> {
        FirebaseVerificationRepository({ FirebaseAuth.getInstance() }, { FirebaseDatabase.getInstance() })
    }
    viewModel { ScanViewModel(get(), get()) }
}

