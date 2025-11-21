package top.javaxiaowu.saleseasypackagingassistant.utils

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import java.io.File

data class VersionRecord(
    val path: String,
    val version: String,
    val lastExportTime: Long
)

object ExportVersionManager {
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()
    
    /**
     * 获取项目根目录的版本记录文件路径
     */
    private fun getVersionRecordFile(projectRoot: String): File {
        val versionDir = File(projectRoot, ".SalesEasy")
        if (!versionDir.exists()) {
            versionDir.mkdirs()
        }
        return File(versionDir, "versions.json")
    }
    
    /**
     * 读取所有版本记录
     */
    private fun readVersionRecords(projectRoot: String): Map<String, VersionRecord> {
        val recordFile = getVersionRecordFile(projectRoot)
        if (!recordFile.exists()) {
            return emptyMap()
        }
        
        return try {
            val jsonString = recordFile.readText(Charsets.UTF_8)
            if (jsonString.isBlank()) {
                emptyMap()
            } else {
                val jsonObject = gson.fromJson(jsonString, JsonObject::class.java)
                val result = mutableMapOf<String, VersionRecord>()
                
                for ((key, value) in jsonObject.entrySet()) {
                    try {
                        if (value.isJsonObject) {
                            val obj = value.asJsonObject
                            val path = obj.get("path")?.asString ?: continue
                            val version = obj.get("version")?.asString ?: continue
                            val lastExportTime = obj.get("lastExportTime")?.asLong ?: System.currentTimeMillis()
                            result[key] = VersionRecord(path, version, lastExportTime)
                        }
                    } catch (e: Exception) {
                        println("Error parsing record for key $key: ${e.message}")
                    }
                }
                result
            }
        } catch (e: Exception) {
            println("Error reading version records: ${e.message}")
            emptyMap()
        }
    }
    
    /**
     * 保存版本记录
     */
    private fun saveVersionRecords(projectRoot: String, records: Map<String, VersionRecord>) {
        val recordFile = getVersionRecordFile(projectRoot)
        try {
            val recordsMap = records.mapValues { (_, record) ->
                mapOf(
                    "path" to record.path,
                    "version" to record.version,
                    "lastExportTime" to record.lastExportTime
                )
            }
            val jsonString = gson.toJson(recordsMap)
            recordFile.writeText(jsonString, Charsets.UTF_8)
        } catch (e: Exception) {
            println("Error saving version records: ${e.message}")
        }
    }
    
    /**
     * 提取3级路径（用于版本管理的key）
     * 例如：other.rainbow.inventoryview.controller.InventoryViewController -> other.rainbow.inventoryview
     */
    private fun extractThreeLevelPath(fullPath: String): String {
        val parts = fullPath.split(".")
        return if (parts.size >= 3) {
            parts.take(3).joinToString(".")
        } else {
            fullPath
        }
    }
    
    /**
     * 获取指定路径的当前版本号
     */
    fun getVersionForPath(projectRoot: String, fullPath: String): String {
        val threeLevel = extractThreeLevelPath(fullPath)
        val records = readVersionRecords(projectRoot)
        val record = records[threeLevel]
        return record?.version ?: "V1.0"
    }
    
    /**
     * 递增指定路径的版本号
     */
    fun incrementVersionForPath(projectRoot: String, fullPath: String): String {
        val threeLevel = extractThreeLevelPath(fullPath)
        val records = readVersionRecords(projectRoot).toMutableMap()
        
        val currentRecord = records[threeLevel]
        val newVersion = if (currentRecord != null) {
            incrementVersion(currentRecord.version)
        } else {
            "V1.0"
        }
        
        val newRecord = VersionRecord(threeLevel, newVersion, System.currentTimeMillis())
        records[threeLevel] = newRecord
        saveVersionRecords(projectRoot, records)
        
        println("Version incremented for path: $threeLevel -> $newVersion")
        return newVersion
    }
    
    /**
     * 为导出文件集合递增版本号
     * 返回使用最新版本号的路径
     */
    fun incrementVersionsForExport(projectRoot: String, paths: List<String>): String {
        var latestVersion = "V1.0"
        
        for (path in paths) {
            val version = incrementVersionForPath(projectRoot, path)
            if (compareVersions(version, latestVersion) > 0) {
                latestVersion = version
            }
        }
        
        return latestVersion
    }
    
    /**
     * 版本号递增逻辑：V1.0 -> V2.0 -> V3.0（每次递增主版本号）
     */
    private fun incrementVersion(currentVersion: String): String {
        return try {
            val versionParts = currentVersion.removePrefix("V").split(".")
            if (versionParts.size >= 1) {
                val major = versionParts[0].toInt()
                "V${major + 1}.0"
            } else {
                "V2.0"
            }
        } catch (e: Exception) {
            "V2.0"
        }
    }
    
    /**
     * 比较两个版本号大小
     * 返回值: > 0 表示 version1 > version2，0 表示相等，< 0 表示 version1 < version2
     */
    private fun compareVersions(version1: String, version2: String): Int {
        val v1Parts = version1.removePrefix("V").split(".").mapNotNull { it.toIntOrNull() }
        val v2Parts = version2.removePrefix("V").split(".").mapNotNull { it.toIntOrNull() }
        
        if (v1Parts.isEmpty() || v2Parts.isEmpty()) return 0
        
        val majorCompare = v1Parts[0].compareTo(v2Parts[0])
        if (majorCompare != 0) return majorCompare
        
        val minorCompare = (v1Parts.getOrNull(1) ?: 0).compareTo(v2Parts.getOrNull(1) ?: 0)
        return minorCompare
    }
    
    /**
     * 获取所有版本记录（用于调试）
     */
    fun getAllVersionRecords(projectRoot: String): Map<String, VersionRecord> {
        return readVersionRecords(projectRoot)
    }
}
