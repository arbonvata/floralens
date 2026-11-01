package com.arbon.vata.floralens.testdata

import androidx.test.platform.app.InstrumentationRegistry
import com.arbon.vata.floralens.data.repository.PlantRecordRepository
import com.arbon.vata.floralens.debug.PlantDocSamples
import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.koin.core.context.GlobalContext

/** Optional command-line alternative to Debug settings. */
class PlantDocSeedTest {
    @Test
    fun seedSamples() = runBlocking {
        assumeTrue(InstrumentationRegistry.getArguments().getString("seedPlantDoc") == "true")
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        PlantDocSamples.import(context, GlobalContext.get().get<PlantRecordRepository>())
        Unit
    }
}
