package com.uthtodo.dpgh.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
fun UthLogoIcon() {
    val primaryColor = MaterialTheme.colorScheme.primary
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .background(primaryColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(12.dp)) {
                    val path = Path().apply {
                        moveTo(size.width * 0.2f, size.height * 0.5f)
                        lineTo(size.width * 0.45f, size.height * 0.75f)
                        lineTo(size.width * 0.85f, size.height * 0.25f)
                    }
                    drawPath(
                        path = path,
                        color = Color.White,
                        style = Stroke(
                            width = 2.5.dp.toPx(),
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                }
            }

            Box(
                modifier = Modifier
                    .width(38.dp)
                    .height(10.dp)
                    .background(primaryColor, RoundedCornerShape(5.dp))
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Canvas(modifier = Modifier.size(22.dp)) {
                drawCircle(
                    color = primaryColor.copy(alpha = 0.55f),
                    radius = (size.minDimension / 2) - (1.75.dp.toPx()),
                    style = Stroke(width = 3.5.dp.toPx())
                )
            }

            Box(
                modifier = Modifier
                    .width(38.dp)
                    .height(10.dp)
                    .background(primaryColor.copy(alpha = 0.35f), RoundedCornerShape(5.dp))
            )
        }
    }
}

@Composable
fun StudyIcon(color: Color) {
    Canvas(modifier = Modifier.size(18.dp)) {
        val path = Path().apply {
            moveTo(size.width * 0.5f, size.height * 0.25f)
            lineTo(size.width * 0.15f, size.height * 0.15f)
            lineTo(size.width * 0.15f, size.height * 0.75f)
            lineTo(size.width * 0.5f, size.height * 0.85f)
            lineTo(size.width * 0.85f, size.height * 0.75f)
            lineTo(size.width * 0.85f, size.height * 0.15f)
            close()
        }
        drawPath(path, color = color, style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawLine(color, Offset(size.width * 0.5f, size.height * 0.25f), Offset(size.width * 0.5f, size.height * 0.85f), 1.8.dp.toPx())
    }
}

@Composable
fun LifeIcon(color: Color) {
    Canvas(modifier = Modifier.size(18.dp)) {
        val path = Path().apply {
            moveTo(size.width * 0.22f, size.height * 0.25f)
            lineTo(size.width * 0.78f, size.height * 0.25f)
            lineTo(size.width * 0.72f, size.height * 0.75f)
            cubicTo(size.width * 0.7f, size.height * 0.88f, size.width * 0.3f, size.height * 0.88f, size.width * 0.28f, size.height * 0.75f)
            close()
        }
        drawPath(path, color = color, style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawArc(
            color = color,
            startAngle = -90f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(size.width * 0.65f, size.height * 0.35f),
            size = Size(size.width * 0.25f, size.height * 0.3f),
            style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}
