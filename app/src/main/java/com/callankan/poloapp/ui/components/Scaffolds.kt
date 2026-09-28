package com.callankan.poloapp.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.callankan.poloapp.ui.theme.PoloDimens

/** Pantalla principal de pestaña: cabecera grande + lista + botón flotante opcional. */
@Composable
fun TabScaffold(
    title: String,
    subtitle: String? = null,
    fab: (@Composable () -> Unit)? = null,
    actions: (@Composable RowScope.() -> Unit)? = null,
    header: (@Composable () -> Unit)? = null,
    content: LazyListScope.() -> Unit,
) {
    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = PoloDimens.screenPadding, end = PoloDimens.screenPadding, bottom = 104.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "header") {
                Column(Modifier.fillMaxWidth().statusBarsPadding().padding(top = 16.dp, bottom = 6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(title, style = MaterialTheme.typography.headlineLarge, modifier = Modifier.weight(1f))
                        actions?.invoke(this)
                    }
                    if (subtitle != null) {
                        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    header?.let {
                        androidx.compose.foundation.layout.Spacer(Modifier.padding(top = 14.dp))
                        it()
                    }
                }
            }
            content()
        }
        if (fab != null) {
            Box(Modifier.align(Alignment.BottomEnd).padding(20.dp)) { fab() }
        }
    }
}

/** Lista secundaria con barra superior y flecha atrás. */
@Composable
fun ListScaffold(
    title: String,
    onBack: () -> Unit,
    fab: (@Composable () -> Unit)? = null,
    actions: @Composable () -> Unit = {},
    content: LazyListScope.() -> Unit,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { BackTopBar(title, onBack, actions) },
        floatingActionButton = { fab?.let { Box(Modifier.navigationBarsPadding()) { it() } } },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(top = padding.calculateTopPadding()),
            contentPadding = PaddingValues(start = PoloDimens.screenPadding, end = PoloDimens.screenPadding, top = 4.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content,
        )
    }
}

@Composable
fun PoloFab(text: String, icon: ImageVector, onClick: () -> Unit) {
    ExtendedFloatingActionButton(
        onClick = onClick,
        icon = { Icon(icon, null) },
        text = { Text(text, style = MaterialTheme.typography.labelLarge) },
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        shape = MaterialTheme.shapes.large,
    )
}
