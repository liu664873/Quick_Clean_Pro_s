package com.quickcleanpro.phonecleaner.common.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun RoundedProgressBar(
    progress: Float,
    width: Dp = 320.dp,
    height: Dp = 10.dp,
    trackColor: Color = Color(0x59FFFFFF),
    fillColor: Color = Color.White,
    modifier: Modifier = Modifier,
) {
    Canvas(
        modifier =
            modifier
                .size(width = width, height = height),
    ) {
        // 閸﹀棜顫楅崡濠傜窞 = 妤傛ê瀹抽惃鍕閿?閿?娑撱倗顏崨鍫㈠箛閸楀﹤娓捐ぐ顫礄閼宠泛娉ぐ顫礆
        val cornerRadiusPx = size.height / 2
        val cornerRadius = CornerRadius(cornerRadiusPx)

        // 1. 缂佹ê鍩楅懗灞炬珯鏉炪劑浜鹃敍鍫濆弿鐎规枻绱?
        drawRoundRect(
            color = trackColor,
            cornerRadius = cornerRadius,
        )

        // 2. 缂佹ê鍩楅崜宥嗘珯鏉╂稑瀹抽敍鍫濐啍閿?= 閹顔?鑴?鏉╂稑瀹抽敓?
        if (progress > 0f) {
            val fillWidth = size.width * progress.coerceIn(0f, 1f)
            drawRoundRect(
                color = fillColor,
                topLeft = Offset.Zero,
                size = Size(fillWidth, size.height),
                cornerRadius = cornerRadius,
            )
        }
    }
}
