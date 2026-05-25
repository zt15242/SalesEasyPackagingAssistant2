package top.javaxiaowu.saleseasypackagingassistant.utils

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import top.javaxiaowu.saleseasypackagingassistant.config.ApiConfig
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

/**
 * HTTP客户端工具类
 * 用于处理所有的API请求
 */
object HttpClient {
    
    private val gson = Gson()
    private var authToken: String? = null
    
    /**
     * 设置认证Token
     */
    fun setAuthToken(token: String?) {
        authToken = token
    }
    
    /**
     * 获取当前认证Token
     */
    fun getAuthToken(): String? = authToken
    
    /**
     * 清除认证Token
     */
    fun clearAuthToken() {
        authToken = null
    }
    
    /**
     * API响应结果
     */
    data class ApiResponse<T>(
        val success: Boolean,
        val data: T? = null,
        val message: String? = null,
        val code: Int = 200
    )
    
    /**
     * 分页响应结果
     */
    data class PaginatedResponse<T>(
        val total: Int,
        val items: List<T>
    )
    
    /**
     * 发送GET请求
     */
    fun <T> get(
        endpoint: String,
        queryParams: Map<String, String>? = null,
        requireAuth: Boolean = true,
        responseType: Class<T>
    ): ApiResponse<T> {
        return request("GET", endpoint, null, queryParams, requireAuth, responseType)
    }

    /**
     * 发送GET请求 (支持 Type)
     */
    fun <T> get(
        endpoint: String,
        queryParams: Map<String, String>? = null,
        requireAuth: Boolean = true,
        responseType: java.lang.reflect.Type
    ): ApiResponse<T> {
        return request("GET", endpoint, null, queryParams, requireAuth, responseType)
    }
    
    /**
     * 发送POST请求
     */
    fun <T> post(
        endpoint: String,
        body: Any? = null,
        queryParams: Map<String, String>? = null,
        requireAuth: Boolean = true,
        responseType: Class<T>
    ): ApiResponse<T> {
        return request("POST", endpoint, body, queryParams, requireAuth, responseType)
    }
    
    /**
     * 发送PUT请求
     */
    fun <T> put(
        endpoint: String,
        body: Any? = null,
        queryParams: Map<String, String>? = null,
        requireAuth: Boolean = true,
        responseType: Class<T>
    ): ApiResponse<T> {
        return request("PUT", endpoint, body, queryParams, requireAuth, responseType)
    }
    
    /**
     * 发送DELETE请求
     */
    fun <T> delete(
        endpoint: String,
        queryParams: Map<String, String>? = null,
        requireAuth: Boolean = true,
        responseType: Class<T>
    ): ApiResponse<T> {
        return request("DELETE", endpoint, null, queryParams, requireAuth, responseType)
    }
    
    /**
     * 通用请求方法
     */
    fun <T> requestFullUrl(
        method: String,
        url: String,
        body: Any? = null,
        headers: Map<String, String>? = null,
        responseType: Class<T>
    ): ApiResponse<T> {
        var connection: HttpURLConnection? = null
        
        try {
            val urlObj = URL(url)
            connection = urlObj.openConnection() as HttpURLConnection
            
            // 设置请求方法
            connection.requestMethod = method
            connection.connectTimeout = ApiConfig.Timeout.CONNECT.toInt()
            connection.readTimeout = ApiConfig.Timeout.READ.toInt()
            
            // 设置请求头
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Accept", "application/json")
            
            headers?.forEach { (key, value) ->
                connection.setRequestProperty(key, value)
            }
            
            // 发送请求体
            if (body != null && (method == "POST" || method == "PUT")) {
                connection.doOutput = true
                val jsonBody = if (body is String) body else gson.toJson(body)
                
                OutputStreamWriter(connection.outputStream, StandardCharsets.UTF_8).use { writer ->
                    writer.write(jsonBody)
                    writer.flush()
                }
            }
            
            // 读取响应
            val responseCode = connection.responseCode
            val inputStream = if (responseCode in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream
            }
            
            val response = BufferedReader(InputStreamReader(inputStream, StandardCharsets.UTF_8)).use { reader ->
                reader.readText()
            }
            
            // 调试：打印响应内容
            println("API Request: $method $url")
            println("API Response [$responseCode]: $response")
            
            // 解析响应
            return if (responseCode in 200..299) {
                try {
                    val data = if (responseType == String::class.java) {
                        @Suppress("UNCHECKED_CAST")
                        response as T
                    } else {
                        gson.fromJson(response, responseType)
                    }
                    ApiResponse(
                        success = true,
                        data = data,
                        code = responseCode
                    )
                } catch (e: Exception) {
                    println("JSON解析失败: ${e.message}")
                    ApiResponse(
                        success = false,
                        message = "响应解析失败: ${e.message}",
                        code = responseCode
                    )
                }
            } else {
                 val errorMessage = try {
                    @Suppress("UNCHECKED_CAST")
                    val errorJson = gson.fromJson(response, Map::class.java) as? Map<*, *>
                    errorJson?.get("detail")?.toString() 
                        ?: errorJson?.get("message")?.toString() 
                        ?: "请求失败"
                } catch (e: Exception) {
                    "请求失败: HTTP $responseCode"
                }
                
                ApiResponse(
                    success = false,
                    message = errorMessage,
                    code = responseCode
                )
            }
            
        } catch (e: Exception) {
            return ApiResponse(
                success = false,
                message = "网络请求异常: ${e.message}",
                code = 500
            )
        } finally {
            connection?.disconnect()
        }
    }

    private fun <T> request(
        method: String,
        endpoint: String,
        body: Any? = null,
        queryParams: Map<String, String>? = null,
        requireAuth: Boolean = true,
        responseType: java.lang.reflect.Type
    ): ApiResponse<T> {
        var connection: HttpURLConnection? = null
        
        try {
            // 构建URL
            var urlString = ApiConfig.getApiUrl(endpoint)
            if (!queryParams.isNullOrEmpty()) {
                val queryString = queryParams.entries.joinToString("&") { (key, value) ->
                    "$key=${java.net.URLEncoder.encode(value, "UTF-8")}"
                }
                urlString += "?$queryString"
            }
            
            val url = URL(urlString)
            connection = url.openConnection() as HttpURLConnection
            
            // 设置请求方法
            connection.requestMethod = method
            connection.connectTimeout = ApiConfig.Timeout.CONNECT.toInt()
            connection.readTimeout = ApiConfig.Timeout.READ.toInt()
            
            // 设置请求头
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Accept", "application/json")
            
            // 添加认证Token
            if (requireAuth && authToken != null) {
                connection.setRequestProperty("Authorization", "Bearer $authToken")
            }
            
            // 发送请求体
            if (body != null && (method == "POST" || method == "PUT")) {
                connection.doOutput = true
                val jsonBody = if (body is String) body else gson.toJson(body)
                
                OutputStreamWriter(connection.outputStream, StandardCharsets.UTF_8).use { writer ->
                    writer.write(jsonBody)
                    writer.flush()
                }
            }
            
            // 读取响应
            val responseCode = connection.responseCode
            val inputStream = if (responseCode in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream
            }
            
            val response = BufferedReader(InputStreamReader(inputStream, StandardCharsets.UTF_8)).use { reader ->
                reader.readText()
            }
            
            // 调试：打印响应内容
            println("API Request: $method $urlString")
            println("API Response [$responseCode]: $response")
            
            // 解析响应
            return if (responseCode in 200..299) {
                try {
                    val data = if (responseType == String::class.java) {
                        @Suppress("UNCHECKED_CAST")
                        response as T
                    } else {
                        gson.fromJson(response, responseType)
                    }
                    ApiResponse(
                        success = true,
                        data = data,
                        code = responseCode
                    )
                } catch (e: Exception) {
                    println("JSON解析失败: ${e.message}")
                    ApiResponse(
                        success = false,
                        message = "响应解析失败: ${e.message}",
                        code = responseCode
                    )
                }
            } else {
                // 尝试解析错误信息
                val errorMessage = try {
                    @Suppress("UNCHECKED_CAST")
                    val errorJson = gson.fromJson(response, Map::class.java) as? Map<*, *>
                    errorJson?.get("detail")?.toString() 
                        ?: errorJson?.get("message")?.toString() 
                        ?: "请求失败"
                } catch (e: Exception) {
                    "请求失败: HTTP $responseCode"
                }
                
                ApiResponse(
                    success = false,
                    message = errorMessage,
                    code = responseCode
                )
            }
            
        } catch (e: Exception) {
            return ApiResponse(
                success = false,
                message = "网络请求异常: ${e.message}",
                code = 500
            )
        } finally {
            connection?.disconnect()
        }
    }
    
    /**
     * 发送POST请求（表单数据）
     * 用于登录等场景
     */
    fun <T> postForm(
        endpoint: String,
        formData: Map<String, String>,
        requireAuth: Boolean = false,
        responseType: Class<T>
    ): ApiResponse<T> {
        var connection: HttpURLConnection? = null
        
        try {
            val url = URL(ApiConfig.getApiUrl(endpoint))
            connection = url.openConnection() as HttpURLConnection
            
            connection.requestMethod = "POST"
            connection.connectTimeout = ApiConfig.Timeout.CONNECT.toInt()
            connection.readTimeout = ApiConfig.Timeout.READ.toInt()
            connection.doOutput = true
            
            // 设置请求头
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            connection.setRequestProperty("Accept", "application/json")
            
            if (requireAuth && authToken != null) {
                connection.setRequestProperty("Authorization", "Bearer $authToken")
            }
            
            // 构建表单数据
            val formBody = formData.entries.joinToString("&") { (key, value) ->
                "$key=${java.net.URLEncoder.encode(value, "UTF-8")}"
            }
            
            // 发送请求
            OutputStreamWriter(connection.outputStream, StandardCharsets.UTF_8).use { writer ->
                writer.write(formBody)
                writer.flush()
            }
            
            // 读取响应
            val responseCode = connection.responseCode
            val inputStream = if (responseCode in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream
            }
            
            val response = BufferedReader(InputStreamReader(inputStream, StandardCharsets.UTF_8)).use { reader ->
                reader.readText()
            }
            
            // 调试：打印响应内容
            println("API Response [$responseCode]: $response")
            
            // 解析响应
            return if (responseCode in 200..299) {
                try {
                    val data = gson.fromJson(response, responseType)
                    ApiResponse(
                        success = true,
                        data = data,
                        code = responseCode
                    )
                } catch (e: Exception) {
                    println("JSON解析失败: ${e.message}")
                    println("响应内容: $response")
                    ApiResponse(
                        success = false,
                        message = "响应解析失败: ${e.message}",
                        code = responseCode
                    )
                }
            } else {
                val errorMessage = try {
                    @Suppress("UNCHECKED_CAST")
                    val errorJson = gson.fromJson(response, Map::class.java) as? Map<*, *>
                    errorJson?.get("detail")?.toString() ?: "请求失败"
                } catch (e: Exception) {
                    "请求失败: HTTP $responseCode"
                }
                
                ApiResponse(
                    success = false,
                    message = errorMessage,
                    code = responseCode
                )
            }
            
        } catch (e: Exception) {
            return ApiResponse(
                success = false,
                message = "网络请求异常: ${e.message}",
                code = 500
            )
        } finally {
            connection?.disconnect()
        }
    }
    /**
     * 上传文件 (Multipart)
     */
    fun <T> uploadFile(
        endpoint: String,
        file: java.io.File,
        params: Map<String, String>? = null,
        requireAuth: Boolean = true,
        responseType: Class<T>
    ): ApiResponse<T> {
        var connection: HttpURLConnection? = null
        val boundary = "---" + System.currentTimeMillis() + "---"
        val lineFeed = "\r\n"
        
        try {
            val url = URL(ApiConfig.getApiUrl(endpoint))
            connection = url.openConnection() as HttpURLConnection
            
            connection.requestMethod = "POST"
            connection.connectTimeout = ApiConfig.Timeout.CONNECT.toInt()
            connection.readTimeout = ApiConfig.Timeout.WRITE.toInt() // Upload might take longer
            connection.doOutput = true
            connection.doInput = true
            
            // Set Headers
            connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
            connection.setRequestProperty("Accept", "application/json")
            if (requireAuth && authToken != null) {
                connection.setRequestProperty("Authorization", "Bearer $authToken")
            }
            
            val outputStream = connection.outputStream
            val writer = java.io.PrintWriter(OutputStreamWriter(outputStream, StandardCharsets.UTF_8), true)
            
            // Add Params
            params?.forEach { (key, value) ->
                writer.append("--$boundary").append(lineFeed)
                writer.append("Content-Disposition: form-data; name=\"$key\"").append(lineFeed)
                writer.append("Content-Type: text/plain; charset=UTF-8").append(lineFeed)
                writer.append(lineFeed)
                writer.append(value).append(lineFeed)
                writer.flush()
            }
            
            // Add File
            val fileName = file.name
            writer.append("--$boundary").append(lineFeed)
            writer.append("Content-Disposition: form-data; name=\"file\"; filename=\"$fileName\"").append(lineFeed)
            writer.append("Content-Type: application/zip").append(lineFeed) // Assuming zip for this context, or guess mime type
            writer.append("Content-Transfer-Encoding: binary").append(lineFeed)
            writer.append(lineFeed)
            writer.flush()
            
            // Write File Data
            java.io.FileInputStream(file).use { inputStream ->
                val buffer = ByteArray(4096)
                var bytesRead: Int
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                }
            }
            outputStream.flush()
            
            writer.append(lineFeed)
            writer.flush()
            
            // End of multipart
            writer.append("--$boundary--").append(lineFeed)
            writer.close()
            
            // Read Response
            val responseCode = connection.responseCode
            val inputStream = if (responseCode in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream
            }
            
            val response = BufferedReader(InputStreamReader(inputStream, StandardCharsets.UTF_8)).use { reader ->
                reader.readText()
            }
            
            println("API Upload Response [$responseCode]: $response")
            
             return if (responseCode in 200..299) {
                try {
                    val data = gson.fromJson(response, responseType)
                    ApiResponse(true, data, code = responseCode)
                } catch (e: Exception) {
                    ApiResponse(false, message = "响应解析失败: ${e.message}", code = responseCode)
                }
            } else {
                 val errorMessage = try {
                    @Suppress("UNCHECKED_CAST")
                    val errorJson = gson.fromJson(response, Map::class.java) as? Map<*, *>
                    errorJson?.get("detail")?.toString() ?: errorJson?.get("message")?.toString() ?: "上传失败"
                } catch (e: Exception) {
                    "上传失败: HTTP $responseCode"
                }
                ApiResponse(false, message = errorMessage, code = responseCode)
            }
            
        } catch (e: Exception) {
            e.printStackTrace()
             return ApiResponse(false, message = "上传异常: ${e.message}", code = 500)
        } finally {
            connection?.disconnect()
        }
    }
}
