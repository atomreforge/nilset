package net.atomreforge.nilset.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import net.atomreforge.nilset.R
import net.atomreforge.nilset.core.theme.ThemeColorFields
import net.atomreforge.nilset.core.theme.ThemeColorParser
import net.atomreforge.nilset.core.theme.ThemePreset
import net.atomreforge.nilset.core.theme.UserThemeSettings

private val colorLabels = mapOf(
    ThemeColorFields.PRIMARY to "primary",
    ThemeColorFields.ON_PRIMARY to "onPrimary",
    ThemeColorFields.PRIMARY_CONTAINER to "primaryContainer",
    ThemeColorFields.ON_PRIMARY_CONTAINER to "onPrimaryContainer",
    ThemeColorFields.SECONDARY to "secondary",
    ThemeColorFields.ON_SECONDARY to "onSecondary",
    ThemeColorFields.SECONDARY_CONTAINER to "secondaryContainer",
    ThemeColorFields.ON_SECONDARY_CONTAINER to "onSecondaryContainer",
    ThemeColorFields.TERTIARY to "tertiary",
    ThemeColorFields.ON_TERTIARY to "onTertiary",
    ThemeColorFields.TERTIARY_CONTAINER to "tertiaryContainer",
    ThemeColorFields.ON_TERTIARY_CONTAINER to "onTertiaryContainer",
    ThemeColorFields.BACKGROUND to "background",
    ThemeColorFields.ON_BACKGROUND to "onBackground",
    ThemeColorFields.SURFACE to "surface",
    ThemeColorFields.ON_SURFACE to "onSurface",
    ThemeColorFields.SURFACE_VARIANT to "surfaceVariant",
    ThemeColorFields.ON_SURFACE_VARIANT to "onSurfaceVariant",
    ThemeColorFields.OUTLINE to "outline",
    ThemeColorFields.ERROR to "error",
    ThemeColorFields.ON_ERROR to "onError",
)

@Composable
private fun ThemePresetPreview(preset: ThemePreset) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        listOf(
            preset.colors?.primary,
            preset.colors?.secondary,
            preset.colors?.background,
        ).forEach { value ->
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(
                        ThemeColorParser.parseArgb(value)?.let(::Color)
                            ?: MaterialTheme.colorScheme.surfaceVariant,
                    ),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeSettingsSheet(
    settings: UserThemeSettings,
    onSelectPreset: (String) -> Unit,
    onMaterialYouChange: (Boolean) -> Unit,
    onSaveColors: (Map<String, String>) -> Unit,
    onClearColors: () -> Unit,
    onDismiss: () -> Unit,
) {
    var colorValues by remember(settings.presetId, settings.colorOverrides) {
        mutableStateOf(
            ThemeColorFields.ALL.associateWith { field ->
                settings.colorOverrides[field].orEmpty()
            },
        )
    }
    val canSave = colorValues.values.all { value ->
        value.isBlank() || ThemeColorParser.normalize(value) != null
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.settings_theme),
                style = MaterialTheme.typography.titleLarge,
            )

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceContainerLow,
            ) {
                ListItem(
                    headlineContent = {
                        Text(stringResource(R.string.theme_material_you))
                    },
                    supportingContent = {
                        Text("Android 12+ 跟随系统动态配色")
                    },
                    trailingContent = {
                        Switch(
                            checked = settings.materialYou,
                            onCheckedChange = onMaterialYouChange,
                        )
                    },
                )
            }

            Text(
                text = stringResource(R.string.theme_preset),
                style = MaterialTheme.typography.titleMedium,
            )
            ThemePreset.entries.forEach { preset ->
                ListItem(
                    headlineContent = {
                        Text(preset.label)
                    },
                    leadingContent = {
                        ThemePresetPreview(preset)
                    },
                    trailingContent = {
                        if (settings.preset == preset) {
                            Text(
                                text = "✓",
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.titleMedium,
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectPreset(preset.id) },
                    tonalElevation = 2.dp,
                )
            }

            HorizontalDivider()

            Text(
                text = stringResource(R.string.theme_custom_colors),
                style = MaterialTheme.typography.titleMedium,
            )
            ThemeColorFields.ALL.forEach { field ->
                val value = colorValues.getValue(field)
                val parsed = ThemeColorParser.parseArgb(value)
                val isError = value.isNotBlank() && parsed == null

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
                                .background(parsed?.let(::Color) ?: MaterialTheme.colorScheme.surfaceVariant),
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Button(
                    onClick = {
                        onSaveColors(
                            colorValues.filterValues { value ->
                                ThemeColorParser.normalize(value) != null
                            },
                        )
                    },
                    enabled = canSave,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(R.string.theme_save_colors))
                }
                OutlinedButton(
                    onClick = onClearColors,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(R.string.theme_clear_colors))
                }
            }
        }
    }
}
