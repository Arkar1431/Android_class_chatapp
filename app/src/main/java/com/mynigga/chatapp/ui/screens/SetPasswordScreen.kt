package com.mynigga.chatapp.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mynigga.chatapp.ui.components.AuthTextField
import com.mynigga.chatapp.ui.components.PrimaryButton
import com.mynigga.chatapp.ui.viewmodels.AuthState
import com.mynigga.chatapp.ui.viewmodels.AuthViewModel

@Composable
fun SetPasswordScreen(
    viewModel: AuthViewModel,
    onComplete: () -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    
    val authState by viewModel.authState
    val isLoading = authState is AuthState.Loading

    // Observe AuthState for success or error feedback
    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.Authenticated -> {
                // If account creation was successful, move to main app
                onComplete()
            }
            is AuthState.Error -> {
                // Keep the error message visible on screen (handled by the UI below)
            }
            else -> {}
        }
    }

    // Validation logic
    val isFormValid = name.isNotBlank() && 
                     password.length >= 6 && 
                     password == confirmPassword

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Complete Your Profile",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(Modifier.height(8.dp))
        
        Text(
            text = "Set your unique username and secure your account.",
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            color = androidx.compose.ui.graphics.Color.Gray,
            fontSize = 14.sp
        )

        Spacer(Modifier.height(32.dp))

        // Username Field
        AuthTextField(
            value = name,
            onValueChange = { 
                name = it
                if (authState is AuthState.Error) viewModel.resetState() // Clear errors when user types
            },
            placeholder = "Choose a unique username",
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        // Password Field
        AuthTextField(
            value = password,
            onValueChange = { 
                password = it
                if (authState is AuthState.Error) viewModel.resetState()
            },
            placeholder = "Create password (6+ chars)",
            isPassword = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        // Confirm Password Field
        AuthTextField(
            value = confirmPassword,
            onValueChange = { 
                confirmPassword = it
                if (authState is AuthState.Error) viewModel.resetState()
            },
            placeholder = "Confirm password",
            isPassword = true,
            modifier = Modifier.fillMaxWidth()
        )

        // Error Feedback Box (Animated)
        AnimatedVisibility(visible = authState is AuthState.Error || (password != confirmPassword && confirmPassword.isNotEmpty())) {
            val errorMessage = when {
                authState is AuthState.Error -> (authState as AuthState.Error).message
                password != confirmPassword -> "Passwords do not match"
                else -> ""
            }

            if (errorMessage.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                        .background(MaterialTheme.colorScheme.errorContainer, RoundedCornerShape(8.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.ErrorOutline, null, tint = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.width(8.dp))
                    Text(text = errorMessage, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                }
            }
        }

        Spacer(Modifier.height(32.dp))

        PrimaryButton(
            text = "create account",
            onClick = { viewModel.finalizeAccountSetup(name, password) },
            isLoading = isLoading,
            enabled = isFormValid && authState !is AuthState.Error,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(8.dp))

        // Cancel / Return Button
        TextButton(
            onClick = { viewModel.signOut() },
            enabled = !isLoading
        ) {
            Text(
                text = "Cancel and Return to Login",
                color = Color.Gray,
                fontSize = 14.sp
            )
        }
    }
}
