package top.javaxiaowu.saleseasypackagingassistant.ui

import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBScrollPane
import java.awt.*
import javax.swing.*
import com.google.gson.reflect.TypeToken
import top.javaxiaowu.saleseasypackagingassistant.utils.HttpClient.PaginatedResponse

/**
 * SalesEasy 工具窗口面板
 * 包含登录功能和通知功能的UI界面
 */
class SalesEasyToolWindowPanel(private val project: Project) {
    
    private val mainPanel = JBPanel<JBPanel<*>>()
    private var isLoggedIn = false
    private var username: String? = null
    
    // UI 组件
    private lateinit var loginPanel: JPanel
    private lateinit var userInfoPanel: JPanel
    private lateinit var notificationPanel: JPanel
    private lateinit var usernameField: JTextField
    private lateinit var passwordField: JPasswordField
    private lateinit var loginButton: JButton
    private lateinit var logoutButton: JButton
    private lateinit var userLabel: JLabel
    private lateinit var notificationList: DefaultListModel<NotificationItem>
    private lateinit var projectComboBox: JComboBox<String>
    private lateinit var environmentComboBox: JComboBox<String>
    
    // 自动刷新定时器
    private var refreshTimer: javax.swing.Timer? = null
    
    // 当前选择的项目和环境
    private var currentProject: String? = null
    private var currentEnvironment: String? = null
    
    // 项目名称到ID的映射
    private val projectNameToId = mutableMapOf<String, String>()
    // 环境名称到ID和URL的映射
    private val envNameToId = mutableMapOf<String, String>()
    private val envNameToUrl = mutableMapOf<String, String>()
    
    // 是否正在加载数据（避免触发change事件）
    private var isLoadingData = false
    
    init {
        setupUI()
    }
    
    fun getContent(): JComponent {
        return mainPanel
    }
    
    /**
     * 释放资源
     */
    fun dispose() {
        stopAutoRefresh()
    }
    
    private fun setupUI() {
        mainPanel.layout = BorderLayout()
        mainPanel.border = BorderFactory.createEmptyBorder(10, 10, 10, 10)
        
        // 创建主容器，使用垂直布局
        val containerPanel = JPanel()
        containerPanel.layout = BoxLayout(containerPanel, BoxLayout.Y_AXIS)
        
        // 添加登录区域
        containerPanel.add(createLoginSection())
        containerPanel.add(Box.createVerticalStrut(20))
        
        // 添加通知区域
        containerPanel.add(createNotificationSection())
        
        mainPanel.add(containerPanel, BorderLayout.NORTH)
    }
    
    /**
     * 创建登录区域
     */
    private fun createLoginSection(): JPanel {
        val sectionPanel = JPanel()
        sectionPanel.layout = BorderLayout()
        sectionPanel.border = BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(JBColor.GRAY, 1),
            "用户登录"
        )
        sectionPanel.maximumSize = Dimension(Int.MAX_VALUE, 200)
        
        // 登录表单面板
        loginPanel = createLoginForm()
        
        // 用户信息面板（登录后显示）
        userInfoPanel = createUserInfoPanel()
        userInfoPanel.isVisible = false
        
        // 使用 CardLayout 切换登录和用户信息面板
        val cardPanel = JPanel(CardLayout())
        cardPanel.add(loginPanel, "login")
        cardPanel.add(userInfoPanel, "userInfo")
        
        sectionPanel.add(cardPanel, BorderLayout.CENTER)
        
        return sectionPanel
    }
    
    /**
     * 创建登录表单
     */
    private fun createLoginForm(): JPanel {
        val formPanel = JPanel()
        formPanel.layout = GridBagLayout()
        formPanel.border = BorderFactory.createEmptyBorder(10, 10, 10, 10)
        
        val gbc = GridBagConstraints()
        gbc.fill = GridBagConstraints.HORIZONTAL
        gbc.insets = Insets(5, 5, 5, 5)
        
        // 用户名标签和输入框
        gbc.gridx = 0
        gbc.gridy = 0
        gbc.weightx = 0.3
        val usernameLabel = JLabel("用户名:")
        formPanel.add(usernameLabel, gbc)
        
        gbc.gridx = 1
        gbc.weightx = 0.7
        usernameField = JTextField()
        usernameField.toolTipText = "请输入用户名"
        formPanel.add(usernameField, gbc)
        
        // 密码标签和输入框
        gbc.gridx = 0
        gbc.gridy = 1
        gbc.weightx = 0.3
        val passwordLabel = JLabel("密码:")
        formPanel.add(passwordLabel, gbc)
        
        gbc.gridx = 1
        gbc.weightx = 0.7
        passwordField = JPasswordField()
        passwordField.toolTipText = "请输入密码"
        formPanel.add(passwordField, gbc)
        
        // 登录按钮
        gbc.gridx = 0
        gbc.gridy = 2
        gbc.gridwidth = 2
        gbc.weightx = 1.0
        loginButton = JButton("登录")
        loginButton.icon = AllIcons.Actions.Execute
        loginButton.addActionListener { handleLogin() }
        formPanel.add(loginButton, gbc)
        
        return formPanel
    }
    
    /**
     * 创建用户信息面板（登录后显示）
     */
    private fun createUserInfoPanel(): JPanel {
        val infoPanel = JPanel()
        infoPanel.layout = BorderLayout()
        infoPanel.border = BorderFactory.createEmptyBorder(10, 10, 10, 10)
        
        // 用户信息区域
        val userInfoArea = JPanel()
        userInfoArea.layout = BoxLayout(userInfoArea, BoxLayout.Y_AXIS)
        
        // 用户头像和名称
        val userHeaderPanel = JPanel(BorderLayout())
        
        val leftPanel = JPanel(FlowLayout(FlowLayout.LEFT, 0, 0))
        val avatarLabel = JLabel(AllIcons.General.User)
        userLabel = JLabel("未登录")
        userLabel.font = userLabel.font.deriveFont(Font.BOLD, 14f)
        leftPanel.add(avatarLabel)
        leftPanel.add(Box.createHorizontalStrut(5))
        leftPanel.add(userLabel)
        
        val rightPanel = JPanel(FlowLayout(FlowLayout.RIGHT, 0, 0))
        val syncButton = JButton("代码同步")
        syncButton.addActionListener {
            val envId = envNameToId[currentEnvironment]
            val envUrl = envNameToUrl[currentEnvironment]
            val projectId = projectNameToId[currentProject]
            
            if (envId != null && envUrl != null && projectId != null) {
                CodeSyncDialog(project, envId, envUrl, projectId).show()
            } else {
                JOptionPane.showMessageDialog(mainPanel, "请先选择有效的项目和环境", "提示", JOptionPane.WARNING_MESSAGE)
            }
        }
        rightPanel.add(syncButton)
        
        userHeaderPanel.add(leftPanel, BorderLayout.WEST)
        userHeaderPanel.add(rightPanel, BorderLayout.EAST)
        userInfoArea.add(userHeaderPanel)
        
        // 登录状态
        val statusPanel = JPanel(FlowLayout(FlowLayout.LEFT))
        val statusLabel = JLabel("状态: ")
        val statusValue = JLabel("已登录")
        statusValue.foreground = JBColor.GREEN
        statusValue.icon = AllIcons.RunConfigurations.TestPassed
        statusPanel.add(statusLabel)
        statusPanel.add(statusValue)
        userInfoArea.add(statusPanel)
        
        // 添加间隔
        userInfoArea.add(Box.createVerticalStrut(10))
        
        // 项目选择器
        val projectPanel = JPanel(GridBagLayout())
        val projectGbc = GridBagConstraints()
        projectGbc.fill = GridBagConstraints.HORIZONTAL
        projectGbc.insets = Insets(5, 5, 5, 5)
        
        projectGbc.gridx = 0
        projectGbc.gridy = 0
        projectGbc.weightx = 0.3
        val projectLabel = JLabel("项目:")
        projectPanel.add(projectLabel, projectGbc)
        
        projectGbc.gridx = 1
        projectGbc.weightx = 0.7
        projectComboBox = JComboBox()
        projectComboBox.addItem("-- 请选择项目 --")
        projectComboBox.addActionListener {
            handleProjectChange()
        }
        projectPanel.add(projectComboBox, projectGbc)
        userInfoArea.add(projectPanel)
        
        // 环境选择器
        val envPanel = JPanel(GridBagLayout())
        val envGbc = GridBagConstraints()
        envGbc.fill = GridBagConstraints.HORIZONTAL
        envGbc.insets = Insets(5, 5, 5, 5)
        
        envGbc.gridx = 0
        envGbc.gridy = 0
        envGbc.weightx = 0.3
        val envLabel = JLabel("环境:")
        envPanel.add(envLabel, envGbc)
        
        envGbc.gridx = 1
        envGbc.weightx = 0.7
        environmentComboBox = JComboBox()
        environmentComboBox.addItem("-- 请选择环境 --")
        environmentComboBox.addActionListener {
            handleEnvironmentChange()
        }
        envPanel.add(environmentComboBox, envGbc)
        userInfoArea.add(envPanel)
        
        infoPanel.add(userInfoArea, BorderLayout.CENTER)
        
        // 登出按钮
        logoutButton = JButton("登出")
        logoutButton.icon = AllIcons.Actions.Exit
        logoutButton.addActionListener { handleLogout() }
        infoPanel.add(logoutButton, BorderLayout.SOUTH)
        
        return infoPanel
    }
    
    /**
     * 创建通知区域
     */
    private fun createNotificationSection(): JPanel {
        val sectionPanel = JPanel()
        sectionPanel.layout = BorderLayout()
        sectionPanel.border = BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(JBColor.GRAY, 1),
            "通知中心"
        )
        
        // 通知列表
        notificationList = DefaultListModel()
        val jList = JList(notificationList)
        jList.cellRenderer = NotificationCellRenderer()
        jList.selectionMode = ListSelectionModel.SINGLE_SELECTION
        
        // 添加示例通知
        addSampleNotifications()
        
        val scrollPane = JBScrollPane(jList)
        scrollPane.preferredSize = Dimension(300, 300)
        
        sectionPanel.add(scrollPane, BorderLayout.CENTER)
        
        // 底部操作按钮
        val buttonPanel = JPanel(FlowLayout(FlowLayout.RIGHT))
        val refreshButton = JButton("刷新")
        refreshButton.icon = AllIcons.Actions.Refresh
        refreshButton.addActionListener { refreshNotifications() }
        
        val clearButton = JButton("清空")
        clearButton.icon = AllIcons.Actions.GC
        clearButton.addActionListener { clearNotifications() }
        
        buttonPanel.add(refreshButton)
        buttonPanel.add(clearButton)
        
        sectionPanel.add(buttonPanel, BorderLayout.SOUTH)
        
        return sectionPanel
    }
    
    /**
     * 处理登录
     */
    private fun handleLogin() {
        val username = usernameField.text.trim()
        val password = String(passwordField.password)
        
        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(
                mainPanel,
                "请输入用户名和密码",
                "登录失败",
                JOptionPane.WARNING_MESSAGE
            )
            return
        }
        
        // 禁用登录按钮，防止重复点击
        loginButton.isEnabled = false
        loginButton.text = "登录中..."
        
        // 在后台线程执行登录
        com.intellij.openapi.application.ApplicationManager.getApplication().executeOnPooledThread {
            val loginService = top.javaxiaowu.saleseasypackagingassistant.services.ServiceFactory.getLoginService()
            val result = loginService.login(username, password)
            
            // 在UI线程更新界面
            com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater {
                loginButton.isEnabled = true
                loginButton.text = "登录"
                
                if (result.success) {
                    // 登录成功
                    this.username = result.userInfo?.username ?: username
                    this.isLoggedIn = true
                    
                    // 更新 UI
                    userLabel.text = this.username
                    loginPanel.isVisible = false
                    userInfoPanel.isVisible = true
                    
                    // 切换面板
                    val parent = loginPanel.parent as JPanel
                    val layout = parent.layout as CardLayout
                    layout.show(parent, "userInfo")
                    
                    // 显示成功消息
                    JOptionPane.showMessageDialog(
                        mainPanel,
                        "登录成功！欢迎, ${this.username}",
                        "登录成功",
                        JOptionPane.INFORMATION_MESSAGE
                    )
                    
                    // 添加登录成功通知
                    addNotification("登录成功", "欢迎回来, ${this.username}!", NotificationType.SUCCESS)
                    
                    // 加载项目和环境列表
                    loadProjectsAndEnvironments()
                    
                    // 自动刷新通知（静默）
                    refreshNotifications(silent = true)
                    
                    // 启动自动刷新定时器
                    startAutoRefresh()
                } else {
                    // 登录失败
                    JOptionPane.showMessageDialog(
                        mainPanel,
                        "登录失败: ${result.message}",
                        "登录失败",
                        JOptionPane.ERROR_MESSAGE
                    )
                }
            }
        }
    }
    
    /**
     * 处理登出
     */
    private fun handleLogout() {
        val result = JOptionPane.showConfirmDialog(
            mainPanel,
            "确定要登出吗？",
            "确认登出",
            JOptionPane.YES_NO_OPTION
        )
        
        if (result == JOptionPane.YES_OPTION) {
            // 停止自动刷新
            stopAutoRefresh()
            
            // 调用登出服务
            val loginService = top.javaxiaowu.saleseasypackagingassistant.services.ServiceFactory.getLoginService()
            loginService.logout()
            
            // 清空登录信息
            this.username = null
            this.isLoggedIn = false
            usernameField.text = ""
            passwordField.text = ""
            
            // 切换回登录面板
            loginPanel.isVisible = true
            userInfoPanel.isVisible = false
            
            val parent = loginPanel.parent as JPanel
            val layout = parent.layout as CardLayout
            layout.show(parent, "login")
            
            // 清空通知列表
            notificationList.clear()
            
            // 添加登出通知
            addNotification("已登出", "您已成功登出", NotificationType.INFO)
        }
    }
    
    /**
     * 添加示例通知（已弃用，改为从API加载）
     */
    @Deprecated("使用 refreshNotifications() 从API加载通知")
    private fun addSampleNotifications() {
        // 不再添加示例通知，改为显示欢迎消息
        addNotification("欢迎", "欢迎使用 SalesEasy 打包助手，请登录后查看通知", NotificationType.INFO)
    }
    
    /**
     * 添加通知
     */
    private fun addNotification(title: String, message: String, type: NotificationType) {
        val notification = NotificationItem(title, message, type, System.currentTimeMillis(), false)
        notificationList.add(0, notification)  // 添加到列表顶部
        
        // 限制通知数量
        while (notificationList.size() > 50) {
            notificationList.remove(notificationList.size() - 1)
        }
    }
    
    /**
     * 刷新通知
     * @param silent 是否静默刷新（不显示提示消息）
     */
    private fun refreshNotifications(silent: Boolean = false) {
        if (!isLoggedIn) {
            if (!silent) {
                addNotification("未登录", "请先登录后再刷新通知", NotificationType.WARNING)
            }
            return
        }
        
        // 记录刷新前的通知数量
        val oldNotificationCount = notificationList.size()
        
        // 在后台线程执行刷新
        com.intellij.openapi.application.ApplicationManager.getApplication().executeOnPooledThread {
            try {
                val notificationService = top.javaxiaowu.saleseasypackagingassistant.services.ServiceFactory.getNotificationService()
                val notifications = notificationService.getNotifications(page = 1, pageSize = 50)
                
                // 在UI线程更新界面
                com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater {
                    // 清空现有通知
                    notificationList.clear()
                    
                    // 添加新通知
                    val unreadNotifications = notifications.filter { !it.isRead }
                    unreadNotifications.forEach { notification ->
                        notificationList.addElement(notification)
                    }
                    
                    // 如果没有通知且不是静默刷新，显示提示
                    if (unreadNotifications.isEmpty() && !silent) {
                        addNotification("无未读通知", "暂无新的未读通知", NotificationType.INFO)
                    }
                    
                    // 检测是否有新通知（仅在静默刷新时）
                    if (silent && unreadNotifications.isNotEmpty()) {
                        val newNotificationCount = unreadNotifications.size
                        if (newNotificationCount > oldNotificationCount) {
                            // 有新通知，显示系统通知
                            val newCount = newNotificationCount - oldNotificationCount
                            showSystemNotification(
                                "收到 $newCount 条新通知",
                                unreadNotifications.firstOrNull()?.title ?: "新通知"
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                // 在UI线程显示错误（仅在非静默模式）
                if (!silent) {
                    com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater {
                        addNotification(
                            "刷新失败",
                            "无法获取通知: ${e.message}",
                            NotificationType.ERROR
                        )
                    }
                }
            }
        }
    }
    
    /**
     * 显示系统通知（气泡通知）
     */
    private fun showSystemNotification(title: String, content: String) {
        val notificationGroup = com.intellij.notification.NotificationGroupManager.getInstance()
            .getNotificationGroup("SalesEasy.Notifications")
        
        if (notificationGroup != null) {
            val notification = notificationGroup.createNotification(
                title,
                content,
                com.intellij.notification.NotificationType.INFORMATION
            )
            notification.notify(project)
        } else {
            // 如果通知组不存在，使用默认方式
            com.intellij.notification.Notifications.Bus.notify(
                com.intellij.notification.Notification(
                    "SalesEasy",
                    title,
                    content,
                    com.intellij.notification.NotificationType.INFORMATION
                ),
                project
            )
        }
    }
    
    /**
     * 启动自动刷新
     */
    private fun startAutoRefresh() {
        stopAutoRefresh() // 先停止已有的定时器
        
        // 每30秒自动刷新一次（静默刷新）
        refreshTimer = javax.swing.Timer(30000) {
            refreshNotifications(silent = true)
        }
        refreshTimer?.start()
    }
    
    /**
     * 停止自动刷新
     */
    private fun stopAutoRefresh() {
        refreshTimer?.stop()
        refreshTimer = null
    }
    
    /**
     * 清空通知
     */
    private fun clearNotifications() {
        val result = JOptionPane.showConfirmDialog(
            mainPanel,
            "确定要清空所有通知吗？",
            "确认清空",
            JOptionPane.YES_NO_OPTION
        )
        
        if (result == JOptionPane.YES_OPTION) {
            notificationList.clear()
            addNotification("已清空", "所有通知已清空", NotificationType.INFO)
        }
    }
    
    /**
     * 加载项目和环境列表
     */
    private fun loadProjectsAndEnvironments() {
        // 在后台线程加载
        com.intellij.openapi.application.ApplicationManager.getApplication().executeOnPooledThread {
            try {
                // 调用API获取项目列表
                val responseType = object : TypeToken<PaginatedResponse<ProjectItem>>() {}.type
                val projectsResponse = top.javaxiaowu.saleseasypackagingassistant.utils.HttpClient.get<PaginatedResponse<ProjectItem>>(
                    endpoint = top.javaxiaowu.saleseasypackagingassistant.config.ApiConfig.Projects.LIST,
                    queryParams = mapOf("skip" to "0", "limit" to "500"),
                    requireAuth = true,
                    responseType = responseType
                )
                
                // 在UI线程更新下拉框
                com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater {
                    // 设置加载标志，避免触发change事件
                    isLoadingData = true
                    
                    // 清空现有项
                    projectComboBox.removeAllItems()
                    projectNameToId.clear()
                    
                    // 添加默认选项
                    projectComboBox.addItem("-- 请选择项目 --")
                    
                    // 添加项目列表
                    if (projectsResponse.success && projectsResponse.data != null) {
                        val items = projectsResponse.data.items
                        items.forEach { project ->
                            projectComboBox.addItem(project.name)
                            projectNameToId[project.name] = project.id
                        }
                        
                        // 如果有项目，加载第一个项目的环境（但不触发通知）
                        if (items.isNotEmpty()) {
                            val firstProject = items[0]
                            loadEnvironmentsForProject(firstProject.id)
                        }
                    } else {
                        println("获取项目列表失败: ${projectsResponse.message}")
                        // 添加默认环境选项
                        environmentComboBox.removeAllItems()
                        environmentComboBox.addItem("-- 请选择环境 --")
                    }
                    
                    // 恢复加载标志
                    isLoadingData = false
                }
            } catch (e: Exception) {
                println("加载项目列表失败: ${e.message}")
                // 在UI线程显示错误
                com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater {
                    projectComboBox.removeAllItems()
                    projectComboBox.addItem("-- 加载失败 --")
                    environmentComboBox.removeAllItems()
                    environmentComboBox.addItem("-- 请先选择项目 --")
                }
            }
        }
    }
    
    /**
     * 加载指定项目的环境列表
     */
    private fun loadEnvironmentsForProject(projectId: String) {
        com.intellij.openapi.application.ApplicationManager.getApplication().executeOnPooledThread {
            try {
                // 调用API获取环境列表
                val endpoint = top.javaxiaowu.saleseasypackagingassistant.config.ApiConfig.Environments.LIST
                    .replace("{project_id}", projectId)
                val environmentsResponse = top.javaxiaowu.saleseasypackagingassistant.utils.HttpClient.get<Array<EnvironmentItem>>(
                    endpoint = endpoint,
                    requireAuth = true,
                    responseType = Array<EnvironmentItem>::class.java
                )
                
                // 在UI线程更新下拉框
                com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater {
                    // 设置加载标志
                    isLoadingData = true
                    
                    // 清空现有项
                    environmentComboBox.removeAllItems()
                    envNameToId.clear()
                    envNameToUrl.clear()
                    
                    // 添加默认选项
                    environmentComboBox.addItem("-- 请选择环境 --")
                    
                    // 添加环境列表
                    if (environmentsResponse.success && environmentsResponse.data != null) {
                        environmentsResponse.data.forEach { env ->
                            environmentComboBox.addItem(env.name)
                            envNameToId[env.name] = env.id
                            envNameToUrl[env.name] = env.url
                        }
                    } else {
                        println("获取环境列表失败: ${environmentsResponse.message}")
                    }
                    
                    // 恢复加载标志
                    isLoadingData = false
                }
            } catch (e: Exception) {
                println("加载环境列表失败: ${e.message}")
                com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater {
                    environmentComboBox.removeAllItems()
                    environmentComboBox.addItem("-- 加载失败 --")
                }
            }
        }
    }
    
    /**
     * 项目数据类
     */
    private data class ProjectItem(
        val id: String,
        val name: String,
        val description: String?,
        val project_manager_id: String?,
        val start_date: String?,
        val end_date: String?
    )
    
    /**
     * 环境数据类
     */
    private data class EnvironmentItem(
        val id: String,
        val name: String,
        val url: String,
        val username: String?,
        val description: String?
    )
    
    /**
     * 处理项目选择变化
     */
    private fun handleProjectChange() {
        // 如果正在加载数据，不处理change事件
        if (isLoadingData) {
            return
        }
        
        val selectedProject = projectComboBox.selectedItem as? String
        if (selectedProject != null && !selectedProject.startsWith("--")) {
            currentProject = selectedProject
            println("切换到项目: $currentProject")
            
            // 获取项目ID并加载对应的环境
            val projectId = projectNameToId[selectedProject]
            if (projectId != null) {
                loadEnvironmentsForProject(projectId)
            }
            
            // 显示通知
            addNotification(
                "项目切换",
                "已切换到: $currentProject",
                NotificationType.INFO
            )
        }
    }
    
    /**
     * 处理环境选择变化
     */
    private fun handleEnvironmentChange() {
        // 如果正在加载数据，不处理change事件
        if (isLoadingData) {
            return
        }
        
        val selectedEnv = environmentComboBox.selectedItem as? String
        if (selectedEnv != null && !selectedEnv.startsWith("--")) {
            currentEnvironment = selectedEnv
            println("切换到环境: $currentEnvironment")
            
            // 显示通知
            addNotification(
                "环境切换",
                "已切换到: $currentEnvironment",
                NotificationType.INFO
            )
        }
    }
    
    /**
     * 获取当前选择的项目
     */
    fun getCurrentProject(): String? = currentProject
    
    /**
     * 获取当前选择的环境
     */
    fun getCurrentEnvironment(): String? = currentEnvironment
}

/**
 * 通知项数据类
 */
data class NotificationItem(
    val title: String,
    val message: String,
    val type: NotificationType,
    val timestamp: Long,
    val isRead: Boolean = false
)

/**
 * 通知类型枚举
 */
enum class NotificationType {
    INFO,
    SUCCESS,
    WARNING,
    ERROR
}

/**
 * 通知列表单元格渲染器
 */
class NotificationCellRenderer : DefaultListCellRenderer() {
    override fun getListCellRendererComponent(
        list: JList<*>?,
        value: Any?,
        index: Int,
        isSelected: Boolean,
        cellHasFocus: Boolean
    ): Component {
        val component = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus) as JLabel
        
        if (value is NotificationItem) {
            // 设置图标
            component.icon = when (value.type) {
                NotificationType.INFO -> AllIcons.General.Information
                NotificationType.SUCCESS -> AllIcons.RunConfigurations.TestPassed
                NotificationType.WARNING -> AllIcons.General.Warning
                NotificationType.ERROR -> AllIcons.General.Error
            }
            
            // 设置文本
            val timeStr = java.text.SimpleDateFormat("HH:mm:ss").format(java.util.Date(value.timestamp))
            component.text = "<html><b>${value.title}</b><br/>${value.message}<br/><small style='color:gray;'>$timeStr</small></html>"
            
            // 设置边框
            component.border = BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, JBColor.GRAY),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)
            )
        }
        
        return component
    }
}
