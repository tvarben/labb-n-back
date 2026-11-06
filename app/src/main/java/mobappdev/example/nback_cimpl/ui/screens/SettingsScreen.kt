package mobappdev.example.nback_cimpl.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import mobappdev.example.nback_cimpl.domain.GameSettings
import mobappdev.example.nback_cimpl.ui.viewmodels.FakeVM
import mobappdev.example.nback_cimpl.ui.viewmodels.GameViewModel
import kotlin.math.roundToInt

/**
 * Settings Screen composable.
 *
 * Provides persistent adjustment of all N-back parameters:
 * 1. N-Back level (n: 1..4)
 * 2. Number of events per round (10..40)
 * 3. Event duration / stimulus presentation interval (1000..3500 ms)
 * 4. Grid dimension (3x3, 4x4, 5x5)
 * 5. Number of distinct spoken letters (4..8)
 * 6. Target match percentage (20%..40%)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    vm: GameViewModel,
    onNavigateBack: () -> Unit
) {
    val settings by vm.settings.collectAsState()
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(text = "Game Settings", fontWeight = FontWeight.Bold)
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. N-Back Level
            SettingCard(
                title = "N-Back Level (n)",
                currentValue = "${settings.nBack}-Back",
                description = "Distance between matching stimuli"
            ) {
                Slider(
                    value = settings.nBack.toFloat(),
                    onValueChange = { vm.updateSettings(settings.copy(nBack = it.roundToInt())) },
                    valueRange = 1f..4f,
                    steps = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // 2. Events Per Round
            SettingCard(
                title = "Events Per Round",
                currentValue = "${settings.eventCount} events",
                description = "Total number of stimuli presented per game"
            ) {
                Slider(
                    value = settings.eventCount.toFloat(),
                    onValueChange = { vm.updateSettings(settings.copy(eventCount = it.roundToInt())) },
                    valueRange = 10f..40f,
                    steps = 5,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // 3. Event Duration
            SettingCard(
                title = "Event Duration",
                currentValue = "${settings.eventDurationMs / 1000.0} seconds",
                description = "Time interval each stimulus is displayed"
            ) {
                Slider(
                    value = (settings.eventDurationMs / 500L).toFloat(),
                    onValueChange = {
                        val duration = (it.roundToInt() * 500L).coerceIn(1000L, 3500L)
                        vm.updateSettings(settings.copy(eventDurationMs = duration))
                    },
                    valueRange = 2f..7f,
                    steps = 4,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // 4. Grid Size
            SettingCard(
                title = "Grid Size",
                currentValue = "${settings.gridSize} × ${settings.gridSize} (${settings.gridSize * settings.gridSize} cells)",
                description = "Dimension of the visual stimulus grid"
            ) {
                Slider(
                    value = settings.gridSize.toFloat(),
                    onValueChange = { vm.updateSettings(settings.copy(gridSize = it.roundToInt())) },
                    valueRange = 3f..5f,
                    steps = 1,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // 5. Distinct Spoken Letters
            SettingCard(
                title = "Phonetic Spoken Letters",
                currentValue = "${settings.letterCount} distinct letters",
                description = "Phonetically distinct letters used for auditory stimuli"
            ) {
                Slider(
                    value = settings.letterCount.toFloat(),
                    onValueChange = { vm.updateSettings(settings.copy(letterCount = it.roundToInt())) },
                    valueRange = 4f..8f,
                    steps = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // 6. Target Match Percentage
            SettingCard(
                title = "Target Match Rate",
                currentValue = "${settings.targetMatchPercentage}%",
                description = "Enforced frequency of matching stimuli (~20% - 40%)"
            ) {
                Slider(
                    value = settings.targetMatchPercentage.toFloat(),
                    onValueChange = { vm.updateSettings(settings.copy(targetMatchPercentage = it.roundToInt())) },
                    valueRange = 20f..40f,
                    steps = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Reset to Defaults Button
            OutlinedButton(
                onClick = { vm.updateSettings(GameSettings()) },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                Text(text = "Reset to Defaults", fontWeight = FontWeight.SemiBold)
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SettingCard(
    title: String,
    currentValue: String,
    description: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    text = currentValue,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp, bottom = 8.dp)
            )
            content()
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SettingsScreenPreview() {
    Surface {
        SettingsScreen(FakeVM(), onNavigateBack = {})
    }
}
