package com.guet.liang.kuiklychart.internal

import com.guet.liang.kuiklychart.api.ChartViewport
import kotlin.test.Test
import kotlin.test.assertEquals

class ViewportMathTest {

    @Test
    fun fullCoversEveryDataIndex() {
        assertEquals(ChartViewport(0f, 4f), ViewportMath.full(5))
        assertEquals(ChartViewport(0f, 0f), ViewportMath.full(0))
    }

    @Test
    fun initialShowsTheRequestedTrailingWindow() {
        assertEquals(ChartViewport(6f, 9f), ViewportMath.initial(10, 4))
        assertEquals(ViewportMath.full(10), ViewportMath.initial(10, 0))
        assertEquals(ViewportMath.full(10), ViewportMath.initial(10, 10))
    }

    @Test
    fun normalizePreservesSpanWhileClampingToDataBounds() {
        assertEquals(
            ChartViewport(0f, 3f),
            ViewportMath.normalize(ChartViewport(-2f, 1f), 10),
        )
        assertEquals(
            ChartViewport(5f, 9f),
            ViewportMath.normalize(ChartViewport(7f, 11f), 10),
        )
        assertEquals(
            ChartViewport(4f, 4f),
            ViewportMath.normalize(ChartViewport(4f, 1f), 10),
        )
    }

    @Test
    fun panConvertsPixelMovementToIndexMovementAndClamps() {
        assertEquals(
            ChartViewport(3f, 6f),
            ViewportMath.pan(
                viewport = ChartViewport(2f, 5f),
                horizontalDelta = -25f,
                plotWidth = 100f,
                dataCount = 10,
            ),
        )
        assertEquals(
            ChartViewport(0f, 3f),
            ViewportMath.pan(
                viewport = ChartViewport(0f, 3f),
                horizontalDelta = 50f,
                plotWidth = 100f,
                dataCount = 10,
            ),
        )
    }

    @Test
    fun zoomKeepsTheFocalPointAndHonorsMinimumVisiblePoints() {
        assertViewport(
            expectedStart = 2.25f,
            expectedEnd = 6.75f,
            actual = ViewportMath.zoom(
                viewport = ChartViewport(0f, 9f),
                scale = 2f,
                focalRatio = 0.5f,
                dataCount = 10,
                minimumVisiblePoints = 3,
            ),
        )
        assertViewport(
            expectedStart = 3.5f,
            expectedEnd = 5.5f,
            actual = ViewportMath.zoom(
                viewport = ChartViewport(0f, 9f),
                scale = 100f,
                focalRatio = 0.5f,
                dataCount = 10,
                minimumVisiblePoints = 3,
            ),
        )
    }

    private fun assertViewport(
        expectedStart: Float,
        expectedEnd: Float,
        actual: ChartViewport,
    ) {
        assertEquals(expectedStart, actual.startIndex, 0.0001f)
        assertEquals(expectedEnd, actual.endIndex, 0.0001f)
    }
}
