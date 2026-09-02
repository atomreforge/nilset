package net.atomreforge.nilset.ui.settings

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.theme_settings_title)) },
                navigationIcon = {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clickable(onClick = onNavigateBack),
                        contentAlignment = Alignment.Center,
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
            ThemePreset.entries.forEach { palette ->
                ThemePaletteCard(
                    palette = palette,
                    isSelected = settings.palette == palette,
                    onClick = { viewModel.selectPalette(palette.id) },
                )
                if (palette == ThemePreset.CUSTOM && settings.palette == ThemePreset.CUSTOM) {
                    ThemeCustomColorEditor(
                        settings = settings,
                        onSaveColors = viewModel::saveCustomColors,
                        onResetColors = viewModel::resetCustomColors,
                    )
                }
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
        ThemeMode.STANDARD to stringResource(R.string.theme_mode_standard),
        ThemeMode.DYNAMIC to stringResource(R.string.theme_mode_dynamic),
    )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column {
            modes.forEachIndexed { index, (mode, label) ->
                ListItem(
                    headlineContent = { Text(label) },
                    supportingContent = if (mode == ThemeMode.DYNAMIC) {
                        { Text(stringResource(R.string.theme_mode_dynamic_description)) }
                    } else {
                        null
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
private fun ThemePaletteCard(
    palette: ThemePreset,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        color = if (isSelected) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        },
    ) {
        ListItem(
            headlineContent = { Text(palette.label) },
            leadingContent = { ThemePalettePreview(palette) },
            trailingContent = {
                if (isSelected) {
                    Text(
                        text = "✓",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            },
        )
    }
}

@Composable
private fun ThemePalettePreview(palette: ThemePreset) {
    val colors = palette.colors ?: UserThemeSettings.DEFAULT_COLORS
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        ThemeColorFields.ALL.forEach { field ->
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(
                        ThemeColorParser.parseArgb(colors.value(field))?.let(::Color)
                            ?: MaterialTheme.colorScheme.surfaceVariant,
                    ),
            )
        }
    }
}

@Composable
private fun ThemeCustomColorEditor(
    settings: UserThemeSettings,
    onSaveColors: (Map<String, String>) -> Unit,
    onResetColors: () -> Unit,
) {
    var colorValues by remember(settings.customColors) {
        mutableStateOf(
            settings.effectiveColors().let { colors ->
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
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
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
