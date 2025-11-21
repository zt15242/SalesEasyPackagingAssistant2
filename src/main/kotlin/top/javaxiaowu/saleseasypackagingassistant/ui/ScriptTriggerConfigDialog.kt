package top.javaxiaowu.saleseasypackagingassistant.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.components.JBScrollPane
import javax.swing.*
import java.awt.Component

/**
 * 配置类型
 */
enum class ConfigType(val displayName: String, val xmlTagName: String) {
    APPROVAL_EVENT("审批流配置", "approvalevent"),
    WORKFLOW_EVENT("工作流配置", "approvalevent"),
    STAGE_PROCESS_EVENT("可视化流配置", "stageProcessEvent"),
    AUTO_FLOW_EVENT("自动流配置", "autoflowevent"),
    RULE_EVENT("触发规则配置", "ruleevent"),
    SCHEDULE_EVENT("定时调度配置", "ruleevent"),
    TRIGGER_EVENT("触发器配置", "trigger"),
    SCHEDULE_JOB("定时调度", "schedule")
}

/**
 * 操作类型
 */
enum class OperationType(val displayName: String, val value: String) {
    // Approval Event
    SUBMIT("提交审批", "submit"),
    AGREE("通过", "agree"),
    REJECT("拒绝", "reject"),
    USER_TURN("转办", "userTurn"),
    PLUS_SIGN("加签", "plussign"),
    WITHDRAW("撤回", "withdraw"),
    ARRIVE("到达后-通过", "arrive"),
    REJECT_ARRIVE("到达后-拒绝", "rejectArrive"),
    COMPLETE("完成审批流程且最终结果为通过", "complete"),
    RETURN("完成审批流程且最终结果为拒绝", "return"),
    
    // Trigger Event
    ADD("创建", "add"),
    DELETE("删除", "delete"),
    UPDATE("更新", "update"),
    TRANSFER("转移", "transfer"),
    LOCK("锁定", "lock"),
    UNLOCK("解锁", "unlock"),
    RECOVER("从数据回收站恢复数据", "recover"),
    
    // Stage Process Event
    ADVANCE("阶段推进", "advance"),
    REACTIVATE("阶段重新激活", "reactivate")
}

/**
 * 执行时机
 */
enum class ExecutionTiming(val displayName: String, val value: String) {
    VALIDATE("validate", "validate"),
    BEFORE("before", "before"),
    AFTER("after", "after")
}

data class ScriptTriggerConfig(
    val fileName: String,
    val configType: ConfigType,
    val objectName: String = "",
    val operations: List<String> = emptyList(),
    val timings: List<String> = emptyList(),
    val order: String = "",
    val className: String = ""
)

class ScriptTriggerConfigDialog(
    project: Project,
    private val fileName: String,
    private val virtualFile: VirtualFile? = null,
    private val projectBaseDir: VirtualFile? = null
) : DialogWrapper(project, true) {
    
    private var selectedConfigType: ConfigType? = null
    private var selectedOperations = mutableSetOf<String>()
    private var selectedTimings = mutableSetOf<String>()
    private var objectNameField: JTextField? = null
    private var orderField: JTextField? = null
    private var operationPanel: JPanel? = null
    private var orderPanel: JPanel? = null
    private var generatedClassName: String = ""
    
    var config: ScriptTriggerConfig? = null

    init {
        title = "配置 ScriptTrigger.xml - $fileName"
        // 自动计算类名
        generateClassName()
        init()
    }
    
    private fun generateClassName() {
        // 从文件的相对路径计算类名
        if (virtualFile != null && projectBaseDir != null) {
            try {
                val filePath = virtualFile.path
                val baseDir = projectBaseDir.path
                
                // 计算相对路径
                val relativePath = if (filePath.startsWith(baseDir)) {
                    filePath.substring(baseDir.length).removePrefix("/").removePrefix("\\")
                } else {
                    filePath
                }
                
                // 去掉文件后缀
                val classPath = relativePath.substringBeforeLast(".")
                // 将路径消死为符日常规录（正斋/），然后为.以构成常规类名
                generatedClassName = classPath.replace("\\", "/")
                    .replace("/", ".")
                    .removePrefix(".")
                    .removePrefix("src.main.java.")
                    .removePrefix("src.main.kotlin.")
                
                println("DEBUG: Generated className = $generatedClassName")
            } catch (e: Exception) {
                println("DEBUG: Error generating className: ${e.message}")
            }
        }
    }

    override fun createCenterPanel(): JComponent {
        val mainPanel = JPanel()
        mainPanel.layout = BoxLayout(mainPanel, BoxLayout.Y_AXIS)
        mainPanel.border = BorderFactory.createEmptyBorder(10, 10, 10, 10)
        mainPanel.preferredSize = java.awt.Dimension(500, 400)

        // 上部：配置类型选择
        val configTypePanel = JPanel()
        configTypePanel.layout = BoxLayout(configTypePanel, BoxLayout.X_AXIS)
        configTypePanel.border = BorderFactory.createTitledBorder("配置类型")
        configTypePanel.alignmentX = Component.LEFT_ALIGNMENT
        
        val configTypeGroup = ButtonGroup()
        val configTypeOptions = getApplicableConfigTypes()
        
        for (configType in configTypeOptions) {
            val radioButton = JRadioButton(configType.displayName)
            radioButton.addActionListener {
                selectedConfigType = configType
                selectedOperations.clear()
                selectedTimings.clear()
                updateUIForConfigType(configType)
            }
            configTypeGroup.add(radioButton)
            configTypePanel.add(radioButton)
        }
        
        if (configTypeOptions.isNotEmpty()) {
            val firstButton = configTypePanel.components.find { it is JRadioButton } as? JRadioButton
            firstButton?.isSelected = true
            selectedConfigType = configTypeOptions[0]
        }
        
        configTypePanel.add(Box.createHorizontalGlue())
        mainPanel.add(configTypePanel)
        mainPanel.add(Box.createVerticalStrut(12))

        // 中部：对象名称、操作选项、执行时机（使用标签页或组合框）
        // 对象名称
        val objectNamePanel = JPanel()
        objectNamePanel.layout = BoxLayout(objectNamePanel, BoxLayout.X_AXIS)
        objectNamePanel.alignmentX = Component.LEFT_ALIGNMENT
        val objectLabel = JLabel("对象名称:")
        objectLabel.preferredSize = java.awt.Dimension(80, 25)
        objectNamePanel.add(objectLabel)
        objectNameField = JTextField()
        objectNameField!!.preferredSize = java.awt.Dimension(150, 25)
        objectNameField!!.maximumSize = java.awt.Dimension(150, 25)
        objectNamePanel.add(objectNameField)
        objectNamePanel.add(Box.createHorizontalGlue())
        mainPanel.add(objectNamePanel)
        mainPanel.add(Box.createVerticalStrut(8))

        // 操作选项
        val operationLabelPanel = JPanel()
        operationLabelPanel.layout = BoxLayout(operationLabelPanel, BoxLayout.X_AXIS)
        operationLabelPanel.alignmentX = Component.LEFT_ALIGNMENT
        operationLabelPanel.add(JLabel("操作选项:"))
        mainPanel.add(operationLabelPanel)
        
        operationPanel = JPanel()
        operationPanel!!.layout = BoxLayout(operationPanel!!, BoxLayout.Y_AXIS)
        operationPanel!!.alignmentX = Component.LEFT_ALIGNMENT
        operationPanel!!.border = BorderFactory.createEmptyBorder(5, 20, 5, 5)
        updateOperationPanel()
        
        val operationScroll = JBScrollPane(operationPanel)
        operationScroll.preferredSize = java.awt.Dimension(450, 80)
        operationScroll.alignmentX = Component.LEFT_ALIGNMENT
        operationScroll.isVisible = shouldShowOperations()
        mainPanel.add(operationScroll)
        mainPanel.add(Box.createVerticalStrut(8))

        // 执行时机
        val timingLabelPanel = JPanel()
        timingLabelPanel.layout = BoxLayout(timingLabelPanel, BoxLayout.X_AXIS)
        timingLabelPanel.alignmentX = Component.LEFT_ALIGNMENT
        timingLabelPanel.add(JLabel("执行时机:"))
        mainPanel.add(timingLabelPanel)
        
        val timingPanel = JPanel()
        timingPanel.layout = BoxLayout(timingPanel, BoxLayout.Y_AXIS)
        timingPanel.alignmentX = Component.LEFT_ALIGNMENT
        timingPanel.border = BorderFactory.createEmptyBorder(5, 20, 5, 5)
        
        for (timing in ExecutionTiming.values()) {
            val checkBox = JCheckBox(timing.displayName)
            checkBox.alignmentX = Component.LEFT_ALIGNMENT
            checkBox.addActionListener {
                if (checkBox.isSelected) {
                    selectedTimings.add(timing.value)
                } else {
                    selectedTimings.remove(timing.value)
                }
            }
            timingPanel.add(checkBox)
        }
        
        val timingScroll = JBScrollPane(timingPanel)
        timingScroll.preferredSize = java.awt.Dimension(450, 60)
        timingScroll.alignmentX = Component.LEFT_ALIGNMENT
        timingScroll.isVisible = shouldShowTimings()
        mainPanel.add(timingScroll)
        mainPanel.add(Box.createVerticalStrut(8))

        // 顺序（仅for Trigger）
        orderPanel = JPanel()
        orderPanel!!.layout = BoxLayout(orderPanel!!, BoxLayout.X_AXIS)
        orderPanel!!.alignmentX = Component.LEFT_ALIGNMENT
        val orderLabel = JLabel("执行顺序:")
        orderLabel.preferredSize = java.awt.Dimension(80, 25)
        orderPanel!!.add(orderLabel)
        orderField = JTextField()
        orderPanel!!.add(orderField)
        orderPanel!!.isVisible = selectedConfigType == ConfigType.TRIGGER_EVENT
        mainPanel.add(orderPanel!!)

        return mainPanel
    }
    
    private fun updateOperationPanel() {
        operationPanel?.removeAll()
        selectedOperations.clear()
        
        val operationOptions = getApplicableOperations()
        for (op in operationOptions) {
            val checkBox = JCheckBox(op.displayName)
            checkBox.addActionListener {
                if (checkBox.isSelected) {
                    selectedOperations.add(op.value)
                } else {
                    selectedOperations.remove(op.value)
                }
            }
            operationPanel?.add(checkBox)
        }
        
        operationPanel?.revalidate()
        operationPanel?.repaint()
    }

    private fun getApplicableConfigTypes(): List<ConfigType> {
        return when {
            fileName.contains("Event") && !fileName.contains("EventImpl") -> {
                listOf(
                    ConfigType.APPROVAL_EVENT,
                    ConfigType.WORKFLOW_EVENT,
                    ConfigType.STAGE_PROCESS_EVENT
                )
            }
            fileName.contains("EventImpl") -> {
                listOf(
                    ConfigType.AUTO_FLOW_EVENT,
                    ConfigType.RULE_EVENT,
                    ConfigType.SCHEDULE_EVENT
                )
            }
            fileName.contains("Trigger") || fileName.contains("trigger") -> {
                listOf(ConfigType.TRIGGER_EVENT)
            }
            fileName.contains("ScheduleJob") -> {
                listOf(ConfigType.SCHEDULE_JOB)
            }
            else -> emptyList()
        }
    }

    private fun getApplicableOperations(): List<OperationType> {
        return when (selectedConfigType) {
            ConfigType.TRIGGER_EVENT -> {
                listOf(
                    OperationType.ADD,
                    OperationType.DELETE,
                    OperationType.UPDATE,
                    OperationType.TRANSFER,
                    OperationType.LOCK,
                    OperationType.UNLOCK,
                    OperationType.RECOVER
                )
            }
            ConfigType.STAGE_PROCESS_EVENT -> {
                listOf(
                    OperationType.ARRIVE,
                    OperationType.ADVANCE,
                    OperationType.REACTIVATE
                )
            }
            ConfigType.AUTO_FLOW_EVENT, ConfigType.RULE_EVENT, ConfigType.SCHEDULE_EVENT, ConfigType.SCHEDULE_JOB -> {
                // 这些类型不需要操作选项
                emptyList()
            }
            else -> {
                listOf(
                    OperationType.SUBMIT,
                    OperationType.AGREE,
                    OperationType.REJECT,
                    OperationType.USER_TURN,
                    OperationType.PLUS_SIGN,
                    OperationType.WITHDRAW,
                    OperationType.ARRIVE,
                    OperationType.REJECT_ARRIVE,
                    OperationType.COMPLETE,
                    OperationType.RETURN
                )
            }
        }
    }

    private fun updateUIForConfigType(configType: ConfigType) {
        updateOperationPanel()
        orderPanel?.isVisible = configType == ConfigType.TRIGGER_EVENT
    }
    
    private fun shouldShowOperations(): Boolean {
        return when (selectedConfigType) {
            ConfigType.AUTO_FLOW_EVENT, ConfigType.RULE_EVENT, ConfigType.SCHEDULE_EVENT, ConfigType.SCHEDULE_JOB -> false
            else -> true
        }
    }
    
    private fun shouldShowTimings(): Boolean {
        return when (selectedConfigType) {
            ConfigType.AUTO_FLOW_EVENT, ConfigType.RULE_EVENT, ConfigType.SCHEDULE_EVENT, ConfigType.SCHEDULE_JOB -> false
            else -> true
        }
    }

    override fun doOKAction() {
        // 使用自动生成的类名
        if (generatedClassName.isEmpty()) {
            com.intellij.openapi.ui.Messages.showErrorDialog("无法生成类名", "错误")
            return
        }
        
        // ScheduleJob 类型不需要对象名称
        val objectName = if (selectedConfigType == ConfigType.SCHEDULE_JOB) {
            ""
        } else {
            objectNameField?.text?.trim() ?: ""
        }
        
        if (objectName.isEmpty() && selectedConfigType != ConfigType.SCHEDULE_JOB) {
            com.intellij.openapi.ui.Messages.showErrorDialog("请输入对象名称", "错误")
            return
        }

        val order = orderField?.text?.trim() ?: ""
        
        config = ScriptTriggerConfig(
            fileName = fileName,
            configType = selectedConfigType ?: ConfigType.APPROVAL_EVENT,
            objectName = objectName,
            operations = selectedOperations.toList(),
            timings = selectedTimings.toList(),
            order = order,
            className = generatedClassName
        )
        
        super.doOKAction()
    }
}