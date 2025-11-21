package top.javaxiaowu.saleseasypackagingassistant.utils

import org.junit.Before
import org.junit.After
import org.junit.Test
import org.junit.Assert.*
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths

class ExportVersionManagerTest {
    
    private lateinit var testProjectRoot: File
    
    @Before
    fun setUp() {
        // 创建临时项目根目录
        testProjectRoot = Files.createTempDirectory("sales_easy_test_").toFile()
        println("Test project root: ${testProjectRoot.absolutePath}")
    }
    
    @After
    fun tearDown() {
        // 清理临时目录
        testProjectRoot.deleteRecursively()
    }
    
    @Test
    fun testExtractThreeLevelPath() {
        // 测试3级路径提取
        val manager = ExportVersionManager
        
        // 使用反射来测试私有方法
        val method = manager.javaClass.getDeclaredMethod("extractThreeLevelPath", String::class.java)
        method.isAccessible = true
        
        val result1 = method.invoke(manager, "other.rainbow.inventoryview.controller.InventoryViewController") as String
        assertEquals("other.rainbow.inventoryview", result1)
        
        val result2 = method.invoke(manager, "other.rainbow") as String
        assertEquals("other.rainbow", result2)
        
        val result3 = method.invoke(manager, "com.example.demo.service.UserService") as String
        assertEquals("com.example.demo", result3)
    }
    
    @Test
    fun testVersionIncrement() {
        // 测试版本号递增逻辑
        val manager = ExportVersionManager
        
        // 使用反射来测试私有方法
        val method = manager.javaClass.getDeclaredMethod("incrementVersion", String::class.java)
        method.isAccessible = true
        
        val result1 = method.invoke(manager, "V1.0") as String
        assertEquals("V2.0", result1)
        
        val result2 = method.invoke(manager, "V2.0") as String
        assertEquals("V3.0", result2)
        
        val result3 = method.invoke(manager, "V9.5") as String
        assertEquals("V10.0", result3)
    }
    
    @Test
    fun testVersionCompare() {
        // 测试版本号比较逻辑
        val manager = ExportVersionManager
        
        // 使用反射来测试私有方法
        val method = manager.javaClass.getDeclaredMethod("compareVersions", String::class.java, String::class.java)
        method.isAccessible = true
        
        val result1 = method.invoke(manager, "V2.0", "V1.0") as Int
        assertTrue(result1 > 0)
        
        val result2 = method.invoke(manager, "V1.5", "V2.0") as Int
        assertTrue(result2 < 0)
        
        val result3 = method.invoke(manager, "V1.0", "V1.0") as Int
        assertEquals(0, result3)
    }
    
    @Test
    fun testGetVersionForPath() {
        // 测试获取版本号（初始时应为V1.0）
        val version = ExportVersionManager.getVersionForPath(testProjectRoot.absolutePath, "other.rainbow.inventoryview.controller.InventoryViewController")
        assertEquals("V1.0", version)
    }
    
    @Test
    fun testIncrementVersionForPath() {
        // 第一次导出应该是V1.0
        val version1 = ExportVersionManager.incrementVersionForPath(testProjectRoot.absolutePath, "other.rainbow.inventoryview.controller.InventoryViewController")
        assertEquals("V1.0", version1)
        
        // 第二次导出应该是V2.0
        val version2 = ExportVersionManager.incrementVersionForPath(testProjectRoot.absolutePath, "other.rainbow.inventoryview.controller.InventoryViewController")
        assertEquals("V2.0", version2)
        
        // 第三次导出同一个 3 级路径应该是 V3.0
        val version3 = ExportVersionManager.incrementVersionForPath(testProjectRoot.absolutePath, "other.rainbow.inventoryview.service.InventoryService")
        assertEquals("V3.0", version3)
    }
    
    @Test
    fun testIncrementVersionsForExport() {
        // 测试为多个文件递增版本号
        val paths = listOf(
            "other.rainbow.inventoryview.controller.InventoryViewController",
            "other.rainbow.inventoryview.service.InventoryService",
            "com.example.demo.utils.Helper"
        )
        
        val version1 = ExportVersionManager.incrementVersionsForExport(testProjectRoot.absolutePath, paths)
        // 第一次导出：
        // other.rainbow.inventoryview (path1和path2合并为同一3级路径) -> V1.0, V2.0
        // com.example.demo (path3) -> V1.0
        // 最新版本是 V2.0
        assertEquals("V2.0", version1)
        
        // 第二次导出相同路径
        val version2 = ExportVersionManager.incrementVersionsForExport(testProjectRoot.absolutePath, paths)
        // other.rainbow.inventoryview -> V3.0, V4.0
        // com.example.demo -> V2.0
        // 最新版本是 V4.0
        assertEquals("V4.0", version2)
    }
    
    @Test
    fun testVersionJsonPersistence() {
        // 测试版本号持久化到JSON
        ExportVersionManager.incrementVersionForPath(testProjectRoot.absolutePath, "other.rainbow.inventoryview.controller.InventoryViewController")
        ExportVersionManager.incrementVersionForPath(testProjectRoot.absolutePath, "other.rainbow.inventoryview.controller.InventoryViewController")
        ExportVersionManager.incrementVersionForPath(testProjectRoot.absolutePath, "com.example.demo.utils.Helper")
        
        // 检查JSON文件是否存在
        val versionFile = File(testProjectRoot, ".SalesEasy/versions.json")
        assertTrue("Version file should exist", versionFile.exists())
        
        // 检查JSON内容
        val jsonContent = versionFile.readText()
        assertTrue("JSON should contain other.rainbow.inventoryview", jsonContent.contains("other.rainbow.inventoryview"))
        assertTrue("JSON should contain com.example.demo", jsonContent.contains("com.example.demo"))
        
        println("Generated JSON content:")
        println(jsonContent)
    }
    
    @Test
    fun testGetAllVersionRecords() {
        // 测试获取所有版本记录
        ExportVersionManager.incrementVersionForPath(testProjectRoot.absolutePath, "other.rainbow.inventoryview.controller.InventoryViewController")
        ExportVersionManager.incrementVersionForPath(testProjectRoot.absolutePath, "other.rainbow.inventoryview.controller.InventoryViewController")
        ExportVersionManager.incrementVersionForPath(testProjectRoot.absolutePath, "com.example.demo.utils.Helper")
        
        val records = ExportVersionManager.getAllVersionRecords(testProjectRoot.absolutePath)
        assertEquals(2, records.size)
        
        assertTrue(records.containsKey("other.rainbow.inventoryview"))
        assertTrue(records.containsKey("com.example.demo"))
        
        assertEquals("V2.0", records["other.rainbow.inventoryview"]?.version)
        assertEquals("V1.0", records["com.example.demo"]?.version)
    }
}
