package com.example.ui.records

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.domain.model.PersonalRecord
import com.example.domain.model.RecordType

data class RecordCardItem(
    val type: RecordType,
    val titleRes: Int,
    val icon: ImageVector,
    val unit: String
)

@Composable
fun RecordsScreen(
    records: List<PersonalRecord>,
    onNavVisibilityChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    val isScrollingUp by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex == 0 ||
                    listState.isScrollInProgress.not() ||
                    listState.firstVisibleItemScrollOffset == 0
        }
    }

    LaunchedEffect(isScrollingUp) {
        onNavVisibilityChanged(isScrollingUp)
    }

    val recordTypes = listOf(
        RecordCardItem(RecordType.LONGEST_STREAK, R.string.record_longest_streak, Icons.Default.LocalFireDepartment, "jumps"),
        RecordCardItem(RecordType.MOST_30S, R.string.record_30s, Icons.Default.Timer, "jumps"),
        RecordCardItem(RecordType.MOST_60S, R.string.record_60s, Icons.Default.HourglassBottom, "jumps"),
        RecordCardItem(RecordType.MOST_2M, R.string.record_2m, Icons.Default.ElectricBolt, "jumps"),
        RecordCardItem(RecordType.MOST_5M, R.string.record_5m, Icons.Default.Star, "jumps"),
        RecordCardItem(RecordType.MOST_SESSION, R.string.record_session, Icons.Default.MilitaryTech, "jumps"),
        RecordCardItem(RecordType.PEAK_RATE, R.string.record_peak_rate, Icons.Default.Speed, "RPM")
    )

    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxSize()
            .testTag("records_screen"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column(modifier = Modifier.padding(bottom = 6.dp)) {
                Text(
                    text = stringResource(R.string.records_title),
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = stringResource(R.string.records_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        items(recordTypes, key = { it.type.name }) { item ->
            val existing = records.find { it.type == item.type }
            RecordRowCard(item = item, record = existing)
        }
    }
}

@Composable
private fun RecordRowCard(
    item: RecordCardItem,
    record: PersonalRecord?
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("record_card_${item.type.name}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(item.titleRes),
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (record != null && record.previousValue > 0) {
                    Text(
                        text = "Prev: ${record.previousValue.toInt()} ${item.unit}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                if (record != null) {
                    Text(
                        text = "${record.value.toInt()}",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = item.unit,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        text = stringResource(R.string.record_none),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}
