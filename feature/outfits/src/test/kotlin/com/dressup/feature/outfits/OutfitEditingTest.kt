package com.dressup.feature.outfits

import com.dressup.core.model.OutfitItem
import org.junit.Assert.assertEquals
import org.junit.Test

class OutfitEditingTest {
    private val top = OutfitItem(garmentId = 1, x = 0.5f, y = 0.3f, scale = 0.4f)
    private val shoes = OutfitItem(garmentId = 2, x = 0.5f, y = 0.9f, scale = 0.3f)
    private val board = listOf(top, shoes)

    @Test
    fun `moving and pinching a garment changes only that garment, and keeps it on the board`() {
        val moved = board.transformed(1, dx = 0.1f, dy = -0.5f, zoom = 2f)
        assertEquals(OutfitItem(1, 0.6f, 0f, 0.8f), moved[0])
        assertEquals(shoes, moved[1])

        val huge = board.transformed(2, dx = 5f, dy = 0f, zoom = 100f)[1]
        assertEquals(1f, huge.x)
        assertEquals(OutfitItem.MAX_SCALE, huge.scale)
        assertEquals(OutfitItem.MIN_SCALE, board.resized(2, 0f)[1].scale)
    }

    @Test
    fun `bringing a garment to the front draws it last`() {
        assertEquals(listOf(shoes, top), board.broughtToFront(1))
        assertEquals(board, board.broughtToFront(99))
    }

    @Test
    fun `picking garments keeps the placed ones where they are, drops unpicked and adds new ones`() {
        val picked = board.withGarments(listOf(2L, 3L))
        assertEquals(listOf(2L, 3L), picked.map { it.garmentId })
        assertEquals(shoes, picked[0])
        assertEquals(OutfitItem.DEFAULT_SCALE, picked[1].scale)
    }

    @Test
    fun `turning adds up and stays within half a turn either way`() {
        val board = listOf(OutfitItem(1, 0.5f, 0.5f, 0.4f, rotation = 170f), OutfitItem(2, 0.5f, 0.5f, 0.4f))
        assertEquals(-170f, board.transformed(1, 0f, 0f, 1f, rotation = 20f)[0].rotation, 0.001f)
        assertEquals(0f, board.transformed(1, 0f, 0f, 1f, rotation = 20f)[1].rotation, 0.001f)
        assertEquals(90f, board.rotated(2, 450f)[1].rotation, 0.001f)
    }
}
