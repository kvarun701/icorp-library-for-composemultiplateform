package icorp

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize

object CropEngine {
    /**
     * Crops the original [image] using the coordinates provided in [cropRect].
     * [cropRect] should be defined in the pixel space of the original image.
     */
    fun crop(
        image: ImageBitmap,
        cropRect: Rect
    ): ImageBitmap {
        val left = cropRect.left.coerceIn(0f, image.width.toFloat()).toInt()
        val top = cropRect.top.coerceIn(0f, image.height.toFloat()).toInt()
        val right = cropRect.right.coerceIn(0f, image.width.toFloat()).toInt()
        val bottom = cropRect.bottom.coerceIn(0f, image.height.toFloat()).toInt()

        val width = (right - left).coerceAtLeast(1)
        val height = (bottom - top).coerceAtLeast(1)

        // Create a new blank ImageBitmap of target size
        val croppedBitmap = ImageBitmap(width, height)

        // Draw the specific region of the source image onto the new ImageBitmap
        val canvas = Canvas(croppedBitmap)
        canvas.drawImageRect(
            image = image,
            srcOffset = IntOffset(left, top),
            srcSize = IntSize(width, height),
            dstOffset = IntOffset(0, 0),
            dstSize = IntSize(width, height),
            paint = Paint().apply {
                isAntiAlias = true
            }
        )

        return scaleDownIfNeeded(croppedBitmap, 1500)
    }

    private fun scaleDownIfNeeded(image: ImageBitmap, maxDimension: Int): ImageBitmap {
        if (image.width <= maxDimension && image.height <= maxDimension) {
            return image
        }

        val ratio = image.width.toFloat() / image.height.toFloat()
        val newWidth: Int
        val newHeight: Int
        if (image.width > image.height) {
            newWidth = maxDimension
            newHeight = (maxDimension / ratio).toInt().coerceAtLeast(1)
        } else {
            newHeight = maxDimension
            newWidth = (maxDimension * ratio).toInt().coerceAtLeast(1)
        }

        val scaledBitmap = ImageBitmap(newWidth, newHeight)
        val canvas = Canvas(scaledBitmap)
        canvas.drawImageRect(
            image = image,
            srcOffset = IntOffset.Zero,
            srcSize = IntSize(image.width, image.height),
            dstOffset = IntOffset.Zero,
            dstSize = IntSize(newWidth, newHeight),
            paint = Paint().apply {
                isAntiAlias = true
            }
        )

        return scaledBitmap
    }
}
