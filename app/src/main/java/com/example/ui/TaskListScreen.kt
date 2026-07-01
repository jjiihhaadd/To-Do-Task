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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
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
                    
                    // Avatar & About
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { showAboutDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "About",
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
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "No tasks yet. Enjoy your day!",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
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
            onAdd = { title, desc, cat, reminderTime ->
                viewModel.addTask(title, desc, cat, reminderTime)
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
            onSave = { title, desc, cat, reminderTime ->
                viewModel.updateTask(editingTask!!.copy(title = title, description = desc, category = cat, reminderTime = reminderTime))
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
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
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
            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else CategoryGrayText
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
    onAdd: (String, String, String, Long?) -> Unit,
    onAutoTag: (String, String, (String) -> Unit) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var reminderTime by remember { mutableStateOf<Long?>(null) }
    var isTagging by remember { mutableStateOf(false) }
    val context = LocalContext.current

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
                        onAdd(title, description, category, reminderTime)
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
    onSave: (String, String, String, Long?) -> Unit,
    onAutoTag: (String, String, (String) -> Unit) -> Unit
) {
    var title by remember { mutableStateOf(task.title) }
    var description by remember { mutableStateOf(task.description) }
    var category by remember { mutableStateOf(task.category) }
    var reminderTime by remember { mutableStateOf(task.reminderTime) }
    var isTagging by remember { mutableStateOf(false) }
    val context = LocalContext.current

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
                        onSave(title, description, category, reminderTime)
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
