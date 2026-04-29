package com.diplom.tuner

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.diplom.R
import com.diplom.tuner.models.NoteFrequencies
import com.diplom.tuner.models.Tuning
import com.diplom.tuner.models.Tunings
import com.diplom.tuner.ui.theme.AppColors
import com.diplom.ui.components.lighten
import java.util.Locale
import kotlin.math.abs
import kotlin.math.log2
import kotlin.math.roundToInt

@Composable
fun TunerUI(viewModel: TunerViewModel) {
    val Mono2 = Color(0xFF6C2E91)
    val Mono3 = Color(0xFF5F158A)
    val AccentGreen = Color(0xFF4CAF50)
    val GrayText = Color(0xFFAAAAAA)
    val ButtonTextColor = Color.White
    val SpeedometerWarning = Color(0xFFFFA500)
    val SpeedometerCenter = AccentGreen

    val rawNote    by viewModel.currentNote.collectAsState()
    val useFlats   by viewModel.useFlats.collectAsState()
    val cents      by viewModel.currentCents.collectAsState()
    val referenceA by viewModel.referenceA.collectAsState()
    val refFreq    by viewModel.referenceFreq.collectAsState()
    val useSolfege by viewModel.useSolfege.collectAsState()
    var tempUseSolfege by remember { mutableStateOf(useSolfege) }
    val note = remember(rawNote, useFlats) { viewModel.formatNoteForDisplay(rawNote) }

    var showDialog           by remember { mutableStateOf(false) }
    var isRunning            by remember { mutableStateOf(viewModel.isTunerRunning()) }
    var inputValue           by remember { mutableStateOf(referenceA.toInt().toString()) }
    val keyboardController   = LocalSoftwareKeyboardController.current
    var tempUseFlats         by remember { mutableStateOf(useFlats) }
    var showHelpDialog       by remember { mutableStateOf(false) }
    var showHelp             by remember { mutableStateOf(false) }
    var showTuningDialog     by remember { mutableStateOf(false) }
    var showStringIndicators by remember { mutableStateOf(false) }
    var selectedTuning       by remember { mutableStateOf(Tuning("", emptyList())) }
    var selectedStringIndex  by remember { mutableStateOf(0) }
    var stringReady          by remember { mutableStateOf(List(6) { false }) }
    var helpMessage          by remember { mutableStateOf("") }
    var tuneStableStart      by remember { mutableStateOf<Long?>(null) }
    val STABLE_DURATION_MS   = 1500L
    val buttonShape          = RoundedCornerShape(8.dp)

    // Рядки для підказок (оголошуємо тут щоб використовувати в LaunchedEffect)
    val strSelectTuning     = stringResource(R.string.tuner_select_tuning)
    val strHintTightenMuch  = stringResource(R.string.tuner_hint_tighten_much)
    val strHintLoosenMuch   = stringResource(R.string.tuner_hint_loosen_much)
    val strHintTighten      = stringResource(R.string.tuner_hint_tighten)
    val strHintLoosen       = stringResource(R.string.tuner_hint_loosen)
    val strHintTightenLittle = stringResource(R.string.tuner_hint_tighten_little)
    val strHintLoosenLittle = stringResource(R.string.tuner_hint_loosen_little)
    val strHintAlmost       = stringResource(R.string.tuner_hint_almost)
    val strHintDone         = stringResource(R.string.tuner_hint_done)
    val strHintNextString   = stringResource(R.string.tuner_hint_next_string)
    val strHintHold         = stringResource(R.string.tuner_hint_hold)
    val strErrorSelectTuning = stringResource(R.string.tuner_error_select_tuning)
    val strErrorHzRange     = stringResource(R.string.tuner_error_hz_range)
    val strErrorHzInput     = stringResource(R.string.tuner_error_hz_input)

    // Ініціалізуємо selectedTuning з локалізованою назвою
    LaunchedEffect(strSelectTuning) {
        if (selectedTuning.name.isEmpty()) {
            selectedTuning = Tuning(strSelectTuning, emptyList())
        }
    }

    val infiniteTransition = rememberInfiniteTransition()
    val blinkAlpha by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(500), RepeatMode.Reverse)
    )
    val textBlinkAlpha by rememberInfiniteTransition().animateFloat(
        initialValue = 0.4f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse)
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(colors = listOf(
                AppColors.BackgroundTop, AppColors.BackgroundBottom
            )))
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(64.dp))

            Text(
                text = stringResource(R.string.tuner_note, note),
                fontSize = 32.sp,
                color = if (abs(cents) <= 5) SpeedometerCenter else GrayText
            )
            Text(
                text = stringResource(R.string.tuner_reference, refFreq.toInt()),
                fontSize = 20.sp,
                color = GrayText
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Спідометр
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .background(Mono2.copy(alpha = 0.2f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val widthF = size.width
                    val heightF = size.height
                    val centerY = heightF / 2f
                    val totalRange = 100f
                    val mainStep = 10
                    val subSteps = 9
                    val clampedCents = cents.toFloat().coerceIn(-50f, 50f)

                    for (i in -50..50 step mainStep) {
                        val xMain = ((i + 50f) / totalRange) * widthF
                        val mainColor = if (i in -10..10) SpeedometerCenter else GrayText
                        drawLine(color = mainColor,
                            start = androidx.compose.ui.geometry.Offset(xMain, centerY - 20f),
                            end = androidx.compose.ui.geometry.Offset(xMain, centerY + 20f),
                            strokeWidth = 4f)
                        if (i < 50) {
                            val nextI = i + mainStep
                            val stepWidth = (nextI - i).toFloat() / (subSteps + 1)
                            for (sub in 1..subSteps) {
                                val xSub = ((i + 50f + sub * stepWidth) / totalRange) * widthF
                                val subColor = if ((xSub / widthF * totalRange - 50f) in -10f..10f)
                                    SpeedometerCenter else GrayText
                                drawLine(color = subColor,
                                    start = androidx.compose.ui.geometry.Offset(xSub, centerY - 10f),
                                    end = androidx.compose.ui.geometry.Offset(xSub, centerY + 10f),
                                    strokeWidth = 2f)
                            }
                        }
                    }
                    val indicatorX = ((clampedCents + 50f) / totalRange) * widthF
                    val indicatorColor = when {
                        abs(clampedCents) <= 5f -> SpeedometerCenter
                        abs(clampedCents) <= 10f -> SpeedometerWarning
                        else -> GrayText
                    }
                    val indicatorHeight = when {
                        abs(clampedCents) <= 5f -> 50f
                        abs(clampedCents) <= 10f -> 40f
                        else -> 35f
                    }
                    drawLine(color = indicatorColor,
                        start = androidx.compose.ui.geometry.Offset(indicatorX, centerY - indicatorHeight / 2f),
                        end = androidx.compose.ui.geometry.Offset(indicatorX, centerY + indicatorHeight / 2f),
                        strokeWidth = 6f)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                for (i in -50..50 step 10) {
                    Text(text = if (i > 0) "+$i" else "$i", fontSize = 16.sp, color = GrayText)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    if (isRunning) viewModel.stop().also { isRunning = false }
                    else viewModel.start().also { isRunning = true }
                },
                shape = buttonShape,
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp, pressedElevation = 2.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Mono2)
            ) {
                Text(if (isRunning) stringResource(R.string.tuner_stop)
                else stringResource(R.string.tuner_start), color = ButtonTextColor)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (showHelp) {
                        showHelp = false
                    } else {
                        showHelpDialog = true
                        selectedTuning = Tuning(strSelectTuning, emptyList())
                        inputValue = referenceA.toInt().toString()
                        showStringIndicators = false
                        helpMessage = ""
                        stringReady = emptyList()
                        selectedStringIndex = 0
                        tuneStableStart = null
                    }
                },
                shape = buttonShape,
                colors = ButtonDefaults.buttonColors(containerColor = Mono2)
            ) {
                Text(if (showHelp) stringResource(R.string.tuner_help_hide)
                else stringResource(R.string.tuner_help_button))
            }

            if (showHelpDialog) {
                Dialog(onDismissRequest = { showHelpDialog = false }) {
                    Box(
                        modifier = Modifier
                            .background(
                                Brush.verticalGradient(colors = listOf(
                                    AppColors.BackgroundTop.lighten(1.15f), AppColors.BackgroundBottom
                                )),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(16.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = stringResource(R.string.tuner_help_title),
                                color = ButtonTextColor, fontSize = 22.sp
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = buildAnnotatedString {
                                    val full = stringResource(R.string.tuner_help_text)
                                    // Виділяємо ключові фрази акцентом
                                    append(full)
                                },
                                color = Color.White, fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            Button(
                                onClick = { showHelpDialog = false; showHelp = true },
                                shape = buttonShape,
                                colors = ButtonDefaults.buttonColors(containerColor = Mono3)
                            ) { Text(stringResource(R.string.tuner_help_accept), color = ButtonTextColor) }
                        }
                    }
                }
            }

            if (showHelp) {
                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = { showTuningDialog = true },
                    shape = buttonShape,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Mono2)
                ) {
                    fun formatTuningText(name: String): String = name.replace(Regex("\\d"), "")
                    Text(formatTuningText(selectedTuning.name).ifEmpty { strSelectTuning })
                }

                if (showTuningDialog) {
                    fun formatTuningText(name: String): String = name.replace(Regex("\\d"), "")

                    AlertDialog(
                        containerColor = AppColors.Surface,
                        shape = RoundedCornerShape(20.dp),
                        onDismissRequest = { showTuningDialog = false },
                        title = { Text(stringResource(R.string.tuner_tuning_dialog_title)) },
                        text = {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth().weight(1f)
                            ) {
                                Tunings.byCategory.forEach { (category, tunings) ->
                                    item {
                                        Text(category, fontSize = 18.sp, color = Color.White)
                                        Spacer(modifier = Modifier.height(8.dp))
                                    }
                                    items(tunings) { tuning ->
                                        Surface(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp)
                                                .clickable {
                                                    selectedTuning = tuning
                                                    showTuningDialog = false
                                                    showStringIndicators = false
                                                    helpMessage = ""
                                                },
                                            shape = RoundedCornerShape(14.dp),
                                            color = AppColors.Surface,
                                            shadowElevation = 6.dp
                                        ) {
                                            Text(
                                                text = formatTuningText(tuning.name),
                                                modifier = Modifier.padding(14.dp),
                                                color = AppColors.TextPrimary,
                                                maxLines = 1
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = { showTuningDialog = false }) {
                                Text(stringResource(R.string.tuner_cancel))
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = inputValue,
                    onValueChange = { if (it.all { it.isDigit() }) inputValue = it },
                    label = { Text(stringResource(R.string.tuner_reference_label), color = Color.White) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number, imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            val entered = inputValue.toIntOrNull()
                            if (entered != null && entered in 415..455) {
                                viewModel.setReferenceA(entered.toDouble())
                                showDialog = false; helpMessage = ""; showStringIndicators = false
                            } else {
                                helpMessage = strErrorHzInput
                            }
                            keyboardController?.hide()
                        }
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = AppColors.TextPrimary,
                        unfocusedTextColor = AppColors.TextPrimary,
                        focusedBorderColor = AppColors.Primary,
                        unfocusedBorderColor = AppColors.TextSecondary,
                        cursorColor = AppColors.Primary,
                        focusedLabelColor = AppColors.Primary,
                        unfocusedLabelColor = AppColors.TextSecondary
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        val freq = inputValue.toIntOrNull()
                        when {
                            selectedTuning.strings.isEmpty() -> {
                                helpMessage = strErrorSelectTuning
                                showStringIndicators = false
                            }
                            freq == null || freq !in 415..455 -> {
                                helpMessage = strErrorHzRange
                                showStringIndicators = false
                            }
                            else -> {
                                viewModel.setReferenceA(freq.toDouble())
                                stringReady = List(selectedTuning.strings.size) { false }
                                selectedStringIndex = 0
                                showStringIndicators = true
                                helpMessage = ""
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = buttonShape,
                    colors = ButtonDefaults.buttonColors(containerColor = Mono2)
                ) { Text(stringResource(R.string.tuner_apply)) }

                Spacer(modifier = Modifier.height(16.dp))

                if (showStringIndicators &&
                    selectedTuning.strings.isNotEmpty() &&
                    inputValue.toIntOrNull() in 415..455
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        selectedTuning.strings.forEachIndexed { index, _ ->
                            val ready = stringReady.getOrElse(index) { false }
                            Surface(
                                modifier = Modifier.size(48.dp).clickable { selectedStringIndex = index },
                                shape = CircleShape,
                                color = when {
                                    ready -> AppColors.Accent
                                    index == selectedStringIndex -> AppColors.Primary.copy(alpha = blinkAlpha)
                                    else -> AppColors.Surface
                                },
                                shadowElevation = 8.dp
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        "${selectedTuning.strings.size - index}",
                                        color = if (ready) Color.White else Color.Black,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    val detectedFreq by viewModel.referenceFreq.collectAsState()

                    LaunchedEffect(detectedFreq, cents, inputValue, showStringIndicators) {
                        val freqInput = inputValue.toIntOrNull() ?: return@LaunchedEffect
                        if (freqInput !in 415..455 || detectedFreq <= 0 || !showStringIndicators) {
                            helpMessage = ""; tuneStableStart = null; return@LaunchedEffect
                        }

                        val frequencies = NoteFrequencies.getTuningFrequencies(selectedTuning, referenceA)
                        val targetFreq = frequencies[selectedStringIndex]

                        var freq = detectedFreq
                        val ratios = listOf(1.0, 2.0, 4.0, 0.5, 0.25)
                        freq = ratios.minByOrNull { ratio ->
                            kotlin.math.abs(freq * ratio - targetFreq)
                        }?.let { ratio -> detectedFreq * ratio } ?: detectedFreq

                        val deviationCents = 1200 * log2(freq / targetFreq)
                        val inTune = abs(deviationCents) <= 7.0

                        if (inTune) {
                            if (tuneStableStart == null) tuneStableStart = System.currentTimeMillis()
                        } else {
                            tuneStableStart = null
                        }

                        val stableEnough = tuneStableStart != null &&
                                (System.currentTimeMillis() - tuneStableStart!!) >= STABLE_DURATION_MS

                        if (stableEnough) {
                            stringReady = stringReady.toMutableList().apply { this[selectedStringIndex] = true }
                        }

                        if (stringReady.all { it }) {
                            helpMessage = strHintDone
                            return@LaunchedEffect
                        }

                        val isLastString = selectedStringIndex >= selectedTuning.strings.lastIndex
                        if (stringReady[selectedStringIndex] && !isLastString) {
                            selectedStringIndex += 1
                            tuneStableStart = null
                            helpMessage = strHintNextString.format(
                                selectedTuning.strings.size - selectedStringIndex
                            )
                            return@LaunchedEffect
                        }

                        val freqDiff = freq - targetFreq
                        helpMessage = when {
                            inTune -> {
                                val elapsed = tuneStableStart?.let { System.currentTimeMillis() - it } ?: 0L
                                val seconds = (STABLE_DURATION_MS - elapsed) / 1000.0
                                strHintHold.format(String.format("%.1f", seconds))
                            }
                            freqDiff < -10 -> strHintTightenMuch
                            freqDiff > 10  -> strHintLoosenMuch
                            freqDiff < -2  -> strHintTighten
                            freqDiff > 2   -> strHintLoosen
                            deviationCents < -4 -> strHintTightenLittle
                            deviationCents > 4  -> strHintLoosenLittle
                            else -> strHintAlmost
                        }
                    }
                }

                if (helpMessage.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = helpMessage,
                        color = when {
                            helpMessage == strHintDone -> Color.Green
                            helpMessage == strErrorSelectTuning -> Color.Red
                            helpMessage == strErrorHzRange || helpMessage == strErrorHzInput -> Color.Red
                            else -> Color(0xFFFFA500)
                        },
                        fontSize = 18.sp,
                        modifier = Modifier.alpha(textBlinkAlpha)
                    )
                }
            }
        }

        // Top Row A4 + Cents
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .align(Alignment.TopCenter),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            val topButtonHeight = 50.dp
            val topFontSize = 16.sp
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 36.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { tempUseFlats = useFlats; showDialog = true },
                    modifier = Modifier.weight(1f).height(topButtonHeight),
                    shape = buttonShape,
                    colors = ButtonDefaults.buttonColors(containerColor = Mono2),
                    contentPadding = PaddingValues(horizontal = 12.dp)
                ) {
                    Text("A4 = ${referenceA.toInt()} Hz", color = ButtonTextColor, fontSize = topFontSize)
                }

                Button(
                    onClick = {},
                    enabled = false,
                    modifier = Modifier.weight(1f).height(topButtonHeight),
                    shape = buttonShape,
                    colors = ButtonDefaults.buttonColors(containerColor = Mono3),
                    contentPadding = PaddingValues(horizontal = 12.dp)
                ) {
                    Text(
                        text = "Cents: ${String.format(Locale.US, "%+03d", cents.roundToInt())}",
                        color = ButtonTextColor, fontSize = topFontSize,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // A4 Dialog
        if (showDialog) {
            var dialogErrorMessage by remember { mutableStateOf("") }

            Dialog(onDismissRequest = { showDialog = false }) {
                Box(
                    modifier = Modifier
                        .background(
                            Brush.verticalGradient(colors = listOf(
                                AppColors.BackgroundTop.lighten(1.15f), AppColors.BackgroundBottom
                            )),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(16.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = stringResource(R.string.tuner_reference_label),
                            color = ButtonTextColor, fontSize = 22.sp
                        )
                        Spacer(modifier = Modifier.height(32.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("A4", color = ButtonTextColor, fontSize = 16.sp)
                            Button(
                                onClick = { tempUseFlats = !tempUseFlats },
                                shape = RoundedCornerShape(6.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Mono3)
                            ) { Text(if (tempUseFlats) "b" else "#", color = ButtonTextColor) }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Do Re Mi / C D E", color = ButtonTextColor, fontSize = 16.sp)
                            Button(
                                onClick = { tempUseSolfege = !tempUseSolfege },
                                shape = RoundedCornerShape(6.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Mono3)
                            ) {
                                Text(if (tempUseSolfege) "До Ре Ми" else "C D E", color = ButtonTextColor)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = inputValue,
                            onValueChange = {
                                if (it.all { ch -> ch.isDigit() }) {
                                    inputValue = it; dialogErrorMessage = ""; showStringIndicators = false
                                }
                            },
                            label = { Text("Hz", color = Color.White) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number, imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    val entered = inputValue.toIntOrNull()
                                    if (entered != null && entered in 415..455) {
                                        viewModel.setReferenceA(entered.toDouble())
                                        showDialog = false; dialogErrorMessage = ""
                                    } else {
                                        dialogErrorMessage = strErrorHzRange
                                    }
                                    keyboardController?.hide()
                                }
                            )
                        )

                        if (dialogErrorMessage.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(dialogErrorMessage, color = Color.Red)
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row {
                            Button(
                                shape = buttonShape,
                                colors = ButtonDefaults.buttonColors(containerColor = Mono2),
                                onClick = {
                                    val value = inputValue.toIntOrNull() ?: 415
                                    if (value > 415) inputValue = (value - 1).toString()
                                    else dialogErrorMessage = "Min 415 Hz"
                                }
                            ) { Text("-") }

                            Spacer(modifier = Modifier.width(12.dp))

                            Button(
                                shape = buttonShape,
                                colors = ButtonDefaults.buttonColors(containerColor = Mono2),
                                onClick = {
                                    val value = inputValue.toIntOrNull() ?: 440
                                    if (value < 455) inputValue = (value + 1).toString()
                                    else dialogErrorMessage = "Max 455 Hz"
                                }
                            ) { Text("+") }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row {
                            Button(
                                shape = buttonShape,
                                colors = ButtonDefaults.buttonColors(containerColor = Mono2),
                                onClick = {
                                    viewModel.setReferenceA(440.0)
                                    inputValue = "440"; dialogErrorMessage = ""; showDialog = false
                                    keyboardController?.hide()
                                }
                            ) { Text("440 Hz") }

                            Spacer(modifier = Modifier.width(12.dp))

                            Button(
                                shape = RoundedCornerShape(6.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Mono3),
                                onClick = {
                                    val entered = inputValue.toIntOrNull()
                                    if (entered != null && entered in 415..455) {
                                        viewModel.setReferenceA(entered.toDouble())
                                        viewModel.setNoteSystem(tempUseFlats)
                                        viewModel.setSolfegeSystem(tempUseSolfege)
                                        dialogErrorMessage = ""; showDialog = false
                                    } else {
                                        dialogErrorMessage = strErrorHzRange
                                    }
                                    keyboardController?.hide()
                                }
                            ) { Text(stringResource(R.string.tuner_apply)) }
                        }
                    }
                }
            }
        }
    }
}