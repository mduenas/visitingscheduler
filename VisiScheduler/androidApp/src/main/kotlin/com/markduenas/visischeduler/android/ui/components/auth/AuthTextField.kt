package com.markduenas.visischeduler.android.ui.components.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp

/**
 * Reusable text field component for authentication screens.
 * Provides consistent styling and validation error display.
 *
 * @param value Current text value
 * @param onValueChange Callback when value changes
 * @param label Label text for the field
 * @param modifier Modifier for the text field
 * @param enabled Whether the field is enabled
 * @param error Error message to display (null if no error)
 * @param placeholder Placeholder text
 * @param leadingIcon Leading icon composable
 * @param trailingIcon Trailing icon composable
 * @param keyboardType Type of keyboard to show
 * @param imeAction IME action button type
 * @param onImeAction Callback when IME action is triggered
 * @param singleLine Whether the field is single line
 * @param maxLines Maximum number of lines
 * @param visualTransformation Visual transformation (e.g., for password masking)
 * @param capitalization Keyboard capitalization mode
 */
@Composable
fun AuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    error: String? = null,
    placeholder: String? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    onImeAction: () -> Unit = {},
    singleLine: Boolean = true,
    maxLines: Int = 1,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.None
) {
    val isError = error != null

    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled,
            isError = isError,
            placeholder = placeholder?.let { { Text(it) } },
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            singleLine = singleLine,
            maxLines = maxLines,
            visualTransformation = visualTransformation,
            keyboardOptions = KeyboardOptions(
                keyboardType = keyboardType,
                imeAction = imeAction,
                capitalization = capitalization
            ),
            keyboardActions = KeyboardActions(
                onDone = { onImeAction() },
                onGo = { onImeAction() },
                onNext = { onImeAction() },
                onSearch = { onImeAction() },
                onSend = { onImeAction() }
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                errorBorderColor = MaterialTheme.colorScheme.error,
                focusedLabelColor = MaterialTheme.colorScheme.primary,
                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                errorLabelColor = MaterialTheme.colorScheme.error
            ),
            shape = MaterialTheme.shapes.medium
        )

        // Error message with animation
        AnimatedVisibility(
            visible = isError,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Text(
                text = error ?: "",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
            )
        }
    }
}

/**
 * Email-specific text field with appropriate keyboard and validation.
 */
@Composable
fun EmailTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    error: String? = null,
    label: String = "Email",
    placeholder: String = "Enter your email",
    imeAction: ImeAction = ImeAction.Next,
    onImeAction: () -> Unit = {}
) {
    AuthTextField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        modifier = modifier,
        enabled = enabled,
        error = error,
        placeholder = placeholder,
        keyboardType = KeyboardType.Email,
        imeAction = imeAction,
        onImeAction = onImeAction,
        capitalization = KeyboardCapitalization.None
    )
}

/**
 * Phone number-specific text field with appropriate keyboard.
 */
@Composable
fun PhoneTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    error: String? = null,
    label: String = "Phone Number",
    placeholder: String = "Enter your phone number",
    imeAction: ImeAction = ImeAction.Next,
    onImeAction: () -> Unit = {}
) {
    AuthTextField(
        value = value,
        onValueChange = { newValue ->
            // Only allow digits, spaces, dashes, parentheses, and plus sign
            val filtered = newValue.filter { it.isDigit() || it in " -()+" }
            onValueChange(filtered)
        },
        label = label,
        modifier = modifier,
        enabled = enabled,
        error = error,
        placeholder = placeholder,
        keyboardType = KeyboardType.Phone,
        imeAction = imeAction,
        onImeAction = onImeAction
    )
}

/**
 * Name text field with appropriate capitalization.
 */
@Composable
fun NameTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    error: String? = null,
    placeholder: String = "Enter name",
    imeAction: ImeAction = ImeAction.Next,
    onImeAction: () -> Unit = {}
) {
    AuthTextField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        modifier = modifier,
        enabled = enabled,
        error = error,
        placeholder = placeholder,
        keyboardType = KeyboardType.Text,
        imeAction = imeAction,
        onImeAction = onImeAction,
        capitalization = KeyboardCapitalization.Words
    )
}
