package com.example.ui

import android.Manifest
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.foundation.Canvas
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.Task
import com.example.worker.ReminderWorker
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import com.example.ui.theme.*

import com.example.BuildConfig
import coil.compose.AsyncImage
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.io.InputStream
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.Image
import androidx.compose.material.icons.filled.Image

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskListScreen(
    viewModel: TaskViewModel = viewModel(),
    authViewModel: AuthViewModel = viewModel()
) {
    val tasks by viewModel.uiState.collectAsStateWithLifecycle(initialValue = emptyList())
    val categories by viewModel.categories.collectAsStateWithLifecycle(initialValue = emptyList())
    val user by authViewModel.user.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    var showAddDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var selectedCategoryFilter by remember { mutableStateOf<String?>(null) }
    var editingTask by remember { mutableStateOf<Task?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = {}
    )

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val filteredTasks = if (selectedCategoryFilter == null) tasks else tasks.filter { it.category == selectedCategoryFilter }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(top = 32.dp, bottom = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Today's Focus",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground,
                            letterSpacing = (-0.5).sp
                        )
                        val dateString = SimpleDateFormat("EEEE, MMM dd", Locale.getDefault()).format(Date())
                        Text(
                            text = dateString.uppercase(),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 1.5.sp,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                        
                        val activeCount = tasks.count { !it.isCompleted }
                        Text(
                            text = if (activeCount == 0) "All done for today!" else "$activeCount active tasks",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    
                    // Avatar, Theme Toggle & About
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { showAboutDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "About",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        
                        val isDarkTheme by viewModel.isDarkTheme.collectAsStateWithLifecycle()
                        IconButton(onClick = { viewModel.toggleTheme() }) {
                            Icon(
                                imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                                contentDescription = if (isDarkTheme) "Switch Theme" else "Switch Theme",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        if (user != null) {
                            var menuExpanded by remember { mutableStateOf(false) }
                            Box {
                                if (user?.photoUrl != null) {
                                    AsyncImage(
                                        model = user?.photoUrl,
                                        contentDescription = "User Avatar",
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .border(2.dp, MaterialTheme.colorScheme.outline, CircleShape)
                                            .clickable { menuExpanded = true }
                                    )
                                } else {
                                    val initials = (user?.displayName ?: user?.email ?: "U").take(1).uppercase()
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primaryContainer)
                                            .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                            .clickable { menuExpanded = true },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = initials,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                DropdownMenu(
                                    expanded = menuExpanded,
                                    onDismissRequest = { menuExpanded = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Sign Out") },
                                        onClick = { 
                                            menuExpanded = false
                                            authViewModel.signOut()
                                        }
                                    )
                                }
                            }
                        } else {
                            TextButton(onClick = {
                                val clientId = BuildConfig.WEB_CLIENT_ID
                                if (clientId.isBlank()) {
                                    Toast.makeText(context, "WEB_CLIENT_ID not configured", Toast.LENGTH_SHORT).show()
                                } else {
                                    coroutineScope.launch {
                                        val errorMsg = authViewModel.signInWithGoogle(context, clientId)
                                        if (errorMsg != null) {
                                            Toast.makeText(context, "Sign-in failed: $errorMsg", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                }
                            }) {
                                Text("Sign In")
                            }
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { 
                    showAddDialog = true 
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Task")
            }
        },
        contentWindowInsets = WindowInsets.systemBars
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
        ) {
            // Custom Tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                CategoryTab(
                    text = "All",
                    isSelected = selectedCategoryFilter == null,
                    onClick = { selectedCategoryFilter = null }
                )
                categories.take(3).forEach { category ->
                    CategoryTab(
                        text = category,
                        isSelected = selectedCategoryFilter == category,
                        onClick = { selectedCategoryFilter = category }
                    )
                }
            }

            if (filteredTasks.isEmpty()) {
                EmptyStateComponent(onAddTaskClick = { showAddDialog = true })
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(filteredTasks, key = { it.id }) { task ->
                        TaskItem(
                            task = task,
                            onToggle = { viewModel.toggleTaskStatus(task) },
                            onEdit = { editingTask = task },
                            onDelete = { viewModel.deleteTask(task) }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddTaskDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { title, desc, cat, reminderTime, photoBase64 ->
                viewModel.addTask(title, desc, cat, reminderTime, photoBase64)
                if (reminderTime != null) {
                    ReminderWorker.scheduleReminderAtTime(context, title, reminderTime)
                }
                showAddDialog = false
            },
            onAutoTag = { titleText, descText, callback ->
                viewModel.suggestCategory(titleText, descText, callback) { error ->
                    Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                }
            }
        )
    }

    if (editingTask != null) {
        EditTaskDialog(
            task = editingTask!!,
            onDismiss = { editingTask = null },
            onSave = { title, desc, cat, reminderTime, photoBase64 ->
                viewModel.updateTask(editingTask!!.copy(title = title, description = desc, category = cat, reminderTime = reminderTime, photoBase64 = photoBase64))
                if (reminderTime != null) {
                    ReminderWorker.scheduleReminderAtTime(context, title, reminderTime)
                }
                editingTask = null
            },
            onAutoTag = { titleText, descText, callback ->
                viewModel.suggestCategory(titleText, descText, callback) { error ->
                    Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                }
            }
        )
    }

    if (showAboutDialog) {
        AboutDialog(onDismiss = { showAboutDialog = false })
    }
}

@Composable
fun AboutDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text("About", color = MaterialTheme.colorScheme.onBackground)
            }
        },
        text = {
            Column {
                Text("Developer Details:", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Name: Jihad Ahmed", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Contact: tensionidk@gmail.com", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Build version: 1.0", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Close")
            }
        },
        containerColor = MaterialTheme.colorScheme.surfaceVariant
    )
}

@Composable
fun CategoryTab(text: String, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .background(
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 14.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun TaskItem(task: Task, onToggle: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    val animatedAlpha by animateFloatAsState(
        targetValue = if (task.isCompleted) 0.6f else 1f,
        animationSpec = tween(durationMillis = 400),
        label = "taskAlpha"
    )
    val checkboxColor by animateColorAsState(
        targetValue = if (task.isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
        animationSpec = tween(durationMillis = 300),
        label = "checkboxColor"
    )
    val checkmarkAlpha by animateFloatAsState(
        targetValue = if (task.isCompleted) 1f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "checkmarkAlpha"
    )
    val strikethroughProgress by animateFloatAsState(
        targetValue = if (task.isCompleted) 1f else 0f,
        animationSpec = tween(durationMillis = 400),
        label = "strikethroughProgress"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(animatedAlpha)
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(28.dp)
            )
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(28.dp)
            )
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Checkbox mapping
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(Color.Transparent, RoundedCornerShape(6.dp))
                    .border(
                        width = 2.dp,
                        color = checkboxColor,
                        shape = RoundedCornerShape(6.dp)
                    )
                    .clickable { onToggle() },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .alpha(checkmarkAlpha)
                        .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp))
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                val textColor = MaterialTheme.colorScheme.onSurface
                Text(
                    text = task.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 20.sp,
                    color = textColor,
                    modifier = Modifier.drawWithContent {
                        drawContent()
                        if (strikethroughProgress > 0f) {
                            val y = size.height / 2f
                            drawLine(
                                color = textColor.copy(alpha = 0.6f),
                                start = Offset(0f, y),
                                end = Offset(size.width * strikethroughProgress, y),
                                strokeWidth = 2.dp.toPx()
                            )
                        }
                    }
                )
                if (task.category.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Category Badge
                        val hash = kotlin.math.abs(task.category.hashCode())
                        val (bgColor, textColor) = when (hash % 3) {
                            0 -> CategoryBlueBg to CategoryBlueText
                            1 -> CategoryRedBg to CategoryRedText
                            else -> CategoryGrayBg to CategoryGrayText
                        }
                        Box(
                            modifier = Modifier
                                .background(bgColor, RoundedCornerShape(12.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = task.category,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = textColor,
                                letterSpacing = (-0.5).sp
                            )
                        }
                    }
                }
                if (task.reminderTime != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Reminder Time",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        val sdf = java.text.SimpleDateFormat("MMM dd, yyyy, hh:mm a", java.util.Locale.getDefault())
                        Text(
                            text = sdf.format(java.util.Date(task.reminderTime)),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            
            if (!task.photoBase64.isNullOrBlank()) {
                val bitmap = rememberBitmapFromBase64(task.photoBase64)
                if (bitmap != null) {
                    var showExpandedPhoto by remember { mutableStateOf(false) }
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    Image(
                        bitmap = bitmap,
                        contentDescription = "Task Thumbnail",
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { showExpandedPhoto = true }
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )
                    
                    if (showExpandedPhoto) {
                        AlertDialog(
                            onDismissRequest = { showExpandedPhoto = false },
                            confirmButton = {
                                TextButton(onClick = { showExpandedPhoto = false }) {
                                    Text("Close")
                                }
                            },
                            title = { Text(task.title, color = MaterialTheme.colorScheme.onBackground) },
                            text = {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(1f)
                                        .clip(RoundedCornerShape(16.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Image(
                                        bitmap = bitmap,
                                        contentDescription = "Expanded Task Photo",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Fit
                                    )
                                }
                            },
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                }
            }
            
            IconButton(onClick = onEdit) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit Task",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete Task",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

fun showDateTimePicker(
    context: android.content.Context,
    initialTime: Long? = null,
    onDateTimeSelected: (Long) -> Unit
) {
    val calendar = java.util.Calendar.getInstance()
    if (initialTime != null) {
        calendar.timeInMillis = initialTime
    }
    
    val datePickerDialog = android.app.DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            calendar.set(java.util.Calendar.YEAR, year)
            calendar.set(java.util.Calendar.MONTH, month)
            calendar.set(java.util.Calendar.DAY_OF_MONTH, dayOfMonth)
            
            val timePickerDialog = android.app.TimePickerDialog(
                context,
                { _, hourOfDay, minute ->
                    calendar.set(java.util.Calendar.HOUR_OF_DAY, hourOfDay)
                    calendar.set(java.util.Calendar.MINUTE, minute)
                    calendar.set(java.util.Calendar.SECOND, 0)
                    calendar.set(java.util.Calendar.MILLISECOND, 0)
                    onDateTimeSelected(calendar.timeInMillis)
                },
                calendar.get(java.util.Calendar.HOUR_OF_DAY),
                calendar.get(java.util.Calendar.MINUTE),
                false
            )
            timePickerDialog.show()
        },
        calendar.get(java.util.Calendar.YEAR),
        calendar.get(java.util.Calendar.MONTH),
        calendar.get(java.util.Calendar.DAY_OF_MONTH)
    )
    datePickerDialog.show()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskDialog(
    onDismiss: () -> Unit,
    onAdd: (String, String, String, Long?, String?) -> Unit,
    onAutoTag: (String, String, (String) -> Unit) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var reminderTime by remember { mutableStateOf<Long?>(null) }
    var isTagging by remember { mutableStateOf(false) }
    var photoBase64 by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        if (uri != null) {
            photoBase64 = uriToBase64(context, uri)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Task", color = MaterialTheme.colorScheme.onBackground) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Task Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (Optional)") },
                    singleLine = false,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Category (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        if (isTagging) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            IconButton(onClick = {
                                if (title.isNotBlank()) {
                                    isTagging = true
                                    onAutoTag(title, description) { suggestedCategory ->
                                        category = suggestedCategory
                                        isTagging = false
                                    }
                                }
                            }) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = "Auto Tag",
                                    tint = MaterialTheme.colorScheme.primary
                               )
                            }
                        }
                    }
                )
                
                // Photo Attachment Row
                if (photoBase64 != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .clip(RoundedCornerShape(12.dp))
                    ) {
                        val bitmap = rememberBitmapFromBase64(photoBase64)
                        if (bitmap != null) {
                            Image(
                                bitmap = bitmap,
                                contentDescription = "Task Photo Preview",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                        IconButton(
                            onClick = { photoBase64 = null },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Remove Photo",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                } else {
                    OutlinedButton(
                        onClick = { imagePickerLauncher.launch("image/*") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add Photo Attachment")
                    }
                }
                
                // Reminder Selection Row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            showDateTimePicker(context, reminderTime) { selectedTime ->
                                reminderTime = selectedTime
                            }
                        }
                        .padding(vertical = 12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "Set Reminder",
                        tint = if (reminderTime != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Specific Reminder Time",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        val reminderText = if (reminderTime != null) {
                            val sdf = java.text.SimpleDateFormat("MMM dd, yyyy, hh:mm a", java.util.Locale.getDefault())
                            sdf.format(java.util.Date(reminderTime!!))
                        } else {
                            "Not Set"
                        }
                        Text(
                            text = reminderText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (reminderTime != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (reminderTime != null) {
                        IconButton(onClick = { reminderTime = null }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear Reminder",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    } else {
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Pick Date and Time",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onAdd(title, description, category, reminderTime, photoBase64)
                    }
                }
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        containerColor = MaterialTheme.colorScheme.surfaceVariant
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTaskDialog(
    task: Task,
    onDismiss: () -> Unit,
    onSave: (String, String, String, Long?, String?) -> Unit,
    onAutoTag: (String, String, (String) -> Unit) -> Unit
) {
    var title by remember { mutableStateOf(task.title) }
    var description by remember { mutableStateOf(task.description) }
    var category by remember { mutableStateOf(task.category) }
    var reminderTime by remember { mutableStateOf(task.reminderTime) }
    var isTagging by remember { mutableStateOf(false) }
    var photoBase64 by remember { mutableStateOf<String?>(task.photoBase64) }
    val context = LocalContext.current

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        if (uri != null) {
            photoBase64 = uriToBase64(context, uri)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Task", color = MaterialTheme.colorScheme.onBackground) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Task Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (Optional)") },
                    singleLine = false,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Category (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        if (isTagging) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            IconButton(onClick = {
                                if (title.isNotBlank()) {
                                    isTagging = true
                                    onAutoTag(title, description) { suggestedCategory ->
                                        category = suggestedCategory
                                        isTagging = false
                                    }
                                }
                            }) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = "Auto Tag",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                )
                
                // Photo Attachment Row
                if (photoBase64 != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .clip(RoundedCornerShape(12.dp))
                    ) {
                        val bitmap = rememberBitmapFromBase64(photoBase64)
                        if (bitmap != null) {
                            Image(
                                bitmap = bitmap,
                                contentDescription = "Task Photo Preview",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                        IconButton(
                            onClick = { photoBase64 = null },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Remove Photo",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                } else {
                    OutlinedButton(
                        onClick = { imagePickerLauncher.launch("image/*") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add Photo Attachment")
                    }
                }
                
                // Reminder Selection Row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            showDateTimePicker(context, reminderTime) { selectedTime ->
                                reminderTime = selectedTime
                            }
                        }
                        .padding(vertical = 12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "Set Reminder",
                        tint = if (reminderTime != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Specific Reminder Time",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        val reminderText = if (reminderTime != null) {
                            val sdf = java.text.SimpleDateFormat("MMM dd, yyyy, hh:mm a", java.util.Locale.getDefault())
                            sdf.format(java.util.Date(reminderTime!!))
                        } else {
                            "Not Set"
                        }
                        Text(
                            text = reminderText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (reminderTime != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (reminderTime != null) {
                        IconButton(onClick = { reminderTime = null }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear Reminder",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    } else {
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Pick Date and Time",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onSave(title, description, category, reminderTime, photoBase64)
                    }
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        containerColor = MaterialTheme.colorScheme.surfaceVariant
    )
}

@Composable
fun EmptyStateComponent(
    onAddTaskClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "EmptyStateBounce")
    
    // Smooth floating animation
    val offsetY by infiniteTransition.animateFloat(
        initialValue = -8f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "offsetY"
    )

    // Pulse animation for sparkles
    val sparkleScale by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sparkleScale"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // High-fidelity Canvas Illustration
        val primaryColor = MaterialTheme.colorScheme.primary
        val secondaryColor = MaterialTheme.colorScheme.secondary
        val tertiaryColor = MaterialTheme.colorScheme.tertiary
        val surfaceVariantColor = MaterialTheme.colorScheme.surfaceVariant
        val onSurfaceColor = MaterialTheme.colorScheme.onSurface

        Box(
            modifier = Modifier
                .size(180.dp)
                .offset(y = offsetY.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val centerOffset = Offset(size.width / 2, size.height / 2)
                
                // 1. Draw elegant background glowing aura
                drawCircle(
                    color = primaryColor.copy(alpha = 0.08f),
                    radius = size.width * 0.45f,
                    center = centerOffset
                )
                
                // 2. Draw outer dashed circular ring
                drawCircle(
                    color = primaryColor.copy(alpha = 0.25f),
                    radius = size.width * 0.38f,
                    center = centerOffset,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                        width = 2.dp.toPx(),
                        pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                            floatArrayOf(12f, 12f), 0f
                        )
                    )
                )

                // 3. Draw a sleek modern clipboard/document outline
                val padWidth = size.width * 0.32f
                val padHeight = size.height * 0.42f
                val padLeft = centerOffset.x - padWidth / 2f
                val padTop = centerOffset.y - padHeight / 2f
                
                val padPath = androidx.compose.ui.graphics.Path().apply {
                    addRoundRect(
                        androidx.compose.ui.geometry.RoundRect(
                            rect = androidx.compose.ui.geometry.Rect(
                                left = padLeft,
                                top = padTop,
                                right = padLeft + padWidth,
                                bottom = padTop + padHeight
                            ),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(12.dp.toPx())
                        )
                    )
                }
                
                // Draw pad filled background
                drawPath(
                    path = padPath,
                    color = surfaceVariantColor
                )
                // Draw pad outline
                drawPath(
                    path = padPath,
                    color = primaryColor.copy(alpha = 0.7f),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                        width = 3.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                )

                // 4. Draw sleek clipboard top clamp
                val clampWidth = padWidth * 0.45f
                val clampHeight = size.height * 0.07f
                val clampLeft = centerOffset.x - clampWidth / 2f
                val clampTop = padTop - clampHeight * 0.3f
                val clampPath = androidx.compose.ui.graphics.Path().apply {
                    addRoundRect(
                        androidx.compose.ui.geometry.RoundRect(
                            rect = androidx.compose.ui.geometry.Rect(
                                left = clampLeft,
                                top = clampTop,
                                right = clampLeft + clampWidth,
                                bottom = clampTop + clampHeight
                            ),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx())
                        )
                    )
                }
                drawPath(
                    path = clampPath,
                    color = secondaryColor
                )

                // 5. Draw decorative lines inside clipboard
                val startX = padLeft + 12.dp.toPx()
                val endX = padLeft + padWidth - 12.dp.toPx()
                val firstLineY = padTop + 24.dp.toPx()
                val secondLineY = padTop + 36.dp.toPx()
                val thirdLineY = padTop + 48.dp.toPx()

                drawLine(
                    color = onSurfaceColor.copy(alpha = 0.15f),
                    start = Offset(startX, firstLineY),
                    end = Offset(endX, firstLineY),
                    strokeWidth = 3.dp.toPx(),
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = onSurfaceColor.copy(alpha = 0.15f),
                    start = Offset(startX, secondLineY),
                    end = Offset(endX - 20.dp.toPx(), secondLineY),
                    strokeWidth = 3.dp.toPx(),
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = onSurfaceColor.copy(alpha = 0.15f),
                    start = Offset(startX, thirdLineY),
                    end = Offset(endX - 10.dp.toPx(), thirdLineY),
                    strokeWidth = 3.dp.toPx(),
                    cap = StrokeCap.Round
                )

                // 6. Draw a shiny success checkmark badge overlay
                val badgeRadius = 18.dp.toPx()
                val badgeCenter = Offset(padLeft + padWidth - 4.dp.toPx(), padTop + padHeight - 4.dp.toPx())
                drawCircle(
                    color = primaryColor,
                    radius = badgeRadius,
                    center = badgeCenter
                )
                // Draw checkmark inside badge
                val checkPath = androidx.compose.ui.graphics.Path().apply {
                    moveTo(badgeCenter.x - 7.dp.toPx(), badgeCenter.y)
                    lineTo(badgeCenter.x - 2.dp.toPx(), badgeCenter.y + 5.dp.toPx())
                    lineTo(badgeCenter.x + 8.dp.toPx(), badgeCenter.y - 5.dp.toPx())
                }
                drawPath(
                    path = checkPath,
                    color = androidx.compose.ui.graphics.Color.White,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                        width = 2.5.dp.toPx(),
                        cap = StrokeCap.Round,
                        join = androidx.compose.ui.graphics.StrokeJoin.Round
                    )
                )

                // 7. Draw floating sparkle dots around
                // Left Sparkle
                drawCircle(
                    color = tertiaryColor.copy(alpha = 0.8f),
                    radius = 5.dp.toPx() * sparkleScale,
                    center = Offset(centerOffset.x - 65.dp.toPx(), centerOffset.y - 30.dp.toPx())
                )
                // Top Right Sparkle
                drawCircle(
                    color = secondaryColor.copy(alpha = 0.8f),
                    radius = 4.dp.toPx() * (1.8f - sparkleScale),
                    center = Offset(centerOffset.x + 60.dp.toPx(), centerOffset.y - 55.dp.toPx())
                )
                // Bottom Left Sparkle
                drawCircle(
                    color = primaryColor.copy(alpha = 0.6f),
                    radius = 3.dp.toPx() * sparkleScale,
                    center = Offset(centerOffset.x - 50.dp.toPx(), centerOffset.y + 50.dp.toPx())
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Compelling Title
        Text(
            text = "Your Slate is Clean!",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Encouraging Subtitle
        Text(
            text = "No tasks found. Tap below to organize your goals, reminders, and daily habits.",
            fontSize = 14.sp,
            lineHeight = 21.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Encouraging action button
        Button(
            onClick = onAddTaskClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ),
            shape = RoundedCornerShape(24.dp),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
            modifier = Modifier.height(48.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Add Your First Task",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }
    }
}

fun uriToBase64(context: android.content.Context, uri: android.net.Uri): String? {
    return try {
        val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
        val originalBitmap = BitmapFactory.decodeStream(inputStream)
        inputStream?.close()
        if (originalBitmap == null) return null

        // Resize the bitmap to keep storage/payload lightweight (max 500px dimension)
        val maxDimension = 500
        val width = originalBitmap.width
        val height = originalBitmap.height
        val (newWidth, newHeight) = if (width > height) {
            val ratio = height.toFloat() / width.toFloat()
            maxDimension to (maxDimension * ratio).toInt()
        } else {
            val ratio = width.toFloat() / height.toFloat()
            (maxDimension * ratio).toInt() to maxDimension
        }
        val resizedBitmap = Bitmap.createScaledBitmap(originalBitmap, newWidth, newHeight, true)
        
        val outputStream = ByteArrayOutputStream()
        resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 75, outputStream)
        val bytes = outputStream.toByteArray()
        Base64.encodeToString(bytes, Base64.NO_WRAP)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

@Composable
fun rememberBitmapFromBase64(base64Str: String?): androidx.compose.ui.graphics.ImageBitmap? {
    return remember(base64Str) {
        if (base64Str.isNullOrBlank()) null else {
            try {
                val bytes = Base64.decode(base64Str, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                bitmap?.asImageBitmap()
            } catch (e: Exception) {
                null
            }
        }
    }
}
