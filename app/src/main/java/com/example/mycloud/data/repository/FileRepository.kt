package com.example.mycloud.data.repository

import com.example.mycloud.data.local.FileDao
import com.example.mycloud.data.local.FileEntity
import kotlinx.coroutines.flow.Flow

class FileRepository(private val fileDao: FileDao) {

    val allFiles: Flow<List<FileEntity>> = fileDao.getAllFiles()

    suspend fun getFileById(id: Int): FileEntity? = fileDao.getFileById(id)

    suspend fun insert(file: FileEntity) = fileDao.insertFile(file)

    suspend fun update(file: FileEntity) = fileDao.updateFile(file)

    suspend fun delete(file: FileEntity) = fileDao.deleteFile(file)
}

//FileRepository