package icorp

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ImageBitmap
import kotlin.test.Test
import kotlin.test.assertEquals

class CropEngineTest {

    @Test
    fun testCropDimensions() {
        // Create a dummy ImageBitmap of 100x100 pixels
        val sourceImage = ImageBitmap(100, 100)
        
        // Define crop rectangle: x=10, y=20, width=50, height=40
        val cropRect = Rect(10f, 20f, 60f, 60f)
        
        // Perform crop
        val cropped = CropEngine.crop(sourceImage, cropRect)
        
        // Verify output dimensions
        assertEquals(50, cropped.width, "Cropped image width should be exactly 50 pixels")
        assertEquals(40, cropped.height, "Cropped image height should be exactly 40 pixels")
    }

    @Test
    fun testCropScaleDownConstraint() {
        // Create a large dummy ImageBitmap of 2000x2000 pixels
        val sourceImage = ImageBitmap(2000, 2000)
        
        // Crop the full image (2000x2000px)
        val cropRect = Rect(0f, 0f, 2000f, 2000f)
        
        // Perform crop
        val cropped = CropEngine.crop(sourceImage, cropRect)
        
        // CropEngine should auto-scale down the output to 1500px (to guarantee < 1MB)
        assertEquals(1500, cropped.width, "Cropped image width should be scaled down to 1500 pixels")
        assertEquals(1500, cropped.height, "Cropped image height should be scaled down to 1500 pixels")
    }
}
