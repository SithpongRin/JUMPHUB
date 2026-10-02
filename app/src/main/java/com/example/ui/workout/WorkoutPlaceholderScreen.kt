package com.example.ui.workout

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R

@Composable
fun WorkoutPlaceholderScreen(
    onFinishWorkout: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var jumpCount by remember { mutableIntStateOf(0) }
    var isPaused by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp)
            .testTag("workout_screen"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Phase and Round Indicator
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 28.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Text(
                    text = if (isPaused) "PAUSED" else "ROUND 1 OF 8",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "00:30 remaining",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        // Huge Jump Counter
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$jumpCount",
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = 96.sp,
                    lineHeight = 100.sp,
                    fontWeight = FontWeight.Black
                ),
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = stringResource(R.string.unit_jumps).uppercase(),
                style = MaterialTheme.typography.titleMedium.copy(
                    letterSpacing = 2.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Large Pause / Stop Controls (Hands-free layout)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Large Pause / Resume Button
            Button(
                onClick = {
                    isPaused = !isPaused
                    if (!isPaused) jumpCount += 10
                },
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Icon(
                    imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                    contentDescription = if (isPaused) "Resume" else "Pause",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(36.dp)
                )
            }

            // Very Large Stop Button
            Button(
                onClick = { onFinishWorkout(jumpCount) },
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Stop,
                    contentDescription = "Stop Workout",
                    tint = MaterialTheme.colorScheme.onError,
                    modifier = Modifier.size(44.dp)
                )
            }
        }
    }
}
