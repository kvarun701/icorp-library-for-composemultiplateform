package icorp

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun App() {
    // App state
    var originalImage by remember { mutableStateOf(createDummyImage()) }

    ImageCropper(
        image = originalImage,
        onCropSuccess = { cropped ->
            originalImage = cropped
        },
        onCancel = {
            // Revert back to the starting test image on the same screen
            originalImage = createDummyImage()
        }
    )
}

fun createDummyImage(): ImageBitmap {
    val width = 600
    val height = 400
    val bitmap = ImageBitmap(width, height)
    val canvas = Canvas(bitmap)

    // Draw background
    val paint = Paint().apply {
        color = Color(0xFF2E3B4E)
    }
    canvas.drawRect(Rect(0f, 0f, width.toFloat(), height.toFloat()), paint)

    // Draw grid pattern in background
    paint.color = Color(0xFF37474F)
    val gridSize = 40f
    for (i in 0..(width / gridSize.toInt())) {
        canvas.drawLine(
            Offset(i * gridSize, 0f),
            Offset(i * gridSize, height.toFloat()),
            paint
        )
    }
    for (j in 0..(height / gridSize.toInt())) {
        canvas.drawLine(
            Offset(0f, j * gridSize),
            Offset(width.toFloat(), j * gridSize),
            paint
        )
    }

    // Draw some colored shapes
    paint.color = Color(0xFFFF7043) // Coral Orange
    canvas.drawCircle(Offset(180f, 180f), 80f, paint)

    paint.color = Color(0xFF26A69A) // Teal
    canvas.drawRect(Rect(320f, 100f, 480f, 260f), paint)

    paint.color = Color(0xFFFFCA28) // Amber Yellow
    canvas.drawCircle(Offset(480f, 280f), 50f, paint)

    return bitmap
}
