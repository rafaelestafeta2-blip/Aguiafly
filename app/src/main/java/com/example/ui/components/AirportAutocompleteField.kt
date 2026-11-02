package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlightLand
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.AirportPlaceSuggestion
import com.example.data.model.PlaceAutocompleteCatalog
import kotlinx.coroutines.delay

@Composable
fun AirportAutocompleteField(
    label: String,
    placeholder: String,
    selectedIata: String,
    isOrigin: Boolean,
    testTag: String,
    onQuerySuggestions: suspend (String) -> List<AirportPlaceSuggestion>,
    onSelectIata: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    val resolvedInitial = remember(selectedIata) {
        PlaceAutocompleteCatalog.resolveByIata(selectedIata)
    }

    var textValue by remember(selectedIata) {
        mutableStateOf(resolvedInitial.dropdownDisplayLabel)
    }
    var isEditing by remember { mutableStateOf(false) }
    var isFocused by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var suggestions by remember { mutableStateOf<List<AirportPlaceSuggestion>>(emptyList()) }

    LaunchedEffect(textValue, isFocused, isEditing) {
        if (!isFocused) return@LaunchedEffect
        val queryToSearch = if (isEditing) textValue else ""
        isLoading = true
        if (queryToSearch.length >= 2) {
            delay(180L)
        }
        suggestions = onQuerySuggestions(queryToSearch)
        isLoading = false
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        OutlinedTextField(
            value = textValue,
            onValueChange = { newText ->
                textValue = newText
                isEditing = true
                val trimmedUpper = newText.trim().uppercase()
                if (trimmedUpper.length == 3 && trimmedUpper.all { it.isLetter() }) {
                    val exact = PlaceAutocompleteCatalog.GLOBAL_AIRPORTS.firstOrNull {
                        it.iataCode == trimmedUpper
                    }
                    if (exact != null) {
                        onSelectIata(exact.iataCode)
                    }
                }
            },
            label = { Text(label) },
            placeholder = { Text(placeholder) },
            singleLine = true,
            leadingIcon = {
                Icon(
                    imageVector = if (isOrigin) Icons.Default.FlightTakeoff else Icons.Default.FlightLand,
                    contentDescription = label,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            trailingIcon = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(end = 6.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                    ) {
                        Text(
                            text = selectedIata.uppercase(),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }
                    if (textValue.isNotEmpty() && isFocused) {
                        IconButton(
                            onClick = {
                                textValue = ""
                                isEditing = true
                            },
                            modifier = Modifier
                                .size(28.dp)
                                .testTag("${testTag}_clear")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Limpar campo",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { state ->
                    isFocused = state.isFocused
                    if (!state.isFocused && isEditing) {
                        val refreshed = PlaceAutocompleteCatalog.resolveByIata(selectedIata)
                        textValue = refreshed.dropdownDisplayLabel
                        isEditing = false
                    }
                }
                .testTag(testTag)
        )

        AnimatedVisibility(visible = isFocused && suggestions.isNotEmpty()) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                tonalElevation = 6.dp,
                shadowElevation = 6.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 260.dp)
                    .testTag("${testTag}_dropdown")
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    suggestions.take(5).forEachIndexed { index, suggestion ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    textValue = suggestion.dropdownDisplayLabel
                                    isEditing = false
                                    onSelectIata(suggestion.iataCode)
                                    focusManager.clearFocus()
                                }
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                                .testTag("${testTag}_option_${suggestion.iataCode}"),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = suggestion.dropdownDisplayLabel,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (suggestion.countryName.isNotBlank()) {
                                    Text(
                                        text = "${suggestion.countryName} • Código IATA: ${suggestion.iataCode}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = suggestion.iataCode,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                        if (index < suggestions.take(5).lastIndex) {
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                            )
                        }
                    }
                }
            }
        }
    }
}
