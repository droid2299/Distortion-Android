/*
 * Adapted from BitChord (https://github.com/kushagrasinghx/BitChord), GPL-3.0-or-later,
 * sharedUi/src/jvmSharedMain/kotlin/com/music/bitchord/ui/components/SearchField.kt at edc1e6f. Modified for Myspoti, 2026-10.
 */
package com.music.spotui.ui.immersive.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.myspoti.engine.R
import com.music.spotui.ui.immersive.Immersive

/**
 * The search field of the Immersive look: a [Immersive.Raised] capsule with a magnifier that
 * doubles as the submit button, a hint that gives way to the typed text, and a clear button once
 * there is something to clear. Stateless: keep [query] in the caller (the Search view model).
 *
 * @param onSubmit the keyboard's search key, or a tap on the magnifier with a non-blank query.
 * @param focusRequester lets the host focus it (re-tapping the Search tab).
 * @param placeholder what the empty field says it is for.
 */
@Composable
fun ImmersiveSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester = remember { FocusRequester() },
    placeholder: String = stringResource(R.string.immersive_shell_search_hint),
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    // Both ways of saying "search this" do the same two things, so they are written once here.
    val submit = {
        onSubmit()
        focusManager.clearFocus()
    }
    androidx.compose.material3.OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier.fillMaxWidth().focusRequester(focusRequester),
        shape = CircleShape,
        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Immersive.scheme.surfaceContainerHigh,
            unfocusedContainerColor = Immersive.scheme.surfaceContainer,
            unfocusedBorderColor = Immersive.scheme.outlineVariant,
        ),
        singleLine = true,
        placeholder = { Text(placeholder, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        leadingIcon = {
            androidx.compose.material3.IconButton(onClick = submit, enabled = query.isNotBlank()) {
                Icon(Icons.Rounded.Search, contentDescription = stringResource(R.string.immersive_shell_search_hint))
            }
        },
        trailingIcon = if (query.isNotEmpty()) {
            {
                androidx.compose.material3.IconButton(onClick = {
                    onQueryChange("")
                    focusRequester.requestFocus()
                    keyboardController?.show()
                }) {
                    Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.immersive_shell_search_clear))
                }
            }
        } else null,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { submit() }),
    )
}
