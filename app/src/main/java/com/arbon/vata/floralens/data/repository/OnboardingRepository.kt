package com.arbon.vata.floralens.data.repository

import android.content.Context

/** Persists the one-bit "onboarding seen" flag so Onboarding only shows on first launch. */
class OnboardingRepository(
    context: Context,
) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isDone(): Boolean = prefs.getBoolean(KEY_ONBOARDING_DONE, false)

    fun setDone() {
        prefs.edit().putBoolean(KEY_ONBOARDING_DONE, true).apply()
    }

    private companion object {
        const val PREFS_NAME = "flora_lens_prefs"
        const val KEY_ONBOARDING_DONE = "onboarding_done"
    }
}