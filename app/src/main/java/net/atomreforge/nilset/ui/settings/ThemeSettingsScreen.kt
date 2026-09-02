package net.atomreforge.nilset.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.atomreforge.nilset.R
import net.atomreforge.nilset.core.theme.ThemeColorFields
import net.atomreforge.nilset.core.theme.ThemeColorParser
import net.atomreforge.nilset.core.theme.ThemeColors
import net.atomreforge.nilset.core.theme.ThemeMode
import net.atomreforge.nilset.core.theme.ThemePreset
import net.atomreforge.nilset.core.theme.UserThemeSettings
import kotlinx.coroutines.delay

private const val ThemeEntranceInputDelayMillis = 360L

private val colorLabels = mapOf(
    ThemeColorFields.PRIMARY to "primary（主按钮 / 选中态）",
    ThemeColorFields.SECONDARY to "secondary（点缀）",
    ThemeColorFields.BACKGROUND to "background（页面底）",
    ThemeColorFields.SURFACE to "surface（卡片底）",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeSettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val settings by viewModel.themeSettings.collectAsStateWithLifecycle()
    val useDarkTheme = settings.usesDarkTheme()
    var isNavigatingBack by rememberSaveable { mutableStateOf(false) }
    var isInputEnabled by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(ThemeEntranceInputDelayMillis)
        isInputEnabled = true
    }

    Scaffold(
        modifier = Modifier.blockTouchWhileEntranceLocked(!isInputEnabled),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.theme_settings_title)) },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (!isNavigatingBack) {
                                isNavigatingBack = true
                                onNavigateBack()
                            }
                        },
                        modifier = Modifier
                            .size(48.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_back),
                            contentDescription = stringResource(R.string.back),
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(28.dp),
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.theme_mode),
                style = MaterialTheme.typography.titleMedium,
            )
            ThemeModeGroup(
                settings = settings,
                onModeChange = viewModel::setThemeMode,
            )
            ThemeCardBorderToggle(
                settings = settings,
                onChange = viewModel::setCardBorders,
            )

            Text(
                text = stringResource(R.string.theme_palette),
                style = MaterialTheme.typography.titleMedium,
            )
            ThemePreset.entries.chunked(2).forEach { rowPalettes ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    rowPalettes.forEach { palette ->
                        ThemePaletteCard(
                            palette = palette,
                            useDark = useDarkTheme,
                            isSelected = settings.palette == palette,
                            showBorder = settings.showCardBorders,
                            customColors = if (palette == ThemePreset.CUSTOM) {
                                if (useDarkTheme) settings.customDarkColors else settings.customLightColors
                            } else {
                                null
                            },
                            onClick = { viewModel.selectPalette(palette.id) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (rowPalettes.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }

            if (settings.palette == ThemePreset.CUSTOM) {
                ThemeCustomColorEditor(
                    settings = settings,
                    useDark = useDarkTheme,
                    showBorder = settings.showCardBorders,
                    onSaveColors = { values ->
                        viewModel.saveCustomColors(values, useDarkTheme)
                    },
                    onResetColors = {
                        viewModel.resetCustomColors(useDarkTheme)
                    },
                )
            }

            Text(
                text = stringResource(R.string.theme_display_scaling),
                style = MaterialTheme.typography.titleMedium,
            )
            ThemeScaleControl(
                title = stringResource(R.string.theme_text_scale),
                checked = settings.textScaleEnabled,
                scale = settings.textScale,
                showBorder = settings.showCardBorders,
                onCheckedChange = viewModel::setTextScaleEnabled,
                onScaleChange = viewModel::setTextScale,
                onReset = {
                    viewModel.setTextScaleEnabled(false)
                    viewModel.setTextScale(UserThemeSettings.DEFAULT_SCALE)
                },
            )
            ThemeScaleControl(
                title = stringResource(R.string.theme_ui_scale),
                checked = settings.uiScaleEnabled,
                scale = settings.uiScale,
                showBorder = settings.showCardBorders,
                onCheckedChange = viewModel::setUiScaleEnabled,
                onScaleChange = viewModel::setUiScale,
                onReset = {
                    viewModel.setUiScaleEnabled(false)
                    viewModel.setUiScale(UserThemeSettings.DEFAULT_SCALE)
                },
            )
        }
    }
}

private fun Modifier.blockTouchWhileEntranceLocked(locked: Boolean): Modifier = pointerInput(locked) {
    if (!locked) return@pointerInput
    awaitPointerEventScope {
        while (true) {
            awaitPointerEvent(PointerEventPass.Initial)
                .changes
                .forEach { change -> change.consume() }
        }
    }
}

@Composable
private fun ThemeModeGroup(
    settings: UserThemeSettings,
    onModeChange: (ThemeMode) -> Unit,
) {
    val modes = listOf(
        ThemeMode.LIGHT to stringResource(R.string.theme_mode_light),
        ThemeMode.DARK to stringResource(R.string.theme_mode_dark),
    )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.16f),
        border = if (settings.showCardBorders) {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        } else {
            null
        },
    ) {
        Column {
            modes.forEachIndexed { index, (mode, label) ->
                ListItem(
                    headlineContent = { Text(label) },
                    supportingContent = null,
                    leadingContent = {
                        Icon(
                            painter = painterResource(
                                if (mode == ThemeMode.LIGHT) R.drawable.ic_bright else R.drawable.ic_dark,
                            ),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    trailingContent = {
                        RadioButton(
                            selected = settings.mode == mode,
                            onClick = null,
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onModeChange(mode) },
                )
                if (index < modes.lastIndex) {
                    androidx.compose.material3.HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun ThemeCardBorderToggle(
    settings: UserThemeSettings,
    onChange: (Boolean) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.16f),
        border = if (settings.showCardBorders) {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        } else {
            null
        },
    ) {
        ListItem(
            headlineContent = { Text(stringResource(R.string.theme_card_borders)) },
            leadingContent = {
                Icon(
                    painter = painterResource(R.drawable.ic_border),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            trailingContent = {
                Switch(
                    checked = settings.showCardBorders,
                    onCheckedChange = onChange,
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onChange(!settings.showCardBorders) },
        )
    }
}

@Composable
private fun ThemeScaleControl(
    title: String,
    checked: Boolean,
    scale: Float,
    showBorder: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onScaleChange: (Float) -> Unit,
    onReset: () -> Unit,
) {
    var sliderValue by remember(checked, scale) {
        mutableStateOf(scale)
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.16f),
        border = if (showBorder) {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        } else {
            null
        },
    ) {
        Column {
            ListItem(
                headlineContent = { Text(title) },
                trailingContent = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        IconButton(
                            onClick = onReset,
                            modifier = Modifier.size(38.dp),
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_reset),
                                contentDescription = stringResource(R.string.theme_reset_scale),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(22.dp),
                            )
                        }
                        Switch(
                            checked = checked,
                            onCheckedChange = onCheckedChange,
                        )
                    }
                },
            )
            Slider(
                value = sliderValue,
                onValueChange = { value ->
                    sliderValue = value
                },
                onValueChangeFinished = {
                    onScaleChange(sliderValue)
                },
                valueRange = UserThemeSettings.MIN_SCALE..UserThemeSettings.MAX_SCALE,
                enabled = checked,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            )
            Text(
                text = stringResource(
                    R.string.theme_scale_percent,
                    (sliderValue * 100).roundToInt(),
                ),
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(end = 20.dp, bottom = 10.dp),
            )
        }
    }
}

@Composable
private fun ThemePaletteCard(
    palette: ThemePreset,
    useDark: Boolean,
    isSelected: Boolean,
    showBorder: Boolean,
    customColors: ThemeColors?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        border = if (showBorder) {
            BorderStroke(
                width = 1.dp,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.outlineVariant
                },
            )
        } else {
            null
        },
        color = if (isSelected) {
            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.42f)
        } else {
            MaterialTheme.colorScheme.surface.copy(alpha = 0.24f)
        },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ThemePalettePreview(palette, useDark, customColors)
            Text(
                text = palette.label,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
            )
            Spacer(modifier = Modifier.weight(1f))
            if (isSelected) {
                Text(
                    text = "✓",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.titleSmall,
                )
            }
        }
    }
}

@Composable
private fun ThemePalettePreview(
    palette: ThemePreset,
    useDark: Boolean,
    customColors: ThemeColors?,
) {
    val colors = palette.colors(useDark)
        ?: customColors
        ?: UserThemeSettings.FALLBACK_DARK_COLORS
    val swatches = ThemeColorFields.ALL.mapNotNull { field ->
        ThemeColorParser.parseArgb(colors.value(field))?.let(::Color)
    }

    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        swatches.forEach { color ->
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(color),
            )
        }
    }
}

@Composable
private fun ThemeCustomColorEditor(
    settings: UserThemeSettings,
    useDark: Boolean,
    showBorder: Boolean,
    onSaveColors: (Map<String, String>) -> Unit,
    onResetColors: () -> Unit,
) {
    var colorValues by remember(
        settings.mode,
        settings.customLightColors,
        settings.customDarkColors,
    ) {
        mutableStateOf(
            settings.effectiveColors(useDark).let { colors ->
                ThemeColorFields.ALL.associateWith { field ->
                    colors.value(field).orEmpty()
                }
            },
        )
    }
    val canSave = colorValues.values.all { value ->
        ThemeColorParser.normalize(value) != null
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        border = if (showBorder) {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        } else {
            null
        },
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.24f),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ThemeColorFields.ALL.forEach { field ->
                val value = colorValues.getValue(field)
                val parsedColor = ThemeColorParser.parseArgb(value)
                val isError = value.isNotBlank() && parsedColor == null

                OutlinedTextField(
                    value = value,
                    onValueChange = { next ->
                        colorValues = colorValues + (field to next)
                    },
                    label = { Text(colorLabels.getValue(field)) },
                    isError = isError,
                    supportingText = if (isError) {
                        { Text(stringResource(R.string.theme_invalid_color)) }
                    } else {
                        null
                    },
                    singleLine = true,
                    leadingIcon = {
                        Box(
                            modifier = Modifier
                                .padding(start = 8.dp)
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(
                                    parsedColor?.let(::Color)
                                        ?: MaterialTheme.colorScheme.surfaceVariant,
                                ),
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = { onSaveColors(colorValues) },
                    enabled = canSave,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(R.string.theme_save_colors))
                }
                OutlinedButton(
                    onClick = onResetColors,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(R.string.theme_reset_colors))
                }
            }
        }
    }
}
