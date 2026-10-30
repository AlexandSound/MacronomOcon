package com.example.macronomocon.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.macronomocon.R
import com.example.macronomocon.domain.model.PantryItem
import com.example.macronomocon.ui.common.ViewState
import com.example.macronomocon.ui.components.EmptyState
import com.example.macronomocon.ui.components.ScreenHeading
import com.example.macronomocon.ui.components.StateFeedback
import com.example.macronomocon.ui.components.WarmButton
import com.example.macronomocon.ui.components.quantityLabel
import com.example.macronomocon.ui.pantry.PantryState
import com.example.macronomocon.ui.pantry.PantryViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun PantryScreen(
    state: ViewState<PantryState>,
    onAdd: () -> Unit,
    onRemove: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var soonOnly by rememberSaveable { mutableStateOf(false) }
    val visible = state.data.items.filter {
        !soonOnly ||
            it.isExpired() ||
            it.isExpiringWithin(PantryItem.EXPIRING_SOON_DAYS)
    }
    LazyColumn(
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            ScreenHeading(
                stringResource(R.string.pantry_title),
                stringResource(R.string.pantry_subtitle)
            )
        }
        item { WarmButton(stringResource(R.string.add_ingredient), onAdd, Modifier.fillMaxWidth()) }
        item { StateFeedback(state, onDismiss) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = !soonOnly, onClick = {
                    soonOnly = false
                }, label = { Text(stringResource(R.string.all_ingredients)) })
                FilterChip(selected = soonOnly, onClick = {
                    soonOnly = true
                }, label = { Text(stringResource(R.string.use_soon)) })
            }
        }
        if (visible.isEmpty()) {
            item {
                EmptyState(
                    stringResource(R.string.empty_pantry),
                    stringResource(
                        if (soonOnly) R.string.empty_expiring else R.string.empty_pantry_body
                    )
                )
            }
        }
        if(visible.isNotEmpty()){
            item { WarmButton("Remove all expired items", onClick =  {
                visible.forEach {
                    if (it.isExpired()) onRemove(it.pantryItemId)
                }
            }, Modifier.fillMaxWidth()) }
        }
        items(visible, key = {
            it.pantryItemId
        }) { item -> PantryItemRow(item, !state.isLoading) { onRemove(item.pantryItemId) } }
    }
}

@Composable
private fun PantryItemRow(item: PantryItem, enabled: Boolean, onRemove: () -> Unit) {
    val expiry = when {
        item.isExpired() -> stringResource(R.string.expired)
        item.expirationDate == LocalDate.now() -> stringResource(R.string.expires_today)
        else -> stringResource(
            R.string.expires_on,
            item.expirationDate.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
        )
    }
    Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(20.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(item.ingredient.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    quantityLabel(item.quantity, item.unit),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    expiry,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (item.isExpired()) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.secondary
                    }
                )
            }
            IconButton(onClick = onRemove, enabled = enabled) {
                Icon(
                    painterResource(R.drawable.ic_close),
                    contentDescription = stringResource(
                        R.string.remove_ingredient,
                        item.ingredient.name
                    )
                )
            }
        }
    }
}

