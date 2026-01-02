package com.project.salbabida.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "home_location")
data class HomeLocation(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val latitude: Double,
    val longitude: Double,
    val isHouse: Boolean = true,
    val name: String = "My Home",
    val createdAt: Long = System.currentTimeMillis()
)
