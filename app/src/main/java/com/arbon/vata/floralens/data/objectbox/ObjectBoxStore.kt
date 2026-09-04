package com.arbon.vata.floralens.data.objectbox

import android.content.Context
import io.objectbox.Box
import io.objectbox.BoxStore

object ObjectBoxStore {
    lateinit var store: BoxStore
        private set

    fun init(context: Context) {
        if (!::store.isInitialized) {
            store = MyObjectBox.builder()
                .androidContext(context.applicationContext)
                .build()
        }
    }

    val plantRecordBox: Box<PlantRecord>
        get() = store.boxFor(PlantRecord::class.java)
}
