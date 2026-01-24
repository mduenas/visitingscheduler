package com.markduenas.visischeduler.android.ui.components.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.markduenas.visischeduler.presentation.viewmodel.auth.PasswordStrength

/**
 * Password text field with visibility toggle.
 *
 * @param value Current password value
 * @param onValueChange Callback when value changes
 * @param label Label text for the field
 * @param modifier Modifier for the text field
 * @param enabled Whether the field is enabled
 * @param error Error message to display (null if no error)
 * @param placeholder Placeholder text
 * @param imeAction IME action button type
 * @param onImeAction Callback when IME action is triggered
 * @param showStrengthIndicator Whether to show password strength indicator
 * @param passwordStrength Current password strength (required if showStrengthIndicator is true)
 */
@Composable
fun PasswordTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String = "Password",
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    error: String? = null,
    placeholder: String = "Enter your password",
    imeAction: ImeAction = ImeAction.Done,
    onImeAction: () -> Unit = {},
    showStrengthIndicator: Boolean = false,
    passwordStrength: PasswordStrength = PasswordStrength.WEAK
) {
    var passwordVisible by remember { mutableStateOf(false) }
    val isError = error != null

    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled,
            isError = isError,
            placeholder = { Text(placeholder) },
            singleLine = true,
            visualTransformation = if (passwordVisible) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) {
                            Icons.Default.VisibilityOff
                        } else {
                            Icons.Default.Visibility
                        },
                        contentDescription = if (passwordVisible) {
                            "Hide password"
                        } else {
                            "Show password"
                        }
                    )
                }
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = imeAction
            ),
            keyboardActions = KeyboardActions(
                onDone = { onImeAction() },
                onGo = { onImeAction() },
                onNext = { onImeAction() }
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

        // Password strength indicator
        AnimatedVisibility(
            visible = showStrengthIndicator && value.isNotEmpty(),
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            PasswordStrengthIndicator(
                strength = passwordStrength,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

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
 * Visual indicator for password strength.
 */
@Composable
fun PasswordStrengthIndicator(
    strength: PasswordStrength,
    modifier: Modifier = Modifier
) {
    val (color, label) = when (strength) {
        PasswordStrength.WEAK -> Color(0xFFF44336) to "Weak"
        PasswordStrength.FAIR -> Color(0xFFFF9800) to "Fair"
        PasswordStrength.GOOD -> Color(0xFF2196F3) to "Good"
        PasswordStrength.STRONG -> Color(0xFF4CAF50) to "Strong"
    }

    val animatedColor by animateColorAsState(
        targetValue = color,
        label = "strength_color"
    )

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Strength bars
            repeat(4) { index ->
                val isActive = index < strength.ordinal + 1
                val barColor by animateColorAsState(
                    targetValue = if (isActive) animatedColor else MaterialTheme.colorScheme.surfaceVariant,
                    label = "bar_color_$index"
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                        .clip(MaterialTheme.shapes.small)
                        .background(barColor)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(animatedColor)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = animatedColor
            )
        }
    }
}

/**
 * Confirm password text field.
 */
@Composable
fun ConfirmPasswordTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    error: String? = null,
    label: String = "Confirm Password",
    placeholder: String = "Re-enter your password",
    imeAction: ImeAction = ImeAction.Done,
    onImeAction: () -> Unit = {}
) {
    PasswordTextField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        modifier = modifier,
        enabled = enabled,
        error = error,
        placeholder = placeholder,
        imeAction = imeAction,
        onImeAction = onImeAction,
        showStrengthIndicator = false
    )
}
