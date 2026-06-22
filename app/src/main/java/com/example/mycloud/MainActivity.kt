package com.example.mycloud

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavType
import androidx.navigation.navArgument
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
            TopAppBar(
                title = { Text("MyCloud — Хранилище файлов") },
                actions = {
                    IconButton(onClick = onOpenAbout) {
                        Icon(Icons.Default.Info, contentDescription = "О приложении")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { launcher.launch("*/*") }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Загрузить файл")
            }
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
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Файлов пока нет.\nНажми + чтобы загрузить.")
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp)
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

    // Диалог подтверждения удаления     ← ВСТАВЛЯЕШЬ ЗДЕСЬ
    if (fileToDelete != null) {
        AlertDialog(
            onDismissRequest = { fileToDelete = null },
            title = { Text("Удалить файл?") },
            text = { Text("Файл \"${fileToDelete?.name}\" будет удалён безвозвратно.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        fileToDelete?.let { viewModel.deleteFile(it) }
                        fileToDelete = null
                    }
                ) {
                    Text("Удалить")
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
fun FileItem(
    file: FileEntity,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(file.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    "Тип: ${file.type} • ${file.sizeKb} КБ",
                    style = MaterialTheme.typography.bodySmall
                )
                if (file.description.isNotBlank()) {
                    Text(
                        file.description,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "Изменить")
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Удалить")
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