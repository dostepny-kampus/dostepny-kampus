/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.app.debug

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.metrics.performance.FrameData
import androidx.metrics.performance.JankStats
import androidx.metrics.performance.PerformanceMetricsState
import timber.log.Timber

/** Włącza JankStats na oknie aktywności, gdy jest widoczna, i wyłącza, gdy przestaje być. */
internal class JankTracking : Application.ActivityLifecycleCallbacks {
    @Volatile
    private var holder: PerformanceMetricsState.Holder? = null

    private val tracked = mutableMapOf<Activity, JankStats>()

    override fun onActivityResumed(activity: Activity) {
        holder = PerformanceMetricsState.getHolderForHierarchy(activity.window.decorView)
        tracked
            .getOrPut(activity) { JankStats.createAndTrack(activity.window, ::report) }
            .isTrackingEnabled = true
    }

    override fun onActivityPaused(activity: Activity) {
        tracked[activity]?.isTrackingEnabled = false
    }

    override fun onActivityDestroyed(activity: Activity) {
        tracked.remove(activity)
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit

    override fun onActivityStarted(activity: Activity) = Unit

    override fun onActivityStopped(activity: Activity) = Unit

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit

    /** Dopisuje etykietę [key] = [value] do stanu bieżącego okna; przed pierwszą aktywnością nic nie robi. */
    fun label(key: String, value: String) {
        holder?.state?.putState(key, value)
    }

    private fun report(frame: FrameData) {
        if (frame.isJank) {
            val states = frame.states.joinToString { state -> "${state.key}=${state.value}" }
            Timber.w("Wolna klatka: %d ms, stan: %s", frame.frameDurationUiNanos / NANOS_PER_MILLI, states)
        }
    }

    private companion object {
        const val NANOS_PER_MILLI = 1_000_000L
    }
}
