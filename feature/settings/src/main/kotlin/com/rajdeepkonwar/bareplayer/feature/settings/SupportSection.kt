package com.rajdeepkonwar.bareplayer.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rajdeepkonwar.bareplayer.core.domain.tips.TipJar
import com.rajdeepkonwar.bareplayer.core.domain.tips.TipJarEvent
import com.rajdeepkonwar.bareplayer.core.domain.tips.TipJarState.Status
import com.rajdeepkonwar.bareplayer.core.domain.tips.TipProduct

/**
 * Optional tips. Hidden entirely when store billing isn't available (e.g. a sideloaded build), so
 * there is never a broken purchase button.
 */
@Composable
internal fun SupportSection(tipJar: TipJar, snackbarHostState: SnackbarHostState) {
    val state by tipJar.state.collectAsStateWithLifecycle()
    val resources = LocalResources.current
    var choosingAmount by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(tipJar) { tipJar.connect() }
    LaunchedEffect(tipJar) {
        tipJar.events.collect { event ->
            val message = when (event) {
                TipJarEvent.Thanks -> R.string.tip_thanks
                TipJarEvent.Pending -> R.string.tip_pending
                TipJarEvent.Failed -> R.string.tip_failed
            }
            snackbarHostState.showSnackbar(resources.getString(message))
        }
    }

    if (state.status != Status.Ready && !state.isSupporter) return

    SectionHeader(stringResource(R.string.section_support))
    Column(Modifier.padding(horizontal = 16.dp)) {
        Text(
            text = stringResource(if (state.isSupporter) R.string.supporter_thanks else R.string.support_body),
            style = MaterialTheme.typography.bodyMedium,
        )
        if (state.status == Status.Ready) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(vertical = 12.dp),
            ) {
                state.products.forEach { product -> TipButton(product, onClick = { tipJar.tip(product.id) }) }
                if (state.customAmounts.isNotEmpty()) {
                    OutlinedButton(onClick = { choosingAmount = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.tip_choose_amount))
                    }
                }
            }
            Text(
                text = stringResource(if (state.isPreview) R.string.support_preview_footnote else R.string.support_footnote),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }
    }
    HorizontalDivider(Modifier.padding(vertical = 8.dp))

    if (choosingAmount && state.customAmounts.isNotEmpty()) {
        CustomAmountDialog(
            amounts = state.customAmounts,
            onTip = { product ->
                choosingAmount = false
                tipJar.tip(product.id)
            },
            onDismiss = { choosingAmount = false },
        )
    }
}

@Composable
private fun TipButton(product: TipProduct, onClick: () -> Unit) {
    val label = stringResource(R.string.tip_button, tipLabel(product), product.formattedPrice)
    val modifier = Modifier.fillMaxWidth()
    if (product.featured) {
        Button(onClick = onClick, modifier = modifier) { Text(label) }
    } else {
        FilledTonalButton(onClick = onClick, modifier = modifier) { Text(label) }
    }
}

/** App-side names for the main tips, so they can be translated; falls back to the store title. */
@Composable
private fun tipLabel(product: TipProduct): String = when (product.id) {
    "tip_coffee" -> stringResource(R.string.tip_coffee)
    "tip_lunch" -> stringResource(R.string.tip_lunch)
    "tip_feature" -> stringResource(R.string.tip_feature)
    else -> product.title
}

@Composable
private fun CustomAmountDialog(amounts: List<TipProduct>, onTip: (TipProduct) -> Unit, onDismiss: () -> Unit) {
    // Start near the middle of the range rather than at the minimum.
    var index by rememberSaveable { mutableIntStateOf((amounts.size / 2 - 1).coerceIn(0, amounts.lastIndex)) }
    val selected = amounts[index.coerceIn(0, amounts.lastIndex)]

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.tip_choose_amount_title)) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = selected.formattedPrice,
                    style = MaterialTheme.typography.displaySmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                )
                Slider(
                    value = index.toFloat(),
                    onValueChange = { index = it.toInt().coerceIn(0, amounts.lastIndex) },
                    valueRange = 0f..amounts.lastIndex.toFloat(),
                    steps = (amounts.size - 2).coerceAtLeast(0),
                    modifier = Modifier.semantics { stateDescription = selected.formattedPrice },
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(amounts.first().formattedPrice, style = MaterialTheme.typography.labelMedium)
                    Text(amounts.last().formattedPrice, style = MaterialTheme.typography.labelMedium)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onTip(selected) }) {
                Text(stringResource(R.string.tip_amount_confirm, selected.formattedPrice))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}
