package com.example.mycloud

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import coil.compose.AsyncImage
import com.example.mycloud.data.local.FileEntity
import com.example.mycloud.ui.FileViewModel
import com.example.mycloud.ui.theme.MyCloudTheme
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.example.mycloud.ui.UiState

class MainActivity : ComponentActivity() {

    private val viewModel: FileViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyCloudTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation(viewModel)
                }
            }
        }
    }
}

@Composable
fun AppNavigation(viewModel: FileViewModel) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "list") {
        composable("list") {
            FileScreen(
                viewModel = viewModel,
                onOpenDetail = { fileId -> navController.navigate("detail/$fileId") },
                onOpenAbout = { navController.navigate("about") }
            )
        }
        composable(
            route = "detail/{fileId}",
            arguments = listOf(navArgument("fileId") { type = NavType.IntType })
        ) { backStackEntry ->
            val fileId = backStackEntry.arguments?.getInt("fileId") ?: 0
            val files by viewModel.files.collectAsStateWithLifecycle()
            val file = files.find { it.id == fileId }
            DetailScreen(
                file = file,
                onBack = { navController.popBackStack() }
            )
        }
        composable("about") {
            AboutScreen(onBack = { navController.popBackStack() })
        }
    }
}

// Возвращает иконку и цвет по типу файла
fun getFileVisual(type: String): Pair<ImageVector, Color> {
    val t = type.lowercase()
    return when {
        listOf("jpeg", "jpg", "png", "webp", "gif", "image", "фото").any { t.contains(it) } ->
            Icons.Default.Image to Color(0xFF4CAF50)
        listOf("mp4", "avi", "mkv", "video", "видео", "mov").any { t.contains(it) } ->
            Icons.Default.Movie to Color(0xFFE91E63)
        listOf("mp3", "wav", "audio", "аудио").any { t.contains(it) } ->
            Icons.Default.MusicNote to Color(0xFF9C27B0)
        listOf("pdf").any { t.contains(it) } ->
            Icons.Default.PictureAsPdf to Color(0xFFF44336)
        listOf("doc", "txt", "документ").any { t.contains(it) } ->
            Icons.Default.Description to Color(0xFF2196F3)
        else -> Icons.Default.InsertDriveFile to Color(0xFF607D8B)
    }
}

fun isImage(type: String): Boolean {
    val t = type.lowercase()
    return listOf("jpeg", "jpg", "png", "webp", "gif", "image", "фото").any { t.contains(it) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileScreen(
    viewModel: FileViewModel,
    onOpenDetail: (Int) -> Unit,
    onOpenAbout: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var showDialog by remember { mutableStateOf(false) }
    var fileToDelete by remember { mutableStateOf<FileEntity?>(null) }
    var editingFile by remember { mutableStateOf<FileEntity?>(null) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.uploadFile(it) }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.CloudQueue,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("MyCloud", fontWeight = FontWeight.Bold)
                    }
                },
                actions = {
                    IconButton(onClick = onOpenAbout) {
                        Icon(Icons.Default.Info, contentDescription = "О приложении")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { launcher.launch("*/*") },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Загрузить") }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (val state = uiState) {
                is UiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                is UiState.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = state.message,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                is UiState.Success -> {
                    val files = state.files
                    if (files.isEmpty()) {
                        EmptyState()
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(files) { file ->
                                FileItem(
                                    file = file,
                                    onClick = { onOpenDetail(file.id) },
                                    onEdit = {
                                        editingFile = file
                                        showDialog = true
                                    },
                                    onDelete = { fileToDelete = file }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        FileDialog(
            file = editingFile,
            onDismiss = { showDialog = false },
            onSave = { name, type, size, desc ->
                val current = editingFile
                if (current != null) {
                    viewModel.updateFile(
                        current.copy(
                            name = name,
                            type = type,
                            sizeKb = size,
                            description = desc
                        )
                    )
                }
                showDialog = false
            }
        )
    }

    if (fileToDelete != null) {
        AlertDialog(
            onDismissRequest = { fileToDelete = null },
            icon = { Icon(Icons.Default.Delete, contentDescription = null) },
            title = { Text("Удалить файл?") },
            text = { Text("Файл \"${fileToDelete?.name}\" будет удалён безвозвратно.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        fileToDelete?.let { viewModel.deleteFile(it) }
                        fileToDelete = null
                    }
                ) {
                    Text("Удалить", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { fileToDelete = null }) {
                    Text("Отмена")
                }
            }
        )
    }
}

@Composable
fun EmptyState() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.CloudUpload,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "Файлов пока нет",
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "Нажми «Загрузить», чтобы добавить файл",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileItem(
    file: FileEntity,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val (icon, color) = getFileVisual(file.type)

    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Превью картинки или цветная иконка
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                if (isImage(file.type) && file.uri.isNotEmpty()) {
                    AsyncImage(
                        model = file.uri.toUri(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    file.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    "${file.type.uppercase()} • ${file.sizeKb} КБ",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = onEdit) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "Изменить",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Удалить",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileDialog(
    file: FileEntity?,
    onDismiss: () -> Unit,
    onSave: (String, String, Long, String) -> Unit
) {
    var name by remember { mutableStateOf(file?.name ?: "") }
    var type by remember { mutableStateOf(file?.type ?: "") }
    var size by remember { mutableStateOf(file?.sizeKb?.toString() ?: "") }
    var desc by remember { mutableStateOf(file?.description ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (file == null) "Новый файл" else "Редактировать файл") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Название") },
                    singleLine = true
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = type,
                    onValueChange = { type = it },
                    label = { Text("Тип (pdf, jpg...)") },
                    singleLine = true
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = size,
                    onValueChange = { size = it.filter { c -> c.isDigit() } },
                    label = { Text("Размер (КБ)") },
                    singleLine = true
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Описание") }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank()) {
                        onSave(name, type, size.toLongOrNull() ?: 0L, desc)
                    }
                }
            ) {
                Text("Сохранить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}