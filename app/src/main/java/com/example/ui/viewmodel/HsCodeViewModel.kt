package com.example.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.HistoryDatabase
import com.example.data.local.HistoryEntity
import com.example.data.repository.HsCodeRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject

sealed interface SearchUiState {
    object Idle : SearchUiState
    object Loading : SearchUiState
    data class Success(val result: HistoryEntity) : SearchUiState
    data class Error(val message: String) : SearchUiState
}

class HsCodeViewModel(
    application: Application,
    private val repository: HsCodeRepository
) : AndroidViewModel(application) {

    fun getCustomApiKey(): String {
        val prefs = getApplication<Application>().getSharedPreferences("app_settings", android.content.Context.MODE_PRIVATE)
        return prefs.getString("custom_api_key", "") ?: ""
    }

    fun saveCustomApiKey(key: String) {
        val prefs = getApplication<Application>().getSharedPreferences("app_settings", android.content.Context.MODE_PRIVATE)
        prefs.edit().putString("custom_api_key", key).apply()
    }

    // --- Activation & Device Locking Fields ---
    
    companion object {
        val AUTHORIZED_TP_KEYS: Set<String> = setOf(
            "TP1049", "TP1132", "TP1217", "TP1218", "TP1219", "TP1220", "TP1221", "TP1222",
            "TP1223", "TP1224", "TP1225", "TP1226", "TP1227", "TP1228", "TP1229", "TP1255",
            "TP1264", "TP1281", "TP1283", "TP1284", "TP1285", "TP1286", "TP1287", "TP1288",
            "TP1289", "TP1290", "TP1291", "TP1292", "TP1293", "TP1294", "TP1295", "TP1296",
            "TP1298", "TP1299", "TP1300", "TP1301", "TP1302", "TP1303", "TP138", "TP1383",
            "TP1394", "TP1398", "TP1399", "TP140", "TP1400", "TP1401", "TP1402", "TP1403",
            "TP1404", "TP1405", "TP1406", "TP1407", "TP1408", "TP1409", "TP1410", "TP1411",
            "TP1413", "TP1414", "TP1415", "TP1417", "TP1418", "TP1419", "TP1420", "TP1421",
            "TP1422", "TP150", "TP1531", "TP1532", "TP1533", "TP1534", "TP1535", "TP1536",
            "TP1537", "TP1538", "TP1539", "TP1540", "TP1541", "TP1542", "TP1543", "TP1544",
            "TP1545", "TP1546", "TP1547", "TP1548", "TP1549", "TP1550", "TP1551", "TP1552",
            "TP1553", "TP1554", "TP1555", "TP1556", "TP1557", "TP1558", "TP1559", "TP1560",
            "TP1561", "TP1562", "TP1563", "TP1564", "TP1565", "TP1566", "TP1567", "TP1568",
            "TP1569", "TP1570", "TP1571", "TP161", "TP174", "TP18", "TP195", "TP214",
            "TP222", "TP223", "TP226", "TP227", "TP23", "TP240", "TP246", "TP257",
            "TP258", "TP259", "TP260", "TP263", "TP3", "TP300", "TP370", "TP371",
            "TP42", "TP430", "TP434", "TP440", "TP462", "TP465", "TP466", "TP56",
            "TP637", "TP650", "TP659", "TP666", "TP769", "TP770", "TP81", "TP893",
            "TP915", "TP951", "TP952", "TP953", "TP954", "TP955", "TP957", "TP958",
            "TP959", "TP96", "TP960"
        )

        val LIFETIME_KEYS: Set<String> = setOf(
            "DDFMOTO"
        )
    }

    fun getDeviceId(): String {
        val context = getApplication<Application>()
        return Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "UNKNOWN_DEVICE"
    }

    private fun isTpKeyExpired(): Boolean {
        val current = java.util.Calendar.getInstance()
        val expiry = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.YEAR, 2026)
            set(java.util.Calendar.MONTH, java.util.Calendar.OCTOBER) // October is 9
            set(java.util.Calendar.DAY_OF_MONTH, 1)
            set(java.util.Calendar.HOUR_OF_DAY, 23)
            set(java.util.Calendar.MINUTE, 59)
            set(java.util.Calendar.SECOND, 59)
            set(java.util.Calendar.MILLISECOND, 999)
        }
        return current.after(expiry)
    }

    private fun isDemoExpired(): Boolean {
        val current = java.util.Calendar.getInstance()
        val expiry = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.YEAR, 2026)
            set(java.util.Calendar.MONTH, java.util.Calendar.AUGUST) // Calendar.AUGUST is 7
            set(java.util.Calendar.DAY_OF_MONTH, 25)
            set(java.util.Calendar.HOUR_OF_DAY, 23)
            set(java.util.Calendar.MINUTE, 59)
            set(java.util.Calendar.SECOND, 59)
            set(java.util.Calendar.MILLISECOND, 999)
        }
        return current.after(expiry)
    }

    private fun isKeyExpired(key: String): Boolean {
        val upper = key.trim().uppercase()
        if (LIFETIME_KEYS.contains(upper)) {
            return false
        }
        if (AUTHORIZED_TP_KEYS.contains(upper)) {
            return isTpKeyExpired()
        }
        if (upper == "DEMO12345" || upper == "DEMO123") {
            return isDemoExpired()
        }
        return false
    }

    fun isDisclaimerAccepted(): Boolean {
        val prefs = getApplication<Application>().getSharedPreferences("app_settings", android.content.Context.MODE_PRIVATE)
        return prefs.getBoolean("disclaimer_accepted_v1", false)
    }

    fun setDisclaimerAccepted() {
        val prefs = getApplication<Application>().getSharedPreferences("app_settings", android.content.Context.MODE_PRIVATE)
        prefs.edit().putBoolean("disclaimer_accepted_v1", true).apply()
    }

    fun isLoggedIn(): Boolean {
        val prefs = getApplication<Application>().getSharedPreferences("app_settings", android.content.Context.MODE_PRIVATE)
        val loggedIn = prefs.getBoolean("is_logged_in", false)
        val key = prefs.getString("activation_key", "") ?: ""
        if (loggedIn && key.isNotBlank()) {
            if (isKeyExpired(key)) {
                prefs.edit()
                    .putBoolean("is_logged_in", false)
                    .putString("activation_key", "")
                    .apply()
                return false
            }
        }
        return loggedIn
    }

    fun getActivationKey(): String {
        val prefs = getApplication<Application>().getSharedPreferences("app_settings", android.content.Context.MODE_PRIVATE)
        return prefs.getString("activation_key", "") ?: ""
    }

    fun getAppsScriptUrl(): String {
        return "https://script.google.com/macros/s/AKfycbxGswuAyKgWO_rh5mvaxL4ISI-LI-wv6V4V6xNfGjSfhPXo_QpvST-MAhD9Eni0hooNpw/exec"
    }

    fun saveAppsScriptUrl(url: String) {
        val prefs = getApplication<Application>().getSharedPreferences("app_settings", android.content.Context.MODE_PRIVATE)
        prefs.edit().putString("apps_script_url", url).apply()
    }

    fun getCryptolensToken(): String {
        val prefs = getApplication<Application>().getSharedPreferences("app_settings", android.content.Context.MODE_PRIVATE)
        return prefs.getString("cryptolens_token", "WyIxMjE2OTY0OTYiLCJRWjhhaHVhZ2JuZjN5Z1ltNnpna0tOcWMzNldiTHdpMGF4aXVOZmw0Il0=") ?: "WyIxMjE2OTY0OTYiLCJRWjhhaHVhZ2JuZjN5Z1ltNnpna0tOcWMzNldiTHdpMGF4aXVOZmw0Il0="
    }

    fun saveCryptolensToken(token: String) {
        val prefs = getApplication<Application>().getSharedPreferences("app_settings", android.content.Context.MODE_PRIVATE)
        prefs.edit().putString("cryptolens_token", token).apply()
    }

    fun getCryptolensProductId(): String {
        val prefs = getApplication<Application>().getSharedPreferences("app_settings", android.content.Context.MODE_PRIVATE)
        return prefs.getString("cryptolens_product_id", "33073") ?: "33073"
    }

    fun saveCryptolensProductId(productId: String) {
        val prefs = getApplication<Application>().getSharedPreferences("app_settings", android.content.Context.MODE_PRIVATE)
        prefs.edit().putString("cryptolens_product_id", productId).apply()
    }

    fun getLicensingSystem(): String {
        val prefs = getApplication<Application>().getSharedPreferences("app_settings", android.content.Context.MODE_PRIVATE)
        return prefs.getString("licensing_system", "cryptolens") ?: "cryptolens"
    }

    fun saveLicensingSystem(system: String) {
        val prefs = getApplication<Application>().getSharedPreferences("app_settings", android.content.Context.MODE_PRIVATE)
        prefs.edit().putString("licensing_system", system).apply()
    }

    fun logout() {
        val prefs = getApplication<Application>().getSharedPreferences("app_settings", android.content.Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean("is_logged_in", false)
            .putString("activation_key", "")
            .apply()
    }

    fun verifyActivationKey(key: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val system = getLicensingSystem()
            val deviceId = getDeviceId()
            val normalizedKey = key.trim().uppercase()

            // Check Lifetime Keys (never expire)
            if (LIFETIME_KEYS.contains(normalizedKey)) {
                val prefs = getApplication<Application>().getSharedPreferences("app_settings", android.content.Context.MODE_PRIVATE)
                prefs.edit()
                    .putBoolean("is_logged_in", true)
                    .putString("activation_key", key.trim())
                    .apply()
                viewModelScope.launch(Dispatchers.Main) {
                    onResult(true, "تم تفعيل الترخيص مدى الحياة بنجاح! (${key.trim()})")
                }
                return@launch
            }
            
            // Check Authorized TP Keys (valid until 1/10/2026)
            if (AUTHORIZED_TP_KEYS.contains(normalizedKey)) {
                if (isTpKeyExpired()) {
                    viewModelScope.launch(Dispatchers.Main) {
                        onResult(false, "انتهت صلاحية المفتاح ($normalizedKey) في 1/10/2026")
                    }
                    return@launch
                }
                val prefs = getApplication<Application>().getSharedPreferences("app_settings", android.content.Context.MODE_PRIVATE)
                prefs.edit()
                    .putBoolean("is_logged_in", true)
                    .putString("activation_key", normalizedKey)
                    .apply()
                viewModelScope.launch(Dispatchers.Main) {
                    onResult(true, "تم تفعيل الترخيص وتسجيل الدخول بنجاح! المفتاح صالح لغاية 1/10/2026 ($normalizedKey)")
                }
                return@launch
            }

            // Bypass for DEMO12345 / DEMO123 keys
            if (normalizedKey == "DEMO12345" || normalizedKey == "DEMO123") {
                if (isDemoExpired()) {
                    viewModelScope.launch(Dispatchers.Main) {
                        onResult(false, "انتهت صلاحية المفتاح التجريبي DEMO12345 (صلاحية المفتاح انتهت في 25/8/2026)")
                    }
                    return@launch
                }
                val prefs = getApplication<Application>().getSharedPreferences("app_settings", android.content.Context.MODE_PRIVATE)
                prefs.edit()
                    .putBoolean("is_logged_in", true)
                    .putString("activation_key", normalizedKey)
                    .apply()
                viewModelScope.launch(Dispatchers.Main) {
                    onResult(true, "تم التفعيل بنجاح (وضع المحاكاة التجريبي المفتوح - صالح لغاية 25/8/2026)")
                }
                return@launch
            }

            if (system == "cryptolens") {
                val token = getCryptolensToken().trim()
                val productIdStr = getCryptolensProductId().trim()
                
                if (token.isEmpty() || productIdStr.isEmpty()) {
                    viewModelScope.launch(Dispatchers.Main) {
                        onResult(false, "يرجى تعيين 'Token' و 'Product ID' لنظام Cryptolens أولاً في إعدادات التطبيق المتقدمة.")
                    }
                    return@launch
                }
                
                val productId = productIdStr.toIntOrNull()
                if (productId == null) {
                    viewModelScope.launch(Dispatchers.Main) {
                        onResult(false, "قيمة Product ID لنظام Cryptolens غير صالحة. يجب أن تكون رقمية.")
                    }
                    return@launch
                }
                
                try {
                    val url = URL("https://api.cryptolens.io/api/key/Activate")
                    val connection = url.openConnection() as HttpURLConnection
                    connection.requestMethod = "POST"
                    connection.doOutput = true
                    connection.connectTimeout = 15000
                    connection.readTimeout = 15000
                    connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
                    
                    val postData = "token=" + java.net.URLEncoder.encode(token, "UTF-8") +
                            "&productId=" + productId +
                            "&key=" + java.net.URLEncoder.encode(key.trim(), "UTF-8") +
                            "&machineCode=" + java.net.URLEncoder.encode(deviceId, "UTF-8") +
                            "&sign=true"
                    
                    val outputStream = connection.outputStream
                    outputStream.write(postData.toByteArray(charset("UTF-8")))
                    outputStream.flush()
                    outputStream.close()
                    
                    val responseCode = connection.responseCode
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        val reader = BufferedReader(InputStreamReader(connection.inputStream))
                        val response = StringBuilder()
                        var line: String?
                        while (reader.readLine().also { line = it } != null) {
                            response.append(line)
                        }
                        reader.close()
                        
                        val json = JSONObject(response.toString())
                        val resultVal = json.optInt("result", -1)
                        val message = json.optString("message", "حدث خطأ غير معروف")
                        
                        if (resultVal == 0) {
                            val prefs = getApplication<Application>().getSharedPreferences("app_settings", android.content.Context.MODE_PRIVATE)
                            prefs.edit()
                                .putBoolean("is_logged_in", true)
                                .putString("activation_key", key.trim())
                                .apply()
                            viewModelScope.launch(Dispatchers.Main) {
                                onResult(true, "تم تفعيل الترخيص بنجاح عبر Cryptolens.io!")
                            }
                        } else {
                            viewModelScope.launch(Dispatchers.Main) {
                                onResult(false, "فشل تفعيل Cryptolens: $message")
                            }
                        }
                    } else {
                        viewModelScope.launch(Dispatchers.Main) {
                            onResult(false, "فشل الاتصال بـ Cryptolens.io (رمز الاستجابة: $responseCode)")
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    viewModelScope.launch(Dispatchers.Main) {
                        onResult(false, "خطأ في الاتصال بالشبكة لـ Cryptolens: ${e.localizedMessage}")
                    }
                }
                return@launch
            }
            
            val scriptUrl = getAppsScriptUrl().trim()
            if (scriptUrl.isEmpty()) {
                viewModelScope.launch(Dispatchers.Main) {
                    onResult(false, "يرجى تعيين رابط Google Apps Script أولاً في الإعدادات، أو استخدم الكود التجريبي 'DEMO123' للمحاكاة.")
                }
                return@launch
            }
            
            try {
                val encodedKey = java.net.URLEncoder.encode(key.trim(), "UTF-8")
                val encodedDeviceId = java.net.URLEncoder.encode(deviceId, "UTF-8")
                val urlString = "$scriptUrl?action=activate&key=$encodedKey&deviceId=$encodedDeviceId"
                var url = URL(urlString)
                var connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 10000
                connection.readTimeout = 10000
                connection.instanceFollowRedirects = true
                
                var responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_MOVED_TEMP || responseCode == HttpURLConnection.HTTP_MOVED_PERM) {
                    val newUrl = connection.getHeaderField("Location")
                    connection = URL(newUrl).openConnection() as HttpURLConnection
                    connection.requestMethod = "GET"
                    responseCode = connection.responseCode
                }
                
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val reader = BufferedReader(InputStreamReader(connection.inputStream))
                    val response = StringBuilder()
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        response.append(line)
                    }
                    reader.close()
                    
                    val json = JSONObject(response.toString())
                    val status = json.optString("status", "error")
                    val message = json.optString("message", "حدث خطأ غير معروف")
                    
                    if (status == "success") {
                        val prefs = getApplication<Application>().getSharedPreferences("app_settings", android.content.Context.MODE_PRIVATE)
                        prefs.edit()
                            .putBoolean("is_logged_in", true)
                            .putString("activation_key", key.trim())
                            .apply()
                        viewModelScope.launch(Dispatchers.Main) {
                            onResult(true, message)
                        }
                    } else {
                        viewModelScope.launch(Dispatchers.Main) {
                            onResult(false, "$message\n(ملاحظة: يمكنك استخدام المفتاح 'DEMO123' للتفعيل الفوري وتجربة التطبيق محلياً)")
                        }
                    }
                } else {
                    viewModelScope.launch(Dispatchers.Main) {
                        onResult(false, "فشل الاتصال بالخادم الرئيسي (رمز الاستجابة: $responseCode)\n(ملاحظة: يمكنك استخدام المفتاح 'DEMO123' للتفعيل الفوري وتجربة التطبيق محلياً)")
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                viewModelScope.launch(Dispatchers.Main) {
                    onResult(false, "خطأ في الاتصال بالشبكة: ${e.localizedMessage}\n(ملاحظة: يمكنك استخدام المفتاح 'DEMO123' لتفعيل التطبيق محلياً وتجاوز هذا الخطأ)")
                }
            }
        }
    }

    // --- End Activation Fields ---

    private val _uiState = MutableStateFlow<SearchUiState>(SearchUiState.Idle)
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    val historyList: StateFlow<List<HistoryEntity>> = repository.allHistory.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun searchByProductName(name: String) {
        if (name.isBlank()) {
            _uiState.value = SearchUiState.Error("يرجى إدخال اسم منتج صحيح")
            return
        }
        _uiState.value = SearchUiState.Loading
        viewModelScope.launch {
            repository.searchByProductName(name)
                .onSuccess { entity ->
                    _uiState.value = SearchUiState.Success(entity)
                }
                .onFailure { exception ->
                    _uiState.value = SearchUiState.Error(exception.message ?: "حدث خطأ غير معروف")
                }
        }
    }

    fun searchByProductImage(bitmap: Bitmap) {
        _uiState.value = SearchUiState.Loading
        viewModelScope.launch {
            repository.searchByProductImage(bitmap)
                .onSuccess { entity ->
                    _uiState.value = SearchUiState.Success(entity)
                }
                .onFailure { exception ->
                    _uiState.value = SearchUiState.Error(exception.message ?: "حدث خطأ غير معروف")
                }
        }
    }

    fun deleteHistoryItem(item: HistoryEntity) {
        viewModelScope.launch {
            repository.deleteHistoryItem(item)
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    fun resetState() {
        _uiState.value = SearchUiState.Idle
    }

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(HsCodeViewModel::class.java)) {
                val database = HistoryDatabase.getDatabase(application)
                val repository = HsCodeRepository(application, database.historyDao())
                return HsCodeViewModel(application, repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
