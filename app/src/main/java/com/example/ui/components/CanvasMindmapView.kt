package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.data.model.MindNode
import com.example.data.model.NodeMastery
import com.example.data.model.NodeShape
import kotlin.math.roundToInt

@Composable
fun CanvasMindmapView(
    nodes: List<MindNode>,
    selectedNodeId: String?,
    onNodeSelected: (MindNode) -> Unit,
    onNodeMoved: (MindNode, Float, Float) -> Unit,
    onBackgroundTapped: () -> Unit,
    onNodeDoubleTapped: (MindNode) -> Unit,
    modifier: Modifier = Modifier,
    snapToGrid: Boolean = false,
    showCrossLinks: Boolean = true
) {
    var panX by remember { mutableFloatStateOf(100f) }
    var panY by remember { mutableFloatStateOf(100f) }
    var scale by remember { mutableFloatStateOf(1.0f) }

    var draggingNodeId by remember { mutableStateOf<String?>(null) }
    var dragOffsetDelta by remember { mutableStateOf(Offset.Zero) }

    val nodeMap = remember(nodes) { nodes.associateBy { it.id } }

    val nodeWidth = 190f
    val nodeHeight = 60f

    val surfaceColor = MaterialTheme.colorScheme.surface
    val outlineColor = MaterialTheme.colorScheme.outline
    val primaryColor = MaterialTheme.colorScheme.primary

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("mindmap_canvas")
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(0.25f, 2.8f)
                    panX += pan.x
                    panY += pan.y
                }
            }
            .pointerInput(nodes, scale, panX, panY) {
                detectTapGestures(
                    onTap = { tapOffset ->
                        val worldX = (tapOffset.x - panX) / scale
                        val worldY = (tapOffset.y - panY) / scale
                        val tappedNode = nodes.findLast { n ->
                            worldX >= n.posX && worldX <= n.posX + nodeWidth &&
                                    worldY >= n.posY && worldY <= n.posY + nodeHeight
                        }
                        if (tappedNode != null) {
                            onNodeSelected(tappedNode)
                        } else {
                            onBackgroundTapped()
                        }
                    },
                    onDoubleTap = { tapOffset ->
                        val worldX = (tapOffset.x - panX) / scale
                        val worldY = (tapOffset.y - panY) / scale
                        val tappedNode = nodes.findLast { n ->
                            worldX >= n.posX && worldX <= n.posX + nodeWidth &&
                                    worldY >= n.posY && worldY <= n.posY + nodeHeight
                        }
                        if (tappedNode != null) {
                            onNodeDoubleTapped(tappedNode)
                        }
                    }
                )
            }
            .pointerInput(nodes, scale, panX, panY) {
                detectDragGestures(
                    onDragStart = { startOffset ->
                        val worldX = (startOffset.x - panX) / scale
                        val worldY = (startOffset.y - panY) / scale
                        val tapped = nodes.findLast { n ->
                            worldX >= n.posX && worldX <= n.posX + nodeWidth &&
                                    worldY >= n.posY && worldY <= n.posY + nodeHeight
                        }
                        if (tapped != null) {
                            draggingNodeId = tapped.id
                            dragOffsetDelta = Offset.Zero
                            onNodeSelected(tapped)
                        }
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        if (draggingNodeId != null) {
                            dragOffsetDelta += dragAmount / scale
                            val node = nodeMap[draggingNodeId]
                            if (node != null) {
                                var newX = node.posX + (dragAmount.x / scale)
                                var newY = node.posY + (dragAmount.y / scale)
                                if (snapToGrid) {
                                    newX = (newX / 20f).roundToInt() * 20f
                                    newY = (newY / 20f).roundToInt() * 20f
                                }
                                onNodeMoved(node, newX, newY)
                            }
                        } else {
                            panX += dragAmount.x
                            panY += dragAmount.y
                        }
                    },
                    onDragEnd = {
                        draggingNodeId = null
                        dragOffsetDelta = Offset.Zero
                    },
                    onDragCancel = {
                        draggingNodeId = null
                        dragOffsetDelta = Offset.Zero
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height

            // 1. Draw subtle background dot grid
            val gridSpacing = 40f * scale
            if (gridSpacing > 10f) {
                val startX = (panX % gridSpacing + gridSpacing) % gridSpacing
                val startY = (panY % gridSpacing + gridSpacing) % gridSpacing
                var x = startX
                while (x < canvasWidth) {
                    var y = startY
                    while (y < canvasHeight) {
                        drawCircle(
                            color = outlineColor.copy(alpha = 0.35f),
                            radius = 1.5f * scale.coerceAtLeast(0.8f),
                            center = Offset(x, y)
                        )
                        y += gridSpacing
                    }
                    x += gridSpacing
                }
            }

            // 2. Draw Bezier connections between parent and children
            for (node in nodes) {
                if (node.parentId.isNotBlank() && nodeMap.containsKey(node.parentId)) {
                    val parent = nodeMap[node.parentId]!!

                    val startX = panX + (parent.posX + nodeWidth / 2) * scale
                    val startY = panY + (parent.posY + nodeHeight / 2) * scale
                    val endX = panX + (node.posX + nodeWidth / 2) * scale
                    val endY = panY + (node.posY + nodeHeight / 2) * scale

                    val controlX1 = startX + (endX - startX) * 0.5f
                    val controlY1 = startY
                    val controlX2 = startX + (endX - startX) * 0.5f
                    val controlY2 = endY

                    val path = Path().apply {
                        moveTo(startX, startY)
                        cubicTo(controlX1, controlY1, controlX2, controlY2, endX, endY)
                    }

                    val lineColor = parseColor(parent.colorHex, primaryColor)
                    drawPath(
                        path = path,
                        color = lineColor.copy(alpha = 0.75f),
                        style = Stroke(
                            width = (3.2f * scale).coerceIn(1.5f, 6.0f),
                            cap = StrokeCap.Round
                        )
                    )
                }

                // Draw Cross-links (dotted lines)
                if (showCrossLinks && node.crossLinks.isNotEmpty()) {
                    for (targetId in node.crossLinks) {
                        val targetNode = nodeMap[targetId]
                        if (targetNode != null) {
                            val sx = panX + (node.posX + nodeWidth / 2) * scale
                            val sy = panY + (node.posY + nodeHeight / 2) * scale
                            val ex = panX + (targetNode.posX + nodeWidth / 2) * scale
                            val ey = panY + (targetNode.posY + nodeHeight / 2) * scale

                            val crossPath = Path().apply {
                                moveTo(sx, sy)
                                quadraticTo((sx + ex) / 2 + 30f, (sy + ey) / 2 - 30f, ex, ey)
                            }
                            drawPath(
                                path = crossPath,
                                color = Color(0xFFF59E0B).copy(alpha = 0.6f),
                                style = Stroke(
                                    width = (2f * scale).coerceIn(1.2f, 4f),
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f),
                                    cap = StrokeCap.Round
                                )
                            )
                        }
                    }
                }
            }

            // 3. Draw Nodes
            for (node in nodes) {
                val screenX = panX + node.posX * scale
                val screenY = panY + node.posY * scale
                val scaledWidth = nodeWidth * scale
                val scaledHeight = nodeHeight * scale

                // Frustum culling: skip rendering if totally outside view
                if (screenX + scaledWidth < 0 || screenX > canvasWidth ||
                    screenY + scaledHeight < 0 || screenY > canvasHeight
                ) continue

                val isSelected = node.id == selectedNodeId
                val nodeColor = parseColor(node.colorHex, primaryColor)

                // Draw glow halo if selected
                if (isSelected) {
                    drawRoundRect(
                        color = nodeColor.copy(alpha = 0.35f),
                        topLeft = Offset(screenX - 8f * scale, screenY - 8f * scale),
                        size = Size(scaledWidth + 16f * scale, scaledHeight + 16f * scale),
                        cornerRadius = CornerRadius(16f * scale, 16f * scale)
                    )
                }

                // Draw shape based on node.shape
                val shapeEnum = try { NodeShape.valueOf(node.shape) } catch (_: Exception) { NodeShape.ROUNDED }
                drawNodeShape(
                    shape = shapeEnum,
                    topLeft = Offset(screenX, screenY),
                    size = Size(scaledWidth, scaledHeight),
                    fillColor = surfaceColor,
                    strokeColor = if (isSelected) primaryColor else nodeColor,
                    strokeWidth = (if (isSelected) 3f else 2f) * scale.coerceAtLeast(0.8f)
                )

                // Mastery status badge (dot on top right)
                val masteryColor = when (node.mastery) {
                    NodeMastery.MASTERED.name -> Color(0xFF10B981)
                    NodeMastery.LEARNING.name -> Color(0xFFF59E0B)
                    else -> Color(0xFFEF4444)
                }
                drawCircle(
                    color = masteryColor,
                    radius = (4.5f * scale).coerceIn(3f, 8f),
                    center = Offset(screenX + scaledWidth - 12f * scale, screenY + 12f * scale)
                )

                // Draw Text via Native Canvas
                if (scale >= 0.45f) {
                    drawContext.canvas.nativeCanvas.apply {
                        val paint = android.graphics.Paint().apply {
                            color = android.graphics.Color.WHITE
                            textSize = (13.5f * scale).coerceIn(8f, 26f)
                            isAntiAlias = true
                            typeface = android.graphics.Typeface.create(
                                android.graphics.Typeface.DEFAULT,
                                if (isSelected) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL
                            )
                        }

                        val displayText = "${node.icon} ${node.title}"
                        val maxChars = (20 / scale).toInt().coerceAtLeast(10)
                        val truncated = if (displayText.length > maxChars) displayText.take(maxChars) + "..." else displayText
                        drawText(
                            truncated,
                            screenX + 14f * scale,
                            screenY + (scaledHeight / 2) + (5f * scale),
                            paint
                        )
                    }
                }
            }
        }

        // Mini-Map Navigator in corner
        MiniMap(
            nodes = nodes,
            selectedNodeId = selectedNodeId,
            panX = panX,
            panY = panY,
            scale = scale,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        )
    }
}

private fun DrawScope.drawNodeShape(
    shape: NodeShape,
    topLeft: Offset,
    size: Size,
    fillColor: Color,
    strokeColor: Color,
    strokeWidth: Float
) {
    when (shape) {
        NodeShape.ROUNDED -> {
            drawRoundRect(
                color = fillColor,
                topLeft = topLeft,
                size = size,
                cornerRadius = CornerRadius(14f, 14f)
            )
            drawRoundRect(
                color = strokeColor,
                topLeft = topLeft,
                size = size,
                cornerRadius = CornerRadius(14f, 14f),
                style = Stroke(width = strokeWidth)
            )
        }
        NodeShape.PILL -> {
            val radius = size.height / 2
            drawRoundRect(
                color = fillColor,
                topLeft = topLeft,
                size = size,
                cornerRadius = CornerRadius(radius, radius)
            )
            drawRoundRect(
                color = strokeColor,
                topLeft = topLeft,
                size = size,
                cornerRadius = CornerRadius(radius, radius),
                style = Stroke(width = strokeWidth)
            )
        }
        NodeShape.RECTANGLE -> {
            drawRect(color = fillColor, topLeft = topLeft, size = size)
            drawRect(color = strokeColor, topLeft = topLeft, size = size, style = Stroke(width = strokeWidth))
        }
        NodeShape.DIAMOND -> {
            val path = Path().apply {
                moveTo(topLeft.x + size.width / 2, topLeft.y)
                lineTo(topLeft.x + size.width, topLeft.y + size.height / 2)
                lineTo(topLeft.x + size.width / 2, topLeft.y + size.height)
                lineTo(topLeft.x, topLeft.y + size.height / 2)
                close()
            }
            drawPath(path = path, color = fillColor)
            drawPath(path = path, color = strokeColor, style = Stroke(width = strokeWidth))
        }
        NodeShape.HEXAGON -> {
            val inset = size.width * 0.15f
            val path = Path().apply {
                moveTo(topLeft.x + inset, topLeft.y)
                lineTo(topLeft.x + size.width - inset, topLeft.y)
                lineTo(topLeft.x + size.width, topLeft.y + size.height / 2)
                lineTo(topLeft.x + size.width - inset, topLeft.y + size.height)
                lineTo(topLeft.x + inset, topLeft.y + size.height)
                lineTo(topLeft.x, topLeft.y + size.height / 2)
                close()
            }
            drawPath(path = path, color = fillColor)
            drawPath(path = path, color = strokeColor, style = Stroke(width = strokeWidth))
        }
        NodeShape.CLOUD -> {
            drawRoundRect(
                color = fillColor,
                topLeft = topLeft,
                size = size,
                cornerRadius = CornerRadius(24f, 24f)
            )
            drawRoundRect(
                color = strokeColor,
                topLeft = topLeft,
                size = size,
                cornerRadius = CornerRadius(24f, 24f),
                style = Stroke(width = strokeWidth)
            )
        }
    }
}

@Composable
fun MiniMap(
    nodes: List<MindNode>,
    selectedNodeId: String?,
    panX: Float,
    panY: Float,
    scale: Float,
    modifier: Modifier = Modifier
) {
    if (nodes.isEmpty()) return

    Box(
        modifier = modifier
            .size(110.dp, 80.dp)
            .background(Color(0xCC161B22), RoundedCornerShape(8.dp))
            .border(1.dp, Color(0xFF30363D), RoundedCornerShape(8.dp))
            .padding(4.dp)
            .testTag("mini_map_view")
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            var minX = Float.MAX_VALUE
            var minY = Float.MAX_VALUE
            var maxX = Float.MIN_VALUE
            var maxY = Float.MIN_VALUE

            for (n in nodes) {
                minX = minOf(minX, n.posX)
                maxX = maxOf(maxX, n.posX + 200f)
                minY = minOf(minY, n.posY)
                maxY = maxOf(maxY, n.posY + 70f)
            }

            val rangeX = (maxX - minX).coerceAtLeast(100f)
            val rangeY = (maxY - minY).coerceAtLeast(100f)

            // Draw miniature nodes
            for (n in nodes) {
                val mx = ((n.posX - minX) / rangeX) * size.width
                val my = ((n.posY - minY) / rangeY) * size.height
                val isSel = n.id == selectedNodeId
                drawCircle(
                    color = if (isSel) Color(0xFF58A6FF) else Color(0xFF8B949E),
                    radius = if (isSel) 3.5f else 2.0f,
                    center = Offset(mx.coerceIn(2f, size.width - 2f), my.coerceIn(2f, size.height - 2f))
                )
            }

            // Draw current viewport indicator
            val vpX = ((-panX / scale - minX) / rangeX) * size.width
            val vpY = ((-panY / scale - minY) / rangeY) * size.height
            val vpW = ((size.width / scale) / rangeX) * size.width
            val vpH = ((size.height / scale) / rangeY) * size.height

            drawRect(
                color = Color(0xFF58A6FF).copy(alpha = 0.3f),
                topLeft = Offset(vpX.coerceIn(0f, size.width), vpY.coerceIn(0f, size.height)),
                size = Size(vpW.coerceIn(10f, size.width), vpH.coerceIn(10f, size.height)),
                style = Stroke(width = 1.2f)
            )
        }
    }
}

private fun parseColor(hex: String, defaultColor: Color): Color {
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (_: Exception) {
        defaultColor
    }
}
