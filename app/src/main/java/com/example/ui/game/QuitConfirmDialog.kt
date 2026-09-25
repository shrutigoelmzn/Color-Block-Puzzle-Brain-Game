package com.example.ui.game

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.domain.model.GameTheme

/**
 * Quit Confirmation Dialog.
 *
 * Appears when the user taps the top-bar back arrow or presses the Android system back button
 * during active gameplay.
 *
 * Lifecycle & Navigation Rules:
 * 1. Remains persistent throughout lifecycle events (retained via ViewModel state and rememberSaveable).
 * 2. When dismissed (by tapping outside, pressing back button, or clicking "Keep Playing"):
 *    Calls [onDismiss] ONLY — never triggers navigation to the main menu.
 * 3. Only when the user deliberately clicks "Quit to Main Menu":
 *    Calls [onConfirmQuit], which performs clean teardown and navigates to the home screen.
 */
@Composable
fun QuitConfirmDialog(
    theme: GameTheme,
    score: Int,
    linesCleared: Int,
    onDismiss: () -> Unit,
    onConfirmQuit: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier
                .testTag("quit_confirmation_dialog")
                .padding(24.dp)
                .widthIn(max = 380.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            color = theme.cardBg,
            border = BorderStroke(1.dp, theme.cardBorder),
            tonalElevation = 6.dp,
            shadowElevation = 16.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Warning / Exit Icon Badge
                Surface(
                    shape = CircleShape,
                    color = Color(0xFFFF9800).copy(alpha = 0.16f),
                    modifier = Modifier.size(56.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.WarningAmber,
                            contentDescription = "Warning",
                            tint = Color(0xFFFF9800),
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Title
                Text(
                    text = "Quit Game?",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                    color = theme.textColorPrimary,
                    letterSpacing = 0.5.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Description
                Text(
                    text = "Are you sure you want to leave this game? Your current score and board progress will not be saved.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = theme.textColorSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Stats Preview Card
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = theme.cellEmptyBg,
                    border = BorderStroke(1.dp, theme.cardBorder.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp, horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "CURRENT SCORE",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = theme.textColorSecondary,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = "$score",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black,
                                color = theme.accentColor
                            )
                        }

                        Box(
                            modifier = Modifier
                                .height(28.dp)
                                .width(1.dp)
                                .background(theme.cardBorder)
                        )

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "LINES CLEARED",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = theme.textColorSecondary,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = "$linesCleared",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black,
                                color = theme.textColorPrimary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Primary Action: Keep Playing (Dismiss dialog, stay on game screen)
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .testTag("quit_dialog_keep_playing_button")
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = theme.accentColor,
                        contentColor = theme.onAccentColor
                    )
                ) {
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "KEEP PLAYING",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Secondary Action: Quit to Main Menu (Confirmed exit)
                OutlinedButton(
                    onClick = onConfirmQuit,
                    modifier = Modifier
                        .testTag("quit_dialog_confirm_quit_button")
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, theme.cardBorder)
                ) {
                    Icon(
                        imageVector = Icons.Default.Home,
                        contentDescription = null,
                        tint = theme.textColorSecondary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "QUIT TO MAIN MENU",
                        style = MaterialTheme.typography.labelLarge,
                        color = theme.textColorSecondary
                    )
                }
            }
        }
    }
}
