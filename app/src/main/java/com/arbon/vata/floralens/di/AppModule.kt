package com.arbon.vata.floralens.di

import com.arbon.vata.floralens.data.objectbox.MyObjectBox
import com.arbon.vata.floralens.data.objectbox.PlantRecord
import com.arbon.vata.floralens.data.photos.ScanPhotoStore
import com.arbon.vata.floralens.data.repository.OnboardingRepository
import com.arbon.vata.floralens.data.repository.PlantRecordRepository
import com.arbon.vata.floralens.presentation.viewmodel.PlantViewModel
import io.objectbox.Box
import io.objectbox.BoxStore
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule =
    module {
        single<BoxStore> {
            MyObjectBox.builder()
                .androidContext(androidContext())
                .build()
        }

        single<Box<PlantRecord>> {
            get<BoxStore>().boxFor(PlantRecord::class.java)
        }

        single {
            ScanPhotoStore(androidContext())
        }

        single {
            PlantRecordRepository(
                photoStore = get(),
                plantRecordBox = get(),
            )
        }

        single {
            OnboardingRepository(androidContext())
        }

        viewModel {
            PlantViewModel(
                plantRecordRepository = get(),
                onboardingRepository = get(),
            )
        }
    }