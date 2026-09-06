package com.arbon.vata.floralens.di

import com.arbon.vata.floralens.BuildConfig
import com.arbon.vata.floralens.data.objectbox.MyObjectBox
import com.arbon.vata.floralens.data.objectbox.PlantRecord
import com.arbon.vata.floralens.data.photos.ScanPhotoStore
import com.arbon.vata.floralens.data.plantnet.PlantNetClient
import com.arbon.vata.floralens.data.repository.OnboardingRepository
import com.arbon.vata.floralens.data.repository.PlantRecordRepository
import com.arbon.vata.floralens.domain.classifier.CascadeClassifier
import com.arbon.vata.floralens.domain.classifier.PlantClassifier
import com.arbon.vata.floralens.domain.classifier.TFLitePlantClassifier
import com.arbon.vata.floralens.domain.species.SpeciesIdentifier
import com.arbon.vata.floralens.presentation.viewmodel.PlantViewModel
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import io.objectbox.Box
import io.objectbox.BoxStore
import kotlinx.serialization.json.Json
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule =
    module {
        // --- ObjectBox ---
        single<BoxStore> {
            MyObjectBox.builder()
                .androidContext(androidContext())
                .build()
        }

        single<Box<PlantRecord>> {
            get<BoxStore>().boxFor(PlantRecord::class.java)
        }

        // --- Persistence ---
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

        // --- Classification cascade ---
        // Real LiteRT model from assets; FakePlantClassifier stays for previews/tests.
        single<PlantClassifier> {
            TFLitePlantClassifier(androidContext())
        }

        single {
            CascadeClassifier(get())
        }

        // --- Pl@ntNet second opinion ---
        single {
            HttpClient(CIO) {
                install(ContentNegotiation) {
                    json(
                        Json {
                            ignoreUnknownKeys = true
                        },
                    )
                }
                install(HttpTimeout) {
                    requestTimeoutMillis = 10_000
                }
            }
        }

        // Blank key (no local.properties entry) makes identify() a graceful no-op.
        single<SpeciesIdentifier> {
            PlantNetClient(
                client = get(),
                apiKey = BuildConfig.PLANTNET_API_KEY,
            )
        }

        // --- Presentation ---
        viewModel {
            PlantViewModel(
                plantRecordRepository = get(),
                onboardingRepository = get(),
                cascadeClassifier = get(),
                speciesIdentifier = get(),
            )
        }
    }