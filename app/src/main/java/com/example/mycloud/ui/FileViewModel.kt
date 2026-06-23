package com.example.mycloud.ui

import android.app.Application
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.mycloud.data.local.AppDatabase
import com.example.mycloud.data.local.FileEntity
import com.example.mycloud.data.repository.FileRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class FileViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: FileRepository

    init {
        val dao = AppDatabase.getDatabase(application).fileDao()
        repository = FileRepository(dao)
    }

    // Состояние экрана: Loading / Success / Error
    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    // Список файлов (оставляем для совместимости)
    val files: StateFlow<List<FileEntity>> = repository.allFiles
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        viewModelScope.launch {
            kotlinx.coroutines.delay(800) // показываем индикатор загрузки
            repository.allFiles
                .onEach { list ->
                    _uiState.value = UiState.Success(list)
                }
                .catch { e ->
                    _uiState.value = UiState.Error("Ошибка загрузки: ${e.message}")
                }
                .collect {}
        }
    }

    // CREATE (ручное добавление через диалог)
    fun addFile(name: String, type: String, sizeKb: Long, description: String) {
        viewModelScope.launch {
            try {
                repository.insert(
                    FileEntity(
                        name = name,
                        type = type,
                        sizeKb = sizeKb,
                        description = description
                    )
                )
            } catch (e: Exception) {
                _uiState.value = UiState.Error("Не удалось добавить файл: ${e.message}")
            }
        }
    }

    // ЗАГРУЗКА РЕАЛЬНОГО ФАЙЛА — копируем его внутрь приложения
    fun uploadFile(uri: Uri) {
        viewModelScope.launch {
            try {
                val context = getApplication<Application>()
                val resolver = context.contentResolver

                var fileName = "file_${System.currentTimeMillis()}"
                var sizeBytes = 0L
                resolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (cursor.moveToFirst()) {
                        if (nameIndex >= 0) fileName = cursor.getString(nameIndex)
                        if (sizeIndex >= 0) sizeBytes = cursor.getLong(sizeIndex)
                    }
                }

                val type = fileName.substringAfterLast('.', "файл")

                val savedPath = withContext(Dispatchers.IO) {
                    val destFile = File(context.filesDir, "${System.currentTimeMillis()}_$fileName")
                    resolver.openInputStream(uri)?.use { input ->
                        destFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    destFile.absolutePath
                }

                repository.insert(
                    FileEntity(
                        name = fileName,
                        type = type,
                        sizeKb = sizeBytes / 1024,
                        description = "Загружен в хранилище",
                        uri = savedPath
                    )
                )
            } catch (e: Exception) {
                _uiState.value = UiState.Error("Не удалось загрузить файл: ${e.message}")
            }
        }
    }

    // UPDATE
    fun updateFile(file: FileEntity) {
        viewModelScope.launch {
            try {
                repository.update(file)
            } catch (e: Exception) {
                _uiState.value = UiState.Error("Не удалось обновить файл: ${e.message}")
            }
        }
    }

    // DELETE — удаляем и из базы, и сам файл
    fun deleteFile(file: FileEntity) {
        viewModelScope.launch {
            try {
                if (file.uri.isNotBlank()) {
                    withContext(Dispatchers.IO) {
                        File(file.uri).delete()
                    }
                }
                repository.delete(file)
            } catch (e: Exception) {
                _uiState.value = UiState.Error("Не удалось удалить файл: ${e.message}")
            }
        }
    }
}

//FileViewModel