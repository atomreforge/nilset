package net.atomreforge.nilset.ui.schedule

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextOverflow
import net.atomreforge.nilset.R
import net.atomreforge.nilset.ui.theme.themeContainerBorderColor
import net.atomreforge.nilset.ui.theme.themeContainerColor

@Composable
fun ScheduleGreetingBar(
    state: ScheduleUiState,
    onSelectMember: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var memberMenuExpanded by remember { mutableStateOf(false) }
    val selectedUsername = state.selectedUsername

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, themeContainerBorderColor()),
        color = themeContainerColor(),
    ) {
        Row(
            modifier = Modifier.padding(start = 14.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(
                        R.string.schedule_greeting,
                        selectedUsername.orEmpty(),
                        ScheduleCourseSelector.greetingFor(state.greetingHour),
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = nextCourseText(state.nextCourse),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Box {
                IconButton(onClick = { memberMenuExpanded = true }) {
                    Icon(
                        painter = painterResource(R.drawable.ic_arrowdown),
                        contentDescription = stringResource(R.string.schedule_member_menu),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                DropdownMenu(
                    expanded = memberMenuExpanded,
                    onDismissRequest = { memberMenuExpanded = false },
                    containerColor = themeContainerColor(),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.width(180.dp),
                ) {
                    state.members.forEach { member ->
                        val isSelected = member.username == selectedUsername
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = member.username,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = if (isSelected) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    },
                                )
                            },
                            trailingIcon = if (isSelected) {
                                {
                                    Text(
                                        text = "✓",
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            } else {
                                null
                            },
                            onClick = {
                                memberMenuExpanded = false
                                onSelectMember(member.username)
                            },
                            modifier = Modifier.padding(horizontal = 4.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun nextCourseText(course: ScheduleNextCourse): String = when (course.kind) {
    ScheduleNextCourseKind.TODAY -> stringResource(
        R.string.schedule_next_course_today,
        course.title.orEmpty(),
        course.startTime.orEmpty(),
        course.endTime.orEmpty(),
    )
    ScheduleNextCourseKind.TOMORROW_FIRST -> stringResource(
        R.string.schedule_next_course_tomorrow,
        course.title.orEmpty(),
        course.startTime.orEmpty(),
        course.endTime.orEmpty(),
    )
    ScheduleNextCourseKind.TODAY_FINISHED -> stringResource(R.string.schedule_today_finished)
    ScheduleNextCourseKind.EMPTY -> stringResource(R.string.schedule_no_courses)
}
