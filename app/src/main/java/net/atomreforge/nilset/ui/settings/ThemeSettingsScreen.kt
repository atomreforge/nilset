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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.atomreforge.nilset.R
import net.atomreforge.nilset.core.theme.ThemeColorFields
import net.atomreforge.nilset.core.theme.ThemeColorParser
import net.atomreforge.nilset.core.theme.ThemeMode
import net.atomreforge.nilset.core.theme.ThemePreset
import net.atomreforge.nilset.core.theme.UserThemeSettings

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

    Scaffold(
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
                    onSaveColors = { values ->
                        viewModel.saveCustomColors(values, useDarkTheme)
                    },
                    onResetColors = {
                        viewModel.resetCustomColors(useDarkTheme)
                    },
                )
            }
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
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column {
            modes.forEachIndexed { index, (mode, label) ->
                ListItem(
                    headlineContent = { Text(label) },
                    supportingContent = null,
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
private fun ThemePaletteCard(
    palette: ThemePreset,
    useDark: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(
            width = 1.dp,
            color = if (isSelected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.outlineVariant
            },
        ),
        color = if (isSelected) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ThemePalettePreview(palette, useDark)
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
) {
    val swatches = if (palette == ThemePreset.DYNAMIC) {
        listOf(
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.secondary,
            MaterialTheme.colorScheme.background,
            MaterialTheme.colorScheme.surface,
        )
    } else {
        val colors = palette.colors(useDark) ?: UserThemeSettings.FALLBACK_DARK_COLORS
        ThemeColorFields.ALL.mapNotNull { field ->
            ThemeColorParser.parseArgb(colors.value(field))?.let(::Color)
        }
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
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
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
