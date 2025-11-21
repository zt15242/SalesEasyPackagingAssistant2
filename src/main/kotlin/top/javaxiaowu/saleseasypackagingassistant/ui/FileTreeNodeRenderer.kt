package top.javaxiaowu.saleseasypackagingassistant.ui

import com.intellij.icons.AllIcons
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.CheckboxTree
import javax.swing.JTree

class FileTreeNodeRenderer : CheckboxTree.CheckboxTreeCellRenderer() {
    override fun customizeRenderer(
        tree: JTree,
        value: Any,
        selected: Boolean,
        expanded: Boolean,
        leaf: Boolean,
        row: Int,
        hasFocus: Boolean
    ) {
        val node = value as? com.intellij.ui.CheckedTreeNode
        val virtualFile = node?.userObject as? VirtualFile
        
        if (virtualFile != null) {
            val icon = if (virtualFile.isDirectory) AllIcons.Nodes.Folder else AllIcons.Nodes.Class
            textRenderer.icon = icon
            textRenderer.append(virtualFile.name)
        } else {
            textRenderer.append("Unknown")
        }
    }
}
