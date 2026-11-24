package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "search_history")
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val query: String,
    val productName: String,
    val hsCode: String,
    val description: String,
    val chapter: String,
    val dutyRate: String,
    val confidence: String,
    val imageUrl: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val fee: String? = null,
    val serviceImportFee: String? = null,
    val taxAdvanceFee: String? = null,
    val customsItemName: String? = null,
    val customsUnit: String? = null,
    val isImportAllowed: Boolean? = true
)
