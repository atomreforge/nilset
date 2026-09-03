package net.atomreforge.nilset.ui.settings

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import net.atomreforge.nilset.R
import net.atomreforge.nilset.ui.theme.rememberCustomBackgroundImage
import kotlin.math.pow

private data class CropSelection(
    val startX: Float,
    val startY: Float,
    val endX: Float,
    val endY: Float,
) {
    companion object {
        val Initial = CropSelection(0.12f, 0.10f, 0.88f, 0.80f)
        const val MIN_SPAN = 0.12f
    }
}

private enum class CropDragMode {
    NONE,
    MOVE,
    TOP_LEFT,
    TOP_RIGHT,
    BOTTOM_LEFT,
    BOTTOM_RIGHT,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackgroundCropScreen(
    sourceUri: String,
    onApplied: () -> Unit,
    onDiscard: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val image = rememberCustomBackgroundImage(sourceUri)
    val coroutineScope = rememberCoroutineScope()
    val primaryColor = MaterialTheme.colorScheme.primary
    var selection by remember(sourceUri) { mutableStateOf(CropSelection.Initial) }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    var dragMode by remember { mutableStateOf(CropDragMode.NONE) }
    var isApplying by remember { mutableStateOf(false) }
    var isFailed by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.background_crop_title)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
                navigationIcon = {
                    IconButton(onClick = onDiscard) {
                        Icon(
                            painter = painterResource(R.drawable.ic_back),
                            contentDescription = stringResource(R.string.back),
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.background_crop_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            val currentImage = image
            if (currentImage == null) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            } else {
                val aspectRatio = currentImage.width.toFloat() / currentImage.height.toFloat()
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .aspectRatio(aspectRatio)
                        .onSizeChanged { canvasSize = it }
                        .pointerInput(sourceUri, canvasSize) {
                            detectDragGestures(
                                onDragStart = { position ->
                                    dragMode = cropDragModeFor(
                                        position = position,
                                        selection = selection,
                                        canvasSize = canvasSize,
                                    )
                                },
                                onDrag = { change, amount ->
                                    change.consume()
                                    if (canvasSize == IntSize.Zero || dragMode == CropDragMode.NONE) {
                                        return@detectDragGestures
                                    }

                                    selection = updateCropSelection(
                                        selection = selection,
                                        mode = dragMode,
                                        dragAmount = amount,
                                        canvasSize = canvasSize,
                                    )
                                },
                                onDragEnd = {
                                    dragMode = CropDragMode.NONE
                                },
                                onDragCancel = {
                                    dragMode = CropDragMode.NONE
                                },
                            )
                        },
                ) {
                    Image(
                        bitmap = currentImage,
                        contentDescription = null,
                        contentScale = ContentScale.FillBounds,
                        modifier = Modifier.matchParentSize(),
                    )

                    Canvas(modifier = Modifier.matchParentSize()) {
                        val left = selection.startX * size.width
                        val top = selection.startY * size.height
                        val right = selection.endX * size.width
                        val bottom = selection.endY * size.height
                        val dimColor = Color.Black.copy(alpha = 0.55f)

                        drawRect(dimColor, topLeft = Offset.Zero, size = Size(size.width, top))
                        drawRect(
                            dimColor,
                            topLeft = Offset(0f, bottom),
                            size = Size(size.width, size.height - bottom),
                        )
                        drawRect(
                            dimColor,
                            topLeft = Offset(0f, top),
                            size = Size(left, bottom - top),
                        )
                        drawRect(
                            dimColor,
                            topLeft = Offset(right, top),
                            size = Size(size.width - right, bottom - top),
                        )

                        drawRect(
                            color = primaryColor,
                            topLeft = Offset(left, top),
                            size = Size(right - left, bottom - top),
                            style = Stroke(width = 2.dp.toPx()),
                        )

                        listOf(
                            Offset(left, top),
                            Offset(right, top),
                            Offset(left, bottom),
                            Offset(right, bottom),
                        ).forEach { corner ->
                            drawCircle(
                                color = primaryColor,
                                radius = 7.dp.toPx(),
                                center = corner,
                            )
                        }
                    }
                }
            }

            if (isFailed) {
                Text(
                    text = stringResource(R.string.background_crop_failed),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = {
                        selection = CropSelection.Initial
                        isFailed = false
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(R.string.background_crop_reset))
                }
                Button(
                    onClick = {
                        coroutineScope.launch {
                            isApplying = true
                            isFailed = false
                            val appliedUri = viewModel.applyCroppedBackgroundImage(
                                sourceUri = sourceUri,
                                cropLeft = selection.startX,
                                cropTop = selection.startY,
                                cropRight = selection.endX,
                                cropBottom = selection.endY,
                            )
                            isApplying = false
                            if (appliedUri != null) {
                                onApplied()
                            } else {
                                isFailed = true
                            }
                        }
                    },
                    enabled = currentImage != null && !isApplying,
                    modifier = Modifier.weight(1f),
                ) {
                    if (isApplying) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Text(stringResource(R.string.background_crop_apply))
                    }
                }
            }
        }
    }
}

private fun cropDragModeFor(
    position: Offset,
    selection: CropSelection,
    canvasSize: IntSize,
): CropDragMode {
    if (canvasSize == IntSize.Zero) return CropDragMode.NONE
    val left = selection.startX * canvasSize.width
    val top = selection.startY * canvasSize.height
    val right = selection.endX * canvasSize.width
    val bottom = selection.endY * canvasSize.height
    val handleRadius = (minOf(canvasSize.width, canvasSize.height) * 0.06f).coerceAtLeast(36f)

    return when {
        position.distanceSquared(Offset(left, top)) <= handleRadius * handleRadius -> CropDragMode.TOP_LEFT
        position.distanceSquared(Offset(right, top)) <= handleRadius * handleRadius -> CropDragMode.TOP_RIGHT
        position.distanceSquared(Offset(left, bottom)) <= handleRadius * handleRadius -> CropDragMode.BOTTOM_LEFT
        position.distanceSquared(Offset(right, bottom)) <= handleRadius * handleRadius -> CropDragMode.BOTTOM_RIGHT
        position.x in left..right && position.y in top..bottom -> CropDragMode.MOVE
        else -> CropDragMode.NONE
    }
}

private fun updateCropSelection(
    selection: CropSelection,
    mode: CropDragMode,
    dragAmount: Offset,
    canvasSize: IntSize,
): CropSelection {
    if (canvasSize == IntSize.Zero) return selection
    val deltaX = dragAmount.x / canvasSize.width
    val deltaY = dragAmount.y / canvasSize.height
    val minSpan = CropSelection.MIN_SPAN

    return when (mode) {
        CropDragMode.MOVE -> {
            val boundedX = deltaX.coerceIn(-selection.startX, 1f - selection.endX)
            val boundedY = deltaY.coerceIn(-selection.startY, 1f - selection.endY)
            selection.copy(
                startX = selection.startX + boundedX,
                startY = selection.startY + boundedY,
                endX = selection.endX + boundedX,
                endY = selection.endY + boundedY,
            )
        }
        CropDragMode.TOP_LEFT -> selection.copy(
            startX = (selection.startX + deltaX).coerceIn(0f, selection.endX - minSpan),
            startY = (selection.startY + deltaY).coerceIn(0f, selection.endY - minSpan),
        )
        CropDragMode.TOP_RIGHT -> selection.copy(
            endX = (selection.endX + deltaX).coerceIn(selection.startX + minSpan, 1f),
            startY = (selection.startY + deltaY).coerceIn(0f, selection.endY - minSpan),
        )
        CropDragMode.BOTTOM_LEFT -> selection.copy(
            startX = (selection.startX + deltaX).coerceIn(0f, selection.endX - minSpan),
            endY = (selection.endY + deltaY).coerceIn(selection.startY + minSpan, 1f),
        )
        CropDragMode.BOTTOM_RIGHT -> selection.copy(
            endX = (selection.endX + deltaX).coerceIn(selection.startX + minSpan, 1f),
            endY = (selection.endY + deltaY).coerceIn(selection.startY + minSpan, 1f),
        )
        CropDragMode.NONE -> selection
    }
}

private fun Offset.distanceSquared(other: Offset): Float {
    val deltaX = x - other.x
    val deltaY = y - other.y
    return deltaX * deltaX + deltaY * deltaY
}
