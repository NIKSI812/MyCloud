package com.example.mycloud.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface FileDao {

    // READ: все файлы, авто-обновление через Flow
    @Query("SELECT * FROM files ORDER BY createdAt DESC")
    fun getAllFiles(): Flow<List<FileEntity>>

    // READ: один файл по id
    @Query("SELECT * FROM files WHERE id = :id")
    suspend fun getFileById(id: Int): FileEntity?

    // CREATE
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFile(file: FileEntity)

    // UPDATE
    @Update
    suspend fun updateFile(file: FileEntity)

    // DELETE
    @Delete
    suspend fun deleteFile(file: FileEntity)
}