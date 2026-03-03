package com.mynigga.chatapp.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.mynigga.chatapp.ui.viewmodels.AuthViewModel
import com.mynigga.chatapp.ui.viewmodels.AuthState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangePasswordScreen(
    viewModel: AuthViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var oldPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    val authState by viewModel.authState
    val isLoading = authState is AuthState.Loading

    // Logic to handle password change
    fun handlePasswordChange() {
        if (oldPassword.isBlank() || newPassword.isBlank()) {
            Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Firebase re-authentication logic is usually needed before updating sensitive data
        // We'll call a dedicated function in AuthViewModel for this.
        viewModel.changeAccountPassword(oldPassword, newPassword)
    }

    LaunchedEffect(authState) {
        if (authState is AuthState.Authenticated) {
            // If the state is Authenticated, it means the operation was a success 
            // (since we stay in Authenticated state after success)
            // But we should reset state to idle to clear any previous errors
            viewModel.resetState()
            Toast.makeText(context, "Password updated successfully!", Toast.LENGTH_SHORT).show()
            onBack()
        } else if (authState is AuthState.Error) {
            Toast.makeText(context, (authState as AuthState.Error).message, Toast.LENGTH_LONG).show()
            viewModel.resetState()
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
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text("Update your security", style = MaterialTheme.typography.labelLarge, color = Color.Gray)
            Spacer(Modifier.height(32.dp))

            Text("Current Password", style = MaterialTheme.typography.labelLarge)
            AuthTextField(
                value = oldPassword,
                onValueChange = { oldPassword = it },
                placeholder = "Confirm old password",
                isPassword = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(24.dp))

            Text("New Password", style = MaterialTheme.typography.labelLarge)
            AuthTextField(
                value = newPassword,
                onValueChange = { newPassword = it },
                placeholder = "Enter new password",
                isPassword = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(40.dp))

            Button(
                onClick = { handlePasswordChange() },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2)), // Blue color
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Text("Set Password", color = Color.White)
                }
            }
        }
    }
}
