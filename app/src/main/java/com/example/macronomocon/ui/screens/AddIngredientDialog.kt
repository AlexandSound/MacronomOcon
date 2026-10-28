package com.example.macronomocon.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.macronomocon.R
import com.example.macronomocon.domain.factory.PantryItemFactory
import com.example.macronomocon.domain.model.Ingredient
import com.example.macronomocon.ui.common.ViewState
import com.example.macronomocon.ui.components.StateFeedback
import com.example.macronomocon.ui.components.WarmButton
import com.example.macronomocon.ui.pantry.PantryState

@Composable
fun AddIngredientDialog(
    state: ViewState<PantryState>,
    onSearch: (String) -> Unit,
    onScan: () -> Unit,
    onSelect: (Ingredient) -> Unit,
    onAdd: (quantity: String, unit: String, daysUntilExpiry: String) -> Unit,
    onDismissError: () -> Unit,
    onDismiss: () -> Unit
) {
    var query by rememberSaveable { mutableStateOf("") }
    var quantity by rememberSaveable { mutableStateOf("") }
    var unit by rememberSaveable { mutableStateOf("") }
    var days by rememberSaveable { mutableStateOf("") }
    var searched by rememberSaveable { mutableStateOf(false) }
    val selected = state.data.selectedIngredient
    AlertDialog(
        onDismissRequest = { if (!state.isLoading) onDismiss() },
        title = { Text(stringResource(R.string.add_dialog_title)) },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StateFeedback(state, onDismissError)
                if (selected == null) {
                    WarmButton(stringResource(R.string.demo_scan), {
                        searched = true
                        onScan()
                    }, Modifier.fillMaxWidth(), !state.isLoading)
                    Text(
                        stringResource(R.string.scan_note),
                        style = MaterialTheme.typography.bodySmall
                    )
                    if (state.data.showIngredientSearch) {
                        Text(
                            stringResource(R.string.search_fallback)
                        )
                    }
                    OutlinedTextField(query, {
                        query = it
                    }, label = {
                        Text(stringResource(R.string.ingredient_search_hint))
                    }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    TextButton(
                        onClick = {
                            searched = true
                            onSearch(query)
                        },
                        enabled =
                        query.isNotBlank() && !state.isLoading
                    ) {
                        Text(stringResource(R.string.search))
                    }
                    if (searched &&
                        !state.isLoading &&
                        state.data.searchResults.isEmpty()
                    ) {
                        Text(stringResource(R.string.no_ingredients_found))
                    }
                    state.data.searchResults.forEach { ingredient ->
                        TextButton(onClick = {
                            onSelect(ingredient)
                        }, modifier = Modifier.fillMaxWidth()) { Text(ingredient.name) }
                    }
                } else {
                    IngredientQuantityFields(selected, quantity, { quantity = it }, unit, {
                        unit =
                            it
                    }, days, { days = it })
                    if (state.data.invalidStock) {
                        Text(
                            pluralStringResource(
                                R.plurals.invalid_stock,
                                PantryItemFactory.MAX_SHELF_LIFE_DAYS,
                                PantryItemFactory.MAX_SHELF_LIFE_DAYS
                            ),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (selected != null) {
                TextButton(onClick = {
                    onAdd(quantity, unit, days)
                }, enabled = !state.isLoading) { Text(stringResource(R.string.add_to_pantry)) }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !state.isLoading) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
private fun IngredientQuantityFields(
    ingredient: Ingredient,
    quantity: String,
    onQuantity: (String) -> Unit,
    unit: String,
    onUnit: (String) -> Unit,
    days: String,
    onDays: (String) -> Unit
) {
    Text(ingredient.name, style = MaterialTheme.typography.titleMedium)
    OutlinedTextField(
        quantity,
        onQuantity,
        label = {
            Text(stringResource(R.string.quantity_label))
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Decimal
        ),
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
    OutlinedTextField(unit, onUnit, label = {
        Text(stringResource(R.string.unit_label))
    }, singleLine = true, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(
        days,
        onDays,
        label = {
            Text(stringResource(R.string.expiry_label))
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Number
        ),
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
}
