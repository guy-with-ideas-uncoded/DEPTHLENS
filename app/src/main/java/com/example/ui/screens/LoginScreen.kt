package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.ui.components.*
import com.example.ui.viewmodel.IntelligenceViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    viewModel: IntelligenceViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    var isLoading by remember { mutableStateOf(false) }

    // Tab state: "signin" or "signup"
    var authMode by remember { mutableStateOf("signin") }

    // Text field states
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }

    // Infinite animations for ambient glow
    val infiniteTransition = rememberInfiniteTransition(label = "ambient_login")
    val orbY by infiniteTransition.animateFloat(
        initialValue = -15f, targetValue = 15f,
        animationSpec = infiniteRepeatable(tween(6000, easing = EaseInOutQuad), RepeatMode.Reverse),
        label = "login_orb_y"
    )
    val glowScale by infiniteTransition.animateFloat(
        initialValue = 0.96f, targetValue = 1.08f,
        animationSpec = Motion.PulseGlow,
        label = "login_glow"
    )

    // Google Sign-In setup
    val gsoHelper = remember {
        val webClientId = getWebClientId(context)
        com.google.android.gms.auth.api.signin.GoogleSignInOptions.Builder(
            com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN
        )
            .requestIdToken(webClientId)
            .requestEmail()
            .build()
    }
    val googleSignInClient = remember {
        com.google.android.gms.auth.api.signin.GoogleSignIn.getClient(context, gsoHelper)
    }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val task = com.google.android.gms.auth.api.signin.GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(com.google.android.gms.common.api.ApiException::class.java)
                val idToken = account.idToken
                if (idToken != null) {
                    isLoading = true
                    viewModel.loginWithRealGoogle(idToken) { success, msg ->
                        isLoading = false
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(context, "Google Sign-In failed: Token null", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Google Sign-In failed: ${e.localizedMessage ?: "API Error"}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(AmbientGradientTop, AmbientGradientBottom)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Blob B1 (Ambient Drift)
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = (-30).dp, y = 100.dp + orbY.dp)
                .size(220.dp)
                .background(
                    Brush.radialGradient(listOf(ElectricViolet.copy(alpha = 0.22f), Color.Transparent))
                )
        )

        // Blob B2 (Cyan Ambient)
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .offset(x = 40.dp, y = orbY.dp)
                .size(240.dp)
                .background(
                    Brush.radialGradient(listOf(PremiumCyan.copy(alpha = 0.18f), Color.Transparent))
                )
        )

        Column(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .widthIn(max = 380.dp)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Official DepthLens logo with breathing animation
            DepthLensLogo(
                size = 88.dp,
                showGlow = true
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Welcome to DepthLens",
                color = TextPrimaryColor,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                fontFamily = InstrumentSansFontFamily
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "See beneath the surface. Sign in to sync your sessions.",
                color = TextMutedColor,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                lineHeight = 16.sp,
                modifier = Modifier.widthIn(max = 260.dp),
                fontFamily = InstrumentSansFontFamily
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Custom Tab Switcher for Sign In / Sign Up (Translucent Glass style)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .depthGlass(cornerRadius = 14.dp, borderWidth = 1.dp)
                    .padding(3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(11.dp))
                        .let { mod ->
                            if (authMode == "signin") {
                                mod.background(Brush.linearGradient(listOf(ElectricViolet, GradientEnd)))
                            } else {
                                mod.background(Color.Transparent)
                            }
                        }
                        .clickable { authMode = "signin" },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Sign In",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = InstrumentSansFontFamily
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(11.dp))
                        .let { mod ->
                            if (authMode == "signup") {
                                mod.background(Brush.linearGradient(listOf(ElectricViolet, GradientEnd)))
                            } else {
                                mod.background(Color.Transparent)
                            }
                        }
                        .clickable { authMode = "signup" },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Sign Up",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = InstrumentSansFontFamily
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Glass TextFields Block
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (authMode == "signup") {
                    // Full Name Input for signup
                    OutlinedTextField(
                        value = displayName,
                        onValueChange = { displayName = it },
                        placeholder = { Text("Display Name", color = TextMutedColor, fontSize = 13.sp) },
                        textStyle = TextStyle(color = TextPrimaryColor, fontSize = 13.sp, fontFamily = InstrumentSansFontFamily),
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ElectricViolet,
                            unfocusedBorderColor = GlassBorder,
                            focusedContainerColor = DynamicGlassFill,
                            unfocusedContainerColor = DynamicGlassFill
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Email field
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    placeholder = { Text("Email", color = TextMutedColor, fontSize = 13.sp) },
                    textStyle = TextStyle(color = TextPrimaryColor, fontSize = 13.sp, fontFamily = InstrumentSansFontFamily),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ElectricViolet,
                        unfocusedBorderColor = GlassBorder,
                        focusedContainerColor = DynamicGlassFill,
                        unfocusedContainerColor = DynamicGlassFill
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Password field
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    placeholder = { Text("Password", color = TextMutedColor, fontSize = 13.sp) },
                    textStyle = TextStyle(color = TextPrimaryColor, fontSize = 13.sp, fontFamily = InstrumentSansFontFamily),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true,
                    visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                    trailingIcon = {
                        IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                            Icon(
                                imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = "Toggle password visibility",
                                tint = TextMutedColor
                            )
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ElectricViolet,
                        unfocusedBorderColor = GlassBorder,
                        focusedContainerColor = DynamicGlassFill,
                        unfocusedContainerColor = DynamicGlassFill
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            if (isLoading) {
                CircularProgressIndicator(color = ElectricViolet, modifier = Modifier.size(32.dp))
            } else {
                // Main Auth Button (Get Started style with premium gradient)
                Button(
                    onClick = {
                        focusManager.clearFocus()
                        if (email.isBlank() || password.isBlank()) {
                            Toast.makeText(context, "Please fill in all details.", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        isLoading = true
                        if (authMode == "signin") {
                            viewModel.signInWithEmailAndPassword(email.trim(), password) { success, message ->
                                isLoading = false
                                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            val nameToUse = displayName.ifBlank { email.substringBefore("@") }
                            viewModel.signUpWithEmailAndPassword(email.trim(), password, nameToUse) { success, message ->
                                isLoading = false
                                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(listOf(ElectricViolet, GradientEnd)),
                                RoundedCornerShape(16.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (authMode == "signin") "Sign In →" else "Get Started →",
                            fontFamily = InstrumentSansFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Or Google Login (Continues with Google)
            Button(
                onClick = {
                    focusManager.clearFocus()
                    googleSignInClient.signOut().addOnCompleteListener {
                        val signInIntent = googleSignInClient.signInIntent
                        googleSignInLauncher.launch(signInIntent)
                    }
                },
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White.copy(alpha = 0.08f),
                    contentColor = TextPrimaryColor
                ),
                border = BorderStroke(1.dp, GlassBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "G",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp,
                        color = TextPrimaryColor,
                        fontFamily = DMMonoFontFamily
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Continue with Google",
                        fontFamily = InstrumentSansFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Guest Option
            Text(
                text = "Continue as Guest",
                color = PremiumCyan,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = InstrumentSansFontFamily,
                modifier = Modifier
                    .clickable {
                        focusManager.clearFocus()
                        viewModel.loginAsGuest("Abhay Shah")
                    }
                    .padding(8.dp)
            )
        }
    }
}

private fun getWebClientId(context: android.content.Context): String {
    return try {
        val resId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
        if (resId != 0) {
            context.getString(resId)
        } else {
            "653474960543-gprmibjuned77oq7tt9h1elqrasrbvth.apps.googleusercontent.com"
        }
    } catch (e: Exception) {
        "653474960543-gprmibjuned77oq7tt9h1elqrasrbvth.apps.googleusercontent.com"
    }
}
