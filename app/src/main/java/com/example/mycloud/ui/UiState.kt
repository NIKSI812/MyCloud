package com.example.mycloud.ui

import com.example.mycloud.data.local.FileEntity

sealed interface UiState {
    object Loading : UiState
    data class Success(val files: List<FileEntity>) : UiState
    data class Error(val message: String) : UiState
}

//UiState