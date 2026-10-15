package com.shearless.bareplayer.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shearless.bareplayer.core.domain.tips.TipJar
import com.shearless.bareplayer.core.domain.tips.TipJarEvent
import com.shearless.bareplayer.core.domain.tips.TipJarState.Status
import com.shearless.bareplayer.core.domain.tips.TipProduct

/**
 * Optional tips. Hidden entirely when store billing isn't available (e.g. a sideloaded build), so
 * there is never a broken purchase button.
 */
@Composable
internal fun SupportSection(tipJar: TipJar, snackbarHostState: SnackbarHostState) {
    val state by tipJar.state.collectAsStateWithLifecycle()
    val resources = LocalResources.current
    var customAmount by rememberSaveable { mutableStateOf("") }

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
                    CustomAmountInput(
                        amount = customAmount,
                        amounts = state.customAmounts,
                        onAmountChange = { customAmount = it },
                        onTip = { tipJar.tip(it.id) },
                    )
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
private fun CustomAmountInput(
    amount: String,
    amounts: List<TipProduct>,
    onAmountChange: (String) -> Unit,
    onTip: (TipProduct) -> Unit,
) {
    val normalizedAmount = amount.trim().removePrefix("$").toBigDecimalOrNull()
        ?.takeIf { it.signum() > 0 }
        ?.stripTrailingZeros()
        ?.toPlainString()
    val selected = amounts.firstOrNull { it.id.removePrefix("tip_custom_") == normalizedAmount }
    val invalid = amount.isNotBlank() && selected == null

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = amount,
            onValueChange = onAmountChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.tip_custom_amount)) },
            placeholder = { Text(stringResource(R.string.tip_custom_amount_hint)) },
            supportingText = {
                Text(stringResource(if (invalid) R.string.tip_custom_amount_invalid else R.string.tip_custom_amount_supported))
            },
            prefix = { Text("$") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            isError = invalid,
        )
        OutlinedButton(
            onClick = { selected?.let(onTip) },
            enabled = selected != null,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                if (selected == null) {
                    stringResource(R.string.tip_custom_amount)
                } else {
                    stringResource(R.string.tip_custom_amount_confirm, selected.formattedPrice)
                },
            )
        }
    }
}
