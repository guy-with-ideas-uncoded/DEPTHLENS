package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.*
import com.example.ui.viewmodel.IntelligenceViewModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.GoogleAuthProvider
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    viewModel: IntelligenceViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isRegisterMode by remember { mutableStateOf(false) }

    // Input States
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var fullName by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    // Real Google Sign-In Activity Result Launcher
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account: GoogleSignInAccount = task.getResult(ApiException::class.java)
            val idToken = account.idToken
            if (idToken != null) {
                isLoading = true
                val credential = GoogleAuthProvider.getCredential(idToken, null)
                FirebaseAuth.getInstance().signInWithCredential(credential)
                    .addOnCompleteListener { authTask ->
                        isLoading = false
                        if (authTask.isSuccessful) {
                            val firebaseUser = authTask.result?.user
                            if (firebaseUser != null) {
                                viewModel.onAuthSuccess(firebaseUser, firebaseUser.displayName, isNew = false)
                                Toast.makeText(context, "Access authorized via Google", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(context, "Google Firebase Auth failed: ${authTask.exception?.message}", Toast.LENGTH_LONG).show()
                        }
                    }
            } else {
                Toast.makeText(context, "Google Sign-In failed: Client ID/ID Token is null.", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Google Sign-In Exception: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        DeepMidnight,
                        Color(0xFF09090F),
                        Color(0xFF040407)
                    )
                )
            )
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 420.dp)
                .background(Surface1, shape = RoundedCornerShape(20.dp))
                .border(1.dp, BorderSubtle, shape = RoundedCornerShape(20.dp))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Stylized Logo / Icon
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(Surface2, shape = RoundedCornerShape(14.dp))
                    .border(1.dp, BorderSubtle, shape = RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Auth lock",
                    tint = ElectricViolet,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Brand Title / Header
            Text(
                text = "DepthLens Intelligence",
                fontFamily = DMSerifDisplayFontFamily,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                fontSize = 24.sp,
                color = TextPrimaryColor,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "SECURITY INTEGRATION & VECTOR GATEWAY",
                fontFamily = DMMonoFontFamily,
                fontSize = 8.sp,
                color = TextMutedColor,
                letterSpacing = 1.2.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Subtitle action status
            Text(
                text = if (isRegisterMode) "Register New Agent Account" else "Authenticate Existing Session",
                fontFamily = InstrumentSansFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                color = TextPrimaryColor,
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Full Name (Only in Register mode)
            AnimatedVisibility(
                visible = isRegisterMode,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column {
                    OutlinedTextField(
                        value = fullName,
                        onValueChange = { fullName = it },
                        label = { Text("Display Name", fontSize = 11.sp, color = TextMutedColor) },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = "User info", tint = TextMutedColor, modifier = Modifier.size(16.dp)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ElectricViolet,
                            unfocusedBorderColor = BorderSubtle,
                            focusedTextColor = TextPrimaryColor,
                            unfocusedTextColor = TextPrimaryColor,
                            focusedContainerColor = Surface2,
                            unfocusedContainerColor = Surface2
                        ),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    )
                }
            }

            // Email Field
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email Address", fontSize = 11.sp, color = TextMutedColor) },
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = "Email address", tint = TextMutedColor, modifier = Modifier.size(16.dp)) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ElectricViolet,
                    unfocusedBorderColor = BorderSubtle,
                    focusedTextColor = TextPrimaryColor,
                    unfocusedTextColor = TextPrimaryColor,
                    focusedContainerColor = Surface2,
                    unfocusedContainerColor = Surface2
                ),
                shape = RoundedCornerShape(10.dp),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Password Field
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Secret Password", fontSize = 11.sp, color = TextMutedColor) },
                leadingIcon = { Icon(Icons.Default.Key, contentDescription = "Password key", tint = TextMutedColor, modifier = Modifier.size(16.dp)) },
                trailingIcon = {
                    IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                        Icon(
                            imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = "Toggle password visibility",
                            tint = TextMutedColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ElectricViolet,
                    unfocusedBorderColor = BorderSubtle,
                    focusedTextColor = TextPrimaryColor,
                    unfocusedTextColor = TextPrimaryColor,
                    focusedContainerColor = Surface2,
                    unfocusedContainerColor = Surface2
                ),
                shape = RoundedCornerShape(10.dp),
                singleLine = true,
                visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Submit Button
            Button(
                onClick = {
                    val cleanEmail = email.trim()
                    val cleanPass = password.trim()
                    val cleanName = fullName.trim()

                    if (cleanEmail.isEmpty() || cleanPass.isEmpty()) {
                        Toast.makeText(context, "Please fill in all security parameters.", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (cleanPass.length < 6) {
                        Toast.makeText(context, "Password must be at least 6 characters.", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (isRegisterMode && cleanName.isEmpty()) {
                        Toast.makeText(context, "Please enter your Display Name.", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    isLoading = true
                    coroutineScope.launch {
                        val auth = FirebaseAuth.getInstance()
                        if (isRegisterMode) {
                            // REGISTER
                            auth.createUserWithEmailAndPassword(cleanEmail, cleanPass)
                                .addOnCompleteListener { task ->
                                    isLoading = false
                                    if (task.isSuccessful) {
                                        val firebaseUser = task.result?.user
                                        if (firebaseUser != null) {
                                            viewModel.onAuthSuccess(firebaseUser, cleanName, isNew = true)
                                            Toast.makeText(context, "Registration successful!", Toast.LENGTH_SHORT).show()
                                        }
                                    } else {
                                        Toast.makeText(context, "Registration Error: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                                    }
                                }
                        } else {
                            // LOGIN
                            auth.signInWithEmailAndPassword(cleanEmail, cleanPass)
                                .addOnCompleteListener { task ->
                                    isLoading = false
                                    if (task.isSuccessful) {
                                        val firebaseUser = task.result?.user
                                        if (firebaseUser != null) {
                                            viewModel.onAuthSuccess(firebaseUser, null, isNew = false)
                                            Toast.makeText(context, "Access authorized.", Toast.LENGTH_SHORT).show()
                                        }
                                    } else {
                                        Toast.makeText(context, "Auth Error: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                                    }
                                }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
                shape = RoundedCornerShape(10.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Text(
                        text = if (isRegisterMode) "Create Account" else "Sign In",
                        fontFamily = InstrumentSansFontFamily,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Sub-options Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isRegisterMode) "Already configured email?" else "Require new account?",
                    fontFamily = InstrumentSansFontFamily,
                    fontSize = 10.5.sp,
                    color = TextMutedColor
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (isRegisterMode) "Sign In" else "Register Node",
                    fontFamily = InstrumentSansFontFamily,
                    fontSize = 10.5.sp,
                    color = ElectricViolet,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clickable { isRegisterMode = !isRegisterMode }
                        .padding(4.dp)
                )
            }

            Divider(
                color = BorderSubtle,
                thickness = 1.dp,
                modifier = Modifier.padding(vertical = 20.dp)
            )

            // Google Sign-In Button
            OutlinedButton(
                onClick = {
                    try {
                        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                            .requestIdToken(context.getString(R.string.default_web_client_id))
                            .requestEmail()
                            .build()
                        val googleSignInClient = GoogleSignIn.getClient(context, gso)
                        googleSignInLauncher.launch(googleSignInClient.signInIntent)
                    } catch (e: Exception) {
                        Toast.makeText(context, "Google Sign-In error: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, BorderSubtle),
                colors = ButtonDefaults.outlinedButtonColors(containerColor = Surface2)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    // Let's draw a nice Google icon color scheme
                    Text(
                        text = "G  ",
                        fontFamily = InstrumentSansFontFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                        color = ElectricViolet
                    )
                    Text(
                        text = "Sign In with Google",
                        fontFamily = InstrumentSansFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimaryColor,
                        fontSize = 11.5.sp
                    )
                }
            }
        }
    }
}
