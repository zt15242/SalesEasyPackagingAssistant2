package top.javaxiaowu.saleseasypackagingassistant.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory

/**
 * 工具窗口工厂类
 * 用于创建 SalesEasy 侧边栏工具窗口
 */
class SalesEasyToolWindowFactory : ToolWindowFactory {
    
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        // 创建工具窗口内容面板
        val toolWindowPanel = SalesEasyToolWindowPanel(project)
        
        // 创建内容并添加到工具窗口
        val contentFactory = ContentFactory.getInstance()
        val content = contentFactory.createContent(toolWindowPanel.getContent(), "", false)
        toolWindow.contentManager.addContent(content)
    }
    
    override fun shouldBeAvailable(project: Project): Boolean {
        // 工具窗口对所有项目可用
        return true
    }
}
