package com.mynigga.chatapp.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.mynigga.chatapp.ui.viewmodels.AuthState
import com.mynigga.chatapp.ui.viewmodels.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangePasswordScreen(
    viewModel: AuthViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var oldPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmNewPassword by remember { mutableStateOf("") }
    
    var isAttemptingChange by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    val authState by viewModel.authState
    val isLoading = authState is AuthState.Loading

    val passwordsMatch = newPassword == confirmNewPassword && newPassword.isNotEmpty()

    LaunchedEffect(authState) {
        if (isAttemptingChange) {
            if (authState is AuthState.Authenticated) {
                Toast.makeText(context, "Password updated successfully!", Toast.LENGTH_SHORT).show()
                isAttemptingChange = false
                errorMessage = null
                onBack()
            } else if (authState is AuthState.Error) {
                errorMessage = (authState as AuthState.Error).message
                isAttemptingChange = false
                viewModel.resetState()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Change Password", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Spacer(Modifier.height(32.dp))

            Text("Current Password", style = MaterialTheme.typography.labelLarge)
            AuthTextField(
                value = oldPassword,
                onValueChange = { 
                    oldPassword = it
                    errorMessage = null
                },
                placeholder = "Verify current password",
                isPassword = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(24.dp))

            Text("New Password", style = MaterialTheme.typography.labelLarge)
            AuthTextField(
                value = newPassword,
                onValueChange = { 
                    newPassword = it
                    errorMessage = null
                },
                placeholder = "Minimum 6 characters",
                isPassword = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))

            AuthTextField(
                value = confirmNewPassword,
                onValueChange = { 
                    confirmNewPassword = it
                    errorMessage = null
                },
                placeholder = "Confirm new password",
                isPassword = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.weight(1f))

            AnimatedVisibility(
                visible = errorMessage != null || (!passwordsMatch && confirmNewPassword.isNotEmpty()),
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                val displayError = when {
                    !passwordsMatch && confirmNewPassword.isNotEmpty() -> "New passwords do not match each other."
                    errorMessage != null -> errorMessage!!
                    else -> ""
                }
                
                if (displayError.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                            .background(MaterialTheme.colorScheme.errorContainer, RoundedCornerShape(8.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = displayError,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Button(
                onClick = { 
                    isAttemptingChange = true
                    viewModel.changeAccountPassword(oldPassword, newPassword) 
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2)),
                enabled = !isLoading && oldPassword.isNotBlank() && newPassword.length >= 6 && passwordsMatch
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Text("Set Password", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
            
            Spacer(Modifier.height(16.dp))
        }
    }
}
