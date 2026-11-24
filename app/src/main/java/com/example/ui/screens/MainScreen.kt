package com.example.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.local.HistoryEntity
import com.example.ui.viewmodel.HsCodeViewModel
import com.example.ui.viewmodel.SearchUiState
import java.io.File
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.graphics.asImageBitmap
import com.example.ui.theme.*
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp

@Composable
fun MainScreen(viewModel: HsCodeViewModel) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val historyList by viewModel.historyList.collectAsState()
    val clipboardManager = LocalClipboardManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    var productNameInput by remember { mutableStateOf("") }
    var selectedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var showSelectedImagePreview by remember { mutableStateOf(false) }
    var activeDetailItem by remember { mutableStateOf<HistoryEntity?>(null) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showCalculatorDialog by remember { mutableStateOf(false) }
    var calculatorPreloadItem by remember { mutableStateOf<HistoryEntity?>(null) }
    var isUserLoggedIn by remember { mutableStateOf(viewModel.isLoggedIn()) }
    var showDisclaimerDialog by remember { mutableStateOf(!viewModel.isDisclaimerAccepted()) }

    // Launchers
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            selectedImageUri = uri
            val bitmap = uriToBitmap(context, uri)
            if (bitmap != null) {
                selectedBitmap = bitmap
                showSelectedImagePreview = true
                viewModel.resetState()
            } else {
                Toast.makeText(context, "فشل في قراءة الصورة من المعرض", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            selectedBitmap = bitmap
            selectedImageUri = null
            showSelectedImagePreview = true
            viewModel.resetState()
        }
    }

    // Force RTL layout direction for Arabic
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        if (!isUserLoggedIn) {
            ActivationScreen(
                viewModel = viewModel,
                onActivationSuccess = {
                    isUserLoggedIn = true
                }
            )
        } else {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = GeoBackground
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // App Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(GeoPrimary)
                            .statusBarsPadding()
                            .height(64.dp)
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(Color.White, CircleShape)
                                    .border(1.dp, GeoGold, CircleShape)
                                    .clip(CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                AsyncImage(
                                    model = R.drawable.app_logo_fg,
                                    contentDescription = "Logo",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            Column {
                                Text(
                                    text = "مستكشف الرموز",
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "HS Code Explorer",
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    calculatorPreloadItem = null
                                    showCalculatorDialog = true
                                },
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(Color.White.copy(alpha = 0.15f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Calculate,
                                    contentDescription = "حاسبة الرسوم",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            IconButton(
                                onClick = {
                                    showSettingsDialog = true
                                },
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(Color.White.copy(alpha = 0.15f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "Settings",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    // Content Container
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Modern Search Bar
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "ابحث عن رموز التعرفة الجمركية",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = GeoTextPrimary,
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                )
                                OutlinedTextField(
                                    value = productNameInput,
                                    onValueChange = { productNameInput = it },
                                    placeholder = { 
                                        Text(
                                            "ابحث عن اسم المنتج جمركياً...", 
                                            color = GeoTextSecondary,
                                            fontSize = 14.sp
                                        ) 
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Search,
                                            contentDescription = "Search",
                                            tint = GeoTextSecondary
                                        )
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(28.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = GeoPrimary,
                                        unfocusedBorderColor = Color.Transparent,
                                        focusedContainerColor = GeoSurfaceLight,
                                        unfocusedContainerColor = GeoSurfaceLight,
                                        focusedTextColor = GeoTextPrimary,
                                        unfocusedTextColor = GeoTextPrimary
                                    ),
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                    keyboardActions = KeyboardActions(onSearch = {
                                        if (productNameInput.isNotBlank()) {
                                            keyboardController?.hide()
                                            showSelectedImagePreview = false
                                            viewModel.searchByProductName(productNameInput)
                                        }
                                    }),
                                    singleLine = true
                                )

                                Button(
                                    onClick = {
                                        if (productNameInput.isNotBlank()) {
                                            keyboardController?.hide()
                                            showSelectedImagePreview = false
                                            viewModel.searchByProductName(productNameInput)
                                        } else {
                                            Toast.makeText(context, "يرجى كتابة اسم المنتج أولاً", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp),
                                    shape = RoundedCornerShape(25.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = GeoGold,
                                        contentColor = Color.White
                                    )
                                ) {
                                    Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("ابحث بالاسم", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                            }
                        }

                        // Camera Card (Primary Geometric Action Card)
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(24.dp))
                                    .background(GeoPrimary)
                                    .padding(24.dp)
                            ) {
                                // Subtle Geometric shapes in the background using Canvas
                                androidx.compose.foundation.Canvas(
                                    modifier = Modifier.matchParentSize()
                                ) {
                                    // Left bottom decorative circle
                                    drawCircle(
                                        color = GeoPrimaryLight,
                                        radius = 110.dp.toPx(),
                                        center = androidx.compose.ui.geometry.Offset(-20.dp.toPx(), size.height + 20.dp.toPx()),
                                        alpha = 0.08f
                                    )
                                    // Right top outline circle
                                    drawCircle(
                                        color = GeoPrimaryLight,
                                        radius = 80.dp.toPx(),
                                        center = androidx.compose.ui.geometry.Offset(size.width + 10.dp.toPx(), -10.dp.toPx()),
                                        alpha = 0.08f,
                                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4.dp.toPx())
                                    )
                                }

                                Column(
                                    verticalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    Column {
                                        Text(
                                            text = "التعرف بالكاميرا الذكية",
                                            color = Color.White,
                                            fontSize = 22.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "وجه الكاميرا نحو المنتج للحصول على رمز HSCODE جمركياً فوراً بالذكاء الاصطناعي",
                                            color = GeoPrimaryLight,
                                            fontSize = 13.sp,
                                            lineHeight = 18.sp
                                        )
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Button(
                                            onClick = { cameraLauncher.launch() },
                                            modifier = Modifier
                                                .weight(1.1f)
                                                .height(46.dp),
                                            shape = RoundedCornerShape(23.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = GeoGold,
                                                contentColor = Color.White
                                            )
                                        ) {
                                            Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("ابدأ التصوير", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        }

                                        OutlinedButton(
                                            onClick = {
                                                galleryLauncher.launch(
                                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                                )
                                            },
                                            modifier = Modifier
                                                .weight(0.9f)
                                                .height(46.dp),
                                            shape = RoundedCornerShape(23.dp),
                                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.5f)),
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                                        ) {
                                            Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("من المعرض", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }

                        // Selected Image Preview and Analysis Trigger Card
                        if (showSelectedImagePreview && selectedBitmap != null) {
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(24.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    border = BorderStroke(1.dp, GeoBorder)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(20.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "الصورة المختارة جاهزة للتحليل",
                                                fontWeight = FontWeight.Bold,
                                                color = GeoPrimary,
                                                fontSize = 14.sp
                                            )
                                            IconButton(
                                                onClick = { showSelectedImagePreview = false }
                                            ) {
                                                Icon(Icons.Default.Close, contentDescription = "إلغاء", tint = GeoTextSecondary)
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Image(
                                            bitmap = selectedBitmap!!.asImageBitmap(),
                                            contentDescription = "Selected image preview",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(200.dp)
                                                .clip(RoundedCornerShape(16.dp))
                                                .border(1.dp, GeoBorder, RoundedCornerShape(16.dp))
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Button(
                                            onClick = {
                                                viewModel.searchByProductImage(selectedBitmap!!)
                                                showSelectedImagePreview = false
                                            },
                                            shape = RoundedCornerShape(25.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = GeoGold,
                                                contentColor = Color.White
                                            ),
                                            modifier = Modifier.fillMaxWidth().height(50.dp)
                                        ) {
                                            Icon(Icons.Default.Search, contentDescription = null)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("تعرف على الرمز المنسق للمنتج", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                        }
                                    }
                                }
                            }
                        }

                        // Search results and loader
                        item {
                            ActiveResultSection(
                                uiState = uiState,
                                onCopy = { code ->
                                    clipboardManager.setText(AnnotatedString(code))
                                    Toast.makeText(context, "تم نسخ الرمز: $code", Toast.LENGTH_SHORT).show()
                                },
                                onClose = { viewModel.resetState() }
                            )
                        }

                        // Static title for history list
                        if (historyList.isNotEmpty()) {
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.History,
                                            contentDescription = null,
                                            tint = GeoTextPrimary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "عمليات البحث الأخيرة",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            color = GeoTextPrimary
                                        )
                                    }
                                    Text(
                                        text = "مسح الكل",
                                        color = Color(0xFFDC2626),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        modifier = Modifier
                                            .clickable {
                                                viewModel.clearHistory()
                                                Toast.makeText(context, "تم مسح سجل البحث", Toast.LENGTH_SHORT).show()
                                            }
                                            .padding(8.dp)
                                    )
                                }
                            }

                            items(historyList) { historyItem ->
                                HistoryItemCard(
                                    item = historyItem,
                                    isExpanded = activeDetailItem == historyItem,
                                    onItemClick = {
                                        activeDetailItem = if (activeDetailItem == historyItem) null else historyItem
                                    },
                                    onDelete = {
                                        viewModel.deleteHistoryItem(historyItem)
                                        Toast.makeText(context, "تم حذف العنصر", Toast.LENGTH_SHORT).show()
                                    },
                                    onCopy = { code ->
                                        clipboardManager.setText(AnnotatedString(code))
                                        Toast.makeText(context, "تم نسخ الرمز: $code", Toast.LENGTH_SHORT).show()
                                    },
                                    onCalculate = { item ->
                                        calculatorPreloadItem = item
                                        showCalculatorDialog = true
                                    }
                                )
                            }
                        } else {
                            item {
                                EmptyState()
                            }
                        }

                        // Safe padding at the bottom of the scroll list
                        item {
                            Spacer(modifier = Modifier.height(60.dp))
                        }
                    }
                }

                if (showSettingsDialog) {
                    SettingsDialog(
                        viewModel = viewModel,
                        onDismiss = { showSettingsDialog = false },
                        onLogout = {
                            isUserLoggedIn = false
                            showSettingsDialog = false
                        }
                    )
                }

                if (showCalculatorDialog) {
                    ShipmentTariffCalculatorDialog(
                        item = calculatorPreloadItem,
                        onDismiss = {
                            showCalculatorDialog = false
                            calculatorPreloadItem = null
                        }
                    )
                }
            }
        }

        if (showDisclaimerDialog) {
            FirstTimeDisclaimerDialog(
                onConfirm = {
                    viewModel.setDisclaimerAccepted()
                    showDisclaimerDialog = false
                }
            )
        }
    }
}
}

@Composable
fun SettingsDialog(
    viewModel: HsCodeViewModel,
    onDismiss: () -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    var apiKeyInput by remember { mutableStateOf(viewModel.getCustomApiKey()) }
    val clipboardManager = LocalClipboardManager.current

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = {
                    viewModel.saveCustomApiKey(apiKeyInput.trim())
                    Toast.makeText(context, "تم حفظ الإعدادات بنجاح", Toast.LENGTH_SHORT).show()
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = GeoGold,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("حفظ", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("إلغاء", color = GeoTextPrimary)
            }
        },
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    tint = GeoGold,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "إعدادات التطبيق المتقدمة",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = GeoTextPrimary
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // section 1: custom gemini api
                Text(
                    text = "مفتاح ذكاء اصطناعي مخصص (اختياري)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = GeoPrimary
                )
                
                OutlinedTextField(
                    value = apiKeyInput,
                    onValueChange = { apiKeyInput = it },
                    placeholder = {
                        Text(
                            "أدخل مفتاح AI API (AIzaSy...)",
                            color = GeoTextSecondary.copy(alpha = 0.6f),
                            fontSize = 12.sp
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GeoPrimary,
                        unfocusedBorderColor = GeoBorder,
                        focusedTextColor = GeoTextPrimary,
                        unfocusedTextColor = GeoTextPrimary
                    ),
                    singleLine = true
                )

                // section 2: device and license info
                Divider(color = GeoBorder.copy(alpha = 0.5f))
                
                Text(
                    text = "معلومات الترخيص والجهاز",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = GeoPrimary
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(GeoSurfaceLight, RoundedCornerShape(8.dp))
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("مفتاح النشط:", fontSize = 11.sp, color = GeoTextSecondary)
                        Text(viewModel.getActivationKey(), fontSize = 11.sp, color = GeoTextPrimary, fontWeight = FontWeight.Bold)
                    }
                    
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("مُعرّف الجهاز الحالي:", fontSize = 11.sp, color = GeoTextSecondary)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable {
                                clipboardManager.setText(AnnotatedString(viewModel.getDeviceId()))
                                Toast.makeText(context, "تم نسخ مُعرّف الجهاز", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Text(
                                text = viewModel.getDeviceId().take(12) + "...",
                                fontSize = 11.sp,
                                color = GeoGold,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.Default.ContentCopy, contentDescription = null, tint = GeoGold, modifier = Modifier.size(12.dp))
                        }
                    }
                }

                // section 3: developer info
                Divider(color = GeoBorder.copy(alpha = 0.5f))
                
                Text(
                    text = "معلومات المطور",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = GeoPrimary
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(GeoSurfaceLight, RoundedCornerShape(8.dp))
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("المطور:", fontSize = 11.sp, color = GeoTextSecondary)
                        Text("حسن داوود", fontSize = 11.sp, color = GeoTextPrimary, fontWeight = FontWeight.Bold)
                    }

                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("الموقع:", fontSize = 11.sp, color = GeoTextSecondary)
                        Text("مرفأ طرطوس - أمانة الجمارك", fontSize = 11.sp, color = GeoTextPrimary, fontWeight = FontWeight.Bold)
                    }

                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("تيلجرام (TG):", fontSize = 11.sp, color = GeoTextSecondary)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable {
                                clipboardManager.setText(AnnotatedString("@ddfmoto0"))
                                Toast.makeText(context, "تم نسخ معرف تيلجرام", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Text(
                                text = "@ddfmoto0",
                                fontSize = 11.sp,
                                color = GeoGold,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.Default.ContentCopy, contentDescription = null, tint = GeoGold, modifier = Modifier.size(12.dp))
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("واتساب (WA):", fontSize = 11.sp, color = GeoTextSecondary)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable {
                                clipboardManager.setText(AnnotatedString("@ddfmoto"))
                                Toast.makeText(context, "تم نسخ معرف واتساب", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Text(
                                text = "@ddfmoto",
                                fontSize = 11.sp,
                                color = GeoGold,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.Default.ContentCopy, contentDescription = null, tint = GeoGold, modifier = Modifier.size(12.dp))
                        }
                    }
                }

                // logout button
                Button(
                    onClick = {
                        viewModel.logout()
                        Toast.makeText(context, "تم تسجيل الخروج بنجاح", Toast.LENGTH_SHORT).show()
                        onLogout()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFDC2626),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("تسجيل الخروج وإلغاء التنشيط", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
fun ActiveResultSection(
    uiState: SearchUiState,
    onCopy: (String) -> Unit,
    onClose: () -> Unit
) {
    AnimatedVisibility(
        visible = uiState != SearchUiState.Idle,
        enter = fadeIn() + slideInVertically(initialOffsetY = { 40 }),
        exit = fadeOut()
    ) {
        when (uiState) {
            is SearchUiState.Loading -> {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    border = BorderStroke(1.dp, GeoBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            color = GeoPrimary,
                            strokeWidth = 4.dp,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "جاري الاتصال بالذكاء الاصطناعي لتصنيف المنتج...",
                            fontWeight = FontWeight.Bold,
                            color = GeoTextPrimary,
                            textAlign = TextAlign.Center,
                            fontSize = 15.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "قد يستغرق فحص ومطابقة المنتج والبحث عن رمزه المنسق بضع ثوانٍ",
                            color = GeoTextSecondary,
                            textAlign = TextAlign.Center,
                            fontSize = 12.sp
                        )
                    }
                }
            }
            is SearchUiState.Success -> {
                val item = uiState.result
                ResultDetailCard(
                    item = item,
                    title = "تم التعرف على المنتج والرمز الجمركي بنجاح",
                    onCopy = onCopy,
                    onClose = onClose
                )
            }
            is SearchUiState.Error -> {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                    border = BorderStroke(1.dp, Color(0xFFFCA5A5))
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Error",
                                tint = Color(0xFFDC2626),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "فشلت عملية البحث",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFDC2626),
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            IconButton(onClick = onClose) {
                                Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = Color(0xFFDC2626))
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = uiState.message,
                            color = Color(0xFF991B1B),
                            fontSize = 14.sp
                        )
                    }
                }
            }
            SearchUiState.Idle -> {}
        }
    }
}

@Composable
fun ResultDetailCard(
    item: HistoryEntity,
    title: String,
    onCopy: (String) -> Unit,
    onClose: () -> Unit
) {
    val cleanCode = item.hsCode.filter { it.isDigit() }.ifEmpty { item.hsCode.replace(".", "").replace(" ", "").replace("-", "") }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = GeoPrimary), // Elegant custom brand purple
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier.padding(22.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Success",
                        tint = GeoGold,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = title,
                        color = GeoGold,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .size(32.dp)
                        .background(Color.White.copy(alpha = 0.15f), CircleShape)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(16.dp))
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Main Product Display
            Text(
                text = item.productName,
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold
            )

            Spacer(modifier = Modifier.height(14.dp))

            // HS CODE Display Card (Without separators or dots)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(GeoOnPrimaryDark, RoundedCornerShape(16.dp))
                    .padding(horizontal = 18.dp, vertical = 14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "رمز النظام المنسق (HS CODE)",
                            color = GeoPrimaryLight.copy(alpha = 0.7f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = cleanCode,
                            color = GeoGold, // Beautiful gold-accent code without separators
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.5.sp
                        )
                    }
                    IconButton(
                        onClick = { onCopy(cleanCode) },
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                            .size(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "نسخ الكود",
                            tint = GeoGold,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Official Syrian Customs Fees Section (رسم الاستيراد، بدل الخدمات، السلفة الضريبية)
            SyrianCustomsTariffSection(
                fee = item.fee ?: item.dutyRate,
                serviceImportFee = item.serviceImportFee,
                taxAdvanceFee = item.taxAdvanceFee,
                customsItemName = item.customsItemName,
                customsUnit = item.customsUnit,
                isImportAllowed = item.isImportAllowed ?: true
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Shipment Tariff Calculator Section (بالطن أو بالعدد)
            ShipmentTariffCalculatorSection(
                feePerUnitStr = item.fee ?: item.dutyRate,
                servicePerUnitStr = item.serviceImportFee,
                taxAdvancePercentStr = item.taxAdvanceFee,
                customsUnitStr = item.customsUnit,
                productName = item.productName,
                containerColor = GeoOnPrimaryDark
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Details list
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Chapter details
                DetailRow(
                    icon = Icons.Default.Info,
                    label = "الفصل والتبويب",
                    value = item.chapter
                )

                // Confidence
                DetailRow(
                    icon = Icons.Default.LocalFireDepartment,
                    label = "دقة المطابقة",
                    value = item.confidence,
                    valueColor = when (item.confidence) {
                        "عالي" -> Color(0xFF10B981)
                        "متوسط" -> Color(0xFFF59E0B)
                        else -> Color(0xFFEF4444)
                    }
                )

                // Divider
                Divider(color = Color.White.copy(alpha = 0.15f), modifier = Modifier.padding(vertical = 4.dp))

                // Description
                Column {
                    Text(
                        text = "التوضيح والتصنيف الجمركي:",
                        color = GeoPrimaryLight.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = item.description,
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 13.sp,
                        lineHeight = 20.sp
                    )
                }
            }
        }
    }
}

private fun formatDisplayDollar(value: String?): String {
    if (value.isNullOrBlank() || value == "null") return "غير متوفر"
    val clean = value.replace("$", "").replace("%", "").trim()
    return if (clean.isEmpty() || clean == "غير متوفر") "غير متوفر" else "$clean $"
}

private fun formatDisplayPercent(value: String?): String {
    if (value.isNullOrBlank() || value == "null") return "غير متوفر"
    val clean = value.replace("%", "").replace("$", "").trim()
    return if (clean.isEmpty() || clean == "غير متوفر") "غير متوفر" else "$clean%"
}

private fun parseNumericValue(value: String?): Double? {
    if (value.isNullOrBlank() || value == "null" || value == "غير متوفر" || value == "-") return null
    val cleaned = value.replace("$", "").replace("%", "").replace(",", "").trim()
    return cleaned.toDoubleOrNull()
}

private fun formatCurrency(amount: Double): String {
    return if (amount % 1.0 == 0.0) {
        String.format(java.util.Locale.US, "%,d $", amount.toLong())
    } else {
        String.format(java.util.Locale.US, "%,.2f $", amount)
    }
}

@Composable
fun ShipmentTariffCalculatorSection(
    feePerUnitStr: String?,
    servicePerUnitStr: String?,
    taxAdvancePercentStr: String?,
    customsUnitStr: String? = null,
    productName: String? = null,
    containerColor: Color = GeoOnPrimaryDark,
    isLightModeCard: Boolean = false
) {
    val detectedIsCount = remember(customsUnitStr) {
        customsUnitStr == "عدد" || customsUnitStr?.contains("عدد") == true
    }
    var isCountUnit by remember(customsUnitStr) { mutableStateOf(detectedIsCount) }

    val defaultFee = remember(feePerUnitStr) { parseNumericValue(feePerUnitStr) ?: 40.0 }
    val defaultService = remember(servicePerUnitStr) { parseNumericValue(servicePerUnitStr) ?: 5.0 }
    val defaultTax = remember(taxAdvancePercentStr) { parseNumericValue(taxAdvancePercentStr) ?: 2.0 }

    var quantityInput by remember(isCountUnit) {
        mutableStateOf(if (isCountUnit) "100" else "10")
    }
    var feeInput by remember(feePerUnitStr) {
        mutableStateOf(if (defaultFee > 0) defaultFee.toString().removeSuffix(".0") else "40")
    }
    var serviceInput by remember(servicePerUnitStr) {
        mutableStateOf(if (defaultService > 0) defaultService.toString().removeSuffix(".0") else "5")
    }
    var taxInput by remember(taxAdvancePercentStr) {
        mutableStateOf(if (defaultTax > 0) defaultTax.toString().removeSuffix(".0") else "2")
    }
    var showEditRates by remember { mutableStateOf(false) }

    val quantity = quantityInput.toDoubleOrNull() ?: 0.0
    val feeRate = feeInput.toDoubleOrNull() ?: 0.0
    val serviceRate = serviceInput.toDoubleOrNull() ?: 0.0
    val taxRate = taxInput.toDoubleOrNull() ?: 0.0

    // Exact formulas:
    // الرسم الجمركي = الكمية/الوزن × رسم الاستيراد
    val customsDuty = quantity * feeRate
    // بدل الخدمات = الكمية/الوزن × بدل الخدمات
    val serviceFee = quantity * serviceRate
    // الرسم النهائي = الرسم الجمركي + بدل الخدمات
    val finalFee = customsDuty + serviceFee
    // السلفة الضريبية = نسبة مئوية من الرسم النهائي
    val taxAdvance = finalFee * (taxRate / 100.0)
    // المجموع الإجمالي المستحق
    val grandTotal = finalFee + taxAdvance

    val unitLabel = if (isCountUnit) "قطعة" else "طن"
    val rateUnitLabel = if (isCountUnit) "$/قطعة" else "$/طن"
    val calculationModeName = if (isCountUnit) "الحساب بالعدد (قطعة)" else "الحساب بالوزن (طن)"

    val titleTextColor = if (isLightModeCard) GeoTextPrimary else Color.White
    val secondaryTextColor = if (isLightModeCard) GeoTextSecondary else Color.White.copy(alpha = 0.8f)
    val innerBoxColor = if (isLightModeCard) GeoSurfaceLight else Color.White.copy(alpha = 0.10f)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(containerColor, RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Calculate,
                    contentDescription = null,
                    tint = GeoGold,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                    Text(
                        text = "حاسبة الرسوم الجمركية للشحنة",
                        color = GeoGold,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp
                    )
                    Text(
                        text = if (isCountUnit) "الوحدة الجمركية: بالعدد (قطعة)" else "الوحدة الجمركية: بالوزن (طن)",
                        color = secondaryTextColor,
                        fontSize = 10.sp
                    )
                }
            }

            IconButton(
                onClick = { showEditRates = !showEditRates },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = if (showEditRates) Icons.Default.KeyboardArrowUp else Icons.Default.Edit,
                    contentDescription = "تعديل الرسوم",
                    tint = GeoGold,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        // Unit Switcher (بالوزن - طن vs بالعدد - قطعة)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(innerBoxColor, RoundedCornerShape(10.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (!isCountUnit) GeoGold else Color.Transparent)
                    .clickable {
                        isCountUnit = false
                        if (quantityInput == "100" || quantityInput == "500" || quantityInput == "1000") {
                            quantityInput = "10"
                        }
                    }
                    .padding(vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Scale,
                        contentDescription = null,
                        tint = if (!isCountUnit) Color.White else titleTextColor,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "بالوزن (طن)",
                        color = if (!isCountUnit) Color.White else titleTextColor,
                        fontSize = 11.5.sp,
                        fontWeight = if (!isCountUnit) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isCountUnit) GeoGold else Color.Transparent)
                    .clickable {
                        isCountUnit = true
                        if (quantityInput == "10" || quantityInput == "5" || quantityInput == "1" || quantityInput == "20" || quantityInput == "50") {
                            quantityInput = "100"
                        }
                    }
                    .padding(vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Calculate,
                        contentDescription = null,
                        tint = if (isCountUnit) Color.White else titleTextColor,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "بالعدد (قطعة)",
                        color = if (isCountUnit) Color.White else titleTextColor,
                        fontSize = 11.5.sp,
                        fontWeight = if (isCountUnit) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }

        // Quantity / Weight Input Section
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(innerBoxColor, RoundedCornerShape(12.dp))
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isCountUnit) Icons.Default.Calculate else Icons.Default.Scale,
                        contentDescription = null,
                        tint = GeoGold,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isCountUnit) "الكمية المطلوبة:" else "وزن الشحنة:",
                        color = titleTextColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }

                // Quick presets
                val presets = if (isCountUnit) listOf("10", "50", "100", "500", "1000") else listOf("1", "5", "10", "20", "50")
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    presets.forEach { preset ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    if (quantityInput == preset) GeoGold
                                    else (if (isLightModeCard) Color.White else Color.White.copy(alpha = 0.15f))
                                )
                                .clickable { quantityInput = preset }
                                .padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "$preset $unitLabel",
                                color = if (quantityInput == preset) Color.White else titleTextColor,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            OutlinedTextField(
                value = quantityInput,
                onValueChange = { quantityInput = it },
                label = {
                    Text(
                        text = if (isCountUnit) "أدخل عدد القطع / الوحدات" else "أدخل وزن الشحنة بالطن",
                        fontSize = 11.sp
                    )
                },
                trailingIcon = {
                    Text(
                        text = unitLabel,
                        color = GeoGold,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                },
                keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GeoGold,
                    unfocusedBorderColor = if (isLightModeCard) GeoBorder else Color.White.copy(alpha = 0.3f),
                    focusedTextColor = titleTextColor,
                    unfocusedTextColor = titleTextColor,
                    focusedLabelColor = GeoGold,
                    unfocusedLabelColor = secondaryTextColor
                ),
                shape = RoundedCornerShape(10.dp)
            )
        }

        // Editable rates section (if expanded)
        AnimatedVisibility(visible = showEditRates) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(innerBoxColor, RoundedCornerShape(12.dp))
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "تعديل قيم الرسوم والبدلات ($rateUnitLabel):",
                    color = GeoGold,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedTextField(
                        value = feeInput,
                        onValueChange = { feeInput = it },
                        label = { Text("الرسم", fontSize = 9.sp) },
                        trailingIcon = { Text(rateUnitLabel, fontSize = 7.5.sp, color = GeoGold) },
                        keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GeoGold,
                            unfocusedBorderColor = if (isLightModeCard) GeoBorder else Color.White.copy(alpha = 0.3f),
                            focusedTextColor = titleTextColor,
                            unfocusedTextColor = titleTextColor
                        )
                    )
                    OutlinedTextField(
                        value = serviceInput,
                        onValueChange = { serviceInput = it },
                        label = { Text("الخدمات", fontSize = 9.sp) },
                        trailingIcon = { Text(rateUnitLabel, fontSize = 7.5.sp, color = GeoGold) },
                        keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GeoGold,
                            unfocusedBorderColor = if (isLightModeCard) GeoBorder else Color.White.copy(alpha = 0.3f),
                            focusedTextColor = titleTextColor,
                            unfocusedTextColor = titleTextColor
                        )
                    )
                    OutlinedTextField(
                        value = taxInput,
                        onValueChange = { taxInput = it },
                        label = { Text("السلفة", fontSize = 9.sp) },
                        trailingIcon = { Text("%", fontSize = 8.sp, color = GeoGold) },
                        keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GeoGold,
                            unfocusedBorderColor = if (isLightModeCard) GeoBorder else Color.White.copy(alpha = 0.3f),
                            focusedTextColor = titleTextColor,
                            unfocusedTextColor = titleTextColor
                        )
                    )
                }
            }
        }

        // Active Rates Bar Summary
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(innerBoxColor, RoundedCornerShape(8.dp))
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "الرسم: $feeRate $rateUnitLabel",
                color = Color(0xFF34D399),
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "بدل الخدمات: $serviceRate $rateUnitLabel",
                color = Color(0xFF60A5FA),
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "السلفة: $taxRate %",
                color = Color(0xFFFBBF24),
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Results Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    if (isLightModeCard) Color(0xFFF0FDF4) else Color(0xFF064E3B).copy(alpha = 0.65f),
                    RoundedCornerShape(12.dp)
                )
                .border(
                    1.dp,
                    if (isLightModeCard) Color(0xFFBBF7D0) else Color(0xFF34D399).copy(alpha = 0.35f),
                    RoundedCornerShape(12.dp)
                )
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "تفاصيل الحساب ($quantity $unitLabel):",
                    color = if (isLightModeCard) Color(0xFF166534) else Color(0xFF34D399),
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = calculationModeName,
                    color = if (isLightModeCard) Color(0xFF15803D) else Color(0xFF6EE7B7),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Line 1: Customs Duty (الرسم الجمركي)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "الرسم الجمركي ($quantity × $feeRate $):",
                    color = titleTextColor,
                    fontSize = 11.5.sp
                )
                Text(
                    text = formatCurrency(customsDuty),
                    color = titleTextColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Line 2: Service Import Fee (بدل الخدمات)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "بدل الخدمات ($quantity × $serviceRate $):",
                    color = titleTextColor,
                    fontSize = 11.5.sp
                )
                Text(
                    text = formatCurrency(serviceFee),
                    color = titleTextColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Line 3: Final Fee (الرسم النهائي = الرسم الجمركي + بدل الخدمات)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (isLightModeCard) Color(0xFFE0F2FE) else Color(0xFF0369A1).copy(alpha = 0.35f),
                        RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "الرسم النهائي (الرسوم + البدلات):",
                        color = if (isLightModeCard) Color(0xFF0369A1) else Color(0xFF7DD3FC),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = formatCurrency(finalFee),
                        color = if (isLightModeCard) Color(0xFF0369A1) else Color(0xFF7DD3FC),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            // Line 4: Tax Advance (السلفة الضريبية من الرسم النهائي)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "السلفة الضريبية ($taxRate% من الرسم النهائي):",
                    color = titleTextColor,
                    fontSize = 11.5.sp
                )
                Text(
                    text = formatCurrency(taxAdvance),
                    color = Color(0xFFF59E0B),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Divider(
                color = if (isLightModeCard) Color(0xFFCBD5E1) else Color.White.copy(alpha = 0.2f),
                modifier = Modifier.padding(vertical = 2.dp)
            )

            // Line 5: Grand Total (المجموع الإجمالي المستحق)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "المجموع الإجمالي المستحق:",
                    color = GeoGold,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = formatCurrency(grandTotal),
                    color = GeoGold,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }
    }
}

@Composable
fun ShipmentTariffCalculatorDialog(
    item: HistoryEntity? = null,
    onDismiss: () -> Unit
) {
    val isCount = item?.customsUnit == "عدد" || item?.customsUnit?.contains("عدد") == true
    val unitLabel = if (isCount) "بالعدد (قطعة)" else "بالوزن (طن)"

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = GeoGold,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("إغلاق", fontWeight = FontWeight.Bold)
            }
        },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Calculate,
                    contentDescription = null,
                    tint = GeoPrimary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "حاسبة الرسوم الجمركية للشحنة",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = GeoTextPrimary
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (item != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = GeoSurfaceLight),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "السلعة: ${item.productName}",
                                    fontWeight = FontWeight.Bold,
                                    color = GeoTextPrimary,
                                    fontSize = 13.sp,
                                    modifier = Modifier.weight(1f)
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isCount) Color(0xFFDBEAFE) else Color(0xFFDCFCE7))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = unitLabel,
                                        color = if (isCount) Color(0xFF1E40AF) else Color(0xFF166534),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "الرمز الجمركي: ${item.hsCode.filter { it.isDigit() }}",
                                    color = GeoPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )

                                if (item.isImportAllowed == false) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Color(0xFFFEE2E2))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "يوجد حجر استيراد",
                                            color = Color(0xFFB91C1C),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Color(0xFFDCFCE7))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "مسموح الاستيراد",
                                            color = Color(0xFF15803D),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                ShipmentTariffCalculatorSection(
                    feePerUnitStr = item?.fee ?: item?.dutyRate ?: "40",
                    servicePerUnitStr = item?.serviceImportFee ?: "5",
                    taxAdvancePercentStr = item?.taxAdvanceFee ?: "2",
                    customsUnitStr = item?.customsUnit,
                    productName = item?.productName,
                    containerColor = Color.White,
                    isLightModeCard = true
                )
            }
        },
        shape = RoundedCornerShape(20.dp),
        containerColor = Color.White
    )
}

@Composable
fun SyrianCustomsTariffSection(
    fee: String?,
    serviceImportFee: String?,
    taxAdvanceFee: String?,
    customsItemName: String?,
    customsUnit: String? = null,
    isImportAllowed: Boolean = true,
    containerColor: Color = GeoOnPrimaryDark
) {
    val isCount = customsUnit == "عدد" || customsUnit?.contains("عدد") == true
    val unitSuffix = if (isCount) "$/قطعة" else "$/طن"
    val unitDisplayTitle = if (isCount) "بالعدد (قطعة)" else "بالوزن (طن)"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(containerColor, RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Status Alert (مسموح الاستيراد vs يوجد حجر على استيراد هذه المادة)
        if (!isImportAllowed) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFEF4444).copy(alpha = 0.22f))
                    .border(1.2.dp, Color(0xFFEF4444), RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Block,
                    contentDescription = "حجر استيراد",
                    tint = Color(0xFFF87171),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "يوجد حجر على استيراد هذه المادة",
                        color = Color(0xFFFCA5A5),
                        fontWeight = FontWeight.Black,
                        fontSize = 12.5.sp
                    )
                    Text(
                        text = "الحالة الجمركية: غير مسموح بالاستيراد (isActive = False)",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 10.sp
                    )
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF10B981).copy(alpha = 0.18f))
                    .border(1.dp, Color(0xFF34D399).copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "مسموح",
                    tint = Color(0xFF34D399),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "مسموح استيراد هذه المادة (isActive = True)",
                    color = Color(0xFF6EE7B7),
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.5.sp
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AccountBalance,
                    contentDescription = null,
                    tint = GeoGold,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "الرسوم والبدلات (الجمارك السورية)",
                    color = GeoGold,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (isCount) Color(0xFF3B82F6).copy(alpha = 0.25f) else Color(0xFF10B981).copy(alpha = 0.25f))
                    .border(
                        1.dp,
                        if (isCount) Color(0xFF60A5FA).copy(alpha = 0.6f) else Color(0xFF34D399).copy(alpha = 0.6f),
                        RoundedCornerShape(6.dp)
                    )
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "الوحدة: $unitDisplayTitle",
                    color = if (isCount) Color(0xFF93C5FD) else Color(0xFF6EE7B7),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (!customsItemName.isNullOrBlank()) {
            Text(
                text = "الوصف المعتمد: $customsItemName",
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 16.sp
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TariffMetricBox(
                title = "رسم الاستيراد",
                unit = unitSuffix,
                value = formatDisplayDollar(fee),
                modifier = Modifier.weight(1f),
                badgeColor = Color(0xFF34D399)
            )
            TariffMetricBox(
                title = "بدل الخدمات",
                unit = unitSuffix,
                value = formatDisplayDollar(serviceImportFee),
                modifier = Modifier.weight(1f),
                badgeColor = Color(0xFF60A5FA)
            )
            TariffMetricBox(
                title = "السلفة الضريبية",
                unit = "نسبة مئوية (%)",
                value = formatDisplayPercent(taxAdvanceFee),
                modifier = Modifier.weight(1f),
                badgeColor = Color(0xFFFBBF24)
            )
        }
    }
}

@Composable
fun TariffMetricBox(
    title: String,
    unit: String,
    value: String,
    modifier: Modifier = Modifier,
    badgeColor: Color = GeoGold
) {
    Box(
        modifier = modifier
            .background(Color.White.copy(alpha = 0.12f), RoundedCornerShape(10.dp))
            .padding(vertical = 8.dp, horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = unit,
                color = Color.White.copy(alpha = 0.65f),
                fontSize = 8.5.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                color = badgeColor,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun DetailRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    valueColor: Color = Color.White
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = GeoGold,
            modifier = Modifier
                .size(16.dp)
                .padding(top = 2.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(1.dp))
            Text(
                text = value,
                color = valueColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun HistoryItemCard(
    item: HistoryEntity,
    isExpanded: Boolean,
    onItemClick: () -> Unit,
    onDelete: () -> Unit,
    onCopy: (String) -> Unit,
    onCalculate: (HistoryEntity) -> Unit
) {
    val cleanCode = item.hsCode.filter { it.isDigit() }.ifEmpty { item.hsCode.replace(".", "").replace(" ", "").replace("-", "") }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onItemClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, GeoBorder)
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // If camera-based, display local image thumbnail
                if (item.imageUrl != null) {
                    val file = File(item.imageUrl)
                    if (file.exists()) {
                        AsyncImage(
                            model = file,
                            contentDescription = "Product thumbnail",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(54.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.dp, GeoBorder, RoundedCornerShape(8.dp))
                        )
                    } else {
                        PlaceholderThumbnail()
                    }
                } else {
                    PlaceholderThumbnailText()
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = item.productName,
                        fontWeight = FontWeight.Bold,
                        color = GeoTextPrimary,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "HS CODE: ",
                            color = GeoTextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = cleanCode,
                            color = GeoPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        if (item.isImportAllowed == false) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFFFEE2E2))
                                    .padding(horizontal = 5.dp, vertical = 1.5.dp)
                            ) {
                                Text(
                                    text = "حجر استيراد",
                                    color = Color(0xFFB91C1C),
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else if (item.fee != null || item.isImportAllowed == true) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFFDCFCE7))
                                    .padding(horizontal = 5.dp, vertical = 1.5.dp)
                            ) {
                                Text(
                                    text = "مسموح",
                                    color = Color(0xFF15803D),
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { onCalculate(item) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Calculate,
                            contentDescription = "حساب الرسوم",
                            tint = GeoPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = { onCopy(cleanCode) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "نسخ الرمز",
                            tint = GeoTextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "حذف من السجل",
                            tint = Color(0xFFEF4444).copy(alpha = 0.8f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Quick Tariff Summary Row
            if (item.fee != null || item.serviceImportFee != null || item.taxAdvanceFee != null) {
                val isCount = item.customsUnit == "عدد" || item.customsUnit?.contains("عدد") == true
                val unitSuffix = if (isCount) "قطعة" else "طن"
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(GeoSurfaceLight, RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "الرسم: ${formatDisplayDollar(item.fee ?: item.dutyRate)}/$unitSuffix",
                        color = Color(0xFF059669),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "بدل الخدمات: ${formatDisplayDollar(item.serviceImportFee)}/$unitSuffix",
                        color = Color(0xFF2563EB),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "السلفة: ${formatDisplayPercent(item.taxAdvanceFee)}",
                        color = Color(0xFFD97706),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                        .background(GeoSurfaceLight, RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "تفاصيل التصنيف:",
                        fontWeight = FontWeight.Bold,
                        color = GeoTextPrimary,
                        fontSize = 12.sp
                    )
                    Text(
                        text = item.description,
                        color = GeoTextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                    if (!item.customsItemName.isNullOrBlank()) {
                        Text(
                            text = "اسم السلعة الجمركي: ${item.customsItemName}",
                            color = GeoPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Divider(color = GeoBorder)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "الفصل: ${item.chapter}",
                            color = GeoTextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "الرسوم: ${item.fee ?: item.dutyRate}",
                            color = GeoTextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Button(
                        onClick = { onCalculate(item) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GeoPrimary,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Calculate,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "حساب الرسوم الإجمالية للشحنة (بالطن)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PlaceholderThumbnail() {
    Box(
        modifier = Modifier
            .size(54.dp)
            .background(GeoSurfaceLight, RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.CameraAlt,
            contentDescription = null,
            tint = GeoTextSecondary,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
fun PlaceholderThumbnailText() {
    Box(
        modifier = Modifier
            .size(54.dp)
            .background(GeoPrimaryLight, RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = null,
            tint = GeoPrimary,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
fun EmptyState() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, GeoBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = GeoTextSecondary.copy(alpha = 0.4f),
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "السجل فارغ حالياً",
                fontWeight = FontWeight.Bold,
                color = GeoTextPrimary,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "أدخل اسم منتج أو التقط له صورة لتبدأ الاستكشاف",
                color = GeoTextSecondary,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

// Helper utility to convert a Uri to a Bitmap
private fun uriToBitmap(context: Context, uri: Uri): Bitmap? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri)
        BitmapFactory.decodeStream(inputStream)
    } catch (e: Exception) {
        null
    }
}

@Composable
fun ActivationScreen(
    viewModel: HsCodeViewModel,
    onActivationSuccess: () -> Unit
) {
    val context = LocalContext.current
    var activationKeyInput by remember { mutableStateOf("") }
    var isVerifying by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    val clipboardManager = LocalClipboardManager.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(GeoBackground)
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Elegant Syrian Visual Identity Header
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .background(Color.White, RoundedCornerShape(20.dp))
                    .border(2.dp, GeoGold, RoundedCornerShape(20.dp))
                    .clip(RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = R.drawable.app_logo_fg,
                    contentDescription = "Logo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Text(
                text = "مستكشف الرموز الجمركية",
                color = GeoTextPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Text(
                text = "تنشيط رخصة التطبيق وحماية الأجهزة",
                color = GeoGold,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                border = BorderStroke(1.dp, GeoBorder),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "يتطلب هذا التطبيق تفعيلًا بمفتاح ترخيص مخصص. يرتبط كل مفتاح بجهاز واحد فقط لحماية الترخيص والاستخدام العادل للذكاء الاصطناعي.",
                        color = GeoTextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        textAlign = TextAlign.Center
                    )

                    OutlinedTextField(
                        value = activationKeyInput,
                        onValueChange = { 
                            activationKeyInput = it
                            errorMessage = null
                        },
                        label = { Text("مفتاح التفعيل الخاص بك", fontSize = 12.sp) },
                        placeholder = { Text("أدخل مفتاح الترخيص (مثال: TP1049 أو DEMO12345)", fontSize = 11.sp, color = GeoTextSecondary.copy(alpha = 0.5f)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GeoPrimary,
                            unfocusedBorderColor = GeoBorder,
                            focusedTextColor = GeoTextPrimary,
                            unfocusedTextColor = GeoTextPrimary
                        ),
                        singleLine = true
                    )

                    if (errorMessage != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFFEF2F2), RoundedCornerShape(8.dp))
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color(0xFFDC2626),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = errorMessage!!,
                                color = Color(0xFFDC2626),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    if (successMessage != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFECFDF5), RoundedCornerShape(8.dp))
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF059669),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = successMessage!!,
                                color = Color(0xFF059669),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Button(
                        onClick = {
                            if (activationKeyInput.trim().isEmpty()) {
                                errorMessage = "يرجى إدخال مفتاح التفعيل أولاً"
                                return@Button
                            }
                            isVerifying = true
                            errorMessage = null
                            successMessage = null
                            
                            viewModel.verifyActivationKey(activationKeyInput.trim()) { success, msg ->
                                isVerifying = false
                                if (success) {
                                    successMessage = msg
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                    onActivationSuccess()
                                } else {
                                    errorMessage = msg
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GeoGold,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        enabled = !isVerifying
                    ) {
                        if (isVerifying) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = "تنشيط التطبيق الآن",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }

            // Current Device Signature Info Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "مُعرّف توقيع هذا الجهاز الحالي:",
                    color = GeoTextSecondary,
                    fontSize = 10.sp
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(GeoSurfaceLight, RoundedCornerShape(6.dp))
                        .clickable {
                            clipboardManager.setText(AnnotatedString(viewModel.getDeviceId()))
                            Toast.makeText(context, "تم نسخ مُعرّف الجهاز بنجاح", Toast.LENGTH_SHORT).show()
                        }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = viewModel.getDeviceId(),
                        color = GeoTextPrimary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy Device ID",
                        tint = GeoGold,
                        modifier = Modifier.size(10.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun FirstTimeDisclaimerDialog(
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { /* Modal: require user to acknowledge */ },
        icon = {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFEF3C7)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "تنبيه إخلاء المسؤولية",
                    tint = Color(0xFFD97706),
                    modifier = Modifier.size(30.dp)
                )
            }
        },
        title = {
            Text(
                text = "تنبيه وإخلاء مسؤولية هـام",
                fontWeight = FontWeight.Bold,
                color = GeoTextPrimary,
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB)),
                    border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "• إن هذا التطبيق غير رسمي وغير معتمد من قبل الهيئة أو الجهات الحكومية الرسمية.",
                            color = Color(0xFF92400E),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.5.sp,
                            lineHeight = 18.sp
                        )
                        Text(
                            text = "• وعليه فإن أي إساءة استخدام أو مسؤولية أو قرارات جمركية/مالية تقع على عاتق المستخدم وحده.",
                            color = Color(0xFF78350F),
                            fontSize = 12.sp,
                            lineHeight = 17.sp
                        )
                        Text(
                            text = "• التطبيق أداة استرشادية مساعدة للبحث والتصنيف بالذكاء الاصطناعي، ويجب دائماً مراجعة الجهات الجمركية المختصة والأنظمة النافذة.",
                            color = GeoTextSecondary,
                            fontSize = 11.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = GeoPrimary,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "فهمت ذلك وأوافق على المتابعة",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(20.dp)
    )
}

