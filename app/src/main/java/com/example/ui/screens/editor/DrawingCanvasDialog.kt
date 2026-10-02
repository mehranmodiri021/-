package com.example.ui.screens.editor

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.Path as AndroidPath
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.core.i18n.LocalAppStrings
import java.io.File
import java.io.FileOutputStream

data class DrawingPath(
    val path: Path,
    val color: Color,
    val strokeWidth: Float,
    val rawPoints: List<Offset>
)

@Composable
fun DrawingCanvasDialog(
    onDismiss: () -> Unit,
    onSaveDrawing: (filePath: String) -> Unit
) {
    val context = LocalContext.current
    val strings = LocalAppStrings.current

    val paths = remember { mutableStateListOf<DrawingPath>() }
    var currentPath by remember { mutableStateOf<Path?>(null) }
    var currentPoints = remember { mutableListOf<Offset>() }

    var selectedColor by remember { mutableStateOf(Color(0xFF1E293B)) }
    var selectedStrokeWidth by remember { mutableStateOf(8f) }

    val colors = listOf(
        Color(0xFF0F172A), // Dark Navy
        Color(0xFF2563EB), // Blue
        Color(0xFFDC2626), // Red
        Color(0xFF16A34A), // Green
        Color(0xFFD97706), // Orange
        Color(0xFF7C3AED)  // Purple
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = strings.drawingTitle,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { if (paths.isNotEmpty()) paths.removeAt(paths.size - 1) },
                            enabled = paths.isNotEmpty()
                        ) {
                            Icon(Icons.Default.Undo, contentDescription = strings.drawingUndo)
                        }
                        IconButton(onClick = { paths.clear() }) {
                            Icon(Icons.Default.Clear, contentDescription = strings.drawingClear, tint = Color(0xFFEF4444))
                        }
                    }
                }

                // Drawing Canvas Sheet
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White)
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                        .pointerInput(selectedColor, selectedStrokeWidth) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    val newPath = Path().apply { moveTo(offset.x, offset.y) }
                                    currentPath = newPath
                                    currentPoints = mutableListOf(offset)
                                },
                                onDrag = { change, _ ->
                                    val pos = change.position
                                    currentPath?.lineTo(pos.x, pos.y)
                                    currentPoints.add(pos)
                                },
                                onDragEnd = {
                                    currentPath?.let { p ->
                                        paths.add(
                                            DrawingPath(
                                                path = p,
                                                color = selectedColor,
                                                strokeWidth = selectedStrokeWidth,
                                                rawPoints = currentPoints.toList()
                                            )
                                        )
                                    }
                                    currentPath = null
                                    currentPoints = mutableListOf()
                                }
                            )
                        }
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        paths.forEach { dp ->
                            drawPath(
                                path = dp.path,
                                color = dp.color,
                                style = Stroke(
                                    width = dp.strokeWidth,
                                    cap = StrokeCap.Round,
                                    join = StrokeJoin.Round
                                )
                            )
                        }
                        currentPath?.let { cp ->
                            drawPath(
                                path = cp,
                                color = selectedColor,
                                style = Stroke(
                                    width = selectedStrokeWidth,
                                    cap = StrokeCap.Round,
                                    join = StrokeJoin.Round
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Color Palette & Stroke Size Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        colors.forEach { c ->
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(c)
                                    .border(
                                        width = if (selectedColor == c) 3.dp else 1.dp,
                                        color = if (selectedColor == c) MaterialTheme.colorScheme.primary else Color.LightGray,
                                        shape = CircleShape
                                    )
                                    .clickable { selectedColor = c }
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(
                            4f to strings.strokeThin,
                            8f to strings.strokeMedium,
                            16f to strings.strokeThick
                        ).forEach { (width, label) ->
                            TextButton(
                                onClick = { selectedStrokeWidth = width }
                            ) {
                                Text(
                                    text = label,
                                    color = if (selectedStrokeWidth == width) MaterialTheme.colorScheme.primary else Color.Gray
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(strings.cancel)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val savedPath = saveCanvasToBitmapFile(context, paths)
                            if (savedPath != null) {
                                onSaveDrawing(savedPath)
                            }
                            onDismiss()
                        },
                        enabled = paths.isNotEmpty()
                    ) {
                        Text(strings.drawingSave)
                    }
                }
            }
        }
    }
}

private fun saveCanvasToBitmapFile(context: Context, paths: List<DrawingPath>): String? {
    if (paths.isEmpty()) return null
    try {
        val width = 800
        val height = 800
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        canvas.drawColor(android.graphics.Color.WHITE)

        val paint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }

        paths.forEach { dp ->
            paint.color = dp.color.toArgb()
            paint.strokeWidth = dp.strokeWidth
            val aPath = AndroidPath()
            if (dp.rawPoints.isNotEmpty()) {
                aPath.moveTo(dp.rawPoints[0].x, dp.rawPoints[0].y)
                for (i in 1 until dp.rawPoints.size) {
                    aPath.lineTo(dp.rawPoints[i].x, dp.rawPoints[i].y)
                }
            }
            canvas.drawPath(aPath, paint)
        }

        val dir = File(context.filesDir, "drawings")
        if (!dir.exists()) dir.mkdirs()
        val file = File(dir, "draw_${System.currentTimeMillis()}.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        return file.absolutePath
    } catch (e: Exception) {
        e.printStackTrace()
        return null
    }
}
