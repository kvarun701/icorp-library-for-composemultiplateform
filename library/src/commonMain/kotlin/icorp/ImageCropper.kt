package icorp

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class CropAspectRatio(val label: String, val ratio: Float?) {
    FREE("Free", null),
    SQUARE("1:1", 1f),
    FOUR_THREE("4:3", 4f / 3f),
    SIXTEEN_NINE("16:9", 16f / 9f)
}

@Composable
fun AspectRatioIcon(modifier: Modifier = Modifier, tint: Color = Color.Black) {
    Canvas(modifier = modifier.size(24.dp)) {
        val w = size.width
        val h = size.height
        drawRect(
            color = tint,
            topLeft = Offset(w * 0.15f, h * 0.22f),
            size = androidx.compose.ui.geometry.Size(w * 0.7f, h * 0.56f),
            style = Stroke(width = 2.dp.toPx())
        )
    }
}

@Composable
fun CropIcon(modifier: Modifier = Modifier, tint: Color = Color.Black) {
    Canvas(modifier = modifier.size(24.dp)) {
        val w = size.width
        val h = size.height
        drawLine(color = tint, start = Offset(w * 0.25f, h * 0.1f), end = Offset(w * 0.25f, h * 0.75f), strokeWidth = 2.dp.toPx())
        drawLine(color = tint, start = Offset(w * 0.25f, h * 0.75f), end = Offset(w * 0.9f, h * 0.75f), strokeWidth = 2.dp.toPx())
        drawLine(color = tint, start = Offset(w * 0.1f, h * 0.25f), end = Offset(w * 0.75f, h * 0.25f), strokeWidth = 2.dp.toPx())
        drawLine(color = tint, start = Offset(w * 0.75f, h * 0.25f), end = Offset(w * 0.75f, h * 0.9f), strokeWidth = 2.dp.toPx())
    }
}

private enum class Handle {
    NONE, TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT, TOP, BOTTOM, LEFT, RIGHT, BODY
}

@Composable
fun ImageCropper(
    image: ImageBitmap,
    modifier: Modifier = Modifier,
    onCropSuccess: (ImageBitmap) -> Unit,
    onCancel: () -> Unit
) {
    var currentImage by remember(image) { mutableStateOf(image) }

    var selectedAspectRatio by remember { mutableStateOf<CropAspectRatio>(CropAspectRatio.FREE) }
    var showAspectRatioOptions by remember { mutableStateOf(false) }
    var zoomScale by remember { mutableStateOf(1.0f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }
    var triggerCrop by remember { mutableStateOf(false) }

    val resetState = {
        zoomScale = 1.0f
        panOffset = Offset.Zero
        selectedAspectRatio = CropAspectRatio.FREE
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF1E1E1E)),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. Top Bar (Orange)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(Color(0xFFF4511E)),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onCancel) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Cancel",
                    tint = Color.White
                )
            }

            Text(
                text = "Edit Photo",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            IconButton(onClick = { triggerCrop = true }) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Save",
                    tint = Color.White
                )
            }

            if (triggerCrop) {
                LaunchedEffect(Unit) {
                    triggerCrop = false
                }
            }
        }

        // 2. Viewport Area
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color(0xFF121212)),
            contentAlignment = Alignment.Center
        ) {
            val density = LocalDensity.current
            val boxWidth = with(density) { maxWidth.toPx() }
            val boxHeight = with(density) { maxHeight.toPx() }

            val imageW = currentImage.width.toFloat()
            val imageH = currentImage.height.toFloat()

            // Calculate base image fitted bounds
            val baseScale = minOf(boxWidth / imageW, boxHeight / imageH)
            val totalScale = baseScale * zoomScale

            val imgRenderedW = imageW * totalScale
            val imgRenderedH = imageH * totalScale

            val imageLeft = (boxWidth / 2f + panOffset.x) - imgRenderedW / 2f
            val imageTop = (boxHeight / 2f + panOffset.y) - imgRenderedH / 2f
            val imageRect = Rect(imageLeft, imageTop, imageLeft + imgRenderedW, imageTop + imgRenderedH)

            // Define Crop Rect State
            var cropRect by remember(currentImage, boxWidth, boxHeight) {
                // Initialize crop box to 80% of fitted image size
                val cw = imageW * baseScale * 0.8f
                val ch = imageH * baseScale * 0.8f
                val cl = (boxWidth - cw) / 2f
                val ct = (boxHeight - ch) / 2f
                mutableStateOf(Rect(cl, ct, cl + cw, ct + ch))
            }

            // Adjust crop box if aspect ratio changes
            LaunchedEffect(selectedAspectRatio) {
                val ratio = selectedAspectRatio.ratio
                if (ratio != null) {
                    val currentW = cropRect.width
                    val proposedH = currentW / ratio
                    
                    if (proposedH <= imageRect.height) {
                        val newTop = imageRect.top + (imageRect.height - proposedH) / 2f
                        cropRect = Rect(
                            left = cropRect.left,
                            top = newTop,
                            right = cropRect.right,
                            bottom = newTop + proposedH
                        )
                    } else {
                        val proposedW = cropRect.height * ratio
                        val newLeft = imageRect.left + (imageRect.width - proposedW) / 2f
                        cropRect = Rect(
                            left = newLeft,
                            top = cropRect.top,
                            right = newLeft + proposedW,
                            bottom = cropRect.bottom
                        )
                    }
                }
            }

            // Keep cropRect strictly coerced inside the active imageRect bounds
            val activeCropRect = cropRect.coerceInside(imageRect)

            if (triggerCrop) {
                val relX = activeCropRect.left - imageRect.left
                val relY = activeCropRect.top - imageRect.top

                val targetRect = Rect(
                    left = relX / totalScale,
                    top = relY / totalScale,
                    right = (relX + activeCropRect.width) / totalScale,
                    bottom = (relY + activeCropRect.height) / totalScale
                )

                val cropped = CropEngine.crop(currentImage, targetRect)
                onCropSuccess(cropped)
            }

            val handleSizePx = with(density) { 20.dp.toPx() }
            var activeHandle by remember { mutableStateOf(Handle.NONE) }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(imageRect, activeCropRect) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                activeHandle = getTouchedHandle(offset, activeCropRect, handleSizePx)
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                when (activeHandle) {
                                    Handle.BODY -> {
                                        // Move crop box inside imageRect bounds
                                        val moved = activeCropRect.translate(dragAmount)
                                        cropRect = moved.coerceInside(imageRect)
                                    }
                                    Handle.NONE -> {
                                        // Dragging outside the crop box pans the image
                                        panOffset = Offset(
                                            x = panOffset.x + dragAmount.x,
                                            y = panOffset.y + dragAmount.y
                                        )
                                    }
                                    else -> {
                                        // Resize/stretch the crop box from all sides
                                        cropRect = updateCropRect(
                                            rect = activeCropRect,
                                            dragAmount = dragAmount,
                                            handle = activeHandle,
                                            bounds = imageRect,
                                            aspectRatio = selectedAspectRatio.ratio
                                        )
                                    }
                                }
                            },
                            onDragEnd = { activeHandle = Handle.NONE },
                            onDragCancel = { activeHandle = Handle.NONE }
                        )
                    }
            ) {
                // Draw Image scaled and offset
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawImage(
                        image = currentImage,
                        srcOffset = androidx.compose.ui.unit.IntOffset.Zero,
                        srcSize = androidx.compose.ui.unit.IntSize(currentImage.width, currentImage.height),
                        dstOffset = androidx.compose.ui.unit.IntOffset(imageRect.left.toInt(), imageRect.top.toInt()),
                        dstSize = androidx.compose.ui.unit.IntSize(imageRect.width.toInt(), imageRect.height.toInt())
                    )
                }

                // Draw Dimmed overlay, Rule of Thirds Grid, and corner drag handles
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val cropPath = Path().apply { addRect(activeCropRect) }
                    
                    clipPath(cropPath, clipOp = ClipOp.Difference) {
                        drawRect(Color.Black.copy(alpha = 0.55f))
                    }

                    drawRect(
                        color = Color.White,
                        topLeft = activeCropRect.topLeft,
                        size = activeCropRect.size,
                        style = Stroke(width = 1.5.dp.toPx())
                    )

                    val segmentW = activeCropRect.width / 3f
                    val segmentH = activeCropRect.height / 3f

                    // Vertical grid lines
                    drawLine(Color.White.copy(alpha = 0.35f), Offset(activeCropRect.left + segmentW, activeCropRect.top), Offset(activeCropRect.left + segmentW, activeCropRect.bottom), strokeWidth = 1.dp.toPx())
                    drawLine(Color.White.copy(alpha = 0.35f), Offset(activeCropRect.left + 2 * segmentW, activeCropRect.top), Offset(activeCropRect.left + 2 * segmentW, activeCropRect.bottom), strokeWidth = 1.dp.toPx())

                    // Horizontal grid lines
                    drawLine(Color.White.copy(alpha = 0.35f), Offset(activeCropRect.left, activeCropRect.top + segmentH), Offset(activeCropRect.right, activeCropRect.top + segmentH), strokeWidth = 1.dp.toPx())
                    drawLine(Color.White.copy(alpha = 0.35f), Offset(activeCropRect.left, activeCropRect.top + 2 * segmentH), Offset(activeCropRect.right, activeCropRect.top + 2 * segmentH), strokeWidth = 1.dp.toPx())

                    // Draw drag handle circles at the four corners
                    val r = 8.dp.toPx()
                    val corners = listOf(
                        activeCropRect.topLeft,
                        activeCropRect.topRight,
                        activeCropRect.bottomLeft,
                        activeCropRect.bottomRight
                    )
                    for (point in corners) {
                        drawCircle(
                            color = Color(0xFFF4511E), // Active orange core
                            radius = r,
                            center = point
                        )
                        drawCircle(
                            color = Color.White,
                            radius = r - 2.dp.toPx(),
                            center = point
                        )
                    }
                }
            }
        }

        // 3. Controller Panel
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (showAspectRatioOptions) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF5F5F5))
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    CropAspectRatio.values().forEach { aspect ->
                        val isSelected = selectedAspectRatio == aspect
                        Text(
                            text = aspect.label,
                            color = if (isSelected) Color(0xFFF4511E) else Color.Gray,
                            fontSize = 14.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier
                                .clickable {
                                    selectedAspectRatio = aspect
                                    showAspectRatioOptions = false
                                }
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Text(
                text = "${(zoomScale * 100).toInt()}%",
                color = Color.Black,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
            )

            TickDial(
                value = zoomScale,
                onValueChange = { zoomScale = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
            )

            Divider(color = Color(0xFFEEEEEE), thickness = 1.dp)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { showAspectRatioOptions = !showAspectRatioOptions }) {
                    AspectRatioIcon(
                        tint = if (showAspectRatioOptions || selectedAspectRatio != CropAspectRatio.FREE) Color(0xFFF4511E) else Color.Black
                    )
                }

                IconButton(onClick = {
                    currentImage = rotateBitmap(currentImage, 90f)
                    panOffset = Offset.Zero
                }) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Rotate Clockwise",
                        tint = Color.Black
                    )
                }

                IconButton(onClick = resetState) {
                    CropIcon(
                        tint = Color.Black
                    )
                }
            }
        }
    }
}

/**
 * Coerces this rectangle bounds to stay completely inside [bounds].
 */
private fun Rect.coerceInside(bounds: Rect): Rect {
    val w = width.coerceAtMost(bounds.width)
    val h = height.coerceAtMost(bounds.height)
    val l = left.coerceIn(bounds.left, bounds.right - w)
    val t = top.coerceIn(bounds.top, bounds.bottom - h)
    return Rect(l, t, l + w, t + h)
}

private fun getTouchedHandle(offset: Offset, rect: Rect, handleSize: Float): Handle {
    val margin = handleSize * 1.5f
    if ((offset - Offset(rect.left, rect.top)).getDistance() < margin) return Handle.TOP_LEFT
    if ((offset - Offset(rect.right, rect.top)).getDistance() < margin) return Handle.TOP_RIGHT
    if ((offset - Offset(rect.left, rect.bottom)).getDistance() < margin) return Handle.BOTTOM_LEFT
    if ((offset - Offset(rect.right, rect.bottom)).getDistance() < margin) return Handle.BOTTOM_RIGHT

    // Check edges
    if (offset.y in (rect.top - margin)..(rect.top + margin) && offset.x in rect.left..rect.right) return Handle.TOP
    if (offset.y in (rect.bottom - margin)..(rect.bottom + margin) && offset.x in rect.left..rect.right) return Handle.BOTTOM
    if (offset.x in (rect.left - margin)..(rect.left + margin) && offset.y in rect.top..rect.bottom) return Handle.LEFT
    if (offset.x in (rect.right - margin)..(rect.right + margin) && offset.y in rect.top..rect.bottom) return Handle.RIGHT

    // Body
    if (rect.contains(offset)) return Handle.BODY

    return Handle.NONE
}

private fun updateCropRect(
    rect: Rect,
    dragAmount: Offset,
    handle: Handle,
    bounds: Rect,
    aspectRatio: Float?
): Rect {
    val minSize = 80f
    var left = rect.left
    var top = rect.top
    var right = rect.right
    var bottom = rect.bottom

    if (aspectRatio == null) {
        // Free sizing
        when (handle) {
            Handle.TOP_LEFT -> {
                left = (left + dragAmount.x).coerceIn(bounds.left, right - minSize)
                top = (top + dragAmount.y).coerceIn(bounds.top, bottom - minSize)
            }
            Handle.TOP_RIGHT -> {
                right = (right + dragAmount.x).coerceIn(left + minSize, bounds.right)
                top = (top + dragAmount.y).coerceIn(bounds.top, bottom - minSize)
            }
            Handle.BOTTOM_LEFT -> {
                left = (left + dragAmount.x).coerceIn(bounds.left, right - minSize)
                bottom = (bottom + dragAmount.y).coerceIn(top + minSize, bounds.bottom)
            }
            Handle.BOTTOM_RIGHT -> {
                right = (right + dragAmount.x).coerceIn(left + minSize, bounds.right)
                bottom = (bottom + dragAmount.y).coerceIn(top + minSize, bounds.bottom)
            }
            Handle.TOP -> {
                top = (top + dragAmount.y).coerceIn(bounds.top, bottom - minSize)
            }
            Handle.BOTTOM -> {
                bottom = (bottom + dragAmount.y).coerceIn(top + minSize, bounds.bottom)
            }
            Handle.LEFT -> {
                left = (left + dragAmount.x).coerceIn(bounds.left, right - minSize)
            }
            Handle.RIGHT -> {
                right = (right + dragAmount.x).coerceIn(left + minSize, bounds.right)
            }
            else -> {}
        }
    } else {
        // Locked aspect ratio
        when (handle) {
            Handle.TOP_LEFT -> {
                val dx = dragAmount.x
                val proposedLeft = (left + dx).coerceIn(bounds.left, right - minSize)
                val newWidth = right - proposedLeft
                val newHeight = newWidth / aspectRatio
                val proposedTop = bottom - newHeight
                if (proposedTop >= bounds.top) {
                    left = proposedLeft
                    top = proposedTop
                } else {
                    top = bounds.top
                    val maxHeight = bottom - top
                    val maxWidth = maxHeight * aspectRatio
                    left = right - maxWidth
                }
            }
            Handle.TOP_RIGHT -> {
                val dx = dragAmount.x
                val proposedRight = (right + dx).coerceIn(left + minSize, bounds.right)
                val newWidth = proposedRight - left
                val newHeight = newWidth / aspectRatio
                val proposedTop = bottom - newHeight
                if (proposedTop >= bounds.top) {
                    right = proposedRight
                    top = proposedTop
                } else {
                    top = bounds.top
                    val maxHeight = bottom - top
                    val maxWidth = maxHeight * aspectRatio
                    right = left + maxWidth
                }
            }
            Handle.BOTTOM_LEFT -> {
                val dx = dragAmount.x
                val proposedLeft = (left + dx).coerceIn(bounds.left, right - minSize)
                val newWidth = right - proposedLeft
                val newHeight = newWidth / aspectRatio
                val proposedBottom = top + newHeight
                if (proposedBottom <= bounds.bottom) {
                    left = proposedLeft
                    bottom = proposedBottom
                } else {
                    bottom = bounds.bottom
                    val maxHeight = bottom - top
                    val maxWidth = maxHeight * aspectRatio
                    left = right - maxWidth
                }
            }
            Handle.BOTTOM_RIGHT -> {
                val dx = dragAmount.x
                val proposedRight = (right + dx).coerceIn(left + minSize, bounds.right)
                val newWidth = proposedRight - left
                val newHeight = newWidth / aspectRatio
                val proposedBottom = top + newHeight
                if (proposedBottom <= bounds.bottom) {
                    right = proposedRight
                    bottom = proposedBottom
                } else {
                    bottom = bounds.bottom
                    val maxHeight = bottom - top
                    val maxWidth = maxHeight * aspectRatio
                    right = left + maxWidth
                }
            }
            Handle.LEFT, Handle.TOP -> {
                val dx = dragAmount.x
                val proposedLeft = (left + dx).coerceIn(bounds.left, right - minSize)
                val newWidth = right - proposedLeft
                val newHeight = newWidth / aspectRatio
                val diffHeight = newHeight - (bottom - top)
                val proposedTop = top - diffHeight / 2f
                val proposedBottom = bottom + diffHeight / 2f
                if (proposedTop >= bounds.top && proposedBottom <= bounds.bottom) {
                    left = proposedLeft
                    top = proposedTop
                    bottom = proposedBottom
                }
            }
            Handle.RIGHT, Handle.BOTTOM -> {
                val dx = dragAmount.x
                val proposedRight = (right + dx).coerceIn(left + minSize, bounds.right)
                val newWidth = proposedRight - left
                val newHeight = newWidth / aspectRatio
                val diffHeight = newHeight - (bottom - top)
                val proposedTop = top - diffHeight / 2f
                val proposedBottom = bottom + diffHeight / 2f
                if (proposedTop >= bounds.top && proposedBottom <= bounds.bottom) {
                    right = proposedRight
                    top = proposedTop
                    bottom = proposedBottom
                }
            }
            else -> {}
        }
    }

    return Rect(left, top, right, bottom)
}

@Composable
private fun TickDial(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val tickSpacing = with(density) { 10.dp.toPx() }

    Box(
        modifier = modifier
            .pointerInput(value) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    val sensitivity = 0.003f
                    val newValue = (value - dragAmount.x * sensitivity).coerceIn(1.0f, 3.0f)
                    onValueChange(newValue)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val centerY = h / 2f

            drawLine(
                color = Color.LightGray.copy(alpha = 0.4f),
                start = Offset(0f, centerY),
                end = Offset(w, centerY),
                strokeWidth = 1.dp.toPx()
            )

            val centerIndexValue = value * 20f
            val startTick = (centerIndexValue - 30).toInt().coerceAtLeast(20)
            val endTick = (centerIndexValue + 30).toInt().coerceAtMost(60)

            for (i in startTick..endTick) {
                val diff = i - centerIndexValue
                val x = w / 2f + diff * tickSpacing

                if (x in 0f..w) {
                    val isMajor = i % 5 == 0
                    val tickHeight = if (isMajor) 18.dp.toPx() else 10.dp.toPx()
                    val tickColor = if (isMajor) Color.DarkGray else Color.Gray.copy(alpha = 0.5f)

                    drawLine(
                        color = tickColor,
                        start = Offset(x, centerY - tickHeight / 2f),
                        end = Offset(x, centerY + tickHeight / 2f),
                        strokeWidth = (if (isMajor) 1.5.dp else 1.dp).toPx()
                    )
                }
            }

            drawLine(
                color = Color(0xFFF4511E),
                start = Offset(w / 2f, centerY - 24.dp.toPx() / 2f),
                end = Offset(w / 2f, centerY + 24.dp.toPx() / 2f),
                strokeWidth = 2.dp.toPx()
            )
        }
    }
}

private fun rotateBitmap(image: ImageBitmap, degrees: Float): ImageBitmap {
    val is90or270 = (degrees % 180f) != 0f
    val newWidth = if (is90or270) image.height else image.width
    val newHeight = if (is90or270) image.width else image.height

    val rotatedBitmap = ImageBitmap(newWidth, newHeight)
    val canvas = androidx.compose.ui.graphics.Canvas(rotatedBitmap)

    val paint = Paint().apply { isAntiAlias = true }

    canvas.save()
    canvas.translate(newWidth / 2f, newHeight / 2f)
    canvas.rotate(degrees)
    canvas.translate(-image.width / 2f, -image.height / 2f)
    canvas.drawImage(image, Offset.Zero, paint)
    canvas.restore()

    return rotatedBitmap
}
