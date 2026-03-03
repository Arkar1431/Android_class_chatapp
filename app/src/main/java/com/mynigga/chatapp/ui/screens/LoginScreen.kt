package com.mynigga.chatapp.ui.screens

import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.mynigga.chatapp.R
import com.mynigga.chatapp.ui.viewmodels.AuthState
import com.mynigga.chatapp.ui.viewmodels.AuthViewModel
import com.mynigga.chatapp.ui.components.AuthTextField
import com.mynigga.chatapp.ui.components.PrimaryButton
import com.mynigga.chatapp.utils.NetworkUtils
import kotlinx.coroutines.delay

@Composable
fun LoginScreen(
    viewModel: AuthViewModel = viewModel(),
    onNavigateToSignUp: () -> Unit,
    onLoginSuccess: () -> Unit
) {
    val context = LocalContext.current
    val authState by viewModel.authState
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val isLoading = authState is AuthState.Loading

    val focusRequester = remember { FocusRequester() }

    val googleLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val idToken = account.idToken
            if (idToken != null) {
                viewModel.signInWithGoogle(idToken)
            } else {
                Log.e("AuthError", "Google Sign-In: ID Token is null. Check Web Client ID.")
                Toast.makeText(context, "Google configuration error.", Toast.LENGTH_LONG).show()
            }
        } catch (e: ApiException) {
            Log.e("AuthError", "Google Sign-In failed code: ${e.statusCode}", e)
            val msg = when (e.statusCode) {
                7 -> "Network error. Check your connection."
                10 -> "Developer error. Is SHA-1 registered in Firebase?"
                12501 -> "Sign-in cancelled."
                else -> "Google Sign-In failed (${e.statusCode})"
            }
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        delay(300)
        focusRequester.requestFocus()
    }

    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.Authenticated -> onLoginSuccess()
            is AuthState.Error -> {
                Toast.makeText(context, (authState as AuthState.Error).message, Toast.LENGTH_LONG).show()
                viewModel.resetState()
            }
            else -> {}
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_logo),
            contentDescription = "Logo",
            modifier = Modifier.size(200.dp).padding(bottom = 32.dp)
        )

        AuthTextField(
            value = email, 
            onValueChange = { email = it }, 
            placeholder = "Email",
            modifier = Modifier.width(280.dp).focusRequester(focusRequester)
        )
        Spacer(Modifier.height(12.dp))
        AuthTextField(value = password, onValueChange = { password = it }, placeholder = "Password", isPassword = true)
        Spacer(Modifier.height(24.dp))

        PrimaryButton(
            text = "Login", 
            onClick = { 
                if (NetworkUtils.isNetworkAvailable(context)) {
                    viewModel.signIn(email, password)
                } else {
                    Toast.makeText(context, "No internet connection", Toast.LENGTH_SHORT).show()
                }
            }, 
            isLoading = isLoading
        )

        Spacer(Modifier.height(12.dp))

        TextButton(
            onClick = {
                if (!NetworkUtils.isNetworkAvailable(context)) {
                    Toast.makeText(context, "No internet connection", Toast.LENGTH_SHORT).show()
                    return@TextButton
                }
                
                val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(context.getString(R.string.default_web_client_id))
                    .requestEmail()
                    .build()
                val googleSignInClient = GoogleSignIn.getClient(context, gso)
                
                googleSignInClient.signOut().addOnCompleteListener {
                    googleLauncher.launch(googleSignInClient.signInIntent)
                }
            },
            enabled = !isLoading,
            modifier = Modifier.width(280.dp),
            contentPadding = PaddingValues(0.dp)
        ) {
            Text("Continue with Google")
        }

        Spacer(Modifier.height(8.dp))

        TextButton(
            onClick = onNavigateToSignUp,
            enabled = !isLoading,
            modifier = Modifier.width(280.dp),
            contentPadding = PaddingValues(0.dp)
        ) {
            Text("Don't have an account? Sign Up")
        }
    }
}
