package com.jimz011apps.hki7.ui.screens

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ContainerStackHeaderTest {
    @Test
    fun `omitted name and icon remove the complete header in normal mode`() {
        assertFalse(
            shouldRenderContainerHeader(
                title = null,
                icon = null,
                isEditMode = false,
                isCollapsed = false,
                isHidden = false
            )
        )
    }

    @Test
    fun `name or icon renders the identity header`() {
        assertTrue(shouldRenderContainerHeader("Scenes", null, isEditMode = false, isCollapsed = false, isHidden = false))
        assertTrue(shouldRenderContainerHeader(null, "palette", isEditMode = false, isCollapsed = false, isHidden = false))
    }

    @Test
    fun `editing and collapsed states retain a recovery header`() {
        assertTrue(shouldRenderContainerHeader(null, null, isEditMode = true, isCollapsed = false, isHidden = false))
        assertTrue(shouldRenderContainerHeader(null, null, isEditMode = false, isCollapsed = true, isHidden = false))
    }
}
