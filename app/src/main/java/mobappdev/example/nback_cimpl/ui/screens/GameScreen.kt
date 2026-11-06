package mobappdev.example.nback_cimpl.ui.screens

import android.content.res.Configuration
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import mobappdev.example.nback_cimpl.R
import mobappdev.example.nback_cimpl.ui.viewmodels.FakeVM
import mobappdev.example.nback_cimpl.ui.viewmodels.GameType
import mobappdev.example.nback_cimpl.ui.viewmodels.GameViewModel
import kotlin.math.roundToInt

/**
 * Game Screen composable.
 *
 * Implements:
 * 1. Live HUD (current event number, hits, score).
 * 2. Responsive square grid (works in portrait and landscape, phone and tablet).
 * 3. Auditory stimulus badge and TTS fallback notification.
 * 4. Dual and single match buttons meeting minimum touch-target size (>= 48dp).
 * 5. Non-offensive animated feedback on incorrect match press (smooth color transition + subtle shake).
 * 6. Clean coroutine cancellation when navigating back mid-round.
 * 7. End-of-round dialog to save score with player name.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameScreen(
    vm: GameViewModel,
    onNavigateBack: () -> Unit
) {
    val gameState by vm.gameState.collectAsState() //14 increments eventIndex & currentScore whenever gameState changes
    val settings by vm.settings.collectAsState()
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    // Player name input state for saving score
    var playerName by remember { mutableStateOf("") }
    var scoreSaved by remember { mutableStateOf(false) }

    //12.2
    // Cleanly cancel game if player leaves screen mid-round
    DisposableEffect(Unit) {
        onDispose {
            if (gameState.isRunning) {
                vm.cancelGame()
            }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "${gameState.gameType.name} (N = ${settings.nBack})",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        vm.cancelGame()
                        onNavigateBack()
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Exit Round")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isLandscape) {
                LandscapeGameLayout(
                    vm = vm,
                    gameState = gameState,
                    gridSize = settings.gridSize,
                    nBack = settings.nBack
                )
            } else {
                PortraitGameLayout(
                    vm = vm,
                    gameState = gameState,
                    gridSize = settings.gridSize,
                    nBack = settings.nBack
                )
            }

            // Round Completion Dialog
            if (gameState.isFinished) {
                AlertDialog(
                    onDismissRequest = { /* Force explicit user choice */ },
                    title = {
                        Text(
                            text = "Round Complete!",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.headlineSmall
                        )
                    },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Final Score: ${gameState.finalScore}",
                                fontWeight = FontWeight.ExtraBold,
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(text = "Hits: ${gameState.hits}")
                            Text(text = "Misses: ${gameState.misses}")
                            Text(text = "False Alarms: ${gameState.falseAlarms}")

                            Spacer(modifier = Modifier.height(8.dp))

                            if (!scoreSaved) {
                                OutlinedTextField(
                                    value = playerName,
                                    onValueChange = { playerName = it },
                                    label = { Text("Enter Your Name") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Button(
                                    onClick = {
                                        vm.saveScore(playerName)
                                        scoreSaved = true
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Save to Scoreboard")
                                }
                            } else {
                                Text(
                                    text = "Score saved for ${playerName.ifEmpty { "Anonymous" }}!",
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                scoreSaved = false
                                playerName = ""
                                vm.startGame()
                            }
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Play Again")
                        }
                    },
                    dismissButton = {
                        OutlinedButton(
                            onClick = onNavigateBack
                        ) {
                            Text("Home Screen")
                        }
                    }
                )
            }
        }
    }
}

/**
 * Portrait Layout: HUD at top, Square Grid in center, Match button(s) at bottom.
 */
@Composable
private fun PortraitGameLayout(
    vm: GameViewModel,
    gameState: mobappdev.example.nback_cimpl.ui.viewmodels.GameState,
    gridSize: Int,
    nBack: Int
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top HUD
        GameHud(gameState = gameState, nBack = nBack)

        // TTS Fallback message if voice data is unavailable
        if (!gameState.ttsAvailable && gameState.ttsFallbackMessage != null && gameState.gameType != GameType.Visual) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Text(
                    text = "TTS Voice Unavailable: Spoken letter is shown below.",
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(8.dp),
                    textAlign = TextAlign.Center
                )
            }
        }

        // Auditory Stimulus Display (for Audio or Dual)
        if (gameState.gameType != GameType.Visual) {
            AuditoryStimulusDisplay(
                audioLetter = gameState.audioLetter,
                isAudioMode = gameState.gameType == GameType.Audio
            )
        }

        // Center Square Grid (for Visual or Dual)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            if (gameState.gameType != GameType.Audio) {
                SquareGrid(
                    gridSize = gridSize,
                    highlightedCell = gameState.eventValue
                )
            } else {
                Text(
                    text = "Listen carefully to each spoken letter...",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }

        // Bottom Match Buttons
        MatchControls(
            vm = vm,
            gameState = gameState,
            nBack = nBack,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * Landscape Layout: Square Grid on Left, HUD + Match buttons on Right.
 */
@Composable
private fun LandscapeGameLayout(
    vm: GameViewModel,
    gameState: mobappdev.example.nback_cimpl.ui.viewmodels.GameState,
    gridSize: Int,
    nBack: Int
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left Column: Square Grid
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            contentAlignment = Alignment.Center
        ) {
            if (gameState.gameType != GameType.Audio) {
                SquareGrid(
                    gridSize = gridSize,
                    highlightedCell = gameState.eventValue
                )
            } else {
                AuditoryStimulusDisplay(
                    audioLetter = gameState.audioLetter,
                    isAudioMode = true
                )
            }
        }

        // Right Column: HUD + Controls
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            GameHud(gameState = gameState, nBack = nBack)

            if (gameState.gameType == GameType.AudioVisual) {
                AuditoryStimulusDisplay(
                    audioLetter = gameState.audioLetter,
                    isAudioMode = false
                )
            }

            MatchControls(
                vm = vm,
                gameState = gameState,
                nBack = nBack,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * HUD displaying progress, hits count, and current score.
 */
@Composable
private fun GameHud(
    gameState: mobappdev.example.nback_cimpl.ui.viewmodels.GameState,
    nBack: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            val currentProgress = if (gameState.totalEvents > 0) {
                (gameState.eventIndex + 1).coerceAtLeast(0).toFloat() / gameState.totalEvents.toFloat()
            } else 0f

            LinearProgressIndicator(
                progress = currentProgress,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val eventNum = (gameState.eventIndex + 1).coerceAtLeast(0)
                Text(
                    text = "Event: $eventNum / ${gameState.totalEvents}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Text(
                    text = "Hits: ${gameState.hits}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = "Score: ${gameState.currentScore}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            if (gameState.eventIndex in 0 until nBack) {
                Text(
                    text = "First $nBack events: Memorizing (no matches possible)",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

/**
 * Responsive square grid composable.
 *
 * Automatically sizes to min(availableWidth, availableHeight) and applies
 * aspectRatio(1f) to stay perfectly square across all screen dimensions.
 */
@Composable
private fun SquareGrid(
    gridSize: Int,
    highlightedCell: Int
) {
    BoxWithConstraints(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        val size = min(maxWidth, maxHeight) * 0.9f //24 Uses BoxWithConstraints to automatically size to min(availableWidth, availableHeight) regardless of mode

        Column(
            modifier = Modifier
                .size(size)
                .aspectRatio(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .border(2.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            for (row in 0 until gridSize) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    for (col in 0 until gridSize) {
                        val cellIndex = row * gridSize + col + 1 // 1-based
                        val isHighlighted = cellIndex == highlightedCell

                        // Animated cell highlight
                        val cellColor by animateColorAsState(
                            targetValue = if (isHighlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                            animationSpec = tween(durationMillis = 150),
                            label = "cellColor"
                        )

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(8.dp))
                                .background(cellColor)
                                .border(
                                    1.dp,
                                    if (isHighlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                    RoundedCornerShape(8.dp)
                                )
                        )
                    }
                }
            }
        }
    }
}

/**
 * Auditory stimulus indicator.
 */
@Composable
private fun AuditoryStimulusDisplay(
    audioLetter: Char?,
    isAudioMode: Boolean = false
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = if (isAudioMode) 0.85f else 0.6f))
            .padding(horizontal = if (isAudioMode) 24.dp else 16.dp, vertical = if (isAudioMode) 16.dp else 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            painter = painterResource(id = R.drawable.sound_on),
            contentDescription = "Spoken Letter",
            modifier = Modifier.size(if (isAudioMode) 32.dp else 24.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Text(
            text = if (audioLetter != null) "Sound: '$audioLetter'" else "Sound: ...",
            style = if (isAudioMode) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

/**
 * Match buttons with non-offensive animated feedback on incorrect press.
 */
@Composable
private fun MatchControls(
    vm: GameViewModel,
    gameState: mobappdev.example.nback_cimpl.ui.viewmodels.GameState,
    nBack: Int,
    modifier: Modifier = Modifier
) {
    val isFirstN = gameState.eventIndex < nBack
    val isGameActive = gameState.isRunning && !isFirstN

    when (gameState.gameType) {
        GameType.Visual -> {
            FeedbackMatchButton(
                text = if (isFirstN) "First $nBack events..." else "Visual Match",
                iconRes = R.drawable.visual,
                isError = gameState.visualFeedbackError,
                isPressedThisTick = gameState.visualPressedThisTick,
                enabled = isGameActive,
                onClick = vm::onVisualMatchPress,
                modifier = modifier
            )
        }
        GameType.Audio -> {
            FeedbackMatchButton(
                text = if (isFirstN) "First $nBack events..." else "Audio Match",
                iconRes = R.drawable.sound_on,
                isError = gameState.audioFeedbackError,
                isPressedThisTick = gameState.audioPressedThisTick,
                enabled = isGameActive,
                onClick = vm::onAudioMatchPress,
                modifier = modifier
            )
        }
        GameType.AudioVisual -> {
            Row(
                modifier = modifier,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FeedbackMatchButton(
                    text = "Visual Match",
                    iconRes = R.drawable.visual,
                    isError = gameState.visualFeedbackError,
                    isPressedThisTick = gameState.visualPressedThisTick,
                    enabled = isGameActive,
                    onClick = vm::onVisualMatchPress,
                    modifier = Modifier.weight(1f)
                )

                FeedbackMatchButton(
                    text = "Audio Match",
                    iconRes = R.drawable.sound_on,
                    isError = gameState.audioFeedbackError,
                    isPressedThisTick = gameState.audioPressedThisTick,
                    enabled = isGameActive,
                    onClick = vm::onAudioMatchPress,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/**
 * Single Match button with:
 * 1. Minimum 48dp touch target height (Accessibility requirement).
 * 2. Non-offensive animated feedback on incorrect press (soft amber transition + subtle horizontal shake).
 * 3. Visual acknowledgment when pressed for the current stimulus.
 */
@Composable
private fun FeedbackMatchButton(
    text: String,
    iconRes: Int,
    isError: Boolean,
    isPressedThisTick: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Non-offensive gentle color transition on incorrect guess:
    // Transitions to a soft warm amber/coral tint, then smoothly returns to primary.
    val buttonColor by animateColorAsState(
        targetValue = when {
            isError -> Color(0xFFE57373) // Soft coral/amber (gentle, non-offensive)
            isPressedThisTick -> MaterialTheme.colorScheme.secondary
            else -> MaterialTheme.colorScheme.primary
        },
        animationSpec = tween(durationMillis = 250),
        label = "buttonErrorColor"
    )

    // Subtle horizontal shake offset animation for tactile feedback
    val shakeOffset = remember { Animatable(0f) }
    LaunchedEffect(isError) {
        if (isError) {
            shakeOffset.animateTo(
                targetValue = 0f,
                animationSpec = keyframes {
                    durationMillis = 300
                    0f at 0
                    -8f at 50
                    8f at 100
                    -6f at 150
                    6f at 200
                    -3f at 250
                    0f at 300
                }
            )
        }
    }

    Button(
        onClick = onClick,
        enabled = enabled && !isPressedThisTick,
        modifier = modifier
            .heightIn(min = 52.dp) // Meets and exceeds 48dp accessibility requirement
            //25, this just works
            .offset { IntOffset(x = shakeOffset.value.roundToInt(), y = 0) },
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = buttonColor,
            disabledContainerColor = if (isPressedThisTick) MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isPressedThisTick) "✓ Registered" else text,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GameScreenPreview() {
    Surface {
        GameScreen(FakeVM(), onNavigateBack = {})
    }
}
