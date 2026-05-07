package io.jadu.catylst.data.local

import androidx.room3.Dao
import androidx.room3.Delete
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SampleEntityDao {

    @Query("SELECT * FROM sample_entities ORDER BY createdAt DESC")
    fun getAll(): Flow<List<SampleEntity>>

    @Query("SELECT * FROM sample_entities WHERE id = :id")
    suspend fun getById(id: Long): SampleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: SampleEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<SampleEntity>)

    @Update
    suspend fun update(entity: SampleEntity)

    @Delete
    suspend fun delete(entity: SampleEntity)

    @Query("DELETE FROM sample_entities")
    suspend fun deleteAll()
}
