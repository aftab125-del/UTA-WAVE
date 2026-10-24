package com.uta.wave.ui.settings.icon

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ImageDecoder
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.net.Uri
import android.os.Build
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.uta.wave.MainActivity
import com.uta.wave.player.AppLogger
import java.io.File
import java.io.FileOutputStream

data class IconPreset(
    val id: String,
    val name: String,
    val description: String,
    val startColor: Int,
    val endColor: Int,
    val accentColor: Int
)

object AppIconHelper {

    val PRESETS = listOf(
        IconPreset(
            id = "DEFAULT",
            name = "Cosmic Violet",
            description = "Original UTA-wave aesthetic",
            startColor = 0xFF581C87.toInt(),
            endColor = 0xFFA855F7.toInt(),
            accentColor = 0xFFF3E8FF.toInt()
        ),
        IconPreset(
            id = "ECHO_PINK",
            name = "Echo Blush Pink",
            description = "Echo Music signature palette",
            startColor = 0xFF4A1D34.toInt(),
            endColor = 0xFFF2AEC7.toInt(),
            accentColor = 0xFFFFFFFF.toInt()
        ),
        IconPreset(
            id = "CYBER_GOLD",
            name = "AMOLED Cyber Gold",
            description = "Deep black with luxury amber",
            startColor = 0xFF09090B.toInt(),
            endColor = 0xFFD97706.toInt(),
            accentColor = 0xFFFDE68A.toInt()
        ),
        IconPreset(
            id = "NEON_BLUE",
            name = "Electric Cyan",
            description = "Vibrant synthwave pulse",
            startColor = 0xFF082F49.toInt(),
            endColor = 0xFF0EA5E9.toInt(),
            accentColor = 0xFFE0F2FE.toInt()
        ),
        IconPreset(
            id = "CRIMSON",
            name = "Crimson Flame",
            description = "Energetic ruby beats",
            startColor = 0xFF450A0A.toInt(),
            endColor = 0xFFE11D48.toInt(),
            accentColor = 0xFFFFE4E6.toInt()
        ),
        IconPreset(
            id = "SAKURA",
            name = "Sakura Pastel",
            description = "Soft floral harmony",
            startColor = 0xFF500724.toInt(),
            endColor = 0xFFF472B6.toInt(),
            accentColor = 0xFFFFF1F2.toInt()
        )
    )

    /**
     * Decodes and processes a user-selected gallery image into a high-res, masked 512x512 app icon Bitmap.
     */
    fun processGalleryImageToIcon(
        context: Context,
        imageUri: Uri,
        shape: String = "SQUIRCLE",
        size: Int = 512
    ): Bitmap? {
        return try {
            val rawBitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(context.contentResolver, imageUri)
                ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                    decoder.isMutableRequired = true
                }
            } else {
                context.contentResolver.openInputStream(imageUri)?.use { stream ->
                    BitmapFactory.decodeStream(stream)
                }
            } ?: return null

            // Crop center square
            val minDim = Math.min(rawBitmap.width, rawBitmap.height)
            val cropX = (rawBitmap.width - minDim) / 2
            val cropY = (rawBitmap.height - minDim) / 2
            val squareBitmap = Bitmap.createBitmap(rawBitmap, cropX, cropY, minDim, minDim)

            // Scale to target size
            val scaledBitmap = Bitmap.createScaledBitmap(squareBitmap, size, size, true)

            // Mask into requested shape
            applyShapeMask(scaledBitmap, shape, size)
        } catch (e: Exception) {
            AppLogger.e("Failed to process gallery image into icon", e)
            null
        }
    }

    /**
     * Generates a 512x512 Bitmap for a preset icon style.
     */
    fun generatePresetIcon(
        preset: IconPreset,
        shape: String = "SQUIRCLE",
        size: Int = 512
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Gradient background
        val gradient = LinearGradient(
            0f, 0f, size.toFloat(), size.toFloat(),
            preset.startColor, preset.endColor,
            Shader.TileMode.CLAMP
        )
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = gradient
        }

        // Clip to shape
        val rect = RectF(0f, 0f, size.toFloat(), size.toFloat())
        val clipPath = createShapePath(rect, shape, size)
        canvas.clipPath(clipPath)
        canvas.drawRect(rect, bgPaint)

        // Draw Stylized "UTA Waveform" in center
        val wavePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = preset.accentColor
            strokeCap = Paint.Cap.ROUND
            strokeWidth = size * 0.045f
            style = Paint.Style.STROKE
        }

        // Draw 5 dynamic sound bars in center
        val centerY = size / 2f
        val centerX = size / 2f
        val barSpacing = size * 0.12f
        val heights = floatArrayOf(0.20f, 0.42f, 0.60f, 0.38f, 0.22f)

        for (i in -2..2) {
            val x = centerX + (i * barSpacing)
            val h = size * heights[i + 2]
            val top = centerY - (h / 2f)
            val bottom = centerY + (h / 2f)
            canvas.drawLine(x, top, x, bottom, wavePaint)
        }

        // Draw subtle ring highlight
        val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0x22FFFFFF
            style = Paint.Style.STROKE
            strokeWidth = size * 0.015f
        }
        canvas.drawCircle(centerX, centerY, size * 0.42f, ringPaint)

        return bitmap
    }

    /**
     * Applies the chosen shape mask (Squircle, Circle, or Rounded) to a square bitmap.
     */
    private fun applyShapeMask(source: Bitmap, shape: String, size: Int): Bitmap {
        val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        val rect = RectF(0f, 0f, size.toFloat(), size.toFloat())
        val path = createShapePath(rect, shape, size)

        canvas.clipPath(path)
        canvas.drawBitmap(source, null, rect, paint)
        return output
    }

    private fun createShapePath(rect: RectF, shape: String, size: Int): Path {
        val path = Path()
        when (shape.uppercase()) {
            "CIRCLE" -> {
                path.addCircle(rect.centerX(), rect.centerY(), size / 2f, Path.Direction.CW)
            }
            "ROUNDED" -> {
                val radius = size * 0.16f
                path.addRoundRect(rect, radius, radius, Path.Direction.CW)
            }
            else -> { // "SQUIRCLE" (Standard modern Android adaptive shape)
                val radius = size * 0.24f
                path.addRoundRect(rect, radius, radius, Path.Direction.CW)
            }
        }
        return path
    }

    /**
     * Saves a Bitmap as a PNG file in app storage.
     */
    fun saveIconFile(context: Context, bitmap: Bitmap, fileName: String = "custom_app_icon.png"): File? {
        return try {
            val iconDir = File(context.filesDir, "custom_icons").apply { if (!exists()) mkdirs() }
            val file = File(iconDir, fileName)
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            file
        } catch (e: Exception) {
            AppLogger.e("Failed to save custom icon to disk", e)
            null
        }
    }

    /**
     * Pins the custom icon directly to the Android Home Screen launcher with a custom app label.
     */
    fun pinToHomeScreen(
        context: Context,
        bitmap: Bitmap,
        label: String
    ): Boolean {
        if (!ShortcutManagerCompat.isRequestPinShortcutSupported(context)) {
            AppLogger.w("Pinning shortcut not supported by device launcher")
            return false
        }

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_MAIN
            addCategory(Intent.CATEGORY_LAUNCHER)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val safeLabel = label.trim().ifBlank { "UTA-wave" }.take(25)
        val shortcutId = "uta_wave_custom_icon_${System.currentTimeMillis()}"

        val shortcutInfo = ShortcutInfoCompat.Builder(context, shortcutId)
            .setShortLabel(safeLabel)
            .setLongLabel(safeLabel)
            .setIcon(IconCompat.createWithBitmap(bitmap))
            .setIntent(launchIntent)
            .build()

        return ShortcutManagerCompat.requestPinShortcut(context, shortcutInfo, null)
    }
}
