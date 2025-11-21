package top.javaxiaowu.saleseasypackagingassistant.utils

import org.junit.Test
import org.junit.Assert.*
import top.javaxiaowu.saleseasypackagingassistant.ui.ConfigType
import top.javaxiaowu.saleseasypackagingassistant.ui.ScriptTriggerConfig

class ScriptTriggerXmlGeneratorTest {
    
    @Test
    fun testShouldCreateScriptTrigger() {
        // Test files containing "Event"
        assertTrue(ScriptTriggerXmlGenerator.shouldCreateScriptTrigger("UserApprovalEvent.java"))
        assertTrue(ScriptTriggerXmlGenerator.shouldCreateScriptTrigger("OrderEvent.kt"))
        
        // Test files containing "EventImpl"
        assertTrue(ScriptTriggerXmlGenerator.shouldCreateScriptTrigger("UserApprovalEventImpl.java"))
        
        // Test files containing "Trigger"
        assertTrue(ScriptTriggerXmlGenerator.shouldCreateScriptTrigger("UserTrigger.java"))
        assertTrue(ScriptTriggerXmlGenerator.shouldCreateScriptTrigger("OrderTrigger.kt"))
        
        // Test files containing "trigger"
        assertTrue(ScriptTriggerXmlGenerator.shouldCreateScriptTrigger("user_trigger.java"))
        
        // Test files containing "ScheduleJob"
        assertTrue(ScriptTriggerXmlGenerator.shouldCreateScriptTrigger("DailyScheduleJob.java"))
        
        // Test files that don't match
        assertFalse(ScriptTriggerXmlGenerator.shouldCreateScriptTrigger("UserService.java"))
    }
    
    @Test
    fun testGenerateApprovalEventXml() {
        val config = ScriptTriggerConfig(
            fileName = "UserApprovalEvent.java",
            configType = ConfigType.APPROVAL_EVENT,
            objectName = "account",
            operations = listOf("submit", "agree"),
            timings = listOf("before", "after"),
            className = "com.example.UserApprovalEvent"
        )
        
        val xml = ScriptTriggerXmlGenerator.generateScriptTriggerXml(listOf(config))
        
        assertTrue(xml.contains("<configs>"))
        // 应有 2 * 2 = 4 个配置（两个操作乘以两个时机）
        val approvaleventCount = xml.split("<approvalevent>").size - 1
        assertEquals(4, approvaleventCount)
        
        // 检查是否有各个操作和时机的组合
        assertTrue(xml.contains("<operate>submit</operate>"))
        assertTrue(xml.contains("<operate>agree</operate>"))
        assertTrue(xml.contains("<position>before</position>"))
        assertTrue(xml.contains("<position>after</position>"))
    }
    
    @Test
    fun testGenerateTriggerEventXml() {
        val config = ScriptTriggerConfig(
            fileName = "UserTrigger.java",
            configType = ConfigType.TRIGGER_EVENT,
            objectName = "account",
            operations = listOf("add", "update"),
            timings = listOf("before", "after"),
            order = "1",
            className = "com.example.UserTrigger"
        )
        
        val xml = ScriptTriggerXmlGenerator.generateScriptTriggerXml(listOf(config))
        
        // 应有 2 * 2 = 4 个配置
        val triggerCount = xml.split("<trigger>").size - 1
        assertEquals(4, triggerCount)
        
        // 检查是否有各个操作和时机的组合
        assertTrue(xml.contains("<operate>add</operate>"))
        assertTrue(xml.contains("<operate>update</operate>"))
        assertTrue(xml.contains("<position>before</position>"))
        assertTrue(xml.contains("<position>after</position>"))
        // 每条配置中都应有顺序
        assertTrue(xml.contains("<order>1</order>"))
    }
    
    @Test
    fun testGenerateScheduleJobXml() {
        val config = ScriptTriggerConfig(
            fileName = "DailyScheduleJob.java",
            configType = ConfigType.SCHEDULE_JOB,
            className = "com.example.DailyScheduleJob"
        )
        
        val xml = ScriptTriggerXmlGenerator.generateScriptTriggerXml(listOf(config))
        
        assertTrue(xml.contains("<schedule>"))
        assertTrue(xml.contains("<class>com.example.DailyScheduleJob</class>"))
        assertTrue(xml.contains("</schedule>"))
        // ScheduleJob should not have object element
        assertFalse(xml.contains("<object>"))
    }
    
    @Test
    fun testGenerateMultipleConfigs() {
        val config1 = ScriptTriggerConfig(
            fileName = "UserApprovalEvent.java",
            configType = ConfigType.APPROVAL_EVENT,
            objectName = "account",
            operations = listOf("submit"),
            timings = listOf("before"),
            className = "com.example.UserApprovalEvent"
        )
        
        val config2 = ScriptTriggerConfig(
            fileName = "UserTrigger.java",
            configType = ConfigType.TRIGGER_EVENT,
            objectName = "account",
            operations = listOf("add"),
            timings = listOf("after"),
            order = "1",
            className = "com.example.UserTrigger"
        )
        
        val xml = ScriptTriggerXmlGenerator.generateScriptTriggerXml(listOf(config1, config2))
        
        // config1 有 1 * 1 = 1 个配置，config2 有 1 * 1 = 1 个配置，總共 2 个
        val configCount = xml.split("<config>").size - 1
        assertEquals(2, configCount)
        
        assertTrue(xml.contains("<approvalevent>"))
        assertTrue(xml.contains("<trigger>"))
    }
}
