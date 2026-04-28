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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.royalfreshapp.R
import com.example.royalfreshapp.navigation.Routes
import com.example.royalfreshapp.ui.theme.RoyalFreshTheme

// Preference key
const val PREFS_NAME = "RoyalFreshPrefs"
const val KEY_PASSWORD_ENTERED = "password_entered"

@Composable
fun PasswordScreen(
    navController: NavController,
    onPasswordCorrect: () -> Unit
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
                contentDescription = stringResource(R.string.logo_description),
                modifier = Modifier.size(150.dp).padding(bottom = 32.dp)
            )
            Text(stringResource(R.string.please_enter_password), style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                    showError = false
                },
                label = { Text(stringResource(R.string.password)) },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                isError = showError,
                modifier = Modifier.fillMaxWidth(0.8f)
            )
            if (showError) {
                Text(
                    text = stringResource(R.string.incorrect_password),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = {
                if (password == correctPassword) {
                    val sharedPref = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    with(sharedPref.edit()) {
                        putBoolean(KEY_PASSWORD_ENTERED, true)
                        apply()
                    }
                    onPasswordCorrect()
                    println("Password Correct! Navigating...")
                } else {
                    showError = true
                }
            }) {
                Text(stringResource(R.string.login))
            }
        }
    }
}

// --- Preview ---
@Preview(showBackground = true)
@Composable
fun PasswordScreenPreview() {
    RoyalFreshTheme {
        PasswordScreen(navController = rememberNavController(), onPasswordCorrect = {})
    }
}
