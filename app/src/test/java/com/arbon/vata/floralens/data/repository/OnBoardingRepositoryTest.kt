package com.arbon.vata.floralens.data.repository

import android.content.Context
import android.content.SharedPreferences
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class OnboardingRepositoryTest {

    private val prefs = mockk<SharedPreferences>()
    private val editor = mockk<SharedPreferences.Editor>(relaxed = true)
    private val context = mockk<Context>()
    private lateinit var repository: OnboardingRepository

    @BeforeEach
    fun setUp() {
        every { context.getSharedPreferences(any(), any()) } returns prefs
        every { prefs.edit() } returns editor
        // Chain on the same mock so apply() is verifiable on `editor`.
        every { editor.putBoolean(any(), any()) } returns editor
        repository = OnboardingRepository(context)
    }

    @Test
    fun `first launch reports onboarding as not done`() {
        every { prefs.getBoolean("onboarding_done", false) } returns false

        assertFalse(repository.isDone())
    }

    @Test
    fun `setDone persists the flag`() {
        repository.setDone()

        verify { editor.putBoolean("onboarding_done", true) }
        verify { editor.apply() }
    }
}