package com.example.data.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class SyrianCustomsTariffInfo(
    val code: String? = null,
    val name: String? = null,
    val fee: String? = null,              // رسم الاستيراد
    val serviceImportFee: String? = null, // بدل الخدمات
    val taxAdvanceFee: String? = null,    // السلفة الضريبية
    val unit: String? = null,             // الوحدة: عدد أو وزن (طن)
    val isActive: Boolean = true          // هل مسموح الاستيراد (true) أم يوجد حجر (false)
)

object SyrianCustomsClient {
    private const val BASE_URL = "https://customs.syrianborders.gov.sy/api/customsItemCommerceNames"
    private const val API_KEY = "f8e7d1a2-3b4c-5d6e-7f8a-9b0c1d2e3f4a"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    suspend fun fetchTariffInfo(itemCode: String): SyrianCustomsTariffInfo? = withContext(Dispatchers.IO) {
        val cleanCode = itemCode.filter { it.isDigit() }
        if (cleanCode.isBlank()) return@withContext null

        val codesToTry = mutableListOf(cleanCode)
        if (cleanCode.length > 6) {
            codesToTry.add(cleanCode.take(6))
        }
        if (cleanCode.length > 4 && !codesToTry.contains(cleanCode.take(4))) {
            codesToTry.add(cleanCode.take(4))
        }

        for (codeQuery in codesToTry) {
            try {
                // Try query without restricting isActive so both allowed and quarantined items can be retrieved
                val url = "$BASE_URL?PageNumber=1&itemCodeWithName=$codeQuery"
                val request = Request.Builder()
                    .url(url)
                    .addHeader("accept", "application/json")
                    .addHeader("accept-encoding", "gzip")
                    .addHeader("apikey", API_KEY)
                    .addHeader("content-type", "application/json")
                    .addHeader("host", "customs.syrianborders.gov.sy")
                    .addHeader("user-agent", "Dart/3.11 (dart:io)")
                    .build()

                val response = okHttpClient.newCall(request).execute()
                if (response.isSuccessful) {
                    val bodyString = response.body?.string() ?: continue
                    val json = JSONObject(bodyString)
                    val itemsArray = json.optJSONArray("data")
                    if (itemsArray != null && itemsArray.length() > 0) {
                        var chosenItem: JSONObject? = null
                        for (i in 0 until itemsArray.length()) {
                            val itemObj = itemsArray.getJSONObject(i)
                            val itemCodeStr = itemObj.opt("code")?.toString()?.filter { it.isDigit() } ?: ""
                            if (itemCodeStr == cleanCode || itemCodeStr.startsWith(cleanCode) || cleanCode.startsWith(itemCodeStr)) {
                                chosenItem = itemObj
                                break
                            }
                        }
                        if (chosenItem == null) {
                            chosenItem = itemsArray.getJSONObject(0)
                        }

                        val feeRaw = chosenItem.opt("fee")?.toString()?.trim()
                        val serviceTaxRaw = chosenItem.opt("serviceImportFee")?.toString()?.trim()
                        val taxAdvanceRaw = chosenItem.opt("taxAdvanceFee")?.toString()?.trim()
                        val nameRaw = chosenItem.optString("name", "").trim()
                        val codeRaw = chosenItem.opt("code")?.toString()?.filter { it.isDigit() }

                        // Extract Unit Information (عدد vs وزن)
                        val itemStr = chosenItem.toString()
                        val unitField = chosenItem.optString("unit", "") + " " +
                                        chosenItem.optString("unitName", "") + " " +
                                        chosenItem.optString("itemUnit", "") + " " +
                                        chosenItem.optString("customsUnit", "") + " " +
                                        chosenItem.optString("measureUnit", "")

                        val detectedUnit = when {
                            unitField.contains("عدد") || itemStr.contains("الوحدة عدد") || itemStr.contains("\"unit\":\"عدد\"") || itemStr.contains("عدد") -> "عدد"
                            unitField.contains("وزن") || itemStr.contains("الوحدة وزن") || itemStr.contains("\"unit\":\"وزن\"") || itemStr.contains("وزن") || itemStr.contains("طن") -> "وزن"
                            bodyString.contains("الوحدة عدد") -> "عدد"
                            bodyString.contains("الوحدة وزن") -> "وزن"
                            else -> "وزن" // Default to weight (طن)
                        }

                        // Extract isActive status (مسموح vs يوجد حجر على استيراد المادة)
                        val isActiveDetected = when {
                            chosenItem.has("isActive") -> {
                                val activeVal = chosenItem.opt("isActive")
                                if (activeVal is Boolean) {
                                    activeVal
                                } else {
                                    activeVal?.toString()?.equals("false", ignoreCase = true) != true
                                }
                            }
                            itemStr.contains("\"isActive\":false", ignoreCase = true) ||
                            itemStr.contains("\"isActive\": false", ignoreCase = true) ||
                            itemStr.contains("'isActive': False", ignoreCase = true) ||
                            itemStr.contains("'isActive':False", ignoreCase = true) ||
                            itemStr.contains("isActive=false", ignoreCase = true) -> false
                            itemStr.contains("\"isActive\":true", ignoreCase = true) ||
                            itemStr.contains("\"isActive\": true", ignoreCase = true) ||
                            itemStr.contains("'isActive': True", ignoreCase = true) ||
                            itemStr.contains("'isActive':True", ignoreCase = true) ||
                            itemStr.contains("isActive=true", ignoreCase = true) -> true
                            else -> true
                        }

                        return@withContext SyrianCustomsTariffInfo(
                            code = codeRaw,
                            name = if (nameRaw.isNotEmpty()) nameRaw else null,
                            fee = formatDollarFee(feeRaw),
                            serviceImportFee = formatDollarFee(serviceTaxRaw),
                            taxAdvanceFee = formatPercentFee(taxAdvanceRaw),
                            unit = detectedUnit,
                            isActive = isActiveDetected
                        )
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        null
    }

    private fun formatDollarFee(raw: String?): String? {
        if (raw == null || raw.isEmpty() || raw == "null") return null
        val clean = raw.replace("$", "").replace("%", "").trim()
        if (clean.isEmpty()) return null
        return "$clean $"
    }

    private fun formatPercentFee(raw: String?): String? {
        if (raw == null || raw.isEmpty() || raw == "null") return null
        val clean = raw.replace("%", "").replace("$", "").trim()
        if (clean.isEmpty()) return null
        return "$clean%"
    }
}
