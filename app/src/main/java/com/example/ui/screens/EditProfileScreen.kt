package com.example.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.example.ui.theme.*
import com.example.ui.components.premiumGlassBg
import com.example.ui.viewmodel.IntelligenceViewModel
import java.io.ByteArrayOutputStream
import java.io.File

@Composable
fun EditProfileScreen(
    viewModel: IntelligenceViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isUploading by viewModel.isProfileUploading.collectAsState()
    val userName by viewModel.userName.collectAsState()
    val userEmail by viewModel.userEmail.collectAsState()
    val userPhotoUrl by viewModel.userPhotoUrl.collectAsState()
    
    // Edit mode state
    var isEditing by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.resetProfileSavingState()
        isEditing = false
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.resetProfileSavingState()
        }
    }
    
    // Bio and focus area persistence using SharedPreferences
    val profilePrefs = remember { context.getSharedPreferences("depthlens_profile", Context.MODE_PRIVATE) }
    
    var fullNameInput by remember(userName) { mutableStateOf(userName) }
    var bioInput by remember { mutableStateOf(profilePrefs.getString("profile_bio", "Building things. Thinking deeper.") ?: "Building things. Thinking deeper.") }
    var focusAreaInput by remember { mutableStateOf(profilePrefs.getString("profile_focus", "Strategy · Psychology · Startups") ?: "Strategy · Psychology · Startups") }
    
    var showPhotoOptions by remember { mutableStateOf(false) }

    // Tokens
    val bgTop = Color(0xFF161334)
    val bgBottom = Color(0xFF0C0B1C)
    val textPrimary = Color(0xFFEFEDFF)
    val textMuted = Color(0xFF9D98C9)
    val labelViolet = Color(0xFFA78BFA)
    val glassFill = DynamicGlassFill
    val glassBorder = GlassBorder
    val accentGrad = Brush.linearGradient(listOf(Color(0xFF7C5CFF), Color(0xFF5B3FD6)))

    // Camera capture Uri helper
    val tempCacheFile = remember { File(context.cacheDir, "camera_avatar.jpg") }
    val cameraImageUri = remember {
        FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            tempCacheFile
        )
    }

    // Capture from Camera Launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            val bytes = processAndCompress(context, cameraImageUri)
            if (bytes != null) {
                viewModel.uploadProfilePhoto(bytes) { ok, msg ->
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(context, "Error processing camera image", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Pick from Gallery Launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val bytes = processAndCompress(context, uri)
            if (bytes != null) {
                viewModel.uploadProfilePhoto(bytes) { ok, msg ->
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(context, "Error processing gallery image", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val onSaveProfile = {
        viewModel.updateProfileName(fullNameInput) { success, msg ->
            viewModel.resetProfileSavingState()
            if (success) {
                profilePrefs.edit()
                    .putString("profile_bio", bioInput)
                    .putString("profile_focus", focusAreaInput)
                    .apply()
                Toast.makeText(context, "Profile saved ✓", Toast.LENGTH_SHORT).show()
                isEditing = false
            } else {
                Toast.makeText(context, "Failed to save: $msg", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .premiumGlassBg(cornerRadius = 0.dp, borderWidth = 1.dp, showSpecularHighlight = false)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .bounceClick(scaleOnPress = 0.96f) {
                            viewModel.resetProfileSavingState()
                            onNavigateBack()
                        }
                        .background(Color.White.copy(alpha = 0.05f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("‹", color = textPrimary, fontSize = 28.sp, fontWeight = FontWeight.Light, modifier = Modifier.offset(y = (-2).dp))
                }
                Text(
                    text = if (isEditing) "Edit Profile" else "Profile",
                    color = textPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = InstrumentSansFontFamily,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
                
                if (!isEditing) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(13.dp))
                            .bounceClick(scaleOnPress = 0.96f) { isEditing = true }
                            .background(Color.White.copy(alpha = 0.05f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Profile",
                            tint = textPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.size(38.dp))
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Spacer(modifier = Modifier.height(10.dp))

                // Avatar and Change Photo trigger
                Box(
                    modifier = Modifier.size(94.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(94.dp)
                            .clip(RoundedCornerShape(30.dp))
                            .background(accentGrad),
                        contentAlignment = Alignment.Center
                    ) {
                        if (userPhotoUrl.isNotEmpty()) {
                            Image(
                                painter = rememberAsyncImagePainter(
                                    model = ImageRequest.Builder(context)
                                        .data(userPhotoUrl)
                                        .crossfade(true)
                                        .build()
                                ),
                                contentDescription = "User Avatar",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            val initial = if (userName.isNotEmpty()) userName.first().uppercase() else "A"
                            Text(
                                text = initial,
                                color = Color.White,
                                fontSize = 32.sp,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = InstrumentSansFontFamily
                            )
                        }
                    }
                }

                // Change Photo text chip (Visible and clickable only in Edit Mode)
                if (isEditing) {
                    Box(
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .premiumGlassBg(cornerRadius = 100.dp)
                            .bounceClick(scaleOnPress = 0.96f) { showPhotoOptions = true }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "Change photo",
                            color = textPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = InstrumentSansFontFamily
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Form Fields
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Display name field
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Display name",
                            color = labelViolet,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            fontFamily = InstrumentSansFontFamily,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                        OutlinedTextField(
                            value = fullNameInput,
                            onValueChange = { fullNameInput = it },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp, color = textPrimary, fontFamily = InstrumentSansFontFamily),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = glassFill,
                                unfocusedContainerColor = glassFill,
                                disabledContainerColor = glassFill,
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                disabledBorderColor = Color.Transparent,
                                focusedTextColor = textPrimary,
                                unfocusedTextColor = textPrimary,
                                disabledTextColor = textPrimary.copy(alpha = 0.7f)
                            ),
                            shape = RoundedCornerShape(14.dp),
                            singleLine = true,
                            enabled = isEditing && !isUploading
                        )
                    }

                    // Email address (Disabled)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Email",
                            color = labelViolet,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            fontFamily = InstrumentSansFontFamily,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                        OutlinedTextField(
                            value = userEmail,
                            onValueChange = {},
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp, color = textMuted, fontFamily = InstrumentSansFontFamily),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = glassFill,
                                unfocusedContainerColor = glassFill,
                                disabledContainerColor = glassFill,
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                disabledBorderColor = Color.Transparent,
                                focusedTextColor = textMuted,
                                unfocusedTextColor = textMuted,
                                disabledTextColor = textMuted
                            ),
                            shape = RoundedCornerShape(14.dp),
                            singleLine = true,
                            enabled = false
                        )
                    }

                    // Bio field
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Bio",
                            color = labelViolet,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            fontFamily = InstrumentSansFontFamily,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                        OutlinedTextField(
                            value = bioInput,
                            onValueChange = { bioInput = it },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp, color = textPrimary, fontFamily = InstrumentSansFontFamily),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = glassFill,
                                unfocusedContainerColor = glassFill,
                                disabledContainerColor = glassFill,
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                disabledBorderColor = Color.Transparent,
                                focusedTextColor = textPrimary,
                                unfocusedTextColor = textPrimary,
                                disabledTextColor = textPrimary.copy(alpha = 0.7f)
                            ),
                            shape = RoundedCornerShape(14.dp),
                            singleLine = true,
                            enabled = isEditing && !isUploading
                        )
                    }

                    // Focus Area
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Focus area",
                            color = labelViolet,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            fontFamily = InstrumentSansFontFamily,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                        OutlinedTextField(
                            value = focusAreaInput,
                            onValueChange = { focusAreaInput = it },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp, color = textPrimary, fontFamily = InstrumentSansFontFamily),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = glassFill,
                                unfocusedContainerColor = glassFill,
                                disabledContainerColor = glassFill,
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                disabledBorderColor = Color.Transparent,
                                focusedTextColor = textPrimary,
                                unfocusedTextColor = textPrimary,
                                disabledTextColor = textPrimary.copy(alpha = 0.7f)
                            ),
                            shape = RoundedCornerShape(14.dp),
                            singleLine = true,
                            enabled = isEditing && !isUploading
                        )
                    }
                }

                if (isEditing) {
                    Spacer(modifier = Modifier.height(20.dp))

                    // Save button
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .alpha(if (isUploading) 0.5f else 1f)
                            .background(accentGrad)
                            .bounceClick(scaleOnPress = if (isUploading) 1f else 0.96f) {
                                if (!isUploading) {
                                    onSaveProfile()
                                }
                            }
                            .padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (isUploading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            }
                            Text(
                                text = if (isUploading) "Saving..." else "Save",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = InstrumentSansFontFamily
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }

    // Photo selection Dialog
    if (showPhotoOptions) {
        AlertDialog(
            onDismissRequest = { showPhotoOptions = false },
            containerColor = Color(0xFF16132E),
            title = { Text("Choose Avatar Source", color = textPrimary, fontFamily = InstrumentSansFontFamily, fontSize = 14.sp, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = {
                            showPhotoOptions = false
                            try {
                                cameraLauncher.launch(cameraImageUri)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Camera launch failed", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = glassFill),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Take Photo via Camera", color = textPrimary, fontSize = 12.sp)
                    }

                    Button(
                        onClick = {
                            showPhotoOptions = false
                            galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = glassFill),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Select from Media Library", color = textPrimary, fontSize = 12.sp)
                    }
                    
                    if (userPhotoUrl.isNotEmpty()) {
                        Button(
                            onClick = {
                                showPhotoOptions = false
                                viewModel.removeProfilePhoto { success, msg ->
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.2f)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Remove Photo", color = Color.Red, fontSize = 12.sp)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPhotoOptions = false }) {
                    Text("Cancel", color = textMuted)
                }
            }
        )
    }
}

// Crop & Compress Helper function
private fun processAndCompress(context: Context, uri: Uri): ByteArray? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri)
        val originalBitmap = BitmapFactory.decodeStream(inputStream) ?: return null
        inputStream?.close()

        // Center Crop to square aspect ratio
        val size = minOf(originalBitmap.width, originalBitmap.height)
        val x = (originalBitmap.width - size) / 2
        val y = (originalBitmap.height - size) / 2
        val cropped = Bitmap.createBitmap(originalBitmap, x, y, size, size)

        // Sensible avatar dimension: 400x400
        val finalSize = 400
        val scaled = Bitmap.createScaledBitmap(cropped, finalSize, finalSize, true)

        // Compress to JPEG size-efficient array
        val bos = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, 80, bos)
        bos.toByteArray()
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
