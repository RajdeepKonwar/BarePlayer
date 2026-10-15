package com.rajdeepkonwar.bareplayer.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rajdeepkonwar.bareplayer.core.domain.tips.TipJar
import com.rajdeepkonwar.bareplayer.core.domain.tips.TipJarEvent
import com.rajdeepkonwar.bareplayer.core.domain.tips.TipJarState.Status

/**
 * Optional tips. Hidden entirely when store billing isn't available (e.g. a sideloaded build), so
 * there is never a broken purchase button.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SupportSection(tipJar: TipJar, snackbarHostState: SnackbarHostState) {
    val state by tipJar.state.collectAsStateWithLifecycle()
    val resources = LocalResources.current

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
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(vertical = 8.dp),
            ) {
                state.products.forEach { product ->
                    FilledTonalButton(onClick = { tipJar.tip(product.id) }) {
                        Text(stringResource(R.string.tip_button, product.title, product.formattedPrice))
                    }
                }
            }
            Text(
                text = stringResource(R.string.support_footnote),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }
    }
    HorizontalDivider(Modifier.padding(vertical = 8.dp))
}
