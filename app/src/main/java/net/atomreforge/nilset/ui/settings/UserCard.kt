package net.atomreforge.nilset.ui.settings

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import kotlin.math.roundToInt
import net.atomreforge.nilset.R
import net.atomreforge.nilset.data.session.UserInfo
import net.atomreforge.nilset.ui.theme.themeContainerBorderColor
import net.atomreforge.nilset.ui.theme.themeContainerColor

private val UserCardHeight = 96.dp
private val UserAvatarSize = 56.dp
private val UserActionSize = 40.dp
private val UserActionInset = 28.dp

data class UserCardContent(
    val nickname: String,
    val username: String,
    val avatar: String,
)

fun userCardContent(
    userInfo: UserInfo?,
    fallbackUsername: String?,
    unknownLabel: String,
): UserCardContent {
    val username = userInfo?.username?.takeIf { it.isNotBlank() }
        ?: fallbackUsername?.takeIf { it.isNotBlank() }
        ?: unknownLabel
    val nickname = userInfo?.nickname?.takeIf { it.isNotBlank() } ?: username
    return UserCardContent(
        nickname = nickname,
        username = username,
        avatar = userInfo?.avatar.orEmpty(),
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SettingsUserCard(
    userInfo: UserInfo?,
    fallbackUsername: String?,
    showBorder: Boolean,
    onLogout: () -> Unit,
) {
    val unknownLabel = stringResource(R.string.settings_user_unknown)
    val content = userCardContent(userInfo, fallbackUsername, unknownLabel)
    var actionMenuVisible by remember { mutableStateOf(false) }

    Box {
        Surface(
            shape = RoundedCornerShape(8.dp),
            border = if (showBorder) {
                BorderStroke(1.dp, themeContainerBorderColor())
            } else {
                null
            },
            color = themeContainerColor(),
            modifier = Modifier
                .fillMaxWidth()
                .height(UserCardHeight)
                .combinedClickable(
                    onClick = { actionMenuVisible = false },
                    onLongClick = { actionMenuVisible = true },
                ),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(start = 20.dp, end = 76.dp, top = 20.dp, bottom = 20.dp),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(UserAvatarSize)
                        .clip(shape = CircleShape),
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        shape = CircleShape,
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                painter = painterResource(R.drawable.ic_account),
                                contentDescription = stringResource(R.string.settings_user_avatar),
                                modifier = Modifier.size(32.dp),
                            )
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = content.nickname,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                    )
                    Text(
                        text = "@${content.username}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
            }
        }

        if (actionMenuVisible) {
            val popupOffsetX = with(LocalDensity.current) {
                -UserActionInset.roundToPx()
            }
            Popup(
                alignment = Alignment.CenterEnd,
                offset = IntOffset(x = popupOffsetX, y = 0),
                onDismissRequest = { actionMenuVisible = false },
                properties = PopupProperties(focusable = true),
            ) {
                SettingsLogoutActionButton(
                    onClick = {
                        actionMenuVisible = false
                        onLogout()
                    },
                )
            }
        }
    }
}

@Composable
private fun SettingsLogoutActionButton(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.error,
        contentColor = MaterialTheme.colorScheme.onError,
        shape = RoundedCornerShape(10.dp),
        shadowElevation = 6.dp,
        modifier = Modifier.size(UserActionSize),
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Icon(
                painter = painterResource(R.drawable.ic_logout),
                contentDescription = stringResource(R.string.settings_logout),
            )
        }
    }
}
