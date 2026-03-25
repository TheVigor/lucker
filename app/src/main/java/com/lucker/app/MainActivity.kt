package com.lucker.app

import android.graphics.Paint as FrameworkPaint
import android.graphics.RuntimeShader
import android.graphics.Typeface
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LuckerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = LuckerPalette.background
                ) {
                    WinnerPickerScreen()
                }
            }
        }
    }
}

private enum class PickerMode(
    val title: String,
    val subtitle: String,
) {
    Wheel(
        title = "Wheel",
        subtitle = "Сегменты, свет и драматичный стоп у указателя",
    ),
}

private enum class FlowScreen {
    Setup,
    Draw,
}

private object LuckerPalette {
    val background = Color(0xFF050816)
    val panel = Color(0xCC111936)
    val panelBorder = Color(0x40F5C66B)
    val primary = Color(0xFFF5C66B)
    val secondary = Color(0xFFFF6B8E)
    val tertiary = Color(0xFF61D8FF)
    val textPrimary = Color(0xFFF9F5EF)
    val textSecondary = Color(0xB3EEF4FF)
    val wheel = listOf(
        Color(0xFFFFA24B),
        Color(0xFFFF6B8E),
        Color(0xFF8D76FF),
        Color(0xFF5ECAF7),
        Color(0xFF7BE495),
        Color(0xFFF4D35E),
    )
}

private val LuckerTypography = androidx.compose.material3.Typography(
    displayMedium = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Black,
        fontSize = 30.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.4).sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        lineHeight = 22.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Bold,
        fontSize = 15.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 18.sp,
    ),
)

@Composable
private fun LuckerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = LuckerPalette.primary,
            secondary = LuckerPalette.secondary,
            tertiary = LuckerPalette.tertiary,
            background = LuckerPalette.background,
            surface = LuckerPalette.panel,
            onSurface = LuckerPalette.textPrimary,
            onPrimary = Color(0xFF1A1208),
            onSecondary = Color.White,
        ),
        typography = LuckerTypography,
        content = content,
    )
}

@Composable
private fun WinnerPickerScreen() {
    val coroutineScope = rememberCoroutineScope()
    val random = remember { Random(System.currentTimeMillis()) }

    var rawNicknames by rememberSaveable { mutableStateOf(DEFAULT_NICKNAMES) }
    var screenName by rememberSaveable { mutableStateOf(FlowScreen.Setup.name) }
    var winner by rememberSaveable { mutableStateOf<String?>(null) }
    var spinning by remember { mutableStateOf(false) }
    val revealProgress = remember { Animatable(0f) }

    val screen = FlowScreen.valueOf(screenName)
    val nicknames = remember(rawNicknames) { parseNicknames(rawNicknames) }
    val wheelRotation = remember { Animatable(0f) }

    fun launchSpin() {
        if (nicknames.isEmpty() || spinning) {
            return
        }

        coroutineScope.launch {
            spinning = true
            winner = null
            revealProgress.snapTo(0f)
            val winnerIndex = random.nextInt(nicknames.size)
            val winnerHandle = nicknames[winnerIndex]

            spinWheel(
                namesCount = nicknames.size,
                winnerIndex = winnerIndex,
                rotation = wheelRotation,
            )
            winner = winnerHandle
            delay(90)
            revealProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = 950,
                    easing = CubicBezierEasing(0.18f, 0.9f, 0.2f, 1f),
                ),
            )
            spinning = false
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedBackdrop(isSpinning = spinning)

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(
                    WindowInsets.safeDrawing.only(
                        WindowInsetsSides.Top + WindowInsetsSides.Bottom
                    )
                )
                .imePadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            when (screen) {
                FlowScreen.Setup -> SetupScreen(
                    rawNicknames = rawNicknames,
                    onRawNicknamesChange = { rawNicknames = it },
                    participantsCount = nicknames.size,
                    onContinue = {
                        winner = null
                        screenName = FlowScreen.Draw.name
                    },
                    modifier = Modifier.fillMaxSize(),
                )
                FlowScreen.Draw -> DrawScreen(
                    names = nicknames,
                    winner = winner,
                    spinning = spinning,
                    wheelRotation = wheelRotation.value,
                    revealProgress = revealProgress.value,
                    onSpin = ::launchSpin,
                    onBack = {
                        if (!spinning) {
                            screenName = FlowScreen.Setup.name
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        FrontEffectsOverlay(
            energetic = spinning,
            modifier = Modifier.matchParentSize(),
        )
    }
}

@Composable
private fun HeroHeader(
    participantsCount: Int,
    mode: PickerMode,
    winner: String?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Badge(text = "LUCKER")

        Text(
            text = "Розыгрыш в Telegram",
            style = MaterialTheme.typography.displayMedium,
            color = LuckerPalette.textPrimary,
        )

        Text(
            text = "Список, режим, запуск.",
            style = MaterialTheme.typography.bodyLarge,
            color = LuckerPalette.textSecondary,
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            StatPill(label = "Участники", value = participantsCount.toString())
            StatPill(label = "Режим", value = mode.title)
            StatPill(label = "Победа", value = winner ?: "нет")
        }
    }
}

@Composable
private fun SetupScreen(
    rawNicknames: String,
    onRawNicknamesChange: (String) -> Unit,
    participantsCount: Int,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    MinimalSetupCard(
        rawNicknames = rawNicknames,
        onRawNicknamesChange = onRawNicknamesChange,
        participantsCount = participantsCount,
        onContinue = onContinue,
        modifier = modifier,
    )
}

@Composable
private fun SetupHeader(
    participantsCount: Int,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        color = LuckerPalette.panel.copy(alpha = 0.72f),
        border = androidx.compose.foundation.BorderStroke(1.dp, LuckerPalette.panelBorder),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Badge(text = "STEP 1")
                Text(
                    text = "Добавь участников.",
                    style = MaterialTheme.typography.displayMedium,
                    color = LuckerPalette.textPrimary,
                )
                Text(
                    text = "Потом откроем сцену розыгрыша.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = LuckerPalette.textSecondary,
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                StatPill(label = "Участники", value = participantsCount.toString())
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0x1AFFFFFF),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x24FFFFFF)),
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = "Дальше",
                            style = MaterialTheme.typography.titleLarge,
                            color = LuckerPalette.textPrimary,
                        )
                        Text(
                            text = "Только режим и запуск.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = LuckerPalette.textSecondary,
                        )
                    }
                }
            }
        }

    }
}

@Composable
private fun CompactSetupHeader(
    participantsCount: Int,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(28.dp),
        color = LuckerPalette.panel.copy(alpha = 0.72f),
        border = androidx.compose.foundation.BorderStroke(1.dp, LuckerPalette.panelBorder),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Badge(text = "STEP 1")
                Text(
                    text = "Добавь участников.",
                    style = MaterialTheme.typography.headlineMedium,
                    color = LuckerPalette.textPrimary,
                )
            }

            Spacer(modifier = Modifier.size(8.dp))
            StatPill(label = "Участники", value = participantsCount.toString())
        }
    }
}

@Composable
private fun SetupCard(
    rawNicknames: String,
    onRawNicknamesChange: (String) -> Unit,
    participantsCount: Int,
    onContinue: () -> Unit,
    compactLayout: Boolean,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(28.dp),
        color = LuckerPalette.panel.copy(alpha = 0.94f),
        border = androidx.compose.foundation.BorderStroke(1.dp, LuckerPalette.panelBorder),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Участники",
                    style = MaterialTheme.typography.headlineMedium,
                    color = LuckerPalette.textPrimary,
                )
                Text(
                    text = "@username, t.me или ник.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = LuckerPalette.textSecondary,
                )
            }

            OutlinedTextField(
                value = rawNicknames,
                onValueChange = onRawNicknamesChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = LuckerPalette.textPrimary),
                label = { Text("Никнеймы / ссылки") },
                placeholder = { Text("@lucky_one\nt.me/perfect_guest") },
                minLines = if (compactLayout) 7 else 10,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None),
                colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = LuckerPalette.primary,
                    unfocusedBorderColor = LuckerPalette.panelBorder,
                    focusedLabelColor = LuckerPalette.primary,
                    unfocusedLabelColor = LuckerPalette.textSecondary,
                    focusedTextColor = LuckerPalette.textPrimary,
                    unfocusedTextColor = LuckerPalette.textPrimary,
                    cursorColor = LuckerPalette.secondary,
                ),
            )

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0x40111A30),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x1FFFFFFF)),
            ) {
                if (compactLayout) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "Готово к переходу",
                                style = MaterialTheme.typography.titleLarge,
                                color = LuckerPalette.textPrimary,
                            )
                            Text(
                                text = "Уникальных участников: $participantsCount",
                                style = MaterialTheme.typography.bodyLarge,
                                color = LuckerPalette.textSecondary,
                            )
                        }

                        Button(
                            onClick = onContinue,
                            enabled = participantsCount > 0,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = LuckerPalette.primary,
                                contentColor = Color(0xFF1D1405),
                                disabledContainerColor = Color(0xFF6A6151),
                                disabledContentColor = Color(0xFF211C13),
                            ),
                            shape = RoundedCornerShape(18.dp),
                        ) {
                            Text(
                                text = if (participantsCount > 0) "К сцене розыгрыша" else "Добавь участников",
                                style = MaterialTheme.typography.titleLarge,
                            )
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "Готово к переходу",
                                style = MaterialTheme.typography.titleLarge,
                                color = LuckerPalette.textPrimary,
                            )
                            Text(
                                text = "Уникальных участников: $participantsCount",
                                style = MaterialTheme.typography.bodyLarge,
                                color = LuckerPalette.textSecondary,
                            )
                        }

                        Button(
                            onClick = onContinue,
                            enabled = participantsCount > 0,
                            modifier = Modifier.height(48.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = LuckerPalette.primary,
                                contentColor = Color(0xFF1D1405),
                                disabledContainerColor = Color(0xFF6A6151),
                                disabledContentColor = Color(0xFF211C13),
                            ),
                            shape = RoundedCornerShape(18.dp),
                        ) {
                            Text(
                                text = if (participantsCount > 0) "К сцене розыгрыша" else "Добавь участников",
                                style = MaterialTheme.typography.titleLarge,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DrawScreen(
    names: List<String>,
    winner: String?,
    spinning: Boolean,
    wheelRotation: Float,
    revealProgress: Float,
    onSpin: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(enabled = !spinning, onBack = onBack)

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        StageCard(
            names = names,
            winner = winner,
            spinning = spinning,
            wheelRotation = wheelRotation,
            revealProgress = revealProgress,
            onSpin = onSpin,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun MinimalSetupCard(
    rawNicknames: String,
    onRawNicknamesChange: (String) -> Unit,
    participantsCount: Int,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(28.dp),
        color = LuckerPalette.panel.copy(alpha = 0.94f),
        border = androidx.compose.foundation.BorderStroke(1.dp, LuckerPalette.panelBorder),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            OutlinedTextField(
                value = rawNicknames,
                onValueChange = onRawNicknamesChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = LuckerPalette.textPrimary),
                placeholder = { Text("@lucky_one\nt.me/perfect_guest") },
                minLines = 10,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None),
                colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = LuckerPalette.primary,
                    unfocusedBorderColor = LuckerPalette.panelBorder,
                    focusedTextColor = LuckerPalette.textPrimary,
                    unfocusedTextColor = LuckerPalette.textPrimary,
                    cursorColor = LuckerPalette.secondary,
                ),
            )

            Button(
                onClick = onContinue,
                enabled = participantsCount > 0,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = LuckerPalette.primary,
                    contentColor = Color(0xFF1D1405),
                    disabledContainerColor = Color(0xFF6A6151),
                    disabledContentColor = Color(0xFF211C13),
                ),
                shape = RoundedCornerShape(18.dp),
            ) {
                Text(
                    text = if (participantsCount > 0) "Дальше" else "Добавь участников",
                    style = MaterialTheme.typography.titleLarge,
                )
            }
        }
    }
}

@Composable
private fun StageCard(
    names: List<String>,
    winner: String?,
    spinning: Boolean,
    wheelRotation: Float,
    revealProgress: Float,
    onSpin: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(28.dp),
        color = LuckerPalette.panel,
        border = androidx.compose.foundation.BorderStroke(1.dp, LuckerPalette.panelBorder),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
        ) {
            WheelStage(
                names = names,
                rotation = wheelRotation,
                winner = winner,
                spinning = spinning,
                revealProgress = revealProgress,
                onSpin = onSpin,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun DrawControlsCard(
    currentMode: PickerMode,
    onModeChange: (PickerMode) -> Unit,
    participants: List<String>,
    spinning: Boolean,
    onSpin: () -> Unit,
    onBack: () -> Unit,
    winner: String?,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(28.dp),
        color = LuckerPalette.panel.copy(alpha = 0.94f),
        border = androidx.compose.foundation.BorderStroke(1.dp, LuckerPalette.panelBorder),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Розыгрыш",
                    style = MaterialTheme.typography.headlineMedium,
                    color = LuckerPalette.textPrimary,
                )
                Text(
                    text = "Режим, запуск, сводка.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = LuckerPalette.textSecondary,
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Режим финала",
                    style = MaterialTheme.typography.titleLarge,
                    color = LuckerPalette.textPrimary,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PickerMode.entries.forEach { mode ->
                        ModeChip(
                            mode = mode,
                            selected = currentMode == mode,
                            onClick = { onModeChange(mode) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0x40111A30),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x1FFFFFFF)),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = "Готово",
                        style = MaterialTheme.typography.titleLarge,
                        color = LuckerPalette.textPrimary,
                    )
                    Text(
                        text = "Уникальных участников: ${participants.size}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = LuckerPalette.textSecondary,
                    )
                    Text(
                        text = currentMode.subtitle,
                        style = MaterialTheme.typography.bodyLarge,
                        color = LuckerPalette.textSecondary,
                    )
                }
            }

            ParticipantsPreview(
                participants = participants,
                modifier = Modifier.weight(1f, fill = false),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = onBack,
                    enabled = !spinning,
                    modifier = Modifier
                        .weight(0.42f)
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0x22FFFFFF),
                        contentColor = LuckerPalette.textPrimary,
                        disabledContainerColor = Color(0x1AFFFFFF),
                        disabledContentColor = LuckerPalette.textSecondary,
                    ),
                    shape = RoundedCornerShape(22.dp),
                ) {
                    Text(
                        text = "Список",
                        style = MaterialTheme.typography.titleLarge,
                    )
                }

                Button(
                    onClick = onSpin,
                    enabled = participants.isNotEmpty() && !spinning,
                    modifier = Modifier
                        .weight(0.58f)
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LuckerPalette.primary,
                        contentColor = Color(0xFF1D1405),
                        disabledContainerColor = Color(0xFF6A6151),
                        disabledContentColor = Color(0xFF211C13),
                    ),
                    shape = RoundedCornerShape(22.dp),
                ) {
                    Text(
                        text = when {
                            spinning -> "Крутим..."
                            participants.isEmpty() -> "Добавь участников"
                            else -> "Запустить"
                        },
                        style = MaterialTheme.typography.titleLarge,
                    )
                }
            }

            if (winner != null) {
                Text(
                    text = "Финальный результат: $winner",
                    style = MaterialTheme.typography.headlineMedium,
                    color = LuckerPalette.primary,
                )
            }
        }
    }
}

@Composable
private fun ParticipantsPreview(
    participants: List<String>,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = Color(0x1AFFFFFF),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x24FFFFFF)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = "Превью",
                style = MaterialTheme.typography.titleLarge,
                color = LuckerPalette.textPrimary,
            )
            Text(
                text = participants.take(6).joinToString("    ") { it.shortHandle(18) },
                style = MaterialTheme.typography.bodyLarge,
                color = LuckerPalette.textSecondary,
            )
            if (participants.size > 6) {
                Text(
                    text = "И ещё ${participants.size - 6} участников в пуле.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = LuckerPalette.textSecondary,
                )
            }
        }
    }
}

@Composable
private fun ModeChip(
    mode: PickerMode,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = if (selected) LuckerPalette.primary.copy(alpha = 0.92f) else Color(0x1AFFFFFF),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (selected) LuckerPalette.primary else Color(0x29FFFFFF),
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            Text(
                text = mode.title,
                style = MaterialTheme.typography.titleLarge,
                color = if (selected) Color(0xFF251600) else LuckerPalette.textPrimary,
            )
        }
    }
}

@Composable
private fun Badge(text: String) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = Color(0x1FF5C66B),
        border = androidx.compose.foundation.BorderStroke(1.dp, LuckerPalette.panelBorder),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            color = LuckerPalette.primary,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.8.sp,
            ),
        )
    }
}

@Composable
private fun StatPill(
    label: String,
    value: String,
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color(0x1AFFFFFF),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x24FFFFFF)),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(1.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 10.sp),
                color = LuckerPalette.textSecondary,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                color = LuckerPalette.textPrimary,
            )
        }
    }
}

@Composable
private fun AnimatedBackdrop(
    isSpinning: Boolean,
    modifier: Modifier = Modifier,
) {
    val infinite = rememberInfiniteTransition(label = "background_shader")
    val time by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1_000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 34_000, easing = LinearEasing),
        ),
        label = "background_shader_time",
    )

    Box(modifier = modifier.fillMaxSize()) {
        ShaderBackdrop(
            time = time,
            modifier = Modifier.matchParentSize(),
        )
    }
}

@Composable
private fun FrontEffectsOverlay(
    energetic: Boolean,
    modifier: Modifier = Modifier,
) {
    val infinite = rememberInfiniteTransition(label = "front_lava")
    val time by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1_000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 52_000, easing = LinearEasing),
        ),
        label = "front_lava_time",
    )

    LavaLampOverlay(
        time = time,
        energetic = energetic,
        modifier = modifier,
    )
}

@Composable
private fun ShaderBackdrop(
    time: Float,
    modifier: Modifier = Modifier,
) {
    val shader = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            RuntimeShader(BACKDROP_SHADER)
        } else {
            null
        }
    }

    Canvas(modifier = modifier) {
        if (shader != null) {
            shader.setFloatUniform("resolution", size.width, size.height)
            shader.setFloatUniform("time", time)

            drawIntoCanvas { canvas ->
                val paint = FrameworkPaint().apply {
                    isAntiAlias = true
                    this.shader = shader
                }
                canvas.nativeCanvas.drawRect(0f, 0f, size.width, size.height, paint)
            }
        } else {
            drawRect(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF071023),
                        Color(0xFF10142C),
                        Color(0xFF170C24),
                    ),
                ),
            )
        }
    }
}

@Composable
private fun WheelStage(
    names: List<String>,
    rotation: Float,
    winner: String?,
    spinning: Boolean,
    revealProgress: Float,
    onSpin: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pulseTransition = rememberInfiniteTransition(label = "wheel_center_pulse")
    val centerScale by pulseTransition.animateFloat(
        initialValue = 0.97f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "wheel_center_scale",
    )
    val centerGlow by pulseTransition.animateFloat(
        initialValue = 0.22f,
        targetValue = 0.46f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "wheel_center_glow",
    )

    Column(modifier = modifier) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 2.dp),
            ) {
                drawWheel(
                    names = names,
                    rotation = rotation,
                    winner = winner,
                    spinning = spinning,
                    revealProgress = revealProgress,
                    centerYFraction = 0.44f,
                    diameterFactor = 0.98f,
                )
            }

            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 4.dp),
            ) {
                Canvas(modifier = Modifier.size(width = 46.dp, height = 28.dp)) {
                    val pointer = Path().apply {
                        moveTo(size.width / 2f, size.height)
                        lineTo(0f, 0f)
                        lineTo(size.width, 0f)
                        close()
                    }
                    drawPath(pointer, brush = Brush.verticalGradient(listOf(LuckerPalette.primary, LuckerPalette.secondary)))
                }
            }

            Surface(
                onClick = onSpin,
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = -(maxHeight * 0.06f))
                    .graphicsLayer(
                        scaleX = if (spinning) 1f else centerScale,
                        scaleY = if (spinning) 1f else centerScale,
                    ),
                enabled = !spinning,
                shape = CircleShape,
                color = Color(0xD1141D3C),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    Color.White.copy(alpha = if (spinning) 0.18f else 0.32f + centerGlow * 0.24f),
                ),
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(0.dp),
                ) {
                    Text(
                        text = "LUCKER",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = 10.sp,
                            letterSpacing = 1.2.sp,
                            fontWeight = FontWeight.SemiBold,
                        ),
                        color = LuckerPalette.textSecondary,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(156.dp),
        ) {
            WinnerRevealOverlay(
                winner = winner,
                progress = revealProgress,
                modifier = Modifier.matchParentSize(),
            )
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawWheel(
    names: List<String>,
    rotation: Float,
    winner: String?,
    spinning: Boolean,
    revealProgress: Float,
    centerYFraction: Float,
    diameterFactor: Float,
) {
    val diameter = min(size.width * diameterFactor, size.height * 0.98f)
    val radius = diameter / 2f
    val wheelCenter = Offset(size.width / 2f, size.height * centerYFraction)
    val topLeft = Offset(wheelCenter.x - radius, wheelCenter.y - radius)
    val arcSize = Size(diameter, diameter)

    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                LuckerPalette.primary.copy(alpha = 0.28f),
                Color.Transparent,
            ),
            center = wheelCenter,
            radius = radius * 1.25f,
        ),
        radius = radius * 1.25f,
        center = wheelCenter,
        blendMode = BlendMode.Screen,
    )

    if (names.isEmpty()) {
        drawCircle(
            color = Color(0xFF111936),
            radius = radius,
            center = wheelCenter,
        )
        drawCircle(
            color = Color(0x30FFFFFF),
            radius = radius * 0.98f,
            center = wheelCenter,
            style = Stroke(width = radius * 0.03f),
        )
        return
    }

    val sweep = 360f / names.size
    val labelPaint = FrameworkPaint(FrameworkPaint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        textAlign = android.graphics.Paint.Align.CENTER
        textSize = (radius * (0.24f - (names.size.coerceAtLeast(4) / 52f))).coerceIn(22f, 42f)
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        setShadowLayer(radius * 0.04f, 0f, 0f, android.graphics.Color.argb(180, 0, 0, 0))
    }

    names.forEachIndexed { index, name ->
        val startAngle = rotation + segmentStartAngle(index, sweep)
        val segmentColor = LuckerPalette.wheel[index % LuckerPalette.wheel.size]
        val isWinner = winner == name && !spinning

        drawArc(
            color = if (isWinner) segmentColor.copy(alpha = 1f) else segmentColor.copy(alpha = 0.88f),
            startAngle = startAngle,
            sweepAngle = sweep,
            useCenter = true,
            topLeft = topLeft,
            size = arcSize,
        )

        drawArc(
            color = Color.White.copy(alpha = if (isWinner) 0.28f else 0.12f),
            startAngle = startAngle + 0.6f,
            sweepAngle = sweep - 1.2f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = radius * 0.18f),
        )

        drawIntoCanvas { canvas ->
            canvas.nativeCanvas.save()
            canvas.nativeCanvas.rotate(startAngle + sweep / 2f, wheelCenter.x, wheelCenter.y)
            canvas.nativeCanvas.drawText(
                name.shortHandle(),
                wheelCenter.x + radius * 0.57f,
                wheelCenter.y + labelPaint.textSize / 3.2f,
                labelPaint,
            )
            canvas.nativeCanvas.restore()
        }
    }

    if (winner != null) {
        val winnerIndex = names.indexOf(winner)
        if (winnerIndex >= 0) {
            val winnerAngle = rotation + segmentStartAngle(winnerIndex, sweep)
            val winnerCenterAngle = Math.toRadians((winnerAngle + sweep / 2f).toDouble())
            val glowCenter = Offset(
                x = wheelCenter.x + cos(winnerCenterAngle).toFloat() * radius * 0.54f,
                y = wheelCenter.y + sin(winnerCenterAngle).toFloat() * radius * 0.54f,
            )
            drawArc(
                color = LuckerPalette.primary.copy(alpha = 0.24f + revealProgress * 0.4f),
                startAngle = winnerAngle + 1.2f,
                sweepAngle = sweep - 2.4f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = radius * 0.22f),
                blendMode = BlendMode.Screen,
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        LuckerPalette.primary.copy(alpha = 0.38f + revealProgress * 0.32f),
                        Color.Transparent,
                    ),
                    center = glowCenter,
                    radius = radius * 0.34f,
                ),
                radius = radius * 0.34f,
                center = glowCenter,
                blendMode = BlendMode.Screen,
            )
        }
    }

    drawCircle(
        color = Color(0xE30E152F),
        radius = radius * 0.3f,
        center = wheelCenter,
    )
    drawCircle(
        color = Color.White.copy(alpha = 0.14f),
        radius = radius * 0.28f,
        center = wheelCenter,
        style = Stroke(width = radius * 0.024f),
    )
    drawCircle(
        color = Color.White.copy(alpha = 0.12f),
        radius = radius,
        center = wheelCenter,
        style = Stroke(width = radius * 0.025f),
    )
    drawCircle(
        color = LuckerPalette.primary.copy(alpha = 0.42f),
        radius = radius * 1.02f,
        center = wheelCenter,
        style = Stroke(width = radius * 0.02f),
    )
}

@Composable
private fun LavaLampOverlay(
    time: Float,
    energetic: Boolean,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val depthAlpha = if (energetic) 1f else 0.94f
        val baseGlow = if (energetic) 1.18f else 1.04f

        val blobs = 22
        repeat(blobs) { index ->
            val seed = index / blobs.toFloat()
            val spreadX = (0.13f + index * 0.61803395f).positiveMod(1f)
            val spreadY = (0.37f + index * 0.41421357f).positiveMod(1f)
            val driftX = sin(time * (0.0034f + seed * 0.0018f) + seed * 13f)
            val driftY = cos(time * (0.0026f + seed * 0.0014f) + seed * 17f)
            val orbit = sin(time * (0.0042f + seed * 0.0018f) + seed * 29f)
            val morph = 0.72f + 0.28f * sin(time * (0.0058f + seed * 0.0028f) + seed * 41f)
            val stretch = 0.76f + 0.42f * cos(time * (0.0064f + seed * 0.0021f) + seed * 33f)
            val center = Offset(
                x = size.width * (0.06f + spreadX * 0.88f) + driftX * size.width * (0.03f + seed * 0.042f),
                y = size.height * (0.08f + spreadY * 0.84f) + driftY * size.height * (0.032f + seed * 0.038f),
            )
            val radius = size.minDimension * (0.036f + (1f - seed) * 0.062f) * (0.9f + 0.14f * orbit) * baseGlow
            val blobWidth = radius * (1.3f + 0.55f * stretch)
            val blobHeight = radius * (1.0f + 0.75f * morph)
            val color = when (index % 3) {
                0 -> LuckerPalette.primary
                1 -> LuckerPalette.secondary
                else -> LuckerPalette.tertiary
            }
            val alpha = (0.12f + (1f - seed) * 0.16f) * depthAlpha
            val angle = -20f + 40f * sin(time * (0.0028f + seed * 0.0015f) + seed * 7f)
            val lobeOffset = Offset(
                x = blobWidth * 0.18f * cos(time * (0.005f + seed * 0.002f) + seed * 12f),
                y = blobHeight * 0.16f * sin(time * (0.0046f + seed * 0.0017f) + seed * 16f),
            )

            rotate(angle, pivot = center) {
                drawOval(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            color.copy(alpha = alpha * 1.8f),
                            color.copy(alpha = alpha * 0.88f),
                            color.copy(alpha = alpha * 0.24f),
                            Color.Transparent,
                        ),
                        center = center,
                        radius = maxOf(blobWidth, blobHeight) * 1.15f,
                    ),
                    topLeft = Offset(center.x - blobWidth * 0.5f, center.y - blobHeight * 0.5f),
                    size = Size(blobWidth, blobHeight),
                    blendMode = BlendMode.Screen,
                )
                drawOval(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            color.copy(alpha = alpha * 1.12f),
                            color.copy(alpha = alpha * 0.18f),
                            Color.Transparent,
                        ),
                        center = Offset(center.x + lobeOffset.x, center.y - lobeOffset.y),
                        radius = maxOf(blobWidth, blobHeight) * 0.7f,
                    ),
                    topLeft = Offset(
                        center.x + lobeOffset.x - blobWidth * 0.28f,
                        center.y - lobeOffset.y - blobHeight * 0.26f,
                    ),
                    size = Size(blobWidth * 0.56f, blobHeight * 0.52f),
                    blendMode = BlendMode.Screen,
                )
                drawCircle(
                    color = Color.White.copy(alpha = alpha * 0.42f),
                    radius = radius * 0.18f,
                    center = Offset(center.x - blobWidth * 0.14f, center.y - blobHeight * 0.12f),
                    blendMode = BlendMode.Screen,
                )
            }
            drawCircle(
                color = color.copy(alpha = alpha * 0.18f),
                radius = radius * 1.18f,
                center = center,
                blendMode = BlendMode.Screen,
            )
        }

        val capsules = 34
        repeat(capsules) { index ->
            val seed = index / capsules.toFloat()
            val spreadX = (0.21f + index * 0.7548777f).positiveMod(1f)
            val spreadY = (0.09f + index * 0.5698403f).positiveMod(1f)
            val wave = time * (0.0038f + seed * 0.0019f)
            val x = size.width * (0.05f + spreadX * 0.9f) + sin(wave + seed * 21f) * size.width * 0.03f
            val y = size.height * (0.05f + spreadY * 0.9f) + cos(wave * 1.14f + seed * 11f) * size.height * 0.05f
            val width = 12f + (1f - seed) * 20f
            val height = 22f + (1f - seed) * 40f
            val color = if (index % 2 == 0) LuckerPalette.tertiary else LuckerPalette.secondary
            val angle = -18f + sin(wave + seed * 15f) * 28f
            val alpha = (0.1f + (1f - seed) * 0.12f) * depthAlpha

            rotate(angle, pivot = Offset(x, y)) {
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            color.copy(alpha = alpha * 1.45f),
                            color.copy(alpha = alpha * 0.74f),
                            color.copy(alpha = alpha * 0.2f),
                        ),
                        startY = y,
                        endY = y + height,
                    ),
                    topLeft = Offset(x, y),
                    size = Size(width, height),
                    cornerRadius = CornerRadius(width * 0.5f, width * 0.5f),
                    blendMode = BlendMode.Screen,
                )
                drawCircle(
                    color = Color.White.copy(alpha = alpha * 0.26f),
                    radius = width * 0.14f,
                    center = Offset(x + width * 0.38f, y + height * 0.22f),
                    blendMode = BlendMode.Screen,
                )
            }
        }

        val motes = 60
        repeat(motes) { index ->
            val seed = index / motes.toFloat()
            val spreadX = (0.07f + index * 0.6803399f).positiveMod(1f)
            val spreadY = (0.41f + index * 0.533141f).positiveMod(1f)
            val wave = time * (0.0048f + seed * 0.0021f)
            val x = size.width * (0.03f + spreadX * 0.94f) + cos(wave + seed * 9f) * size.width * 0.024f
            val y = size.height * (0.04f + spreadY * 0.92f) + sin(wave * 1.2f + seed * 14f) * size.height * 0.038f
            val radius = 3.5f + (1f - seed) * 7f
            val color = when (index % 3) {
                0 -> LuckerPalette.primary
                1 -> LuckerPalette.secondary
                else -> LuckerPalette.tertiary
            }
            drawCircle(
                color = color.copy(alpha = (0.08f + (1f - seed) * 0.12f) * depthAlpha),
                radius = radius,
                center = Offset(x, y),
                blendMode = BlendMode.Screen,
            )
        }
    }
}

@Composable
private fun WinnerRevealOverlay(
    winner: String?,
    progress: Float,
    modifier: Modifier = Modifier,
) {
    if (winner == null || progress <= 0f) return

    val infinite = rememberInfiniteTransition(label = "winner_fireworks")
    val loopProgress by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3_200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "winner_fireworks_phase",
    )
    val repeatAlpha = ((progress - 0.78f) / 0.22f).coerceIn(0f, 1f)
    val textPulse = 0.96f + 0.04f * sin(loopProgress * 6.28318f)
    val textLift = (0.5f + 0.5f * sin(loopProgress * 6.28318f + 1.2f)) * 6f

    Box(modifier = modifier) {
        Canvas(modifier = Modifier.matchParentSize()) {
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color(0xA0121933).copy(alpha = progress * 0.52f),
                    ),
                    startY = size.height * 0.2f,
                    endY = size.height,
                ),
                blendMode = BlendMode.Screen,
            )

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        LuckerPalette.primary.copy(
                            alpha = 0.14f + progress * 0.18f + repeatAlpha * (0.08f + 0.06f * textPulse)
                        ),
                        Color.Transparent,
                    ),
                    center = Offset(size.width * 0.5f, size.height * 0.82f),
                    radius = size.width * (0.34f + repeatAlpha * 0.06f),
                ),
                radius = size.width * (0.34f + repeatAlpha * 0.06f),
                center = Offset(size.width * 0.5f, size.height * 0.82f),
                blendMode = BlendMode.Screen,
            )

            val specs = listOf(
                FireworkSpec(0.19f, 0.40f, 0.31f, 0.03f, 1.0f, LuckerPalette.secondary, LuckerPalette.primary),
                FireworkSpec(0.81f, 0.34f, 0.69f, 0.16f, 1.08f, LuckerPalette.tertiary, LuckerPalette.primary),
                FireworkSpec(0.5f, 0.18f, 0.5f, 0.3f, 1.18f, LuckerPalette.primary, LuckerPalette.secondary),
                FireworkSpec(0.33f, 0.26f, 0.41f, 0.42f, 0.84f, LuckerPalette.tertiary, LuckerPalette.secondary),
                FireworkSpec(0.67f, 0.24f, 0.59f, 0.5f, 0.84f, LuckerPalette.secondary, LuckerPalette.tertiary),
            )
            specs.forEach { spec ->
                drawFireworkBurst(spec = spec, progress = progress, alphaMultiplier = 1f)
            }
            if (repeatAlpha > 0f) {
                specs.forEach { spec ->
                    drawFireworkBurst(
                        spec = spec.copy(delay = (spec.delay + 0.08f) % 0.64f),
                        progress = loopProgress,
                        alphaMultiplier = 0.35f + repeatAlpha * 0.65f,
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .graphicsLayer(
                    alpha = progress,
                    scaleX = (0.92f + progress * 0.08f) * (if (repeatAlpha > 0f) textPulse else 1f),
                    scaleY = (0.92f + progress * 0.08f) * (if (repeatAlpha > 0f) textPulse else 1f),
                    translationY = (1f - progress) * 24f - if (repeatAlpha > 0f) textLift else 0f,
                )
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Badge(text = "WINNER")
            Text(
                text = winner,
                style = MaterialTheme.typography.displayMedium.copy(
                    fontSize = 30.sp,
                    lineHeight = 32.sp,
                ),
                color = LuckerPalette.textPrimary.copy(alpha = 0.74f + progress * 0.26f),
                textAlign = TextAlign.Center,
            )
        }
    }
}

private data class FireworkSpec(
    val xFactor: Float,
    val yFactor: Float,
    val launchXFactor: Float,
    val delay: Float,
    val scale: Float,
    val primary: Color,
    val secondary: Color,
)

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawFireworkBurst(
    spec: FireworkSpec,
    progress: Float,
    alphaMultiplier: Float,
) {
    val local = ((progress - spec.delay) / 0.44f).coerceIn(0f, 1f)
    if (local <= 0f) return

    val center = Offset(
        x = size.width * spec.xFactor,
        y = size.height * spec.yFactor,
    )
    val launchOrigin = Offset(
        x = size.width * spec.launchXFactor,
        y = size.height * 1.06f,
    )
    val launchPhase = (local / 0.24f).coerceIn(0f, 1f)
    val explodePhase = ((local - 0.16f) / 0.84f).coerceIn(0f, 1f)

    if (launchPhase < 1f) {
        val trailHead = Offset(
            x = launchOrigin.x + (center.x - launchOrigin.x) * launchPhase,
            y = launchOrigin.y + (center.y - launchOrigin.y) * launchPhase,
        )
        drawLine(
            color = spec.primary.copy(alpha = (0.22f + launchPhase * 0.42f) * alphaMultiplier),
            start = launchOrigin,
            end = trailHead,
            strokeWidth = 4f + spec.scale * 2.5f,
            cap = StrokeCap.Round,
            blendMode = BlendMode.Screen,
        )
        drawCircle(
            color = Color.White.copy(alpha = (0.45f + launchPhase * 0.35f) * alphaMultiplier),
            radius = 4f + spec.scale * 2f,
            center = trailHead,
            blendMode = BlendMode.Screen,
        )
    }

    if (explodePhase <= 0f) return

    val ringRadius = size.minDimension * spec.scale * (0.04f + explodePhase * 0.12f)
    val flashAlpha = (1f - explodePhase).coerceIn(0f, 1f)

    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color.White.copy(alpha = (0.22f + flashAlpha * 0.52f) * alphaMultiplier),
                spec.primary.copy(alpha = (0.16f + flashAlpha * 0.34f) * alphaMultiplier),
                Color.Transparent,
            ),
            center = center,
            radius = ringRadius * 1.8f,
        ),
        radius = ringRadius * 1.8f,
        center = center,
        blendMode = BlendMode.Screen,
    )
    drawCircle(
        color = spec.secondary.copy(alpha = (0.14f + flashAlpha * 0.28f) * alphaMultiplier),
        radius = ringRadius,
        center = center,
        style = Stroke(width = 3f + spec.scale * 2.5f),
        blendMode = BlendMode.Screen,
    )

    val sparkCount = 24
    repeat(sparkCount) { index ->
        val seed = index / sparkCount.toFloat()
        val angle = seed * 6.28318f + spec.delay * 11f
        val spread = size.minDimension * spec.scale * (0.06f + explodePhase * 0.14f) * (0.8f + 0.35f * sin(seed * 17f))
        val gravity = explodePhase * explodePhase * size.height * 0.045f * (0.3f + seed * 0.8f)
        val sparkEnd = Offset(
            x = center.x + cos(angle) * spread,
            y = center.y + sin(angle) * spread + gravity,
        )
        val sparkStart = Offset(
            x = center.x + cos(angle) * spread * 0.18f,
            y = center.y + sin(angle) * spread * 0.18f + gravity * 0.08f,
        )
        val sparkColor = if (index % 2 == 0) spec.primary else spec.secondary
        val alpha = (0.2f + (1f - explodePhase) * 0.72f) * (0.65f + (1f - seed) * 0.35f) * alphaMultiplier

        drawLine(
            color = sparkColor.copy(alpha = alpha),
            start = sparkStart,
            end = sparkEnd,
            strokeWidth = 2.5f + spec.scale * 2.2f * (1f - seed * 0.5f),
            cap = StrokeCap.Round,
            blendMode = BlendMode.Screen,
        )
        drawCircle(
            color = Color.White.copy(alpha = alpha * 0.9f),
            radius = 1.8f + spec.scale * 2.2f * (1f - seed * 0.4f),
            center = sparkEnd,
            blendMode = BlendMode.Screen,
        )
    }

    val emberCount = 12
    repeat(emberCount) { index ->
        val seed = index / emberCount.toFloat()
        val angle = seed * 6.28318f + spec.delay * 13f
        val spread = size.minDimension * spec.scale * (0.035f + explodePhase * 0.08f)
        val emberCenter = Offset(
            x = center.x + cos(angle) * spread * (0.7f + seed * 0.9f),
            y = center.y + sin(angle * 1.2f) * spread * (0.7f + seed * 0.9f) + explodePhase * explodePhase * size.height * 0.025f,
        )
        val emberColor = if (index % 2 == 0) spec.secondary else spec.primary
        drawCircle(
            color = emberColor.copy(alpha = (0.12f + (1f - explodePhase) * 0.32f) * alphaMultiplier),
            radius = 2.5f + spec.scale * 2.4f * (1f - seed * 0.45f),
            center = emberCenter,
            blendMode = BlendMode.Screen,
        )
    }
}

private suspend fun spinWheel(
    namesCount: Int,
    winnerIndex: Int,
    rotation: Animatable<Float, *>,
) {
    val segmentAngle = 360f / namesCount
    val currentNormalized = rotation.value.positiveMod(360f)
    val targetNormalized = winnerCenterRotation(winnerIndex, segmentAngle)
    val delta = (targetNormalized - currentNormalized).positiveMod(360f)
    val baseTarget = rotation.value + 360f * 8f + delta
    val overshoot = segmentAngle * 0.22f

    rotation.animateTo(
        targetValue = baseTarget + overshoot,
        animationSpec = tween(
            durationMillis = 4_300,
            easing = CubicBezierEasing(0.06f, 0.9f, 0.12f, 1f),
        ),
    )
    rotation.animateTo(
        targetValue = baseTarget - segmentAngle * 0.07f,
        animationSpec = tween(
            durationMillis = 320,
            easing = CubicBezierEasing(0.24f, 0f, 0.36f, 1f),
        ),
    )
    rotation.animateTo(
        targetValue = baseTarget,
        animationSpec = tween(
            durationMillis = 260,
            easing = CubicBezierEasing(0.2f, 0.8f, 0.24f, 1f),
        ),
    )
    rotation.snapTo(baseTarget)
}

private fun parseNicknames(rawInput: String): List<String> {
    return rawInput
        .split(Regex("[\\s,;]+"))
        .mapNotNull(::normalizeHandle)
        .distinct()
}

private fun normalizeHandle(candidate: String): String? {
    val trimmed = candidate.trim()
    if (trimmed.isBlank()) return null

    val withoutProtocol = trimmed
        .removePrefix("https://")
        .removePrefix("http://")
        .removePrefix("t.me/")
        .removePrefix("telegram.me/")
        .removePrefix("@")
        .substringBefore("/")
        .substringBefore("?")
        .trim()

    if (withoutProtocol.isBlank()) return null
    return "@$withoutProtocol"
}

private fun String.shortHandle(limit: Int = 14): String {
    return if (length <= limit) this else take(limit - 1) + "…"
}

private fun Float.positiveMod(divider: Float): Float {
    if (divider == 0f) return 0f
    return ((this % divider) + divider) % divider
}

private fun segmentStartAngle(index: Int, sweep: Float): Float {
    return index * sweep - 90f
}

private fun winnerCenterRotation(index: Int, sweep: Float): Float {
    return (360f - ((index + 0.5f) * sweep).positiveMod(360f)).positiveMod(360f)
}

private val DEFAULT_NICKNAMES = """
    @golden_guest
    @lucky_lens
    @night_signal
    @spark_follower
    @mint_hype
    @dreamy_handle
    @telegram_star
    @silent_rocket
    @afterglow_user
    @bravo_reader
    @flash_wave
    @crystal_guest
""".trimIndent()

private const val BACKDROP_SHADER = """
uniform float2 resolution;
uniform float time;

half4 main(float2 fragCoord) {
    float2 uv = fragCoord / resolution;
    float2 p = uv - 0.5;
    p.x *= resolution.x / resolution.y;

    float radius = length(p);
    float angle = atan(p.y, p.x);

    float plasma = 0.5 + 0.5 * sin((radius * 18.0) - (time * 0.025) + angle * 4.0);
    float rings = 0.5 + 0.5 * cos((radius * 28.0) + (time * 0.018));
    float glow = smoothstep(0.95, 0.12, radius);

    float3 base = float3(0.02, 0.04, 0.10);
    float3 accentA = float3(0.95, 0.42, 0.55);
    float3 accentB = float3(0.34, 0.76, 1.00);
    float3 accentC = float3(0.97, 0.80, 0.42);

    float mixA = smoothstep(1.0, 0.0, radius);
    float mixB = smoothstep(0.75, 0.15, abs(sin(angle * 2.0 + time * 0.01)));

    float3 color = base;
    color += accentA * plasma * 0.35 * glow;
    color += accentB * rings * 0.30 * glow;
    color += accentC * mixB * 0.18 * glow;
    color += float3(0.06, 0.08, 0.16) * (0.5 + 0.5 * sin(time * 0.012 + uv.xyx * float3(5.0, 7.0, 9.0)));

    return half4(color, 1.0);
}
"""
