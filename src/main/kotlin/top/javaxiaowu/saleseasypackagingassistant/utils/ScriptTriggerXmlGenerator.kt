package top.javaxiaowu.saleseasypackagingassistant.utils

import top.javaxiaowu.saleseasypackagingassistant.ui.ConfigType
import top.javaxiaowu.saleseasypackagingassistant.ui.ScriptTriggerConfig

object ScriptTriggerXmlGenerator {
    
    /**
     * 检查文件名是否需要创建 scriptTrigger.xml
     */
    fun shouldCreateScriptTrigger(fileName: String): Boolean {
        return fileName.contains("Event") ||
               fileName.contains("Trigger") ||
               fileName.contains("trigger") ||
               fileName.contains("ScheduleJob")
    }

    /**
     * 生成 scriptTrigger.xml 内容
     */
    fun generateScriptTriggerXml(configs: List<ScriptTriggerConfig>): String {
        val sb = StringBuilder()
        sb.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n\n")
        sb.append("<configs>\n")
        sb.append("  <config>\n")
        
        // 为每个配置及其操作和时机的组合生成配置块
        for (config in configs) {
            // 如果有多个操作或多个时机，为每个组合生成一条配置
            if (config.operations.isNotEmpty() && config.timings.isNotEmpty()) {
                // 为每个操作和时机的组合生成一条配置
                for (operation in config.operations) {
                    for (timing in config.timings) {
                        sb.append(generateConfigContent(config.copy(
                            operations = listOf(operation),
                            timings = listOf(timing)
                        )))
                    }
                }
            } else if (config.operations.isNotEmpty()) {
                // 只有操作，为每个操作生成一条配置
                for (operation in config.operations) {
                    sb.append(generateConfigContent(config.copy(
                        operations = listOf(operation)
                    )))
                }
            } else if (config.timings.isNotEmpty()) {
                // 只有时机，为每个时机生成一条配置
                for (timing in config.timings) {
                    sb.append(generateConfigContent(config.copy(
                        timings = listOf(timing)
                    )))
                }
            } else {
                // 没有操作和时机，直接生成
                sb.append(generateConfigContent(config))
            }
        }
        
        sb.append("  </config>\n")
        sb.append("</configs>\n")
        return sb.toString()
    }

    private fun generateConfigContent(config: ScriptTriggerConfig): String {
        val sb = StringBuilder()
        
        when (config.configType) {
            ConfigType.APPROVAL_EVENT -> {
                sb.append("    <approvalevent>\n")
                sb.append("      <object>${config.objectName}</object>\n")
                
                // 每个操作一条
                if (config.operations.isNotEmpty()) {
                    sb.append("      <operate>${config.operations[0]}</operate>\n")
                }
                
                // 每个时机一条
                if (config.timings.isNotEmpty()) {
                    sb.append("      <position>${config.timings[0]}</position>\n")
                }
                
                sb.append("      <class>${config.className}</class>\n")
                sb.append("    </approvalevent>\n")
            }
            
            ConfigType.WORKFLOW_EVENT -> {
                sb.append("    <approvalevent>\n")
                sb.append("      <object>${config.objectName}</object>\n")
                
                if (config.operations.isNotEmpty()) {
                    sb.append("      <operate>${config.operations[0]}</operate>\n")
                }
                
                if (config.timings.isNotEmpty()) {
                    sb.append("      <position>${config.timings[0]}</position>\n")
                }
                
                sb.append("      <class>${config.className}</class>\n")
                sb.append("    </approvalevent>\n")
            }
            
            ConfigType.STAGE_PROCESS_EVENT -> {
                sb.append("    <stageProcessEvent>\n")
                sb.append("      <object>${config.objectName}</object>\n")
                
                if (config.operations.isNotEmpty()) {
                    sb.append("      <operate>${config.operations[0]}</operate>\n")
                }
                
                if (config.timings.isNotEmpty()) {
                    sb.append("      <position>${config.timings[0]}</position>\n")
                }
                
                sb.append("      <class>${config.className}</class>\n")
                sb.append("    </stageProcessEvent>\n")
            }
            
            ConfigType.AUTO_FLOW_EVENT -> {
                sb.append("    <autoflowevent>\n")
                sb.append("      <object>${config.objectName}</object>\n")
                sb.append("      <class>${config.className}</class>\n")
                sb.append("    </autoflowevent>\n")
            }
            
            ConfigType.RULE_EVENT -> {
                sb.append("    <ruleevent>\n")
                sb.append("      <object>${config.objectName}</object>\n")
                sb.append("      <class>${config.className}</class>\n")
                sb.append("    </ruleevent>\n")
            }
            
            ConfigType.SCHEDULE_EVENT -> {
                sb.append("    <ruleevent>\n")
                sb.append("      <object>${config.objectName}</object>\n")
                sb.append("      <class>${config.className}</class>\n")
                sb.append("    </ruleevent>\n")
            }
            
            ConfigType.TRIGGER_EVENT -> {
                sb.append("    <trigger>\n")
                sb.append("      <object>${config.objectName}</object>\n")
                
                if (config.operations.isNotEmpty()) {
                    sb.append("      <operate>${config.operations[0]}</operate>\n")
                }
                
                if (config.timings.isNotEmpty()) {
                    sb.append("      <position>${config.timings[0]}</position>\n")
                }
                
                if (config.order.isNotEmpty()) {
                    sb.append("      <order>${config.order}</order>\n")
                }
                
                sb.append("      <class>${config.className}</class>\n")
                sb.append("    </trigger>\n")
            }
            
            ConfigType.SCHEDULE_JOB -> {
                sb.append("    <schedule>\n")
                sb.append("      <class>${config.className}</class>\n")
                sb.append("    </schedule>\n")
            }
        }
        
        return sb.toString()
    }
}