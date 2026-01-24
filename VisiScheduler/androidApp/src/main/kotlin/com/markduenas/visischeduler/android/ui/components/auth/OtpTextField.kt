package com.markduenas.visischeduler.android.ui.components.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 6-digit OTP input component with individual digit boxes.
 *
 * @param value Current OTP value
 * @param onValueChange Callback when value changes
 * @param modifier Modifier for the component
 * @param enabled Whether the input is enabled
 * @param error Error message to display (null if no error)
 * @param digitCount Number of digits (default 6)
 * @param onComplete Callback when all digits are entered
 * @param autoFocus Whether to auto-focus the input
 */
@Composable
fun OtpTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    error: String? = null,
    digitCount: Int = 6,
    onComplete: (String) -> Unit = {},
    autoFocus: Boolean = true
) {
    val focusRequester = remember { FocusRequester() }
    val isError = error != null

    LaunchedEffect(autoFocus) {
        if (autoFocus) {
            focusRequester.requestFocus()
        }
    }

    LaunchedEffect(value) {
        if (value.length == digitCount) {
            onComplete(value)
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        BasicTextField(
            value = value,
            onValueChange = { newValue ->
                // Only accept digits and limit to digitCount
                val filtered = newValue.filter { it.isDigit() }.take(digitCount)
                onValueChange(filtered)
            },
            modifier = Modifier.focusRequester(focusRequester),
            enabled = enabled,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.NumberPassword,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    if (value.length == digitCount) {
                        onComplete(value)
                    }
                }
            ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            decorationBox = { _ ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
                ) {
                    repeat(digitCount) { index ->
                        val char = value.getOrNull(index)
                        val isFocused = value.length == index
                        val hasValue = char != null

                        OtpDigitBox(
                            digit = char,
                            isFocused = isFocused && enabled,
                            isError = isError,
                            hasValue = hasValue,
                            enabled = enabled
                        )
                    }
                }
            }
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
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

/**
 * Individual digit box for OTP input.
 */
@Composable
private fun OtpDigitBox(
    digit: Char?,
    isFocused: Boolean,
    isError: Boolean,
    hasValue: Boolean,
    enabled: Boolean
) {
    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.05f else 1f,
        animationSpec = tween(durationMillis = 150),
        label = "otp_box_scale"
    )

    val borderColor by animateColorAsState(
        targetValue = when {
            isError -> MaterialTheme.colorScheme.error
            isFocused -> MaterialTheme.colorScheme.primary
            hasValue -> MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            !enabled -> MaterialTheme.colorScheme.outline.copy(alpha = 0.38f)
            else -> MaterialTheme.colorScheme.outline
        },
        label = "otp_border_color"
    )

    val backgroundColor by animateColorAsState(
        targetValue = when {
            !enabled -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            hasValue -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else -> MaterialTheme.colorScheme.surface
        },
        label = "otp_background_color"
    )

    val borderWidth = if (isFocused || isError) 2.dp else 1.dp

    Box(
        modifier = Modifier
            .width(48.dp)
            .height(56.dp)
            .scale(scale)
            .background(
                color = backgroundColor,
                shape = MaterialTheme.shapes.medium
            )
            .border(
                width = borderWidth,
                color = borderColor,
                shape = MaterialTheme.shapes.medium
            ),
        contentAlignment = Alignment.Center
    ) {
        if (digit != null) {
            Text(
                text = digit.toString(),
                style = TextStyle(
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                ),
                color = if (enabled) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                }
            )
        } else if (isFocused) {
            // Cursor indicator
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height(24.dp)
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
    }
}

/**
 * OTP resend timer component.
 */
@Composable
fun OtpResendTimer(
    secondsRemaining: Int,
    onResend: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val canResend = secondsRemaining <= 0

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (canResend) {
            Text(
                text = "Didn't receive the code? ",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            androidx.compose.material3.TextButton(
                onClick = onResend,
                enabled = enabled
            ) {
                Text(
                    text = "Resend",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }
        } else {
            Text(
                text = "Resend code in ${formatTime(secondsRemaining)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Format seconds to MM:SS format.
 */
private fun formatTime(seconds: Int): String {
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return if (minutes > 0) {
        String.format("%d:%02d", minutes, remainingSeconds)
    } else {
        "$remainingSeconds seconds"
    }
}
