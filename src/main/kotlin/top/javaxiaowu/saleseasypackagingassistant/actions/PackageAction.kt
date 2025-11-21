package top.javaxiaowu.saleseasypackagingassistant.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.project.Project
import top.javaxiaowu.saleseasypackagingassistant.ui.FileSelectionDialog
import javax.swing.JComponent

class PackageAction : AnAction("Package Files") {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        // 从事件中获取选中的文件/文件夹
        val selectedFiles = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY)
        val dialog = FileSelectionDialog(project, selectedFiles?.toList())
        dialog.show()
    }
    
    override fun update(e: AnActionEvent) {
        // 可以基于是否有选中的文件来決定Action是否可用
        super.update(e)
    }
}
