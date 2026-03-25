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
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
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
    Slots(
        title = "Slots",
        subtitle = "Три барабана и джекпотный финиш на победителе",
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
    val density = LocalDensity.current
    val slotItemHeightPx = with(density) { 78.dp.toPx() }

    var rawNicknames by rememberSaveable { mutableStateOf(DEFAULT_NICKNAMES) }
    var modeName by rememberSaveable { mutableStateOf(PickerMode.Wheel.name) }
    var screenName by rememberSaveable { mutableStateOf(FlowScreen.Setup.name) }
    var winner by rememberSaveable { mutableStateOf<String?>(null) }
    var spinning by remember { mutableStateOf(false) }

    val mode = PickerMode.valueOf(modeName)
    val screen = FlowScreen.valueOf(screenName)
    val nicknames = remember(rawNicknames) { parseNicknames(rawNicknames) }
    val wheelRotation = remember { Animatable(0f) }
    val reelOffsets = remember { List(3) { Animatable(0f) } }

    fun launchSpin() {
        if (nicknames.isEmpty() || spinning) {
            return
        }

        coroutineScope.launch {
            spinning = true
            winner = null
            val winnerIndex = random.nextInt(nicknames.size)
            val winnerHandle = nicknames[winnerIndex]

            when (mode) {
                PickerMode.Wheel -> spinWheel(
                    namesCount = nicknames.size,
                    winnerIndex = winnerIndex,
                    rotation = wheelRotation,
                )
                PickerMode.Slots -> spinSlots(
                    namesCount = nicknames.size,
                    winnerIndex = winnerIndex,
                    itemHeightPx = slotItemHeightPx,
                    reels = reelOffsets,
                )
            }

            winner = winnerHandle
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
                    mode = mode,
                    names = nicknames,
                    winner = winner,
                    spinning = spinning,
                    wheelRotation = wheelRotation.value,
                    reelOffsets = reelOffsets.map { it.value },
                    slotItemHeightPx = slotItemHeightPx,
                    onModeChange = {
                        winner = null
                        modeName = it.name
                    },
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
    mode: PickerMode,
    names: List<String>,
    winner: String?,
    spinning: Boolean,
    wheelRotation: Float,
    reelOffsets: List<Float>,
    slotItemHeightPx: Float,
    onModeChange: (PickerMode) -> Unit,
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
            mode = mode,
            names = names,
            winner = winner,
            spinning = spinning,
            wheelRotation = wheelRotation,
            reelOffsets = reelOffsets,
            slotItemHeightPx = slotItemHeightPx,
            modifier = Modifier.weight(1f),
        )
        MinimalDrawControls(
            currentMode = mode,
            onModeChange = onModeChange,
            spinning = spinning,
            onSpin = onSpin,
            modifier = Modifier.fillMaxWidth(),
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
private fun MinimalDrawControls(
    currentMode: PickerMode,
    onModeChange: (PickerMode) -> Unit,
    spinning: Boolean,
    onSpin: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        color = LuckerPalette.panel.copy(alpha = 0.94f),
        border = androidx.compose.foundation.BorderStroke(1.dp, LuckerPalette.panelBorder),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PickerMode.entries.forEach { mode ->
                ModeChip(
                    mode = mode,
                    selected = currentMode == mode,
                    onClick = { onModeChange(mode) },
                    modifier = Modifier.weight(1f),
                )
            }

            Button(
                onClick = onSpin,
                enabled = !spinning,
                modifier = Modifier
                    .weight(1.08f)
                    .height(46.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = LuckerPalette.primary,
                    contentColor = Color(0xFF1D1405),
                    disabledContainerColor = Color(0xFF6A6151),
                    disabledContentColor = Color(0xFF211C13),
                ),
                shape = RoundedCornerShape(16.dp),
            ) {
                Text(
                    text = if (spinning) "Крутим..." else "Старт",
                    style = MaterialTheme.typography.titleLarge,
                )
            }
        }
    }
}

@Composable
private fun StageCard(
    mode: PickerMode,
    names: List<String>,
    winner: String?,
    spinning: Boolean,
    wheelRotation: Float,
    reelOffsets: List<Float>,
    slotItemHeightPx: Float,
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
            when (mode) {
                PickerMode.Wheel -> WheelStage(
                    names = names,
                    rotation = wheelRotation,
                    winner = winner,
                    spinning = spinning,
                    modifier = Modifier.fillMaxSize(),
                )
                PickerMode.Slots -> SlotsStage(
                    names = names,
                    reelOffsets = reelOffsets,
                    itemHeightPx = slotItemHeightPx,
                    winner = winner,
                    spinning = spinning,
                    modifier = Modifier.fillMaxSize(),
                )
            }
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
    val infinite = rememberInfiniteTransition()
    val time by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1_000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 34_000, easing = LinearEasing),
        ),
    )
    val pulse by infinite.animateFloat(
        initialValue = 0.78f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (isSpinning) 1_250 else 2_800,
                easing = LinearEasing,
            ),
            repeatMode = RepeatMode.Reverse,
        ),
    )

    Box(modifier = modifier.fillMaxSize()) {
        ShaderBackdrop(
            time = time,
            modifier = Modifier.matchParentSize(),
        )
        Canvas(modifier = Modifier.matchParentSize()) {
            val baseRadius = min(size.width, size.height)
            val colors = listOf(LuckerPalette.secondary, LuckerPalette.tertiary, LuckerPalette.primary)

            for (i in 0 until 60) {
                val seed = i / 60f
                val angle = time * (0.06f + seed * 0.03f) + seed * 17f
                val orbit = baseRadius * (0.15f + seed * 0.52f)
                val x = center.x + cos(angle) * orbit
                val y = center.y + sin(angle * 1.24f) * orbit * 0.58f
                val alpha = 0.06f + ((sin(time * 0.18f + seed * 28f) + 1f) * 0.5f) * 0.26f
                val radius = 2f + (((cos(time * 0.28f + seed * 31f) + 1f) * 0.5f) * 10f * pulse)
                drawCircle(
                    color = colors[i % colors.size].copy(alpha = alpha),
                    radius = radius,
                    center = Offset(x, y),
                    blendMode = BlendMode.Screen,
                )
            }

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        LuckerPalette.secondary.copy(alpha = 0.18f * pulse),
                        Color.Transparent,
                    ),
                    center = Offset(size.width * 0.78f, size.height * 0.22f),
                    radius = size.minDimension * 0.34f,
                ),
                radius = size.minDimension * 0.34f,
                center = Offset(size.width * 0.78f, size.height * 0.22f),
                blendMode = BlendMode.Screen,
            )

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        LuckerPalette.tertiary.copy(alpha = 0.16f * pulse),
                        Color.Transparent,
                    ),
                    center = Offset(size.width * 0.22f, size.height * 0.72f),
                    radius = size.minDimension * 0.4f,
                ),
                radius = size.minDimension * 0.4f,
                center = Offset(size.width * 0.22f, size.height * 0.72f),
                blendMode = BlendMode.Screen,
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 90.dp, y = (-42).dp)
                .size(280.dp)
                .blur(90.dp)
                .background(LuckerPalette.secondary.copy(alpha = 0.22f), CircleShape),
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = (-50).dp, y = 40.dp)
                .size(300.dp)
                .blur(100.dp)
                .background(LuckerPalette.tertiary.copy(alpha = 0.2f), CircleShape),
        )
    }
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
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
                ) {
            drawWheel(
                names = names,
                rotation = rotation,
                winner = winner,
                spinning = spinning,
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 14.dp),
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
            modifier = Modifier.align(Alignment.Center),
            shape = CircleShape,
            color = Color(0xD8162142),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x40FFFFFF)),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = if (spinning) "rolling" else "winner",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 12.sp,
                        letterSpacing = 1.6.sp,
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = LuckerPalette.textSecondary,
                )
                Text(
                    text = winner ?: if (names.isEmpty()) "@" else "tap spin",
                    style = MaterialTheme.typography.titleLarge,
                    color = LuckerPalette.textPrimary,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawWheel(
    names: List<String>,
    rotation: Float,
    winner: String?,
    spinning: Boolean,
) {
    val diameter = min(size.width, size.height) * 0.9f
    val radius = diameter / 2f
    val topLeft = Offset(center.x - radius, center.y - radius)
    val arcSize = Size(diameter, diameter)

    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                LuckerPalette.primary.copy(alpha = 0.28f),
                Color.Transparent,
            ),
            center = center,
            radius = radius * 1.25f,
        ),
        radius = radius * 1.25f,
        center = center,
        blendMode = BlendMode.Screen,
    )

    if (names.isEmpty()) {
        drawCircle(
            color = Color(0xFF111936),
            radius = radius,
            center = center,
        )
        drawCircle(
            color = Color(0x30FFFFFF),
            radius = radius * 0.98f,
            center = center,
            style = Stroke(width = radius * 0.03f),
        )
        return
    }

    val sweep = 360f / names.size
    val labelPaint = FrameworkPaint(FrameworkPaint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        textAlign = android.graphics.Paint.Align.CENTER
        textSize = (radius * (0.19f - (names.size.coerceAtLeast(4) / 45f))).coerceIn(18f, 34f)
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        setShadowLayer(radius * 0.04f, 0f, 0f, android.graphics.Color.argb(180, 0, 0, 0))
    }

    names.forEachIndexed { index, name ->
        val startAngle = rotation + index * sweep - 90f
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
            canvas.nativeCanvas.rotate(startAngle + sweep / 2f, center.x, center.y)
            canvas.nativeCanvas.drawText(
                name.shortHandle(),
                center.x + radius * 0.57f,
                center.y + labelPaint.textSize / 3.2f,
                labelPaint,
            )
            canvas.nativeCanvas.restore()
        }
    }

    drawCircle(
        color = Color(0xE30E152F),
        radius = radius * 0.3f,
        center = center,
    )
    drawCircle(
        color = Color.White.copy(alpha = 0.14f),
        radius = radius * 0.28f,
        center = center,
        style = Stroke(width = radius * 0.024f),
    )
    drawCircle(
        color = Color.White.copy(alpha = 0.12f),
        radius = radius,
        center = center,
        style = Stroke(width = radius * 0.025f),
    )
    drawCircle(
        color = LuckerPalette.primary.copy(alpha = 0.42f),
        radius = radius * 1.02f,
        center = center,
        style = Stroke(width = radius * 0.02f),
    )
}

@Composable
private fun SlotsStage(
    names: List<String>,
    reelOffsets: List<Float>,
    itemHeightPx: Float,
    winner: String?,
    spinning: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 18.dp),
        ) {
            drawSlots(
                names = names,
                reelOffsets = reelOffsets,
                itemHeightPx = itemHeightPx,
                winner = winner,
                spinning = spinning,
            )
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSlots(
    names: List<String>,
    reelOffsets: List<Float>,
    itemHeightPx: Float,
    winner: String?,
    spinning: Boolean,
) {
    if (names.isEmpty()) {
        drawRoundRect(
            color = Color(0xFF111936),
            topLeft = Offset.Zero,
            size = size,
            cornerRadius = CornerRadius(42f, 42f),
        )
        return
    }

    val reelGap = size.width * 0.03f
    val reelWidth = (size.width - reelGap * 2f) / 3f
    val itemHeight = itemHeightPx.coerceAtMost(size.height * 0.22f)
    val reelHeight = itemHeight * 3f
    val reelTop = center.y - reelHeight / 2f

    drawRoundRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color(0x22192043),
                Color(0x552B1739),
                Color(0x220E1C39),
            ),
        ),
        topLeft = Offset(0f, reelTop - 26f),
        size = Size(size.width, reelHeight + 52f),
        cornerRadius = CornerRadius(42f, 42f),
    )

    repeat(3) { reelIndex ->
        val left = reelIndex * (reelWidth + reelGap)
        drawSlotReel(
            names = names,
            left = left,
            top = reelTop,
            width = reelWidth,
            height = reelHeight,
            offset = reelOffsets.getOrElse(reelIndex) { 0f },
            winner = winner,
            spinning = spinning,
        )
    }

    drawRoundRect(
        color = Color.White.copy(alpha = if (winner != null && !spinning) 0.2f else 0.12f),
        topLeft = Offset(0f, center.y - itemHeight / 2f),
        size = Size(size.width, itemHeight),
        cornerRadius = CornerRadius(30f, 30f),
        style = Stroke(width = 5f),
    )

    drawRoundRect(
        brush = Brush.horizontalGradient(
            colors = listOf(
                Color.Transparent,
                LuckerPalette.primary.copy(alpha = 0.16f),
                LuckerPalette.secondary.copy(alpha = 0.18f),
                LuckerPalette.primary.copy(alpha = 0.16f),
                Color.Transparent,
            ),
        ),
        topLeft = Offset(0f, center.y - itemHeight / 2f),
        size = Size(size.width, itemHeight),
        cornerRadius = CornerRadius(30f, 30f),
        blendMode = BlendMode.Screen,
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSlotReel(
    names: List<String>,
    left: Float,
    top: Float,
    width: Float,
    height: Float,
    offset: Float,
    winner: String?,
    spinning: Boolean,
) {
    val itemHeight = height / 3f
    val centerY = top + height / 2f
    val baseIndex = floor(offset / itemHeight).toInt()
    val corner = CornerRadius(30f, 30f)

    drawRoundRect(
        color = Color(0xFF0E1630),
        topLeft = Offset(left, top),
        size = Size(width, height),
        cornerRadius = corner,
    )

    clipRect(
        left = left,
        top = top,
        right = left + width,
        bottom = top + height,
    ) {
        val paint = FrameworkPaint(FrameworkPaint.ANTI_ALIAS_FLAG).apply {
            textAlign = android.graphics.Paint.Align.CENTER
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }

        for (virtualIndex in (baseIndex - 12)..(baseIndex + 12)) {
            val yCenter = centerY + virtualIndex * itemHeight - offset
            if (yCenter < top - itemHeight || yCenter > top + height + itemHeight) {
                continue
            }

            val name = names.positiveAt(virtualIndex)
            val distance = (abs(yCenter - centerY) / (height / 2f)).coerceIn(0f, 1f)
            val alpha = (1f - distance * 0.8f).coerceIn(0.12f, 1f)
            val textSize = itemHeight * (0.32f - distance * 0.08f)
            val glow = winner == name && !spinning && distance < 0.1f

            paint.textSize = textSize
            paint.color = if (glow) {
                android.graphics.Color.rgb(245, 198, 107)
            } else {
                android.graphics.Color.argb(
                    (alpha * 255).toInt(),
                    255,
                    255,
                    255,
                )
            }
            paint.setShadowLayer(
                if (glow) 24f else 10f,
                0f,
                0f,
                if (glow) android.graphics.Color.argb(180, 245, 198, 107) else android.graphics.Color.argb(90, 0, 0, 0),
            )

            drawIntoCanvas { canvas ->
                canvas.nativeCanvas.drawText(
                    name.shortHandle(limit = 16),
                    left + width / 2f,
                    yCenter + paint.textSize / 3.2f,
                    paint,
                )
            }
        }
    }

    drawRoundRect(
        color = Color.White.copy(alpha = 0.11f),
        topLeft = Offset(left, top),
        size = Size(width, height),
        cornerRadius = corner,
        style = Stroke(width = 4f),
    )
}

private suspend fun spinWheel(
    namesCount: Int,
    winnerIndex: Int,
    rotation: Animatable<Float, *>,
) {
    val segmentAngle = 360f / namesCount
    val currentNormalized = rotation.value.positiveMod(360f)
    val targetNormalized = (360f - ((winnerIndex + 0.5f) * segmentAngle).positiveMod(360f)).positiveMod(360f)
    val delta = (targetNormalized - currentNormalized).positiveMod(360f)

    rotation.animateTo(
        targetValue = rotation.value + 360f * 8f + delta,
        animationSpec = tween(
            durationMillis = 4_600,
            easing = CubicBezierEasing(0.08f, 0.92f, 0.16f, 1f),
        ),
    )
}

private suspend fun spinSlots(
    namesCount: Int,
    winnerIndex: Int,
    itemHeightPx: Float,
    reels: List<Animatable<Float, *>>,
) {
    coroutineScope {
        reels.mapIndexed { index, reel ->
            launch {
                val extraTurns = namesCount * (16 + index * 4)
                val targetIndex = winnerIndex + extraTurns
                reel.animateTo(
                    targetValue = targetIndex * itemHeightPx,
                    animationSpec = tween(
                        durationMillis = 2_800 + index * 550,
                        easing = CubicBezierEasing(0.1f, 0.9f, 0.16f, 1f),
                    ),
                )
            }
        }.joinAll()
    }
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

private fun List<String>.positiveAt(index: Int): String {
    return this[index.positiveMod(size)]
}

private fun Int.positiveMod(divider: Int): Int {
    if (divider == 0) return 0
    return ((this % divider) + divider) % divider
}

private fun Float.positiveMod(divider: Float): Float {
    if (divider == 0f) return 0f
    return ((this % divider) + divider) % divider
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
