package com.quickcleanpro.phonecleaner.common.ui.components.animations

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate

@Composable
fun SemiCircularGauge(
    modifier: Modifier = Modifier,
    isAnimating: Boolean,
    arcStartColor: Color,
    arcEndColor: Color,
    needleColor: Color,
    tickColor: Color
) {
    val angleState = remember { Animatable(-90f) }

    LaunchedEffect(isAnimating) {
        if (isAnimating) {
            while (true) {
                angleState.animateTo(90f, tween(800))
                angleState.animateTo(-90f, tween(800))
            }
        }
    }

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val radius = width * 0.45f
        val centerX = width / 2f
        val centerY = height * 0.65f

        // 1. 閸旂姷鐭栨稉濠傚磹閸﹀棗濮敍鍫ｆ憫閼瑰弶绗庨崣姗堢礆
        drawArc(
            brush = Brush.horizontalGradient(colors = listOf(arcStartColor, arcEndColor)),
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(centerX - radius, centerY - radius),
            size = Size(radius * 2, radius * 2),
            style = Stroke(width = 24f, cap = StrokeCap.Round)
        )

        // 2. 閸掕瀹抽敓?
        val tickCount = 11
        for (i in 0..tickCount) {
            val ratio = i.toFloat() / tickCount
            val arcAngle = 180f + 180f * ratio
            val innerRadius = radius - 8f
            val outerRadius = radius + 8f
            val startPoint = getPointOnCircle(centerX, centerY, innerRadius, arcAngle)
            val endPoint = getPointOnCircle(centerX, centerY, outerRadius, arcAngle)
            drawLine(color = tickColor, start = startPoint, end = endPoint, strokeWidth = 4f)
        }

        // 3. 缂佹ê鍩楅幐鍥嫛閿涘牊鐗撮柈銊х煐 閿?鐏忔牠鍎寸紒鍡礆
        rotate(degrees = angleState.value, pivot = Offset(centerX, centerY)) {
            val needleLength = radius * 0.8f
            val rootWidth = 20f   // 閺嶅綊鍎寸€硅棄瀹抽敍鍫㈢煐閿涘苯娓鹃悙瑙勫妳閿?
            val tipWidth = 4f     // 鐏忔牠鍎寸€硅棄瀹抽敍鍫㈢矎閿?

            // 濮婎垰鑸伴幐鍥嫛閿涙艾绨抽柈顭掔礄閸﹀棗绺炬径鍕剁礆鐎规枻绱濇い鍫曞劥閿涘牆娓惧褍顦╅敍澶岀崕
            val leftRoot = Offset(centerX - rootWidth / 2, centerY)
            val rightRoot = Offset(centerX + rootWidth / 2, centerY)
            val leftTip = Offset(centerX - tipWidth / 2, centerY - needleLength)
            val rightTip = Offset(centerX + tipWidth / 2, centerY - needleLength)

            val needlePath = Path().apply {
                moveTo(leftRoot.x, leftRoot.y)
                lineTo(rightRoot.x, rightRoot.y)
                lineTo(rightTip.x, rightTip.y)
                lineTo(leftTip.x, leftTip.y)
                close()
            }
            drawPath(path = needlePath, color = needleColor)

            // 娑擃厼绺炬径褍娓鹃悙鐧哥礄瀵搫瀵茬划妤€娓鹃幇鐕傜礆
            drawCircle(color = needleColor, radius = 14f, center = Offset(centerX, centerY))
            drawCircle(color = Color.White, radius = 5f, center = Offset(centerX, centerY))
        }
    }
}


private fun DrawScope.getPointOnCircle(
    cx: Float,
    cy: Float,
    radius: Float,
    angleDeg: Float
): Offset {
    val angleRad = Math.toRadians(angleDeg.toDouble())
    val x = cx + radius * Math.cos(angleRad).toFloat()
    val y = cy + radius * Math.sin(angleRad).toFloat()
    return Offset(x, y)
}
