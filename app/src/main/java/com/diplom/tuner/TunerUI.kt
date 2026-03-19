        
        package com.diplom.tuner

        import androidx.compose.animation.core.*
        import androidx.compose.foundation.Canvas
        import androidx.compose.foundation.background
        import androidx.compose.foundation.clickable
        import androidx.compose.foundation.layout.*
        import androidx.compose.foundation.shape.CircleShape
        import androidx.compose.foundation.shape.RoundedCornerShape
        import androidx.compose.foundation.text.KeyboardActions
        import androidx.compose.foundation.text.KeyboardOptions
        import androidx.compose.foundation.layout.*
        import androidx.compose.foundation.lazy.LazyColumn
        import androidx.compose.foundation.lazy.items
        import androidx.compose.foundation.lazy.itemsIndexed
        import androidx.compose.material3.*
        import androidx.compose.runtime.*
        import androidx.compose.ui.Alignment
        import androidx.compose.ui.Modifier
        import androidx.compose.ui.graphics.Color
        import androidx.compose.ui.platform.LocalSoftwareKeyboardController
        import androidx.compose.ui.text.font.FontFamily
        import androidx.compose.ui.text.input.ImeAction
        import androidx.compose.ui.text.input.KeyboardType
        import androidx.compose.ui.unit.dp
        import androidx.compose.ui.unit.sp
        import androidx.compose.ui.draw.alpha
        import com.diplom.tuner.models.Tunings
        import com.diplom.tuner.models.NoteFrequencies
        import com.diplom.tuner.models.Tuning
        import kotlin.math.abs
        import kotlin.math.roundToInt
        import java.util.Locale
        import androidx.compose.ui.text.AnnotatedString
        import androidx.compose.ui.text.buildAnnotatedString
        import androidx.compose.ui.text.withStyle
        import androidx.compose.ui.text.SpanStyle
        import androidx.compose.ui.unit.sp
        import androidx.compose.ui.graphics.Brush
        import androidx.compose.ui.text.style.TextAlign
        import com.diplom.tuner.ui.theme.AppColors
        import kotlin.math.log2
        import kotlin.math.abs
        import com.diplom.ui.components.lighten
        import androidx.compose.ui.window.Dialog
        import androidx.compose.ui.text.font.FontWeight

        
        fun formatTuningText(text: String): AnnotatedString {
        
            return buildAnnotatedString {
        
                text.forEach { char ->
        
                    if (char == '#' || char == 'b') {
        
                        withStyle(
                            SpanStyle(
                                fontSize = 13.sp
                            )
                        ) {
                            append(char)
                        }
        
                    } else {
        
                        withStyle(
                            SpanStyle(
                                fontSize = 13.sp
                            )
                        ) {
                            append(char)
                        }
        
                    }
        
                }
            }
        }
        
        @Composable
        fun TunerUI(viewModel: TunerViewModel) {
            // ================= Colors =================
            val Background = Color(0xFF450769)
            val Mono2 = Color(0xFF6C2E91)
            val Mono3 = Color(0xFF5F158A)
            val AccentGreen = Color(0xFF4CAF50)
            val GrayText = Color(0xFFAAAAAA)
            val ButtonTextColor = Color.White
            val SpeedometerWarning = Color(0xFFFFA500)
            val SpeedometerCenter = AccentGreen
        
            // ================= State =================
            val rawNote by viewModel.currentNote.collectAsState()
            val useFlats by viewModel.useFlats.collectAsState()
            val cents by viewModel.currentCents.collectAsState()
            val referenceA by viewModel.referenceA.collectAsState()
            val refFreq by viewModel.referenceFreq.collectAsState()
            val useSolfege by viewModel.useSolfege.collectAsState()
            var tempUseSolfege by remember { mutableStateOf(useSolfege) }
            val note = remember(rawNote, useFlats) { viewModel.formatNoteForDisplay(rawNote) }
        
            var showDialog by remember { mutableStateOf(false) }
            var limitMessage by remember { mutableStateOf("") }
            var isRunning by remember { mutableStateOf(viewModel.isTunerRunning()) }
            var inputValue by remember { mutableStateOf(referenceA.toInt().toString()) }
            val keyboardController = LocalSoftwareKeyboardController.current
            var tempUseFlats by remember { mutableStateOf(useFlats) }
        
            // ================= Help State =================
            var showHelp by remember { mutableStateOf(false) }
            var showTuningDialog by remember { mutableStateOf(false) }
            var showStringIndicators by remember { mutableStateOf(false) }
            var selectedTuning by remember { mutableStateOf(Tuning("Выбрать строй", emptyList())) }
            var selectedStringIndex by remember { mutableStateOf(0) }
            var stringReady by remember { mutableStateOf(List(6) { false }) }
            var helpMessage by remember { mutableStateOf("") }
            var enteredFreq by remember { mutableStateOf(referenceA.toInt().toString()) }
            var tuneStableStart by remember { mutableStateOf<Long?>(null) }
            val STABLE_DURATION_MS = 1000L
        
            val buttonShape = RoundedCornerShape(8.dp)
            val buttonHeight = 48.dp
            val buttonWidth = 170.dp
        
            // ================= Infinite Transition для мигания =================
            val infiniteTransition = rememberInfiniteTransition()
            val blinkAlpha by infiniteTransition.animateFloat(
                initialValue = 1f,
                targetValue = 0f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 500),
                    repeatMode = RepeatMode.Reverse
                )
            )
        
            val textBlinkAlpha by rememberInfiniteTransition().animateFloat(
                initialValue = 0.4f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(700),
                    repeatMode = RepeatMode.Reverse
                )
            )
        
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                AppColors.BackgroundTop,
                                AppColors.BackgroundBottom
                            )
                        )
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(64.dp))
        
                    // --- Note info ---
                    Text(
                        text = "Нота: $note",
                        fontSize = 32.sp,
                        color = if (abs(cents) <= 5) SpeedometerCenter else GrayText
                    )
                    Text(
                        text = "Эталон: ${refFreq.toInt()} Hz",
                        fontSize = 20.sp,
                        color = GrayText
                    )
        
                    Spacer(modifier = Modifier.height(24.dp))
        
                    // --- Speedometer ---
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
                                val mainHeight = 40f
                                drawLine(
                                    color = mainColor,
                                    start = androidx.compose.ui.geometry.Offset(xMain, centerY - mainHeight / 2f),
                                    end = androidx.compose.ui.geometry.Offset(xMain, centerY + mainHeight / 2f),
                                    strokeWidth = 4f
                                )
                                if (i < 50) {
                                    val nextI = i + mainStep
                                    val stepWidth = (nextI - i).toFloat() / (subSteps + 1)
                                    for (sub in 1..subSteps) {
                                        val xSub = ((i + 50f + sub * stepWidth) / totalRange) * widthF
                                        val subColor =
                                            if ((xSub / widthF * totalRange - 50f) in -10f..10f) SpeedometerCenter else GrayText
                                        val subHeight = 20f
                                        drawLine(
                                            color = subColor,
                                            start = androidx.compose.ui.geometry.Offset(xSub, centerY - subHeight / 2f),
                                            end = androidx.compose.ui.geometry.Offset(xSub, centerY + subHeight / 2f),
                                            strokeWidth = 2f
                                        )
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
                            drawLine(
                                color = indicatorColor,
                                start = androidx.compose.ui.geometry.Offset(indicatorX, centerY - indicatorHeight / 2f),
                                end = androidx.compose.ui.geometry.Offset(indicatorX, centerY + indicatorHeight / 2f),
                                strokeWidth = 6f
                            )
                        }
                    }
        
                    Spacer(modifier = Modifier.height(16.dp))
        
                    // --- Scale labels ---
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        for (i in -50..50 step 10) {
                            Text(text = if (i > 0) "+$i" else "$i", fontSize = 16.sp, color = GrayText)
                        }
                    }
        
                    Spacer(modifier = Modifier.height(32.dp))
        
                    // --- Start/Stop Button ---
                    Button(
                        onClick = {
                            if (isRunning) viewModel.stop().also { isRunning = false }
                            else viewModel.start().also { isRunning = true }
                        },
                        shape = buttonShape,
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 6.dp,
                            pressedElevation = 2.dp
                        ),
                        colors = ButtonDefaults.buttonColors(containerColor = Mono2)
                    ) {
                        Text(if (isRunning) "Стоп" else "Старт", color = ButtonTextColor)
                    }
        
                    Spacer(modifier = Modifier.height(16.dp))
        
                    // --- Help toggle ---
                    Button(onClick = {
                        showHelp = !showHelp
                        if (showHelp) {
                            selectedTuning = Tuning("Выбрать строй", emptyList())
                            enteredFreq = referenceA.toInt().toString()
                            helpMessage = ""
                            showStringIndicators = false
                        } else {
                            helpMessage = ""
                            showStringIndicators = false
                        }
                    }, shape = buttonShape, colors = ButtonDefaults.buttonColors(containerColor = Mono2),) {
                        Text("Помощь в настройке")
                    }
    
                    // --- Help Panel ---
                    if (showHelp) {
                        Spacer(modifier = Modifier.height(12.dp))
    
                        // --- Tuning selection ---
                        Button(
                            onClick = { showTuningDialog = true },
                            shape = buttonShape,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Mono2)
                        ) {
                            // форматируем текст прямо на кнопке
                            fun formatTuningText(name: String): String = name.replace(Regex("\\d"), "")
                            Text(formatTuningText(selectedTuning.name))
                        }
    
                        if (showTuningDialog) {
    
                            // --- функция форматирования: убирает октавы ---
                            fun formatTuningText(name: String): String {
                                return name.replace(Regex("\\d"), "")
                            }
    
                            AlertDialog(
                                containerColor = AppColors.Surface,
                                shape = RoundedCornerShape(20.dp),
                                onDismissRequest = { showTuningDialog = false },
                                title = { Text("Выберите строй") },
                                text = {
                                    LazyColumn(
                                        verticalArrangement = Arrangement.spacedBy(8.dp), // меньше расстояние между кнопками
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .weight(1f) // занимает доступное пространство
                                    ) {
                                        Tunings.byCategory.forEach { (category, tunings) ->
                                            // Заголовок категории
                                            item {
                                                Text(category, fontSize = 18.sp, color = Color.White)
                                                Spacer(modifier = Modifier.height(8.dp))
                                            }
    
                                            // Кнопки строев
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
                                                        text = formatTuningText(tuning.name), // <-- здесь убираем октавы
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
                                        Text("Отмена")
                                    }
                                }
                            )
                        }
        
                        Spacer(modifier = Modifier.height(12.dp))
        
                        // --- Frequency input ---
                        OutlinedTextField(
                            value = inputValue,
                            onValueChange = { if (it.all { it.isDigit() }) inputValue = it },
                            label = { Text("Эталон для ноты А4 (Hz)", color = Color.White) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    val entered = inputValue.toIntOrNull()
                                    if (entered != null && entered in 415..455)
                                        viewModel.setReferenceA(entered.toDouble()).also {
                                            showDialog = false
                                            limitMessage = ""
                                        }
                                    else
                                        limitMessage = "Введите значение от 415 до 455 Hz"
        
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
        
                        // --- Apply Tuning ---
                        Button(
                            onClick = {
        
                                val freq = inputValue.toIntOrNull()
        
                                when {
        
                                    selectedTuning.strings.isEmpty() -> {
                                        helpMessage = "Выберите строй!"
                                        showStringIndicators = false
                                    }
        
                                    freq == null || freq !in 415..455 -> {
                                        helpMessage = "Введите значение от 415 до 455 Hz"
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
                            colors = ButtonDefaults.buttonColors(containerColor = Mono2),
                        ) {
                            Text("Применить")
                        }
        
                        Spacer(modifier = Modifier.height(16.dp))
        
                        // --- Strings indicators ---
        
                        if (
                            showStringIndicators &&
                            selectedTuning.strings.isNotEmpty() &&
                            inputValue.toIntOrNull() in 415..455
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                selectedTuning.strings.forEachIndexed { index, _ ->
                                    val ready = stringReady.getOrElse(index) { false }
                                    Surface(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clickable { selectedStringIndex = index },
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
                                    helpMessage = ""
                                    tuneStableStart = null
                                    return@LaunchedEffect
                                }

                                val frequencies = NoteFrequencies.getTuningFrequencies(selectedTuning, referenceA)
                                val targetFreq = frequencies[selectedStringIndex]
                                val targetNote = viewModel.formatTuningNote(selectedTuning.strings[selectedStringIndex])
                                val deviationCents = 1200 * log2(detectedFreq / targetFreq)
                                val inTune = abs(deviationCents) <= 7.0

                                // Таймер стабильности
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

                                // Все настроены?
                                if (stringReady.all { it }) {
                                    helpMessage = "Гитара полностью настроена!"
                                    return@LaunchedEffect
                                }

                                // Автопереход
                                val isLastString = selectedStringIndex >= selectedTuning.strings.lastIndex
                                if (stringReady[selectedStringIndex] && !isLastString) {
                                    selectedStringIndex += 1
                                    tuneStableStart = null
                                    helpMessage = "Отлично! Настраиваем струну ${selectedTuning.strings.size - selectedStringIndex}"
                                    return@LaunchedEffect
                                }

                                // Подсказки только по частоте и центам
                                val freqDiff = detectedFreq - targetFreq
                                helpMessage = when {
                                    inTune -> {
                                        val elapsed = tuneStableStart?.let { System.currentTimeMillis() - it } ?: 0L
                                        val seconds = (STABLE_DURATION_MS - elapsed) / 1000.0
                                        "Держи… ещё ${String.format("%.1f", seconds)} сек"
                                    }
                                    freqDiff < -10 -> "Подтянуть: ${detectedFreq.toInt()} Гц → ${targetFreq.toInt()} Гц"
                                    freqDiff > 10  -> "Ослабить: ${detectedFreq.toInt()} Гц → ${targetFreq.toInt()} Гц"
                                    freqDiff < -2  -> "Подтянуть $targetNote (${detectedFreq.toInt()} → ${targetFreq.toInt()} Гц)"
                                    freqDiff > 2   -> "Ослабить $targetNote (${detectedFreq.toInt()} → ${targetFreq.toInt()} Гц)"
                                    deviationCents < -4 -> "Чуть подтянуть (${String.format("%+.0f", deviationCents)}¢)"
                                    deviationCents > 4  -> "Чуть ослабить (${String.format("%+.0f", deviationCents)}¢)"
                                    else -> "Почти идеально… чуть подстрой"
                                }
                            }
                        }
        
                        if (helpMessage.isNotEmpty()) {
        
                            Spacer(modifier = Modifier.height(12.dp))
        
                            Text(
                                text = helpMessage,
                                color = when {
                                    helpMessage == "Готово" -> Color.Green
                                    helpMessage.contains("Введите") -> Color.Red
                                    helpMessage.contains("Выберите") -> Color.Red
                                    else -> Color(0xFFFFA500) // предупреждение (подтянуть / опустить)
                                },
                                fontSize = 18.sp,
                                modifier = Modifier.alpha(textBlinkAlpha)
                            )
                        }
                    }
                }
    
                // ================= Top Row A4 + Cents =================
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .align(Alignment.TopCenter),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(16.dp))
                    val topButtonHeight = 50.dp
                    val topButtonWidth = 160.dp
                    val topFontSize = 16.sp
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 36.dp), // 👈 ВОТ ЭТО КЛЮЧ
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
    
                        Button(
                            onClick = { tempUseFlats = useFlats; showDialog = true },
                            modifier = Modifier
                                .weight(1f)
                                .height(topButtonHeight),
                            shape = buttonShape,
                            colors = ButtonDefaults.buttonColors(containerColor = Mono2),
                            contentPadding = PaddingValues(horizontal = 12.dp)
                        ) {
                            Text(
                                "A4 = ${referenceA.toInt()} Hz",
                                color = ButtonTextColor,
                                fontSize = topFontSize
                            )
                        }
    
                        Button(
                            onClick = {},
                            enabled = false,
                            modifier = Modifier
                                .weight(1f)
                                .height(topButtonHeight),
                            shape = buttonShape,
                            colors = ButtonDefaults.buttonColors(containerColor = Mono3),
                            contentPadding = PaddingValues(horizontal = 12.dp)
                        ) {
                            Text(
                                text = "Cents: ${String.format(Locale.US, "%+03d", cents.roundToInt())}",
                                color = ButtonTextColor,
                                fontSize = topFontSize,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
                // ================= A4 Dialog =================
                if (showDialog) {

                    var dialogErrorMessage by remember { mutableStateOf("") }

                    Dialog(
                        onDismissRequest = { showDialog = false }
                    ) {
                        Box(
                            modifier = Modifier
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            AppColors.BackgroundTop.lighten(1.15f),
                                            AppColors.BackgroundBottom
                                        )
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .padding(16.dp)
                        ) {

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {


                                    Text(
                                        text = "Эталонная частота A4",
                                        color = ButtonTextColor,
                                        fontSize = 22.sp,
                                    )

                                Spacer(modifier = Modifier.height(32.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Система нот", color = ButtonTextColor, fontSize = 16.sp)

                                    Button(
                                        onClick = { tempUseFlats = !tempUseFlats },
                                        shape = RoundedCornerShape(6.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Mono3)
                                    ) {
                                        Text(if (tempUseFlats) "b" else "#", color = ButtonTextColor)
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Формат нот", color = ButtonTextColor, fontSize = 16.sp)

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
                                            inputValue = it
                                            dialogErrorMessage = ""
                                            showStringIndicators = false
                                        }
                                    },
                                    label = { Text("Частота (Hz)", color = Color.White) },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Number,
                                        imeAction = ImeAction.Done
                                    ),
                                    keyboardActions = KeyboardActions(
                                        onDone = {
                                            val entered = inputValue.toIntOrNull()

                                            if (entered != null && entered in 415..455) {
                                                viewModel.setReferenceA(entered.toDouble())
                                                showDialog = false
                                                dialogErrorMessage = ""
                                            } else {
                                                dialogErrorMessage = "Введите значение от 415 до 455 Hz"
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
                                        if (value > 415) {
                                            inputValue = (value - 1).toString()
                                        } else {
                                            dialogErrorMessage = "Минимум 415 Hz"
                                        }
                                    }) {
                                        Text("-")
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Button(
                                        shape = buttonShape,
                                        colors = ButtonDefaults.buttonColors(containerColor = Mono2),
                                        onClick = {
                                        val value = inputValue.toIntOrNull() ?: 440
                                        if (value < 455) {
                                            inputValue = (value + 1).toString()
                                        } else {
                                            dialogErrorMessage = "Максимум 455 Hz"
                                        }
                                    }) {
                                        Text("+")
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Row {

                                    Button(
                                        shape = buttonShape,
                                        colors = ButtonDefaults.buttonColors(containerColor = Mono2),
                                        onClick = {

                                        viewModel.setReferenceA(440.0)
                                        inputValue = "440"
                                        dialogErrorMessage = ""
                                        showDialog = false
                                        keyboardController?.hide()
                                    }) {
                                        Text("440 Hz")
                                    }

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

                                            dialogErrorMessage = ""
                                            showDialog = false
                                        } else {
                                            dialogErrorMessage = "Введите значение от 415 до 455 Hz"
                                        }

                                        keyboardController?.hide()
                                    }) {
                                        Text("Применить")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }