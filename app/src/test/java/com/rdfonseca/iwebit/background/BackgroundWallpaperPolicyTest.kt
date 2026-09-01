package com.rdfonseca.iwebit.background

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BackgroundWallpaperPolicyTest {
    @Test
    fun `aceita URL HTTPS absoluto`() {
        val result = BackgroundWallpaperPolicy.resolveImageUrl(
            "https://agent.iwebit.app/backgrounds/company.jpg",
            "https://agent.iwebit.app/"
        )

        assertEquals("https://agent.iwebit.app/backgrounds/company.jpg", result.toString())
    }

    @Test
    fun `resolve caminho relativo no servidor do agente`() {
        val result = BackgroundWallpaperPolicy.resolveImageUrl(
            "/backgrounds/company.jpg",
            "https://agent.iwebit.app/"
        )

        assertEquals("https://agent.iwebit.app/backgrounds/company.jpg", result.toString())
    }

    @Test
    fun `recusa HTTP e valor vazio`() {
        assertNull(
            BackgroundWallpaperPolicy.resolveImageUrl(
                "http://agent.iwebit.app/background.jpg",
                "https://agent.iwebit.app/"
            )
        )
        assertNull(BackgroundWallpaperPolicy.resolveImageUrl(" ", "https://agent.iwebit.app/"))
    }

    @Test
    fun `aplica apenas pedido novo e completo`() {
        assertTrue(BackgroundWallpaperPolicy.shouldApply(1, "https://host/image.jpg", null, 10, -1))
        assertFalse(BackgroundWallpaperPolicy.shouldApply(0, "https://host/image.jpg", null, 10, -1))
        assertFalse(BackgroundWallpaperPolicy.shouldApply(1, null, null, 10, -1))
        assertFalse(
            BackgroundWallpaperPolicy.shouldApply(
                1,
                "https://host/image.jpg",
                "https://host/image.jpg",
                10,
                10
            )
        )
        assertTrue(
            BackgroundWallpaperPolicy.shouldApply(
                1,
                "https://host/image.jpg",
                "https://host/image.jpg",
                11,
                10
            )
        )
    }

    @Test
    fun `calcula amostragem sem reduzir abaixo do alvo`() {
        assertEquals(4, BackgroundWallpaperPolicy.calculateInSampleSize(4320, 7680, 1080, 1920))
        assertEquals(1, BackgroundWallpaperPolicy.calculateInSampleSize(1080, 1920, 1080, 1920))
        assertEquals(1, BackgroundWallpaperPolicy.calculateInSampleSize(0, 0, 1080, 1920))
    }
}