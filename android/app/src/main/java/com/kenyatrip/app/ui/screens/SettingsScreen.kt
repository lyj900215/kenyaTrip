package com.kenyatrip.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.kenyatrip.app.data.AppSettings
import com.kenyatrip.app.ui.LangSetting
import com.kenyatrip.app.ui.S
import com.kenyatrip.app.ui.components.SectionCard
import com.kenyatrip.app.ui.t
import com.kenyatrip.app.ui.theme.Danger
import com.kenyatrip.app.ui.theme.Success

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: AppSettings,
    lang: LangSetting,
    aiTest: Boolean?,
    onLang: (LangSetting) -> Unit,
    onUpdate: ((AppSettings) -> AppSettings) -> Unit,
    onTestAi: () -> Unit,
    padding: PaddingValues,
) {
    Column(
        Modifier
            .padding(padding)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SectionCard(title = t(S.language)) {
            val options = LangSetting.entries
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                options.forEachIndexed { i, o ->
                    SegmentedButton(
                        selected = lang == o,
                        onClick = { onLang(o) },
                        shape = SegmentedButtonDefaults.itemShape(i, options.size),
                    ) {
                        Text(
                            when (o) {
                                LangSetting.AUTO -> t(S.langAuto)
                                LangSetting.ZH -> "中文"
                                LangSetting.EN -> "English"
                            },
                        )
                    }
                }
            }
        }
        SectionCard(title = t(S.aiSection)) {
            Text(t(S.aiExplain), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            ToggleRow(t(S.aiEnabled), settings.aiEnabled) { v -> onUpdate { it.copy(aiEnabled = v) } }
            OutlinedTextField(
                settings.aiBaseUrl, { v -> onUpdate { it.copy(aiBaseUrl = v.trim()) } },
                label = { Text(t(S.baseUrl)) }, modifier = Modifier.fillMaxWidth(), singleLine = true,
            )
            OutlinedTextField(
                settings.aiApiKey, { v -> onUpdate { it.copy(aiApiKey = v.trim()) } },
                label = { Text(t(S.apiKey)) }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
            )
            OutlinedTextField(
                settings.aiModel, { v -> onUpdate { it.copy(aiModel = v.trim()) } },
                label = { Text(t(S.model)) }, modifier = Modifier.fillMaxWidth(), singleLine = true,
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onTestAi) { Text(t(S.testConnection)) }
                when (aiTest) {
                    true -> Text(t(S.connectionOk), color = Success)
                    false -> Text(t(S.connectionFail), color = Danger)
                    null -> Unit
                }
            }
        }
        SectionCard(title = t(S.demoSection)) {
            ToggleRow(t(S.demoSoldOut), settings.demoSoldOut) { v -> onUpdate { it.copy(demoSoldOut = v) } }
            ToggleRow(t(S.demoNoAlt), settings.demoNoAlternatives) { v -> onUpdate { it.copy(demoNoAlternatives = v) } }
            ToggleRow(t(S.demoDecline), settings.demoPaymentDecline) { v -> onUpdate { it.copy(demoPaymentDecline = v) } }
        }
        SectionCard(title = t(S.about)) {
            Text("KenyaTrip 0.1.0", style = MaterialTheme.typography.bodyMedium)
            Text(t(S.demoNotice), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f).padding(end = 8.dp))
        Switch(checked, onChange)
    }
}
