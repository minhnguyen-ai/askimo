/* SPDX-License-Identifier: AGPLv3
 *
 * Copyright (c) 2026 Askimo
 */
package io.askimo.ui.common.theme

import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals

class LayoutDensityTest {

    @Test
    fun `fromPreference returns comfortable when value is null`() {
        assertEquals(LayoutDensity.COMFORTABLE, LayoutDensity.fromPreference(null))
    }

    @Test
    fun `fromPreference returns comfortable when value is unknown`() {
        assertEquals(LayoutDensity.COMFORTABLE, LayoutDensity.fromPreference("UNKNOWN"))
    }

    @Test
    fun `fromPreference resolves compact`() {
        assertEquals(LayoutDensity.COMPACT, LayoutDensity.fromPreference(LayoutDensity.COMPACT.name))
    }

    @Test
    fun `compact scale is less than comfortable`() {
        assert(LayoutDensity.COMPACT.scale < LayoutDensity.COMFORTABLE.scale)
    }

    @Test
    fun `SpacingValues scales tokens correctly`() {
        val scaled = SpacingValues(scale = 0.75f)
        assertEquals(6.dp, scaled.small) // 8.dp * 0.75 = 6.dp
        assert(scaled.medium < 12.dp)
        assert(scaled.large < 16.dp)
    }
}
