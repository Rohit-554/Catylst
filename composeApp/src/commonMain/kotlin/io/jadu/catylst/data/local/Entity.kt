package io.jadu.catylst.data.local

import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "sample_entities")
data class SampleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String,
    val createdAt: Long = 0
)
