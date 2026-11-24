package com.example.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.util.Base64
import com.example.BuildConfig
import com.example.data.local.HistoryDao
import com.example.data.local.HistoryEntity
import com.example.data.network.Content
import com.example.data.network.GenerateContentRequest
import com.example.data.network.GenerationConfig
import com.example.data.network.HsCodeResult
import com.example.data.network.InlineData
import com.example.data.network.Part
import com.example.data.network.RetrofitClient
import com.example.data.network.SyrianCustomsClient
import com.squareup.moshi.Moshi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

class HsCodeRepository(
    private val context: Context,
    private val historyDao: HistoryDao
) {
    val allHistory: Flow<List<HistoryEntity>> = historyDao.getAllHistory()

    private val moshi: Moshi = RetrofitClient.moshiInstance
    private val jsonAdapter = moshi.adapter(HsCodeResult::class.java)

    private fun getApiKey(): String {
        val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        val customKey = prefs.getString("custom_api_key", "") ?: ""
        if (customKey.isNotBlank()) {
            return customKey
        }
        return BuildConfig.GEMINI_API_KEY
    }

    private val systemInstruction = Content(
        parts = listOf(
            Part(
                text = """
                    You are a global Harmonized System Code (HS Code) classification expert.
                    Analyze the product described by name or shown in the image and determine its correct HS Code (normally 6 to 10 digits).
                    CRITICAL: The HS Code MUST be returned as clean continuous digits without any dots, spaces, slashes or separators (for example '84713000' or '48025600').
                    Return a JSON object in the following format ONLY:
                    {
                      "hs_code": "The HS Code of the product as clean digits without dots or spaces (e.g. '84713000' or '48025600')",
                      "product_name": "The common product name in Arabic",
                      "description": "Explanation in Arabic of why this HS Code was chosen and what category rules apply",
                      "chapter": "Details of the HS Chapter and Section in Arabic (e.g. 'الفصل 84: المفاعلات النووية والغلايات والآلات')",
                      "duty_rate": "Estimated customs duty rate or range in Arabic (e.g., '5%' or 'معفى' or 'خاضع لرسوم جمركية')",
                      "confidence": "Confidence level in Arabic: 'عالي' or 'متوسط' or 'منخفض'"
                    }
                    Do not write any markdown wrappers (like ```json) or extra text outside the JSON object.
                    Ensure all descriptions, product names, chapters, duty rates, and confidence are in professional, clear Arabic.
                """.trimIndent()
            )
        )
    )

    private fun cleanHsCode(raw: String): String {
        val digits = raw.filter { it.isDigit() }
        return if (digits.isNotBlank()) digits else raw.replace(Regex("[^0-9A-Za-z]"), "")
    }

    suspend fun searchByProductName(name: String): Result<HistoryEntity> = withContext(Dispatchers.IO) {
        try {
            val apiKey = getApiKey()
            if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
                return@withContext Result.failure(Exception("يرجى إعداد مفتاح API الخاص بـ Gemini في الإعدادات أو عبر Secrets."))
            }

            val request = GenerateContentRequest(
                contents = listOf(
                    Content(parts = listOf(Part(text = "البحث عن رمز النظام المنسق للمنتج التالي: $name (أعد الرمز كأرقام فقط بدون فواصل أو نقاط)")))
                ),
                generationConfig = GenerationConfig(
                    responseMimeType = "application/json",
                    temperature = 0.2f
                ),
                systemInstruction = systemInstruction
            )

            val response = RetrofitClient.service.generateContent(apiKey, request)
            val jsonText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: return@withContext Result.failure(Exception("لم يتم تلقي استجابة من الذكاء الاصطناعي."))

            val result = jsonAdapter.fromJson(jsonText)
                ?: return@withContext Result.failure(Exception("خطأ في قراءة استجابة النظام المنسق."))

            val cleanCode = cleanHsCode(result.hsCode)

            // Query Syrian Customs API for official tariff fees (رسم الاستيراد، بدل الخدمات، السلفة الضريبية)
            val tariffInfo = SyrianCustomsClient.fetchTariffInfo(cleanCode)

            val finalDutyRate = tariffInfo?.fee ?: result.dutyRate

            val entity = HistoryEntity(
                query = name,
                productName = result.productName,
                hsCode = cleanCode,
                description = result.description,
                chapter = result.chapter,
                dutyRate = finalDutyRate,
                confidence = result.confidence,
                imageUrl = null,
                fee = tariffInfo?.fee,
                serviceImportFee = tariffInfo?.serviceImportFee,
                taxAdvanceFee = tariffInfo?.taxAdvanceFee,
                customsItemName = tariffInfo?.name,
                customsUnit = tariffInfo?.unit,
                isImportAllowed = tariffInfo?.isActive ?: true
            )

            historyDao.insertHistory(entity)
            Result.success(entity)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun searchByProductImage(bitmap: Bitmap): Result<HistoryEntity> = withContext(Dispatchers.IO) {
        try {
            val apiKey = getApiKey()
            if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
                return@withContext Result.failure(Exception("يرجى إعداد مفتاح API الخاص بـ Gemini في الإعدادات أو عبر Secrets."))
            }

            val byteArrayOutputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream)
            val base64Image = Base64.encodeToString(byteArrayOutputStream.toByteArray(), Base64.NO_WRAP)

            val request = GenerateContentRequest(
                contents = listOf(
                    Content(
                        parts = listOf(
                            Part(text = "تعرف على هذا المنتج وحدد رمز النظام المنسق (HS Code) له بدون فواصل أو نقاط."),
                            Part(inlineData = InlineData(mimeType = "image/jpeg", data = base64Image))
                        )
                    )
                ),
                generationConfig = GenerationConfig(
                    responseMimeType = "application/json",
                    temperature = 0.2f
                ),
                systemInstruction = systemInstruction
            )

            val response = RetrofitClient.service.generateContent(apiKey, request)
            val jsonText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: return@withContext Result.failure(Exception("لم يتم تلقي استجابة من الكاميرا."))

            val result = jsonAdapter.fromJson(jsonText)
                ?: return@withContext Result.failure(Exception("خطأ في قراءة استجابة النظام المنسق."))

            val localImagePath = saveBitmapToLocal(context, bitmap)
            val cleanCode = cleanHsCode(result.hsCode)

            // Query Syrian Customs API for official tariff fees
            val tariffInfo = SyrianCustomsClient.fetchTariffInfo(cleanCode)
            val finalDutyRate = tariffInfo?.fee ?: result.dutyRate

            val entity = HistoryEntity(
                query = "صورة منتج (${result.productName})",
                productName = result.productName,
                hsCode = cleanCode,
                description = result.description,
                chapter = result.chapter,
                dutyRate = finalDutyRate,
                confidence = result.confidence,
                imageUrl = localImagePath,
                fee = tariffInfo?.fee,
                serviceImportFee = tariffInfo?.serviceImportFee,
                taxAdvanceFee = tariffInfo?.taxAdvanceFee,
                customsItemName = tariffInfo?.name,
                customsUnit = tariffInfo?.unit,
                isImportAllowed = tariffInfo?.isActive ?: true
            )

            historyDao.insertHistory(entity)
            Result.success(entity)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteHistoryItem(item: HistoryEntity) = withContext(Dispatchers.IO) {
        item.imageUrl?.let { path ->
            try {
                val file = File(path)
                if (file.exists()) {
                    file.delete()
                }
            } catch (e: Exception) {
                // Ignore
            }
        }
        historyDao.deleteHistory(item)
    }

    suspend fun clearHistory() = withContext(Dispatchers.IO) {
        val directory = File(context.filesDir, "history_images")
        if (directory.exists()) {
            directory.deleteRecursively()
        }
        historyDao.clearAllHistory()
    }

    private fun saveBitmapToLocal(context: Context, bitmap: Bitmap): String {
        val directory = File(context.filesDir, "history_images")
        if (!directory.exists()) {
            directory.mkdirs()
        }
        val file = File(directory, "img_${System.currentTimeMillis()}.jpg")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }
        return file.absolutePath
    }
}
