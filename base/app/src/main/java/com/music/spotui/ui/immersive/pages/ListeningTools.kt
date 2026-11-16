package com.music.spotui.ui.immersive.pages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import app.myspoti.engine.R
import com.music.spotui.ui.immersive.Immersive
import com.music.spotui.ui.navigation.Routes

@Composable
internal fun ListeningTools(nav: NavController) {
    LazyRow(
        modifier = Modifier.padding(bottom = 16.dp),
        contentPadding = PaddingValues(horizontal = Immersive.Gutter),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            FilledTonalButton(onClick = { nav.navigate(Routes.MixBuilder.route) }) {
                Text(stringResource(R.string.app_mix_title))
            }
        }
        item {
            FilledTonalButton(onClick = { nav.navigate(Routes.OfflineFavorites.route) }) {
                Text(stringResource(R.string.app_offline_favorites))
            }
        }
        item {
            FilledTonalButton(onClick = { nav.navigate(Routes.MonthlyRecap.route) }) {
                Text(stringResource(R.string.app_recap_monthly))
            }
        }
    }
}
