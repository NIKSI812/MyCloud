package com.example.mycloud.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.mycloud.data.local.AppDatabase
import com.example.mycloud.data.local.FileEntity
import com.example.mycloud.data.repository.FileRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FileViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: FileRepository

    init {
        val dao = AppDatabase.getDatabase(application).fileDao()
        repository = FileRepository(dao)
    }

    // Список всех файлов для экрана
    val files: StateFlow<List<FileEntity>> = repository.allFiles
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // CREATE
    fun addFile(name: String, type: String, sizeKb: Long, description: String) {
        viewModelScope.launch {
            repository.insert(
                FileEntity(
                    name = name,
                    type = type,
                    sizeKb = sizeKb,
                    description = description
                )
            )
        }
    }

    // UPDATE
    fun updateFile(file: FileEntity) {
        viewModelScope.launch {
            repository.update(file)
        }
    }

    // DELETE
    fun deleteFile(file: FileEntity) {
        viewModelScope.launch {
            repository.delete(file)
        }
    }
}