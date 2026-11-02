package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CardMembership
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.FrequentFlyerProgram
import com.example.data.model.FrequentFlyerSelection
import kotlin.math.roundToInt

@Composable
fun FrequentFlyerCheckoutCard(
    selection: FrequentFlyerSelection,
    totalFareEur: Double,
    cabinClass: String,
    onUpdateSelection: (FrequentFlyerSelection) -> Unit,
    modifier: Modifier = Modifier
) {
    val gold = MaterialTheme.colorScheme.primary
    val emerald = MaterialTheme.colorScheme.tertiary
    val pointsMultiplier = if (cabinClass in listOf("business", "first")) 12 else 6
    val estimatedPoints = (totalFareEur * pointsMultiplier).roundToInt().coerceAtLeast(850)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f)
                    )
                )
            )
            .border(
                width = 1.dp,
                color = gold.copy(alpha = 0.45f),
                shape = RoundedCornerShape(18.dp)
            )
            .padding(16.dp)
            .testTag("frequent_flyer_checkout_card")
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.WorkspacePremium,
                        contentDescription = null,
                        tint = gold,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Fidelidade & Programa de Milhas",
                            style = MaterialTheme.typography.titleMedium,
                            color = gold,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Pontue simultaneamente na companhia e no AguiaFly Club",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Surface(
                    shape = RoundedCornerShape(50),
                    color = emerald.copy(alpha = 0.18f),
                    modifier = Modifier.border(1.dp, emerald.copy(alpha = 0.6f), RoundedCornerShape(50))
                ) {
                    Text(
                        text = "+$estimatedPoints MILHAS",
                        style = MaterialTheme.typography.labelLarge,
                        color = emerald,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FrequentFlyerProgram.entries.forEach { program ->
                    val selected = selection.program == program
                    FilterChip(
                        selected = selected,
                        onClick = { onUpdateSelection(selection.copy(program = program)) },
                        label = {
                            Text(
                                text = program.displayName,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = gold,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier.testTag("ff_program_chip_${program.code.lowercase()}")
                    )
                }
            }

            if (selection.program != FrequentFlyerProgram.NONE) {
                OutlinedTextField(
                    value = selection.memberNumber,
                    onValueChange = { onUpdateSelection(selection.copy(memberNumber = it.uppercase().take(18))) },
                    label = { Text("Nº Cartão ${selection.program.displayName} (${selection.program.alliance})") },
                    placeholder = { Text("Ex: TP-904812743") },
                    singleLine = true,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.CardMembership,
                            contentDescription = null,
                            tint = gold
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("frequent_flyer_number_input")
                )
            }
        }
    }
}
