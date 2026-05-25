package top.javaxiaowu.saleseasypackagingassistant.ui

import com.google.gson.Gson
import com.intellij.icons.AllIcons
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.table.JBTable
import top.javaxiaowu.saleseasypackagingassistant.config.ApiConfig
import top.javaxiaowu.saleseasypackagingassistant.utils.HttpClient
import top.javaxiaowu.saleseasypackagingassistant.utils.HttpClient.PaginatedResponse
import com.google.gson.reflect.TypeToken
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Dimension
import java.awt.FlowLayout
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import javax.swing.*
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableCellEditor
import javax.swing.table.TableCellRenderer
import org.jetbrains.idea.maven.project.MavenProjectsManager
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.ui.components.JBTextField
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.fileChooser.FileChooser

class CodeSyncDialog(
    private val project: Project?,
    private val envId: String,
    private val envUrl: String,
    private val projectId: String
) : DialogWrapper(project) {

    private val tableModel: DefaultTableModel
    private val table: JBTable
    private val gson = Gson()
    
    // Pagination controls
    private val pageLabel = JLabel("第 1 页")
    private val prevButton = JButton("<")
    private val nextButton = JButton(">")
    private val pageSizeCombo = JComboBox(arrayOf(10, 20, 50, 100))
    private val searchField = JBTextField(20)
    private val searchButton = JButton("搜索")
    
    // State
    private var currentPage = 1
    private var pageSize = 20
    private var totalRecords = 0
    private var cookies: String? = null
    private var searchKeyword = ""
    private var packageList = mutableListOf<PackageItem>()
    
    // Config
    private var downloadBasePath: String = project?.basePath ?: ""
    private val pathLabel = JLabel()

    init {
        title = "代码同步"
        
        // Setup Table
        val columnNames = arrayOf("代码包名称", "Package", "当前版本", "上传时间", "最新上传人", "操作")
        tableModel = object : DefaultTableModel(columnNames, 0) {
            override fun isCellEditable(row: Int, column: Int): Boolean {
                return column == 5
            }
        }
        table = JBTable(tableModel)
        setupTable()

        init()
        
        // Load saved path
        if (project != null) {
            val savedPath = com.intellij.ide.util.PropertiesComponent.getInstance(project).getValue("SalesEasy.CodeSync.DownloadPath")
            if (!savedPath.isNullOrEmpty() && File(savedPath).exists()) {
                downloadBasePath = savedPath
            }
        }
        updatePathLabel()
        
        // Initial Fetch
        fetchCookiesAndData()
    }
    
    private fun updatePathLabel() {
        // Truncate if too long
        val maxLen = 50
        var text = downloadBasePath
        if (text.length > maxLen) {
            text = "..." + text.takeLast(maxLen)
        }
        pathLabel.text = text
        pathLabel.toolTipText = downloadBasePath
    }

    private fun setupTable() {
        table.columnModel.getColumn(5).cellRenderer = ActionButtonRenderer()
        table.columnModel.getColumn(5).cellEditor = ActionButtonEditor()
        
        table.columnModel.getColumn(0).preferredWidth = 120
        table.columnModel.getColumn(1).preferredWidth = 200
        table.columnModel.getColumn(2).preferredWidth = 80
        table.columnModel.getColumn(3).preferredWidth = 150
        table.columnModel.getColumn(4).preferredWidth = 100
        table.columnModel.getColumn(5).preferredWidth = 160
        table.rowHeight = 30
    }

    override fun createCenterPanel(): JComponent {
        val panel = JPanel(BorderLayout())
        panel.preferredSize = Dimension(900, 500)

        // Config Panel (Top)
        val configPanel = JPanel(FlowLayout(FlowLayout.LEFT))
        configPanel.add(JLabel("同步根目录:"))
        configPanel.add(pathLabel)
        val changePathBtn = JButton("更改")
        changePathBtn.addActionListener {
            val descriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor()
            descriptor.title = "选择同步根目录"
            val defaultPath = LocalFileSystem.getInstance().findFileByPath(downloadBasePath)
            val selectedDir = FileChooser.chooseFile(descriptor, project, defaultPath)
            
            if (selectedDir != null) {
                downloadBasePath = selectedDir.path
                updatePathLabel()
                if (project != null) {
                   com.intellij.ide.util.PropertiesComponent.getInstance(project).setValue("SalesEasy.CodeSync.DownloadPath", downloadBasePath)
                }
            }
        }
        configPanel.add(changePathBtn)
        
        panel.add(configPanel, BorderLayout.NORTH)

        // Toolbar / Pagination
        val toolbar = JPanel(FlowLayout(FlowLayout.RIGHT))
        
        prevButton.addActionListener {
            if (currentPage > 1) {
                currentPage--
                fetchData()
            }
        }
        
        nextButton.addActionListener {
            // Simple check, real logic depends on total records
            currentPage++
            fetchData()
        }
        
        pageSizeCombo.selectedItem = pageSize
        pageSizeCombo.addActionListener {
            val newSize = pageSizeCombo.selectedItem as Int
            if (newSize != pageSize) {
                pageSize = newSize
                currentPage = 1
                fetchData()
            }
        }
        
    
        // Search bar
        val searchPanel = JPanel(FlowLayout(FlowLayout.LEFT))
        searchPanel.add(JLabel("搜索:"))
        searchPanel.add(searchField)
        searchPanel.add(searchButton)
        
        searchButton.addActionListener {
            searchKeyword = searchField.text.trim()
            currentPage = 1
            fetchData()
        }

        toolbar.add(searchPanel)
        toolbar.add(JLabel("每页显示:"))
        toolbar.add(pageSizeCombo)
        toolbar.add(prevButton)
        toolbar.add(pageLabel)
        toolbar.add(nextButton)
        
        panel.add(JBScrollPane(table), BorderLayout.CENTER)
        panel.add(toolbar, BorderLayout.SOUTH)

        return panel
    }
    
    private fun fetchCookiesAndData() {
        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "获取环境信息...", false) {
            override fun run(indicator: ProgressIndicator) {
                try {
                    // Fetch Cookies
                    val cookieEndpoint = ApiConfig.Environments.COOKIES.replace("{environment_id}", envId)
                    val cookieResponse = HttpClient.get(cookieEndpoint, requireAuth = true, responseType = Map::class.java)
                    
                    if (cookieResponse.success && cookieResponse.data != null) {
                        try {
                             @Suppress("UNCHECKED_CAST")
                             val respData = cookieResponse.data as Map<String, Any>
                             val cookieList = respData["cookies"] as? List<Map<String, Any>>
                             
                             val sb = StringBuilder()
                             
                             if (cookieList != null) {
                                 // Handle new structure: { "cookies": [ {"name": "...", "value": "..."} ] }
                                 cookieList.forEach { cookie ->
                                     val name = cookie["name"] as? String
                                     val value = cookie["value"] as? String
                                     if (name != null && value != null) {
                                         sb.append("$name=$value; ")
                                     }
                                 }
                             } else {
                                 // Fallback or Handle simple map structure if API changes
                                 // Try to treat as flat map just in case?
                                 respData.forEach { (k, v) ->
                                     if (k != "cookies" && v is String) {
                                        sb.append("$k=$v; ")
                                     }
                                 }
                             }
                             
                             cookies = sb.toString()
                        } catch (e: Exception) {
                            println("Cookie 解析错误: ${e.message}")
                            cookies = ""
                        }
                        
                        // Fetch Data
                        fetchDataInternal()
                    } else {
                        SwingUtilities.invokeLater {
                            JOptionPane.showMessageDialog(rootPane, "无法获取环境Cookie: ${cookieResponse.message}", "错误", JOptionPane.ERROR_MESSAGE)
                        }
                    }
                } catch (e: Exception) {
                    SwingUtilities.invokeLater {
                        JOptionPane.showMessageDialog(rootPane, "发生错误: ${e.message}", "错误", JOptionPane.ERROR_MESSAGE)
                    }
                }
            }
        })
    }
    
    private fun fetchData() {
        if (cookies == null) {
            fetchCookiesAndData() // Retry cookie fetch if missing
            return
        }
        
        // UI Feedback
        searchButton.isEnabled = false
        searchButton.text = "搜索中..."
        
        ApplicationManager.getApplication().executeOnPooledThread {
            try {
                fetchDataInternal()
            } finally {
                SwingUtilities.invokeLater {
                    searchButton.isEnabled = true
                    searchButton.text = "搜索"
                }
            }
        }
    }
    
    private fun fetchDataInternal() {
        try {
            // /rest/metadata/v2.0/dx/logic/packages?type=0&pageNo=1&pageSize=20
            val sb = StringBuilder("$envUrl/rest/metadata/v2.0/dx/logic/packages?type=0&pageNo=$currentPage&pageSize=$pageSize")
            if (searchKeyword.isNotEmpty()) {
                val encodedKw = java.net.URLEncoder.encode(searchKeyword, "UTF-8")
                // Try multiple common parameter names since API doc is not available
                sb.append("&keyword=$encodedKw")
                sb.append("&q=$encodedKw")
                sb.append("&name=$encodedKw")
                sb.append("&codePackageName=$encodedKw")
            }
            val targetUrl = sb.toString()
            
            val headers = mapOf("Cookie" to (cookies ?: ""))
            
            val response = HttpClient.requestFullUrl("GET", targetUrl, null, headers, Map::class.java)
            
            SwingUtilities.invokeLater {
                if (response.success && response.data != null) {
                    processResponse(response.data as Map<String, Any>)
                } else {
                    JOptionPane.showMessageDialog(rootPane, "请求失败: ${response.message}", "错误", JOptionPane.ERROR_MESSAGE)
                }
                updatePaginationControls()
            }
        } catch (e: Exception) {
            SwingUtilities.invokeLater {
                JOptionPane.showMessageDialog(rootPane, "数据获取异常: ${e.message}", "错误", JOptionPane.ERROR_MESSAGE)
            }
        }
    }
    
    private fun processResponse(rootData: Map<String, Any>) {
        tableModel.rowCount = 0
        packageList.clear()
        
        // Parse: root -> data -> records -> codePackageList
        val dataObj = rootData["data"] as? Map<String, Any>
        val recordsObj = dataObj?.get("records") as? Map<String, Any>
        
        totalRecords = (recordsObj?.get("counts") as? Number)?.toInt() ?: 0
        
        val list = recordsObj?.get("codePackageList") as? List<Map<String, Any>> ?: emptyList()
        
        val sdf = java.text.SimpleDateFormat("yyyy/MM/dd HH:mm")
        
        list.forEach { item ->
            val codePackageName = item["codePackageName"] as? String ?: ""
            val codePackageTitle = item["codePackageTitle"] as? String ?: item["packageTitle"] as? String ?: ""
            val instanceVersion = item["instanceVersion"] as? String ?: ""
            
            val updatedAt = (item["updatedAt"] as? Number)?.toLong() ?: 0L
            val timeStr = if (updatedAt > 0) sdf.format(java.util.Date(updatedAt)) else ""
            
            val userName = item["userName"] as? String ?: ""
            val uploadFile = item["uploadFile"] as? String ?: ""
            
            tableModel.addRow(arrayOf(
               codePackageName, codePackageTitle, instanceVersion, timeStr, userName, ""
            ))
            
            packageList.add(PackageItem(
                codePackageName, codePackageTitle, instanceVersion, timeStr, userName, uploadFile
            ))
        }
        
        val totalPages = if (pageSize > 0) (totalRecords + pageSize - 1) / pageSize else 0
        pageLabel.text = "第 $currentPage 页 / 共 $totalPages 页"
        
        // Update next button state
        nextButton.isEnabled = currentPage < totalPages
    }

    private fun updatePaginationControls() {
        prevButton.isEnabled = currentPage > 1
        // nextButton state is updated in processResponse based on actual totals
    }
    
    // Actions
    private fun handleDownload(row: Int) {
        if (row < 0 || row >= packageList.size) return
        val item = packageList[row]
        
        if (item.uploadFile.isEmpty()) {
            JOptionPane.showMessageDialog(rootPane, "暂无下载链接", "提示", JOptionPane.WARNING_MESSAGE)
            return
        }
        
        // 1. Parsing Module Info
        // packageTitle: "other.xsy.bid"
        // groupId: "other.xsy"
        // artifactId: "bid"
        val parts = item.pkgName.split(".")
        val artifactId = if (parts.size >= 3) parts[2] else item.pkgName.replace(".", "_")
        val groupId = if (parts.size >= 3) parts.subList(0, 2).joinToString(".") else "other.xsy"
        val packagePath = item.pkgName.replace(".", "/")
        
        val projectBase = project?.basePath
        if (projectBase == null) {
            JOptionPane.showMessageDialog(rootPane, "无法确定项目根目录", "错误", JOptionPane.ERROR_MESSAGE)
            return
        }

        // Use configured path
        val targetBaseDir = File(downloadBasePath)
        if (!targetBaseDir.exists()) {
             targetBaseDir.mkdirs()
        }
        
        val targetModuleDir = File(targetBaseDir, artifactId)
        
        val confirm = JOptionPane.showConfirmDialog(
            rootPane, 
            "确定要下载并创建 Maven 模块 '$artifactId' 吗？\n" +
            "GroupId: $groupId\n" +
            "Package: ${item.pkgName}\n" +
            "将在 ${targetModuleDir.path} 创建以及覆盖。", 
            "确认下载", 
            JOptionPane.YES_NO_OPTION
        )
        
        if (confirm != JOptionPane.YES_OPTION) return
        
        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "正在处理模块...", true) {
            override fun run(indicator: ProgressIndicator) {
                try {
                    // 2. Create Directory Structure
                    if (!targetModuleDir.exists()) targetModuleDir.mkdirs()
                    
                    // Clean src/main/java if exists (to avoid duplicates/stale files)
                    val javaSrcDir = File(targetModuleDir, "src/main/java")
                    if (javaSrcDir.exists()) {
                        indicator.text = "清理旧文件..."
                        javaSrcDir.deleteRecursively()
                    }
                    javaSrcDir.mkdirs()
                    
                    // Create src/main/resources
                    val resourcesDir = File(targetModuleDir, "src/main/resources")
                    resourcesDir.mkdirs()
                    
                    // 3. Create pom.xml if not exists
                    val pomFile = File(targetModuleDir, "pom.xml")
                    if (!pomFile.exists()) {
                        indicator.text = "生成 pom.xml..."
                        val pomContent = """
                            <?xml version="1.0" encoding="UTF-8"?>
                            <project xmlns="http://maven.apache.org/POM/4.0.0"
                                     xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                                     xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                                <modelVersion>4.0.0</modelVersion>

                                <groupId>$groupId</groupId>
                                <artifactId>$artifactId</artifactId>
                                <version>1.0-SNAPSHOT</version>
                                
                                <properties>
                                    <maven.compiler.source>8</maven.compiler.source>
                                    <maven.compiler.target>8</maven.compiler.target>
                                </properties>

                            </project>
                        """.trimIndent()
                        FileOutputStream(pomFile).use { it.write(pomContent.toByteArray()) }
                    }
                    
                    // 3.5 Copy oauthConfig.properties from Project Root
                    copyOAuthConfig(projectBase ?: "", resourcesDir)
                    
                    // 4. Download Zip
                    indicator.text = "正在下载..."
                    val downloadUrl = if (item.uploadFile.startsWith("http")) item.uploadFile else "$envUrl/${item.uploadFile.trimStart('/')}"
                    val urlObj = java.net.URL(downloadUrl)
                    val conn = urlObj.openConnection() as java.net.HttpURLConnection
                    // Add cookie only if not external S3
                    if (!downloadUrl.contains("amazonaws.com") && !downloadUrl.contains("aliyun")) {
                         if (cookies != null) conn.setRequestProperty("Cookie", cookies)
                    }
                    
                    val tempZip = File.createTempFile("pkg_download", ".zip")
                    tempZip.deleteOnExit()
                    BufferedInputStream(conn.inputStream).use { input ->
                        FileOutputStream(tempZip).use { output ->
                            input.copyTo(output)
                        }
                    }
                    
                    // 5. Unzip and Organize
                    indicator.text = "解压并整理文件..."
                    val tempUnzipDir = File(targetModuleDir, ".temp_unzip_${System.currentTimeMillis()}")
                    tempUnzipDir.mkdirs()
                    
                    ZipInputStream(java.io.FileInputStream(tempZip)).use { zis ->
                       var entry = zis.nextEntry
                       while (entry != null) {
                           if (!entry.name.contains("..")) {
                               val newFile = File(tempUnzipDir, entry.name)
                               if (entry.isDirectory) {
                                   newFile.mkdirs()
                               } else {
                                   newFile.parentFile.mkdirs()
                                   FileOutputStream(newFile).use { it.write(zis.readBytes()) }
                               }
                           }
                           zis.closeEntry()
                           entry = zis.nextEntry
                       }
                    }
                    
                    // 6. Move files to correct package
                    // Logic: Detect if zip has 'src', or 'other/...' package structure, or is flat.
                    
                    val srcInZip = File(tempUnzipDir, "src")
                    
                    // Check if the zip root contains the first segment of the package (e.g. "other")
                    val firstPackageSegment = if (parts.isNotEmpty()) parts[0] else ""
                    val hasPackageStructure = if (firstPackageSegment.isNotEmpty()) {
                        File(tempUnzipDir, firstPackageSegment).exists() && File(tempUnzipDir, firstPackageSegment).isDirectory
                    } else false

                    if (srcInZip.exists() && srcInZip.isDirectory) {
                         // Case 1: Zip already has 'src' structure (e.g. src/main/java/...)
                         // Copy 'src' content to module root's 'src'
                         srcInZip.copyRecursively(File(targetModuleDir, "src"), overwrite = true)
                    } else if (hasPackageStructure) {
                         // Case 2: Zip contains package structure (e.g. other/oa/demss/...)
                         // Copy content to 'src/main/java' so it merges
                         tempUnzipDir.listFiles()?.forEach { file ->
                             if (file.isDirectory) {
                                 file.copyRecursively(File(javaSrcDir, file.name), overwrite = true)
                             } else {
                                 // Root files in zip (if any) go to java root? Or resource?
                                 // Usually unlikely for legal java package zips to have root files unless readme.
                                 // Let's copy to javaSrcDir
                                 file.copyTo(File(javaSrcDir, file.name), overwrite = true)
                             }
                         }
                    } else {
                         // Case 3: Flat structure (just files) or unknown folder
                         // Copy content to target package path: src/main/java/other/oa/demss/
                         val targetPackageDir = File(javaSrcDir, packagePath)
                         if (!targetPackageDir.exists()) targetPackageDir.mkdirs()
                         
                         tempUnzipDir.listFiles()?.forEach { file ->
                             if (file.isDirectory) {
                                 file.copyRecursively(File(targetPackageDir, file.name), overwrite = true)
                             } else {
                                 file.copyTo(File(targetPackageDir, file.name), overwrite = true)
                             }
                         }
                    }
                    // Cleanup
                    tempUnzipDir.deleteRecursively()
                    
                    SwingUtilities.invokeLater {
                        try {
                            // Auto-add as Maven Project
                            val pomFile = File(targetModuleDir, "pom.xml")
                            val pomVirtualFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(pomFile)
                            
                            if (pomVirtualFile != null && project != null) {
                                val mavenManager = MavenProjectsManager.getInstance(project)
                                if (!mavenManager.isManagedFile(pomVirtualFile)) {
                                    mavenManager.addManagedFiles(listOf(pomVirtualFile))
                                }
                            }
                        } catch (e: Exception) {
                            // Ignore if Maven plugin is not available or other issues
                            e.printStackTrace()
                        }
                        
                        JOptionPane.showMessageDialog(rootPane, "Maven 模块创建成功！\n位置: ${targetModuleDir.path}", "成功", JOptionPane.INFORMATION_MESSAGE)
                        project?.baseDir?.refresh(false, true)
                    }
                    
                } catch (e: Exception) {
                    SwingUtilities.invokeLater {
                        JOptionPane.showMessageDialog(rootPane, "处理失败: ${e.message}", "错误", JOptionPane.ERROR_MESSAGE)
                    }
                    e.printStackTrace()
                }
            }
        })
    }

    private fun handleUpload(row: Int) {
        if (row < 0 || row >= packageList.size) return
        val item = packageList[row]
        val projectBasePath = project?.basePath ?: return
        
        // Determine ArtifactId
        val parts = item.pkgName.split(".")
        val artifactId = if (parts.size >= 3) parts[2] else item.pkgName.replace(".", "_")
        
        var moduleDir = File(projectBasePath, artifactId)
        var srcDir = File(moduleDir, "src/main/java")
        
        if (!srcDir.exists()) {
            val choice = JOptionPane.showConfirmDialog(
                rootPane, 
                "在项目根目录下未找到模块 '$artifactId'。\n是否手动选择模块根目录？", 
                "查找模块", 
                JOptionPane.YES_NO_OPTION
            )
            
            if (choice == JOptionPane.YES_OPTION) {
                 val descriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor()
                 descriptor.title = "选择模块根目录"
                 val selectedDir = FileChooser.chooseFile(descriptor, project, null)
                 if (selectedDir != null) {
                     moduleDir = File(selectedDir.path)
                     srcDir = File(moduleDir, "src/main/java")
                 } else {
                     return
                 }
            } else {
                return
            }
        }
        
        if (!srcDir.exists()) {
            JOptionPane.showMessageDialog(rootPane, "在选定目录下找不到源码目录 (src/main/java)。", "错误", JOptionPane.ERROR_MESSAGE)
            return
        }
        
        val confirm = JOptionPane.showConfirmDialog(rootPane, "确认打包并上传模块 '$artifactId' (${item.displayName}) 到云端？\n这将自动创建新版本并触发AI审核。", "确认上传", JOptionPane.YES_NO_OPTION)
        if (confirm != JOptionPane.YES_OPTION) return
        
        // Show progress
        val progressDialog = JOptionPane("正在上传并处理...", JOptionPane.INFORMATION_MESSAGE, JOptionPane.DEFAULT_OPTION, null, arrayOf(), null).createDialog("处理中")
        progressDialog.isModal = false
        progressDialog.defaultCloseOperation = JDialog.DO_NOTHING_ON_CLOSE
        progressDialog.isVisible = true
        
        ApplicationManager.getApplication().executeOnPooledThread {
            try {
                // 1. Zip src/main/java content
                val zipFile = File.createTempFile("upload_${artifactId}_", ".zip")
                zipDirectory(srcDir, zipFile)
                println("Created temp zip: ${zipFile.absolutePath}")
                
                // Step A: Get List
                val listEndpoint = "${ApiConfig.Deployments.LIST}?project_id=$projectId"
                val listResp = HttpClient.get<Array<Any>>(listEndpoint, requireAuth = true, responseType = Array<Any>::class.java)
                
                var deploymentId: String? = null
                
                if (listResp.success && listResp.data != null) {
                    val dataArray = listResp.data
                    
                    val existing = dataArray.mapNotNull { it as? Map<*, *> }.find {
                        (it["package_path"] as? String) == item.pkgName ||
                        (it["title"] as? String) == item.displayName // Fallback match
                    }
                    deploymentId = existing?.get("id") as? String
                }
                
                // Step B: Create if not exists
                if (deploymentId == null) {
                    val createParams = mapOf(
                        "title" to item.displayName,
                        "project_id" to projectId,
                        "deployment_type" to "logic_code", // Fixed type
                        "description" to "Auto created by IDEA Plugin",
                        "environment_id" to envId,
                        "package_path" to item.pkgName
                    )
                    
                    // User spec says "Form Data", so usage of JSON post might be rejected. Use postForm.
                    val createResp = HttpClient.postForm(ApiConfig.Deployments.CREATE, createParams, requireAuth = true, responseType = Map::class.java)
                    if (!createResp.success || createResp.data == null) {
                         throw Exception("创建发布包失败: ${createResp.message}")
                    }
                    @Suppress("UNCHECKED_CAST")
                    val newData = createResp.data as Map<String, Any>
                    deploymentId = newData["id"] as String
                }
                
                // 3. Upload Version
                val uploadEndpoint = ApiConfig.Deployments.UPLOAD_VERSION.replace("{deployment_id}", deploymentId)
                val uploadResp = HttpClient.uploadFile(
                    endpoint = uploadEndpoint,
                    file = zipFile,
                    params = mapOf("description" to "Uploaded from IntelliJ Plugin"),
                    requireAuth = true,
                    responseType = Map::class.java
                )
                
                if (!uploadResp.success || uploadResp.data == null) {
                    throw Exception("上传文件失败: ${uploadResp.message}")
                }
                
                @Suppress("UNCHECKED_CAST")
                val versionData = uploadResp.data as Map<String, Any>
                val version = versionData["version"] as? String ?: "Unknown"
                
                // 4. Cleanup and Notify
                zipFile.delete()
                progressDialog.isVisible = false
                
                SwingUtilities.invokeLater {
                    JOptionPane.showMessageDialog(rootPane, "上传成功！\n版本: $version\nAI分析已在后台启动。", "成功", JOptionPane.INFORMATION_MESSAGE)
                    fetchData()
                }
                
            } catch (e: Exception) {
                progressDialog.isVisible = false
                SwingUtilities.invokeLater {
                    JOptionPane.showMessageDialog(rootPane, "操作失败: ${e.message}", "错误", JOptionPane.ERROR_MESSAGE)
                }
                e.printStackTrace()
            }
        }
    }
    
    private fun zipDirectory(sourceDir: File, zipFile: File) {
        java.util.zip.ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
            sourceDir.walkTopDown().forEach { file ->
                if (file.isFile) {
                    val relativePath = file.relativeTo(sourceDir).path.replace("\\", "/")
                    if (!relativePath.startsWith(".") && !relativePath.contains(".DS_Store")) {
                        val entry = ZipEntry(relativePath)
                        zos.putNextEntry(entry)
                        java.io.FileInputStream(file).use { fis ->
                            fis.copyTo(zos)
                        }
                        zos.closeEntry()
                    }
                }
            }
        }
    }
    
    private fun copyOAuthConfig(projectBasePath: String, targetResourcesDir: File) {
        if (projectBasePath.isEmpty()) return
        
        try {
            // Look for oauthConfig.properties in the project root
            val rootConfig = File(projectBasePath, "oauthConfig.properties")
            
            if (rootConfig.exists()) {
                val targetFile = File(targetResourcesDir, "oauthConfig.properties")
                rootConfig.copyTo(targetFile, overwrite = true)
                println("Copied oauthConfig.properties to ${targetFile.path}")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private data class PackageItem(
        val displayName: String,
        val pkgName: String, 
        val version: String,
        val time: String,
        val uploader: String,
        val uploadFile: String
    )
    
    // Components
    private inner class ActionButtonRenderer : TableCellRenderer {
        override fun getTableCellRendererComponent(
            table: JTable?, value: Any?, isSelected: Boolean, hasFocus: Boolean, row: Int, column: Int
        ): Component {
            val panel = JPanel(FlowLayout(FlowLayout.CENTER, 5, 0))
            panel.isOpaque = true
            panel.background = if (isSelected) table?.selectionBackground else table?.background

            val uploadBtn = JButton("上传")
            val downloadBtn = JButton("下载")
            
            panel.add(downloadBtn)
            panel.add(uploadBtn)
            return panel
        }
    }

    // Custom Editor for Buttons
    private inner class ActionButtonEditor : AbstractCellEditor(), TableCellEditor {
        private val panel = JPanel(FlowLayout(FlowLayout.CENTER, 5, 0))
        private val uploadBtn = JButton("上传")
        private val downloadBtn = JButton("下载")
        private var currentRow = -1

        init {
            panel.add(downloadBtn)
            panel.add(uploadBtn)

            uploadBtn.addActionListener {
                stopCellEditing()
                handleUpload(currentRow)
            }

            downloadBtn.addActionListener {
                stopCellEditing()
                handleDownload(currentRow)
            }
        }

        override fun getTableCellEditorComponent(
            table: JTable?, value: Any?, isSelected: Boolean, row: Int, column: Int
        ): Component {
            panel.background = if (isSelected) table?.selectionBackground else table?.background
            currentRow = row
            return panel
        }

        override fun getCellEditorValue(): Any = ""
    }
}
