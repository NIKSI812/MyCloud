package com.example.mycloud.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "files")
data class FileEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,          // имя файла
    val type: String,          // тип: документ, фото, видео...
    val sizeKb: Long,          // размер в КБ
    val description: String,   // описание
    val createdAt: Long = System.currentTimeMillis()  // дата создания
)