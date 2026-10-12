package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.remote.ApiResult
import com.example.ui.theme.AmberGlow
import com.example.ui.theme.AmberWarm
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.WarningAmber
import kotlinx.coroutines.launch

enum class SetupStep(val stepNumber: Int, val title: String) {
    WELCOME(1, "Willkommen"),
    SERVER(2, "Server"),
    TOKEN(3, "API-Token"),
    TEST_AND_FINISH(4, "Verbindung prüfen")
}

@Composable
fun SetupWizardScreen(
    onTestConnection: suspend (baseUrl: String, token: String) -> ApiResult<String>,
    onCompleteSetup: (baseUrl: String, token: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    var currentStep by remember { mutableStateOf(SetupStep.WELCOME) }

    var baseUrlInput by remember { mutableStateOf("http://192.168.1.100:3000") }
    var tokenInput by remember { mutableStateOf("") }
    var tokenVisible by remember { mutableStateOf(false) }

    var isTesting by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<ApiResult<String>?>(null) }

    // Validation for Step 2: URL format
    val urlError = remember(baseUrlInput) {
        val trimmed = baseUrlInput.trim()
        when {
            trimmed.isEmpty() -> "Bitte gib eine Serveradresse ein."
            !trimmed.startsWith("http://", ignoreCase = true) && !trimmed.startsWith("https://", ignoreCase = true) ->
                "Die Adresse muss mit http:// oder https:// beginnen."
            trimmed.length < 10 || !trimmed.contains(".") && !trimmed.contains("localhost") ->
                "Bitte gib einen gültigen Hostnamen oder eine IP-Adresse ein."
            else -> null
        }
    }

    // Validation for Step 3: Token
    val tokenError = remember(tokenInput) {
        if (tokenInput.trim().isEmpty()) "Das API-Token darf nicht leer sein." else null
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 24.dp)
        ) {
            // Header: Step Progress Indicator
            StepProgressBar(currentStep = currentStep)

            Spacer(modifier = Modifier.height(16.dp))

            // Body: Step content with smooth horizontal slide animation
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                AnimatedContent(
                    targetState = currentStep,
                    transitionSpec = {
                        if (targetState.stepNumber > initialState.stepNumber) {
                            (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                                slideOutHorizontally { width -> -width } + fadeOut()
                            )
                        } else {
                            (slideInHorizontally { width -> -width } + fadeIn()).togetherWith(
                                slideOutHorizontally { width -> width } + fadeOut()
                            )
                        }
                    },
                    label = "setupStepTransition"
                ) { step ->
                    when (step) {
                        SetupStep.WELCOME -> {
                            WelcomeStep(
                                onStart = { currentStep = SetupStep.SERVER }
                            )
                        }
                        SetupStep.SERVER -> {
                            ServerConfigStep(
                                baseUrl = baseUrlInput,
                                onBaseUrlChange = { baseUrlInput = it },
                                errorMessage = urlError,
                                onNext = {
                                    if (urlError == null) {
                                        currentStep = SetupStep.TOKEN
                                    }
                                },
                                onBack = { currentStep = SetupStep.WELCOME }
                            )
                        }
                        SetupStep.TOKEN -> {
                            TokenInputStep(
                                token = tokenInput,
                                onTokenChange = { tokenInput = it },
                                tokenVisible = tokenVisible,
                                onToggleVisibility = { tokenVisible = !tokenVisible },
                                errorMessage = tokenError,
                                onNext = {
                                    if (tokenError == null) {
                                        testResult = null
                                        currentStep = SetupStep.TEST_AND_FINISH
                                    }
                                },
                                onBack = { currentStep = SetupStep.SERVER }
                            )
                        }
                        SetupStep.TEST_AND_FINISH -> {
                            TestAndFinishStep(
                                baseUrl = baseUrlInput.trim(),
                                isTesting = isTesting,
                                testResult = testResult,
                                onRunTest = {
                                    if (!isTesting) {
                                        isTesting = true
                                        testResult = null
                                        coroutineScope.launch {
                                            val res = onTestConnection(baseUrlInput.trim(), tokenInput.trim())
                                            testResult = res
                                            isTesting = false
                                        }
                                    }
                                },
                                onComplete = {
                                    onCompleteSetup(baseUrlInput.trim(), tokenInput.trim())
                                },
                                onBack = { currentStep = SetupStep.TOKEN }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StepProgressBar(currentStep: SetupStep) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        SetupStep.values().forEachIndexed { index, step ->
            val isActive = step.stepNumber <= currentStep.stepNumber
            val isCurrent = step == currentStep

            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            isCurrent -> AmberWarm
                            isActive -> AmberWarm.copy(alpha = 0.6f)
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (step.stepNumber < currentStep.stepNumber) {
                    Icon(
                        imageVector = Icons.Outlined.Check,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(16.dp)
                    )
                } else {
                    Text(
                        text = "${step.stepNumber}",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (isCurrent) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (index < SetupStep.values().size - 1) {
                Box(
                    modifier = Modifier
                        .width(36.dp)
                        .height(3.dp)
                        .background(
                            if (step.stepNumber < currentStep.stepNumber) AmberWarm else MaterialTheme.colorScheme.surfaceVariant
                        )
                )
            }
        }
    }
}

@Composable
private fun WelcomeStep(onStart: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(AmberWarm.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Lightbulb,
                contentDescription = "WiZ Home",
                tint = AmberGlow,
                modifier = Modifier.size(54.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Willkommen bei WiZ Home",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "Zur Steuerung deiner smarten WiZ-Beleuchtung benötigt die App eine Verbindung zu deinem WiZ Light Controller Server.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(28.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = AmberWarm,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Voraussetzungen",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "• Eine erreichbare Server-URL deines WiZ REST-APIs im Heimnetzwerk (LAN).\n• Ein gültiges Bearer-Token mit entsprechenden Berechtigungen (z. B. devices.read, devices.control).",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(36.dp))

        Button(
            onClick = onStart,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("wizard_start_button"),
            colors = ButtonDefaults.buttonColors(containerColor = AmberWarm, contentColor = Color.Black),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text("Einrichtung starten", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            Spacer(modifier = Modifier.width(8.dp))
            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun ServerConfigStep(
    baseUrl: String,
    onBaseUrlChange: (String) -> Unit,
    errorMessage: String?,
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = "Server konfigurieren",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Gib die Netzwerkadresse deines Node.js WiZ Light Controller Backends an.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = baseUrl,
                onValueChange = onBaseUrlChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("wizard_base_url_input"),
                label = { Text("API-Basis-URL") },
                placeholder = { Text("http://192.168.1.100:3000") },
                leadingIcon = {
                    Icon(imageVector = Icons.Outlined.Dns, contentDescription = null)
                },
                isError = errorMessage != null,
                supportingText = {
                    if (errorMessage != null) {
                        Text(text = errorMessage, color = ErrorRed)
                    } else {
                        Text("Beispiel: http://192.168.1.10:3000 (Protokoll, Host und Port)")
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AmberWarm
                )
            )

            Spacer(modifier = Modifier.height(18.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Hinweis: Der Server muss aus dem WLAN deines Smartphones erreichbar sein. Stelle sicher, dass die Firewall deines Servers Port 3000 (oder den gewählten Port) nicht blockiert.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Zurück")
            }

            Button(
                onClick = onNext,
                enabled = errorMessage == null,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("wizard_server_next_button"),
                colors = ButtonDefaults.buttonColors(containerColor = AmberWarm, contentColor = Color.Black),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Weiter")
            }
        }
    }
}

@Composable
private fun TokenInputStep(
    token: String,
    onTokenChange: (String) -> Unit,
    tokenVisible: Boolean,
    onToggleVisibility: () -> Unit,
    errorMessage: String?,
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = "API-Token eingeben",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Dein Server schützt alle Befehle und Statusabfragen mit einem Bearer-Token.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = token,
                onValueChange = onTokenChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("wizard_token_input"),
                label = { Text("Bearer-Token") },
                placeholder = { Text("Dein generiertes API-Token") },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Key, contentDescription = null)
                },
                trailingIcon = {
                    IconButton(onClick = onToggleVisibility) {
                        Icon(
                            imageVector = if (tokenVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (tokenVisible) "Token maskieren" else "Token anzeigen"
                        )
                    }
                },
                visualTransformation = if (tokenVisible) VisualTransformation.None else PasswordVisualTransformation(),
                isError = errorMessage != null,
                supportingText = {
                    if (errorMessage != null) {
                        Text(text = errorMessage, color = ErrorRed)
                    } else {
                        Text("Das Token wird sicher im lokalen Schlüsselspeicher abgelegt und nicht protokolliert.")
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AmberWarm)
            )

            Spacer(modifier = Modifier.height(18.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = SuccessGreen,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Sicherheitshinweis: Tokens mit eingeschränkten Rechten werden unterstützt. Funktionen, für die dein Token keine Berechtigung besitzt (z. B. Presets verwalten), werden in der App automatisch deaktiviert.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Zurück")
            }

            Button(
                onClick = onNext,
                enabled = errorMessage == null,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("wizard_token_next_button"),
                colors = ButtonDefaults.buttonColors(containerColor = AmberWarm, contentColor = Color.Black),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Weiter")
            }
        }
    }
}

@Composable
private fun TestAndFinishStep(
    baseUrl: String,
    isTesting: Boolean,
    testResult: ApiResult<String>?,
    onRunTest: () -> Unit,
    onComplete: () -> Unit,
    onBack: () -> Unit
) {
    val isSuccess = testResult is ApiResult.Success

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = "Verbindung überprüfen",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Teste die Verbindung zu '$baseUrl', bevor die App freigeschaltet wird.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onRunTest,
                enabled = !isTesting,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("wizard_test_connection_button"),
                colors = ButtonDefaults.buttonColors(containerColor = AmberWarm, contentColor = Color.Black),
                shape = RoundedCornerShape(14.dp)
            ) {
                if (isTesting) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.Black)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Verbindung wird geprüft...")
                } else {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Verbindung jetzt testen")
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Test Result Feedback Banner
            testResult?.let { result ->
                val (bgColor, iconTint, icon, title, description) = when (result) {
                    is ApiResult.Success -> {
                        Tuple5(
                            SuccessGreen.copy(alpha = 0.15f),
                            SuccessGreen,
                            Icons.Default.CheckCircle,
                            "Verbindung erfolgreich",
                            result.data
                        )
                    }
                    is ApiResult.Error -> {
                        if (result.httpStatus == 401) {
                            Tuple5(
                                ErrorRed.copy(alpha = 0.15f),
                                ErrorRed,
                                Icons.Default.Warning,
                                "Authentifizierung fehlgeschlagen (HTTP 401)",
                                "Das angegebene Token wurde vom Server abgewiesen. Bitte überprüfe das Token auf Tippfehler."
                            )
                        } else if (result.httpStatus == 403) {
                            Tuple5(
                                WarningAmber.copy(alpha = 0.15f),
                                WarningAmber,
                                Icons.Default.Warning,
                                "Zugriff eingeschränkt (HTTP 403)",
                                "Das Token ist authentifiziert, besitzt jedoch eingeschränkte Rechte. Du kannst die Einrichtung dennoch fortsetzen."
                            )
                        } else {
                            Tuple5(
                                ErrorRed.copy(alpha = 0.15f),
                                ErrorRed,
                                Icons.Default.Warning,
                                "Server nicht erreichbar",
                                result.message
                            )
                        }
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = bgColor
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = iconTint
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Zurück")
            }

            Button(
                onClick = onComplete,
                enabled = isSuccess && !isTesting,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("wizard_finish_button"),
                colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen, contentColor = Color.White),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Einrichtung abschließen")
            }
        }
    }
}

private data class Tuple5<A, B, C, D, E>(
    val a: A,
    val b: B,
    val c: C,
    val d: D,
    val e: E
)
