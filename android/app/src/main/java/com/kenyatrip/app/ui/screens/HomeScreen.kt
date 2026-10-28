package com.kenyatrip.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.kenyatrip.app.core.model.Destination
import com.kenyatrip.app.core.model.Text as T
import com.kenyatrip.app.ui.S
import com.kenyatrip.app.ui.UiState
import com.kenyatrip.app.ui.components.IconBadge
import com.kenyatrip.app.ui.t
import com.kenyatrip.app.ui.theme.Acacia
import com.kenyatrip.app.ui.theme.ChinaRed
import com.kenyatrip.app.ui.theme.Gold
import com.kenyatrip.app.ui.theme.Sunset

private val kenyaExamples = listOf(
    T("8月带孩子看动物大迁徙，8天，人均2万", "Migration safari in August with a 10-year-old, 8 days, \$3000 each"),
    T("蜜月10天，想去海边和马赛马拉，住豪华一点", "10-day honeymoon, beach and Masai Mara, luxury"),
    T("从广州出发，9月，十天，4人摄影团，要坐热气球", "4 photographers, September, 10 days, hot-air balloon"),
)

private val chinaExamples = listOf(
    T("10月广交会，7天，3位采购商，每人2500美元", "Canton Fair in October, 7 days, 3 traders, budget \$2500 each"),
    T("5月北京和上海家庭游，8天，2个大人2个孩子", "Family trip to Beijing and Shanghai in May, 8 days, 2 adults and 2 kids"),
    T("义乌采购加桂林观光，9天，2个人", "Yiwu sourcing plus Guilin sightseeing, 9 days, 2 people"),
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    state: UiState,
    onDestination: (Destination) -> Unit,
    onInput: (String) -> Unit,
    onPlan: () -> Unit,
) {
    val kenya = state.destination == Destination.KENYA
    val accent = if (kenya) Acacia else ChinaRed
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        if (kenya) listOf(Acacia, Color(0xFF3E7D4F), Sunset) else listOf(ChinaRed, Color(0xFFD0573A), Gold),
                    ),
                    RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp),
                )
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 24.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.AutoAwesome, null, tint = Color.White)
                    Spacer(Modifier.size(8.dp))
                    Text("KenyaTrip", color = Color.White.copy(alpha = 0.9f), style = MaterialTheme.typography.labelLarge)
                }
                Text(t(S.appTitle), color = Color.White, style = MaterialTheme.typography.headlineMedium)
                Text(t(S.appSubtitle), color = Color.White.copy(alpha = 0.92f), style = MaterialTheme.typography.bodyLarge)
            }
        }

        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DestinationCard(t(S.toKenya), t(S.toKenyaSub), "CN → KE", kenya, Acacia, Modifier.weight(1f)) {
                    onDestination(Destination.KENYA)
                }
                DestinationCard(t(S.toChina), t(S.toChinaSub), "KE → CN", !kenya, ChinaRed, Modifier.weight(1f)) {
                    onDestination(Destination.CHINA)
                }
            }

            OutlinedTextField(
                value = state.input,
                onValueChange = onInput,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .testTag("trip-input"),
                placeholder = { Text(t(if (kenya) S.inputHintKenya else S.inputHintChina)) },
                shape = RoundedCornerShape(16.dp),
            )
            Button(
                onClick = onPlan,
                enabled = !state.understanding,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("plan-button"),
                shape = RoundedCornerShape(16.dp),
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = accent),
            ) {
                if (state.understanding) {
                    CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Filled.AutoAwesome, null)
                    Spacer(Modifier.size(8.dp))
                    Text(t(S.planWithAi))
                }
            }

            Text(t(S.tryThese), style = MaterialTheme.typography.titleMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                (if (kenya) kenyaExamples else chinaExamples).forEach { example ->
                    val text = t(example)
                    Surface(
                        onClick = { onInput(text) },
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        color = MaterialTheme.colorScheme.surface,
                    ) {
                        Text(
                            text,
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        )
                    }
                }
            }

            Text(t(S.howItWorks), style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                HowStep(Icons.Filled.ChatBubbleOutline, t(S.step1), accent, Modifier.weight(1f))
                HowStep(Icons.Filled.Route, t(S.step2), Sunset, Modifier.weight(1f))
                HowStep(Icons.Filled.TaskAlt, t(S.step3), accent, Modifier.weight(1f))
            }
            Text(
                t(S.demoNotice),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun DestinationCard(
    title: String,
    subtitle: String,
    flags: String,
    selected: Boolean,
    color: Color,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        border = if (selected) BorderStroke(2.dp, color) else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(containerColor = if (selected) color.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(flags, style = MaterialTheme.typography.titleMedium)
            Text(title, style = MaterialTheme.typography.titleMedium, color = if (selected) color else MaterialTheme.colorScheme.onSurface)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun HowStep(icon: ImageVector, label: String, tint: Color, modifier: Modifier) {
    Card(
        modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            IconBadge(icon, tint)
            Text(label, style = MaterialTheme.typography.bodySmall, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
    }
}
