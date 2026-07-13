package com.quickcleanpro.phonecleaner.feature.files.shared.ui

import androidx.compose.ui.graphics.Color
import com.quickcleanpro.phonecleaner.feature.files.shared.FileImageDisplayItem

internal val FileImageDisplayItem.colors: List<Color>
    get() {
        val palettes = listOf(
            listOf(Color(0xFF36543B), Color(0xFFD5C7B9)),
            listOf(Color(0xFF1D2330), Color(0xFFC47D63)),
            listOf(Color(0xFF5F794A), Color(0xFFFFC4D6)),
            listOf(Color(0xFFA9745D), Color(0xFFF1D6CD)),
            listOf(Color(0xFFE3DFCB), Color(0xFF9DB58D)),
            listOf(Color(0xFF476941), Color(0xFFFFD6E8)),
        )
        return palettes[Math.floorMod(paletteIndex, palettes.size)]
    }
