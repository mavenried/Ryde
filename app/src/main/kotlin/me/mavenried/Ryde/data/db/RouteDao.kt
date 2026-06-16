package me.mavenried.Ryde.data.db

import androidx.room.*
import me.mavenried.Ryde.data.model.RouteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RouteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(route: RouteEntity)

    @Query("SELECT * FROM routes ORDER BY startTime DESC")
    fun getAllRoutes(): Flow<List<RouteEntity>>

    @Query("SELECT * FROM routes WHERE id = :id")
    suspend fun getRouteById(id: String): RouteEntity?

    @Query("SELECT * FROM routes WHERE completed = 0 ORDER BY startTime DESC LIMIT 1")
    suspend fun getLastIncompleteRoute(): RouteEntity?

    @Delete
    suspend fun delete(route: RouteEntity)

    @Query("DELETE FROM routes")
    suspend fun deleteAll()
}
