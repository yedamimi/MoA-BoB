package com.eatda.app.util

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator

object HapticManager {
    // 알레르기 경고 — 강하게 3번 반복
    fun allergenAlert(context: Context) = vibrate(
        context,
        longArrayOf(0, 200, 100, 200, 100, 200),
        intArrayOf(0, 255, 0, 255, 0, 255),
    )

    // 유통기한 D-1 — 짧게 2번
    fun expiryWarning(context: Context) = vibrate(
        context,
        longArrayOf(0, 100, 80, 100),
        intArrayOf(0, 180, 0, 180),
    )

    // 신선도 경고 — 길게 1번
    fun freshnessAlert(context: Context) = vibrate(
        context,
        longArrayOf(0, 400),
        intArrayOf(0, 200),
    )

    private fun vibrate(context: Context, timings: LongArray, amplitudes: IntArray) {
        val vibrator = context.getSystemService(Vibrator::class.java) ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(timings, -1)
        }
    }
}
