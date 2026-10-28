package com.kenyatrip.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.AirportShuttle
import androidx.compose.material.icons.filled.BusinessCenter
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.LocalActivity
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Train
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kenyatrip.app.core.model.ComponentType
import com.kenyatrip.app.core.model.ItemType
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove

@Composable
fun SectionCard(
    modifier: Modifier = Modifier,
    title: String? = null,
    container: Color = MaterialTheme.colorScheme.surface,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = container.compositeOver(MaterialTheme.colorScheme.surface)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (title != null) Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

@Composable
fun Pill(text: String, color: Color, contentColor: Color = Color.White, modifier: Modifier = Modifier) {
    Box(
        modifier
            .background(color, RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(text, color = contentColor, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun IconBadge(icon: ImageVector, tint: Color, modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(36.dp)
            .background(tint.copy(alpha = 0.12f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
    }
}

@Composable
fun Stepper(label: String, value: Int, onChange: (Int) -> Unit, min: Int, max: Int, suffix: String = "") {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        OutlinedIconButton(onClick = { onChange(value - 1) }, enabled = value > min, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Filled.Remove, contentDescription = "-")
        }
        Text(
            "$value$suffix",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.width(64.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        OutlinedIconButton(onClick = { onChange(value + 1) }, enabled = value < max, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Filled.Add, contentDescription = "+")
        }
    }
}

@Composable
fun SmallIconButton(icon: ImageVector, description: String, onClick: () -> Unit) {
    IconButton(onClick = onClick) { Icon(icon, contentDescription = description) }
}

@Composable
fun VSpace(h: Int) = Spacer(Modifier.padding(top = h.dp))

fun itemIcon(type: ItemType): ImageVector = when (type) {
    ItemType.FLIGHT -> Icons.Filled.Flight
    ItemType.TRANSFER -> Icons.Filled.DirectionsCar
    ItemType.TRAIN -> Icons.Filled.Train
    ItemType.DOMESTIC_FLIGHT -> Icons.Filled.Flight
    ItemType.GAME_DRIVE -> Icons.Filled.Pets
    ItemType.ACTIVITY -> Icons.Filled.LocalActivity
    ItemType.STAY -> Icons.Filled.Hotel
    ItemType.MEAL -> Icons.Filled.Restaurant
    ItemType.FREE -> Icons.AutoMirrored.Filled.DirectionsWalk
    ItemType.BUSINESS -> Icons.Filled.BusinessCenter
}

fun componentIcon(type: ComponentType, supplier: String = ""): ImageVector = when (type) {
    ComponentType.FLIGHT -> Icons.Filled.Flight
    ComponentType.DOMESTIC_TRANSPORT ->
        if (supplier.contains("airline", ignoreCase = true)) Icons.Filled.Flight
        else Icons.Filled.Train
    ComponentType.STAY -> Icons.Filled.Hotel
    ComponentType.GROUND -> Icons.Filled.AirportShuttle
    ComponentType.PARK_FEES -> Icons.Filled.Park
    ComponentType.ACTIVITY -> Icons.Filled.LocalActivity
}
