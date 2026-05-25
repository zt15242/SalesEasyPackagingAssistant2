package top.javaxiaowu.saleseasypackagingassistant.project

import com.intellij.ide.util.projectWizard.ModuleWizardStep
import com.intellij.openapi.ui.Messages
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import javax.swing.*

/**
 * SalesEasy 项目配置向导步骤
 * 用于收集 OAuth 配置信息
 */
class SalesEasyConfigStep(private val builder: SalesEasyModuleBuilder) : ModuleWizardStep() {
    
    private val panel = JPanel()
    
    // 输入框
    private val userNameField = JTextField(20)
    private val passwordField = JPasswordField(20)
    private val securityCodeField = JTextField(20)
    private val clientIdField = JTextField(20)
    private val clientSecretField = JPasswordField(20)
    private val domainField = JTextField(20)
    
    // GroupId 和 ArtifactId
    private val groupIdField = JTextField(20)
    private val artifactIdField = JTextField(20)
    
    init {
        setupUI()
    }
    
    private fun setupUI() {
        panel.layout = GridBagLayout()
        panel.border = BorderFactory.createEmptyBorder(20, 20, 20, 20)
        
        val gbc = GridBagConstraints()
        gbc.fill = GridBagConstraints.HORIZONTAL
        gbc.insets = Insets(5, 5, 5, 5)
        gbc.anchor = GridBagConstraints.WEST
        
        var row = 0
        
        // 标题
        gbc.gridx = 0
        gbc.gridy = row
        gbc.gridwidth = 2
        val titleLabel = JLabel("<html><h2>SalesEasy 项目配置</h2></html>")
        panel.add(titleLabel, gbc)
        row++
        
        // 分隔线
        gbc.gridy = row
        gbc.insets = Insets(10, 5, 10, 5)
        panel.add(JSeparator(), gbc)
        row++
        
        // 项目信息部分
        gbc.gridwidth = 2
        gbc.gridy = row
        gbc.insets = Insets(5, 5, 5, 5)
        val projectInfoLabel = JLabel("<html><b>项目信息</b></html>")
        panel.add(projectInfoLabel, gbc)
        row++
        
        // GroupId
        gbc.gridwidth = 1
        gbc.gridx = 0
        gbc.gridy = row
        gbc.weightx = 0.3
        panel.add(JLabel("GroupId:"), gbc)
        
        gbc.gridx = 1
        gbc.weightx = 0.7
        groupIdField.text = "other.xsy"
        groupIdField.toolTipText = "例如: other.xsy"
        panel.add(groupIdField, gbc)
        row++
        
        // ArtifactId
        gbc.gridx = 0
        gbc.gridy = row
        gbc.weightx = 0.3
        panel.add(JLabel("ArtifactId:"), gbc)
        
        gbc.gridx = 1
        gbc.weightx = 0.7
        artifactIdField.text = "jinpan"
        artifactIdField.toolTipText = "例如: jinpan"
        panel.add(artifactIdField, gbc)
        row++
        
        // OAuth 配置部分
        gbc.gridx = 0
        gbc.gridy = row
        gbc.gridwidth = 2
        gbc.insets = Insets(15, 5, 5, 5)
        val oauthLabel = JLabel("<html><b>OAuth 配置</b></html>")
        panel.add(oauthLabel, gbc)
        row++
        
        gbc.insets = Insets(5, 5, 5, 5)
        
        // UserName
        gbc.gridwidth = 1
        gbc.gridx = 0
        gbc.gridy = row
        gbc.weightx = 0.3
        panel.add(JLabel("UserName:"), gbc)
        
        gbc.gridx = 1
        gbc.weightx = 0.7
        userNameField.toolTipText = "请输入用户名"
        panel.add(userNameField, gbc)
        row++
        
        // Password
        gbc.gridx = 0
        gbc.gridy = row
        gbc.weightx = 0.3
        panel.add(JLabel("Password:"), gbc)
        
        gbc.gridx = 1
        gbc.weightx = 0.7
        passwordField.toolTipText = "请输入密码"
        panel.add(passwordField, gbc)
        row++
        
        // SecurityCode
        gbc.gridx = 0
        gbc.gridy = row
        gbc.weightx = 0.3
        panel.add(JLabel("SecurityCode:"), gbc)
        
        gbc.gridx = 1
        gbc.weightx = 0.7
        securityCodeField.toolTipText = "请输入安全码"
        panel.add(securityCodeField, gbc)
        row++
        
        // ClientId
        gbc.gridx = 0
        gbc.gridy = row
        gbc.weightx = 0.3
        panel.add(JLabel("ClientId:"), gbc)
        
        gbc.gridx = 1
        gbc.weightx = 0.7
        clientIdField.toolTipText = "请输入客户端ID"
        panel.add(clientIdField, gbc)
        row++
        
        // ClientSecret
        gbc.gridx = 0
        gbc.gridy = row
        gbc.weightx = 0.3
        panel.add(JLabel("ClientSecret:"), gbc)
        
        gbc.gridx = 1
        gbc.weightx = 0.7
        clientSecretField.toolTipText = "请输入客户端密钥"
        panel.add(clientSecretField, gbc)
        row++
        
        // Domain
        gbc.gridx = 0
        gbc.gridy = row
        gbc.weightx = 0.3
        panel.add(JLabel("Domain:"), gbc)
        
        gbc.gridx = 1
        gbc.weightx = 0.7
        domainField.toolTipText = "例如: https://api-scrm.xiaoshouyi.com/"
        panel.add(domainField, gbc)
        row++
        
        // 说明文本
        gbc.gridx = 0
        gbc.gridy = row
        gbc.gridwidth = 2
        gbc.insets = Insets(15, 5, 5, 5)
        val noteLabel = JLabel("<html><i>提示: 这些配置将保存到 resources/oauthConfig.properties 文件中</i></html>")
        noteLabel.foreground = java.awt.Color.GRAY
        panel.add(noteLabel, gbc)
        
        // 添加填充空间
        gbc.gridy = row + 1
        gbc.weighty = 1.0
        panel.add(Box.createVerticalGlue(), gbc)
    }
    
    override fun getComponent(): JComponent {
        return panel
    }
    
    override fun updateDataModel() {
        // 保存配置到 builder
        builder.userName = userNameField.text.trim()
        builder.password = String(passwordField.password)
        builder.securityCode = securityCodeField.text.trim()
        builder.clientId = clientIdField.text.trim()
        builder.clientSecret = String(clientSecretField.password)
        builder.domain = domainField.text.trim()
        builder.groupId = groupIdField.text.trim()
        builder.artifactId = artifactIdField.text.trim()
    }
    
    override fun validate(): Boolean {
        // 验证必填字段
        if (groupIdField.text.trim().isEmpty()) {
            Messages.showErrorDialog("GroupId 不能为空", "验证失败")
            return false
        }
        
        if (artifactIdField.text.trim().isEmpty()) {
            Messages.showErrorDialog("ArtifactId 不能为空", "验证失败")
            return false
        }
        
        // OAuth 配置可以为空，但如果填写了就要完整
        val hasAnyOAuthConfig = userNameField.text.isNotEmpty() ||
                passwordField.password.isNotEmpty() ||
                securityCodeField.text.isNotEmpty() ||
                clientIdField.text.isNotEmpty() ||
                clientSecretField.password.isNotEmpty() ||
                domainField.text.isNotEmpty()
        
        if (hasAnyOAuthConfig) {
            if (userNameField.text.trim().isEmpty()) {
                Messages.showErrorDialog("如果配置 OAuth，UserName 不能为空", "验证失败")
                return false
            }
            if (passwordField.password.isEmpty()) {
                Messages.showErrorDialog("如果配置 OAuth，Password 不能为空", "验证失败")
                return false
            }
            if (domainField.text.trim().isEmpty()) {
                Messages.showErrorDialog("如果配置 OAuth，Domain 不能为空", "验证失败")
                return false
            }
        }
        
        return true
    }
}
