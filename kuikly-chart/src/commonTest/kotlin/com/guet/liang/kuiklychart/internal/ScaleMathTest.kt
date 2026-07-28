package com.guet.liang.kuiklychart.internal

import com.guet.liang.kuiklychart.api.ChartSpec
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ScaleMathTest {

    @Test
    fun positiveLineValuesProduceAPositiveNiceScale() {
        val spec = ChartSpec().apply {
            line("Revenue", 12f, 18f, 25f)
        }

        val scale = ScaleMath.calculate(spec, ViewportMath.full(3))

        assertEquals(10f, scale.minimum, 0.0001f)
        assertEquals(25f, scale.maximum, 0.0001f)
        assertEquals(listOf(10f, 15f, 20f, 25f), scale.ticks)
    }

    @Test
    fun negativeLineValuesKeepTheScaleBelowZero() {
        val spec = ChartSpec().apply {
            line("Change", -25f, -12f, -18f)
        }

        val scale = ScaleMath.calculate(spec, ViewportMath.full(3))

        assertEquals(-25f, scale.minimum, 0.0001f)
        assertEquals(-10f, scale.maximum, 0.0001f)
        assertEquals(listOf(-25f, -20f, -15f, -10f), scale.ticks)
    }

    @Test
    fun positiveBarsIncludeZero() {
        val spec = ChartSpec().apply {
            bars("Orders", 4f, 8f, 12f)
        }

        val scale = ScaleMath.calculate(spec, ViewportMath.full(3))

        assertEquals(0f, scale.minimum, 0.0001f)
        assertTrue(scale.maximum >= 12f)
        assertTrue(0f in scale.ticks)
    }

    @Test
    fun explicitAxisRangeOverridesCalculatedBounds() {
        val spec = ChartSpec().apply {
            axes {
                y {
                    minimum = -10f
                    maximum = 40f
                    tickCount = 5
                }
            }
            line("Temperature", 0f, 15f, 30f)
        }

        val scale = ScaleMath.calculate(spec, ViewportMath.full(3))

        assertEquals(-10f, scale.minimum, 0.0001f)
        assertEquals(40f, scale.maximum, 0.0001f)
        assertEquals(-10f, scale.ticks.first(), 0.0001f)
        assertEquals(40f, scale.ticks.last(), 0.0001f)
    }

    @Test
    fun emptyDataUsesFiniteFallbackScale() {
        val scale = ScaleMath.calculate(ChartSpec(), ViewportMath.full(0))

        assertEquals(0f, scale.minimum, 0.0001f)
        assertEquals(1f, scale.maximum, 0.0001f)
        assertTrue(scale.ticks.isNotEmpty())
        assertTrue(scale.ticks.all(Float::isFinite))
    }
}
