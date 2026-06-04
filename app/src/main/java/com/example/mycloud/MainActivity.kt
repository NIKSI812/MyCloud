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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.mycloud.data.local.FileEntity
import com.example.mycloud.ui.FileViewModel
import com.example.mycloud.ui.theme.MyCloudTheme

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
                    FileScreen(viewModel)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileScreen(viewModel: FileViewModel) {
    val files by viewModel.files.collectAsStateWithLifecycle()

    var showDialog by remember { mutableStateOf(false) }
    var editingFile by remember { mutableStateOf<FileEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("MyCloud — Хранилище файлов") })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                editingFile = null
                showDialog = true
            }) {
                Icon(Icons.Default.Add, contentDescription = "Добавить")
            }
        }
    ) { padding ->
        if (files.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("Файлов пока нет.\nНажми + чтобы добавить.")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(8.dp)
            ) {
                items(files) { file ->
                    FileItem(
                        file = file,
                        onEdit = {
                            editingFile = file
                            showDialog = true
                        },
                        onDelete = { viewModel.deleteFile(file) }
                    )
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
                if (current == null) {
                    viewModel.addFile(name, type, size, desc)
                } else {
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
}

@Composable
fun FileItem(
    file: FileEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
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