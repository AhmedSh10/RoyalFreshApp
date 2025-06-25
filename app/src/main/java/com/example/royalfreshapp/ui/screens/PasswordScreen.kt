package com.example.royalfreshapp.ui.screens

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.royalfreshapp.R // Import R from your actual project package
import com.example.royalfreshapp.navigation.Routes
import com.example.royalfreshapp.ui.theme.RoyalFreshTheme // Assuming Theme.kt exists

// Preference key
const val PREFS_NAME = "RoyalFreshPrefs"
const val KEY_PASSWORD_ENTERED = "password_entered"

@Composable
fun PasswordScreen(
    navController: NavController,
    onPasswordCorrect: () -> Unit // Callback when password is correct
) {
    var password by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }
    val correctPassword = "1111"
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.logo),
                contentDescription = "Royal Fresh Logo", // English
                modifier = Modifier.size(150.dp).padding(bottom = 32.dp)
            )
            Text("Please Enter Password", style = MaterialTheme.typography.headlineSmall) // English
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { 
                    password = it
                    showError = false // Hide error when user types
                 },
                label = { Text("Password") }, // English
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                isError = showError,
                modifier = Modifier.fillMaxWidth(0.8f)
            )
            if (showError) {
                Text(
                    text = "Incorrect Password", // English
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = {
                if (password == correctPassword) {
                    // Save the flag indicating password was entered correctly
                    val sharedPref = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    with (sharedPref.edit()) {
                        putBoolean(KEY_PASSWORD_ENTERED, true)
                        apply()
                    }
                    // Trigger the callback
                    onPasswordCorrect()
                    println("Password Correct! Navigating...") // Log for debugging
                } else {
                    showError = true
                }
            }) {
                Text("Login") // English
            }
        }
    }
}

// --- Preview --- 
@Preview(showBackground = true)
@Composable
fun PasswordScreenPreview() {
    RoyalFreshTheme {
        PasswordScreen(navController = rememberNavController(), onPasswordCorrect = {}) // Pass dummy NavController and callback
    }
}
