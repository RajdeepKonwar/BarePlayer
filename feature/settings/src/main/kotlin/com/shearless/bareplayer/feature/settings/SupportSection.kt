package com.shearless.bareplayer.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shearless.bareplayer.core.domain.tips.TipJar
import com.shearless.bareplayer.core.domain.tips.TipJarEvent
import com.shearless.bareplayer.core.domain.tips.TipJarState.Status
import com.shearless.bareplayer.core.domain.tips.TipProduct
import kotlin.math.roundToInt

/**
 * Optional tips. Hidden entirely when store billing isn't available (e.g. a sideloaded build), so
 * there is never a broken purchase button.
 */
@Composable
internal fun SupportSection(tipJar: TipJar, snackbarHostState: SnackbarHostState) {
    val state by tipJar.state.collectAsStateWithLifecycle()
    val resources = LocalResources.current
    var choosingCustom by rememberSaveable { mutableStateOf(false) }

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
                    OutlinedButton(onClick = { choosingCustom = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.tip_custom))
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

    if (choosingCustom && state.status == Status.Ready && state.customAmounts.isNotEmpty()) {
        CustomTipDialog(
            amounts = state.customAmounts,
            onTip = { product ->
                choosingCustom = false
                tipJar.tip(product.id)
            },
            onDismiss = { choosingCustom = false },
        )
    }
}

@Composable
private fun TipButton(product: TipProduct, onClick: () -> Unit) {
    FilledTonalButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.tip_button, tipLabel(product), product.formattedPrice))
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

/** Play only sells fixed prices, so the slider snaps to the store-backed amounts instead of taking free text. */
@Composable
private fun CustomTipDialog(amounts: List<TipProduct>, onTip: (TipProduct) -> Unit, onDismiss: () -> Unit) {
    var index by rememberSaveable { mutableIntStateOf(0) }
    val selected = amounts[index.coerceIn(amounts.indices)]

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.tip_custom)) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(selected.formattedPrice, style = MaterialTheme.typography.displaySmall)
                if (amounts.size > 1) {
                    Slider(
                        value = index.toFloat(),
                        onValueChange = { index = it.roundToInt() },
                        valueRange = 0f..amounts.lastIndex.toFloat(),
                        steps = amounts.size - 2,
                        modifier = Modifier.padding(top = 16.dp),
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(
                            amounts.first().formattedPrice,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            amounts.last().formattedPrice,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onTip(selected) }) {
                Text(stringResource(R.string.tip_custom_confirm, selected.formattedPrice))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}
