package top.javaxiaowu.saleseasypackagingassistant.utils

import com.intellij.openapi.vfs.VirtualFile
import top.javaxiaowu.saleseasypackagingassistant.ui.ScriptTriggerConfig
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object ZipPackageUtil {
    fun createZip(selectedFiles: List<VirtualFile>, zipFile: File, baseDir: VirtualFile, scriptTriggerConfigs: List<ScriptTriggerConfig>? = null, projectRoot: String? = null) {
        zipFile.parentFile?.mkdirs()
        
        // 处理版本号并修改ZIP文件名
        val finalZipFile = if (projectRoot != null) {
            val paths = selectedFiles.map { it.path }
            val version = ExportVersionManager.incrementVersionsForExport(projectRoot, paths)
            val originalName = zipFile.name
            val nameWithoutExt = originalName.substringBeforeLast(".")
            val newZipFile = File(zipFile.parentFile, "${nameWithoutExt}_${version}.zip")
            newZipFile
        } else {
            zipFile
        }
        
        
        // 移除重复项和被包含的子项
        val uniqueFiles = mutableListOf<VirtualFile>()
        val sortedFiles = selectedFiles.sortedBy { it.path }
        
        for (file in sortedFiles) {
            // 检查这个文件是否已经被添加过的文件包含
            var isIncluded = false
            for (existing in uniqueFiles) {
                if (existing.isDirectory && file.path.startsWith(existing.path + File.separator)) {
                    isIncluded = true
                    break
                }
            }
            
            if (!isIncluded) {
                // 移除被当前文件包含的已添加文件
                uniqueFiles.removeAll { existing ->
                    file.isDirectory && existing.path.startsWith(file.path + File.separator)
                }
                uniqueFiles.add(file)
            }
        }
        
        // 查找 src/main/java 作为basePath
        // 无论你选择什么，都会基于 src/main/java 来计算相对路径
        // 这样就是 IDEA Copy Reference 的方式
        val basePath = findSourceRoot(uniqueFiles)
        println("DEBUG: Calculated basePath = $basePath")
        
        val addedPaths = mutableSetOf<String>()
        ZipOutputStream(finalZipFile.outputStream()).use { zos ->
            for (file in uniqueFiles) {
                addFileToZip(zos, file, basePath, addedPaths)
            }
            
            // 如果需要生成 scriptTrigger.xml，添加到压缩包
            if (scriptTriggerConfigs != null && scriptTriggerConfigs.isNotEmpty()) {
                val xmlContent = ScriptTriggerXmlGenerator.generateScriptTriggerXml(scriptTriggerConfigs)
                if (!addedPaths.contains("scriptTrigger.xml")) {
                    try {
                        val entry = ZipEntry("scriptTrigger.xml")
                        zos.putNextEntry(entry)
                        val bytes = xmlContent.toByteArray(Charsets.UTF_8)
                        zos.write(bytes, 0, bytes.size)
                        zos.closeEntry()
                        addedPaths.add("scriptTrigger.xml")
                        println("DEBUG: Generated and added scriptTrigger.xml to root")
                    } catch (e: Exception) {
                        println("DEBUG: Error adding generated scriptTrigger.xml: ${e.message}")
                        throw e
                    }
                }
            }
        }
    }

    private fun findSourceRoot(files: List<VirtualFile>): String {
        // 从任一个文件开始遍历，查找 src/main/java
        for (file in files) {
            var currentPath = file.path
            val segments = currentPath.split("/").toMutableList()
            
            // 从后往前查找 src/main/java
            val javaIndex = segments.lastIndexOf("java")
            if (javaIndex >= 2) {
                // 检查 java 前面是不是 main 和 src
                if (segments[javaIndex - 1] == "main" && segments[javaIndex - 2] == "src") {
                    // 找到了 src/main/java
                    return segments.subList(0, javaIndex + 1).joinToString("/")
                }
            }
        }
        
        // 如果找不到，返回空串
        return ""
    }

    private fun findCommonParent(paths: List<String>): String? {
        if (paths.isEmpty()) return null
        if (paths.size == 1) {
            val file = File(paths[0])
            return if (file.isDirectory) file.path else file.parent
        }

        val allParts = paths.map { it.split(File.separator) }
        var commonIndex = 0
        
        while (commonIndex < allParts[0].size) {
            val part = allParts[0][commonIndex]
            if (allParts.all { it.size > commonIndex && it[commonIndex] == part }) {
                commonIndex++
            } else {
                break
            }
        }
        
        return if (commonIndex > 0) {
            allParts[0].take(commonIndex).joinToString(File.separator)
        } else {
            null
        }
    }

    private fun addFileToZip(zos: ZipOutputStream, file: VirtualFile, basePath: String, addedPaths: MutableSet<String>) {
        // 计算相对路径
        val filePath = file.path
        
        // 特殊处理：scriptTrigger.xml 文件，直接放到压缩包根目录
        if (file.name == "scriptTrigger.xml") {
            if (!addedPaths.contains("scriptTrigger.xml")) {
                try {
                    val entry = ZipEntry("scriptTrigger.xml")
                    zos.putNextEntry(entry)
                    val bytes = file.contentsToByteArray()
                    zos.write(bytes, 0, bytes.size)
                    zos.closeEntry()
                    addedPaths.add("scriptTrigger.xml")
                    println("DEBUG: Added scriptTrigger.xml directly to root")
                } catch (e: Exception) {
                    println("DEBUG: Error adding scriptTrigger.xml: ${e.message}")
                    throw e
                }
            }
            return
        }
        
        val relativePath = if (filePath.startsWith(basePath + "/")) {
            // 如果是 basePath/something 的形式，需要去掉前缀和最前面的 /
            filePath.substring(basePath.length + 1)  // +1 是为了钐二 basePath 后面的 /
        } else if (filePath == basePath) {
            // 如果是 basePath 本身，不需要添加
            ""
        } else {
            // 鹰谌曲三段，使用文件名
            file.name
        }
        
        println("DEBUG: addFileToZip - filePath=$filePath, basePath=$basePath, relativePath=$relativePath")

        if (file.isDirectory) {
            // 处理目录 - 在 ZIP 中添加目录条目
            if (relativePath.isNotEmpty()) {
                val dirPath = relativePath + "/"
                if (!addedPaths.contains(dirPath)) {
                    val dirEntry = ZipEntry(dirPath)
                    zos.putNextEntry(dirEntry)
                    zos.closeEntry()
                    addedPaths.add(dirPath)
                }
            }
            
            // 递归添加子文件
            try {
                file.children.forEach { child ->
                    addFileToZip(zos, child, basePath, addedPaths)
                }
            } catch (e: Exception) {
                println("DEBUG: Error processing directory ${file.path}: ${e.message}")
            }
        } else {
            // 处理文件
            if (relativePath.isNotEmpty() && !addedPaths.contains(relativePath)) {
                try {
                    val entry = ZipEntry(relativePath)
                    zos.putNextEntry(entry)
                    
                    val bytes = file.contentsToByteArray()
                    zos.write(bytes, 0, bytes.size)
                    zos.closeEntry()
                    addedPaths.add(relativePath)
                } catch (e: Exception) {
                    println("DEBUG: Error adding file ${file.path}: ${e.message}")
                    throw e
                }
            }
        }
    }
}
