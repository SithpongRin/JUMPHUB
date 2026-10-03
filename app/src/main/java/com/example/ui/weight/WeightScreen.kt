package com.example.ui.weight

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.domain.model.BodyMetric
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun WeightScreen(
    metrics: List<BodyMetric>,
    onLogWeight: (weightKg: Float, waistCm: Float?) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var weightInput by remember { mutableStateOf("") }
    var waistInput by remember { mutableStateOf("") }
    var inputError by remember { mutableStateOf<String?>(null) }

    val latestMetric = metrics.firstOrNull()
    val previousMetric = if (metrics.size >= 2) metrics[1] else null
    val weightDelta = if (latestMetric != null && previousMetric != null) {
        latestMetric.weightKg - previousMetric.weightKg
    } else {
        null
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("weight_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // App Bar
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                Text(
                    text = "Weekly Weight Check-in",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        // Current Snapshot Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Current Weight",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Icon(
                            imageVector = Icons.Default.Scale,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (latestMetric != null) {
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = String.format("%.1f", latestMetric.weightKg),
                                style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Black),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "kg",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                        }

                        if (weightDelta != null) {
                            val sign = if (weightDelta > 0) "+" else ""
                            Text(
                                text = "$sign${String.format("%.1f", weightDelta)} kg since last entry",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (weightDelta <= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary
                            )
                        }
                    } else {
                        Text(
                            text = "No weight logged yet",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Canvas Weight Trend Line Chart
        if (metrics.size >= 2) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "Weight Trend (Weekly)",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        WeightTrendCanvas(metrics = metrics.take(8).reversed())
                    }
                }
            }
        }

        // Add Weight Entry Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Log Weekly Check-in",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = weightInput,
                            onValueChange = {
                                weightInput = it
                                inputError = null
                            },
                            label = { Text("Weight (kg)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f).testTag("weight_input_field"),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = waistInput,
                            onValueChange = { waistInput = it },
                            label = { Text("Waist cm (opt)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    if (inputError != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = inputError!!,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            val weightVal = weightInput.toFloatOrNull()
                            if (weightVal == null || weightVal <= 20f || weightVal >= 300f) {
                                inputError = "Please enter a valid weight (20-300 kg)"
                            } else {
                                val waistVal = waistInput.toFloatOrNull()
                                onLogWeight(weightVal, waistVal)
                                weightInput = ""
                                waistInput = ""
                            }
                        },
                        modifier = Modifier.fillMaxWidth().testTag("save_weight_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save Weekly Weight")
                    }
                }
            }
        }

        // History Log List
        item {
            Text(
                text = "History Logs",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        items(metrics, key = { it.uuid }) { metric ->
            WeightHistoryRow(metric = metric)
        }
    }
}

@Composable
private fun WeightTrendCanvas(metrics: List<BodyMetric>) {
    val primaryColor = MaterialTheme.colorScheme.primary

    Spacer(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .testTag("weight_trend_canvas")
            .drawWithCache {
                if (metrics.size < 2) {
                    onDrawBehind { }
                } else {
                    val weights = metrics.map { it.weightKg }
                    val minWeight = (weights.minOrNull() ?: 50f) - 1f
                    val maxWeight = (weights.maxOrNull() ?: 80f) + 1f
                    val weightRange = (maxWeight - minWeight).coerceAtLeast(1f)

                    val stepX = size.width / (weights.size - 1)
                    val points = weights.mapIndexed { index, w ->
                        val x = index * stepX
                        val y = size.height - ((w - minWeight) / weightRange * (size.height - 20.dp.toPx())) - 10.dp.toPx()
                        Offset(x, y)
                    }
                    val strokeWidthPx = 3.dp.toPx()
                    val circleRadiusPx = 4.dp.toPx()

                    onDrawBehind {
                        for (i in 0 until points.size - 1) {
                            drawLine(
                                color = primaryColor,
                                start = points[i],
                                end = points[i + 1],
                                strokeWidth = strokeWidthPx,
                                cap = StrokeCap.Round
                            )
                        }

                        points.forEach { point ->
                            drawCircle(
                                color = primaryColor,
                                radius = circleRadiusPx,
                                center = point
                            )
                        }
                    }
                }
            }
    )
}

@Composable
private fun WeightHistoryRow(metric: BodyMetric) {
    val dateStr = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(metric.date))
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = dateStr,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = String.format("%.1f", metric.weightKg),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "kg",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
